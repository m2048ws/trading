package trading.economics

import trading.quantity.*
import trading.quantity.runtime.RegisteredGridRef

private[economics] final class InstrumentScenariosImpl[
  O,
  D <: Dimension,
  B <: Dimension,
  Q <: Dimension,
  S <: Dimension
](
  authority: Instrument.OwnerAuthority[O],
  positionGrid: RegisteredGridRef[D])
  extends ScenarioCapability[
    O,
    InstrumentLots[O, D],
    InstrumentPrice[O, B, Q],
    InstrumentMarketState[O, B, Q, S],
    InstrumentPosition[O, D]
  ]:

  private type Lots     = InstrumentLots[O, D]
  private type Price    = InstrumentPrice[O, B, Q]
  private type Market   = InstrumentMarketState[O, B, Q, S]
  private type Position = InstrumentPosition[O, D]

  val immediate: ImmediateAssumption[O, Price]         = authority.immediateAssumption
  val directPricing: DirectPricingAssumption[O, Price] = authority.directPricing

  def fixedEvidence(reference: PriceReference, observedPrice: Price): FixedTriggerEvidence[O, Price] =
    authority.fixedEvidence(reference, observedPrice)

  def trailingEvidence(
    reference: PriceReference,
    favorableExtreme: Price,
    observedPrice: Price
  ): TrailingTriggerEvidence[O, Price] =
    authority.trailingEvidence(reference, favorableExtreme, observedPrice)

  def triggered(evidence: TriggerEvidence[O, Price]): TriggeredAssumption[O, Price] =
    authority.triggeredAssumption(evidence)

  def pegResolution(
    reference: PriceReference,
    referencePrice: Price,
    resolvedLimit: Price
  ): PegResolution[O, Price] =
    authority.pegResolution(reference, referencePrice, resolvedLimit)

  def resolvedPeg(resolution: PegResolution[O, Price]): ResolvedPegAssumption[O, Price] =
    authority.resolvedPeg(resolution)

  def slice(lots: Lots, market: Market, role: LiquidityRole): InstrumentLiquiditySlice[O, Lots, Market] =
    authority.liquiditySlice(lots, market, role)

  def assumptions(
    activation: ActivationAssumption[O, Price],
    pricing: PricingAssumption[O, Price],
    matchedSlices: Vector[InstrumentLiquiditySlice[O, Lots, Market]]
  ): ScenarioAssumptions[O, Lots, Price, Market] =
    authority.scenarioAssumptions(activation, pricing, matchedSlices)

  def order(
    order: InstrumentOrder[O, Lots, Price],
    assumptions: ScenarioAssumptions[O, Lots, Price, Market]
  ): Either[EconomicsError, InstrumentOrderScenario[O, Lots, Price, Market, Position]] =
    for
      _              <- validateSliceTotals(order.intent.lots, assumptions.matchedSlices)
      _              <- validateActivation(order.activation, assumptions.activation)
      effectiveLimit <- validatePricing(order.execution, assumptions.pricing)
      _              <- validateSlices(order, assumptions.matchedSlices, effectiveLimit)
    yield
      val coordinate = order.intent.side.sign * order.intent.lots.count.unrefined
      val change     = authority.position(positionGrid)(positionGrid.fromCoordinate(coordinate))
      authority.orderScenario(order, assumptions, change)

  def roundTrip(
    entry: InstrumentOrderScenario[O, Lots, Price, Market, Position],
    exit: InstrumentOrderScenario[O, Lots, Price, Market, Position]
  ): Either[EconomicsError, InstrumentRoundTripScenario[O, Lots, Price, Market, Position]] =
    val entryCount = entry.positionChange.count
    val exitCount  = exit.positionChange.count
    if entryCount + exitCount != 0 then Left(InvalidRoundTrip(entryCount, exitCount))
    else Right(authority.roundTrip(entry, exit, entry.positionChange))

  private def validateSliceTotals(
    expected: Lots,
    slices: Vector[InstrumentLiquiditySlice[O, Lots, Market]]
  ): Either[EconomicsError, Unit] =
    val supplied = slices.foldLeft(BigInt(0))((total, slice) => total + slice.lots.count.unrefined)
    if slices.isEmpty then Left(InvalidScenario(ScenarioFailureReason.NoSlices))
    else if supplied != expected.count.unrefined then
      Left(InvalidScenario(ScenarioFailureReason.SliceLotsMismatch(expected.count.unrefined, supplied)))
    else Right(())

  private def validateActivation(
    activation: OrderActivation[O, Price],
    assumption: ActivationAssumption[O, Price]
  ): Either[EconomicsError, Unit] =
    (activation, assumption) match
      case (_: ImmediateActivation[O, Price], _: ImmediateAssumption[O, Price]) => Right(())
      case (_: ImmediateActivation[O, Price], _)                                =>
        Left(InvalidScenario(ScenarioFailureReason.UnexpectedTriggerEvidence))
      case (_: FixedActivation[O, Price], _: ImmediateAssumption[O, Price]) =>
        Left(InvalidScenario(ScenarioFailureReason.MissingFixedTriggerEvidence))
      case (fixed: FixedActivation[O, Price], triggered: TriggeredAssumption[O, Price]) =>
        triggered.evidence match
          case evidence: FixedTriggerEvidence[O, Price] =>
            if evidence.reference != fixed.reference then
              Left(InvalidScenario(ScenarioFailureReason.TriggerReferenceMismatch))
            else if comparisonSatisfied(fixed.comparison, evidence.observedPrice.ticks.unrefined,
                fixed.triggerPrice.ticks.unrefined)
            then Right(())
            else Left(InvalidScenario(ScenarioFailureReason.FixedTriggerUnsatisfied))
          case _: TrailingTriggerEvidence[O, Price] =>
            Left(InvalidScenario(ScenarioFailureReason.FixedEvidenceExpected))
      case (_: TrailingActivation[O, Price], _: ImmediateAssumption[O, Price]) =>
        Left(InvalidScenario(ScenarioFailureReason.MissingTrailingTriggerEvidence))
      case (trailing: TrailingActivation[O, Price], triggered: TriggeredAssumption[O, Price]) =>
        triggered.evidence match
          case _: FixedTriggerEvidence[O, Price] =>
            Left(InvalidScenario(ScenarioFailureReason.TrailingEvidenceExpected))
          case evidence: TrailingTriggerEvidence[O, Price] =>
            val extreme   = evidence.favorableExtreme.ticks.unrefined
            val threshold = trailing.comparison match
              case TriggerComparison.AtOrAbove => extreme + trailing.offsetTicks.unrefined
              case TriggerComparison.AtOrBelow => extreme - trailing.offsetTicks.unrefined
            if evidence.reference != trailing.reference then
              Left(InvalidScenario(ScenarioFailureReason.TriggerReferenceMismatch))
            else if threshold.signum <= 0 then
              Left(InvalidScenario(ScenarioFailureReason.TrailingThresholdNonPositive))
            else if comparisonSatisfied(trailing.comparison, evidence.observedPrice.ticks.unrefined, threshold) then
              Right(())
            else Left(InvalidScenario(ScenarioFailureReason.TrailingTriggerUnsatisfied))

  private def validatePricing(
    execution: OrderExecution[O, Lots, Price],
    assumption: PricingAssumption[O, Price]
  ): Either[EconomicsError, Option[BigInt]] =
    execution match
      case _: MarketExecution[O, Lots, Price] =>
        assumption match
          case _: DirectPricingAssumption[O, Price] => Right(None)
          case _: ResolvedPegAssumption[O, Price]   =>
            Left(InvalidScenario(ScenarioFailureReason.UnexpectedPegResolution))
      case priced: PricedExecution[O, Lots, Price] =>
        priced.pricing match
          case limit: LimitPricing[O, Price] =>
            assumption match
              case _: DirectPricingAssumption[O, Price] => Right(Some(limit.limit.ticks.unrefined))
              case _: ResolvedPegAssumption[O, Price]   =>
                Left(InvalidScenario(ScenarioFailureReason.UnexpectedPegResolution))
          case pegged: PeggedPricing[O, Price] =>
            assumption match
              case _: DirectPricingAssumption[O, Price] =>
                Left(InvalidScenario(ScenarioFailureReason.MissingPegResolution))
              case resolved: ResolvedPegAssumption[O, Price] =>
                val evidence = resolved.resolution
                if evidence.reference != pegged.reference then
                  Left(InvalidScenario(ScenarioFailureReason.PegReferenceMismatch))
                else if evidence.resolvedLimit.ticks.unrefined - evidence.referencePrice.ticks.unrefined !=
                    pegged.offsetTicks
                then
                  Left(InvalidScenario(ScenarioFailureReason.PegOffsetMismatch))
                else Right(Some(evidence.resolvedLimit.ticks.unrefined))

  private def validateSlices(
    order: InstrumentOrder[O, Lots, Price],
    slices: Vector[InstrumentLiquiditySlice[O, Lots, Market]],
    effectiveLimit: Option[BigInt]
  ): Either[EconomicsError, Unit] =
    slices.zipWithIndex.collectFirst:
      case (slice, index)
        if order.execution.isInstanceOf[MarketExecution[O, Lots, Price]] && slice.role != LiquidityRole.Taker =>
        InvalidScenario(ScenarioFailureReason.MarketSliceNotTaker, Some(index))
      case (slice, index)
        if order.execution match
          case priced: PricedExecution[O, Lots, Price] =>
            priced.liquidityConstraint == LiquidityConstraint.MakerOnly && slice.role != LiquidityRole.Maker
          case _ =>
            false
        =>
        InvalidScenario(ScenarioFailureReason.MakerOnlySliceNotMaker, Some(index))
      case (slice, index)
        if effectiveLimit.exists(limit =>
          order.intent.side match
            case Side.Buy  => slice.market.price.ticks.unrefined > limit
            case Side.Sell => slice.market.price.ticks.unrefined < limit
        ) =>
        InvalidScenario(ScenarioFailureReason.SliceWorseThanLimit, Some(index))
    match
      case Some(error) => Left(error)
      case None        => Right(())

  private def comparisonSatisfied(comparison: TriggerComparison, observed: BigInt, threshold: BigInt): Boolean =
    comparison match
      case TriggerComparison.AtOrAbove => observed >= threshold
      case TriggerComparison.AtOrBelow => observed <= threshold

end InstrumentScenariosImpl
