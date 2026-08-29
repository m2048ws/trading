package trading.economics.instrument

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.reference.*

/** Strictly positive grid price for one ordinary runtime instrument identity. */
final case class Price[B <: Dim, Q <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  ticks: PositiveWhole,
  coefficient: Rational,
  rate: Rate[B, Q])

final class Prices[B <: Dim, Q <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  base: Asset { type D = B },
  quote: Asset { type D = Q },
  grid: GridHandle[Divide[Q, B]]):

  def exact(coefficient: Rational): Either[EconomicsError, Price[B, Q]] =
    fromRate(Rate(base.dimension.ref, quote.dimension.ref, coefficient))

  def fromRate(value: Rate[B, Q]): Either[EconomicsError, Price[B, Q]] =
    value
      .narrowExactlyTo(grid.grid)
      .left
      .map(PriceNotOnGrid(_))
      .flatMap(refine)

  def fromTicks(ticks: PositiveWhole): Price[B, Q] =
    val payload = Positive(grid.fromCoordinate(ticks.unrefined)).toOption.get
    make(payload)

  def quantize(
    coefficient: Rational,
    policy: QuantizationPolicy
  ): Either[EconomicsError, (Price[B, Q], Quantity[Divide[Q, B]])] =
    quantizeRate(Rate(base.dimension.ref, quote.dimension.ref, coefficient), policy)

  def quantizeRate(
    value: Rate[B, Q],
    policy: QuantizationPolicy
  ): Either[EconomicsError, (Price[B, Q], Quantity[Divide[Q, B]])] =
    val result = value.quantizeTo(grid.grid, policy)
    refine(result.value).map(_ -> result.residual)

  private def refine(
    value: GridQuantity[Divide[Q, B], grid.G]
  ): Either[EconomicsError, Price[B, Q]] =
    Positive(value)
      .left
      .map(_ => InvalidPriceCoordinate(grid.coordinate(value)))
      .map(make)

  private def make(payload: Positive[GridQuantity[Divide[Q, B], grid.G]]): Price[B, Q] =
    val ticks       = PositiveWhole(grid.coordinate(payload.unrefined)).toOption.get
    val coefficient = grid.asQuantity(payload.unrefined).coefficient
    Price(instrumentId, ticks, coefficient,
      Rate(base.dimension.ref, quote.dimension.ref,
        coefficient))

end Prices
