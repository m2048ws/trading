package trading.economics

import trading.quantity.Rational

private[economics] object FeeValuationSizingRules:
  def settlePerPosition(
    basePerPosition: Rational,
    baseToSettle: Rational,
    quotePerPosition: Rational,
    quoteToSettle: Rational
  ): Rational =
    basePerPosition * baseToSettle + quotePerPosition * quoteToSettle

  def pricePnl(position: Rational, entrySettlePerPosition: Rational, exitSettlePerPosition: Rational): Rational =
    position * exitSettlePerPosition - position * entrySettlePerPosition

  def sum(values: Vector[Rational]): Rational = values.foldLeft(Rational.zero)(_ + _)

  def minimumCharge(
    accountContribution: Rational,
    nonnegativeMinimum: Rational
  ): Either[Rational, Rational] =
    if nonnegativeMinimum.signum < 0 then Left(nonnegativeMinimum)
    else if accountContribution.signum < 0 && accountContribution.abs.compare(nonnegativeMinimum) < 0 then
      Right(-nonnegativeMinimum)
    else Right(accountContribution)

  def percentageContribution(nonnegativeBasis: Rational, rate: Rational): Either[Rational, Rational] =
    if nonnegativeBasis.signum < 0 then Left(nonnegativeBasis)
    else Right(nonnegativeBasis * -rate)

  def downsideRisk(netPnl: Rational): Rational =
    if netPnl.signum < 0 then -netPnl else Rational.zero

  def selectGreatest[A](
    cap: BigInt,
    riskBudget: Rational
  )(
    evaluate: BigInt => Either[EconomicsError, (A, Rational)]
  ): Either[EconomicsError, Option[A]] =
    var candidate = BigInt(1)
    var selected  = Option.empty[A]
    while candidate <= cap do
      evaluate(candidate) match
        case Left(error)          => return Left(error)
        case Right((value, risk)) => if risk.compare(riskBudget) <= 0 then selected = Some(value)
      candidate += 1
    Right(selected)

end FeeValuationSizingRules
