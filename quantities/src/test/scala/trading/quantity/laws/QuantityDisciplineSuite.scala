package trading.quantity.laws

import algebra.laws.RingLaws
import cats.kernel.laws.discipline.OrderTests
import org.scalacheck.Arbitrary
import org.scalacheck.Cogen
import org.typelevel.discipline.Predicate

import trading.quantity.*
import trading.quantity.algebra.VectorSpace
import trading.quantity.algebra.exactOrders.given
import trading.quantity.algebra.exactQuantityAlgebra.given
import trading.quantity.algebra.refinedAdditive.given
import trading.quantity.refinement.*
import trading.quantity.testkit.ExactGenerators
import trading.quantity.testkit.ExactGenerators.given
import trading.quantity.testkit.TestAsset

class QuantityDisciplineSuite extends TradingDisciplineSuite:
  private val asset             = TestAsset.runtime(AssetId("quantity-discipline"))
  private given DimRef[asset.D] = asset.dimension

  private given Arbitrary[Quantity[asset.D]]              = ExactGenerators.arbitraryQuantity(asset.dimension)
  private given Cogen[Quantity[asset.D]]                  = ExactGenerators.cogenQuantity
  private given Arbitrary[NonNegative[Quantity[asset.D]]] =
    Arbitrary(ExactGenerators.nonNegativeQuantity(asset.dimension))
  private given Arbitrary[Positive[Quantity[asset.D]]] =
    Arbitrary(ExactGenerators.positiveQuantity(asset.dimension))

  checkAll(
    "Quantity.additiveCommutativeGroup",
    RingLaws[Quantity[asset.D]].additiveCommutativeGroup
  )

  checkAll(
    "Quantity.order",
    OrderTests[Quantity[asset.D]].order
  )

  checkAll(
    "Quantity.vectorSpace",
    new VectorSpaceLaws(summon[VectorSpace[Quantity[asset.D], Rational]]).vectorSpace
  )

  private val first  = TestAsset.runtime(AssetId("graded-first"))
  private val second = TestAsset.runtime(AssetId("graded-second"))
  private val third  = TestAsset.runtime(AssetId("graded-third"))

  checkAll(
    "Quantity.gradedMultiplication",
    new GradedQuantityLaws(first.dimension, second.dimension, third.dimension).gradedQuantity
  )

  checkAll(
    "NonNegativeQuantity.additiveCommutativeMonoid",
    RingLaws[NonNegative[Quantity[asset.D]]].additiveCommutativeMonoid
  )

  private val everyPositiveQuantity = new Predicate[Positive[Quantity[asset.D]]]:
    def apply(v: Positive[Quantity[asset.D]]): Boolean = true

  checkAll(
    "PositiveQuantity.additiveCommutativeSemigroup",
    RingLaws
      .withPred[Positive[Quantity[asset.D]]](everyPositiveQuantity)
      .additiveCommutativeSemigroup
  )

end QuantityDisciplineSuite
