package trading.scenario

import cats.syntax.all.*

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*

/** Fee classification of one complete scenario slice. */
enum LiquidityRole:
  case Maker, Taker

/** Entry or exit attribution retained on converted fee contributions. */
enum ScenarioLeg:
  case Entry, Exit

final case class LiquiditySlice[L, M] private[scenario] (
  instrumentId: InstrumentId,
  lots: L,
  market: M,
  role: LiquidityRole)

object LiquiditySlice:
  def create[I <: Instrument](
    instrument: I
  )(
    lots: instrument.Lots,
    market: instrument.MarketState,
    role: LiquidityRole
  ): Either[ScenarioError, LiquiditySlice[instrument.Lots, instrument.MarketState]] =
    ScenarioIdentityChecks
      .check(
        "scenario.slice",
        instrument.identity.id,
        "lots"   -> lots.instrumentId,
        "market" -> market.instrumentId
      )
      .map(_ => LiquiditySlice(instrument.identity.id, lots, market, role))

/** Domain non-empty collection of matched liquidity. */
final class MatchedSlices[L, M] private (
  val head: LiquiditySlice[L, M],
  val tail: Vector[LiquiditySlice[L, M]]):

  val toVector: Vector[LiquiditySlice[L, M]] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: MatchedSlices[?, ?] => toVector == that.toVector
      case _                         => false

  override def hashCode: Int = toVector.hashCode

  override def toString: String = toVector.mkString("MatchedSlices(", ",", ")")
end MatchedSlices

object MatchedSlices:
  def one[L, M](head: LiquiditySlice[L, M]): MatchedSlices[L, M] =
    new MatchedSlices(head, Vector.empty)

  def of[L, M](
    head: LiquiditySlice[L, M],
    tail: LiquiditySlice[L, M]*
  ): MatchedSlices[L, M] =
    new MatchedSlices(head, tail.toVector)

  def fromVector[L, M](
    values: Vector[LiquiditySlice[L, M]]
  ): Either[ScenarioViolation, MatchedSlices[L, M]] =
    values match
      case head +: tail => Right(new MatchedSlices(head, tail))
      case _            => Left(ScenarioViolation.EmptySlices)
end MatchedSlices

/** Cohesive evidence and non-empty matched liquidity for one stable order value. */
final class ScenarioAssumptions[D <: Dim, B <: Dim, Q <: Dim, M] private[scenario] (
  val order: Order[D, B, Q]
)(
  val activationEvidence: order.activation.Evidence,
  val pricingResolution: order.execution.Resolution,
  val matchedSlices: MatchedSlices[Lots[D], M])

object ScenarioAssumptions:
  def create[D <: Dim, B <: Dim, Q <: Dim, M, O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlices: MatchedSlices[Lots[D], M]
  ): ScenarioAssumptions[D, B, Q, M] =
    new ScenarioAssumptions(order)(activationEvidence, pricingResolution, matchedSlices)

  def one[D <: Dim, B <: Dim, Q <: Dim, M, O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlice: LiquiditySlice[Lots[D], M]
  ): ScenarioAssumptions[D, B, Q, M] =
    create(order)(activationEvidence, pricingResolution, MatchedSlices.one(matchedSlice))

  def many[D <: Dim, B <: Dim, Q <: Dim, M, O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    head: LiquiditySlice[Lots[D], M],
    tail: LiquiditySlice[Lots[D], M]*
  ): ScenarioAssumptions[D, B, Q, M] =
    create(order)(activationEvidence, pricingResolution, MatchedSlices.of(head, tail*))

  def fromVector[D <: Dim, B <: Dim, Q <: Dim, M, O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlices: Vector[LiquiditySlice[Lots[D], M]]
  ): Either[ScenarioViolation, ScenarioAssumptions[D, B, Q, M]] =
    MatchedSlices
      .fromVector(matchedSlices)
      .map(create(order)(activationEvidence, pricingResolution, _))
end ScenarioAssumptions

final case class OrderScenario[D <: Dim, B <: Dim, Q <: Dim, M, Pos] private[scenario] (
  instrumentId: InstrumentId,
  order: Order[D, B, Q],
  assumptions: ScenarioAssumptions[D, B, Q, M],
  effectivePricing: EffectivePricing[B, Q],
  positionChange: Pos)

final case class RoundTripScenario[D <: Dim, B <: Dim, Q <: Dim, M, Pos] private[scenario] (
  instrumentId: InstrumentId,
  entry: OrderScenario[D, B, Q, M, Pos],
  exit: OrderScenario[D, B, Q, M, Pos],
  heldPosition: Pos)

final class Scenarios[I <: Instrument] private[scenario] (val instrument: I):

  private val instrumentId = instrument.identity.id

  type D        = instrument.roles.position.D
  type B        = instrument.roles.base.D
  type Q        = instrument.roles.quote.D
  type Lots     = instrument.Lots
  type Price    = instrument.Price
  type Market   = instrument.MarketState
  type Position = instrument.PositionLots

  def slice(
    lots: Lots,
    market: Market,
    role: LiquidityRole
  ): Either[ScenarioError, LiquiditySlice[Lots, Market]] =
    LiquiditySlice.create(instrument)(lots, market, role)

  def assumptions[O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlices: MatchedSlices[Lots, Market]
  ): ScenarioAssumptions[D, B, Q, Market] =
    ScenarioAssumptions.create(order)(activationEvidence, pricingResolution, matchedSlices)

  def assumptionsOne[O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlice: LiquiditySlice[Lots, Market]
  ): ScenarioAssumptions[D, B, Q, Market] =
    ScenarioAssumptions.one(order)(activationEvidence, pricingResolution, matchedSlice)

  def assumptionsMany[O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    head: LiquiditySlice[Lots, Market],
    tail: LiquiditySlice[Lots, Market]*
  ): ScenarioAssumptions[D, B, Q, Market] =
    ScenarioAssumptions.many(order)(activationEvidence, pricingResolution, head, tail*)

  def assumptionsFromVector[O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlices: Vector[LiquiditySlice[Lots, Market]]
  ): Either[InvalidScenarioDiagnostics, ScenarioAssumptions[D, B, Q, Market]] =
    ScenarioAssumptions
      .fromVector(order)(activationEvidence, pricingResolution, matchedSlices)
      .left
      .map(violation => InvalidScenarioDiagnostics(violation, Vector.empty))

  /** Existing deterministic fail-fast complete-scenario boundary. */
  def order(
    order: Order[D, B, Q],
    assumptions: ScenarioAssumptions[D, B, Q, Market]
  ): Either[ScenarioError, OrderScenario[D, B, Q, Market, Position]] =
    diagnose(order, assumptions)
      .left
      .map(error => ScenarioViolationMapping.scenario(error.head))

  /** Accumulating diagnostic boundary derived from the same ordered validation stages. */
  def diagnose(
    order: Order[D, B, Q],
    assumptions: ScenarioAssumptions[D, B, Q, Market]
  ): Either[InvalidScenarioDiagnostics, OrderScenario[D, B, Q, Market, Position]] =
    val evaluated =
      for
        _                <- validateIdentities(order, assumptions)
        _                <- validateSliceTotals(order.intent.lots, assumptions.matchedSlices)
        _                <- validateTarget(order, assumptions)
        _                <- validateActivation(assumptions)
        effectivePricing <- validatePricing(assumptions)
        _                <- validateSlices(order, assumptions.matchedSlices, effectivePricing)
      yield
        val coordinate = order.intent.side.sign * order.intent.lots.count.unrefined
        val change     = PositionLots.fromCoordinate(instrument)(coordinate)
        OrderScenario(instrumentId, order, assumptions, effectivePricing, change)

    evaluated.left.map(violations => InvalidScenarioDiagnostics(violations.head, violations.tail))
  end diagnose

  def roundTrip(
    entry: OrderScenario[D, B, Q, Market, Position],
    exit: OrderScenario[D, B, Q, Market, Position]
  ): Either[ScenarioError, RoundTripScenario[D, B, Q, Market, Position]] =
    for
      _ <- ScenarioIdentityChecks.check(
             "roundTrip",
             instrumentId,
             "entry"                -> entry.instrumentId,
             "entry.positionChange" -> entry.positionChange.instrumentId,
             "exit"                 -> exit.instrumentId,
             "exit.positionChange"  -> exit.positionChange.instrumentId
           )
      result <-
        val entryCount = entry.positionChange.coordinate
        val exitCount  = exit.positionChange.coordinate
        if entryCount + exitCount != 0 then Left(InvalidRoundTrip(entryCount, exitCount))
        else Right(RoundTripScenario(instrumentId, entry, exit, entry.positionChange))
    yield result

  private def validateIdentities(
    order: Order[D, B, Q],
    assumptions: ScenarioAssumptions[D, B, Q, Market]
  ): Either[Vector[ScenarioViolation], Unit] =
    val supplied =
      Vector(
        "order"             -> order.instrumentId,
        "order.intent"      -> order.intent.instrumentId,
        "order.intent.lots" -> order.intent.lots.instrumentId,
        "assumptions.order" -> assumptions.order.instrumentId
      ) ++
        assumptions.order.activation
          .observations(assumptions.activationEvidence)
          .map((name, value) => name -> value.instrumentId) ++
        assumptions.order.execution
          .observations(assumptions.pricingResolution)
          .map((name, value) => name -> value.instrumentId) ++
        assumptions.matchedSlices.toVector.zipWithIndex.flatMap: (slice, index) =>
          Vector(
            s"slices[$index]"        -> slice.instrumentId,
            s"slices[$index].lots"   -> slice.lots.instrumentId,
            s"slices[$index].market" -> slice.market.instrumentId,
            s"slices[$index].price"  -> slice.market.price.instrumentId
          )

    val accumulated = Validation.indexed(supplied): (candidate, ordinal) =>
      val (name, id) = candidate
      Validation.ensure(ordinal, id == instrumentId)(
        ScenarioViolation.Identity(s"scenario.$name", instrumentId, id)
      )
    Validation.ordered(accumulated)
  end validateIdentities

  private def validateSliceTotals(
    expected: Lots,
    slices: MatchedSlices[Lots, Market]
  ): Either[Vector[ScenarioViolation], Unit] =
    val supplied = slices.toVector.foldLeft(BigInt(0))((total, slice) => total + slice.lots.count.unrefined)
    Validation.ordered(
      Validation.ensure(0, supplied == expected.count.unrefined)(
        ScenarioViolation.LotTotal(expected.count.unrefined, supplied)
      )
    )

  private def validateTarget(
    order: Order[D, B, Q],
    assumptions: ScenarioAssumptions[D, B, Q, Market]
  ): Either[Vector[ScenarioViolation], Unit] =
    Validation.ordered(
      Validation.ensure(0, assumptions.order.eq(order))(ScenarioViolation.OrderTargetMismatch)
    )

  private def validateActivation(
    assumptions: ScenarioAssumptions[D, B, Q, Market]
  ): Either[Vector[ScenarioViolation], CheckedActivation[B, Q]] =
    assumptions.order.activation
      .verify(assumptions.activationEvidence)
      .left
      .map(cause => Vector(ScenarioViolation.Activation(cause)))

  private def validatePricing(
    assumptions: ScenarioAssumptions[D, B, Q, Market]
  ): Either[Vector[ScenarioViolation], EffectivePricing[B, Q]] =
    assumptions.order.execution
      .resolve(assumptions.pricingResolution)
      .left
      .map(cause => Vector(ScenarioViolation.Pricing(cause)))

  private def validateSlices(
    order: Order[D, B, Q],
    slices: MatchedSlices[Lots, Market],
    effectivePricing: EffectivePricing[B, Q]
  ): Either[Vector[ScenarioViolation], Unit] =
    val accumulated = Validation.indexed(slices.toVector): (slice, index) =>
      val marketRole = Validation.ensure(
        index * 3,
        effectivePricing match
          case EffectivePricing.Market() => slice.role == LiquidityRole.Taker
          case _                         => true
      )(ScenarioViolation.Slice(index, ScenarioFailureReason.MarketSliceNotTaker))
      val makerRole = Validation.ensure(
        index * 3 + 1,
        !order.execution.requiresMaker || slice.role == LiquidityRole.Maker
      )(ScenarioViolation.Slice(index, ScenarioFailureReason.MakerOnlySliceNotMaker))
      val priceQuality = Validation.ensure(
        index * 3 + 2,
        effectivePricing match
          case EffectivePricing.Market()       => true
          case EffectivePricing.Limited(limit) =>
            order.intent.side match
              case Side.Buy  => slice.market.price.ticks.unrefined <= limit.ticks.unrefined
              case Side.Sell => slice.market.price.ticks.unrefined >= limit.ticks.unrefined
      )(ScenarioViolation.Slice(index, ScenarioFailureReason.SliceWorseThanLimit))
      (marketRole, makerRole, priceQuality).mapN((_, _, _) => ())

    Validation.ordered(accumulated)
  end validateSlices

end Scenarios

object Scenarios:
  def apply[I <: Instrument](instrument: I): Scenarios[instrument.type] =
    new Scenarios[instrument.type](instrument)
