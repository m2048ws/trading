package trading.economics

import trading.quantity.*
import trading.quantity.runtime.AssetRef

private[economics] object InstrumentValuation:
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

end InstrumentValuation

private[economics] final class InstrumentValuationImpl[
  O,
  PosD <: Dimension,
  B <: Dimension,
  Q <: Dimension,
  S <: Dimension
](
  authority: Instrument.OwnerAuthority[O],
  position: AssetRef { type D = PosD },
  settle: AssetRef { type D = S },
  basePerPosition: Rate[PosD, B],
  quotePerPosition: Rate[PosD, Q])
  extends ValuationCapability[
    O,
    PosD,
    B,
    Q,
    S,
    InstrumentLots[O, PosD],
    InstrumentPrice[O, B, Q],
    InstrumentMarketState[O, B, Q, S],
    InstrumentPosition[O, PosD]
  ]:

  private type Lots      = InstrumentLots[O, PosD]
  private type Price     = InstrumentPrice[O, B, Q]
  private type Market    = InstrumentMarketState[O, B, Q, S]
  private type Position  = InstrumentPosition[O, PosD]
  private type Scenario  = InstrumentOrderScenario[O, Lots, Price, Market, Position]
  private type RoundTrip = InstrumentRoundTripScenario[O, Lots, Price, Market, Position]
  private type Schedule  = InstrumentFeeSchedule[O, Lots, Price, Market, Position]

  def settlePerPosition(state: Market): Rate[PosD, S] =
    val coefficient = InstrumentValuation.settlePerPosition(
      basePerPosition.coefficient,
      state.baseToSettle.coefficient,
      quotePerPosition.coefficient,
      state.quoteToSettle.coefficient
    )
    Rate(position.dimension.asDimensionRef, settle.dimension.asDimensionRef, coefficient)

  def positionValue(value: Position, state: Market): Quantity[S] =
    Quantity(
      settle.dimension.asDimensionRef,
      InstrumentValuation.positionValue(value.quantity.coefficient, settlePerPosition(state).coefficient)
    )

  def pricePnl(value: Position, entry: Market, exit: Market): Quantity[S] =
    Quantity(
      settle.dimension.asDimensionRef,
      InstrumentValuation.pricePnl(
        value.quantity.coefficient,
        settlePerPosition(entry).coefficient,
        settlePerPosition(exit).coefficient
      )
    )

  def pnl(roundTrip: RoundTrip, feeSchedule: Schedule): Either[EconomicsError, InstrumentPnl[O, S]] =
    val exactPricePnl = scenarioPricePnl(roundTrip.entry) + scenarioPricePnl(roundTrip.exit)
    for
      entryLines     <- assessAndValidate(feeSchedule, roundTrip.entry)
      exitLines      <- assessAndValidate(feeSchedule, roundTrip.exit)
      convertedEntry <- convertLines(ScenarioLeg.Entry, entryLines)
      convertedExit  <- convertLines(ScenarioLeg.Exit, exitLines)
    yield
      val converted = convertedEntry ++ convertedExit
      val feeTotal  = converted.foldLeft(Rational.zero)((total, line) => total + line.settleContribution.coefficient)
      val feePnl    = Quantity(settle.dimension.asDimensionRef, feeTotal)
      val netPnl    = Quantity(settle.dimension.asDimensionRef, exactPricePnl.coefficient + feeTotal)
      authority.pnl(exactPricePnl, converted, feePnl, netPnl)

  private def scenarioPricePnl(scenario: Scenario): Quantity[S] =
    val coefficient = scenario.assumptions.matchedSlices.foldLeft(Rational.zero): (total, slice) =>
      val signedPosition = slice.lots.quantity.coefficient * Rational(scenario.order.intent.side.sign)
      total - InstrumentValuation.positionValue(signedPosition, settlePerPosition(slice.market).coefficient)
    Quantity(settle.dimension.asDimensionRef, coefficient)

  private def assessAndValidate(
    schedule: Schedule,
    scenario: Scenario
  ): Either[EconomicsError, Vector[InstrumentFeeLine[O, Market]]] =
    schedule.assess(scenario).flatMap: lines =>
      lines.collectFirst:
        case line if !authority.feeLineScenario(line).eq(scenario) =>
          FeeScheduleFailure(FeeScheduleFailureReason.ForeignScenarioLine)
      match
        case Some(error) => Left(error)
        case None        => Right(lines)

  private def convertLines(
    leg: ScenarioLeg,
    lines: Vector[InstrumentFeeLine[O, Market]]
  ): Either[EconomicsError, Vector[InstrumentConvertedFeeLine[O, S]]] =
    lines.foldLeft[Either[EconomicsError, Vector[InstrumentConvertedFeeLine[O, S]]]](Right(Vector.empty)):
      (result, line) =>
        result.flatMap: accumulated =>
          convertLine(leg, line).map(accumulated :+ _)

  private def convertLine(
    leg: ScenarioLeg,
    line: InstrumentFeeLine[O, Market]
  ): Either[EconomicsError, InstrumentConvertedFeeLine[O, S]] =
    val fee = line.fee
    line.sourceMarket
      .convertToSettle(fee.asset)(fee.amount)
      .left
      .map:
        case MissingConversion(source, _, _) => MissingConversion(source, Some(leg), Some(line.sourceSliceIndex))
        case other                           => other
      .map(contribution => authority.convertedFeeLine(fee, leg, line.sourceSliceIndex, contribution))

end InstrumentValuationImpl
