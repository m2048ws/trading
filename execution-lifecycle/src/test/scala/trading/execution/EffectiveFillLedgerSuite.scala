package trading.execution

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream
import java.lang.reflect.Modifier

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.InstrumentFixtures
import trading.economics.instrument.Lots
import trading.economics.instrument.PositionLots
import trading.economics.instrument.Price
import trading.order.Order
import trading.order.Side
import trading.quantity.JavaSerializationUnsupported
import trading.quantity.Rational

final class EffectiveFillLedgerSuite extends ScalaCheckSuite:
  private val fixtures    = new InstrumentFixtures
  private val instrument  = fixtures.linear
  private val target      = executionTarget("source", "account")
  private val sourceOrder = required(
    QualifiedSourceOrderId.create(target, id(NativeSourceOrderId.from("source-order")))
  )
  private val sourceStream = stream("orders")
  private val otherStream  = stream("corrections")

  private type D = instrument.roles.position.D
  private type B = instrument.roles.base.D
  private type Q = instrument.roles.quote.D

  private def required[A](value: Either[?, A]): A =
    value.fold(error => fail(s"expected checked value, received $error"), identity)

  private def id[A](value: Either[ExecutionIdentityError, A]): A = required(value)

  private def executionTarget(source: String, account: String): ExecutionTarget =
    required(ExecutionTarget.create(id(ExecutionSourceId.from(source)), id(ExecutionAccountId.from(account))))

  private def stream(value: String): QualifiedSourceStreamId =
    required(QualifiedSourceStreamId.create(target, id(SourceStreamId.from(value))))

  private def lifecycle(side: Side, orderedLots: BigInt, suffix: String): ExecutionLifecycle[D, B, Q] =
    val order = required(Order.market(instrument)(side, fixtures.lots(instrument, orderedLots)))
    required(
      ExecutionLifecycle.create(instrument)(
        order,
        id(ExecutionOrderId.from(s"logical-$suffix")),
        id(OrderLineageId.from(s"lineage-$suffix")),
        target
      )
    )

  private def event(value: String): QualifiedSourceEventId =
    required(QualifiedSourceEventId.create(target, id(NativeSourceEventId.from(value))))

  private def fillId(value: String): QualifiedFillId =
    required(QualifiedFillId.create(target, id(NativeFillId.from(value))))

  private def ordering(
    source: QualifiedSourceStreamId,
    sequence: BigInt
  ): AuthoritativelySequenced =
    val position = required(QualifiedStreamPosition.create(source, id(SourceSequence.from(sequence))))
    required(SourceOrdering.sequenced(position, required(SourceContinuation.origin(source))))

  private def fill(
    lifecycle: ExecutionLifecycle[D, B, Q],
    eventValue: String,
    fillValue: String,
    lots: BigInt,
    price: BigInt = 1,
    sourceOrdering: SourceOrdering = SourceOrdering.unsequenced
  ): ExecutionFill[D, B, Q] =
    required(
      ExecutionFill.create(lifecycle)(
        event(eventValue),
        lifecycle.executionOrderId,
        sourceOrder,
        fillId(fillValue),
        fixtures.lots(instrument, lots),
        fixtures.price(instrument, Rational(price)),
        sourceOrdering
      )
    )

  private def correction(
    lifecycle: ExecutionLifecycle[D, B, Q],
    eventValue: String,
    targetFill: QualifiedFillId,
    lots: BigInt,
    price: BigInt,
    sourceOrdering: SourceOrdering
  ): FillCorrected[D, B, Q] =
    required(
      FillCorrected.create(lifecycle)(
        event(eventValue),
        lifecycle.executionOrderId,
        sourceOrder,
        targetFill,
        fixtures.lots(instrument, lots),
        fixtures.price(instrument, Rational(price)),
        sourceOrdering
      )
    )

  private def bust(
    lifecycle: ExecutionLifecycle[D, B, Q],
    eventValue: String,
    targetFill: QualifiedFillId,
    sourceOrdering: SourceOrdering
  ): FillBusted[D, B, Q] =
    required(
      FillBusted.create(lifecycle)(
        event(eventValue),
        lifecycle.executionOrderId,
        sourceOrder,
        targetFill,
        sourceOrdering
      )
    )

  private def initial(lifecycle: ExecutionLifecycle[D, B, Q]): ExecutionState[D, B, Q] =
    required(ExecutionState.initial(lifecycle))

  private def accepted(value: LifecycleTransition[D, B, Q]): LifecycleAccepted[D, B, Q] = value match
    case transition: LifecycleAccepted[?, ?, ?] => transition.asInstanceOf[LifecycleAccepted[D, B, Q]]
    case transition: LifecycleRejected[?, ?, ?] => fail(s"unexpected rejection: ${transition.rejection}")

  private def record(
    lifecycle: ExecutionLifecycle[D, B, Q],
    facts: Vector[SourceFact[D, B, Q]]
  ): ExecutionState[D, B, Q] =
    facts.foldLeft(initial(lifecycle))((state, fact) => accepted(state.record(fact)).state)

  private def replay(
    lifecycle: ExecutionLifecycle[D, B, Q],
    facts: Vector[SourceFact[D, B, Q]]
  ): LifecycleReplayResult[D, B, Q] =
    required(ExecutionState.replay(lifecycle)(Vector.empty, Vector.empty, facts))

  private def position(lifecycle: ExecutionLifecycle[D, B, Q], coordinate: BigInt): PositionLots[D] =
    PositionLots.fromCoordinate(lifecycle.instrument)(coordinate).asInstanceOf[PositionLots[D]]

  private def assertSerializationRejected(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  test("distinct partial fills contribute once each while event and fill-identity duplicates do not double count"):
    val value     = lifecycle(Side.Buy, 10, "partials")
    val first     = fill(value, "event-1", "fill-1", 2)
    val second    = fill(value, "event-2", "fill-2", 2)
    val duplicate = fill(value, "event-3", "fill-1", 2)
    val state     = record(value, Vector(first, first, duplicate, second))
    val ledger    = state.observation.effectiveFillLedger

    assertEquals(ledger.byFillId.keySet, Set(first.fillId, second.fillId))
    assertEquals(ledger.knownExposure, position(value, 4))
    assertEquals(ledger.overfill, None)
    assertEquals(state.source.fillsById(first.fillId), first)
    assertEquals(state.source.fillConflicts, Vector.empty)
    assert(ledger.byFillId.values.forall(_.isInstanceOf[ActiveEffectiveFill[?, ?, ?]]))

  test("an authoritatively ordered correction replaces exact economics and a later bust contributes zero"):
    val value           = lifecycle(Side.Buy, 10, "correction")
    val original        = fill(value, "fill", "fill", 2, 1)
    val first           = correction(value, "correction-1", original.fillId, 4, 2, ordering(sourceStream, 1))
    val second          = correction(value, "correction-2", original.fillId, 5, 3, ordering(sourceStream, 2))
    val correctedLedger = record(value, Vector(original, second, first)).observation.effectiveFillLedger

    correctedLedger.byFillId(original.fillId) match
      case active: ActiveEffectiveFill[?, ?, ?] =>
        assertEquals(active.original, original)
        assertEquals(active.effectiveLots, fixtures.lots(instrument, 5))
        assertEquals(active.effectivePrice, fixtures.price(instrument, Rational(3)))
        assertEquals(active.modifiers, Vector(first, second))
      case other => fail(s"expected active corrected fill, received $other")
    assertEquals(correctedLedger.knownExposure, position(value, 5))

    val finalBust    = bust(value, "bust", original.fillId, ordering(sourceStream, 3))
    val bustedLedger = record(value, Vector(finalBust, second, original, first)).observation.effectiveFillLedger
    bustedLedger.byFillId(original.fillId) match
      case busted: BustedEffectiveFill[?, ?, ?] =>
        assertEquals(busted.original, original)
        assertEquals(busted.bust, finalBust)
        assertEquals(busted.modifiers, Vector(first, second, finalBust))
      case other => fail(s"expected busted fill, received $other")
    assertEquals(bustedLedger.knownExposure, position(value, 0))

  test("a modifier before its fill remains unresolved, then resolves without inventing interim exposure"):
    val value       = lifecycle(Side.Buy, 10, "modifier-first")
    val original    = fill(value, "fill", "fill", 2)
    val replacement = correction(value, "correction", original.fillId, 6, 2, ordering(sourceStream, 1))
    val unresolved  = accepted(initial(value).record(replacement)).state
    val before      = unresolved.observation.effectiveFillLedger

    assertEquals(before.byFillId, Map.empty)
    assertEquals(before.knownExposure, position(value, 0))
    assertEquals(before.unresolvedReferences(original.fillId).map(_.modifier), Vector(replacement))

    val resolved = accepted(unresolved.record(original)).state.observation.effectiveFillLedger
    assert(!resolved.unresolvedReferences.contains(original.fillId))
    assertEquals(resolved.knownExposure, position(value, 6))
    assert(resolved.byFillId(original.fillId).isInstanceOf[ActiveEffectiveFill[?, ?, ?]])

  test("unsequenced, conflicting-position, multi-stream, and post-bust modifier chains are explicit ambiguity"):
    val value    = lifecycle(Side.Buy, 10, "ambiguity")
    val original = fill(value, "fill", "fill", 2)

    def ambiguity(facts: Vector[SourceFact[D, B, Q]]): ModifierAmbiguity =
      record(value, original +: facts).observation.effectiveFillLedger.byFillId(original.fillId) match
        case result: AmbiguousEffectiveFill[?, ?, ?] => result.ambiguity
        case other                                   => fail(s"expected ambiguous fill, received $other")

    val unsequenced = correction(value, "unsequenced", original.fillId, 3, 2, SourceOrdering.unsequenced)
    assertEquals(
      ambiguity(Vector(unsequenced)).toVector,
      Vector(ModifierAmbiguityKind.ExplicitlyUnsequencedModifier)
    )

    val samePosition          = ordering(sourceStream, 1)
    val conflictingCorrection = correction(value, "position-correction", original.fillId, 3, 2, samePosition)
    val conflictingBust       = bust(value, "position-bust", original.fillId, samePosition)
    assert(
      ambiguity(Vector(conflictingCorrection, conflictingBust)).toVector.contains(
        ModifierAmbiguityKind.ConflictingAuthoritativePosition
      )
    )

    val firstStream  = correction(value, "stream-a", original.fillId, 3, 2, ordering(sourceStream, 1))
    val secondStream = correction(value, "stream-b", original.fillId, 4, 3, ordering(otherStream, 2))
    assert(
      ambiguity(Vector(firstStream, secondStream)).toVector.contains(
        ModifierAmbiguityKind.MultipleAuthoritativeStreams
      )
    )

    val earlyBust      = bust(value, "early-bust", original.fillId, ordering(sourceStream, 1))
    val afterBust      = correction(value, "after-bust", original.fillId, 3, 2, ordering(sourceStream, 2))
    val afterBustKinds = ambiguity(Vector(afterBust, earlyBust)).toVector
    assert(afterBustKinds.contains(ModifierAmbiguityKind.ModifierAfterBust))

  test("conflicting fill economics remain diagnosed and contribute no invented exposure"):
    val value       = lifecycle(Side.Buy, 10, "conflict")
    val original    = fill(value, "event-1", "fill", 2)
    val conflicting = fill(value, "event-2", "fill", 3)
    val state       = record(value, Vector(original, conflicting))
    val ledger      = state.observation.effectiveFillLedger

    ledger.byFillId(original.fillId) match
      case conflict: ConflictingEffectiveFill[?, ?, ?] =>
        assertEquals(conflict.original, original)
        assertEquals(conflict.identityConflicts.size, 1)
        assertEquals(conflict.identityConflicts.head.conflicting, conflicting)
      case other => fail(s"expected conflicting fill, received $other")
    assertEquals(ledger.knownExposure, position(value, 0))
    assertEquals(ledger.overfill, None)

  test("known exposure and exact overfill excess preserve immutable buy and sell direction"):
    val buy         = lifecycle(Side.Buy, 10, "buy-overfill")
    val sell        = lifecycle(Side.Sell, 10, "sell-overfill")
    val equal       = lifecycle(Side.Buy, 10, "equal")
    val buyLedger   = record(buy, Vector(fill(buy, "buy-fill", "buy-fill", 12))).observation.effectiveFillLedger
    val sellLedger  = record(sell, Vector(fill(sell, "sell-fill", "sell-fill", 12))).observation.effectiveFillLedger
    val equalLedger = record(equal, Vector(fill(equal, "equal-fill", "equal-fill", 10))).observation.effectiveFillLedger

    assertEquals(buyLedger.knownExposure, position(buy, 12))
    assertEquals(buyLedger.overfill.map(_.excessExposure), Some(position(buy, 2)))
    assertEquals(buyLedger.overfill.map(_.orderedLots), Some(buy.orderedLots))
    assertEquals(sellLedger.knownExposure, position(sell, -12))
    assertEquals(sellLedger.overfill.map(_.excessExposure), Some(position(sell, -2)))
    assertEquals(equalLedger.knownExposure, position(equal, 10))
    assertEquals(equalLedger.overfill, None)
    assertEquals(initial(equal).observation.effectiveFillLedger.knownExposure, position(equal, 0))

  test("foreign instrument economics are rejected before an effective ledger contribution exists"):
    val value             = lifecycle(Side.Buy, 10, "foreign")
    val foreignInstrument = fixtures.foreignIdentity
    val result            = ExecutionFill.create(value)(
      event("foreign-fill"),
      value.executionOrderId,
      sourceOrder,
      fillId("foreign-fill"),
      fixtures.lots(foreignInstrument, 2).asInstanceOf[Lots[D]],
      fixtures.price(foreignInstrument, Rational.one).asInstanceOf[Price[B, Q]],
      SourceOrdering.unsequenced
    )

    assert(result.isLeft)
    assertEquals(initial(value).observation.effectiveFillLedger.knownExposure, position(value, 0))

  property("canonical replay gives effective ledgers stable equality and hashes across delivery permutations"):
    forAll { (reverse: Boolean) =>
      val value    = lifecycle(Side.Sell, 20, "permutation")
      val original = fill(value, "fill-1", "fill-1", 2)
      val other    = fill(value, "fill-2", "fill-2", 4)
      val modifier = correction(value, "correction", original.fillId, 3, 2, ordering(sourceStream, 2))
      val facts    = Vector[SourceFact[D, B, Q]](original, other, modifier)
      val forward  = replay(value, facts)
      val supplied = if reverse then facts.reverse else facts
      val permuted = replay(value, supplied)
      val left     = forward.state.observation.effectiveFillLedger
      val right    = permuted.state.observation.effectiveFillLedger

      forward.rejections.isEmpty && permuted.rejections.isEmpty &&
      left == right && right == left && left.hashCode == right.hashCode &&
      left.knownExposure == position(value, -7)
    }

  test("effective ledger representations are closed immutable values with structural equality"):
    val value           = lifecycle(Side.Buy, 10, "representation")
    val original        = fill(value, "fill", "fill", 2)
    val ledger          = record(value, Vector(original)).observation.effectiveFillLedger
    val active          = ledger.byFillId(original.fillId)
    val repeated        = replay(value, Vector(original)).state.observation.effectiveFillLedger
    val representations = List(
      classOf[ModifierAmbiguity],
      classOf[ActiveEffectiveFill[?, ?, ?]],
      classOf[BustedEffectiveFill[?, ?, ?]],
      classOf[AmbiguousEffectiveFill[?, ?, ?]],
      classOf[ConflictingEffectiveFill[?, ?, ?]],
      classOf[EffectiveFillLedger[?, ?, ?]]
    )
    representations.foreach: representation =>
      assert(Modifier.isFinal(representation.getModifiers), s"${representation.getName} must be final")

    assertEquals(ledger, repeated)
    assertEquals(ledger.hashCode, repeated.hashCode)
    List[JavaSerializationUnsupported](ledger, active,
      OverfillAnomaly(value.orderedLots, position(value, 12), position(value, 2)))
      .foreach(assertSerializationRejected)

end EffectiveFillLedgerSuite
