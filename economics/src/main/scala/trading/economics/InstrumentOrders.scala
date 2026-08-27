package trading.economics

import trading.quantity.Dimension
import trading.quantity.refinement.PositiveWhole

private[economics] final class InstrumentOrdersImpl[
  O,
  D <: Dimension,
  B <: Dimension,
  Q <: Dimension
](
  authority: Instrument.OwnerAuthority[O])
  extends OrderCapability[O, InstrumentLots[O, D], InstrumentPrice[O, B, Q]]:

  private type Lots  = InstrumentLots[O, D]
  private type Price = InstrumentPrice[O, B, Q]

  val immediate: ImmediateActivation[O, Price] = authority.immediate
  val displayed: DisplayedVisibility[O, Lots]  = authority.displayed
  val hidden: HiddenVisibility[O, Lots]        = authority.hidden

  def fixedTrigger(
    reference: PriceReference,
    comparison: TriggerComparison,
    triggerPrice: Price
  ): FixedActivation[O, Price] =
    authority.fixed(reference, comparison, triggerPrice)

  def trailingTrigger(
    reference: PriceReference,
    comparison: TriggerComparison,
    offsetTicks: BigInt
  ): Either[EconomicsError, TrailingActivation[O, Price]] =
    PositiveWhole(offsetTicks)
      .left
      .map(_ => InvalidTrailingOffset(offsetTicks))
      .map(ticks => authority.trailing(reference, comparison, ticks))

  def limitPricing(limit: Price): LimitPricing[O, Price] = authority.limitPricing(limit)

  def peggedPricing(reference: PriceReference, offsetTicks: BigInt): PeggedPricing[O, Price] =
    authority.peggedPricing(reference, offsetTicks)

  def iceberg(displayedLots: Lots): IcebergVisibility[O, Lots] = authority.iceberg(displayedLots)

  def marketExecution(timeInForce: NonRestingTimeInForce): MarketExecution[O, Lots, Price] =
    authority.marketExecution(timeInForce)

  def pricedExecution(
    pricing: OrderPricing[O, Price],
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    visibility: PricedVisibility[O, Lots]
  ): PricedExecution[O, Lots, Price] =
    authority.pricedExecution(pricing, timeInForce, liquidityConstraint, visibility)

  def intent(
    side: Side,
    lots: Lots,
    positionEffect: PositionEffect
  ): OrderIntent[O, Lots] =
    authority.orderIntent(side, lots, positionEffect)

  def create(
    intent: OrderIntent[O, Lots],
    activation: OrderActivation[O, Price],
    execution: OrderExecution[O, Lots, Price]
  ): Either[EconomicsError, InstrumentOrder[O, Lots, Price]] =
    execution match
      case priced: PricedExecution[O, Lots, Price] =>
        priced.visibility match
          case iceberg: IcebergVisibility[O, Lots]
            if iceberg.displayedLots.count.unrefined > intent.lots.count.unrefined =>
            Left(
              InvalidOrder(
                OrderFailureReason.IcebergExceedsOrder(
                  iceberg.displayedLots.count.unrefined,
                  intent.lots.count.unrefined
                )
              )
            )
          case _: IcebergVisibility[O, Lots]
            if priced.timeInForce == TimeInForce.ImmediateOrCancel || priced.timeInForce == TimeInForce.FillOrKill =>
            Left(InvalidOrder(OrderFailureReason.NonRestingIceberg))
          case _ => Right(authority.order(intent, activation, execution))
      case _: MarketExecution[O, Lots, Price] => Right(authority.order(intent, activation, execution))

  def market(
    side: Side,
    lots: Lots,
    positionEffect: PositionEffect
  ): Either[EconomicsError, InstrumentOrder[O, Lots, Price]] =
    create(
      intent(side, lots, positionEffect),
      immediate,
      marketExecution(NonRestingTimeInForce.ImmediateOrCancel)
    )

  def limit(
    side: Side,
    lots: Lots,
    limit: Price,
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    positionEffect: PositionEffect,
    visibility: PricedVisibility[O, Lots]
  ): Either[EconomicsError, InstrumentOrder[O, Lots, Price]] =
    create(
      intent(side, lots, positionEffect),
      immediate,
      pricedExecution(limitPricing(limit), timeInForce, liquidityConstraint, visibility)
    )

  def stopMarket(
    side: Side,
    lots: Lots,
    trigger: OrderActivation[O, Price],
    positionEffect: PositionEffect
  ): Either[EconomicsError, InstrumentOrder[O, Lots, Price]] =
    trigger match
      case _: ImmediateActivation[O, Price] => Left(InvalidOrder(OrderFailureReason.StopRequiresTrigger))
      case _                                =>
        create(
          intent(side, lots, positionEffect),
          trigger,
          marketExecution(NonRestingTimeInForce.ImmediateOrCancel)
        )

  def stopLimit(
    side: Side,
    lots: Lots,
    trigger: OrderActivation[O, Price],
    limit: Price,
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    positionEffect: PositionEffect,
    visibility: PricedVisibility[O, Lots]
  ): Either[EconomicsError, InstrumentOrder[O, Lots, Price]] =
    trigger match
      case _: ImmediateActivation[O, Price] => Left(InvalidOrder(OrderFailureReason.StopRequiresTrigger))
      case _                                =>
        create(
          intent(side, lots, positionEffect),
          trigger,
          pricedExecution(limitPricing(limit), timeInForce, liquidityConstraint, visibility)
        )

end InstrumentOrdersImpl
