package trading.economics

import trading.quantity.*

private[economics] object InstrumentFees:
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

  def validateQuantization(
    asset: AssetId,
    settleDimension: DimensionKey,
    assetDimension: DimensionKey,
    assetSharesSettleRegistry: Boolean,
    grid: GridKey,
    gridDimension: DimensionKey,
    gridSharesAssetRegistry: Boolean
  ): Either[EconomicsError, Unit] =
    if !assetSharesSettleRegistry then Left(ForeignRegistry("fee asset", settleDimension, assetDimension))
    else if !gridSharesAssetRegistry then Left(ForeignRegistry("fee grid", assetDimension, gridDimension))
    else if gridDimension != assetDimension then Left(InvalidFeeGrid(asset, grid, assetDimension, gridDimension))
    else Right(())

  def validateAttribution(sliceIndex: Int, sliceCount: Int): Either[EconomicsError, Int] =
    if sliceIndex < 0 || sliceIndex >= sliceCount then Left(InvalidFeeAttribution(sliceIndex, sliceCount))
    else Right(sliceIndex)

  def combine[A, S, L](
    schedules: Vector[A],
    scenario: S
  )(
    assess: (A, S) => Either[EconomicsError, Vector[L]]
  ): Either[EconomicsError, Vector[L]] =
    schedules.foldLeft[Either[EconomicsError, Vector[L]]](Right(Vector.empty)): (result, schedule) =>
      for
        accumulated <- result
        next        <- assess(schedule, scenario)
      yield accumulated ++ next

end InstrumentFees
