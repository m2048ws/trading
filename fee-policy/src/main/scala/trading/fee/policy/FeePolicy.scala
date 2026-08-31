package trading.fee.policy

import cats.syntax.all.*

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.reference.*
import trading.scenario.*

/** Quoted fee-policy sign: positive is a charge and negative is a rebate. */
final case class FeeRate(coefficient: Rational)

/** Downstream attribution of one exact fee to one scenario market state. */
final case class FeeLine[D <: Dim, M] private[policy] (
  instrumentId: InstrumentId,
  fee: Fee[D],
  sourceSliceIndex: Int,
  sourceMarket: M)

trait FeeSchedule[D <: Dim, B <: Dim, Q <: Dim, M]:
  def instrumentId: InstrumentId
  def assess(scenario: OrderScenario[D, B, Q, M]): Either[FeePolicyError, Vector[FeeLine[? <: Dim, M]]]

/** Pure downstream fee-policy and scenario orchestration boundary. */
final class FeePolicy[I <: Instrument] private[policy] (val instrument: I):

  private val instrumentId = instrument.identity.id

  type D         = instrument.roles.position.D
  type B         = instrument.roles.base.D
  type Q         = instrument.roles.quote.D
  type Lots      = instrument.Lots
  type Price     = instrument.Price
  type Market    = instrument.MarketState
  type Position  = instrument.PositionLots
  type Scenario  = _root_.trading.scenario.OrderScenario[D, B, Q, Market]
  type RoundTrip = _root_.trading.scenario.RoundTripScenario[D, B, Q, Market]
  type Schedule  = _root_.trading.fee.policy.FeeSchedule[D, B, Q, Market]

  def denomination(
    asset: Asset
  )(
    grid: GridHandle[? <: Dim],
    policy: QuantizationPolicy
  ): Either[FeePolicyError, FeeDenomination[asset.D]] =
    FeeDenomination.create(instrument)(asset, grid, policy).left.map(FeeValueFailure(_))

  def percentage[FD <: Dim](
    denomination: FeeDenomination[FD],
    kind: FeeKind,
    nonnegativeBasis: Quantity[FD],
    rate: FeeRate
  ): Either[FeePolicyError, Fee[FD]] =
    if nonnegativeBasis.coefficient.signum < 0 then
      Left(InvalidFeeBasis(denomination.asset.id, nonnegativeBasis.coefficient))
    else
      Fee
        .create(instrument)(denomination, kind, nonnegativeBasis * -rate.coefficient)
        .left
        .map(FeeValueFailure(_))

  def minimumCharge[FD <: Dim](
    contribution: Quantity[FD],
    nonnegativeMinimum: Quantity[FD],
    asset: AssetId
  ): Either[FeePolicyError, Quantity[FD]] =
    if nonnegativeMinimum.coefficient.signum < 0 then
      Left(InvalidFeeBasis(asset, nonnegativeMinimum.coefficient))
    else if contribution.coefficient.signum < 0 &&
      contribution.coefficient.abs.compare(nonnegativeMinimum.coefficient) < 0
    then Right(nonnegativeMinimum * Rational(-1))
    else Right(contribution)

  def line[FD <: Dim](
    scenario: Scenario,
    sourceSliceIndex: Int,
    fee: Fee[FD]
  ): Either[FeePolicyError, FeeLine[FD, Market]] =
    val slices = scenario.matchedSlices.toVector
    if sourceSliceIndex < 0 || sourceSliceIndex >= slices.size then
      Left(InvalidFeeAttribution(sourceSliceIndex, slices.size))
    else
      val market = slices(sourceSliceIndex).market
      checkIdentities(
        "line",
        "scenario"     -> scenario.instrumentId,
        "fee"          -> fee.instrumentId,
        "denomination" -> fee.denomination.instrumentId,
        "market"       -> market.instrumentId
      ).map(_ => FeeLine(instrumentId, fee, sourceSliceIndex, market))

  val none: Schedule = new Schedule:
    val instrumentId: InstrumentId = FeePolicy.this.instrumentId
    def assess(scenario: Scenario): Either[FeePolicyError, Vector[FeeLine[? <: Dim, Market]]] =
      checkIdentities("none", "scenario" -> scenario.instrumentId).map(_ => Vector.empty)

  def combine(componentSchedules: Vector[Schedule]): Either[FeePolicyError, Schedule] =
    checkIdentities(
      "combine",
      componentSchedules.zipWithIndex.map((schedule, index) => s"schedules[$index]" -> schedule.instrumentId)*
    ).map: _ =>
      new Schedule:
        val instrumentId: InstrumentId = FeePolicy.this.instrumentId
        def assess(scenario: Scenario): Either[FeePolicyError, Vector[FeeLine[? <: Dim, Market]]] =
          for
            _     <- checkIdentities("assess", "scenario" -> scenario.instrumentId)
            lines <- componentSchedules.traverse(_.assess(scenario)).map(_.flatten)
            _     <- checkIdentities(
                   "assess",
                   lines.zipWithIndex.flatMap((line, index) =>
                     Vector(
                       s"lines[$index]"        -> line.instrumentId,
                       s"lines[$index].fee"    -> line.fee.instrumentId,
                       s"lines[$index].market" -> line.sourceMarket.instrumentId
                     )
                   )*
                 )
          yield lines

  /** Evaluate downstream scenario and policy inputs before invoking pure contribution/PnL composition. */
  def pnl(roundTrip: RoundTrip, schedule: Schedule): Either[FeePolicyError, instrument.Pnl] =
    for
      _ <- checkIdentities(
             "pnl",
             "roundTrip" -> roundTrip.instrumentId,
             "entry"     -> roundTrip.entry.instrumentId,
             "exit"      -> roundTrip.exit.instrumentId,
             "schedule"  -> schedule.instrumentId
           )
      entryValue <- scenarioSignedValue(roundTrip.entry)
      exitSigned <- scenarioSignedValue(roundTrip.exit)
      pricePnl   <- PricePnl
                    .fromValues(instrument)(roundTrip.heldPosition, entryValue, exitSigned * Rational(-1))
                    .left
                    .map(FeeValuationFailure(_))
      entryLines         <- assessAndValidate(schedule, roundTrip.entry)
      exitLines          <- assessAndValidate(schedule, roundTrip.exit)
      entryContributions <- entryLines.traverse(convertLine(ScenarioLeg.Entry, _))
      exitContributions  <- exitLines.traverse(convertLine(ScenarioLeg.Exit, _))
      result             <- Pnl
                  .create(instrument)(pricePnl, entryContributions ++ exitContributions)
                  .left
                  .map(FeePnlFailure(_))
    yield result

  private def scenarioSignedValue(
    scenario: Scenario
  ): Either[FeePolicyError, Quantity[instrument.roles.settle.D]] =
    scenario.matchedSlices.toVector
      .traverse: slice =>
        val coordinate = scenario.order.intent.side.sign * slice.lots.count.unrefined
        val position   = PositionLots.fromCoordinate(instrument)(coordinate)
        Valuation
          .positionValue(instrument)(position, slice.market)
          .left
          .map(FeeValuationFailure(_))
      .map(
        _.foldLeft(
          Quantity.zero[instrument.roles.settle.D](using instrument.roles.settle.dimension.ref)
        )(_ + _)
      )

  private def assessAndValidate(
    schedule: Schedule,
    scenario: Scenario
  ): Either[FeePolicyError, Vector[FeeLine[? <: Dim, Market]]] =
    schedule.assess(scenario).flatMap: lines =>
      for
        _ <- checkIdentities(
               "feeLines",
               lines.zipWithIndex.flatMap((line, index) =>
                 Vector(
                   s"lines[$index]"              -> line.instrumentId,
                   s"lines[$index].fee"          -> line.fee.instrumentId,
                   s"lines[$index].denomination" -> line.fee.denomination.instrumentId,
                   s"lines[$index].market"       -> line.sourceMarket.instrumentId
                 )
               )*
             )
        _ <-
          val slices = scenario.matchedSlices.toVector
          lines.collectFirst:
            case line if line.sourceSliceIndex < 0 || line.sourceSliceIndex >= slices.size =>
              InvalidFeeAttribution(line.sourceSliceIndex, slices.size)
            case line if !line.sourceMarket.eq(slices(line.sourceSliceIndex).market) =>
              ForeignScenarioLine(line.sourceSliceIndex)
          match
            case Some(error) => Left(error)
            case None        => Right(())
      yield lines

  private def convertLine(
    leg: ScenarioLeg,
    line: FeeLine[? <: Dim, Market]
  ): Either[FeePolicyError, SettledFeeContribution[instrument.roles.settle.D]] =
    convertCaptured(leg, line)

  private def convertCaptured[FD <: Dim](
    leg: ScenarioLeg,
    line: FeeLine[FD, Market]
  ): Either[FeePolicyError, SettledFeeContribution[instrument.roles.settle.D]] =
    SettledFeeContribution
      .convert(instrument)(line.fee, line.sourceMarket)
      .left
      .map(cause => FeeContributionFailure(leg, line.sourceSliceIndex, cause))

  private def checkIdentities(
    context: String,
    supplied: (String, InstrumentId)*
  ): Either[FeePolicyError, Unit] =
    supplied.collectFirst:
      case (name, id) if id != instrumentId =>
        FeePolicyInstrumentMismatch(s"$context.$name", instrumentId, id)
    match
      case Some(error) => Left(error)
      case None        => Right(())
end FeePolicy

object FeePolicy:
  def apply[I <: Instrument](instrument: I): FeePolicy[instrument.type] =
    new FeePolicy[instrument.type](instrument)
