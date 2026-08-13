package trading.quantity

import java.math.BigDecimal as JBigDecimal

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators

class RationalInvariantSuite extends ScalaCheckSuite:

  private def assertCanonical(v: Rational): Unit =
    assert(v.denominator > 0)
    assertEquals(v.numerator.gcd(v.denominator), BigInt(1))
    if v.isZero then
      assertEquals(v.numerator, BigInt(0))
      assertEquals(v.denominator, BigInt(1))

  test("construction canonicalizes zero, signs, and reducible fractions"):
    assertEquals(Rational(0, -999), Rational.zero)
    assertEquals(Rational(6, -8), Rational(-3, 4))
    assertEquals(Rational(-6, -8), Rational(3, 4))
    List(Rational.zero, Rational(6, -8), Rational(-6, -8)).foreach(assertCanonical)

  property("all rational operations preserve canonical form and equality/hash agreement"):
    forAll(ExactGenerators.rational, ExactGenerators.nonZeroRational): (left, right) =>
      val operations =
        List(
          left + right,
          left - right,
          left * right,
          -left,
          left / right match
            case Right(value) => value
            case Left(error)  => fail(s"nonzero generator produced $error")
        )
      operations.foreach(assertCanonical)

      val factor     = BigInt(7)
      val equivalent = Rational(left.numerator * factor, left.denominator * factor)
      assertEquals(equivalent, left)
      assertEquals(equivalent.hashCode, left.hashCode)
      assertEquals(left.compare(right), -right.compare(left))
      assertEquals(left.compare(right) == 0, left == right)

  test("division rejects zero explicitly"):
    assertEquals(Rational(5, 7) / Rational.zero, Left(DivisionByZero))

  test("strict parsing rejects malformed sign, fraction, and decimal forms"):
    val malformed = List("--1.2", "+-1.2", "-+1.2", "++1.2", "1.-2", "1/--2", "1//2", "/", ".", "+.")
    malformed.foreach: raw =>
      assert(Rational.parse(raw).isLeft, clues(raw, Rational.parse(raw)))
      assert(PositiveRational.decimal(raw).isLeft, clues(raw, PositiveRational.decimal(raw)))

    assertEquals(Rational.parse("+6/8"), Right(Rational(3, 4)))
    assertEquals(Rational.parse("-0.00"), Right(Rational.zero))
    assert(Rational.parse("1/0").isLeft)
    assertEquals(PositiveRational.decimal("  +1.20  "), Right(PositiveRational.exact(6, 5).toOption.get))

  test("surrounding whitespace is accepted but internal whitespace is rejected"):
    assertEquals(Rational.parse("  -1.20  "), Right(Rational(-6, 5)))
    List("1 2", "1 /2", "1/ 2", "1 .2", "1. 2").foreach: raw =>
      assert(Rational.parse(raw).isLeft, clues(raw))

  test("extreme negative finite-decimal scale never escapes as an incidental exception"):
    val extreme = new JBigDecimal(java.math.BigInteger.ONE, Int.MinValue)
    assertEquals(
      Quantity(DimRef.one, extreme),
      Left(UnsupportedFiniteDecimalScale(Int.MinValue, Quantity.MaximumFiniteDecimalScaleMagnitude))
    )

end RationalInvariantSuite
