package trading.quantity

import java.math.BigDecimal
import scala.annotation.targetName

import trading.quantity.GridRef.Grid
import trading.quantity.refinement.*

/**
 * An exact rational quantity in dimension `D`.
 *
 * The opaque representation prevents a raw scalar from being mistaken for a dimensional value, while `D` prevents
 * incompatible quantities from being combined. Public construction requires a [[DimRef]] that ties `D` to its runtime
 * identity.
 */
opaque type Quantity[D <: Dimension] = Rational

/** Witness-requiring constructors and dimension-safe arithmetic for [[Quantity]]. */
object Quantity:

  /** Maximum absolute Java BigDecimal scale accepted by the eager exact constructor. */
  val MaximumFiniteDecimalScaleMagnitude: Int = 1_000_000

  def zero[D <: Dimension]: Quantity[D] = Rational.zero

  def apply[D <: Dimension](dimension: DimRef[D], coefficient: Rational): Quantity[D] =
    val _ = dimension.key
    fromCoefficient(coefficient)

  def apply[D <: Dimension](d: DimRef[D], coefficient: BigInt): Quantity[D] =
    apply(d, Rational(coefficient))

  def apply[D <: Dimension](d: DimRef[D], coefficient: Int): Quantity[D] =
    apply(d, BigInt(coefficient))

  def apply[D <: Dimension](d: DimRef[D], coefficient: Long): Quantity[D] =
    apply(d, BigInt(coefficient))

  def apply[D <: Dimension](d: DimRef[D], coefficient: String): Either[String, Quantity[D]] =
    Rational.parse(coefficient).map(parsed => apply(d, parsed))

  def apply[D <: Dimension](d: DimRef[D], coefficient: BigDecimal): Either[UnsupportedFiniteDecimalScale, Quantity[D]] =
    validateFiniteDecimalScale(coefficient.scale)
      .map(scale => apply(d, rationalFromFiniteDecimal(coefficient, scale)))

  private def validateFiniteDecimalScale(scale: Int): Either[UnsupportedFiniteDecimalScale, Int] =
    if scale == Int.MinValue || scale.abs > MaximumFiniteDecimalScaleMagnitude then
      Left(UnsupportedFiniteDecimalScale(scale, MaximumFiniteDecimalScaleMagnitude))
    else
      Right(scale)

  private def rationalFromFiniteDecimal(coefficient: BigDecimal, scale: Int): Rational =
    val unscaledValue = BigInt(coefficient.unscaledValue)

    if scale >= 0 then
      Rational(unscaledValue, decimalScaleFactor(scale))
    else
      Rational(unscaledValue * decimalScaleFactor(-scale))

  private def decimalScaleFactor(scaleMagnitude: Int): BigInt =
    BigInt(10).pow(scaleMagnitude)

  private def fromCoefficient[D <: Dimension](coefficient: Rational): Quantity[D] =
    coefficient

  private def add[D <: Dimension](l: Quantity[D], r: Quantity[D]): Quantity[D] =
    l.coefficient + r.coefficient

  private def subtract[D <: Dimension](l: Quantity[D], r: Quantity[D]): Quantity[D] =
    l.coefficient - r.coefficient

  private def scale[D <: Dimension](v: Quantity[D], s: Rational): Quantity[D] =
    v.coefficient * s

  private def multiply[A <: Dimension, B <: Dimension](l: Quantity[A], r: Quantity[B]): Quantity[Times[A, B]] =
    l.coefficient * r.coefficient

  private def convert[F <: Dimension, T <: Dimension](v: Quantity[F], r: Rate[F, T]): Quantity[T] =
    v.coefficient * r.coefficient

  private def compose[A <: Dimension, B <: Dimension, C <: Dimension](f: Rate[A, B], s: Rate[B, C]): Rate[A, C] =
    f.coefficient * s.coefficient

  private def quotient(l: Rational, r: Rational): Rational =
    Rational(l.numerator * r.denominator, l.denominator * r.numerator)

  private def divide[D <: Dimension, E <: Dimension](v: Quantity[D], d: NonZero[Quantity[E]]): Quantity[Divide[D, E]] =
    quotient(v.coefficient, d.unrefined.coefficient)

  private def ratio[D <: Dimension](v: Quantity[D], d: NonZero[Quantity[D]]): Ratio =
    quotient(v.coefficient, d.unrefined.coefficient)

  private def exactDivide[D <: Dimension](v: Quantity[D], d: NonZeroWhole): Quantity[D] =
    quotient(v.coefficient, Rational(d.unrefined))

  extension [D <: Dimension](v: Quantity[D])

    def coefficient: Rational =
      v

    def +(r: Quantity[D]): Quantity[D] =
      add(v, r)

    def -(r: Quantity[D]): Quantity[D] =
      subtract(v, r)

    @targetName("scaleQuantityByRational")
    def *(s: Rational): Quantity[D] =
      scale(v, s)

    @targetName("multiplyQuantities")
    def *[E <: Dimension](r: Quantity[E]): Quantity[Times[D, E]] =
      multiply(v, r)

    def multiplyExact[E <: Dimension, G](r: GridQuantity[E, G], g: Grid[E, G]): Quantity[Times[D, E]] =
      v * g.asQuantity(r)

    def applyRate[E <: Dimension](r: Rate[D, E]): Quantity[E] =
      convert(v, r)

    def divideBy[E <: Dimension](d: NonZero[Quantity[E]]): Quantity[Divide[D, E]] =
      divide(v, d)

    def ratioTo(d: NonZero[Quantity[D]]): Ratio =
      ratio(v, d)

    def exactDivideBy(d: NonZeroWhole): Quantity[D] =
      exactDivide(v, d)

  end extension

  extension [A <: Dimension, B <: Dimension](f: Rate[A, B])
    def andThen[C <: Dimension](s: Rate[B, C]): Rate[A, C] =
      compose(f, s)

end Quantity
