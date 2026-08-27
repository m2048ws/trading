package trading.economics.instrument

import trading.quantity.*
import trading.quantity.refinement.PositiveWhole

private[instrument] object Sizing:
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

end Sizing

final class Sizing[D <: Dim, B <: Dim, Q <: Dim, S <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  settleRef: DimRef[S],
  lotsFor: BigInt => Either[EconomicsError, Lots[D]],
  valuation: Valuation[D, B, Q, S]):

  private type Lots      = _root_.trading.economics.instrument.Lots[D]
  private type Price     = _root_.trading.economics.instrument.Price[B, Q]
  private type Market    = _root_.trading.economics.instrument.MarketState[B, Q, S]
  private type Position  = _root_.trading.economics.instrument.Position[D]
  private type RoundTrip = _root_.trading.economics.instrument.RoundTripScenario[Lots, Price, Market, Position]
  private type Schedule  = _root_.trading.economics.instrument.FeeSchedule[Lots, Price, Market, Position]

  def downsideRisk(pnl: Pnl[S]): Either[EconomicsError, Quantity[S]] =
    IdentityChecks
      .check("sizing.downsideRisk", instrumentId, "pnl" -> pnl.instrumentId)
      .map(_ => Quantity(settleRef, Sizing.downsideRisk(pnl.netPnl.coefficient)))

  def maxLots(
    riskBudget: Quantity[S],
    cap: PositiveWhole,
    feeSchedule: Schedule
  )(
    scenarioFor: Lots => Either[EconomicsError, RoundTrip]
  ): Either[EconomicsError, Option[Lots]] =
    IdentityChecks
      .check("sizing.maxLots", instrumentId, "feeSchedule" -> feeSchedule.instrumentId)
      .flatMap: _ =>
        Sizing.maxLots(riskBudget.coefficient, cap.unrefined)(
          lotsFor,
          scenarioFor,
          scenario =>
            IdentityChecks.check(
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

end Sizing
