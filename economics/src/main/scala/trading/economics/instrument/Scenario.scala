package trading.economics.instrument

import trading.quantity.*
import trading.quantity.runtime.RegisteredGridRef

/** Fee classification of one complete scenario slice. */
enum LiquidityRole:
  case Maker, Taker

/** Entry or exit attribution retained on converted fee contributions. */
enum ScenarioLeg:
  case Entry, Exit

sealed trait TriggerEvidence[+P]:
  def reference: PriceReference
  def observedPrice: P

final case class FixedTriggerEvidence[P](reference: PriceReference, observedPrice: P) extends TriggerEvidence[P]
final case class TrailingTriggerEvidence[P](reference: PriceReference, favorableExtreme: P, observedPrice: P)
  extends TriggerEvidence[P]

sealed trait ActivationAssumption[+P]
case object ImmediateAssumption                                       extends ActivationAssumption[Nothing]
final case class TriggeredAssumption[P](evidence: TriggerEvidence[P]) extends ActivationAssumption[P]

final case class PegResolution[P](reference: PriceReference, referencePrice: P, resolvedLimit: P)

sealed trait PricingAssumption[+P]
case object DirectPricingAssumption                                     extends PricingAssumption[Nothing]
final case class ResolvedPegAssumption[P](resolution: PegResolution[P]) extends PricingAssumption[P]

final case class LiquiditySlice[L, M] private[instrument] (
  instrumentId: InstrumentId,
  lots: L,
  market: M,
  role: LiquidityRole)

final case class ScenarioAssumptions[L, P, M](
  instrumentId: InstrumentId,
  activation: ActivationAssumption[P],
  pricing: PricingAssumption[P],
  matchedSlices: Vector[LiquiditySlice[L, M]])

final case class OrderScenario[L, P, M, Pos] private[instrument] (
  instrumentId: InstrumentId,
  order: Order[L, P],
  assumptions: ScenarioAssumptions[L, P, M],
  positionChange: Pos)

final case class RoundTripScenario[L, P, M, Pos] private[instrument] (
  instrumentId: InstrumentId,
  entry: OrderScenario[L, P, M, Pos],
  exit: OrderScenario[L, P, M, Pos],
  heldPosition: Pos)

final class Scenarios[D <: Dim, B <: Dim, Q <: Dim, S <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  positionGrid: RegisteredGridRef[D]):

  private type Lots     = _root_.trading.economics.instrument.Lots[D]
  private type Price    = _root_.trading.economics.instrument.Price[B, Q]
  private type Market   = _root_.trading.economics.instrument.MarketState[B, Q, S]
  private type Position = _root_.trading.economics.instrument.Position[D]

  val immediate: ActivationAssumption[Price]  = ImmediateAssumption
  val directPricing: PricingAssumption[Price] = DirectPricingAssumption

  def fixedEvidence(reference: PriceReference, observedPrice: Price): FixedTriggerEvidence[Price] =
    FixedTriggerEvidence(reference, observedPrice)

  def trailingEvidence(
    reference: PriceReference,
    favorableExtreme: Price,
    observedPrice: Price
  ): TrailingTriggerEvidence[Price] =
    TrailingTriggerEvidence(reference, favorableExtreme, observedPrice)

  def triggered(evidence: TriggerEvidence[Price]): TriggeredAssumption[Price] =
    TriggeredAssumption(evidence)

  def pegResolution(
    reference: PriceReference,
    referencePrice: Price,
    resolvedLimit: Price
  ): PegResolution[Price] =
    PegResolution(reference, referencePrice, resolvedLimit)

  def resolvedPeg(resolution: PegResolution[Price]): ResolvedPegAssumption[Price] =
    ResolvedPegAssumption(resolution)

  def slice(
    lots: Lots,
    market: Market,
    role: LiquidityRole
  ): Either[EconomicsError, LiquiditySlice[Lots, Market]] =
    IdentityChecks
      .check("scenario.slice", instrumentId, "lots" -> lots.instrumentId, "market" -> market.instrumentId)
      .map(_ => LiquiditySlice(instrumentId, lots, market, role))

  def assumptions(
    activation: ActivationAssumption[Price],
    pricing: PricingAssumption[Price],
    matchedSlices: Vector[LiquiditySlice[Lots, Market]]
  ): ScenarioAssumptions[Lots, Price, Market] =
    ScenarioAssumptions(instrumentId, activation, pricing, matchedSlices)

  def order(
    order: Order[Lots, Price],
    assumptions: ScenarioAssumptions[Lots, Price, Market]
  ): Either[EconomicsError, OrderScenario[Lots, Price, Market, Position]] =
    for
      _              <- validateIdentities(order, assumptions)
      _              <- validateSliceTotals(order.intent.lots, assumptions.matchedSlices)
      _              <- validateActivation(order.activation, assumptions.activation)
      effectiveLimit <- validatePricing(order.execution, assumptions.pricing)
      _              <- validateSlices(order, assumptions.matchedSlices, effectiveLimit)
    yield
      val coordinate = order.intent.side.sign * order.intent.lots.count.unrefined
      val change     = Position(
        instrumentId,
        coordinate,
        positionGrid.asQuantity(positionGrid.fromCoordinate(coordinate))
      )
      OrderScenario(instrumentId, order, assumptions, change)

  def roundTrip(
    entry: OrderScenario[Lots, Price, Market, Position],
    exit: OrderScenario[Lots, Price, Market, Position]
  ): Either[EconomicsError, RoundTripScenario[Lots, Price, Market, Position]] =
    for
      _ <- IdentityChecks.check(
             "roundTrip",
             instrumentId,
             "entry"                -> entry.instrumentId,
             "entry.positionChange" -> entry.positionChange.instrumentId,
             "exit"                 -> exit.instrumentId,
             "exit.positionChange"  -> exit.positionChange.instrumentId
           )
      result <-
        val entryCount = entry.positionChange.count
        val exitCount  = exit.positionChange.count
        if entryCount + exitCount != 0 then Left(InvalidRoundTrip(entryCount, exitCount))
        else Right(RoundTripScenario(instrumentId, entry, exit, entry.positionChange))
    yield result

  private def validateIdentities(
    order: Order[Lots, Price],
    assumptions: ScenarioAssumptions[Lots, Price, Market]
  ): Either[EconomicsError, Unit] =
    val supplied = Vector.newBuilder[(String, InstrumentId)]
    supplied += "order"       -> order.instrumentId
    supplied += "assumptions" -> assumptions.instrumentId
    assumptions.activation match
      case TriggeredAssumption(FixedTriggerEvidence(_, observed)) =>
        supplied += "activation.observed" -> observed.instrumentId
      case TriggeredAssumption(TrailingTriggerEvidence(_, extreme, observed)) =>
        supplied += "activation.extreme"  -> extreme.instrumentId
        supplied += "activation.observed" -> observed.instrumentId
      case ImmediateAssumption => ()
    assumptions.pricing match
      case ResolvedPegAssumption(PegResolution(_, reference, resolved)) =>
        supplied += "pricing.reference" -> reference.instrumentId
        supplied += "pricing.resolved"  -> resolved.instrumentId
      case DirectPricingAssumption => ()
    assumptions.matchedSlices.zipWithIndex.foreach: (slice, index) =>
      supplied += s"slices[$index]"        -> slice.instrumentId
      supplied += s"slices[$index].lots"   -> slice.lots.instrumentId
      supplied += s"slices[$index].market" -> slice.market.instrumentId
      supplied += s"slices[$index].price"  -> slice.market.price.instrumentId
    IdentityChecks.check("scenario", instrumentId, supplied.result()*)
  end validateIdentities

  private def validateSliceTotals(
    expected: Lots,
    slices: Vector[LiquiditySlice[Lots, Market]]
  ): Either[EconomicsError, Unit] =
    val supplied = slices.foldLeft(BigInt(0))((total, slice) => total + slice.lots.count.unrefined)
    if slices.isEmpty then Left(InvalidScenario(ScenarioFailureReason.NoSlices))
    else if supplied != expected.count.unrefined then
      Left(InvalidScenario(ScenarioFailureReason.SliceLotsMismatch(expected.count.unrefined, supplied)))
    else Right(())

  private def validateActivation(
    activation: OrderActivation[Price],
    assumption: ActivationAssumption[Price]
  ): Either[EconomicsError, Unit] =
    (activation, assumption) match
      case (ImmediateActivation, ImmediateAssumption) => Right(())
      case (ImmediateActivation, _)                   =>
        Left(InvalidScenario(ScenarioFailureReason.UnexpectedTriggerEvidence))
      case (_: FixedActivation[Price], ImmediateAssumption) =>
        Left(InvalidScenario(ScenarioFailureReason.MissingFixedTriggerEvidence))
      case (fixed: FixedActivation[Price], TriggeredAssumption(evidence: FixedTriggerEvidence[Price])) =>
        if evidence.reference != fixed.reference then
          Left(InvalidScenario(ScenarioFailureReason.TriggerReferenceMismatch))
        else if comparisonSatisfied(
            fixed.comparison,
            evidence.observedPrice.ticks.unrefined,
            fixed.triggerPrice.ticks.unrefined
          )
        then Right(())
        else Left(InvalidScenario(ScenarioFailureReason.FixedTriggerUnsatisfied))
      case (_: FixedActivation[Price], TriggeredAssumption(_: TrailingTriggerEvidence[Price])) =>
        Left(InvalidScenario(ScenarioFailureReason.FixedEvidenceExpected))
      case (_: TrailingActivation[Price], ImmediateAssumption) =>
        Left(InvalidScenario(ScenarioFailureReason.MissingTrailingTriggerEvidence))
      case (_: TrailingActivation[Price], TriggeredAssumption(_: FixedTriggerEvidence[Price])) =>
        Left(InvalidScenario(ScenarioFailureReason.TrailingEvidenceExpected))
      case (trailing: TrailingActivation[Price], TriggeredAssumption(evidence: TrailingTriggerEvidence[Price])) =>
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
    execution: OrderExecution[Lots, Price],
    assumption: PricingAssumption[Price]
  ): Either[EconomicsError, Option[BigInt]] =
    execution match
      case _: MarketExecution =>
        assumption match
          case DirectPricingAssumption         => Right(None)
          case _: ResolvedPegAssumption[Price] =>
            Left(InvalidScenario(ScenarioFailureReason.UnexpectedPegResolution))
      case priced: PricedExecution[Lots, Price] =>
        priced.pricing match
          case limit: LimitPricing[Price] =>
            assumption match
              case DirectPricingAssumption         => Right(Some(limit.limit.ticks.unrefined))
              case _: ResolvedPegAssumption[Price] =>
                Left(InvalidScenario(ScenarioFailureReason.UnexpectedPegResolution))
          case pegged: PeggedPricing =>
            assumption match
              case DirectPricingAssumption =>
                Left(InvalidScenario(ScenarioFailureReason.MissingPegResolution))
              case ResolvedPegAssumption(evidence) =>
                if evidence.reference != pegged.reference then
                  Left(InvalidScenario(ScenarioFailureReason.PegReferenceMismatch))
                else if evidence.resolvedLimit.ticks.unrefined - evidence.referencePrice.ticks.unrefined !=
                    pegged.offsetTicks
                then
                  Left(InvalidScenario(ScenarioFailureReason.PegOffsetMismatch))
                else Right(Some(evidence.resolvedLimit.ticks.unrefined))

  private def validateSlices(
    order: Order[Lots, Price],
    slices: Vector[LiquiditySlice[Lots, Market]],
    effectiveLimit: Option[BigInt]
  ): Either[EconomicsError, Unit] =
    slices.zipWithIndex.collectFirst:
      case (slice, index) if order.execution.isInstanceOf[MarketExecution] && slice.role != LiquidityRole.Taker =>
        InvalidScenario(ScenarioFailureReason.MarketSliceNotTaker, Some(index))
      case (slice, index)
        if order.execution match
          case priced: PricedExecution[Lots, Price] =>
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

end Scenarios
