package trading.quantity.refinement

import trading.quantity.Rational
import trading.quantity.ZeroRationalDenominator

/** Positive arbitrary-precision whole number used for exact counts and positive divisors. */
type PositiveWhole = Positive[BigInt]

/** Validated `BigInt` and `Int` constructors for [[PositiveWhole]]. */
object PositiveWhole:

  def apply(v: BigInt): Either[ExpectedPositive.type, PositiveWhole] =
    Positive(v)

  def apply(v: Int): Either[ExpectedPositive.type, PositiveWhole] =
    apply(BigInt(v))

/** Nonzero arbitrary-precision whole number used where exact division cannot accept zero. */
type NonZeroWhole = NonZero[BigInt]

/** Validated `BigInt` and `Int` constructors for [[NonZeroWhole]]. */
object NonZeroWhole:

  def apply(v: BigInt): Either[ExpectedNonZero.type, NonZeroWhole] =
    NonZero(v)

  def apply(v: Int): Either[ExpectedNonZero.type, NonZeroWhole] =
    apply(BigInt(v))

/** Positive bounded integer used for collection sizes and allocation counts. */
type PositiveInt = Positive[Int]

/** Validated constructor for [[PositiveInt]]. */
object PositiveInt:

  def apply(v: Int): Either[ExpectedPositive.type, PositiveInt] =
    Positive(v)

/** Positive exact rational used for grid quanta and other strictly positive coefficients. */
type PositiveRational = Positive[Rational]

/** Validated exact and decimal constructors for [[PositiveRational]]. */
object PositiveRational:

  def apply(v: Rational): Either[ExpectedPositive.type, PositiveRational] =
    Positive(v)

  def exact(n: BigInt, d: BigInt): Either[ExpectedPositive.type | ZeroRationalDenominator.type, PositiveRational] =
    if d == 0 then
      Left(ZeroRationalDenominator)
    else
      apply(Rational(n, d))

  def decimal(raw: String): Either[String, PositiveRational] =
    Rational
      .parse(raw)
      .flatMap(parsed => apply(parsed).left.map(_ => s"quantum must be positive: $raw"))

end PositiveRational
