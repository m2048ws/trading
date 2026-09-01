package trading.economics.instrument

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.reference.*

/** Strictly positive grid price retaining its listing grid and endpoint-typed rate. */
final class Price[B <: Dim, Q <: Dim] private (
  val instrumentId: InstrumentId,
  val ticks: PositiveWhole,
  val grid: GridHandle[Divide[Q, B]],
  val rate: Rate[B, Q])
  extends JavaSerializationUnsupported:

  val coefficient: Rational = rate.coefficient

  override def equals(other: Any): Boolean =
    other match
      case that: Price[?, ?] =>
        instrumentId == that.instrumentId && ticks == that.ticks &&
        RetainedReferenceEquality.sameGrid(grid, that.grid) &&
        coefficient == that.coefficient
      case _ => false

  override def hashCode: Int =
    (instrumentId, ticks, RetainedReferenceEquality.gridHash(grid), coefficient).hashCode
end Price

/** Explicit residual-bearing result of quantizing an exact price rate. */
final case class PriceQuantization[B <: Dim, Q <: Dim](
  price: Price[B, Q],
  residual: Quantity[Divide[Q, B]])
  extends JavaSerializationUnsupported

object Price:
  def exact(instrument: Instrument)(coefficient: Rational): Either[PriceError, instrument.Price] =
    fromRate(instrument)(
      Rate(instrument.roles.base.dimension.ref, instrument.roles.quote.dimension.ref, coefficient)
    )

  def fromRate(
    instrument: Instrument
  )(
    value: Rate[instrument.roles.base.D, instrument.roles.quote.D]
  ): Either[PriceError, instrument.Price] =
    value
      .narrowExactlyTo(instrument.priceGrid.grid)
      .left
      .map(PriceNotOnGrid(_))
      .flatMap: coordinate =>
        PositiveWhole(instrument.priceGrid.coordinate(coordinate))
          .left
          .map(_ => InvalidPriceCoordinate(instrument.priceGrid.coordinate(coordinate)))
          .map(ticks => make(instrument)(ticks, coordinate))

  def fromTicks(instrument: Instrument)(ticks: PositiveWhole): instrument.Price =
    val coordinate = instrument.priceGrid.fromCoordinate(ticks.unrefined)
    make(instrument)(ticks, coordinate)

  def quantize(
    instrument: Instrument
  )(
    coefficient: Rational,
    policy: QuantizationPolicy
  ): Either[PriceError, PriceQuantization[instrument.roles.base.D, instrument.roles.quote.D]] =
    quantizeRate(instrument)(
      Rate(instrument.roles.base.dimension.ref, instrument.roles.quote.dimension.ref, coefficient),
      policy
    )

  def quantizeRate(
    instrument: Instrument
  )(
    value: Rate[instrument.roles.base.D, instrument.roles.quote.D],
    policy: QuantizationPolicy
  ): Either[PriceError, PriceQuantization[instrument.roles.base.D, instrument.roles.quote.D]] =
    val result = value.quantizeTo(instrument.priceGrid.grid, policy)
    PositiveWhole(instrument.priceGrid.coordinate(result.value))
      .left
      .map(_ => InvalidPriceCoordinate(instrument.priceGrid.coordinate(result.value)))
      .map: ticks =>
        PriceQuantization(make(instrument)(ticks, result.value), result.residual)

  private def make(
    instrument: Instrument
  )(
    ticks: PositiveWhole,
    coordinate: GridQuantity[
      Divide[instrument.roles.quote.D, instrument.roles.base.D],
      instrument.priceGrid.G
    ]
  ): instrument.Price =
    val coefficient = instrument.priceGrid.asQuantity(coordinate).coefficient
    new Price(
      instrument.identity.id,
      ticks,
      instrument.priceGrid,
      Rate(instrument.roles.base.dimension.ref, instrument.roles.quote.dimension.ref, coefficient)
    )
end Price
