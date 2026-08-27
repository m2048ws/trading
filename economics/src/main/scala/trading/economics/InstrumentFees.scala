package trading.economics

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.runtime.*

final class InstrumentFeeDenomination[DD <: Dimension] private[economics] (
  val instrumentId: InstrumentId,
  val asset: AssetRef { type D = DD },
  private val grid: RegisteredGridRef[DD],
  val policy: QuantizationPolicy):

  val gridKey: GridKey      = grid.key
  val gridQuantum: Rational = grid.quantum.unrefined

  def minimumCharge(
    accountContribution: Quantity[DD],
    nonnegativeMinimum: Quantity[DD]
  ): Either[EconomicsError, Quantity[DD]] =
    InstrumentFees
      .minimumCharge(accountContribution.coefficient, nonnegativeMinimum.coefficient)
      .left
      .map(coefficient => InvalidFeeBasis(asset.id, coefficient))
      .map(coefficient => Quantity(asset.dimension.asDimensionRef, coefficient))

  def quantize(kind: FeeKind, unrounded: Quantity[DD]): InstrumentFee[DD] =
    val result = unrounded.quantizeTo(grid.asGridRef, policy)
    InstrumentFee(
      instrumentId,
      this,
      kind,
      grid.coordinate(result.value),
      asset,
      grid.asQuantity(result.value),
      result.residual,
      unrounded
    )

  def percentage(
    kind: FeeKind,
    nonnegativeBasis: Quantity[DD],
    rate: FeeRate
  ): Either[EconomicsError, InstrumentFee[DD]] =
    InstrumentFees
      .percentageContribution(nonnegativeBasis.coefficient, rate.coefficient)
      .left
      .map(coefficient => InvalidFeeBasis(asset.id, coefficient))
      .map(coefficient => quantize(kind, Quantity(asset.dimension.asDimensionRef, coefficient)))

end InstrumentFeeDenomination

final case class InstrumentFee[DD <: Dimension] private[economics] (
  instrumentId: InstrumentId,
  denomination: InstrumentFeeDenomination[DD],
  kind: FeeKind,
  coordinate: BigInt,
  asset: AssetRef { type D = DD },
  amount: Quantity[DD],
  residual: Quantity[DD],
  unrounded: Quantity[DD])

final case class InstrumentFeeLine[DD <: Dimension, M] private[economics] (
  instrumentId: InstrumentId,
  fee: InstrumentFee[DD],
  sourceSliceIndex: Int,
  sourceMarket: M)

trait InstrumentFeeSchedule[L, P, M, Pos]:
  def instrumentId: InstrumentId
  def assess(
    scenario: InstrumentOrderScenario[L, P, M, Pos]
  ): Either[EconomicsError, Vector[InstrumentFeeLine[? <: Dimension, M]]]

private[economics] object InstrumentFees:
  def minimumCharge(accountContribution: Rational, nonnegativeMinimum: Rational): Either[Rational, Rational] =
    if nonnegativeMinimum.signum < 0 then Left(nonnegativeMinimum)
    else if accountContribution.signum < 0 && accountContribution.abs.compare(nonnegativeMinimum) < 0 then
      Right(-nonnegativeMinimum)
    else Right(accountContribution)

  def percentageContribution(nonnegativeBasis: Rational, rate: Rational): Either[Rational, Rational] =
    if nonnegativeBasis.signum < 0 then Left(nonnegativeBasis)
    else Right(nonnegativeBasis * -rate)

end InstrumentFees

final class InstrumentFees[D <: Dimension, B <: Dimension, Q <: Dimension, S <: Dimension] private[economics] (
  val instrumentId: InstrumentId,
  settle: AssetRef { type D = S }):

  private type Lots     = InstrumentLots[D]
  private type Price    = InstrumentPrice[B, Q]
  private type Market   = InstrumentMarketState[B, Q, S]
  private type Position = InstrumentPosition[D]
  private type Scenario = InstrumentOrderScenario[Lots, Price, Market, Position]
  private type Schedule = InstrumentFeeSchedule[Lots, Price, Market, Position]

  def denomination(
    feeAsset: AssetRef
  )(
    grid: RegisteredGridRef[? <: Dimension],
    policy: QuantizationPolicy
  ): Either[EconomicsError, InstrumentFeeDenomination[feeAsset.D]] =
    if !feeAsset.dimension.sharesRegistryWith(settle.dimension) then
      Left(ForeignRegistry("fee asset", settle.dimension.key, feeAsset.dimension.key))
    else if !grid.dimension.sharesRegistryWith(feeAsset.dimension) then
      Left(ForeignRegistry("fee grid", feeAsset.dimension.key, grid.dimension.key))
    else if grid.dimension.key != feeAsset.dimension.key then
      Left(InvalidFeeGrid(feeAsset.id, grid.key, feeAsset.dimension.key, grid.dimension.key))
    else
      val typedGrid = grid.asInstanceOf[RegisteredGridRef[feeAsset.D]]
      Right(new InstrumentFeeDenomination(instrumentId, feeAsset, typedGrid, policy))

  def line[FD <: Dimension](
    scenario: Scenario,
    sourceSliceIndex: Int,
    fee: InstrumentFee[FD]
  ): Either[EconomicsError, InstrumentFeeLine[FD, Market]] =
    val slices = scenario.assumptions.matchedSlices
    if sourceSliceIndex < 0 || sourceSliceIndex >= slices.size then
      Left(InvalidFeeAttribution(sourceSliceIndex, slices.size))
    else
      val market = slices(sourceSliceIndex).market
      InstrumentIdentityChecks
        .check(
          "fee.line",
          instrumentId,
          "scenario"     -> scenario.instrumentId,
          "fee"          -> fee.instrumentId,
          "denomination" -> fee.denomination.instrumentId,
          "market"       -> market.instrumentId
        )
        .map(_ => InstrumentFeeLine(instrumentId, fee, sourceSliceIndex, market))

  val none: Schedule = new Schedule:
    val instrumentId: InstrumentId = InstrumentFees.this.instrumentId
    def assess(scenario: Scenario): Either[EconomicsError, Vector[InstrumentFeeLine[? <: Dimension, Market]]] =
      InstrumentIdentityChecks.check("fee.none", instrumentId, "scenario" -> scenario.instrumentId).map(_ =>
        Vector.empty
      )

  def combine(componentSchedules: Vector[Schedule]): Either[EconomicsError, Schedule] =
    InstrumentIdentityChecks
      .check(
        "fee.combine",
        instrumentId,
        componentSchedules.zipWithIndex.map((schedule, index) => s"schedules[$index]" -> schedule.instrumentId)*
      )
      .map: _ =>
        new Schedule:
          val instrumentId: InstrumentId = InstrumentFees.this.instrumentId
          def assess(
            scenario: Scenario
          ): Either[EconomicsError, Vector[InstrumentFeeLine[? <: Dimension, Market]]] =
            for
              _     <- InstrumentIdentityChecks.check("fee.assess", instrumentId, "scenario" -> scenario.instrumentId)
              lines <- componentSchedules.foldLeft[
                         Either[EconomicsError,
                           Vector[InstrumentFeeLine[? <: Dimension, Market]]]
                       ](Right(Vector.empty)): (result, schedule) =>
                         for
                           accumulated <- result
                           next        <- schedule.assess(scenario)
                           _           <- InstrumentIdentityChecks.check(
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
                         yield accumulated ++ next
            yield lines

end InstrumentFees
