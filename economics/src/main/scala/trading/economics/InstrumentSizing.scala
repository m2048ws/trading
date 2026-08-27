package trading.economics

import trading.quantity.*
import trading.quantity.refinement.PositiveWhole

private[economics] object InstrumentSizing:
  def downsideRisk(netPnl: Rational): Rational =
    if netPnl.signum < 0 then -netPnl else Rational.zero

  def maxLots[L, S](
    riskBudget: Rational,
    cap: BigInt
  )(
    lotsFor: BigInt => Either[EconomicsError, L],
    scenarioFor: L => Either[EconomicsError, S],
    heldPositionLots: S => BigInt,
    riskFor: S => Either[EconomicsError, Rational]
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

private[economics] final class InstrumentSizingImpl[
  O,
  D <: Dimension,
  B <: Dimension,
  Q <: Dimension,
  S <: Dimension
](
  authority: Instrument.OwnerAuthority[O],
  settleRef: DimRef[S],
  lotsFor: BigInt => Either[EconomicsError, InstrumentLots[O, D]],
  valuation: ValuationCapability[
    O,
    D,
    B,
    Q,
    S,
    InstrumentLots[O, D],
    InstrumentPrice[O, B, Q],
    InstrumentMarketState[O, B, Q, S],
    InstrumentPosition[O, D]
  ])
  extends SizingCapability[
    O,
    S,
    InstrumentLots[O, D],
    InstrumentPrice[O, B, Q],
    InstrumentMarketState[O, B, Q, S],
    InstrumentPosition[O, D]
  ]:

  private type Lots      = InstrumentLots[O, D]
  private type Price     = InstrumentPrice[O, B, Q]
  private type Market    = InstrumentMarketState[O, B, Q, S]
  private type Position  = InstrumentPosition[O, D]
  private type RoundTrip = InstrumentRoundTripScenario[O, Lots, Price, Market, Position]
  private type Schedule  = InstrumentFeeSchedule[O, Lots, Price, Market, Position]

  def downsideRisk(pnl: InstrumentPnl[O, S]): Quantity[S] =
    authority.assertIssued()
    Quantity(settleRef, InstrumentSizing.downsideRisk(pnl.netPnl.coefficient))

  def maxLots(
    riskBudget: Quantity[S],
    cap: PositiveWhole,
    feeSchedule: Schedule
  )(
    scenarioFor: Lots => Either[EconomicsError, RoundTrip]
  ): Either[EconomicsError, Option[Lots]] =
    InstrumentSizing.maxLots(riskBudget.coefficient, cap.unrefined)(
      lotsFor,
      scenarioFor,
      _.heldPosition.count,
      scenario => valuation.pnl(scenario, feeSchedule).map(pnl => downsideRisk(pnl).coefficient)
    )

end InstrumentSizingImpl
