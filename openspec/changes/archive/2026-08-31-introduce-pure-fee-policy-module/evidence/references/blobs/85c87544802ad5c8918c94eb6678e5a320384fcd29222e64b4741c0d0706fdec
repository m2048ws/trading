package trading.fee

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.*
import trading.fee.policy.FeePolicy
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.*
import trading.support.DownstreamFixtures

final class FeeCalculationSuite extends ScalaCheckSuite:
  private val fixture    = new DownstreamFixtures
  private val instrument = fixture.linear

  private def quantity(coefficient: Rational): Quantity[fixture.usd.D] =
    Quantity(fixture.usd.dimension.ref, coefficient)

  private def nonnegative(coefficient: Rational): NonNegative[Quantity[fixture.usd.D]] =
    NonNegative(quantity(coefficient)).toOption.get

  test("percentage preserves the asset dimension and quoted charge/rebate sign convention"):
    val basis                           = nonnegative(Rational(10))
    val charge: Quantity[fixture.usd.D] =
      FeeCalculation.percentage(basis, FeeRate(Rational(1, 1000)))
    val rebate: Quantity[fixture.usd.D] =
      FeeCalculation.percentage(basis, FeeRate(Rational(-1, 1000)))
    val zero: Quantity[fixture.usd.D] =
      FeeCalculation.percentage(basis, FeeRate(Rational.zero))

    assertEquals(charge.coefficient, Rational(-1, 100))
    assertEquals(rebate.coefficient, Rational(1, 100))
    assertEquals(zero.coefficient, Rational.zero)
    assertEquals(FeeRate(Rational(1, 1000)), FeeRate(Rational(1, 1000)))

  test("a negative basis fails at refinement before percentage calculation"):
    assertEquals(NonNegative(quantity(Rational(-1, 3))), Left(ExpectedNonNegative))

  test("minimum charge changes only charges below the refined boundary"):
    val minimum = nonnegative(Rational(1, 100))
    val smaller = quantity(Rational(-1, 1000))
    val equal   = quantity(Rational(-1, 100))
    val larger  = quantity(Rational(-1, 50))
    val rebate  = quantity(Rational(1, 1000))
    val zero    = quantity(Rational.zero)

    assertEquals(FeeCalculation.minimumCharge(smaller, minimum).coefficient, Rational(-1, 100))
    assertEquals(FeeCalculation.minimumCharge(equal, minimum), equal)
    assertEquals(FeeCalculation.minimumCharge(larger, minimum), larger)
    assertEquals(FeeCalculation.minimumCharge(rebate, minimum), rebate)
    assertEquals(FeeCalculation.minimumCharge(zero, minimum), zero)
    assertEquals(FeeCalculation.minimumCharge(smaller, nonnegative(Rational.zero)), smaller)

  property("percentage is exact typed scaling for every refined nonnegative basis and signed rate"):
    forAll { (basisRaw: Int, rateRaw: Int) =>
      val basisCoefficient                = Rational(BigInt(basisRaw).abs, 997)
      val rateCoefficient                 = Rational(rateRaw, 991)
      val basis                           = nonnegative(basisCoefficient)
      val result: Quantity[fixture.usd.D] = FeeCalculation.percentage(basis, FeeRate(rateCoefficient))

      result.coefficient == basisCoefficient * -rateCoefficient
    }

  property("minimum adjustment is total and preserves every contribution outside the smaller-charge case"):
    forAll { (contributionRaw: Int, minimumRaw: Int) =>
      val contributionCoefficient = Rational(contributionRaw, 997)
      val minimumCoefficient      = Rational(BigInt(minimumRaw).abs, 991)
      val contribution            = quantity(contributionCoefficient)
      val minimum                 = nonnegative(minimumCoefficient)
      val expected                =
        if contributionCoefficient.signum < 0 &&
          contributionCoefficient.abs.compare(minimumCoefficient) < 0
        then -minimumCoefficient
        else contributionCoefficient

      FeeCalculation.minimumCharge(contribution, minimum).coefficient == expected
    }

  test("policy sends each exact component through core denomination quantization separately"):
    val policy       = FeePolicy(instrument)
    val denomination = policy
      .denomination(fixture.usd)(fixture.usdCents, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val basis = nonnegative(Rational(6))
    val rate  = FeeRate(Rational(1, 1000))
    val first = policy
      .percentage(denomination, FeeKind.from("first").toOption.get, basis, rate)
      .toOption
      .get
    val second = policy
      .percentage(denomination, FeeKind.from("second").toOption.get, basis, rate)
      .toOption
      .get

    assertEquals(first.unrounded.coefficient, Rational(-3, 500))
    assertEquals(second.unrounded.coefficient, Rational(-3, 500))
    assertEquals(first.amount.coefficient, Rational.zero)
    assertEquals(second.amount.coefficient, Rational.zero)
    assertEquals(first.residual, first.unrounded)
    assertEquals(second.residual, second.unrounded)
    assertEquals(first.amount + first.residual, first.unrounded)
    assertEquals(second.amount + second.residual, second.unrounded)

    val incorrectlyAggregated = Fee
      .create(instrument)(
        denomination,
        FeeKind.from("aggregated-control").toOption.get,
        first.unrounded + second.unrounded
      )
      .toOption
      .get
    assertEquals(incorrectlyAggregated.amount.coefficient, Rational(-1, 100))
    assertEquals(first.amount + second.amount, Quantity.zero(using fixture.usd.dimension.ref))
end FeeCalculationSuite
