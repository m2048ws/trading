package trading.economics

private[economics] object OrderScenarioRules:
  def validateOrder(
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

  def validateSliceTotals(orderLots: BigInt, sliceLots: Vector[BigInt]): Either[EconomicsError, Unit] =
    if sliceLots.isEmpty then Left(InvalidScenario("complete scenario requires at least one slice"))
    else if sliceLots.sum != orderLots then Left(InvalidScenario("slice lots must sum exactly to order lots"))
    else Right(())

  def comparisonSatisfied(comparison: TriggerComparison, observed: BigInt, threshold: BigInt): Boolean =
    comparison match
      case TriggerComparison.AtOrAbove => observed >= threshold
      case TriggerComparison.AtOrBelow => observed <= threshold

  def validatePeg(
    referenceMatches: Boolean,
    resolvedOffset: BigInt,
    expectedOffset: BigInt
  ): Either[EconomicsError, Unit] =
    if !referenceMatches then Left(InvalidScenario("peg reference does not match"))
    else if resolvedOffset != expectedOffset then Left(InvalidScenario("resolved peg tick offset disagrees"))
    else Right(())

  def validateRoundTrip(entryChange: BigInt, exitChange: BigInt): Either[EconomicsError, Unit] =
    if entryChange + exitChange != 0 then Left(InvalidRoundTrip(entryChange, exitChange)) else Right(())

end OrderScenarioRules
