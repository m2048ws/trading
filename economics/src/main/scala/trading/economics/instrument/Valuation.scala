package trading.economics.instrument

import cats.data.Chain
import cats.syntax.all.*

import trading.quantity.*
import trading.reference.Asset

final case class ConvertedFeeLine[S <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  original: Fee[? <: Dim],
  leg: ScenarioLeg,
  sourceSliceIndex: Int,
  settleContribution: Quantity[S])

final case class Pnl[S <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  pricePnl: Quantity[S],
  convertedFeeLines: Vector[ConvertedFeeLine[S]],
  feePnl: Quantity[S],
  netPnl: Quantity[S])

final class Valuation[PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  settle: Asset { type D = S },
  basePerPosition: Rate[PosD, B],
  quotePerPosition: Rate[PosD, Q]):

  private type Lots      = _root_.trading.economics.instrument.Lots[PosD]
  private type Price     = _root_.trading.economics.instrument.Price[B, Q]
  private type Market    = _root_.trading.economics.instrument.MarketState[B, Q, S]
  private type Position  = _root_.trading.economics.instrument.Position[PosD]
  private type Scenario  = _root_.trading.economics.instrument.OrderScenario[Lots, Price, Market, Position]
  private type RoundTrip = _root_.trading.economics.instrument.RoundTripScenario[Lots, Price, Market, Position]
  private type Schedule  = _root_.trading.economics.instrument.FeeSchedule[Lots, Price, Market, Position]

  def settlePerPosition(state: Market): Either[EconomicsError, Rate[PosD, S]] =
    IdentityChecks
      .check("valuation.settlePerPosition", instrumentId, "market" -> state.instrumentId)
      .map: _ =>
        basePerPosition.andThen(state.baseToSettle) +
          quotePerPosition.andThen(state.quoteToSettle)

  def positionValue(value: Position, state: Market): Either[EconomicsError, Quantity[S]] =
    for
      _ <- IdentityChecks.check(
             "valuation.positionValue",
             instrumentId,
             "position" -> value.instrumentId,
             "market"   -> state.instrumentId
           )
      perPosition <- settlePerPosition(state)
    yield value.quantity.applyRate(perPosition)

  def pricePnl(value: Position, entry: Market, exit: Market): Either[EconomicsError, Quantity[S]] =
    for
      _ <- IdentityChecks.check(
             "valuation.pricePnl",
             instrumentId,
             "position" -> value.instrumentId,
             "entry"    -> entry.instrumentId,
             "exit"     -> exit.instrumentId
           )
      entryValue <- positionValue(value, entry)
      exitValue  <- positionValue(value, exit)
    yield exitValue - entryValue

  def pnl(roundTrip: RoundTrip, feeSchedule: Schedule): Either[EconomicsError, Pnl[S]] =
    for
      _ <- IdentityChecks.check(
             "valuation.pnl",
             instrumentId,
             "roundTrip"   -> roundTrip.instrumentId,
             "entry"       -> roundTrip.entry.instrumentId,
             "exit"        -> roundTrip.exit.instrumentId,
             "feeSchedule" -> feeSchedule.instrumentId
           )
      entryPricePnl  <- scenarioPricePnl(roundTrip.entry)
      exitPricePnl   <- scenarioPricePnl(roundTrip.exit)
      entryLines     <- assessAndValidate(feeSchedule, roundTrip.entry)
      exitLines      <- assessAndValidate(feeSchedule, roundTrip.exit)
      convertedEntry <- convertLines(ScenarioLeg.Entry, entryLines)
      convertedExit  <- convertLines(ScenarioLeg.Exit, exitLines)
    yield
      val exactPricePnl = entryPricePnl + exitPricePnl
      val converted     = convertedEntry ++ convertedExit
      val feeTotal      = converted.foldLeft(Quantity.zero[S](using settle.dimension.ref)): (total, line) =>
        total + line.settleContribution
      Pnl(instrumentId, exactPricePnl, converted.toVector, feeTotal, exactPricePnl + feeTotal)

  private def scenarioPricePnl(scenario: Scenario): Either[EconomicsError, Quantity[S]] =
    val slices = scenario.assumptions.matchedSlices.toVector
    IdentityChecks
      .check(
        "valuation.scenario",
        instrumentId,
        (Vector("scenario" -> scenario.instrumentId) ++ slices.zipWithIndex.flatMap: (slice, index) =>
          Vector(
            s"slices[$index]"        -> slice.instrumentId,
            s"slices[$index].lots"   -> slice.lots.instrumentId,
            s"slices[$index].market" -> slice.market.instrumentId
          ))*
      )
      .flatMap: _ =>
        slices
          .traverse: slice =>
            settlePerPosition(slice.market).map: perPosition =>
              val signedPosition = slice.lots.quantity * Rational(scenario.order.intent.side.sign)
              signedPosition.applyRate(perPosition)
          .map: values =>
            values.foldLeft(Quantity.zero[S](using settle.dimension.ref))((total, value) => total - value)
  end scenarioPricePnl

  private def assessAndValidate(
    schedule: Schedule,
    scenario: Scenario
  ): Either[EconomicsError, Vector[FeeLine[? <: Dim, Market]]] =
    schedule.assess(scenario).flatMap: lines =>
      IdentityChecks
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
          val slices = scenario.assumptions.matchedSlices.toVector
          lines.collectFirst:
            case line if line.sourceSliceIndex < 0 || line.sourceSliceIndex >= slices.size =>
              InvalidFeeAttribution(line.sourceSliceIndex, slices.size)
            case line if !line.sourceMarket.eq(slices(line.sourceSliceIndex).market) =>
              FeeScheduleFailure(FeeScheduleFailureReason.ForeignScenarioLine)
          match
            case Some(error) => Left(error)
            case None        => Right(lines)

  private def convertLines(
    leg: ScenarioLeg,
    lines: Vector[FeeLine[? <: Dim, Market]]
  ): Either[EconomicsError, Chain[ConvertedFeeLine[S]]] =
    lines.traverse(convertLine(leg, _)).map(Chain.fromSeq)

  private def convertLine(
    leg: ScenarioLeg,
    line: FeeLine[? <: Dim, Market]
  ): Either[EconomicsError, ConvertedFeeLine[S]] =
    val fee = line.fee
    line.sourceMarket
      .convertToSettle(fee.asset)(fee.amount)
      .left
      .map:
        case MissingConversion(source, _, _) => MissingConversion(source, Some(leg), Some(line.sourceSliceIndex))
        case other                           => other
      .map(contribution => ConvertedFeeLine(instrumentId, fee, leg, line.sourceSliceIndex, contribution))

end Valuation
