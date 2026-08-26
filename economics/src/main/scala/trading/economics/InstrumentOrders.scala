package trading.economics

private[economics] object InstrumentOrders:
  final case class VisibilityPlan[L](kind: VisibilityKind, displayedLots: Option[L])
  final case class ActivationPlan[P](
    kind: ActivationKind,
    reference: Option[PriceReference],
    comparison: Option[TriggerComparison],
    triggerPrice: Option[P],
    trailingOffsetTicks: Option[BigInt])
  final case class PriceInstructionPlan[P](
    kind: PriceInstructionKind,
    limit: Option[P],
    reference: Option[PriceReference],
    offsetTicks: Option[BigInt])
  final case class OrderPlan[L, A, P, V](
    side: Side,
    lots: L,
    activation: A,
    priceInstruction: P,
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    positionEffect: PositionEffect,
    visibility: V)

  def visibility[L](kind: VisibilityKind, displayedLots: Option[L] = None): VisibilityPlan[L] =
    VisibilityPlan(kind, displayedLots)

  def immediate[P]: ActivationPlan[P] = ActivationPlan(ActivationKind.Immediate, None, None, None, None)

  def fixedTrigger[P](
    reference: PriceReference,
    comparison: TriggerComparison,
    triggerPrice: P
  ): ActivationPlan[P] =
    ActivationPlan(ActivationKind.FixedTrigger, Some(reference), Some(comparison), Some(triggerPrice), None)

  def trailingTrigger[P](
    reference: PriceReference,
    comparison: TriggerComparison,
    offsetTicks: BigInt
  ): Either[EconomicsError, ActivationPlan[P]] =
    if offsetTicks.signum <= 0 then Left(InvalidTrailingOffset(offsetTicks))
    else
      Right(ActivationPlan(ActivationKind.TrailingTrigger, Some(reference), Some(comparison), None, Some(offsetTicks)))

  def marketInstruction[P]: PriceInstructionPlan[P] =
    PriceInstructionPlan(PriceInstructionKind.Market, None, None, None)

  def limitInstruction[P](limit: P): PriceInstructionPlan[P] =
    PriceInstructionPlan(PriceInstructionKind.Limit, Some(limit), None, None)

  def peggedInstruction[P](reference: PriceReference, offsetTicks: BigInt): PriceInstructionPlan[P] =
    PriceInstructionPlan(PriceInstructionKind.Pegged, None, Some(reference), Some(offsetTicks))

  def checked[L, A, P, V](
    side: Side,
    lots: L,
    activation: A,
    priceInstruction: P,
    isMarket: Boolean,
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    positionEffect: PositionEffect,
    visibility: V,
    visibilityKind: VisibilityKind,
    displayedLots: Option[BigInt],
    orderLots: BigInt
  ): Either[EconomicsError, OrderPlan[L, A, P, V]] =
    val nonResting = timeInForce == TimeInForce.ImmediateOrCancel || timeInForce == TimeInForce.FillOrKill
    validateOrder(isMarket, nonResting, liquidityConstraint, visibilityKind, displayedLots, orderLots).map: _ =>
      OrderPlan(side, lots, activation, priceInstruction, timeInForce, liquidityConstraint, positionEffect, visibility)

  def market[L, A, P, V](
    side: Side,
    lots: L,
    positionEffect: PositionEffect,
    immediate: A,
    marketInstruction: P,
    notApplicableVisibility: V,
    orderLots: BigInt
  ): Either[EconomicsError, OrderPlan[L, A, P, V]] =
    checked(
      side,
      lots,
      immediate,
      marketInstruction,
      isMarket = true,
      TimeInForce.ImmediateOrCancel,
      LiquidityConstraint.Unrestricted,
      positionEffect,
      notApplicableVisibility,
      VisibilityKind.NotApplicable,
      None,
      orderLots
    )

  def limit[L, A, P, V](
    side: Side,
    lots: L,
    limitInstruction: P,
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    positionEffect: PositionEffect,
    visibility: V,
    visibilityKind: VisibilityKind,
    displayedLots: Option[BigInt],
    orderLots: BigInt,
    immediate: A
  ): Either[EconomicsError, OrderPlan[L, A, P, V]] =
    checked(
      side,
      lots,
      immediate,
      limitInstruction,
      isMarket = false,
      timeInForce,
      liquidityConstraint,
      positionEffect,
      visibility,
      visibilityKind,
      displayedLots,
      orderLots
    )

  def stopMarket[L, A, P, V](
    side: Side,
    lots: L,
    trigger: A,
    triggerKind: ActivationKind,
    positionEffect: PositionEffect,
    marketInstruction: P,
    notApplicableVisibility: V,
    orderLots: BigInt
  ): Either[EconomicsError, OrderPlan[L, A, P, V]] =
    requireTrigger(triggerKind, "stop-market").flatMap: _ =>
      market(side, lots, positionEffect, trigger, marketInstruction, notApplicableVisibility, orderLots)

  def stopLimit[L, A, P, V](
    side: Side,
    lots: L,
    trigger: A,
    triggerKind: ActivationKind,
    limitInstruction: P,
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    positionEffect: PositionEffect,
    visibility: V,
    visibilityKind: VisibilityKind,
    displayedLots: Option[BigInt],
    orderLots: BigInt
  ): Either[EconomicsError, OrderPlan[L, A, P, V]] =
    requireTrigger(triggerKind, "stop-limit").flatMap: _ =>
      checked(
        side,
        lots,
        trigger,
        limitInstruction,
        isMarket = false,
        timeInForce,
        liquidityConstraint,
        positionEffect,
        visibility,
        visibilityKind,
        displayedLots,
        orderLots
      )

  def requireTrigger(kind: ActivationKind, orderKind: String): Either[EconomicsError, Unit] =
    if kind == ActivationKind.Immediate then Left(InvalidOrder(s"$orderKind requires a trigger")) else Right(())

  private def validateOrder(
    isMarket: Boolean,
    nonResting: Boolean,
    liquidityConstraint: LiquidityConstraint,
    visibility: VisibilityKind,
    displayedLots: Option[BigInt],
    orderLots: BigInt
  ): Either[EconomicsError, Unit] =
    if isMarket && liquidityConstraint == LiquidityConstraint.MakerOnly then
      Left(InvalidOrder("market orders cannot be maker-only"))
    else if isMarket && !nonResting then
      Left(InvalidOrder("market orders require immediate-or-cancel or fill-or-kill"))
    else if isMarket && visibility != VisibilityKind.NotApplicable then
      Left(InvalidOrder("market orders require not-applicable visibility"))
    else if !isMarket && visibility == VisibilityKind.NotApplicable then
      Left(InvalidOrder("priced orders require explicit visibility"))
    else if nonResting && visibility == VisibilityKind.Iceberg then
      Left(InvalidOrder("non-resting orders cannot be iceberg"))
    else if displayedLots.exists(_ > orderLots) then
      Left(InvalidOrder("iceberg displayed lots cannot exceed order lots"))
    else Right(())

end InstrumentOrders
