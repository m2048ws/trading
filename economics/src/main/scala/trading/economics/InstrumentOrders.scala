package trading.economics

import trading.quantity.Dimension
import trading.quantity.refinement.PositiveWhole

sealed trait OrderActivation[+P]
case object ImmediateActivation extends OrderActivation[Nothing]
final case class FixedActivation[P](
  reference: PriceReference,
  comparison: TriggerComparison,
  triggerPrice: P)
  extends OrderActivation[P]
final case class TrailingActivation[P](
  reference: PriceReference,
  comparison: TriggerComparison,
  offsetTicks: PositiveWhole)
  extends OrderActivation[P]

sealed trait OrderPricing[+P]
final case class LimitPricing[P](limit: P)                                     extends OrderPricing[P]
final case class PeggedPricing(reference: PriceReference, offsetTicks: BigInt) extends OrderPricing[Nothing]

sealed trait PricedVisibility[+L]
case object DisplayedVisibility                         extends PricedVisibility[Nothing]
case object HiddenVisibility                            extends PricedVisibility[Nothing]
final case class IcebergVisibility[L](displayedLots: L) extends PricedVisibility[L]

sealed trait OrderExecution[+L, +P]
final case class MarketExecution(timeInForce: NonRestingTimeInForce) extends OrderExecution[Nothing, Nothing]
final case class PricedExecution[L, P](
  pricing: OrderPricing[P],
  timeInForce: TimeInForce,
  liquidityConstraint: LiquidityConstraint,
  visibility: PricedVisibility[L])
  extends OrderExecution[L, P]

final case class OrderIntent[L](
  instrumentId: InstrumentId,
  side: Side,
  lots: L,
  positionEffect: PositionEffect)

final case class InstrumentOrder[L, P] private[economics] (
  instrumentId: InstrumentId,
  intent: OrderIntent[L],
  activation: OrderActivation[P],
  execution: OrderExecution[L, P])

final class InstrumentOrders[D <: Dimension, B <: Dimension, Q <: Dimension] private[economics] (
  instrumentId: InstrumentId):

  private type Lots  = InstrumentLots[D]
  private type Price = InstrumentPrice[B, Q]

  val immediate: OrderActivation[Price] = ImmediateActivation
  val displayed: PricedVisibility[Lots] = DisplayedVisibility
  val hidden: PricedVisibility[Lots]    = HiddenVisibility

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
  ): Either[EconomicsError, TrailingActivation[Price]] =
    PositiveWhole(offsetTicks)
      .left
      .map(_ => InvalidTrailingOffset(offsetTicks))
      .map(TrailingActivation(reference, comparison, _))

  def limitPricing(limit: Price): LimitPricing[Price] = LimitPricing(limit)

  def peggedPricing(reference: PriceReference, offsetTicks: BigInt): OrderPricing[Price] =
    PeggedPricing(reference, offsetTicks)

  def iceberg(displayedLots: Lots): IcebergVisibility[Lots] = IcebergVisibility(displayedLots)

  def marketExecution(timeInForce: NonRestingTimeInForce): OrderExecution[Lots, Price] =
    MarketExecution(timeInForce)

  def pricedExecution(
    pricing: OrderPricing[Price],
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    visibility: PricedVisibility[Lots]
  ): PricedExecution[Lots, Price] =
    PricedExecution(pricing, timeInForce, liquidityConstraint, visibility)

  def intent(
    side: Side,
    lots: Lots,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): OrderIntent[Lots] =
    OrderIntent(lots.instrumentId, side, lots, positionEffect)

  def create(
    intent: OrderIntent[Lots],
    activation: OrderActivation[Price],
    execution: OrderExecution[Lots, Price]
  ): Either[EconomicsError, InstrumentOrder[Lots, Price]] =
    for
      _ <- validateIdentities(intent, activation, execution)
      _ <- validateExecution(intent, execution)
    yield InstrumentOrder(instrumentId, intent, activation, execution)

  def market(
    side: Side,
    lots: Lots,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[EconomicsError, InstrumentOrder[Lots, Price]] =
    create(intent(side, lots, positionEffect), immediate, marketExecution(NonRestingTimeInForce.ImmediateOrCancel))

  def limit(
    side: Side,
    lots: Lots,
    limit: Price,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: PricedVisibility[Lots] = DisplayedVisibility
  ): Either[EconomicsError, InstrumentOrder[Lots, Price]] =
    create(
      intent(side, lots, positionEffect),
      immediate,
      pricedExecution(limitPricing(limit), timeInForce, liquidityConstraint, visibility)
    )

  def stopMarket(
    side: Side,
    lots: Lots,
    trigger: OrderActivation[Price],
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[EconomicsError, InstrumentOrder[Lots, Price]] =
    trigger match
      case ImmediateActivation => Left(InvalidOrder(OrderFailureReason.StopRequiresTrigger))
      case _                   =>
        create(
          intent(side, lots, positionEffect),
          trigger,
          marketExecution(NonRestingTimeInForce.ImmediateOrCancel)
        )

  def stopLimit(
    side: Side,
    lots: Lots,
    trigger: OrderActivation[Price],
    limit: Price,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: PricedVisibility[Lots] = DisplayedVisibility
  ): Either[EconomicsError, InstrumentOrder[Lots, Price]] =
    trigger match
      case ImmediateActivation => Left(InvalidOrder(OrderFailureReason.StopRequiresTrigger))
      case _                   =>
        create(
          intent(side, lots, positionEffect),
          trigger,
          pricedExecution(limitPricing(limit), timeInForce, liquidityConstraint, visibility)
        )

  private def validateIdentities(
    intent: OrderIntent[Lots],
    activation: OrderActivation[Price],
    execution: OrderExecution[Lots, Price]
  ): Either[EconomicsError, Unit] =
    val supplied = Vector.newBuilder[(String, InstrumentId)]
    supplied += "intent"      -> intent.instrumentId
    supplied += "intent.lots" -> intent.lots.instrumentId
    activation match
      case FixedActivation(_, _, price) => supplied += "activation.triggerPrice" -> price.instrumentId
      case _                            => ()
    execution match
      case PricedExecution(pricing, _, _, visibility) =>
        pricing match
          case LimitPricing(price) => supplied += "execution.limit" -> price.instrumentId
          case _: PeggedPricing    => ()
        visibility match
          case IcebergVisibility(lots) => supplied += "execution.iceberg" -> lots.instrumentId
          case _                       => ()
      case _: MarketExecution => ()
    InstrumentIdentityChecks.check("order", instrumentId, supplied.result()*)
  end validateIdentities

  private def validateExecution(
    intent: OrderIntent[Lots],
    execution: OrderExecution[Lots, Price]
  ): Either[EconomicsError, Unit] =
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

end InstrumentOrders
