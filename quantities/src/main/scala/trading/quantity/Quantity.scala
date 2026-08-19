package trading.quantity

import java.math.BigDecimal
import scala.annotation.targetName

import trading.quantity.GridRef.Grid
import trading.quantity.refinement.*

/**
 * An exact rational quantity in dimension `D`.
 *
 * The opaque representation prevents a raw scalar from being mistaken for a dimensional value, while `D` prevents
 * incompatible quantities from being combined. A value stores only its exact coefficient and carries no [[DimRef]] or
 * [[DimensionKey]] that callers can recover. Public constructors that attach caller-supplied coefficients require an
 * authoritative `DimRef[D]`; [[Quantity.zero]] instead requires only [[Normalize]] and therefore remains available for
 * normalized dimensions with no public runtime witness.
 */
opaque type Quantity[D <: Dimension] = Rational

/** Static zero, witness-requiring coefficient constructors, and dimension-safe arithmetic for [[Quantity]]. */
object Quantity:

  /** Maximum absolute Java BigDecimal scale accepted by the eager exact constructor. */
  val MaximumFiniteDecimalScaleMagnitude: Int = 1_000_000

  def zero[D <: Dimension](using valid: Normalize[D]): Quantity[D] =
    val _ = valid
    Rational.zero

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

  private def add[D <: Dimension, E <: Dimension](
    l: Quantity[D],
    r: Quantity[E]
  )(using
    left: Normalize[D],
    right: Normalize[E],
    same: SameDimension[D, E]
  ): Quantity[D] =
    val _ = (left, right, same)
    l.coefficient + r.coefficient

  private def subtract[D <: Dimension, E <: Dimension](
    l: Quantity[D],
    r: Quantity[E]
  )(using
    left: Normalize[D],
    right: Normalize[E],
    same: SameDimension[D, E]
  ): Quantity[D] =
    val _ = (left, right, same)
    l.coefficient - r.coefficient

  private def scale[D <: Dimension](v: Quantity[D], s: Rational)(using valid: Normalize[D]): Quantity[D] =
    val _ = valid
    v.coefficient * s

  private def multiply[A <: Dimension, B <: Dimension](
    l: Quantity[A],
    r: Quantity[B]
  )(using
    operation: Normalize[Times[A, B]]
  ): Quantity[operation.Out] =
    val _ = operation
    l.coefficient * r.coefficient

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

  private def quotient(l: Rational, r: Rational): Rational =
    Rational(l.numerator * r.denominator, l.denominator * r.numerator)

  private def divide[D <: Dimension, E <: Dimension](
    v: Quantity[D],
    d: NonZero[Quantity[E]]
  )(using
    operation: Normalize[Divide[D, E]]
  ): Quantity[operation.Out] =
    val _ = operation
    quotient(v.coefficient, d.unrefined.coefficient)

  private def ratio[D <: Dimension](
    v: Quantity[D],
    d: NonZero[Quantity[D]]
  ): Ratio =
    fromCoefficient(quotient(v.coefficient, d.unrefined.coefficient))

  private def exactDivide[D <: Dimension](v: Quantity[D], d: NonZeroWhole)(using valid: Normalize[D]): Quantity[D] =
    val _ = valid
    quotient(v.coefficient, Rational(d.unrefined))

  extension [D <: Dimension](v: Quantity[D])

    def coefficient: Rational =
      v

    /** Retag this value only when its normalized powers are proven equivalent to `Target`. */
    def asDimension[Target <: Dimension](using same: SameDimension[D, Target]): Quantity[Target] =
      same.coerceQuantity(v)

    def +[E <: Dimension](
      r: Quantity[E]
    )(using
      Normalize[D],
      Normalize[E],
      SameDimension[D, E]
    ): Quantity[D] =
      add(v, r)

    def -[E <: Dimension](
      r: Quantity[E]
    )(using
      Normalize[D],
      Normalize[E],
      SameDimension[D, E]
    ): Quantity[D] =
      subtract(v, r)

    @targetName("scaleQuantityByRational")
    def *(s: Rational)(using Normalize[D]): Quantity[D] =
      scale(v, s)

    @targetName("multiplyQuantities")
    def *[E <: Dimension](
      r: Quantity[E]
    )(using operation: Normalize[Times[D, E]]
    ): Quantity[operation.Out] =
      multiply(v, r)

    def multiplyExact[E <: Dimension, G](
      r: GridQuantity[E, G],
      g: Grid[E, G]
    )(using
      operation: Normalize[Times[D, E]]
    ): Quantity[operation.Out] =
      v * g.asQuantity(r)

    def applyRate[E <: Dimension](
      r: Rate[D, E]
    )(using
      operation: Normalize[Times[D, Divide[E, D]]]
    ): Quantity[E] =
      val _ = operation
      convert(v, r)

    def divideBy[E <: Dimension](
      d: NonZero[Quantity[E]]
    )(using
      operation: Normalize[Divide[D, E]]
    ): Quantity[operation.Out] =
      divide(v, d)

    def ratioTo(
      d: NonZero[Quantity[D]]
    )(using
      operation: Normalize[Divide[D, D]]
    ): Ratio =
      val _ = operation
      ratio(v, d)

    def exactDivideBy(d: NonZeroWhole)(using Normalize[D]): Quantity[D] =
      exactDivide(v, d)

  end extension

  extension [A <: Dimension, B <: Dimension](f: Rate[A, B])
    def andThen[C <: Dimension](
      s: Rate[B, C]
    )(using
      operation: Normalize[Times[Divide[B, A], Divide[C, B]]]
    ): Rate[A, C] =
      val _ = operation
      compose(f, s)

    /** Divide rates with a common target and expose the remaining endpoint orientation directly. */
    def crossRate[C <: Dimension](
      denominator: NonZero[Rate[C, B]]
    )(using
      operation: Normalize[Divide[Divide[B, A], Divide[B, C]]]
    ): Rate[A, C] =
      val _ = operation
      cross(f, denominator)

end Quantity
