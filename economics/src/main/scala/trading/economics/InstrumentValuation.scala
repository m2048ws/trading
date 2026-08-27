package trading.economics

import trading.quantity.*
import trading.quantity.runtime.AssetRef

final case class InstrumentConvertedFeeLine[S <: Dimension] private[economics] (
  instrumentId: InstrumentId,
  original: InstrumentFee[? <: Dimension],
  leg: ScenarioLeg,
  sourceSliceIndex: Int,
  settleContribution: Quantity[S])

final case class InstrumentPnl[S <: Dimension] private[economics] (
  instrumentId: InstrumentId,
  pricePnl: Quantity[S],
  convertedFeeLines: Vector[InstrumentConvertedFeeLine[S]],
  feePnl: Quantity[S],
  netPnl: Quantity[S])

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

final class InstrumentValuation[PosD <: Dimension, B <: Dimension, Q <: Dimension, S <: Dimension] private[economics] (
  instrumentId: InstrumentId,
  position: AssetRef { type D = PosD },
  settle: AssetRef { type D = S },
  basePerPosition: Rate[PosD, B],
  quotePerPosition: Rate[PosD, Q]):

  private type Lots      = InstrumentLots[PosD]
  private type Price     = InstrumentPrice[B, Q]
  private type Market    = InstrumentMarketState[B, Q, S]
  private type Position  = InstrumentPosition[PosD]
  private type Scenario  = InstrumentOrderScenario[Lots, Price, Market, Position]
  private type RoundTrip = InstrumentRoundTripScenario[Lots, Price, Market, Position]
  private type Schedule  = InstrumentFeeSchedule[Lots, Price, Market, Position]

  def settlePerPosition(state: Market): Either[EconomicsError, Rate[PosD, S]] =
    InstrumentIdentityChecks
      .check("valuation.settlePerPosition", instrumentId, "market" -> state.instrumentId)
      .map: _ =>
        val coefficient = InstrumentValuation.settlePerPosition(
          basePerPosition.coefficient,
          state.baseToSettle.coefficient,
          quotePerPosition.coefficient,
          state.quoteToSettle.coefficient
        )
        Rate(position.dimension.asDimensionRef, settle.dimension.asDimensionRef, coefficient)

  def positionValue(value: Position, state: Market): Either[EconomicsError, Quantity[S]] =
    for
      _ <- InstrumentIdentityChecks.check(
             "valuation.positionValue",
             instrumentId,
             "position" -> value.instrumentId,
             "market"   -> state.instrumentId
           )
      perPosition <- settlePerPosition(state)
    yield Quantity(
      settle.dimension.asDimensionRef,
      InstrumentValuation.positionValue(value.quantity.coefficient, perPosition.coefficient)
    )

  def pricePnl(value: Position, entry: Market, exit: Market): Either[EconomicsError, Quantity[S]] =
    for
      _ <- InstrumentIdentityChecks.check(
             "valuation.pricePnl",
             instrumentId,
             "position" -> value.instrumentId,
             "entry"    -> entry.instrumentId,
             "exit"     -> exit.instrumentId
           )
      entryPerPosition <- settlePerPosition(entry)
      exitPerPosition  <- settlePerPosition(exit)
    yield Quantity(
      settle.dimension.asDimensionRef,
      InstrumentValuation.pricePnl(
        value.quantity.coefficient,
        entryPerPosition.coefficient,
        exitPerPosition.coefficient
      )
    )

  def pnl(roundTrip: RoundTrip, feeSchedule: Schedule): Either[EconomicsError, InstrumentPnl[S]] =
    for
      _ <- InstrumentIdentityChecks.check(
             "valuation.pnl",
             instrumentId,
             "roundTrip"   -> roundTrip.instrumentId,
             "entry"       -> roundTrip.entry.instrumentId,
             "exit"        -> roundTrip.exit.instrumentId,
             "feeSchedule" -> feeSchedule.instrumentId
           )
      exactPricePnl <- scenarioPricePnl(roundTrip.entry).flatMap: entryPnl =>
                         scenarioPricePnl(roundTrip.exit).map(exitPnl => entryPnl + exitPnl)
      entryLines     <- assessAndValidate(feeSchedule, roundTrip.entry)
      exitLines      <- assessAndValidate(feeSchedule, roundTrip.exit)
      convertedEntry <- convertLines(ScenarioLeg.Entry, entryLines)
      convertedExit  <- convertLines(ScenarioLeg.Exit, exitLines)
    yield
      val converted = convertedEntry ++ convertedExit
      val feeTotal  = converted.foldLeft(Rational.zero)((total, line) => total + line.settleContribution.coefficient)
      val feePnl    = Quantity(settle.dimension.asDimensionRef, feeTotal)
      val netPnl    = Quantity(settle.dimension.asDimensionRef, exactPricePnl.coefficient + feeTotal)
      InstrumentPnl(instrumentId, exactPricePnl, converted, feePnl, netPnl)

  private def scenarioPricePnl(scenario: Scenario): Either[EconomicsError, Quantity[S]] =
    InstrumentIdentityChecks
      .check(
        "valuation.scenario",
        instrumentId,
        (Vector("scenario" -> scenario.instrumentId) ++ scenario.assumptions.matchedSlices.zipWithIndex.flatMap:
          (slice, index) =>
            Vector(
              s"slices[$index]"        -> slice.instrumentId,
              s"slices[$index].lots"   -> slice.lots.instrumentId,
              s"slices[$index].market" -> slice.market.instrumentId
            )
        )*
      )
      .flatMap: _ =>
        scenario.assumptions.matchedSlices.foldLeft[Either[EconomicsError, Rational]](Right(Rational.zero)):
          (result, slice) =>
            for
              total       <- result
              perPosition <- settlePerPosition(slice.market)
            yield
              val signedPosition = slice.lots.quantity.coefficient * Rational(scenario.order.intent.side.sign)
              total - InstrumentValuation.positionValue(signedPosition, perPosition.coefficient)
      .map(coefficient => Quantity(settle.dimension.asDimensionRef, coefficient))

  private def assessAndValidate(
    schedule: Schedule,
    scenario: Scenario
  ): Either[EconomicsError, Vector[InstrumentFeeLine[? <: Dimension, Market]]] =
    schedule.assess(scenario).flatMap: lines =>
      InstrumentIdentityChecks
        .check(
          "valuation.feeLines",
          instrumentId,
          lines.zipWithIndex.flatMap((line, index) =>
            Vector(
              s"lines[$index]"              -> line.instrumentId,
              s"lines[$index].fee"          -> line.fee.instrumentId,
              s"lines[$index].denomination" -> line.fee.denomination.instrumentId,
              s"lines[$index].market"       -> line.sourceMarket.instrumentId
            )
          )*
        )
        .flatMap: _ =>
          lines.collectFirst:
            case line
              if line.sourceSliceIndex < 0 || line.sourceSliceIndex >= scenario.assumptions.matchedSlices.size =>
              InvalidFeeAttribution(line.sourceSliceIndex, scenario.assumptions.matchedSlices.size)
            case line
              if !line.sourceMarket.eq(scenario.assumptions.matchedSlices(line.sourceSliceIndex).market) =>
              FeeScheduleFailure(FeeScheduleFailureReason.ForeignScenarioLine)
          match
            case Some(error) => Left(error)
            case None        => Right(lines)

  private def convertLines(
    leg: ScenarioLeg,
    lines: Vector[InstrumentFeeLine[? <: Dimension, Market]]
  ): Either[EconomicsError, Vector[InstrumentConvertedFeeLine[S]]] =
    lines.foldLeft[Either[EconomicsError, Vector[InstrumentConvertedFeeLine[S]]]](Right(Vector.empty)):
      (result, line) => result.flatMap(accumulated => convertLine(leg, line).map(accumulated :+ _))

  private def convertLine(
    leg: ScenarioLeg,
    line: InstrumentFeeLine[? <: Dimension, Market]
  ): Either[EconomicsError, InstrumentConvertedFeeLine[S]] =
    val fee = line.fee
    line.sourceMarket
      .convertToSettle(fee.asset)(fee.amount)
      .left
      .map:
        case MissingConversion(source, _, _) => MissingConversion(source, Some(leg), Some(line.sourceSliceIndex))
        case other                           => other
      .map(contribution => InstrumentConvertedFeeLine(instrumentId, fee, leg, line.sourceSliceIndex, contribution))

end InstrumentValuation
