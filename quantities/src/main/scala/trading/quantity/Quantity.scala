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
 * [[DimKey]] that callers can recover. Public constructors that attach caller-supplied coefficients require an
 * authoritative `DimRef[D]`. Every normally returned value therefore has an authoritative construction path, while
 * operations that transform trusted carriers require no repeated dimension capability.
 */
opaque type Quantity[D <: Dim] = Rational

/** Static zero, witness-requiring coefficient constructors, and dimension-safe arithmetic for [[Quantity]]. */
object Quantity:

  /** Maximum absolute Java BigDecimal scale accepted by the eager exact constructor. */
  val MaximumFiniteDecimalScaleMagnitude: Int = 1_000_000

  def zero[D <: Dim](using dimension: DimRef[D]): Quantity[D] =
    val _ = dimension.key
    fromCoefficient(Rational.zero)

  def apply[D <: Dim](dimension: DimRef[D], coefficient: Rational): Quantity[D] =
    val _ = dimension.key
    fromCoefficient(coefficient)

  def apply[D <: Dim](d: DimRef[D], coefficient: BigInt): Quantity[D] =
    apply(d, Rational(coefficient))

  def apply[D <: Dim](d: DimRef[D], coefficient: Int): Quantity[D] =
    apply(d, BigInt(coefficient))

  def apply[D <: Dim](d: DimRef[D], coefficient: Long): Quantity[D] =
    apply(d, BigInt(coefficient))

  def apply[D <: Dim](d: DimRef[D], coefficient: String): Either[String, Quantity[D]] =
    Rational.parse(coefficient).map(parsed => apply(d, parsed))

  def apply[D <: Dim](d: DimRef[D], coefficient: BigDecimal): Either[UnsupportedFiniteDecimalScale, Quantity[D]] =
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

  private def fromCoefficient[D <: Dim](coefficient: Rational): Quantity[D] =
    Objects.requireNonNull(coefficient, "quantity coefficient")

  private def add[D <: Dim](l: Quantity[D], r: Quantity[D]): Quantity[D] =
    fromCoefficient(l.coefficient + r.coefficient)

  private def subtract[D <: Dim](l: Quantity[D], r: Quantity[D]): Quantity[D] =
    fromCoefficient(l.coefficient - r.coefficient)

  private def scale[D <: Dim](v: Quantity[D], s: Rational): Quantity[D] =
    fromCoefficient(v.coefficient * s)

  private def multiply[A <: Dim, B <: Dim](
    l: Quantity[A],
    r: Quantity[B]
  ): Quantity[Times[A, B]] =
    fromCoefficient(l.coefficient * r.coefficient)

  private def convert[F <: Dim, T <: Dim](
    v: Quantity[F],
    r: Rate[F, T]
  ): Quantity[T] =
    fromCoefficient(v.coefficient * r.coefficient)

  private def compose[A <: Dim, B <: Dim, C <: Dim](
    f: Rate[A, B],
    s: Rate[B, C]
  ): Rate[A, C] =
    fromCoefficient(f.coefficient * s.coefficient)

  private def cross[A <: Dim, B <: Dim, C <: Dim](
    numerator: Rate[A, B],
    denominator: NonZero[Rate[C, B]]
  ): Rate[A, C] =
    fromCoefficient(quotient(numerator.coefficient, denominator.unrefined.coefficient))

  private def reciprocal[A <: Dim, B <: Dim](rate: NonZero[Rate[A, B]]): Rate[B, A] =
    fromCoefficient(quotient(Rational.one, rate.unrefined.coefficient))

  private def quotient(l: Rational, r: Rational): Rational =
    Rational(l.numerator * r.denominator, l.denominator * r.numerator)

  private def divide[D <: Dim, E <: Dim](
    v: Quantity[D],
    d: NonZero[Quantity[E]]
  ): Quantity[Divide[D, E]] =
    fromCoefficient(quotient(v.coefficient, d.unrefined.coefficient))

  private def ratio[D <: Dim](
    v: Quantity[D],
    d: NonZero[Quantity[D]]
  ): Ratio =
    fromCoefficient(quotient(v.coefficient, d.unrefined.coefficient))

  private def exactDivide[D <: Dim](v: Quantity[D], d: NonZeroWhole): Quantity[D] =
    fromCoefficient(quotient(v.coefficient, Rational(d.unrefined)))

  extension [D <: Dim](v: Quantity[D])

    def coefficient: Rational =
      v

    /** Explicitly align this value to an equivalent static dimension spelling without changing its coefficient. */
    def alignTo[Target <: Dim](using same: SameDimension[D, Target]): Quantity[Target] =
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
    def *[E <: Dim](
      r: Quantity[E]
    ): Quantity[Times[D, E]] =
      multiply(v, r)

    def multiplyExact[E <: Dim, G](
      r: GridQuantity[E, G],
      g: Grid[E, G]
    ): Quantity[Times[D, E]] =
      v * g.asQuantity(r)

    def applyRate[E <: Dim](
      r: Rate[D, E]
    ): Quantity[E] =
      convert(v, r)

    def divideBy[E <: Dim](
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

  extension [A <: Dim, B <: Dim](f: Rate[A, B])
    def andThen[C <: Dim](
      s: Rate[B, C]
    ): Rate[A, C] =
      compose(f, s)

    /** Divide rates with a common target and expose the remaining endpoint orientation directly. */
    def crossRate[C <: Dim](
      denominator: NonZero[Rate[C, B]]
    ): Rate[A, C] =
      cross(f, denominator)

  extension [A <: Dim, B <: Dim](rate: NonZero[Rate[A, B]])
    /** Return the exact checked reciprocal with its endpoint orientation reversed. */
    def reciprocalRate: Rate[B, A] =
      reciprocal(rate)

end Quantity
