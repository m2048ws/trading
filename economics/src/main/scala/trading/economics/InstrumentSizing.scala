package trading.economics

import trading.quantity.*
import trading.quantity.refinement.PositiveWhole

private[economics] object InstrumentSizing:
  def downsideRisk(netPnl: Rational): Rational =
    if netPnl.signum < 0 then -netPnl else Rational.zero

  def maxLots[L, Scenario](
    riskBudget: Rational,
    cap: BigInt
  )(
    lotsFor: BigInt => Either[EconomicsError, L],
    scenarioFor: L => Either[EconomicsError, Scenario],
    validateScenario: Scenario => Either[EconomicsError, Unit],
    heldPositionLots: Scenario => BigInt,
    riskFor: Scenario => Either[EconomicsError, Rational]
  ): Either[EconomicsError, Option[L]] =
    if riskBudget.signum < 0 then Left(InvalidRiskBudget(riskBudget))
    else
      var candidate = BigInt(1)
      var selected  = Option.empty[L]
      while candidate <= cap do
        val evaluated =
          for
            candidateLots <- lotsFor(candidate)
            scenario      <- scenarioFor(candidateLots)
            _             <- validateScenario(scenario)
            held           = heldPositionLots(scenario)
            _             <-
              if held.abs == candidate then Right(())
              else Left(SizingScenarioMismatch(candidate, held))
            risk <- riskFor(scenario)
          yield candidateLots -> risk

        evaluated match
          case Left(error)          => return Left(error)
          case Right((value, risk)) => if risk.compare(riskBudget) <= 0 then selected = Some(value)
        candidate += 1
      Right(selected)

end InstrumentSizing

final class InstrumentSizing[D <: Dimension, B <: Dimension, Q <: Dimension, S <: Dimension] private[economics] (
  instrumentId: InstrumentId,
  settleRef: DimRef[S],
  lotsFor: BigInt => Either[EconomicsError, InstrumentLots[D]],
  valuation: InstrumentValuation[D, B, Q, S]):

  private type Lots      = InstrumentLots[D]
  private type Price     = InstrumentPrice[B, Q]
  private type Market    = InstrumentMarketState[B, Q, S]
  private type Position  = InstrumentPosition[D]
  private type RoundTrip = InstrumentRoundTripScenario[Lots, Price, Market, Position]
  private type Schedule  = InstrumentFeeSchedule[Lots, Price, Market, Position]

  def downsideRisk(pnl: InstrumentPnl[S]): Either[EconomicsError, Quantity[S]] =
    InstrumentIdentityChecks
      .check("sizing.downsideRisk", instrumentId, "pnl" -> pnl.instrumentId)
      .map(_ => Quantity(settleRef, InstrumentSizing.downsideRisk(pnl.netPnl.coefficient)))

  def maxLots(
    riskBudget: Quantity[S],
    cap: PositiveWhole,
    feeSchedule: Schedule
  )(
    scenarioFor: Lots => Either[EconomicsError, RoundTrip]
  ): Either[EconomicsError, Option[Lots]] =
    InstrumentIdentityChecks
      .check("sizing.maxLots", instrumentId, "feeSchedule" -> feeSchedule.instrumentId)
      .flatMap: _ =>
        InstrumentSizing.maxLots(riskBudget.coefficient, cap.unrefined)(
          lotsFor,
          scenarioFor,
          scenario =>
            InstrumentIdentityChecks.check(
              "sizing.maxLots",
              instrumentId,
              "scenario"     -> scenario.instrumentId,
              "heldPosition" -> scenario.heldPosition.instrumentId,
              "entry"        -> scenario.entry.instrumentId,
              "exit"         -> scenario.exit.instrumentId
            ),
          _.heldPosition.count,
          scenario =>
            valuation.pnl(scenario, feeSchedule).flatMap(pnl => downsideRisk(pnl).map(_.coefficient))
        )

end InstrumentSizing
