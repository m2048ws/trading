package trading.order

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.PositiveWhole

/** Order direction and the corresponding account-position sign. */
enum Side:
  case Buy, Sell

  def sign: BigInt =
    this match
      case Buy  => BigInt(1)
      case Sell => BigInt(-1)

/** Duration instruction retained by a priced immutable order. */
enum TimeInForce:
  case GoodTillCancelled, ImmediateOrCancel, FillOrKill, Day

/** The only durations structurally accepted by market execution. */
enum NonRestingTimeInForce:
  case ImmediateOrCancel, FillOrKill

object NonRestingTimeInForce:
  def from(value: TimeInForce): Either[OrderViolation, NonRestingTimeInForce] =
    value match
      case TimeInForce.ImmediateOrCancel => Right(NonRestingTimeInForce.ImmediateOrCancel)
      case TimeInForce.FillOrKill        => Right(NonRestingTimeInForce.FillOrKill)
      case supplied                      => Left(OrderViolation.RestingMarketDuration(supplied))

/** Whether a priced order may take liquidity or must remain passive. */
enum LiquidityConstraint:
  case Unrestricted, MakerOnly

/** Whether an order may open exposure or may only reduce it. */
enum PositionEffect:
  case Unrestricted, ReduceOnly

/** Price source named by a trigger or peg. */
enum PriceReference:
  case Last, Mark, Index

/** Exact comparison used by fixed and trailing activation. */
enum TriggerComparison:
  case AtOrAbove, AtOrBelow

@nowarn("msg=Ignoring.*qualifier")
final class CheckedActivation[B <: Dim, Q <: Dim] private[this] (
  val observations: Vector[(ActivationObservation, Price[B, Q])]):

  override def equals(other: Any): Boolean =
    other match
      case that: CheckedActivation[?, ?] => observations == that.observations
      case _                             => false

  override def hashCode: Int = observations.hashCode

  override def toString: String = s"CheckedActivation($observations)"
end CheckedActivation

object CheckedActivation:
  private val constructor =
    val owner = classOf[CheckedActivation[?, ?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(owner, MethodType.methodType(java.lang.Void.TYPE, classOf[Vector[?]]))

  private def construct[B <: Dim, Q <: Dim](
    observations: Vector[(ActivationObservation, Price[B, Q])]
  ): CheckedActivation[B, Q] =
    constructor.invoke(observations).asInstanceOf[CheckedActivation[B, Q]]

  private[order] def immediate[B <: Dim, Q <: Dim]: CheckedActivation[B, Q] =
    construct(Vector.empty)

  private[order] def fixed[B <: Dim, Q <: Dim](
    activation: FixedActivation[B, Q],
    evidence: FixedTriggerEvidence[B, Q]
  ): Either[ActivationViolation, CheckedActivation[B, Q]] =
    if
      evidence.reference != activation.reference || evidence.comparison != activation.comparison ||
      evidence.triggerPrice != activation.triggerPrice
    then Left(ActivationViolation.FixedEvidenceMismatch)
    else
      Right(
        construct(Vector(ActivationObservation.Observed -> evidence.observedPrice))
      )

  private[order] def trailing[B <: Dim, Q <: Dim](
    activation: TrailingActivation[B, Q],
    evidence: TrailingTriggerEvidence[B, Q]
  ): Either[ActivationViolation, CheckedActivation[B, Q]] =
    if
      evidence.reference != activation.reference || evidence.comparison != activation.comparison ||
      evidence.offsetTicks != activation.offsetTicks
    then Left(ActivationViolation.TrailingEvidenceMismatch)
    else
      Right(
        construct(
          Vector(
            ActivationObservation.FavorableExtreme -> evidence.favorableExtreme,
            ActivationObservation.Observed         -> evidence.observedPrice
          )
        )
      )
end CheckedActivation

enum ActivationObservation:
  case Observed, FavorableExtreme

enum PricingObservation:
  case ReferencePrice, ResolvedLimit

sealed abstract class OrderActivation[B <: Dim, Q <: Dim] protected ():
  OrderActivation.requireBuiltin(this)

  type Evidence

  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]]
  private[trading] def acceptsEvidence(evidence: Any): Boolean
  private[trading] def observations(evidence: Evidence): Vector[(ActivationObservation, Price[B, Q])]

sealed abstract class TriggerActivation[B <: Dim, Q <: Dim] protected () extends OrderActivation[B, Q]()

final class ImmediateActivation[B <: Dim, Q <: Dim] private extends OrderActivation[B, Q]():
  type Evidence = ImmediateActivation.Evidence.type

  val evidence: Evidence = ImmediateActivation.Evidence

  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]] =
    Right(CheckedActivation.immediate)

  private[trading] def acceptsEvidence(evidence: Any): Boolean =
    evidence.asInstanceOf[AnyRef].eq(ImmediateActivation.Evidence)

  private[trading] def observations(
    evidence: Evidence
  ): Vector[(ActivationObservation, Price[B, Q])] = Vector.empty

object ImmediateActivation:
  case object Evidence

  def apply[B <: Dim, Q <: Dim](): ImmediateActivation[B, Q] =
    new ImmediateActivation[B, Q]

final case class FixedActivation[B <: Dim, Q <: Dim](
  reference: PriceReference,
  comparison: TriggerComparison,
  triggerPrice: Price[B, Q])
  extends TriggerActivation[B, Q]():

  type Evidence = FixedTriggerEvidence[B, Q]

  def evidence(observedPrice: Price[B, Q]): Either[ActivationViolation, Evidence] =
    FixedTriggerEvidence.create(this, observedPrice)

  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]] =
    CheckedActivation.fixed(this, evidence)

  private[trading] def acceptsEvidence(evidence: Any): Boolean =
    evidence.isInstanceOf[FixedTriggerEvidence[?, ?]]

  private[trading] def observations(
    evidence: Evidence
  ): Vector[(ActivationObservation, Price[B, Q])] =
    Vector(ActivationObservation.Observed -> evidence.observedPrice)
end FixedActivation

final case class TrailingActivation[B <: Dim, Q <: Dim] private (
  reference: PriceReference,
  comparison: TriggerComparison,
  offsetTicks: PositiveWhole)
  extends TriggerActivation[B, Q]():

  type Evidence = TrailingTriggerEvidence[B, Q]

  def evidence(
    favorableExtreme: Price[B, Q],
    observedPrice: Price[B, Q]
  ): Either[ActivationViolation, Evidence] =
    TrailingTriggerEvidence.create(this, favorableExtreme, observedPrice)

  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]] =
    CheckedActivation.trailing(this, evidence)

  private[trading] def acceptsEvidence(evidence: Any): Boolean =
    evidence.isInstanceOf[TrailingTriggerEvidence[?, ?]]

  private[trading] def observations(
    evidence: Evidence
  ): Vector[(ActivationObservation, Price[B, Q])] =
    Vector(
      ActivationObservation.FavorableExtreme -> evidence.favorableExtreme,
      ActivationObservation.Observed         -> evidence.observedPrice
    )
end TrailingActivation

object TrailingActivation:
  def create[B <: Dim, Q <: Dim](
    reference: PriceReference,
    comparison: TriggerComparison,
    offsetTicks: BigInt
  ): Either[OrderViolation, TrailingActivation[B, Q]] =
    PositiveWhole(offsetTicks)
      .left
      .map(_ => OrderViolation.InvalidTrailingOffset(offsetTicks))
      .map(new TrailingActivation(reference, comparison, _))

object OrderActivation:
  private[order] def requireBuiltin(value: OrderActivation[?, ?]): Unit =
    val runtimeClass = value.getClass
    val supported    =
      runtimeClass == classOf[ImmediateActivation[?, ?]] ||
        runtimeClass == classOf[FixedActivation[?, ?]] ||
        runtimeClass == classOf[TrailingActivation[?, ?]]
    if !supported then
      throw new IllegalAccessError(s"unsupported OrderActivation implementation: ${runtimeClass.getName}")

  private[order] def comparisonSatisfied(
    comparison: TriggerComparison,
    observed: BigInt,
    threshold: BigInt
  ): Boolean =
    comparison match
      case TriggerComparison.AtOrAbove => observed >= threshold
      case TriggerComparison.AtOrBelow => observed <= threshold

@nowarn("msg=Ignoring.*qualifier")
final class FixedTriggerEvidence[B <: Dim, Q <: Dim] private[this] (
  val reference: PriceReference,
  private[order] val comparison: TriggerComparison,
  private[order] val triggerPrice: Price[B, Q],
  val observedPrice: Price[B, Q])

object FixedTriggerEvidence:
  private val constructor =
    val owner = classOf[FixedTriggerEvidence[?, ?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[PriceReference],
          classOf[TriggerComparison],
          classOf[Price[?, ?]],
          classOf[Price[?, ?]]
        )
      )

  private def construct[B <: Dim, Q <: Dim](
    activation: FixedActivation[B, Q],
    observedPrice: Price[B, Q]
  ): FixedTriggerEvidence[B, Q] =
    constructor
      .invoke(
        activation.reference,
        activation.comparison,
        activation.triggerPrice,
        observedPrice
      )
      .asInstanceOf[FixedTriggerEvidence[B, Q]]

  private[order] def create[B <: Dim, Q <: Dim](
    activation: FixedActivation[B, Q],
    observedPrice: Price[B, Q]
  ): Either[ActivationViolation, FixedTriggerEvidence[B, Q]] =
    if
      OrderActivation.comparisonSatisfied(
        activation.comparison,
        observedPrice.ticks.unrefined,
        activation.triggerPrice.ticks.unrefined
      )
    then
      Right(construct(activation, observedPrice))
    else Left(ActivationViolation.FixedTriggerUnsatisfied)
end FixedTriggerEvidence

@nowarn("msg=Ignoring.*qualifier")
final class TrailingTriggerEvidence[B <: Dim, Q <: Dim] private[this] (
  val reference: PriceReference,
  private[order] val comparison: TriggerComparison,
  private[order] val offsetTicks: PositiveWhole,
  val favorableExtreme: Price[B, Q],
  val observedPrice: Price[B, Q])

object TrailingTriggerEvidence:
  private val constructor =
    val owner = classOf[TrailingTriggerEvidence[?, ?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[PriceReference],
          classOf[TriggerComparison],
          classOf[BigInt],
          classOf[Price[?, ?]],
          classOf[Price[?, ?]]
        )
      )

  private def construct[B <: Dim, Q <: Dim](
    activation: TrailingActivation[B, Q],
    favorableExtreme: Price[B, Q],
    observedPrice: Price[B, Q]
  ): TrailingTriggerEvidence[B, Q] =
    constructor
      .invoke(
        activation.reference,
        activation.comparison,
        activation.offsetTicks.unrefined,
        favorableExtreme,
        observedPrice
      )
      .asInstanceOf[TrailingTriggerEvidence[B, Q]]

  private[order] def create[B <: Dim, Q <: Dim](
    activation: TrailingActivation[B, Q],
    favorableExtreme: Price[B, Q],
    observedPrice: Price[B, Q]
  ): Either[ActivationViolation, TrailingTriggerEvidence[B, Q]] =
    val extreme   = favorableExtreme.ticks.unrefined
    val threshold = activation.comparison match
      case TriggerComparison.AtOrAbove => extreme + activation.offsetTicks.unrefined
      case TriggerComparison.AtOrBelow => extreme - activation.offsetTicks.unrefined
    if threshold.signum <= 0 then Left(ActivationViolation.TrailingThresholdNonPositive)
    else if
      OrderActivation.comparisonSatisfied(
        activation.comparison,
        observedPrice.ticks.unrefined,
        threshold
      )
    then
      Right(construct(activation, favorableExtreme, observedPrice))
    else Left(ActivationViolation.TrailingTriggerUnsatisfied)
end TrailingTriggerEvidence

sealed trait EffectivePricing[B <: Dim, Q <: Dim]

object EffectivePricing:
  final case class Market[B <: Dim, Q <: Dim]()                    extends EffectivePricing[B, Q]
  final case class Limited[B <: Dim, Q <: Dim](price: Price[B, Q]) extends EffectivePricing[B, Q]

case object DirectPricingResolution

@nowarn("msg=Ignoring.*qualifier")
final class PegResolution[B <: Dim, Q <: Dim] private[this] (
  val reference: PriceReference,
  private[order] val offsetTicks: BigInt,
  val referencePrice: Price[B, Q],
  val resolvedLimit: Price[B, Q])

object PegResolution:
  private val constructor =
    val owner = classOf[PegResolution[?, ?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[PriceReference],
          classOf[BigInt],
          classOf[Price[?, ?]],
          classOf[Price[?, ?]]
        )
      )

  private def construct[B <: Dim, Q <: Dim](
    pricing: PeggedPricing[B, Q],
    referencePrice: Price[B, Q],
    resolvedLimit: Price[B, Q]
  ): PegResolution[B, Q] =
    constructor
      .invoke(pricing.reference, pricing.offsetTicks, referencePrice, resolvedLimit)
      .asInstanceOf[PegResolution[B, Q]]

  private[order] def create[B <: Dim, Q <: Dim](
    pricing: PeggedPricing[B, Q],
    referencePrice: Price[B, Q],
    resolvedLimit: Price[B, Q]
  ): Either[PricingViolation, PegResolution[B, Q]] =
    val suppliedOffset = resolvedLimit.ticks.unrefined - referencePrice.ticks.unrefined
    if suppliedOffset == pricing.offsetTicks then
      Right(construct(pricing, referencePrice, resolvedLimit))
    else Left(PricingViolation.PegOffsetMismatch(pricing.offsetTicks, suppliedOffset))
end PegResolution

sealed abstract class OrderPricing[B <: Dim, Q <: Dim] protected ():
  OrderPricing.requireBuiltin(this)

  type Resolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]]
  private[trading] def acceptsResolution(resolution: Any): Boolean
  private[trading] def observations(resolution: Resolution): Vector[(PricingObservation, Price[B, Q])]

object OrderPricing:
  private[order] def requireBuiltin(value: OrderPricing[?, ?]): Unit =
    val runtimeClass = value.getClass
    val supported    =
      runtimeClass == classOf[LimitPricing[?, ?]] || runtimeClass == classOf[PeggedPricing[?, ?]]
    if !supported then
      throw new IllegalAccessError(s"unsupported OrderPricing implementation: ${runtimeClass.getName}")

final case class LimitPricing[B <: Dim, Q <: Dim](limit: Price[B, Q]) extends OrderPricing[B, Q]():
  type Resolution = DirectPricingResolution.type

  val resolution: Resolution = DirectPricingResolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]] =
    Right(EffectivePricing.Limited(limit))

  private[trading] def acceptsResolution(resolution: Any): Boolean =
    resolution.asInstanceOf[AnyRef].eq(DirectPricingResolution)

  private[trading] def observations(
    resolution: Resolution
  ): Vector[(PricingObservation, Price[B, Q])] = Vector.empty

final case class PeggedPricing[B <: Dim, Q <: Dim](reference: PriceReference, offsetTicks: BigInt)
  extends OrderPricing[B, Q]():
  type Resolution = PegResolution[B, Q]

  def resolution(
    referencePrice: Price[B, Q],
    resolvedLimit: Price[B, Q]
  ): Either[PricingViolation, Resolution] =
    PegResolution.create(this, referencePrice, resolvedLimit)

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]] =
    if resolution.reference != reference || resolution.offsetTicks != offsetTicks then
      Left(PricingViolation.PegResolutionMismatch)
    else Right(EffectivePricing.Limited(resolution.resolvedLimit))

  private[trading] def acceptsResolution(resolution: Any): Boolean =
    resolution.isInstanceOf[PegResolution[?, ?]]

  private[trading] def observations(
    resolution: Resolution
  ): Vector[(PricingObservation, Price[B, Q])] =
    Vector(
      PricingObservation.ReferencePrice -> resolution.referencePrice,
      PricingObservation.ResolvedLimit  -> resolution.resolvedLimit
    )
end PeggedPricing

sealed abstract class PricedVisibility[+D <: Dim] protected ():
  PricedVisibility.requireBuiltin(this)

object PricedVisibility:
  private[order] def requireBuiltin(value: PricedVisibility[?]): Unit =
    val runtimeClass = value.getClass
    val supported    =
      runtimeClass.getName == "trading.order.DisplayedVisibility$" ||
        runtimeClass.getName == "trading.order.HiddenVisibility$" ||
        runtimeClass == classOf[IcebergVisibility[?]]
    if !supported then
      throw new IllegalAccessError(s"unsupported PricedVisibility implementation: ${runtimeClass.getName}")

case object DisplayedVisibility                                      extends PricedVisibility[Nothing]()
case object HiddenVisibility                                         extends PricedVisibility[Nothing]()
final case class IcebergVisibility[D <: Dim](displayedLots: Lots[D]) extends PricedVisibility[D]()

sealed abstract class OrderExecution[D <: Dim, B <: Dim, Q <: Dim] protected ():
  OrderExecution.requireBuiltin(this)

  type Resolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]]
  private[trading] def acceptsResolution(resolution: Any): Boolean
  private[trading] def requiresMaker: Boolean
  private[trading] def observations(resolution: Resolution): Vector[(PricingObservation, Price[B, Q])]

object OrderExecution:
  private[order] def requireBuiltin(value: OrderExecution[?, ?, ?]): Unit =
    val runtimeClass = value.getClass
    val supported    =
      runtimeClass == classOf[MarketExecution[?, ?, ?]] ||
        runtimeClass == classOf[PricedExecution[?, ?, ?, ?]]
    if !supported then
      throw new IllegalAccessError(s"unsupported OrderExecution implementation: ${runtimeClass.getName}")

final case class MarketExecution[D <: Dim, B <: Dim, Q <: Dim](timeInForce: NonRestingTimeInForce)
  extends OrderExecution[D, B, Q]():
  type Resolution = DirectPricingResolution.type

  val resolution: Resolution = DirectPricingResolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]] =
    Right(EffectivePricing.Market())

  private[trading] def acceptsResolution(resolution: Any): Boolean =
    resolution.asInstanceOf[AnyRef].eq(DirectPricingResolution)

  private[trading] val requiresMaker: Boolean = false

  private[trading] def observations(
    resolution: Resolution
  ): Vector[(PricingObservation, Price[B, Q])] = Vector.empty

final case class PricedExecution[D <: Dim, B <: Dim, Q <: Dim, PR <: OrderPricing[B, Q]](
  pricing: PR,
  timeInForce: TimeInForce,
  liquidityConstraint: LiquidityConstraint,
  visibility: PricedVisibility[D])
  extends OrderExecution[D, B, Q]():

  type Resolution = pricing.Resolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]] =
    pricing.resolve(resolution)

  private[trading] def acceptsResolution(resolution: Any): Boolean =
    pricing.acceptsResolution(resolution)

  private[trading] def requiresMaker: Boolean = liquidityConstraint == LiquidityConstraint.MakerOnly

  private[trading] def observations(
    resolution: Resolution
  ): Vector[(PricingObservation, Price[B, Q])] =
    pricing.observations(resolution)
end PricedExecution

@nowarn("msg=Ignoring.*qualifier")
final class OrderIntent[D <: Dim] private[this] (
  val instrumentId: InstrumentId,
  val side: Side,
  val lots: Lots[D],
  val positionEffect: PositionEffect,
  val positionChange: PositionLots[D]):

  override def equals(other: Any): Boolean =
    other match
      case that: OrderIntent[?] =>
        instrumentId == that.instrumentId && side == that.side && lots == that.lots &&
        positionEffect == that.positionEffect && positionChange == that.positionChange
      case _ => false

  override def hashCode: Int =
    (instrumentId, side, lots, positionEffect, positionChange).hashCode

  override def toString: String =
    s"OrderIntent($instrumentId,$side,$lots,$positionEffect,$positionChange)"
end OrderIntent

object OrderIntent:
  private val constructor =
    val owner = classOf[OrderIntent[?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[InstrumentId],
          classOf[Side],
          classOf[Lots[?]],
          classOf[PositionEffect],
          classOf[PositionLots[?]]
        )
      )

  private def construct[D <: Dim](
    instrumentId: InstrumentId,
    side: Side,
    lots: Lots[D],
    positionEffect: PositionEffect,
    positionChange: PositionLots[D]
  ): OrderIntent[D] =
    constructor
      .invoke(instrumentId, side, lots, positionEffect, positionChange)
      .asInstanceOf[OrderIntent[D]]

  def create[I <: Instrument](
    instrument: I
  )(
    side: Side,
    lots: instrument.Lots,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[OrderViolation, OrderIntent[instrument.roles.position.D]] =
    val expected = instrument.identity.id
    if lots.instrumentId != expected then
      Left(OrderViolation.InstrumentMismatch(OrderComponent.Lots, expected, lots.instrumentId))
    else
      Right(
        construct(
          expected,
          side,
          lots,
          positionEffect,
          PositionLots.fromCoordinate(instrument)(side.sign * lots.count.unrefined)
        )
      )
end OrderIntent

sealed abstract class Order[D <: Dim, B <: Dim, Q <: Dim] protected ():
  Order.requireBuiltin(this)

  type Activation <: OrderActivation[B, Q]
  type Execution <: OrderExecution[D, B, Q]

  val instrumentId: InstrumentId
  val intent: OrderIntent[D]
  val activation: Activation
  val execution: Execution

object Order:
  @nowarn("msg=Ignoring.*qualifier")
  private final class ConstructedOrder[
    D <: Dim,
    B <: Dim,
    Q <: Dim,
    A <: OrderActivation[B, Q],
    E <: OrderExecution[D, B, Q]
  ] private[this] (
    val instrumentId: InstrumentId,
    val intent: OrderIntent[D],
    val activation: A,
    val execution: E)
    extends Order[D, B, Q]():
    type Activation = A
    type Execution  = E

  private val constructor =
    val owner = classOf[ConstructedOrder[?, ?, ?, ?, ?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[InstrumentId],
          classOf[OrderIntent[?]],
          classOf[OrderActivation[?, ?]],
          classOf[OrderExecution[?, ?, ?]]
        )
      )

  private def construct[
    D <: Dim,
    B <: Dim,
    Q <: Dim,
    A <: OrderActivation[B, Q],
    E <: OrderExecution[D, B, Q]
  ](
    instrumentId: InstrumentId,
    intent: OrderIntent[D],
    activation: A,
    execution: E
  ): Order.Aux[D, B, Q, A, E] =
    constructor
      .invoke(instrumentId, intent, activation, execution)
      .asInstanceOf[Order.Aux[D, B, Q, A, E]]

  private[order] def requireBuiltin(value: Order[?, ?, ?]): Unit =
    if value.getClass != classOf[ConstructedOrder[?, ?, ?, ?, ?]] then
      throw new IllegalAccessError(s"unsupported Order implementation: ${value.getClass.getName}")

  type Aux[
    D <: Dim,
    B <: Dim,
    Q <: Dim,
    A <: OrderActivation[B, Q],
    E <: OrderExecution[D, B, Q]
  ] =
    Order[D, B, Q] {
      type Activation = A
      type Execution  = E
    }

  def create[
    D <: Dim,
    B <: Dim,
    Q <: Dim,
    A <: OrderActivation[B, Q],
    E <: OrderExecution[D, B, Q]
  ](
    instrument: Instrument
  )(
    intent: OrderIntent[D],
    activation: A,
    execution: E
  ): Either[OrderViolations, Order.Aux[D, B, Q, A, E]] =
    validate(instrument, intent, activation, execution).map: _ =>
      construct(instrument.identity.id, intent, activation, execution)

  def createFirst[
    D <: Dim,
    B <: Dim,
    Q <: Dim,
    A <: OrderActivation[B, Q],
    E <: OrderExecution[D, B, Q]
  ](
    instrument: Instrument
  )(
    intent: OrderIntent[D],
    activation: A,
    execution: E
  ): Either[OrderViolation, Order.Aux[D, B, Q, A, E]] =
    create(instrument)(intent, activation, execution).left.map(_.head)

  def market[I <: Instrument](
    instrument: I
  )(
    side: Side,
    lots: instrument.Lots,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[
    OrderViolations,
    Order.Aux[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      ImmediateActivation[instrument.roles.base.D, instrument.roles.quote.D],
      MarketExecution[
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D
      ]
    ]
  ] =
    for
      intent <- OrderIntent.create(instrument)(side, lots, positionEffect).left.map(OrderViolations.one)
      result <- create(instrument)(
                  intent,
                  ImmediateActivation[instrument.roles.base.D, instrument.roles.quote.D](),
                  MarketExecution[
                    instrument.roles.position.D,
                    instrument.roles.base.D,
                    instrument.roles.quote.D
                  ](NonRestingTimeInForce.ImmediateOrCancel)
                )
    yield result

  def limit[I <: Instrument](
    instrument: I
  )(
    side: Side,
    lots: instrument.Lots,
    limit: instrument.Price,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: PricedVisibility[instrument.roles.position.D] = DisplayedVisibility
  ): Either[
    OrderViolations,
    Order.Aux[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      ImmediateActivation[instrument.roles.base.D, instrument.roles.quote.D],
      PricedExecution[
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D,
        LimitPricing[instrument.roles.base.D, instrument.roles.quote.D]
      ]
    ]
  ] =
    for
      intent <- OrderIntent.create(instrument)(side, lots, positionEffect).left.map(OrderViolations.one)
      result <- create(instrument)(
                  intent,
                  ImmediateActivation[instrument.roles.base.D, instrument.roles.quote.D](),
                  PricedExecution(
                    LimitPricing(limit),
                    timeInForce,
                    liquidityConstraint,
                    visibility
                  )
                )
    yield result

  def stopMarket[I <: Instrument](
    instrument: I
  )(
    side: Side,
    lots: instrument.Lots,
    trigger: TriggerActivation[instrument.roles.base.D, instrument.roles.quote.D],
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[
    OrderViolations,
    Order.Aux[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      trigger.type,
      MarketExecution[
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D
      ]
    ]
  ] =
    for
      intent <- OrderIntent.create(instrument)(side, lots, positionEffect).left.map(OrderViolations.one)
      result <- create[
                  instrument.roles.position.D,
                  instrument.roles.base.D,
                  instrument.roles.quote.D,
                  trigger.type,
                  MarketExecution[
                    instrument.roles.position.D,
                    instrument.roles.base.D,
                    instrument.roles.quote.D
                  ]
                ](instrument)(
                  intent,
                  trigger: trigger.type,
                  MarketExecution[
                    instrument.roles.position.D,
                    instrument.roles.base.D,
                    instrument.roles.quote.D
                  ](NonRestingTimeInForce.ImmediateOrCancel)
                )
    yield result

  def stopLimit[I <: Instrument](
    instrument: I
  )(
    side: Side,
    lots: instrument.Lots,
    trigger: TriggerActivation[instrument.roles.base.D, instrument.roles.quote.D],
    limit: instrument.Price,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: PricedVisibility[instrument.roles.position.D] = DisplayedVisibility
  ): Either[
    OrderViolations,
    Order.Aux[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      trigger.type,
      PricedExecution[
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D,
        LimitPricing[instrument.roles.base.D, instrument.roles.quote.D]
      ]
    ]
  ] =
    for
      intent <- OrderIntent.create(instrument)(side, lots, positionEffect).left.map(OrderViolations.one)
      result <- create[
                  instrument.roles.position.D,
                  instrument.roles.base.D,
                  instrument.roles.quote.D,
                  trigger.type,
                  PricedExecution[
                    instrument.roles.position.D,
                    instrument.roles.base.D,
                    instrument.roles.quote.D,
                    LimitPricing[instrument.roles.base.D, instrument.roles.quote.D]
                  ]
                ](instrument)(
                  intent,
                  trigger: trigger.type,
                  PricedExecution(
                    LimitPricing(limit),
                    timeInForce,
                    liquidityConstraint,
                    visibility
                  )
                )
    yield result

  private def validate[D <: Dim, B <: Dim, Q <: Dim](
    instrument: Instrument,
    intent: OrderIntent[D],
    activation: OrderActivation[B, Q],
    execution: OrderExecution[D, B, Q]
  ): Either[OrderViolations, Unit] =
    val expected   = instrument.identity.id
    val identities = Vector.newBuilder[(Int, OrderViolation)]
    def identity(ordinal: Int, component: OrderComponent, supplied: InstrumentId): Unit =
      if supplied != expected then
        identities += ordinal -> OrderViolation.InstrumentMismatch(component, expected, supplied)

    identity(0, OrderComponent.Intent, intent.instrumentId)
    identity(1, OrderComponent.Lots, intent.lots.instrumentId)
    activation match
      case FixedActivation(_, _, triggerPrice) =>
        identity(2, OrderComponent.TriggerPrice, triggerPrice.instrumentId)
      case _ => ()
    execution match
      case PricedExecution(pricing, _, _, visibility) =>
        pricing match
          case LimitPricing(limit)    => identity(3, OrderComponent.LimitPrice, limit.instrumentId)
          case _: PeggedPricing[?, ?] => ()
        visibility match
          case IcebergVisibility(displayedLots) =>
            identity(4, OrderComponent.DisplayedLots, displayedLots.instrumentId)
          case _ => ()
      case MarketExecution(_) => ()

    OrderViolations.from(identities.result().sortBy(_._1).map(_._2)) match
      case Some(identityViolations) => Left(identityViolations)
      case None                     =>
        val executionViolations = execution match
          case PricedExecution(_, timeInForce, _, IcebergVisibility(displayedLots)) =>
            Vector(
              Option.when(displayedLots.count.unrefined > intent.lots.count.unrefined)(
                OrderViolation.IcebergExceedsOrder(
                  displayedLots.count.unrefined,
                  intent.lots.count.unrefined
                )
              ),
              Option.when(
                timeInForce == TimeInForce.ImmediateOrCancel || timeInForce == TimeInForce.FillOrKill
              )(OrderViolation.NonRestingIceberg)
            ).flatten
          case _ => Vector.empty
        OrderViolations.from(executionViolations).toLeft(())
  end validate
end Order
