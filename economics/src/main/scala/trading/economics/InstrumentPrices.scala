package trading.economics

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

/** Strictly positive grid price for one ordinary runtime instrument identity. */
final case class InstrumentPrice[B <: Dimension, Q <: Dimension] private[economics] (
  instrumentId: InstrumentId,
  ticks: PositiveWhole,
  coefficient: Rational,
  rate: Rate[B, Q])

final class InstrumentPrices[B <: Dimension, Q <: Dimension] private[economics] (
  instrumentId: InstrumentId,
  base: AssetRef { type D = B },
  quote: AssetRef { type D = Q },
  grid: RegisteredGridRef[Divide[Q, B]]):

  def exact(coefficient: Rational): Either[EconomicsError, InstrumentPrice[B, Q]] =
    fromRate(Rate(base.dimension.asDimensionRef, quote.dimension.asDimensionRef, coefficient))

  def fromRate(value: Rate[B, Q]): Either[EconomicsError, InstrumentPrice[B, Q]] =
    value
      .narrowExactlyTo(grid.asGridRef)
      .left
      .map(PriceNotOnGrid(_))
      .flatMap(refine)

  def fromTicks(ticks: PositiveWhole): InstrumentPrice[B, Q] =
    val payload = Positive(grid.fromCoordinate(ticks.unrefined)).toOption.get
    make(payload)

  def quantize(
    coefficient: Rational,
    policy: QuantizationPolicy
  ): Either[EconomicsError, (InstrumentPrice[B, Q], Quantity[Divide[Q, B]])] =
    quantizeRate(Rate(base.dimension.asDimensionRef, quote.dimension.asDimensionRef, coefficient), policy)

  def quantizeRate(
    value: Rate[B, Q],
    policy: QuantizationPolicy
  ): Either[EconomicsError, (InstrumentPrice[B, Q], Quantity[Divide[Q, B]])] =
    val result = value.quantizeTo(grid.asGridRef, policy)
    refine(result.value).map(_ -> result.residual)

  private def refine(
    value: GridQuantity[Divide[Q, B], grid.G]
  ): Either[EconomicsError, InstrumentPrice[B, Q]] =
    Positive(value)
      .left
      .map(_ => InvalidPriceCoordinate(grid.coordinate(value)))
      .map(make)

  private def make(payload: Positive[GridQuantity[Divide[Q, B], grid.G]]): InstrumentPrice[B, Q] =
    val ticks       = PositiveWhole(grid.coordinate(payload.unrefined)).toOption.get
    val coefficient = grid.asQuantity(payload.unrefined).coefficient
    InstrumentPrice(instrumentId, ticks, coefficient,
      Rate(base.dimension.asDimensionRef, quote.dimension.asDimensionRef,
        coefficient))

end InstrumentPrices
