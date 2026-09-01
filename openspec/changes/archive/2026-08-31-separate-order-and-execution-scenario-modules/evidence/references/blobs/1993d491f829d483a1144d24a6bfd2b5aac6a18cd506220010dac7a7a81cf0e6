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
  def from(value: TimeInForce): Either[OrderError, NonRestingTimeInForce] =
    value match
      case TimeInForce.ImmediateOrCancel => Right(NonRestingTimeInForce.ImmediateOrCancel)
      case TimeInForce.FillOrKill        => Right(NonRestingTimeInForce.FillOrKill)
      case supplied                      => Left(InvalidOrder(OrderFailureReason.RestingMarketDuration(supplied)))

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

final case class CheckedActivation[P] private[order] (observations: Vector[(String, P)])

sealed trait OrderActivation[P]:
  type Evidence

  def validate(evidence: Evidence): Either[ActivationViolation, CheckedActivation[P]]
  private[trading] def observations(evidence: Evidence): Vector[(String, P)]

sealed trait TriggerActivation[P] extends OrderActivation[P]

final class ImmediateActivation[P] private[order] extends OrderActivation[P]:
  type Evidence = ImmediateActivation.Evidence.type

  val evidence: Evidence = ImmediateActivation.Evidence

  def validate(evidence: Evidence): Either[ActivationViolation, CheckedActivation[P]] =
    Right(CheckedActivation(Vector.empty))

  private[trading] def observations(evidence: Evidence): Vector[(String, P)] = Vector.empty

object ImmediateActivation:
  case object Evidence

final case class FixedActivation[P](
  reference: PriceReference,
  comparison: TriggerComparison,
  triggerPrice: P)
  extends TriggerActivation[P]:

  type Evidence = FixedTriggerEvidence[P]

  private[order] def checkedEvidence(
    observedPrice: P
  )(
    observedTicks: P => BigInt,
    triggerTicks: P => BigInt
  ): Either[ActivationViolation, Evidence] =
    if OrderActivation.comparisonSatisfied(comparison, observedTicks(observedPrice), triggerTicks(triggerPrice)) then
      Right(new FixedTriggerEvidence(reference, comparison, triggerPrice, observedPrice))
    else Left(ActivationViolation.FixedTriggerUnsatisfied)

  def validate(evidence: Evidence): Either[ActivationViolation, CheckedActivation[P]] =
    if
      evidence.reference != reference || evidence.comparison != comparison || evidence.triggerPrice != triggerPrice
    then Left(ActivationViolation.FixedEvidenceMismatch)
    else Right(CheckedActivation(Vector("activation.observed" -> evidence.observedPrice)))

  private[trading] def observations(evidence: Evidence): Vector[(String, P)] =
    Vector("activation.observed" -> evidence.observedPrice)
end FixedActivation

final case class TrailingActivation[P](
  reference: PriceReference,
  comparison: TriggerComparison,
  offsetTicks: PositiveWhole)
  extends TriggerActivation[P]:

  type Evidence = TrailingTriggerEvidence[P]

  private[order] def checkedEvidence(
    favorableExtreme: P,
    observedPrice: P
  )(
    ticks: P => BigInt
  ): Either[ActivationViolation, Evidence] =
    val extreme   = ticks(favorableExtreme)
    val threshold = comparison match
      case TriggerComparison.AtOrAbove => extreme + offsetTicks.unrefined
      case TriggerComparison.AtOrBelow => extreme - offsetTicks.unrefined
    if threshold.signum <= 0 then Left(ActivationViolation.TrailingThresholdNonPositive)
    else if OrderActivation.comparisonSatisfied(comparison, ticks(observedPrice), threshold) then
      Right(new TrailingTriggerEvidence(reference, comparison, offsetTicks, favorableExtreme, observedPrice))
    else Left(ActivationViolation.TrailingTriggerUnsatisfied)

  def validate(evidence: Evidence): Either[ActivationViolation, CheckedActivation[P]] =
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

  private[trading] def observations(evidence: Evidence): Vector[(String, P)] =
    Vector(
      "activation.extreme"  -> evidence.favorableExtreme,
      "activation.observed" -> evidence.observedPrice
    )
end TrailingActivation

object OrderActivation:
  private[order] def comparisonSatisfied(
    comparison: TriggerComparison,
    observed: BigInt,
    threshold: BigInt
  ): Boolean =
    comparison match
      case TriggerComparison.AtOrAbove => observed >= threshold
      case TriggerComparison.AtOrBelow => observed <= threshold

final class FixedTriggerEvidence[P] private[order] (
  val reference: PriceReference,
  private[order] val comparison: TriggerComparison,
  private[order] val triggerPrice: P,
  val observedPrice: P)

final class TrailingTriggerEvidence[P] private[order] (
  val reference: PriceReference,
  private[order] val comparison: TriggerComparison,
  private[order] val offsetTicks: PositiveWhole,
  val favorableExtreme: P,
  val observedPrice: P)

enum EffectivePricing[+P]:
  case Market
  case Limited(price: P)

case object DirectPricingResolution

final class PegResolution[P] private[order] (
  val reference: PriceReference,
  private[order] val offsetTicks: BigInt,
  val referencePrice: P,
  val resolvedLimit: P)

sealed trait OrderPricing[P]:
  type Resolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[P]]
  private[trading] def observations(resolution: Resolution): Vector[(String, P)]

final case class LimitPricing[P](limit: P) extends OrderPricing[P]:
  type Resolution = DirectPricingResolution.type

  val resolution: Resolution = DirectPricingResolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[P]] =
    Right(EffectivePricing.Limited(limit))

  private[trading] def observations(resolution: Resolution): Vector[(String, P)] = Vector.empty

final case class PeggedPricing[P](reference: PriceReference, offsetTicks: BigInt) extends OrderPricing[P]:
  type Resolution = PegResolution[P]

  private[order] def checkedResolution(
    referencePrice: P,
    resolvedLimit: P
  )(
    ticks: P => BigInt
  ): Either[PricingViolation, Resolution] =
    val suppliedOffset = ticks(resolvedLimit) - ticks(referencePrice)
    if suppliedOffset == offsetTicks then
      Right(new PegResolution(reference, offsetTicks, referencePrice, resolvedLimit))
    else Left(PricingViolation.PegOffsetMismatch(offsetTicks, suppliedOffset))

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[P]] =
    if resolution.reference != reference || resolution.offsetTicks != offsetTicks then
      Left(PricingViolation.PegResolutionMismatch)
    else Right(EffectivePricing.Limited(resolution.resolvedLimit))

  private[trading] def observations(resolution: Resolution): Vector[(String, P)] =
    Vector(
      "pricing.reference" -> resolution.referencePrice,
      "pricing.resolved"  -> resolution.resolvedLimit
    )
end PeggedPricing

sealed trait PricedVisibility[+L]
case object DisplayedVisibility                         extends PricedVisibility[Nothing]
case object HiddenVisibility                            extends PricedVisibility[Nothing]
final case class IcebergVisibility[L](displayedLots: L) extends PricedVisibility[L]

sealed trait OrderExecution[L, P]:
  type Resolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[P]]
  private[trading] def requiresMaker: Boolean
  private[trading] def observations(resolution: Resolution): Vector[(String, P)]

final case class MarketExecution[L, P](timeInForce: NonRestingTimeInForce) extends OrderExecution[L, P]:
  type Resolution = DirectPricingResolution.type

  val resolution: Resolution = DirectPricingResolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[P]] =
    Right(EffectivePricing.Market)

  private[trading] val requiresMaker: Boolean = false

  private[trading] def observations(resolution: Resolution): Vector[(String, P)] = Vector.empty

final case class PricedExecution[L, P, PR <: OrderPricing[P]](
  pricing: PR,
  timeInForce: TimeInForce,
  liquidityConstraint: LiquidityConstraint,
  visibility: PricedVisibility[L])
  extends OrderExecution[L, P]:

  type Resolution = pricing.Resolution

  def resolve(resolution: Resolution): Either[PricingViolation, EffectivePricing[P]] =
    pricing.resolve(resolution)

  private[trading] def requiresMaker: Boolean = liquidityConstraint == LiquidityConstraint.MakerOnly

  private[trading] def observations(resolution: Resolution): Vector[(String, P)] =
    pricing.observations(resolution)

final case class OrderIntent[L](
  instrumentId: InstrumentId,
  side: Side,
  lots: L,
  positionEffect: PositionEffect,
  positionChange: PositionLots[? <: Dim])

sealed abstract class Order[L, P] private[order]:
  type Activation <: OrderActivation[P]
  type Execution <: OrderExecution[L, P]

  val instrumentId: InstrumentId
  val intent: OrderIntent[L]
  val activation: Activation
  val execution: Execution

private final class ConstructedOrder[
  L,
  P,
  A <: OrderActivation[P],
  E <: OrderExecution[L, P]
](
  val instrumentId: InstrumentId,
  val intent: OrderIntent[L],
  val activation: A,
  val execution: E)
  extends Order[L, P]:
  type Activation = A
  type Execution  = E

object Order:
  type Aux[L, P, A <: OrderActivation[P], E <: OrderExecution[L, P]] =
    Order[L, P] {
      type Activation = A
      type Execution  = E
    }

final class Orders[I <: Instrument] private[order] (val instrument: I):

  private val instrumentId = instrument.identity.id

  type Lots  = instrument.Lots
  type Price = instrument.Price

  val immediate: ImmediateActivation[Price] = new ImmediateActivation[Price]
  val displayed: PricedVisibility[Lots]     = DisplayedVisibility
  val hidden: PricedVisibility[Lots]        = HiddenVisibility

  def fixedTrigger(
    reference: PriceReference,
    comparison: TriggerComparison,
    triggerPrice: Price
  ): FixedActivation[Price] =
    FixedActivation(reference, comparison, triggerPrice)

  def trailingTrigger(
    reference: PriceReference,
    comparison: TriggerComparison,
    offsetTicks: BigInt
  ): Either[OrderError, TrailingActivation[Price]] =
    PositiveWhole(offsetTicks)
      .left
      .map(_ => InvalidTrailingOffset(offsetTicks))
      .map(TrailingActivation(reference, comparison, _))

  def limitPricing(limit: Price): LimitPricing[Price] = LimitPricing(limit)

  def peggedPricing(reference: PriceReference, offsetTicks: BigInt): PeggedPricing[Price] =
    PeggedPricing(reference, offsetTicks)

  def fixedEvidence(
    activation: FixedActivation[Price]
  )(
    observedPrice: Price
  ): Either[ActivationViolation, activation.Evidence] =
    activation.checkedEvidence(observedPrice)(_.ticks.unrefined, _.ticks.unrefined)

  def trailingEvidence(
    activation: TrailingActivation[Price]
  )(
    favorableExtreme: Price,
    observedPrice: Price
  ): Either[ActivationViolation, activation.Evidence] =
    activation.checkedEvidence(favorableExtreme, observedPrice)(_.ticks.unrefined)

  def pegResolution(
    pricing: PeggedPricing[Price]
  )(
    referencePrice: Price,
    resolvedLimit: Price
  ): Either[PricingViolation, pricing.Resolution] =
    pricing.checkedResolution(referencePrice, resolvedLimit)(_.ticks.unrefined)

  def iceberg(displayedLots: Lots): IcebergVisibility[Lots] = IcebergVisibility(displayedLots)

  def marketExecution(timeInForce: NonRestingTimeInForce): MarketExecution[Lots, Price] =
    MarketExecution(timeInForce)

  def pricedExecution[PR <: OrderPricing[Price]](
    pricing: PR,
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    visibility: PricedVisibility[Lots]
  ): PricedExecution[Lots, Price, PR] =
    PricedExecution(pricing, timeInForce, liquidityConstraint, visibility)

  def intent(
    side: Side,
    lots: Lots,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): OrderIntent[Lots] =
    OrderIntent(
      instrumentId,
      side,
      lots,
      positionEffect,
      PositionLots.fromCoordinate(instrument)(side.sign * lots.count.unrefined)
    )

  def create[A <: OrderActivation[Price], E <: OrderExecution[Lots, Price]](
    intent: OrderIntent[Lots],
    activation: A,
    execution: E
  ): Either[OrderError, Order.Aux[Lots, Price, A, E]] =
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
    Order.Aux[Lots, Price, ImmediateActivation[Price], MarketExecution[Lots, Price]]
  ] =
    create(intent(side, lots, positionEffect), immediate, marketExecution(NonRestingTimeInForce.ImmediateOrCancel))

  def limit(
    side: Side,
    lots: Lots,
    limit: Price,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: PricedVisibility[Lots] = DisplayedVisibility
  ): Either[
    OrderError,
    Order.Aux[
      Lots,
      Price,
      ImmediateActivation[Price],
      PricedExecution[Lots, Price,
        LimitPricing[Price]]
    ]
  ] =
    create(
      intent(side, lots, positionEffect),
      immediate,
      pricedExecution(limitPricing(limit), timeInForce, liquidityConstraint, visibility)
    )

  def stopMarket[A <: TriggerActivation[Price]](
    side: Side,
    lots: Lots,
    trigger: A,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[OrderError, Order.Aux[Lots, Price, A, MarketExecution[Lots, Price]]] =
    create(
      intent(side, lots, positionEffect),
      trigger,
      marketExecution(NonRestingTimeInForce.ImmediateOrCancel)
    )

  def stopLimit[A <: TriggerActivation[Price]](
    side: Side,
    lots: Lots,
    trigger: A,
    limit: Price,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: PricedVisibility[Lots] = DisplayedVisibility
  ): Either[
    OrderError,
    Order.Aux[Lots, Price, A, PricedExecution[Lots, Price, LimitPricing[Price]]]
  ] =
    create(
      intent(side, lots, positionEffect),
      trigger,
      pricedExecution(limitPricing(limit), timeInForce, liquidityConstraint, visibility)
    )

  private def validateIdentities(
    intent: OrderIntent[Lots],
    activation: OrderActivation[Price],
    execution: OrderExecution[Lots, Price]
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
          case LimitPricing(price)     => supplied += "execution.limit" -> price.instrumentId
          case _: PeggedPricing[Price] => ()
        visibility match
          case IcebergVisibility(lots) => supplied += "execution.iceberg" -> lots.instrumentId
          case _                       => ()
      case _: MarketExecution[Lots, Price] => ()
    OrderIdentityChecks.check("order", instrumentId, supplied.result()*)
  end validateIdentities

  private def validatePositionChange(intent: OrderIntent[Lots]): Either[OrderError, Unit] =
    val expected = PositionLots.fromCoordinate(instrument)(intent.side.sign * intent.lots.count.unrefined)
    Either.cond(
      intent.positionChange == expected,
      (),
      InvalidOrder(
        OrderFailureReason.PositionChangeMismatch(expected.coordinate, intent.positionChange.coordinate)
      )
    )

  private def validateExecution(
    intent: OrderIntent[Lots],
    execution: OrderExecution[Lots, Price]
  ): Either[OrderError, Unit] =
    execution match
      case PricedExecution(_, timeInForce, _, IcebergVisibility(displayedLots))
        if displayedLots.count.unrefined > intent.lots.count.unrefined =>
        Left(
          InvalidOrder(
            OrderFailureReason.IcebergExceedsOrder(displayedLots.count.unrefined, intent.lots.count.unrefined)
          )
        )
      case PricedExecution(_, timeInForce, _, _: IcebergVisibility[Lots])
        if timeInForce == TimeInForce.ImmediateOrCancel || timeInForce == TimeInForce.FillOrKill =>
        Left(InvalidOrder(OrderFailureReason.NonRestingIceberg))
      case _ => Right(())

end Orders

object Orders:
  def apply[I <: Instrument](instrument: I): Orders[instrument.type] =
    new Orders[instrument.type](instrument)
