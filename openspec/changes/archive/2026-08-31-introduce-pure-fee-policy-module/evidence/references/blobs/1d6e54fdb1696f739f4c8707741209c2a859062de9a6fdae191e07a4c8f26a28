package trading.fee.policy

import cats.syntax.all.*

import trading.economics.instrument.*
import trading.fee.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.NonNegative
import trading.reference.*
import trading.scenario.*

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
  type Scenario   = _root_.trading.scenario.OrderScenario[D, B, Q, Market]
  type RoundTrip  = _root_.trading.scenario.RoundTripScenario[D, B, Q, Market]
  type Policy[+E] = _root_.trading.fee.FeePolicy[E, D, B, Q, S]

  def denomination(
    asset: Asset
  )(
    grid: GridHandle[? <: Dim],
    policy: QuantizationPolicy
  ): Either[FeeValueError, FeeDenomination[asset.D]] =
    FeeDenomination.create(instrument)(asset, grid, policy)

  def percentage[FD <: Dim](
    denomination: FeeDenomination[FD],
    kind: FeeKind,
    basis: NonNegative[Quantity[FD]],
    rate: FeeRate
  ): Either[FeeValueError, Fee[FD]] =
    Fee
      .create(instrument)(denomination, kind, FeeCalculation.percentage(basis, rate))

  def minimumCharge[FD <: Dim](
    contribution: Quantity[FD],
    minimum: NonNegative[Quantity[FD]]
  ): Quantity[FD] =
    FeeCalculation.minimumCharge(contribution, minimum)

  /** Evaluate downstream scenario and policy inputs before invoking pure contribution/PnL composition. */
  def pnl[E](
    roundTrip: RoundTrip,
    policy: Policy[E]
  ): Either[FeeOrchestrationError[E], instrument.Pnl] =
    for
      _ <- checkIdentities(
             FeeOrchestrationLocation.RoundTrip     -> roundTrip.instrumentId,
             FeeOrchestrationLocation.EntryScenario -> roundTrip.entry.instrumentId,
             FeeOrchestrationLocation.ExitScenario  -> roundTrip.exit.instrumentId,
             FeeOrchestrationLocation.Policy        -> policy.instrumentId
           )
      pricePnl <- ScenarioValuation
                    .pricePnl(instrument)(roundTrip)
                    .left
                    .map(FeeOrchestrationValuationFailure(_))
      entryFees          <- assess(policy, RoundTripLeg.Entry, roundTrip.entry)
      exitFees           <- assess(policy, RoundTripLeg.Exit, roundTrip.exit)
      entryContributions <- entryFees.fees.traverse(convertAssessed(RoundTripLeg.Entry, _))
      exitContributions  <- exitFees.fees.traverse(convertAssessed(RoundTripLeg.Exit, _))
      result             <- Pnl
                  .create(instrument)(pricePnl, entryContributions ++ exitContributions)
                  .left
                  .map(FeeOrchestrationPnlFailure(_))
    yield result

  private def assess[E](
    policy: Policy[E],
    leg: RoundTripLeg,
    scenario: Scenario
  ): Either[FeeOrchestrationError[E], ScenarioFees[D, B, Q, S]] =
    FeeAssessment
      .evaluate(instrument)(scenario, policy)
      .left
      .map(FeeOrchestrationAssessmentFailure(leg, _))

  private def convertAssessed(
    leg: RoundTripLeg,
    assessed: AssessedFee[D, B, Q, S]
  ): Either[FeeOrchestrationError[Nothing], SettledFeeContribution[instrument.roles.settle.D]] =
    convertCaptured(leg, assessed)

  private def convertCaptured[FD <: Dim](
    leg: RoundTripLeg,
    assessed: AssessedFee[D, B, Q, S] { type D = FD }
  ): Either[FeeOrchestrationError[Nothing], SettledFeeContribution[instrument.roles.settle.D]] =
    SettledFeeContribution
      .convert(instrument)(assessed.fee, assessed.sourceSlice.market)
      .left
      .map(cause => FeeOrchestrationContributionFailure(leg, assessed.sourceIndex, cause))

  private def checkIdentities(
    supplied: (FeeOrchestrationLocation, InstrumentId)*
  ): Either[FeeOrchestrationError[Nothing], Unit] =
    supplied.collectFirst:
      case (location, id) if id != instrumentId =>
        FeeOrchestrationIdentity(location, instrumentId, id)
    match
      case Some(error) => Left(error)
      case None        => Right(())
end FeeOrchestration

object FeeOrchestration:
  def apply[I <: Instrument](instrument: I): FeeOrchestration[instrument.type] =
    new FeeOrchestration[instrument.type](instrument)
