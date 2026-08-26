package trading.economics

import trading.quantity.Rational

private[economics] object InstrumentValuation:
  final case class PnlPlan[A](
    pricePnl: Rational,
    convertedFeeLines: Vector[A],
    feePnl: Rational,
    netPnl: Rational)

  def settlePerPosition(
    basePerPosition: Rational,
    baseToSettle: Rational,
    quotePerPosition: Rational,
    quoteToSettle: Rational
  ): Rational =
    basePerPosition * baseToSettle + quotePerPosition * quoteToSettle

  def positionValue(position: Rational, settlePerPosition: Rational): Rational = position * settlePerPosition

  def pricePnl(position: Rational, entrySettlePerPosition: Rational, exitSettlePerPosition: Rational): Rational =
    position * exitSettlePerPosition - position * entrySettlePerPosition

  def scenarioPricePnl(slices: Vector[(Rational, Rational)]): Rational =
    slices.foldLeft(Rational.zero):
      case (total, (positionChange, settleValuePerPosition)) =>
        total - positionValue(positionChange, settleValuePerPosition)

  def calculatePnl[S, L, C](
    entry: S,
    exit: S,
    exactPricePnl: Rational
  )(
    assess: S => Either[EconomicsError, Vector[L]],
    convert: (ScenarioLeg, L) => Either[EconomicsError, C],
    contribution: C => Rational
  ): Either[EconomicsError, PnlPlan[C]] =
    for
      entryLines     <- assess(entry)
      exitLines      <- assess(exit)
      convertedEntry <- convertLines(ScenarioLeg.Entry, entryLines)(convert)
      convertedExit  <- convertLines(ScenarioLeg.Exit, exitLines)(convert)
    yield
      val converted = convertedEntry ++ convertedExit
      val feeTotal  = converted.foldLeft(Rational.zero)((total, line) => total + contribution(line))
      PnlPlan(exactPricePnl, converted, feeTotal, exactPricePnl + feeTotal)

  private def convertLines[L, C](
    leg: ScenarioLeg,
    lines: Vector[L]
  )(
    convert: (ScenarioLeg, L) => Either[EconomicsError, C]
  ): Either[EconomicsError, Vector[C]] =
    lines.foldLeft[Either[EconomicsError, Vector[C]]](Right(Vector.empty)): (result, line) =>
      result.flatMap(accumulated => convert(leg, line).map(accumulated :+ _))

end InstrumentValuation
