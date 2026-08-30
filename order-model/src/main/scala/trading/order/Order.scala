package trading.order

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

final case class CheckedActivation[B <: Dim, Q <: Dim] private[order] (
  observations: Vector[(ActivationObservation, Price[B, Q])])

enum ActivationObservation:
  case Observed, FavorableExtreme

enum PricingObservation:
  case ReferencePrice, ResolvedLimit

sealed trait OrderActivation[B <: Dim, Q <: Dim]:
  type Evidence

  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]]
  private[trading] def observations(evidence: Evidence): Vector[(ActivationObservation, Price[B, Q])]

sealed trait TriggerActivation[B <: Dim, Q <: Dim] extends OrderActivation[B, Q]

final class ImmediateActivation[B <: Dim, Q <: Dim] private[order] extends OrderActivation[B, Q]:
  type Evidence = ImmediateActivation.Evidence.type

  val evidence: Evidence = ImmediateActivation.Evidence

  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]] =
    Right(CheckedActivation(Vector.empty))

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
  extends TriggerActivation[B, Q]:

  type Evidence = FixedTriggerEvidence[B, Q]

  def evidence(observedPrice: Price[B, Q]): Either[ActivationViolation, Evidence] =
    if
      OrderActivation.comparisonSatisfied(
        comparison,
        observedPrice.ticks.unrefined,
        triggerPrice.ticks.unrefined
      )
    then
      Right(new FixedTriggerEvidence(reference, comparison, triggerPrice, observedPrice))
    else Left(ActivationViolation.FixedTriggerUnsatisfied)

  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]] =
    if
      evidence.reference != reference || evidence.comparison != comparison || evidence.triggerPrice != triggerPrice
    then Left(ActivationViolation.FixedEvidenceMismatch)
    else Right(CheckedActivation(Vector(ActivationObservation.Observed -> evidence.observedPrice)))

  private[trading] def observations(
    evidence: Evidence
  ): Vector[(ActivationObservation, Price[B, Q])] =
    Vector(ActivationObservation.Observed -> evidence.observedPrice)
end FixedActivation

final case class TrailingActivation[B <: Dim, Q <: Dim] private (
  reference: PriceReference,
  comparison: TriggerComparison,
  offsetTicks: PositiveWhole)
  extends TriggerActivation[B, Q]:

  type Evidence = TrailingTriggerEvidence[B, Q]

  def evidence(
    favorableExtreme: Price[B, Q],
    observedPrice: Price[B, Q]
  ): Either[ActivationViolation, Evidence] =
    val extreme   = favorableExtreme.ticks.unrefined
    val threshold = comparison match
      case TriggerComparison.AtOrAbove => extreme + offsetTicks.unrefined
      case TriggerComparison.AtOrBelow => extreme - offsetTicks.unrefined
    if threshold.signum <= 0 then Left(ActivationViolation.TrailingThresholdNonPositive)
    else if OrderActivation.comparisonSatisfied(comparison, observedPrice.ticks.unrefined, threshold) then
      Right(new TrailingTriggerEvidence(reference, comparison, offsetTicks, favorableExtreme, observedPrice))
    else Left(ActivationViolation.TrailingTriggerUnsatisfied)

  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]] =
    if
      evidence.reference != reference || evidence.comparison != comparison || evidence.offsetTicks != offsetTicks
    then Left(ActivationViolation.TrailingEvidenceMismatch)
    else
      Right(
        CheckedActivation(
          Vector(
            ActivationObservation.FavorableExtreme -> evidence.favorableExtreme,
            ActivationObservation.Observed         -> evidence.observedPrice
          )
        )
      )

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
  private[order] def comparisonSatisfied(
    comparison: TriggerComparison,
    observed: BigInt,
    threshold: BigInt
  ): Boolean =
    comparison match
      case TriggerComparison.AtOrAbove => observed >= threshold
      case TriggerComparison.AtOrBelow => observed <= threshold

final class FixedTriggerEvidence[B <: Dim, Q <: Dim] private[order] (
  val reference: PriceReference,
  private[order] val comparison: TriggerComparison,
  private[order] val triggerPrice: Price[B, Q],
  val observedPrice: Price[B, Q])

final class TrailingTriggerEvidence[B <: Dim, Q <: Dim] private[order] (
  val reference: PriceReference,
  private[order] val comparison: TriggerComparison,
  private[order] val offsetTicks: PositiveWhole,
  val favorableExtreme: Price[B, Q],
  val observedPrice: Price[B, Q])

sealed trait EffectivePricing[B <: Dim, Q <: Dim]

object EffectivePricing:
  final case class Market[B <: Dim, Q <: Dim]()                    extends EffectivePricing[B, Q]
  final case class Limited[B <: Dim, Q <: Dim](price: Price[B, Q]) extends EffectivePricing[B, Q]

case object DirectPricingResolution

final class PegResolution[B <: Dim, Q <: Dim] private[order] (
  val reference: PriceReference,
  private[order] val offsetTicks: BigInt,
  val referencePrice: Price[B, Q],
  val resolvedLimit: Price[B, Q])

sealed trait OrderPricing[B <: Dim, Q <: Dim]:
  type Resolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]]
  private[trading] def observations(resolution: Resolution): Vector[(PricingObservation, Price[B, Q])]

final case class LimitPricing[B <: Dim, Q <: Dim](limit: Price[B, Q]) extends OrderPricing[B, Q]:
  type Resolution = DirectPricingResolution.type

  val resolution: Resolution = DirectPricingResolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]] =
    Right(EffectivePricing.Limited(limit))

  private[trading] def observations(
    resolution: Resolution
  ): Vector[(PricingObservation, Price[B, Q])] = Vector.empty

final case class PeggedPricing[B <: Dim, Q <: Dim](reference: PriceReference, offsetTicks: BigInt)
  extends OrderPricing[B, Q]:
  type Resolution = PegResolution[B, Q]

  def resolution(
    referencePrice: Price[B, Q],
    resolvedLimit: Price[B, Q]
  ): Either[PricingViolation, Resolution] =
    val suppliedOffset = resolvedLimit.ticks.unrefined - referencePrice.ticks.unrefined
    if suppliedOffset == offsetTicks then
      Right(new PegResolution(reference, offsetTicks, referencePrice, resolvedLimit))
    else Left(PricingViolation.PegOffsetMismatch(offsetTicks, suppliedOffset))

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]] =
    if resolution.reference != reference || resolution.offsetTicks != offsetTicks then
      Left(PricingViolation.PegResolutionMismatch)
    else Right(EffectivePricing.Limited(resolution.resolvedLimit))

  private[trading] def observations(
    resolution: Resolution
  ): Vector[(PricingObservation, Price[B, Q])] =
    Vector(
      PricingObservation.ReferencePrice -> resolution.referencePrice,
      PricingObservation.ResolvedLimit  -> resolution.resolvedLimit
    )
end PeggedPricing

sealed trait PricedVisibility[+D <: Dim]
case object DisplayedVisibility                                      extends PricedVisibility[Nothing]
case object HiddenVisibility                                         extends PricedVisibility[Nothing]
final case class IcebergVisibility[D <: Dim](displayedLots: Lots[D]) extends PricedVisibility[D]

sealed trait OrderExecution[D <: Dim, B <: Dim, Q <: Dim]:
  type Resolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]]
  private[trading] def requiresMaker: Boolean
  private[trading] def observations(resolution: Resolution): Vector[(PricingObservation, Price[B, Q])]

final case class MarketExecution[D <: Dim, B <: Dim, Q <: Dim](timeInForce: NonRestingTimeInForce)
  extends OrderExecution[D, B, Q]:
  type Resolution = DirectPricingResolution.type

  val resolution: Resolution = DirectPricingResolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]] =
    Right(EffectivePricing.Market())

  private[trading] val requiresMaker: Boolean = false

  private[trading] def observations(
    resolution: Resolution
  ): Vector[(PricingObservation, Price[B, Q])] = Vector.empty

final case class PricedExecution[D <: Dim, B <: Dim, Q <: Dim, PR <: OrderPricing[B, Q]](
  pricing: PR,
  timeInForce: TimeInForce,
  liquidityConstraint: LiquidityConstraint,
  visibility: PricedVisibility[D])
  extends OrderExecution[D, B, Q]:

  type Resolution = pricing.Resolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]] =
    pricing.resolve(resolution)

  private[trading] def requiresMaker: Boolean = liquidityConstraint == LiquidityConstraint.MakerOnly

  private[trading] def observations(
    resolution: Resolution
  ): Vector[(PricingObservation, Price[B, Q])] =
    pricing.observations(resolution)

final class OrderIntent[D <: Dim] private[order] (
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
        new OrderIntent(
          expected,
          side,
          lots,
          positionEffect,
          PositionLots.fromCoordinate(instrument)(side.sign * lots.count.unrefined)
        )
      )
end OrderIntent

sealed abstract class Order[D <: Dim, B <: Dim, Q <: Dim] private[order]:
  type Activation <: OrderActivation[B, Q]
  type Execution <: OrderExecution[D, B, Q]

  val instrumentId: InstrumentId
  val intent: OrderIntent[D]
  val activation: Activation
  val execution: Execution

private final class ConstructedOrder[
  D <: Dim,
  B <: Dim,
  Q <: Dim,
  A <: OrderActivation[B, Q],
  E <: OrderExecution[D, B, Q]
](
  val instrumentId: InstrumentId,
  val intent: OrderIntent[D],
  val activation: A,
  val execution: E)
  extends Order[D, B, Q]:
  type Activation = A
  type Execution  = E

object Order:
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
      new ConstructedOrder(instrument.identity.id, intent, activation, execution)

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
