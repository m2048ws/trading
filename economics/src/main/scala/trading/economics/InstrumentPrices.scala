package trading.economics

import trading.quantity.Dimension
import trading.quantity.Rational
import trading.quantity.grid.NotOnGrid
import trading.quantity.grid.QuantizationPolicy

private[economics] object InstrumentPrices:
  final case class Observation(coordinate: BigInt, coefficient: Rational)

  def validateCoordinate(coordinate: BigInt): Either[EconomicsError, Unit] =
    if coordinate.signum <= 0 then Left(InvalidPriceCoordinate(coordinate)) else Right(())

  def fromCoordinate[A](coordinate: BigInt)(build: BigInt => A): Either[EconomicsError, A] =
    validateCoordinate(coordinate).map(_ => build(coordinate))

  def exact[D <: Dimension, A](
    narrow: () => Either[NotOnGrid[D], A]
  )(
    coordinate: A => BigInt
  ): Either[EconomicsError, A] =
    narrow().left.map(PriceNotOnGrid(_)).flatMap: value =>
      validateCoordinate(coordinate(value)).map(_ => value)

  def quantized[A, R](
    policy: QuantizationPolicy
  )(
    quantize: QuantizationPolicy => (A, R)
  )(
    coordinate: A => BigInt
  ): Either[EconomicsError, (A, R)] =
    val selected = quantize(policy)
    validateCoordinate(coordinate(selected._1)).map(_ => selected)

  def observe[A](value: A)(coordinate: A => BigInt)(coefficient: A => Rational): Observation =
    Observation(coordinate(value), coefficient(value))

end InstrumentPrices
