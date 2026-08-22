package trading.quantity.refinement

import scala.annotation.targetName

import trading.quantity.*

/** Closed hierarchy of failed refinement checks. */
sealed abstract class RefinementError extends JavaSerializationUnsupported with Product with Serializable

/** A value expected to be nonnegative was negative. */
case object ExpectedNonNegative extends RefinementError

/** A value expected to be positive was zero or negative. */
case object ExpectedPositive extends RefinementError

/** A value expected to be nonzero was zero. */
case object ExpectedNonZero extends RefinementError

/** Closed, exact sign observation for the carriers supported by the refinement API. */
final class Sign[A] private (f: A => Int):
  def signum(v: A): Int =
    f(v)

/** Closed sign capabilities for the exact scalar and quantity carriers supported by refinement constructors. */
object Sign:
  given intSign: Sign[Int]           = new Sign(java.lang.Integer.signum)
  given bigIntSign: Sign[BigInt]     = new Sign(_.signum)
  given rationalSign: Sign[Rational] = new Sign(_.signum)

  given quantitySign[D <: Dimension]: Sign[Quantity[D]] =
    rationalSign.asInstanceOf[Sign[Quantity[D]]]

  given gridQuantitySign[D <: Dimension, G]: Sign[GridQuantity[D, G]] =
    bigIntSign.asInstanceOf[Sign[GridQuantity[D, G]]]

end Sign

/** A value of `A` whose closed [[Sign]] observation is greater than or equal to zero. */
opaque type NonNegative[A] = A

/** Validated construction, unwrapping, and closed identities for [[NonNegative]]. */
object NonNegative:

  private def fromChecked[A](v: A)(using s: Sign[A]): Either[ExpectedNonNegative.type, NonNegative[A]] =
    if s.signum(v) >= 0 then
      Right(v)
    else
      Left(ExpectedNonNegative)

  def apply[A](v: A)(using Sign[A]): Either[ExpectedNonNegative.type, NonNegative[A]] =
    fromChecked(v)

  extension [A](v: NonNegative[A])
    def unrefined: A =
      v

  /** Additive identity for the closed nonnegative exact-quantity structure. */
  def quantityZero[D <: Dimension](using DimRef[D]): NonNegative[Quantity[D]] = Quantity.zero

  /** Additive identity for the closed nonnegative grid-quantity structure. */
  def gridQuantityZero[D <: Dimension, G](using DimRef[D]): NonNegative[GridQuantity[D, G]] = GridQuantity.zero

  extension [D <: Dimension, G](v: NonNegative[GridQuantity[D, G]])

    @targetName("quotRemNonNegativeGrid")
    def quotRemBy(
      d: PositiveWhole,
      g: GridRef.Grid[D, G]
    ): RefinedQuotRem[NonNegative[GridQuantity[D, G]], NonNegative[GridQuantity[D, G]]] =
      val result = trading.quantity.grid.quotRemBy(NonNegative.unrefined(v))(d, g)
      RefinedQuotRem(result.quotient, result.remainder)

  end extension

end NonNegative

/** A value of `A` whose closed [[Sign]] observation is not zero. */
opaque type NonZero[A] = A

/** Validated construction and closed exact-rational operations for [[NonZero]]. */
object NonZero:

  private def fromChecked[A](v: A)(using s: Sign[A]): Either[ExpectedNonZero.type, NonZero[A]] =
    if s.signum(v) != 0 then
      Right(v)
    else
      Left(ExpectedNonZero)

  def apply[A](v: A)(using Sign[A]): Either[ExpectedNonZero.type, NonZero[A]] =
    fromChecked(v)

  extension [A](v: NonZero[A])
    def unrefined: A =
      v

  /** Multiplicative identity for exact nonzero rational arithmetic. */
  def rationalOne: NonZero[Rational] = Rational.one

  extension (l: NonZero[Rational])

    /** Exact multiplication, closed because neither rational factor is zero. */
    def multiply(r: NonZero[Rational]): NonZero[Rational] =
      l * r

    /** Exact reciprocal, closed because the rational numerator is nonzero. */
    def reciprocal: NonZero[Rational] =
      Rational(l.denominator, l.numerator)

  end extension

  extension [D <: Dimension, G](v: NonZero[GridQuantity[D, G]])

    @targetName("quotRemNonZeroGrid")
    def quotRemBy(
      d: PositiveWhole,
      g: GridRef.Grid[D, G]
    ): RefinedQuotRem[GridQuantity[D, G], NonNegative[GridQuantity[D, G]]] =
      val result = trading.quantity.grid.quotRemBy(NonZero.unrefined(v))(d, g)
      RefinedQuotRem(result.quotient, result.remainder)

  end extension

end NonZero

/** A value of `A` whose closed [[Sign]] observation is strictly greater than zero. */
opaque type Positive[A] = A

/** Validated construction and safe weakening operations for [[Positive]]. */
object Positive:

  private def fromChecked[A](v: A)(using s: Sign[A]): Either[ExpectedPositive.type, Positive[A]] =
    if s.signum(v) > 0 then
      Right(v)
    else
      Left(ExpectedPositive)

  def apply[A](v: A)(using Sign[A]): Either[ExpectedPositive.type, Positive[A]] =
    fromChecked(v)

  extension [A](v: Positive[A])
    def unrefined: A =
      v

    def asNonNegative: NonNegative[A] =
      v

    def asNonZero: NonZero[A] =
      v

  /** Widening a positive `Int` to `BigInt` preserves positivity. */
  extension (v: Positive[Int])
    def toPositiveWhole: Positive[BigInt] =
      BigInt(v)

  extension [D <: Dimension, G](v: Positive[GridQuantity[D, G]])

    @targetName("quotRemPositiveGrid")
    def quotRemBy(
      d: PositiveWhole,
      g: GridRef.Grid[D, G]
    ): RefinedQuotRem[NonNegative[GridQuantity[D, G]], NonNegative[GridQuantity[D, G]]] =
      val result = trading.quantity.grid.quotRemBy(Positive.unrefined(v))(d, g)
      RefinedQuotRem(result.quotient, result.remainder)

  end extension

end Positive

/* Closed-operation implementations stay in this lexical source scope so their result refinements are attached directly.
 * The public constructors above remain the only checked entry points for caller-supplied values. */
extension [D <: Dimension, G](v: NonNegative[GridQuantity[D, G]])

  @targetName("addNonNegativeGrid")
  def add(r: NonNegative[GridQuantity[D, G]]): NonNegative[GridQuantity[D, G]] =
    NonNegative.unrefined(v) + NonNegative.unrefined(r)

  @targetName("exactDivideNonNegativeGrid")
  def exactDivideBy(d: PositiveWhole, g: GridRef.Grid[D, G]): NonNegative[Quantity[D]] =
    g.asQuantity(NonNegative.unrefined(v)).exactDivideBy(Positive.asNonZero(d))

end extension

extension [D <: Dimension](v: NonNegative[Quantity[D]])

  @targetName("addNonNegativeQuantity")
  def add(r: NonNegative[Quantity[D]]): NonNegative[Quantity[D]] =
    NonNegative.unrefined(v) + NonNegative.unrefined(r)

  @targetName("exactDivideNonNegativeQuantity")
  def exactDivideBy(d: PositiveWhole): NonNegative[Quantity[D]] =
    Quantity.exactDivideBy(NonNegative.unrefined(v))(Positive.asNonZero(d))

end extension

extension [D <: Dimension, G](v: Positive[GridQuantity[D, G]])

  @targetName("addPositiveGrid")
  def add(r: Positive[GridQuantity[D, G]]): Positive[GridQuantity[D, G]] =
    Positive.unrefined(v) + Positive.unrefined(r)

  @targetName("exactDividePositiveGrid")
  def exactDivideBy(d: PositiveWhole, g: GridRef.Grid[D, G]): Positive[Quantity[D]] =
    g.asQuantity(Positive.unrefined(v)).exactDivideBy(Positive.asNonZero(d))

end extension

extension [D <: Dimension](v: Positive[Quantity[D]])

  @targetName("addPositiveQuantity")
  def add(r: Positive[Quantity[D]]): Positive[Quantity[D]] =
    Positive.unrefined(v) + Positive.unrefined(r)

  @targetName("exactDividePositiveQuantity")
  def exactDivideBy(d: PositiveWhole): Positive[Quantity[D]] =
    Quantity.exactDivideBy(Positive.unrefined(v))(Positive.asNonZero(d))

end extension

extension [D <: Dimension, G](v: NonZero[GridQuantity[D, G]])

  @targetName("addNonZeroGrid")
  def add(r: NonZero[GridQuantity[D, G]]): GridQuantity[D, G] =
    NonZero.unrefined(v) + NonZero.unrefined(r)

  @targetName("exactDivideNonZeroGrid")
  def exactDivideBy(d: NonZeroWhole, g: GridRef.Grid[D, G]): NonZero[Quantity[D]] =
    g.asQuantity(NonZero.unrefined(v)).exactDivideBy(d)

end extension

extension [D <: Dimension](v: NonZero[Quantity[D]])

  @targetName("addNonZeroQuantity")
  def add(r: NonZero[Quantity[D]]): Quantity[D] =
    NonZero.unrefined(v) + NonZero.unrefined(r)

  @targetName("exactDivideNonZeroQuantity")
  def exactDivideBy(d: NonZeroWhole): NonZero[Quantity[D]] =
    Quantity.exactDivideBy(NonZero.unrefined(v))(d)

end extension
