package trading.fee.policy

import cats.syntax.all.*

import trading.economics.instrument.*
import trading.fee.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.NonNegative
import trading.reference.*
import trading.scenario.*

/** Downstream attribution of one exact fee to one scenario market state. */
final case class FeeLine[D <: Dim, M] private[policy] (
  instrumentId: InstrumentId,
  fee: Fee[D],
  sourceSliceIndex: Int,
  sourceMarket: M)

/** Provisional fee-inclusive orchestration retained until the dedicated assessment and PnL Task Groups replace it. */
final class FeeOrchestration[I <: Instrument] private[policy] (val instrument: I):

  private val instrumentId = instrument.identity.id

  type D          = instrument.roles.position.D
  type B          = instrument.roles.base.D
  type Q          = instrument.roles.quote.D
  type S          = instrument.roles.settle.D
  type Lots       = instrument.Lots
  type Price      = instrument.Price
  type Market     = instrument.MarketState
  type Position   = instrument.PositionLots
  type Scenario   = _root_.trading.scenario.OrderScenario[D, B, Q, Market]
  type RoundTrip  = _root_.trading.scenario.RoundTripScenario[D, B, Q, Market]
  type Policy[+E] = _root_.trading.fee.FeePolicy[E, D, B, Q, S]

  def denomination(
    asset: Asset
  )(
    grid: GridHandle[? <: Dim],
    policy: QuantizationPolicy
  ): Either[FeePolicyError, FeeDenomination[asset.D]] =
    FeeDenomination.create(instrument)(asset, grid, policy).left.map(FeeValueFailure(_))

  def percentage[FD <: Dim](
    denomination: FeeDenomination[FD],
    kind: FeeKind,
    basis: NonNegative[Quantity[FD]],
    rate: FeeRate
  ): Either[FeePolicyError, Fee[FD]] =
    Fee
      .create(instrument)(denomination, kind, FeeCalculation.percentage(basis, rate))
      .left
      .map(FeeValueFailure(_))

  def minimumCharge[FD <: Dim](
    contribution: Quantity[FD],
    minimum: NonNegative[Quantity[FD]]
  ): Quantity[FD] =
    FeeCalculation.minimumCharge(contribution, minimum)

  private def line[FD <: Dim](
    scenario: Scenario,
    sourceSlice: SliceIndex,
    fee: Fee[FD]
  ): Either[FeePolicyError, FeeLine[FD, Market]] =
    val sourceSliceIndex = sourceSlice.value
    val slices           = scenario.matchedSlices.toVector
    if sourceSliceIndex < 0 || sourceSliceIndex >= slices.size then
      Left(InvalidFeeAttribution(sourceSliceIndex, slices.size))
    else
      val market = slices(sourceSliceIndex).market
      checkIdentities(
        "line",
        "scenario"     -> scenario.instrumentId,
        "fee"          -> fee.instrumentId,
        "denomination" -> fee.denomination.instrumentId,
        "market"       -> market.instrumentId
      ).map(_ => FeeLine(instrumentId, fee, sourceSliceIndex, market))

  /** Evaluate downstream scenario and policy inputs before invoking pure contribution/PnL composition. */
  def pnl(roundTrip: RoundTrip, policy: Policy[FeePolicyError]): Either[FeePolicyError, instrument.Pnl] =
    for
      _ <- checkIdentities(
             "pnl",
             "roundTrip" -> roundTrip.instrumentId,
             "entry"     -> roundTrip.entry.instrumentId,
             "exit"      -> roundTrip.exit.instrumentId,
             "policy"    -> policy.instrumentId
           )
      entryValue <- scenarioSignedValue(roundTrip.entry)
      exitSigned <- scenarioSignedValue(roundTrip.exit)
      pricePnl   <- PricePnl
                    .fromValues(instrument)(roundTrip.heldPosition, entryValue, exitSigned * Rational(-1))
                    .left
                    .map(FeeValuationFailure(_))
      entryLines         <- assessAndValidate(policy, roundTrip.entry)
      exitLines          <- assessAndValidate(policy, roundTrip.exit)
      entryContributions <- entryLines.traverse(convertLine(ScenarioLeg.Entry, _))
      exitContributions  <- exitLines.traverse(convertLine(ScenarioLeg.Exit, _))
      result             <- Pnl
                  .create(instrument)(pricePnl, entryContributions ++ exitContributions)
                  .left
                  .map(FeePnlFailure(_))
    yield result

  private def scenarioSignedValue(
    scenario: Scenario
  ): Either[FeePolicyError, Quantity[instrument.roles.settle.D]] =
    scenario.matchedSlices.toVector
      .traverse: slice =>
        val coordinate = scenario.order.intent.side.sign * slice.lots.count.unrefined
        val position   = PositionLots.fromCoordinate(instrument)(coordinate)
        Valuation
          .positionValue(instrument)(position, slice.market)
          .left
          .map(FeeValuationFailure(_))
      .map(
        _.foldLeft(
          Quantity.zero[instrument.roles.settle.D](using instrument.roles.settle.dimension.ref)
        )(_ + _)
      )

  private def assessAndValidate(
    policy: Policy[FeePolicyError],
    scenario: Scenario
  ): Either[FeePolicyError, Vector[FeeLine[? <: Dim, Market]]] =
    policy
      .evaluate(scenario)
      .left
      .map(FeePolicyFailures(_))
      .flatMap(_.traverse(directive => line(scenario, directive.sourceSlice, directive.fee)))

  private def convertLine(
    leg: ScenarioLeg,
    line: FeeLine[? <: Dim, Market]
  ): Either[FeePolicyError, SettledFeeContribution[instrument.roles.settle.D]] =
    convertCaptured(leg, line)

  private def convertCaptured[FD <: Dim](
    leg: ScenarioLeg,
    line: FeeLine[FD, Market]
  ): Either[FeePolicyError, SettledFeeContribution[instrument.roles.settle.D]] =
    SettledFeeContribution
      .convert(instrument)(line.fee, line.sourceMarket)
      .left
      .map(cause => FeeContributionFailure(leg, line.sourceSliceIndex, cause))

  private def checkIdentities(
    context: String,
    supplied: (String, InstrumentId)*
  ): Either[FeePolicyError, Unit] =
    supplied.collectFirst:
      case (name, id) if id != instrumentId =>
        FeePolicyInstrumentMismatch(s"$context.$name", instrumentId, id)
    match
      case Some(error) => Left(error)
      case None        => Right(())
end FeeOrchestration

object FeeOrchestration:
  def apply[I <: Instrument](instrument: I): FeeOrchestration[instrument.type] =
    new FeeOrchestration[instrument.type](instrument)
