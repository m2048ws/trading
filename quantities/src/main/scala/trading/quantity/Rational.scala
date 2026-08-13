package trading.quantity

import trading.quantity.refinement.*

/**
 * The canonical exact scalar used throughout the library.
 *
 * Every value is normalized to a positive denominator and relatively prime numerator and denominator. No floating-point
 * conversion is exposed, so supported arithmetic remains exact.
 */
final class Rational private (val numerator: BigInt, val denominator: BigInt):
  require(denominator > 0, "rational denominator must be positive")
  require(numerator.gcd(denominator) == 1, "rational numerator and denominator must be normalized")

  def +(r: Rational): Rational =
    Rational(numerator * r.denominator + r.numerator * denominator, denominator * r.denominator)

  def -(r: Rational): Rational =
    Rational(numerator * r.denominator - r.numerator * denominator, denominator * r.denominator)

  def *(r: Rational): Rational =
    Rational(numerator * r.numerator, denominator * r.denominator)

  def /(r: Rational): Either[DivisionByZero.type, Rational] =
    if r.numerator == 0 then
      Left(DivisionByZero)
    else
      Right(Rational(numerator * r.denominator, denominator * r.numerator))

  def divideBy(d: NonZero[Rational]): Rational =
    Rational(numerator * d.unrefined.denominator, denominator * d.unrefined.numerator)

  def unary_- : Rational =
    Rational(-numerator, denominator)

  def abs: Rational =
    if numerator < 0 then
      -this
    else
      this

  def signum: Int =
    numerator.signum

  def isZero: Boolean =
    numerator == 0

  def isWhole: Boolean =
    numerator % denominator == 0

  def floor: BigInt =
    val (q, r) = numerator /% denominator

    if r < 0 then
      q - 1
    else
      q

  def ceil: BigInt =
    val (q, r) = numerator /% denominator

    if r > 0 then
      q + 1
    else
      q

  /** Round to the nearest integer, resolving ties toward the even integer. */
  def roundHalfEven: BigInt =
    val lower     = floor
    val remainder = this - Rational(lower)
    val twice     = remainder.numerator * 2

    if twice < denominator then
      lower
    else if twice > denominator then
      lower + 1
    else if lower % 2 == 0 then
      lower
    else
      lower + 1

  def compare(r: Rational): Int = (numerator * r.denominator).compare(r.numerator * denominator)

  override def equals(o: Any): Boolean =
    o match
      case r: Rational => numerator == r.numerator && denominator == r.denominator
      case _           => false

  override def hashCode: Int =
    31 * numerator.hashCode + denominator.hashCode

  override def toString: String =
    if denominator == 1 then
      numerator.toString
    else
      s"$numerator/$denominator"

end Rational

/** Normalized constructors, constants, and exact text parsing for [[Rational]]. */
object Rational:
  private val IntegerPattern  = raw"([+-]?)([0-9]+)".r
  private val DecimalPattern  = raw"([+-]?)([0-9]+)\.([0-9]+)".r
  private val FractionPattern = raw"([+-]?)([0-9]+)/([0-9]+)".r

  val zero: Rational = Rational(0, 1)
  val one: Rational  = Rational(1, 1)

  def apply(v: BigInt): Rational =
    new Rational(v, 1)

  def apply(n: BigInt, d: BigInt): Rational =
    if d == 0 then
      throw new IllegalArgumentException("rational denominator cannot be zero")

    val sign =
      if d < 0 then
        -1
      else
        1

    val numerator   = n * sign
    val denominator = d.abs
    val divisor     = numerator.gcd(denominator)

    new Rational(numerator / divisor, denominator / divisor)

  def parse(raw: String): Either[String, Rational] =
    val text = raw.trim

    def signed(sign: String, magnitude: BigInt): BigInt =
      if sign == "-" then
        -magnitude
      else
        magnitude

    text match
      case ""                                            => Left("empty rational")
      case FractionPattern(sign, numerator, denominator) =>
        val d = BigInt(denominator)

        if d == 0 then
          Left(s"invalid rational: $raw")
        else
          Right(Rational(signed(sign, BigInt(numerator)), d))
      case DecimalPattern(sign, whole, fraction) =>
        val scale     = BigInt(10).pow(fraction.length)
        val magnitude = BigInt(whole) * scale + BigInt(fraction)
        Right(Rational(signed(sign, magnitude), scale))
      case IntegerPattern(sign, digits) => Right(Rational(signed(sign, BigInt(digits))))
      case _                            => Left(s"invalid rational: $raw")

  end parse

end Rational
