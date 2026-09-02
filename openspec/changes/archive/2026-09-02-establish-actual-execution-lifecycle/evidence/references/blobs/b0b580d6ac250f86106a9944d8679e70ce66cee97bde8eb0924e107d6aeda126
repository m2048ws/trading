package trading.execution

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

import trading.economics.instrument.Lots
import trading.economics.instrument.PositionLots
import trading.economics.instrument.Price
import trading.order.Side
import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported

enum ModifierAmbiguityKind extends JavaSerializationUnsupported:
  case ExplicitlyUnsequencedModifier
  case MultipleAuthoritativeStreams
  case ConflictingAuthoritativePosition
  case ModifierAfterBust

@nowarn("msg=Ignoring.*qualifier")
final class ModifierAmbiguity private[this] (private val values: Vector[ModifierAmbiguityKind])
  extends JavaSerializationUnsupported:

  def head: ModifierAmbiguityKind             = values.head
  def toVector: Vector[ModifierAmbiguityKind] = values
  def size: Int                               = values.size

  override def equals(other: Any): Boolean = other match
    case that: ModifierAmbiguity => values == that.toVector
    case _                       => false
  override def hashCode(): Int = values.hashCode

object ModifierAmbiguity:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ModifierAmbiguity], MethodHandles.lookup())
      .findConstructor(classOf[ModifierAmbiguity], MethodType.methodType(classOf[Unit], classOf[Vector[?]]))

  private def construct(values: Vector[ModifierAmbiguityKind]): ModifierAmbiguity =
    constructor.invoke(values).asInstanceOf[ModifierAmbiguity]

  private[execution] def from(values: Vector[ModifierAmbiguityKind]): Option[ModifierAmbiguity] =
    Option.when(values.nonEmpty)(construct(values.distinct))

sealed abstract class EffectiveFill[D <: Dim, B <: Dim, Q <: Dim] protected () extends JavaSerializationUnsupported:
  EffectiveFill.requireBuiltin(this)
  def original: ExecutionFill[D, B, Q]
  def modifiers: Vector[FillModifier[D, B, Q]]

@nowarn("msg=Ignoring.*qualifier")
final class ActiveEffectiveFill[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val original: ExecutionFill[D, B, Q],
  val effectiveLots: Lots[D],
  val effectivePrice: Price[B, Q],
  val modifiers: Vector[FillModifier[D, B, Q]])
  extends EffectiveFill[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: ActiveEffectiveFill[?, ?, ?] =>
      original == that.original && effectiveLots == that.effectiveLots &&
      effectivePrice == that.effectivePrice && modifiers == that.modifiers
    case _ => false
  override def hashCode(): Int = (original, effectiveLots, effectivePrice, modifiers).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class BustedEffectiveFill[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val original: ExecutionFill[D, B, Q],
  val bust: FillBusted[D, B, Q],
  val modifiers: Vector[FillModifier[D, B, Q]])
  extends EffectiveFill[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: BustedEffectiveFill[?, ?, ?] =>
      original == that.original && bust == that.bust && modifiers == that.modifiers
    case _ => false
  override def hashCode(): Int = (original, bust, modifiers).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class AmbiguousEffectiveFill[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val original: ExecutionFill[D, B, Q],
  val modifiers: Vector[FillModifier[D, B, Q]],
  val ambiguity: ModifierAmbiguity)
  extends EffectiveFill[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: AmbiguousEffectiveFill[?, ?, ?] =>
      original == that.original && modifiers == that.modifiers && ambiguity == that.ambiguity
    case _ => false
  override def hashCode(): Int = (original, modifiers, ambiguity).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class ConflictingEffectiveFill[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val original: ExecutionFill[D, B, Q],
  val modifiers: Vector[FillModifier[D, B, Q]],
  val eventConflicts: Vector[SourceFactConflict[D, B, Q]],
  val identityConflicts: Vector[FillIdentityConflict[D, B, Q]])
  extends EffectiveFill[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: ConflictingEffectiveFill[?, ?, ?] =>
      original == that.original && modifiers == that.modifiers &&
      eventConflicts == that.eventConflicts && identityConflicts == that.identityConflicts
    case _ => false
  override def hashCode(): Int = (original, modifiers, eventConflicts, identityConflicts).hashCode

object EffectiveFill:
  private[execution] def requireBuiltin(value: EffectiveFill[?, ?, ?]): Unit =
    val runtimeClass = value.getClass
    val supported    =
      runtimeClass == classOf[ActiveEffectiveFill[?, ?, ?]] ||
        runtimeClass == classOf[BustedEffectiveFill[?, ?, ?]] ||
        runtimeClass == classOf[AmbiguousEffectiveFill[?, ?, ?]] ||
        runtimeClass == classOf[ConflictingEffectiveFill[?, ?, ?]]
    if !supported then
      throw new IllegalAccessError(s"unsupported EffectiveFill implementation: ${runtimeClass.getName}")

final case class OverfillAnomaly[D <: Dim](
  orderedLots: Lots[D],
  effectiveExposure: PositionLots[D],
  excessExposure: PositionLots[D])
  extends JavaSerializationUnsupported

@nowarn("msg=Ignoring.*qualifier")
final class EffectiveFillLedger[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val byFillId: Map[QualifiedFillId, EffectiveFill[D, B, Q]],
  val knownExposure: PositionLots[D],
  val overfill: Option[OverfillAnomaly[D]],
  val unresolvedReferences: Map[QualifiedFillId, Vector[UnresolvedFillReference[D, B, Q]]])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: EffectiveFillLedger[?, ?, ?] =>
      byFillId == that.byFillId && knownExposure == that.knownExposure &&
      overfill == that.overfill && unresolvedReferences == that.unresolvedReferences
    case _ => false
  override def hashCode(): Int = (byFillId, knownExposure, overfill, unresolvedReferences).hashCode

object EffectiveFillLedger:
  private val activeConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ActiveEffectiveFill[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[ActiveEffectiveFill[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[ExecutionFill[?, ?, ?]],
          classOf[Lots[?]],
          classOf[Price[?, ?]],
          classOf[Vector[?]]
        )
      )

  private val bustedConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[BustedEffectiveFill[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[BustedEffectiveFill[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[ExecutionFill[?, ?, ?]],
          classOf[FillBusted[?, ?, ?]],
          classOf[Vector[?]]
        )
      )

  private val ambiguousConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[AmbiguousEffectiveFill[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[AmbiguousEffectiveFill[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[ExecutionFill[?, ?, ?]],
          classOf[Vector[?]],
          classOf[ModifierAmbiguity]
        )
      )

  private val conflictingConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ConflictingEffectiveFill[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[ConflictingEffectiveFill[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[ExecutionFill[?, ?, ?]],
          classOf[Vector[?]],
          classOf[Vector[?]],
          classOf[Vector[?]]
        )
      )

  private val ledgerConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[EffectiveFillLedger[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[EffectiveFillLedger[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[Map[?, ?]],
          classOf[PositionLots[?]],
          classOf[Option[?]],
          classOf[Map[?, ?]]
        )
      )

  private def active[D <: Dim, B <: Dim, Q <: Dim](
    original: ExecutionFill[D, B, Q],
    lots: Lots[D],
    price: Price[B, Q],
    modifiers: Vector[FillModifier[D, B, Q]]
  ): ActiveEffectiveFill[D, B, Q] =
    activeConstructor.invoke(original, lots, price, modifiers).asInstanceOf[ActiveEffectiveFill[D, B, Q]]

  private def busted[D <: Dim, B <: Dim, Q <: Dim](
    original: ExecutionFill[D, B, Q],
    bust: FillBusted[D, B, Q],
    modifiers: Vector[FillModifier[D, B, Q]]
  ): BustedEffectiveFill[D, B, Q] =
    bustedConstructor.invoke(original, bust, modifiers).asInstanceOf[BustedEffectiveFill[D, B, Q]]

  private def ambiguous[D <: Dim, B <: Dim, Q <: Dim](
    original: ExecutionFill[D, B, Q],
    modifiers: Vector[FillModifier[D, B, Q]],
    ambiguity: ModifierAmbiguity
  ): AmbiguousEffectiveFill[D, B, Q] =
    ambiguousConstructor
      .invoke(original, modifiers, ambiguity)
      .asInstanceOf[AmbiguousEffectiveFill[D, B, Q]]

  private def conflicting[D <: Dim, B <: Dim, Q <: Dim](
    original: ExecutionFill[D, B, Q],
    modifiers: Vector[FillModifier[D, B, Q]],
    eventConflicts: Vector[SourceFactConflict[D, B, Q]],
    identityConflicts: Vector[FillIdentityConflict[D, B, Q]]
  ): ConflictingEffectiveFill[D, B, Q] =
    conflictingConstructor
      .invoke(original, modifiers, eventConflicts, identityConflicts)
      .asInstanceOf[ConflictingEffectiveFill[D, B, Q]]

  private[execution] def derive[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q]
  ): EffectiveFillLedger[D, B, Q] =
    val allFacts =
      (state.source.factsByEvent.values.toVector ++
        state.source.eventConflicts.flatMap(conflict => Vector(conflict.original, conflict.conflicting))).distinct
    val modifiers = allFacts.collect:
      case value: FillModifier[D, B, Q] => value
    val modifiersByFill      = modifiers.groupBy(_.referencedFillId)
    val eventConflictsByFill = state.source.eventConflicts
      .flatMap: conflict =>
        (referencedFillIds(conflict.original) ++ referencedFillIds(conflict.conflicting)).distinct.map(_ -> conflict)
      .groupMap(_._1)(_._2)
    val identityConflictsByFill = state.source.fillConflicts.groupBy(_.fillId)

    val effective = state.source.fillsById.map: (fillId, original) =>
      val targetModifiers = modifiersByFill.getOrElse(fillId, Vector.empty).sortBy(modifierKey)
      val eventConflicts  = eventConflictsByFill
        .getOrElse(fillId, Vector.empty)
        .distinct
        .sortBy(conflictKey)
      val identityConflicts = identityConflictsByFill
        .getOrElse(fillId, Vector.empty)
        .sortBy(fillConflictKey)
      val value =
        if eventConflicts.nonEmpty || identityConflicts.nonEmpty then
          conflicting(original, targetModifiers, eventConflicts, identityConflicts)
        else resolve(state, original, targetModifiers)
      fillId -> value

    val activeLots = effective.values.collect:
      case value: ActiveEffectiveFill[D, B, Q] => value.effectiveLots.count.unrefined
    .sum
    val exposure = position(state.lifecycle, activeLots)
    val excess   = activeLots - state.lifecycle.orderedLots.count.unrefined
    val overfill = Option.when(excess > 0):
      OverfillAnomaly(state.lifecycle.orderedLots, exposure, position(state.lifecycle, excess))

    ledgerConstructor
      .invoke(effective, exposure, overfill, state.source.unresolvedFillReferences)
      .asInstanceOf[EffectiveFillLedger[D, B, Q]]
  end derive

  private def resolve[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q],
    original: ExecutionFill[D, B, Q],
    modifiers: Vector[FillModifier[D, B, Q]]
  ): EffectiveFill[D, B, Q] =
    if modifiers.isEmpty then active(original, original.lots, original.price, Vector.empty)
    else
      val ordered             = modifiers.flatMap(modifier => modifier.authoritativePosition.map(_ -> modifier))
      val streams             = ordered.map(_._1.stream).distinct
      val conflictingPosition = ordered.exists: (position, _) =>
        state.source.positionConflicts.contains(position)
      val sorted = ordered.sortBy: (position, modifier) =>
        (streamKey(position.stream), position.sequence.value, modifierKey(modifier))
      .map(_._2)
      val firstBust = sorted.indexWhere(_.isInstanceOf[FillBusted[?, ?, ?]])
      val ambiguity = ModifierAmbiguity.from(
        Vector(
          Option.when(ordered.size != modifiers.size)(ModifierAmbiguityKind.ExplicitlyUnsequencedModifier),
          Option.when(streams.size > 1)(ModifierAmbiguityKind.MultipleAuthoritativeStreams),
          Option.when(conflictingPosition)(ModifierAmbiguityKind.ConflictingAuthoritativePosition),
          Option.when(firstBust >= 0 && firstBust != sorted.size - 1)(ModifierAmbiguityKind.ModifierAfterBust)
        ).flatten
      )
      ambiguity match
        case Some(value) => ambiguous(original, modifiers, value)
        case None        =>
          sorted.last match
            case value: FillBusted[D, B, Q]    => busted(original, value, sorted)
            case value: FillCorrected[D, B, Q] =>
              active(original, value.replacementLots, value.replacementPrice, sorted)
  end resolve

  private def referencedFillIds(value: SourceFact[?, ?, ?]): Vector[QualifiedFillId] = value match
    case fill: ExecutionFill[?, ?, ?]    => Vector(fill.fillId)
    case modifier: FillModifier[?, ?, ?] => Vector(modifier.referencedFillId)
    case _                               => Vector.empty

  private def modifierKey(value: FillModifier[?, ?, ?]): String =
    val position = value.authoritativePosition match
      case None        => "0-unsequenced"
      case Some(value) => s"1-${streamKey(value.stream)}-${value.sequence.value}"
    val body = value match
      case correction: FillCorrected[?, ?, ?] =>
        s"correction-${correction.replacementLots.count.unrefined}-${correction.replacementPrice.coefficient}"
      case _: FillBusted[?, ?, ?] => "bust"
    s"$position-${eventKey(value.eventId)}-$body"

  private def conflictKey(value: SourceFactConflict[?, ?, ?]): String =
    s"${sourceFactKey(value.original)}-${sourceFactKey(value.conflicting)}"

  private def fillConflictKey(value: FillIdentityConflict[?, ?, ?]): String =
    s"${sourceFactKey(value.original)}-${sourceFactKey(value.conflicting)}"

  private def sourceFactKey(value: SourceFact[?, ?, ?]): String =
    val body = value match
      case _: OrderAccepted[?, ?, ?]    => "accepted"
      case _: OrderRejected[?, ?, ?]    => "rejected"
      case fill: ExecutionFill[?, ?, ?] =>
        s"fill-${fillKey(fill.fillId)}-${fill.lots.count.unrefined}-${fill.price.coefficient}"
      case correction: FillCorrected[?, ?, ?] =>
        s"correction-${fillKey(correction.referencedFillId)}-${correction.replacementLots.count.unrefined}-${correction.replacementPrice.coefficient}"
      case bust: FillBusted[?, ?, ?]                     => s"bust-${fillKey(bust.referencedFillId)}"
      case _: CancellationEffective[?, ?, ?]             => "cancelled"
      case checkpoint: ReconciliationCheckpoint[?, ?, ?] =>
        s"checkpoint-${streamKey(checkpoint.checkpoint.position.stream)}-${checkpoint.checkpoint.position.sequence.value}"
      case complete: SourceOrderCompleted[?, ?, ?] =>
        s"complete-${streamKey(complete.completeness.completeThrough.stream)}-${complete.completeness.completeThrough.sequence.value}"
      case absent: SourceOrderAbsent[?, ?, ?] =>
        s"absent-${streamKey(absent.completeness.completeThrough.stream)}-${absent.completeness.completeThrough.sequence.value}"
    s"${eventKey(value.eventId)}-${value.executionOrderId.value}-$body-${modifierOrderingKey(value.ordering)}"

  private def modifierOrderingKey(value: SourceOrdering): String = value match
    case ExplicitlyUnsequenced               => "unsequenced"
    case sequenced: AuthoritativelySequenced =>
      s"${streamKey(sequenced.position.stream)}-${sequenced.position.sequence.value}"

  private def eventKey(value: QualifiedSourceEventId): String =
    s"${value.target.source.value}-${value.target.account.value}-${value.native.value}"

  private def fillKey(value: QualifiedFillId): String =
    s"${value.target.source.value}-${value.target.account.value}-${value.native.value}"

  private def streamKey(value: QualifiedSourceStreamId): String =
    s"${value.target.source.value}-${value.target.account.value}-${value.native.value}"

  private def position[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q],
    count: BigInt
  ): PositionLots[D] =
    val coordinate = lifecycle.order.intent.side match
      case Side.Buy  => count
      case Side.Sell => -count
    PositionLots
      .fromCoordinate(lifecycle.instrument)(coordinate)
      .asInstanceOf[PositionLots[D]]
end EffectiveFillLedger
