package trading.economics.instrument

import cats.data.Chain
import cats.syntax.all.*

import trading.quantity.*
import trading.quantity.grid.*
import trading.reference.*

/** Semantic identity of a trading-fee component. */
final case class FeeKind(value: String):
  require(value.trim.nonEmpty, "fee kind cannot be empty")

/** Quoted fee-policy sign: positive is a charge and negative is a rebate. */
final case class FeeRate(coefficient: Rational):
  require(coefficient != null, "fee rate coefficient")

final class FeeDenomination[DD <: Dim] private[instrument] (
  val instrumentId: InstrumentId,
  val asset: Asset { type D = DD },
  private val grid: GridHandle[DD],
  val policy: QuantizationPolicy):

  val gridKey: GridKey      = grid.key
  val gridQuantum: Rational = grid.quantum.unrefined

  def minimumCharge(contrib: Quantity[DD], nonnegativeMinimum: Quantity[DD]): Either[EconomicsError, Quantity[DD]] =
    Fees
      .minimumCharge(contrib.coefficient, nonnegativeMinimum.coefficient)
      .left
      .map(coefficient => InvalidFeeBasis(asset.id, coefficient))
      .map(coefficient => Quantity(asset.dimension.ref, coefficient))

  def quantize(kind: FeeKind, unrounded: Quantity[DD]): Fee[DD] =
    val result = unrounded.quantizeTo(grid.grid, policy)
    Fee(
      instrumentId,
      this,
      kind,
      grid.coordinate(result.value),
      asset,
      grid.asQuantity(result.value),
      result.residual,
      unrounded
    )

  def percentage(kind: FeeKind, nonnegativeBasis: Quantity[DD], rate: FeeRate): Either[EconomicsError, Fee[DD]] =
    Fees
      .percentageContribution(nonnegativeBasis.coefficient, rate.coefficient)
      .left
      .map(coefficient => InvalidFeeBasis(asset.id, coefficient))
      .map(coefficient => quantize(kind, Quantity(asset.dimension.ref, coefficient)))

end FeeDenomination

final case class Fee[DD <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  denomination: FeeDenomination[DD],
  kind: FeeKind,
  coordinate: BigInt,
  asset: Asset { type D = DD },
  amount: Quantity[DD],
  residual: Quantity[DD],
  unrounded: Quantity[DD])

final case class FeeLine[DD <: Dim, M] private[instrument] (
  instrumentId: InstrumentId,
  fee: Fee[DD],
  sourceSliceIndex: Int,
  sourceMarket: M)

trait FeeSchedule[L, P, M, Pos]:
  def instrumentId: InstrumentId
  def assess(
    scenario: OrderScenario[L, P, M, Pos]
  ): Either[EconomicsError, Vector[FeeLine[? <: Dim, M]]]

private[instrument] object Fees:
  def minimumCharge(accountContribution: Rational, nonnegativeMinimum: Rational): Either[Rational, Rational] =
    if nonnegativeMinimum.signum < 0 then Left(nonnegativeMinimum)
    else if accountContribution.signum < 0 && accountContribution.abs.compare(nonnegativeMinimum) < 0 then
      Right(-nonnegativeMinimum)
    else Right(accountContribution)

  def percentageContribution(nonnegativeBasis: Rational, rate: Rational): Either[Rational, Rational] =
    if nonnegativeBasis.signum < 0 then Left(nonnegativeBasis)
    else Right(nonnegativeBasis * -rate)

end Fees

final class Fees[D <: Dim, B <: Dim, Q <: Dim, S <: Dim] private[instrument] (
  val instrumentId: InstrumentId,
  settle: Asset { type D = S }):

  private type Lots     = _root_.trading.economics.instrument.Lots[D]
  private type Price    = _root_.trading.economics.instrument.Price[B, Q]
  private type Market   = _root_.trading.economics.instrument.MarketState[B, Q, S]
  private type Position = _root_.trading.economics.instrument.Position[D]
  private type Scenario = _root_.trading.economics.instrument.OrderScenario[Lots, Price, Market, Position]
  private type Schedule = _root_.trading.economics.instrument.FeeSchedule[Lots, Price, Market, Position]

  def denomination(
    feeAsset: Asset
  )(
    grid: GridHandle[? <: Dim],
    policy: QuantizationPolicy
  ): Either[EconomicsError, FeeDenomination[feeAsset.D]] =
    if DimensionHandle.sameLineage(feeAsset.dimension, settle.dimension).isLeft then
      Left(ForeignReferenceDataLineage("fee asset", settle.dimension.key, feeAsset.dimension.key))
    else if DimensionHandle.sameLineage(grid.dimension, feeAsset.dimension).isLeft then
      Left(ForeignReferenceDataLineage("fee grid", feeAsset.dimension.key, grid.dimension.key))
    else if grid.dimension.key != feeAsset.dimension.key then
      Left(InvalidFeeGrid(feeAsset.id, grid.key, feeAsset.dimension.key, grid.dimension.key))
    else
      // The preceding lineage and canonical-key checks establish the existential grid's exact asset dimension. This
      // cast is private to denomination construction and the resulting checked handle retains the evidence.
      val typedGrid = grid.asInstanceOf[GridHandle[feeAsset.D]]
      Right(new FeeDenomination(instrumentId, feeAsset, typedGrid, policy))

  def line[FD <: Dim](
    scenario: Scenario,
    sourceSliceIndex: Int,
    fee: Fee[FD]
  ): Either[EconomicsError, FeeLine[FD, Market]] =
    val slices = scenario.assumptions.matchedSlices.toVector
    if sourceSliceIndex < 0 || sourceSliceIndex >= slices.size then
      Left(InvalidFeeAttribution(sourceSliceIndex, slices.size))
    else
      val market = slices(sourceSliceIndex).market
      IdentityChecks
        .check(
          "fee.line",
          instrumentId,
          "scenario"     -> scenario.instrumentId,
          "fee"          -> fee.instrumentId,
          "denomination" -> fee.denomination.instrumentId,
          "market"       -> market.instrumentId
        )
        .map(_ => FeeLine(instrumentId, fee, sourceSliceIndex, market))

  val none: Schedule = new Schedule:
    val instrumentId: InstrumentId                                                            = Fees.this.instrumentId
    def assess(scenario: Scenario): Either[EconomicsError, Vector[FeeLine[? <: Dim, Market]]] =
      IdentityChecks.check("fee.none", instrumentId, "scenario" -> scenario.instrumentId).map(_ =>
        Vector.empty
      )

  def combine(componentSchedules: Vector[Schedule]): Either[EconomicsError, Schedule] =
    IdentityChecks
      .check(
        "fee.combine",
        instrumentId,
        componentSchedules.zipWithIndex.map((schedule, index) => s"schedules[$index]" -> schedule.instrumentId)*
      )
      .map: _ =>
        new Schedule:
          val instrumentId: InstrumentId = Fees.this.instrumentId
          def assess(
            scenario: Scenario
          ): Either[EconomicsError, Vector[FeeLine[? <: Dim, Market]]] =
            for
              _          <- IdentityChecks.check("fee.assess", instrumentId, "scenario" -> scenario.instrumentId)
              components <- componentSchedules.traverse: schedule =>
                              schedule.assess(scenario).flatMap: next =>
                                IdentityChecks
                                  .check(
                                    "fee.assess",
                                    instrumentId,
                                    next.zipWithIndex.flatMap((line, index) =>
                                      Vector(
                                        s"lines[$index]"        -> line.instrumentId,
                                        s"lines[$index].fee"    -> line.fee.instrumentId,
                                        s"lines[$index].market" -> line.sourceMarket.instrumentId
                                      )
                                    )*
                                  )
                                  .map(_ => Chain.fromSeq(next))
            yield components.foldLeft(Chain.empty[FeeLine[? <: Dim, Market]])(_ ++ _).toVector

end Fees
