package trading.economics

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.runtime.*

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

private[economics] final class InstrumentFeesImpl[
  O,
  D <: Dimension,
  B <: Dimension,
  Q <: Dimension,
  S <: Dimension
](
  authority: Instrument.OwnerAuthority[O],
  settle: AssetRef { type D = S })
  extends FeeCapability[
    O,
    InstrumentLots[O, D],
    InstrumentPrice[O, B, Q],
    InstrumentMarketState[O, B, Q, S],
    InstrumentPosition[O, D]
  ]:

  private type Lots     = InstrumentLots[O, D]
  private type Price    = InstrumentPrice[O, B, Q]
  private type Market   = InstrumentMarketState[O, B, Q, S]
  private type Position = InstrumentPosition[O, D]
  private type Scenario = InstrumentOrderScenario[O, Lots, Price, Market, Position]
  private type Schedule = InstrumentFeeSchedule[O, Lots, Price, Market, Position]

  def denomination(
    feeAsset: AssetRef
  )(
    grid: RegisteredGridRef[? <: Dimension],
    policy: QuantizationPolicy
  ): Either[EconomicsError, InstrumentFeeDenomination[O] { type D = feeAsset.D }] =
    if !feeAsset.dimension.sharesRegistryWith(settle.dimension) then
      Left(ForeignRegistry("fee asset", settle.dimension.key, feeAsset.dimension.key))
    else if !grid.dimension.sharesRegistryWith(feeAsset.dimension) then
      Left(ForeignRegistry("fee grid", feeAsset.dimension.key, grid.dimension.key))
    else if grid.dimension.key != feeAsset.dimension.key then
      Left(InvalidFeeGrid(feeAsset.id, grid.key, feeAsset.dimension.key, grid.dimension.key))
    else
      val typedGrid = grid.asInstanceOf[RegisteredGridRef[feeAsset.D]]
      Right(authority.feeDenomination(feeAsset, typedGrid, policy))

  def line(
    scenario: Scenario,
    sourceSliceIndex: Int,
    fee: InstrumentFee[O]
  ): Either[EconomicsError, InstrumentFeeLine[O, Market]] =
    val slices = scenario.assumptions.matchedSlices
    if sourceSliceIndex < 0 || sourceSliceIndex >= slices.size then
      Left(InvalidFeeAttribution(sourceSliceIndex, slices.size))
    else
      Right(
        authority.feeLine(
          scenario,
          fee,
          sourceSliceIndex,
          slices(sourceSliceIndex).market
        )
      )

  val none: Schedule = new Schedule:
    def assess(scenario: Scenario): Either[EconomicsError, Vector[InstrumentFeeLine[O, Market]]] = Right(Vector.empty)

  def combine(componentSchedules: Vector[Schedule]): Schedule = new Schedule:
    def assess(scenario: Scenario): Either[EconomicsError, Vector[InstrumentFeeLine[O, Market]]] =
      componentSchedules.foldLeft[Either[EconomicsError, Vector[InstrumentFeeLine[O, Market]]]](Right(Vector.empty)):
        (result, schedule) =>
          for
            accumulated <- result
            next        <- schedule.assess(scenario)
          yield accumulated ++ next

end InstrumentFeesImpl
