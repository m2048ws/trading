package trading.economics

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

private[economics] final class InstrumentPricesImpl[O, B <: Dimension, Q <: Dimension](
  authority: Instrument.OwnerAuthority[O],
  base: AssetRef { type D = B },
  quote: AssetRef { type D = Q },
  grid: RegisteredGridRef[Divide[Q, B]])
  extends PriceCapability[O, B, Q]:

  def exact(coefficient: Rational): Either[EconomicsError, InstrumentPrice[O, B, Q]] =
    fromRate(Rate(base.dimension.asDimensionRef, quote.dimension.asDimensionRef, coefficient))

  def fromRate(value: Rate[B, Q]): Either[EconomicsError, InstrumentPrice[O, B, Q]] =
    value
      .narrowExactlyTo(grid.asGridRef)
      .left
      .map(PriceNotOnGrid(_))
      .flatMap(refine)

  def fromTicks(ticks: PositiveWhole): InstrumentPrice[O, B, Q] =
    val payload = Positive(grid.fromCoordinate(ticks.unrefined)).toOption.get
    authority.price(grid, base.dimension.asDimensionRef, quote.dimension.asDimensionRef)(payload)

  def quantize(
    coefficient: Rational,
    policy: QuantizationPolicy
  ): Either[EconomicsError, (InstrumentPrice[O, B, Q], Quantity[Divide[Q, B]])] =
    quantizeRate(Rate(base.dimension.asDimensionRef, quote.dimension.asDimensionRef, coefficient), policy)

  def quantizeRate(
    value: Rate[B, Q],
    policy: QuantizationPolicy
  ): Either[EconomicsError, (InstrumentPrice[O, B, Q], Quantity[Divide[Q, B]])] =
    val result = value.quantizeTo(grid.asGridRef, policy)
    refine(result.value).map(_ -> result.residual)

  private def refine(
    value: GridQuantity[Divide[Q, B], grid.G]
  ): Either[EconomicsError, InstrumentPrice[O, B, Q]] =
    Positive(value)
      .left
      .map(_ => InvalidPriceCoordinate(grid.coordinate(value)))
      .map(positive =>
        authority.price(grid, base.dimension.asDimensionRef, quote.dimension.asDimensionRef)(positive)
      )

end InstrumentPricesImpl
