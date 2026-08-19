package trading.quantity.laws

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

import trading.quantity.*
import trading.quantity.testkit.ExactGenerators

/**
 * Canonical representation and construction-agreement laws for exact rationals.
 *
 * Expected values come from public numerator/denominator construction and independently rendered integer, fraction, and
 * finite-decimal strings.
 */
final class RationalCanonicalityLaws(using Arbitrary[Rational]) extends Laws:

  private def decimalText(u: BigInt, s: Int): String =
    if s == 0 then
      u.toString
    else
      val sign =
        if u < 0 then
          "-"
        else
          ""

      val magnitude = u.abs.toString.reverse.padTo(s + 1, '0').reverse
      val split     = magnitude.length - s
      s"$sign${magnitude.take(split)}.${magnitude.drop(split)}"

  val canonicality: RuleSet = new SimpleRuleSet(
    "rational canonicality",
    "canonical denominator and gcd" -> forAll: (value: Rational) =>
      Prop(
        value.denominator > 0 &&
          value.numerator.abs.gcd(value.denominator) == 1 &&
          (!value.isZero || value.denominator == 1)
      ),
    "double negation" -> forAll: (value: Rational) =>
      Prop(-(-value) == value),
    "normalized input equality and hash" -> forAll(
      summon[Arbitrary[Rational]].arbitrary,
      ExactGenerators.positiveBigInt
    ): (value, factor) =>
      val equivalent = Rational(value.numerator * factor, value.denominator * factor)
      Prop(equivalent == value && equivalent.hashCode == value.hashCode)
    ,
    "parse render round trip" -> forAll: (value: Rational) =>
      Prop(Rational.parse(value.toString) == Right(value)),
    "integer and fraction construction agree" -> forAll(ExactGenerators.bigInt): value =>
      val integer  = Rational(value)
      val fraction = Rational.parse(s"$value/1")
      Prop(fraction == Right(integer) && Rational.parse(value.toString) == Right(integer))
    ,
    "decimal and fraction construction agree" -> forAll(
      ExactGenerators.bigInt,
      Gen.chooseNum(0, 18)
    ): (unscaled, scale) =>
      val denominator = BigInt(10).pow(scale)
      val expected    = Rational(unscaled, denominator)
      val decimal     = Rational.parse(decimalText(unscaled, scale))
      val fraction    = Rational.parse(s"$unscaled/$denominator")
      Prop(decimal == Right(expected) && fraction == Right(expected))
  )

end RationalCanonicalityLaws

/** Dimension normalization laws checked against an independent immutable-map fold rather than production grouping. */
final class DimensionNormalizationLaws extends Laws:

  private def reference(xs: List[(AtomId, BigInt)]): Vector[(AtomId, BigInt)] =
    xs
      .foldLeft(Map.empty[AtomId, BigInt]): (accumulator, entry) =>
        val (atom, contribution) = entry
        accumulator.updated(atom, accumulator.getOrElse(atom, BigInt(0)) + contribution)
      .iterator
      .filter(_._2 != 0)
      .toVector
      .sortBy(_._1.value)

  val normalization: RuleSet = new SimpleRuleSet(
    "dimension normalization",
    "independent normalized powers" -> forAll(ExactGenerators.dimensionPowers): raw =>
      Prop(DimensionKey(raw).powers == reference(raw)),
    "zero exponents are removed" -> forAll(ExactGenerators.dimensionPowers): raw =>
      Prop(DimensionKey(raw).powers.forall(_._2 != 0)),
    "duplicate atoms are merged" -> forAll(ExactGenerators.dimensionPowers): raw =>
      val actual   = DimensionKey(raw).powers.toMap
      val expected = raw
        .map(_._1)
        .distinct
        .forall: atom =>
          val total = raw.iterator.collect { case (`atom`, exponent) => exponent }.sum

          if total == 0 then
            !actual.contains(atom)
          else
            actual.get(atom).contains(total)
      Prop(expected)
    ,
    "canonical powers are sorted" -> forAll(ExactGenerators.dimensionPowers): raw =>
      val names = DimensionKey(raw).powers.map(_._1.value)
      Prop(names == names.sorted)
    ,
    "equal normalized keys have equal hashes" -> forAll(ExactGenerators.dimensionPowers): raw =>
      val reversed = DimensionKey(raw.reverse)
      val original = DimensionKey(raw)
      Prop(original == reversed && original.hashCode == reversed.hashCode)
    ,
    "division is multiplication by inverse" -> forAll(
      ExactGenerators.dimensionKey,
      ExactGenerators.dimensionKey
    ): (numerator, denominator) =>
      val numeratorRef   = DimRef.fresh(numerator)
      val denominatorRef = DimRef.fresh(denominator)
      val divided        = DimRef.divide(numeratorRef.dimension, denominatorRef.dimension).key
      val multiplied     = DimensionKey.multiply(numerator, DimensionKey.inverse(denominator))
      Prop(divided == multiplied)
    ,
    "BigInt exponent boundaries remain exact" -> {
      val atom       = AtomId("dimension-boundary-law")
      val boundaries = Vector(
        BigInt(Int.MaxValue) + 1,
        BigInt(Int.MinValue) - 1,
        BigInt(10).pow(120),
        -BigInt(10).pow(120)
      )
      Prop:
        boundaries.forall: exponent =>
          val key      = DimensionKey(List(atom -> exponent, atom -> BigInt(1)))
          val expected = exponent + 1

          if expected == 0 then
            key.powers.isEmpty
          else
            key.powers == Vector(atom -> expected)
    }
  )

end DimensionNormalizationLaws

/**
 * Laws for multiplication in the dimension-graded family `D => Quantity[D]`.
 *
 * Expected coefficients use standalone rational equations, while expected dimensions use canonical public keys.
 */
final class GradedQuantityLaws[A <: Dimension, B <: Dimension, C <: Dimension](
  aDimension: DimRef[A],
  bDimension: DimRef[B],
  cDimension: DimRef[C]
)(using
  arbitrary: Arbitrary[Rational],
  aValid: Normalize[A],
  bValid: Normalize[B],
  ab: Normalize[Times[A, B]],
  ba: Normalize[Times[B, A]],
  bc: Normalize[Times[B, C]],
  ac: Normalize[Times[A, C]],
  abOutValid: Normalize[ab.Out],
  acOutValid: Normalize[ac.Out],
  abThenC: Normalize[Times[ab.Out, C]],
  aThenBc: Normalize[Times[A, bc.Out]])
  extends Laws:

  private def quantity[D <: Dimension](d: DimRef[D], c: Rational): Quantity[D] =
    Quantity(d, c)

  val gradedQuantity: RuleSet = new SimpleRuleSet(
    "graded quantity",
    "associativity with canonical dimension equivalence" -> forAll: (a: Rational, b: Rational, c: Rational) =>
      val leftPair       = quantity(aDimension, a).*[B](quantity(bDimension, b))(using ab)
      val rightPair      = quantity(bDimension, b).*[C](quantity(cDimension, c))(using bc)
      val left           = leftPair.*[C](quantity(cDimension, c))(using abThenC)
      val right          = quantity(aDimension, a).*[bc.Out](rightPair)(using aThenBc)
      val leftPairRef    = DimRef.times(aDimension, bDimension)(using ab)
      val rightPairRef   = DimRef.times(bDimension, cDimension)(using bc)
      val leftDimension  = DimRef.times(leftPairRef, cDimension)(using abThenC)
      val rightDimension = DimRef.times(aDimension, rightPairRef)(using aThenBc)
      val canonical      = SameDimension.between(leftDimension, rightDimension).nonEmpty
      Prop(canonical && left.coefficient == a * b * c && right.coefficient == a * (b * c))
    ,
    "commutativity with canonical dimension equivalence" -> forAll: (a: Rational, b: Rational) =>
      val left           = quantity(aDimension, a).*[B](quantity(bDimension, b))(using ab)
      val right          = quantity(bDimension, b).*[A](quantity(aDimension, a))(using ba)
      val leftDimension  = DimRef.times(aDimension, bDimension)(using ab)
      val rightDimension = DimRef.times(bDimension, aDimension)(using ba)
      val canonical      = SameDimension.between(leftDimension, rightDimension).nonEmpty
      Prop(canonical && left.coefficient == a * b && right.coefficient == b * a)
    ,
    "left distributivity" -> forAll: (a: Rational, b: Rational, c: Rational) =>
      val left  = (quantity(aDimension, a) + quantity(aDimension, b)).*[C](quantity(cDimension, c))(using ac)
      val right = quantity(aDimension, a).*[C](quantity(cDimension, c))(using ac) +
        quantity(aDimension, b).*[C](quantity(cDimension, c))(using ac)
      Prop(left.coefficient == (a + b) * c && right.coefficient == a * c + b * c)
    ,
    "right distributivity" -> forAll: (a: Rational, b: Rational, c: Rational) =>
      val left  = quantity(aDimension, a).*[B](quantity(bDimension, b) + quantity(bDimension, c))(using ab)
      val right = quantity(aDimension, a).*[B](quantity(bDimension, b))(using ab) +
        quantity(aDimension, a).*[B](quantity(bDimension, c))(using ab)
      Prop(left.coefficient == a * (b + c) && right.coefficient == a * b + a * c)
    ,
    "scalar compatibility" -> forAll: (scalar: Rational, a: Rational, b: Rational) =>
      val left  = (quantity(aDimension, a) * scalar).*[B](quantity(bDimension, b))(using ab)
      val right = quantity(aDimension, a).*[B](quantity(bDimension, b))(using ab) * scalar
      Prop(left.coefficient == a * scalar * b && right.coefficient == scalar * (a * b))
  )

end GradedQuantityLaws

/**
 * Category-shaped rate laws over authoritative witnesses, without publishing a Cats Category instance.
 *
 * Expected orientation and composition use independent rational coefficient products and witness-built identities.
 */
final class RateLaws[A <: Dimension, B <: Dimension, C <: Dimension, D <: Dimension](
  aDimension: DimRef[A],
  bDimension: DimRef[B],
  cDimension: DimRef[C],
  dDimension: DimRef[D]
)(using
  arbitrary: Arbitrary[Rational],
  quotientAB: Normalize[Divide[B, A]],
  quotientBC: Normalize[Divide[C, B]],
  quotientCD: Normalize[Divide[D, C]],
  quotientAA: Normalize[Divide[A, A]],
  quotientBB: Normalize[Divide[B, B]],
  composeABBC: Normalize[Times[Divide[B, A], Divide[C, B]]],
  composeBCCD: Normalize[Times[Divide[C, B], Divide[D, C]]],
  composeACCD: Normalize[Times[Divide[C, A], Divide[D, C]]],
  composeABBD: Normalize[Times[Divide[B, A], Divide[D, B]]],
  composeAAAB: Normalize[Times[Divide[A, A], Divide[B, A]]],
  composeABBB: Normalize[Times[Divide[B, A], Divide[B, B]]])
  extends Laws:

  private def rate[F <: Dimension, T <: Dimension](
    f: DimRef[F],
    t: DimRef[T],
    c: Rational
  )(using
    operation: Normalize[Divide[T, F]]
  ): Rate[F, T] =
    Rate(f, t, c)(using operation)

  val categoryShape: RuleSet = new SimpleRuleSet(
    "rate category laws",
    "associativity" -> forAll: (f: Rational, g: Rational, h: Rational) =>
      val first  = rate(aDimension, bDimension, f)(using quotientAB)
      val second = rate(bDimension, cDimension, g)(using quotientBC)
      val third  = rate(cDimension, dDimension, h)(using quotientCD)
      val ac     = first.andThen(second)(using composeABBC)
      val bd     = second.andThen(third)(using composeBCCD)
      val left   = ac.andThen(third)(using composeACCD)
      val right  = first.andThen(bd)(using composeABBD)
      Prop(left.coefficient == right.coefficient && left.coefficient == f * g * h)
    ,
    "left identity" -> forAll: (coefficient: Rational) =>
      val value    = rate(aDimension, bDimension, coefficient)(using quotientAB)
      val identity = Rate.identity(aDimension)(using quotientAA)
      Prop(identity.andThen(value)(using composeAAAB).coefficient == coefficient)
    ,
    "right identity" -> forAll: (coefficient: Rational) =>
      val value    = rate(aDimension, bDimension, coefficient)(using quotientAB)
      val identity = Rate.identity(bDimension)(using quotientBB)
      Prop(value.andThen(identity)(using composeABBB).coefficient == coefficient)
    ,
    "coefficient orientation" -> forAll: (f: Rational, g: Rational) =>
      val first    = rate(aDimension, bDimension, f)(using quotientAB)
      val second   = rate(bDimension, cDimension, g)(using quotientBC)
      val composed = first.andThen(second)(using composeABBC)
      Prop(composed.coefficient == f * g)
  )

end RateLaws
