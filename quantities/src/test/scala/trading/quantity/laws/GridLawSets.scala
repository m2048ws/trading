package trading.quantity.laws

import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.testkit.ExactGenerators

/**
 * Coordinate-module and canonical embedding laws for one stable grid witness.
 *
 * Expected results use independent `BigInt` coordinate equations and `coordinate × quantum` rational equations.
 */
final class GridEmbeddingLaws[D <: Dimension, G](grid: GridRef.Grid[D, G]) extends Laws:

  val embedding: RuleSet = new SimpleRuleSet(
    "grid module and embedding",
    "coordinate construction round trip" -> forAll(ExactGenerators.coordinate): coordinate =>
      Prop(grid.coordinate(grid.fromCoordinate(coordinate)) == coordinate),
    "grid construction round trip" -> forAll(ExactGenerators.coordinate): coordinate =>
      val value = grid.fromCoordinate(coordinate)
      Prop(grid.fromCoordinate(grid.coordinate(value)).sameGridEquals(value))
    ,
    "coordinate addition" -> forAll(
      ExactGenerators.coordinate,
      ExactGenerators.coordinate
    ): (left, right) =>
      Prop(grid.coordinate(grid.fromCoordinate(left) + grid.fromCoordinate(right)) == left + right),
    "coordinate subtraction" -> forAll(
      ExactGenerators.coordinate,
      ExactGenerators.coordinate
    ): (left, right) =>
      Prop(grid.coordinate(grid.fromCoordinate(left) - grid.fromCoordinate(right)) == left - right),
    "coordinate scaling" -> forAll(
      ExactGenerators.coordinate,
      ExactGenerators.bigInt
    ): (coordinate, scalar) =>
      Prop(grid.coordinate(grid.fromCoordinate(coordinate) * scalar) == coordinate * scalar),
    "embedding coefficient" -> forAll(ExactGenerators.coordinate): coordinate =>
      val embedded = grid.asQuantity(grid.fromCoordinate(coordinate))
      Prop(embedded.coefficient == Rational(coordinate) * grid.quantum.unrefined)
    ,
    "embedding zero" -> Prop(
      grid.asQuantity(grid.fromCoordinate(BigInt(0))).coefficient == Rational.zero
    ),
    "embedding addition" -> forAll(
      ExactGenerators.coordinate,
      ExactGenerators.coordinate
    ): (left, right) =>
      val leftValue  = grid.fromCoordinate(left)
      val rightValue = grid.fromCoordinate(right)
      Prop(
        grid.asQuantity(leftValue + rightValue).coefficient ==
          grid.asQuantity(leftValue).coefficient + grid.asQuantity(rightValue).coefficient
      )
    ,
    "embedding negation" -> forAll(ExactGenerators.coordinate): coordinate =>
      val value = grid.fromCoordinate(coordinate)
      Prop(grid.asQuantity(-value).coefficient == -grid.asQuantity(value).coefficient)
    ,
    "embedding scaling" -> forAll(
      ExactGenerators.coordinate,
      ExactGenerators.bigInt
    ): (coordinate, scalar) =>
      val value = grid.fromCoordinate(coordinate)
      Prop(
        grid.asQuantity(value * scalar).coefficient ==
          grid.asQuantity(value).coefficient * Rational(scalar)
      )
    ,
    "embedding injective" -> forAll(
      ExactGenerators.coordinate,
      ExactGenerators.coordinate
    ): (left, right) =>
      val equalEmbeddings =
        grid.asQuantity(grid.fromCoordinate(left)).coefficient ==
          grid.asQuantity(grid.fromCoordinate(right)).coefficient
      Prop(!equalEmbeddings || left == right)
  )

end GridEmbeddingLaws

/**
 * Partial-isomorphism laws for exact embedding and exact narrowing.
 *
 * Expected success and coordinates use independent numerator/denominator divisibility arithmetic.
 */
final class ExactNarrowingLaws[D <: Dimension, G](grid: GridRef.Grid[D, G])(using Arbitrary[Rational]) extends Laws:

  private def independentCoordinate(c: Rational): Option[BigInt] =
    val quantum           = grid.quantum.unrefined
    val scaledNumerator   = c.numerator * quantum.denominator
    val scaledDenominator = c.denominator * quantum.numerator
    Option.when(scaledNumerator % scaledDenominator == 0)(scaledNumerator / scaledDenominator)

  val partialIsomorphism: RuleSet = new SimpleRuleSet(
    "exact narrowing partial isomorphism",
    "narrow after embed" -> forAll(ExactGenerators.coordinate): coordinate =>
      val value = grid.fromCoordinate(coordinate)
      grid.asQuantity(value).narrowExactlyTo(grid) match
        case Right(narrowed) => Prop(grid.coordinate(narrowed) == coordinate)
        case Left(_)         => Prop.falsified,
    "successful narrowing embeds to source" -> forAll: (coefficient: Rational) =>
      val source = Quantity(grid.dimension, coefficient)
      source.narrowExactlyTo(grid) match
        case Right(value) => Prop(grid.asQuantity(value).coefficient == coefficient)
        case Left(_)      => Prop.proved,
    "integrality model agrees" -> forAll: (coefficient: Rational) =>
      val source   = Quantity(grid.dimension, coefficient)
      val expected = independentCoordinate(coefficient)
      val actual   = source.narrowExactlyTo(grid)
      (expected, actual) match
        case (Some(coordinate), Right(value)) => Prop(grid.coordinate(value) == coordinate)
        case (None, Left(_))                  => Prop.proved
        case _                                => Prop.falsified
  )

end ExactNarrowingLaws

/**
 * Retraction, order, residual, and nearest-neighbor laws for exact quantization.
 *
 * Expected directed coordinates use integer quotient/remainder models; nearest results use independently selected
 * coordinate neighbors and an explicit tie table. No expected coordinate calls quantization.
 */
final class QuantizationLaws[D <: Dimension, G](grid: GridRef.Grid[D, G])(using Arbitrary[Rational]) extends Laws:
  import QuantizationPolicy.*

  private def quantity(c: Rational): Quantity[D] =
    Quantity(grid.dimension, c)

  private def floorModel(v: Rational): BigInt =
    val (quotient, remainder) = v.numerator /% v.denominator

    if remainder < 0 then
      quotient - 1
    else
      quotient

  private def ceilModel(v: Rational): BigInt =
    val (quotient, remainder) = v.numerator /% v.denominator

    if remainder > 0 then
      quotient + 1
    else
      quotient

  private def exactCoordinate(c: Rational): Rational =
    val quantum = grid.quantum.unrefined
    Rational(
      c.numerator * quantum.denominator,
      c.denominator * quantum.numerator
    )

  private def nearestTieExpected(p: NearestPolicy, l: BigInt): BigInt =
    p match
      case HalfEven =>
        if l % 2 == 0 then
          l
        else
          l + 1
      case HalfOdd =>
        if l % 2 != 0 then
          l
        else
          l + 1
      case HalfUp =>
        l + 1
      case HalfDown =>
        l
      case HalfTowardZero =>
        if l < 0 then
          l + 1
        else
          l
      case HalfAwayFromZero =>
        if l < 0 then
          l
        else
          l + 1

  val quantization: RuleSet = new SimpleRuleSet(
    "quantization retraction and order",
    "exact residual decomposition" -> forAll(
      ExactGenerators.rational,
      ExactGenerators.quantizationPolicy
    ): (coefficient, policy) =>
      val source   = quantity(coefficient)
      val result   = source.quantizeTo(grid, policy)
      val selected = grid.asQuantity(result.value)
      Prop(selected.coefficient + result.residual.coefficient == coefficient)
    ,
    "grid values are fixed points" -> forAll(
      ExactGenerators.coordinate,
      ExactGenerators.quantizationPolicy
    ): (coordinate, policy) =>
      val embedded = grid.asQuantity(grid.fromCoordinate(coordinate))
      val first    = embedded.quantizeTo(grid, policy)
      val second   = grid.asQuantity(first.value).quantizeTo(grid, policy)
      Prop(
        grid.coordinate(first.value) == coordinate &&
          first.residual.coefficient == Rational.zero &&
          grid.coordinate(second.value) == coordinate &&
          second.residual.coefficient == Rational.zero
      )
    ,
    "floor is below source and independently selected" -> forAll: (coefficient: Rational) =>
      val source   = quantity(coefficient)
      val selected = source.quantizeTo(grid, Floor).value
      val embedded = grid.asQuantity(selected).coefficient
      val expected = floorModel(exactCoordinate(coefficient))
      Prop(grid.coordinate(selected) == expected && embedded.compare(coefficient) <= 0)
    ,
    "ceiling is above source and independently selected" -> forAll: (coefficient: Rational) =>
      val source   = quantity(coefficient)
      val selected = source.quantizeTo(grid, Ceiling).value
      val embedded = grid.asQuantity(selected).coefficient
      val expected = ceilModel(exactCoordinate(coefficient))
      Prop(grid.coordinate(selected) == expected && embedded.compare(coefficient) >= 0)
    ,
    "floor is monotone" -> forAll: (left: Rational, right: Rational) =>
      val (lower, upper) =
        if left.compare(right) <= 0 then
          (left, right)
        else
          (right, left)

      val lowerCoordinate = grid.coordinate(quantity(lower).quantizeTo(grid, Floor).value)
      val upperCoordinate = grid.coordinate(quantity(upper).quantizeTo(grid, Floor).value)
      Prop(lowerCoordinate <= upperCoordinate)
    ,
    "ceiling is monotone" -> forAll: (left: Rational, right: Rational) =>
      val (lower, upper) =
        if left.compare(right) <= 0 then
          (left, right)
        else
          (right, left)

      val lowerCoordinate = grid.coordinate(quantity(lower).quantizeTo(grid, Ceiling).value)
      val upperCoordinate = grid.coordinate(quantity(upper).quantizeTo(grid, Ceiling).value)
      Prop(lowerCoordinate <= upperCoordinate)
    ,
    "nearest chooses a neighbor within half a quantum" -> forAll(
      ExactGenerators.rational,
      ExactGenerators.nearestQuantizationPolicy
    ): (coefficient, policy) =>
      val coordinate  = exactCoordinate(coefficient)
      val lower       = floorModel(coordinate)
      val upper       = ceilModel(coordinate)
      val result      = quantity(coefficient).quantizeTo(grid, policy)
      val selected    = grid.coordinate(result.value)
      val halfQuantum = grid.quantum.unrefined * Rational(1, 2)
      Prop(
        (selected == lower || selected == upper) &&
          result.residual.coefficient.abs.compare(halfQuantum) <= 0
      )
    ,
    "nearest tie behavior including negative ties" -> forAll(
      ExactGenerators.bigInt,
      ExactGenerators.nearestQuantizationPolicy
    ): (lower, policy) =>
      val coordinate  = Rational(lower * 2 + 1, 2)
      val coefficient = coordinate * grid.quantum.unrefined
      val selected    = grid.coordinate(quantity(coefficient).quantizeTo(grid, policy).value)
      Prop(selected == nearestTieExpected(policy, lower))
  )

end QuantizationLaws

/**
 * Euclidean division and finite allocation conservation laws for one grid.
 *
 * The expected model is pure `BigInt` reconstruction, bounds, sums, and spread.
 */
final class DivisionAndAllocationLaws[D <: Dimension, G](grid: GridRef.Grid[D, G]) extends Laws:

  val divisionAndAllocation: RuleSet = new SimpleRuleSet(
    "division and allocation",
    "Euclidean quotient remainder" -> forAll(
      ExactGenerators.coordinate,
      ExactGenerators.positiveWhole
    ): (sourceCoordinate, divisor) =>
      val result    = grid.fromCoordinate(sourceCoordinate).quotRemBy(divisor, grid)
      val quotient  = grid.coordinate(result.quotient)
      val remainder = grid.coordinate(result.remainder)
      Prop(
        sourceCoordinate == quotient * divisor.unrefined + remainder &&
          remainder >= 0 && remainder < divisor.unrefined &&
          grid.fromCoordinate(quotient).sameGridEquals(result.quotient) &&
          grid.fromCoordinate(remainder).sameGridEquals(result.remainder)
      )
    ,
    "allocation conservation and spread for every order" -> forAll(
      ExactGenerators.coordinate,
      ExactGenerators.allocationCount
    ): (sourceCoordinate, count) =>
      val source = grid.fromCoordinate(sourceCoordinate)
      val lawful = RemainderOrder.values.forall: order =>
        val allocation  = source.allocateEvenly(count, order, grid)
        val coordinates = allocation.parts.map(grid.coordinate)
        allocation.size == count.unrefined &&
        coordinates.sum == sourceCoordinate &&
        coordinates.max - coordinates.min <= 1 &&
        allocation.parts.forall(part => grid.fromCoordinate(grid.coordinate(part)).sameGridEquals(part))
      Prop(lawful)
  )

end DivisionAndAllocationLaws
