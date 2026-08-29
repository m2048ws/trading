package trading.quantity.laws

import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators

/**
 * Checked-construction, implication, closure, and deliberate weakening laws for refinements.
 *
 * Expected predicates use public exact signs and grid coordinates; closure is observed through unrestricted values, not
 * by invoking refinement construction again.
 */
final class RefinementLaws[D <: Dim, G](grid: GridRef.Grid[D, G])(using Arbitrary[Rational]) extends Laws:

  private def quantity(c: Rational): Quantity[D] =
    Quantity(grid.dimension, c)

  val refinements: RuleSet = new SimpleRuleSet(
    "refinement lattice and closure",
    "checked quantity construction agrees with exact sign" -> forAll: (coefficient: Rational) =>
      val value       = quantity(coefficient)
      val nonnegative = NonNegative(value).isRight == coefficient.signum >= 0
      val nonzero     = NonZero(value).isRight == (coefficient.signum != 0)
      val positive    = Positive(value).isRight == coefficient.signum > 0
      Prop(nonnegative && nonzero && positive)
    ,
    "checked grid construction agrees with coordinate sign" -> forAll(ExactGenerators.coordinate): coordinate =>
      val value       = grid.fromCoordinate(coordinate)
      val nonnegative = NonNegative(value).isRight == coordinate.signum >= 0
      val nonzero     = NonZero(value).isRight == (coordinate.signum != 0)
      val positive    = Positive(value).isRight == coordinate.signum > 0
      Prop(nonnegative && nonzero && positive)
    ,
    "positive implications preserve the exact value" -> forAll(
      ExactGenerators.positiveQuantity(grid.dimension),
      ExactGenerators.positiveGridQuantity(grid)
    ): (exact, discrete) =>
      Prop(
        exact.asNonNegative.unrefined.coefficient == exact.unrefined.coefficient &&
          exact.asNonZero.unrefined.coefficient == exact.unrefined.coefficient &&
          grid.coordinate(discrete.asNonNegative.unrefined) == grid.coordinate(discrete.unrefined) &&
          grid.coordinate(discrete.asNonZero.unrefined) == grid.coordinate(discrete.unrefined)
      ),
    "nonnegative addition is closed" -> forAll(
      ExactGenerators.nonNegativeQuantity(grid.dimension),
      ExactGenerators.nonNegativeQuantity(grid.dimension),
      ExactGenerators.nonNegativeGridQuantity(grid),
      ExactGenerators.nonNegativeGridQuantity(grid)
    ): (leftExact, rightExact, leftGrid, rightGrid) =>
      val exactSum = leftExact.add(rightExact)
      val gridSum  = leftGrid.add(rightGrid)
      Prop(exactSum.unrefined.coefficient.signum >= 0 && grid.coordinate(gridSum.unrefined) >= 0)
    ,
    "positive addition is closed" -> forAll(
      ExactGenerators.positiveQuantity(grid.dimension),
      ExactGenerators.positiveQuantity(grid.dimension),
      ExactGenerators.positiveGridQuantity(grid),
      ExactGenerators.positiveGridQuantity(grid)
    ): (leftExact, rightExact, leftGrid, rightGrid) =>
      val exactSum = leftExact.add(rightExact)
      val gridSum  = leftGrid.add(rightGrid)
      Prop(exactSum.unrefined.coefficient.signum > 0 && grid.coordinate(gridSum.unrefined) > 0)
    ,
    "nonzero addition deliberately returns an unrestricted value" -> forAll(
      ExactGenerators.nonZeroRational
    ): coefficient =>
      val positive = quantity(coefficient)
      val negative = quantity(-coefficient)
      (NonZero(positive), NonZero(negative)) match
        case (Right(left), Right(right)) => Prop(left.add(right).coefficient == Rational.zero)
        case _                           => Prop.falsified,
    "exact division preserves supported refinements" -> forAll(
      ExactGenerators.positiveQuantity(grid.dimension),
      ExactGenerators.nonNegativeQuantity(grid.dimension),
      ExactGenerators.nonZeroQuantity(grid.dimension),
      ExactGenerators.positiveWhole,
      ExactGenerators.nonZeroWhole
    ): (positive, nonnegative, nonzero, positiveDivisor, nonzeroDivisor) =>
      val positiveResult    = positive.exactDivideBy(positiveDivisor)
      val nonnegativeResult = nonnegative.exactDivideBy(positiveDivisor)
      val nonzeroResult     = nonzero.exactDivideBy(nonzeroDivisor)
      Prop(
        positiveResult.unrefined.coefficient.signum > 0 &&
          nonnegativeResult.unrefined.coefficient.signum >= 0 &&
          nonzeroResult.unrefined.coefficient.signum != 0
      )
    ,
    "positive grid quotient may weaken to nonnegative zero" -> {
      (Positive(grid.fromCoordinate(BigInt(1))), PositiveWhole(BigInt(2))) match
        case (Right(value), Right(divisor)) =>
          val quotient = value.quotRemBy(divisor, grid).quotient
          Prop(grid.coordinate(quotient.unrefined) == 0)
        case _ => Prop.falsified
    },
    "positive quantization may weaken to nonnegative zero" -> {
      val tiny = grid.quantum.unrefined * Rational(1, 10)
      Positive(quantity(tiny)) match
        case Right(value) =>
          val quantized = value.quantizeTo(grid, QuantizationPolicy.HalfEven)
          Prop(grid.coordinate(quantized.value.unrefined) == 0)
        case Left(_) => Prop.falsified
    }
  )

end RefinementLaws
