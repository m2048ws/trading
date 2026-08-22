package trading.quantity

import java.math.BigDecimal
import java.util.Objects
import scala.annotation.targetName

import trading.quantity.GridRef.Grid
import trading.quantity.refinement.*

/**
 * An exact rational quantity in dimension `D`.
 *
 * The opaque representation prevents a raw scalar from being mistaken for a dimensional value, while `D` prevents
 * incompatible quantities from being combined. A value stores only its exact coefficient and carries no [[DimRef]] or
 * [[DimensionKey]] that callers can recover. Public constructors that attach caller-supplied coefficients require an
 * authoritative `DimRef[D]`. Every normally returned value therefore has an authoritative construction path, while
 * operations that transform trusted carriers require no repeated dimension capability.
 */
opaque type Quantity[D <: Dimension] = Rational

/** Static zero, witness-requiring coefficient constructors, and dimension-safe arithmetic for [[Quantity]]. */
object Quantity:

  /** Maximum absolute Java BigDecimal scale accepted by the eager exact constructor. */
  val MaximumFiniteDecimalScaleMagnitude: Int = 1_000_000

  def zero[D <: Dimension](using dimension: DimRef[D]): Quantity[D] =
    val _ = dimension.key
    fromCoefficient(Rational.zero)

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
    Objects.requireNonNull(coefficient, "quantity coefficient")

  private def add[D <: Dimension](l: Quantity[D], r: Quantity[D]): Quantity[D] =
    fromCoefficient(l.coefficient + r.coefficient)

  private def subtract[D <: Dimension](l: Quantity[D], r: Quantity[D]): Quantity[D] =
    fromCoefficient(l.coefficient - r.coefficient)

  private def scale[D <: Dimension](v: Quantity[D], s: Rational): Quantity[D] =
    fromCoefficient(v.coefficient * s)

  private def multiply[A <: Dimension, B <: Dimension](
    l: Quantity[A],
    r: Quantity[B]
  ): Quantity[Times[A, B]] =
    fromCoefficient(l.coefficient * r.coefficient)

  private def convert[F <: Dimension, T <: Dimension](
    v: Quantity[F],
    r: Rate[F, T]
  ): Quantity[T] =
    fromCoefficient(v.coefficient * r.coefficient)

  private def compose[A <: Dimension, B <: Dimension, C <: Dimension](
    f: Rate[A, B],
    s: Rate[B, C]
  ): Rate[A, C] =
    fromCoefficient(f.coefficient * s.coefficient)

  private def cross[A <: Dimension, B <: Dimension, C <: Dimension](
    numerator: Rate[A, B],
    denominator: NonZero[Rate[C, B]]
  ): Rate[A, C] =
    fromCoefficient(quotient(numerator.coefficient, denominator.unrefined.coefficient))

  private def reciprocal[A <: Dimension, B <: Dimension](rate: NonZero[Rate[A, B]]): Rate[B, A] =
    fromCoefficient(quotient(Rational.one, rate.unrefined.coefficient))

  private def quotient(l: Rational, r: Rational): Rational =
    Rational(l.numerator * r.denominator, l.denominator * r.numerator)

  private def divide[D <: Dimension, E <: Dimension](
    v: Quantity[D],
    d: NonZero[Quantity[E]]
  ): Quantity[Divide[D, E]] =
    fromCoefficient(quotient(v.coefficient, d.unrefined.coefficient))

  private def ratio[D <: Dimension](
    v: Quantity[D],
    d: NonZero[Quantity[D]]
  ): Ratio =
    fromCoefficient(quotient(v.coefficient, d.unrefined.coefficient))

  private def exactDivide[D <: Dimension](v: Quantity[D], d: NonZeroWhole): Quantity[D] =
    fromCoefficient(quotient(v.coefficient, Rational(d.unrefined)))

  extension [D <: Dimension](v: Quantity[D])

    def coefficient: Rational =
      v

    /** Explicitly align this value to an equivalent static dimension spelling without changing its coefficient. */
    def alignTo[Target <: Dimension](using same: SameDimension[D, Target]): Quantity[Target] =
      val _ = Objects.requireNonNull(same, "same dimension evidence")
      v.asInstanceOf[Quantity[Target]]

    def +(r: Quantity[D]): Quantity[D] =
      add(v, r)

    def -(r: Quantity[D]): Quantity[D] =
      subtract(v, r)

    @targetName("scaleQuantityByRational")
    def *(s: Rational): Quantity[D] =
      scale(v, s)

    @targetName("multiplyQuantities")
    def *[E <: Dimension](
      r: Quantity[E]
    ): Quantity[Times[D, E]] =
      multiply(v, r)

    def multiplyExact[E <: Dimension, G](
      r: GridQuantity[E, G],
      g: Grid[E, G]
    ): Quantity[Times[D, E]] =
      v * g.asQuantity(r)

    def applyRate[E <: Dimension](
      r: Rate[D, E]
    ): Quantity[E] =
      convert(v, r)

    def divideBy[E <: Dimension](
      d: NonZero[Quantity[E]]
    ): Quantity[Divide[D, E]] =
      divide(v, d)

    def ratioTo(
      d: NonZero[Quantity[D]]
    ): Ratio =
      ratio(v, d)

    def exactDivideBy(d: NonZeroWhole): Quantity[D] =
      exactDivide(v, d)

  end extension

  extension [A <: Dimension, B <: Dimension](f: Rate[A, B])
    def andThen[C <: Dimension](
      s: Rate[B, C]
    ): Rate[A, C] =
      compose(f, s)

    /** Divide rates with a common target and expose the remaining endpoint orientation directly. */
    def crossRate[C <: Dimension](
      denominator: NonZero[Rate[C, B]]
    ): Rate[A, C] =
      cross(f, denominator)

  extension [A <: Dimension, B <: Dimension](rate: NonZero[Rate[A, B]])
    /** Return the exact checked reciprocal with its endpoint orientation reversed. */
    def reciprocalRate: Rate[B, A] =
      reciprocal(rate)

end Quantity
