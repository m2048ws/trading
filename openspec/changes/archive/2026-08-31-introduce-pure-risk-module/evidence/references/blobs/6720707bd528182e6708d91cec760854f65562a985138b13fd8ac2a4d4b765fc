package trading.risk

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import trading.economics.instrument.InstrumentFixtures
import trading.quantity.*
import trading.quantity.refinement.*

class RiskCurvePropertiesSuite extends ScalaCheckSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear

  private val capValue = Gen.choose(1, 40)
  private val signed   = Gen.choose(-50, 50)
  private val marginal = Gen.choose(0, 20)

  property("every affine model is exact, total, identity preserving, and nondecreasing"):
    forAll(capValue, signed, marginal): (rawCap, first, additional) =>
      val model = MonotoneLotRisk.affine(instrument)(
        PositiveWhole(rawCap).toOption.get,
        quantity(first),
        NonNegative(quantity(additional)).toOption.get
      )
      val assessments = 1.to(rawCap).toVector.map(count => ModelTestAccess.observe(model, BigInt(count)))
      val exact       = assessments.zipWithIndex.forall: (assessment, index) =>
        val loss     = Rational(first) + Rational(BigInt(index)) * Rational(additional)
        val expected = if loss.signum <= 0 then Rational.zero else loss
        assessment.lots.count.unrefined == BigInt(index + 1) &&
        assessment.lots.instrumentId == instrument.identity.id &&
        assessment.downsideRisk.unrefined.coefficient == expected
      exact && nondecreasing(assessments.map(_.downsideRisk.unrefined.coefficient))

  property("addition, minimum, maximum, and order-preserving quantization are monotone closures"):
    forAll(capValue, signed, marginal, signed, marginal):
      (rawCap, firstLeft, marginalLeft, firstRight, marginalRight) =>
        val checkedCap = PositiveWhole(rawCap).toOption.get
        val left       = MonotoneLotRisk.affine(instrument)(
          checkedCap,
          quantity(firstLeft),
          NonNegative(quantity(marginalLeft)).toOption.get
        )
        val right = MonotoneLotRisk.affine(instrument)(
          checkedCap,
          quantity(firstRight),
          NonNegative(quantity(marginalRight)).toOption.get
        )
        val grid = UniformGrid.create(
          instrument.roles.settle.dimension.ref,
          PositiveRational(Rational(3, 2)).toOption.get
        )
        val models = Vector(
          MonotoneLotRisk.add(left, right).toOption.get,
          MonotoneLotRisk.minimum(left, right).toOption.get,
          MonotoneLotRisk.maximum(left, right).toOption.get,
          MonotoneLotRisk.quantized(left)(grid, OrderPreservingQuantization.Floor).toOption.get,
          MonotoneLotRisk.quantized(left)(grid, OrderPreservingQuantization.Ceiling).toOption.get
        )
        models.forall: model =>
          val risks = 1.to(rawCap).toVector.map(count =>
            ModelTestAccess.observe(model, BigInt(count)).downsideRisk.unrefined.coefficient
          )
          nondecreasing(risks)

  property("valid contiguous piecewise curves remain total and monotone"):
    forAll(Gen.choose(2, 40), signed, marginal, marginal): (rawCap, first, firstSlope, secondSlope) =>
      val boundary     = rawCap / 2
      val firstEndLoss = Rational(first) + Rational(boundary - 1) * Rational(firstSlope)
      val secondStart  = firstEndLoss + Rational(secondSlope)
      val segments     = Vector(
        LossSegment(BigInt(1), BigInt(boundary), quantity(first), quantity(firstSlope)),
        LossSegment(BigInt(boundary + 1), BigInt(rawCap),
          Quantity(instrument.roles.settle.dimension.ref, secondStart), quantity(secondSlope))
      )
      val model = MonotoneLotRisk
        .piecewise(instrument)(PositiveWhole(rawCap).toOption.get, segments)
        .toOption
        .get
      val risks = 1.to(rawCap).toVector.map(count =>
        ModelTestAccess.observe(model, BigInt(count)).downsideRisk.unrefined.coefficient
      )
      nondecreasing(risks)

  private def quantity(value: Int): Quantity[instrument.roles.settle.D] =
    Quantity(instrument.roles.settle.dimension.ref, value)

  private def nondecreasing(values: Vector[Rational]): Boolean =
    values.zip(values.drop(1)).forall((left, right) => left.compare(right) <= 0)
end RiskCurvePropertiesSuite
