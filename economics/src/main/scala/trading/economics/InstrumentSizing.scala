package trading.economics

import trading.quantity.Rational

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
