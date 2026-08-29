package trading.economics.instrument

import scala.annotation.tailrec

import trading.quantity.*
import trading.quantity.refinement.PositiveWhole

private[instrument] object Sizing:
  def downsideRisk[D <: Dim](netPnl: Quantity[D])(using dimension: DimRef[D]): Quantity[D] =
    if netPnl.coefficient.signum < 0 then netPnl * Rational(-1) else Quantity.zero[D]

  def maxLots[L, Scenario, R <: Dim](
    riskBudget: Quantity[R],
    cap: BigInt
  )(
    lotsFor: BigInt => Either[EconomicsError, L],
    scenarioFor: L => Either[EconomicsError, Scenario],
    validateScenario: Scenario => Either[EconomicsError, Unit],
    heldPositionLots: Scenario => BigInt,
    riskFor: Scenario => Either[EconomicsError, Quantity[R]]
  ): Either[EconomicsError, Option[L]] =
    if riskBudget.coefficient.signum < 0 then Left(InvalidRiskBudget(riskBudget.coefficient))
    else
      @tailrec
      def loop(candidate: BigInt, selected: Option[L]): Either[EconomicsError, Option[L]] =
        if candidate > cap then Right(selected)
        else
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
            case Left(error)          => Left(error)
            case Right((value, risk)) =>
              val nextSelected =
                if risk.coefficient.compare(riskBudget.coefficient) <= 0 then Some(value) else selected
              loop(candidate + 1, nextSelected)

      loop(BigInt(1), None)

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
      .map(_ => Sizing.downsideRisk(pnl.netPnl)(using settleRef))

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
        Sizing.maxLots(riskBudget, cap.unrefined)(
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
          scenario => valuation.pnl(scenario, feeSchedule).flatMap(downsideRisk)
        )

end Sizing
