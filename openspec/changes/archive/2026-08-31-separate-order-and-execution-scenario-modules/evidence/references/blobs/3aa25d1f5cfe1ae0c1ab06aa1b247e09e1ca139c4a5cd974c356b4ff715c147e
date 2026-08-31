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
  def from(value: TimeInForce): Either[InvalidMarketDuration, NonRestingTimeInForce] =
    value match
      case TimeInForce.ImmediateOrCancel => Right(NonRestingTimeInForce.ImmediateOrCancel)
      case TimeInForce.FillOrKill        => Right(NonRestingTimeInForce.FillOrKill)
      case supplied                      => Left(InvalidMarketDuration(supplied))

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
  observations: Vector[(String, Price[B, Q])])

sealed trait OrderActivation[B <: Dim, Q <: Dim]:
  type Evidence

  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]]
  private[trading] def observations(evidence: Evidence): Vector[(String, Price[B, Q])]

sealed trait TriggerActivation[B <: Dim, Q <: Dim] extends OrderActivation[B, Q]

final class ImmediateActivation[B <: Dim, Q <: Dim] private[order] extends OrderActivation[B, Q]:
  type Evidence = ImmediateActivation.Evidence.type

  val evidence: Evidence = ImmediateActivation.Evidence

  def verify(evidence: Evidence): Either[ActivationViolation, CheckedActivation[B, Q]] =
    Right(CheckedActivation(Vector.empty))

  private[trading] def observations(evidence: Evidence): Vector[(String, Price[B, Q])] = Vector.empty

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
    else Right(CheckedActivation(Vector("activation.observed" -> evidence.observedPrice)))

  private[trading] def observations(evidence: Evidence): Vector[(String, Price[B, Q])] =
    Vector("activation.observed" -> evidence.observedPrice)
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
            "activation.extreme"  -> evidence.favorableExtreme,
            "activation.observed" -> evidence.observedPrice
          )
        )
      )

  private[trading] def observations(evidence: Evidence): Vector[(String, Price[B, Q])] =
    Vector(
      "activation.extreme"  -> evidence.favorableExtreme,
      "activation.observed" -> evidence.observedPrice
    )
end TrailingActivation

object TrailingActivation:
  def create[B <: Dim, Q <: Dim](
    reference: PriceReference,
    comparison: TriggerComparison,
    offsetTicks: BigInt
  ): Either[InvalidTrailingOffset, TrailingActivation[B, Q]] =
    PositiveWhole(offsetTicks)
      .left
      .map(_ => InvalidTrailingOffset(offsetTicks))
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
  private[trading] def observations(resolution: Resolution): Vector[(String, Price[B, Q])]

final case class LimitPricing[B <: Dim, Q <: Dim](limit: Price[B, Q]) extends OrderPricing[B, Q]:
  type Resolution = DirectPricingResolution.type

  val resolution: Resolution = DirectPricingResolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]] =
    Right(EffectivePricing.Limited(limit))

  private[trading] def observations(resolution: Resolution): Vector[(String, Price[B, Q])] = Vector.empty

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

  private[trading] def observations(resolution: Resolution): Vector[(String, Price[B, Q])] =
    Vector(
      "pricing.reference" -> resolution.referencePrice,
      "pricing.resolved"  -> resolution.resolvedLimit
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
  private[trading] def observations(resolution: Resolution): Vector[(String, Price[B, Q])]

final case class MarketExecution[D <: Dim, B <: Dim, Q <: Dim](timeInForce: NonRestingTimeInForce)
  extends OrderExecution[D, B, Q]:
  type Resolution = DirectPricingResolution.type

  val resolution: Resolution = DirectPricingResolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[B, Q]] =
    Right(EffectivePricing.Market())

  private[trading] val requiresMaker: Boolean = false

  private[trading] def observations(resolution: Resolution): Vector[(String, Price[B, Q])] = Vector.empty

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

  private[trading] def observations(resolution: Resolution): Vector[(String, Price[B, Q])] =
    pricing.observations(resolution)

final case class OrderIntent[D <: Dim](
  instrumentId: InstrumentId,
  side: Side,
  lots: Lots[D],
  positionEffect: PositionEffect,
  positionChange: PositionLots[D])

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

final class Orders[I <: Instrument] private[order] (val instrument: I):

  private val instrumentId = instrument.identity.id

  type D     = instrument.roles.position.D
  type B     = instrument.roles.base.D
  type Q     = instrument.roles.quote.D
  type Lots  = instrument.Lots
  type Price = instrument.Price

  val immediate: ImmediateActivation[B, Q] = ImmediateActivation[B, Q]()
  val displayed: PricedVisibility[D]       = DisplayedVisibility
  val hidden: PricedVisibility[D]          = HiddenVisibility

  def fixedTrigger(
    reference: PriceReference,
    comparison: TriggerComparison,
    triggerPrice: Price
  ): FixedActivation[B, Q] =
    FixedActivation(reference, comparison, triggerPrice)

  def trailingTrigger(
    reference: PriceReference,
    comparison: TriggerComparison,
    offsetTicks: BigInt
  ): Either[InvalidTrailingOffset, TrailingActivation[B, Q]] =
    TrailingActivation.create[B, Q](reference, comparison, offsetTicks)

  def limitPricing(limit: Price): LimitPricing[B, Q] = LimitPricing(limit)

  def peggedPricing(reference: PriceReference, offsetTicks: BigInt): PeggedPricing[B, Q] =
    PeggedPricing(reference, offsetTicks)

  def fixedEvidence(
    activation: FixedActivation[B, Q]
  )(
    observedPrice: Price
  ): Either[ActivationViolation, activation.Evidence] =
    activation.evidence(observedPrice)

  def trailingEvidence(
    activation: TrailingActivation[B, Q]
  )(
    favorableExtreme: Price,
    observedPrice: Price
  ): Either[ActivationViolation, activation.Evidence] =
    activation.evidence(favorableExtreme, observedPrice)

  def pegResolution(
    pricing: PeggedPricing[B, Q]
  )(
    referencePrice: Price,
    resolvedLimit: Price
  ): Either[PricingViolation, pricing.Resolution] =
    pricing.resolution(referencePrice, resolvedLimit)

  def iceberg(displayedLots: Lots): IcebergVisibility[D] = IcebergVisibility(displayedLots)

  def marketExecution(timeInForce: NonRestingTimeInForce): MarketExecution[D, B, Q] =
    MarketExecution(timeInForce)

  def pricedExecution[PR <: OrderPricing[B, Q]](
    pricing: PR,
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    visibility: PricedVisibility[D]
  ): PricedExecution[D, B, Q, PR] =
    PricedExecution(pricing, timeInForce, liquidityConstraint, visibility)

  def intent(
    side: Side,
    lots: Lots,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): OrderIntent[D] =
    OrderIntent(
      instrumentId,
      side,
      lots,
      positionEffect,
      PositionLots.fromCoordinate(instrument)(side.sign * lots.count.unrefined)
    )

  def create[A <: OrderActivation[B, Q], E <: OrderExecution[D, B, Q]](
    intent: OrderIntent[D],
    activation: A,
    execution: E
  ): Either[OrderError, Order.Aux[D, B, Q, A, E]] =
    for
      _ <- validateIdentities(intent, activation, execution)
      _ <- validatePositionChange(intent)
      _ <- validateExecution(intent, execution)
    yield new ConstructedOrder(instrumentId, intent, activation, execution)

  def market(
    side: Side,
    lots: Lots,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[
    OrderError,
    Order.Aux[D, B, Q, ImmediateActivation[B, Q], MarketExecution[D, B, Q]]
  ] =
    create(intent(side, lots, positionEffect), immediate, marketExecution(NonRestingTimeInForce.ImmediateOrCancel))

  def limit(
    side: Side,
    lots: Lots,
    limit: Price,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: PricedVisibility[D] = DisplayedVisibility
  ): Either[
    OrderError,
    Order.Aux[
      D,
      B,
      Q,
      ImmediateActivation[B, Q],
      PricedExecution[D, B, Q,
        LimitPricing[B, Q]]
    ]
  ] =
    create(
      intent(side, lots, positionEffect),
      immediate,
      pricedExecution(limitPricing(limit), timeInForce, liquidityConstraint, visibility)
    )

  def stopMarket[A <: TriggerActivation[B, Q]](
    side: Side,
    lots: Lots,
    trigger: A,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[OrderError, Order.Aux[D, B, Q, A, MarketExecution[D, B, Q]]] =
    create(
      intent(side, lots, positionEffect),
      trigger,
      marketExecution(NonRestingTimeInForce.ImmediateOrCancel)
    )

  def stopLimit[A <: TriggerActivation[B, Q]](
    side: Side,
    lots: Lots,
    trigger: A,
    limit: Price,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: PricedVisibility[D] = DisplayedVisibility
  ): Either[
    OrderError,
    Order.Aux[D, B, Q, A, PricedExecution[D, B, Q, LimitPricing[B, Q]]]
  ] =
    create(
      intent(side, lots, positionEffect),
      trigger,
      pricedExecution(limitPricing(limit), timeInForce, liquidityConstraint, visibility)
    )

  private def validateIdentities(
    intent: OrderIntent[D],
    activation: OrderActivation[B, Q],
    execution: OrderExecution[D, B, Q]
  ): Either[OrderError, Unit] =
    val supplied = Vector.newBuilder[(String, InstrumentId)]
    supplied += "intent"                -> intent.instrumentId
    supplied += "intent.lots"           -> intent.lots.instrumentId
    supplied += "intent.positionChange" -> intent.positionChange.instrumentId
    activation match
      case FixedActivation(_, _, price) => supplied += "activation.triggerPrice" -> price.instrumentId
      case _                            => ()
    execution match
      case PricedExecution(pricing, _, _, visibility) =>
        pricing match
          case LimitPricing(price)    => supplied += "execution.limit" -> price.instrumentId
          case _: PeggedPricing[B, Q] => ()
        visibility match
          case IcebergVisibility(lots) => supplied += "execution.iceberg" -> lots.instrumentId
          case _                       => ()
      case _: MarketExecution[D, B, Q] => ()
    OrderIdentityChecks.check("order", instrumentId, supplied.result()*)
  end validateIdentities

  private def validatePositionChange(intent: OrderIntent[D]): Either[OrderError, Unit] =
    val expected = PositionLots.fromCoordinate(instrument)(intent.side.sign * intent.lots.count.unrefined)
    Either.cond(
      intent.positionChange == expected,
      (),
      InvalidOrder(
        OrderFailureReason.PositionChangeMismatch(expected.coordinate, intent.positionChange.coordinate)
      )
    )

  private def validateExecution(
    intent: OrderIntent[D],
    execution: OrderExecution[D, B, Q]
  ): Either[OrderError, Unit] =
    execution match
      case PricedExecution(_, timeInForce, _, IcebergVisibility(displayedLots))
        if displayedLots.count.unrefined > intent.lots.count.unrefined =>
        Left(
          InvalidOrder(
            OrderFailureReason.IcebergExceedsOrder(displayedLots.count.unrefined, intent.lots.count.unrefined)
          )
        )
      case PricedExecution(_, timeInForce, _, IcebergVisibility(_))
        if timeInForce == TimeInForce.ImmediateOrCancel || timeInForce == TimeInForce.FillOrKill =>
        Left(InvalidOrder(OrderFailureReason.NonRestingIceberg))
      case _ => Right(())

end Orders

object Orders:
  def apply[I <: Instrument](instrument: I): Orders[instrument.type] =
    new Orders[instrument.type](instrument)
