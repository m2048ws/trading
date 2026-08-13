package trading.quantity.laws

import algebra.laws.RingLaws
import cats.kernel.laws.discipline.OrderTests
import org.typelevel.discipline.Predicate

import trading.quantity.Rational
import trading.quantity.algebra.ExactScalarField
import trading.quantity.algebra.exactOrders.given
import trading.quantity.algebra.exactScalarAlgebra.given
import trading.quantity.algebra.nonZeroRationalMultiplicative.given
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators.given

class RationalDisciplineSuite extends TradingDisciplineSuite:

  checkAll(
    "Rational.commutativeRing",
    RingLaws[Rational].commutativeRing
  )

  checkAll(
    "Rational.order",
    OrderTests[Rational].order
  )

  checkAll(
    "Rational.exactScalarField",
    new ExactScalarFieldLaws(summon[ExactScalarField[Rational]]).exactScalarField
  )

  checkAll(
    "Rational.canonicality",
    new RationalCanonicalityLaws().canonicality
  )

  private val allNonZero = new Predicate[NonZero[Rational]]:
    def apply(v: NonZero[Rational]): Boolean = true

  checkAll(
    "NonZeroRational.multiplicativeCommutativeGroup",
    RingLaws
      .withPred[NonZero[Rational]](allNonZero)
      .multiplicativeCommutativeGroup
  )

end RationalDisciplineSuite
