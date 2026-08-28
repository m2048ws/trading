package trading.quantity.algebra

import algebra.ring.AdditiveCommutativeGroup
import algebra.ring.AdditiveCommutativeMonoid
import algebra.ring.AdditiveCommutativeSemigroup
import algebra.ring.CommutativeRing
import algebra.ring.MultiplicativeCommutativeGroup
import cats.kernel.Order
import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.testkit.CompileAssertions.*

class AlgebraInstancesSuite extends FunSuite:
  private val usd   = trading.quantity.testkit.TestAsset.runtime(AtomId("USD-algebra-suite"))
  private val cents = UniformGrid.create[usd.D](usd.dimension,
    PositiveRational.exact(1, 100).toOption.get
  )
  private given DimRef[usd.D] = usd.dimension

  test("one strongest exact scalar and quantity instance supplies every supported supertype"):
    import exactQuantityAlgebra.given
    import exactScalarAlgebra.given

    val scalar = summon[ExactScalarField[Rational]]
    val ring   = summon[CommutativeRing[Rational]]
    val space  = summon[VectorSpace[Quantity[usd.D], Rational]]
    val module = summon[LeftModule[Quantity[usd.D], Rational]]
    val group  = summon[AdditiveCommutativeGroup[Quantity[usd.D]]]
    val value  = Quantity(usd.dimension, Rational(2, 3))

    assert(scalar == ring)
    assertEquals(space.getClass, module.getClass)
    assertEquals(space.getClass, group.getClass)
    assertEquals(space.timesl(Rational(3, 2), value).coefficient, Rational.one)
    assertEquals(scalar.fromBigInt(BigInt(7)), Rational(7))

  test("one strongest grid module instance supplies the additive group"):
    import gridQuantityAlgebra.given

    val module = summon[LeftModule[GridQuantity[usd.D, cents.G], BigInt]]
    val group  = summon[AdditiveCommutativeGroup[GridQuantity[usd.D, cents.G]]]
    val value  = cents.fromCoordinate(7)

    assertEquals(module.getClass, group.getClass)
    assertEquals(cents.coordinate(module.timesl(BigInt(3), value)), BigInt(21))
    assertEquals(cents.coordinate(group.plus(value, group.negate(value))), BigInt(0))

  test("dimension, nonzero-rational, exact-order, and refined-additive instances are production imports"):
    import dimensionAlgebra.given
    import exactOrders.given
    import nonZeroRationalMultiplicative.given
    import refinedAdditive.given

    val dimensionGroup      = summon[MultiplicativeCommutativeGroup[DimKey]]
    val rationalGroup       = summon[MultiplicativeCommutativeGroup[NonZero[Rational]]]
    val rationalOrder       = summon[Order[Rational]]
    val quantityOrder       = summon[Order[Quantity[usd.D]]]
    val gridOrder           = summon[Order[GridQuantity[usd.D, cents.G]]]
    val nonnegativeQuantity =
      summon[AdditiveCommutativeMonoid[NonNegative[Quantity[usd.D]]]]
    val positiveQuantity =
      summon[AdditiveCommutativeSemigroup[Positive[Quantity[usd.D]]]]
    val nonnegativeGrid =
      summon[AdditiveCommutativeMonoid[NonNegative[GridQuantity[usd.D, cents.G]]]]
    val positiveGrid =
      summon[AdditiveCommutativeSemigroup[Positive[GridQuantity[usd.D, cents.G]]]]

    assertEquals(dimensionGroup.times(DimKey.one, usd.dimension.key), usd.dimension.key)
    assertEquals(rationalGroup.reciprocal(NonZero(Rational(2)).toOption.get).unrefined, Rational(1, 2))
    assertEquals(rationalOrder.compare(Rational(1, 3), Rational(1, 2)), -1)
    assertEquals(
      quantityOrder.compare(Quantity.zero, Quantity(usd.dimension, 1)),
      -1
    )
    assertEquals(gridOrder.compare(cents.fromCoordinate(1), cents.fromCoordinate(2)), -1)
    assertEquals(nonnegativeQuantity.zero.unrefined.coefficient, Rational.zero)
    assertEquals(nonnegativeGrid.zero.unrefined.sameGridHash, cents.fromCoordinate(0).sameGridHash)
    assert(positiveQuantity != null)
    assert(positiveGrid != null)

  test("all documented opt-in imports are coherent together and do not change direct operators"):
    val left       = Quantity(usd.dimension, 2)
    val right      = Quantity(usd.dimension, 3)
    val gridLeft   = cents.fromCoordinate(2)
    val gridRight  = cents.fromCoordinate(3)
    val directSum  = (left + right).coefficient
    val directGrid = cents.coordinate(gridLeft + gridRight)

    import dimensionAlgebra.given
    import exactOrders.given
    import exactQuantityAlgebra.given
    import exactScalarAlgebra.given
    import gridQuantityAlgebra.given
    import nonZeroRationalMultiplicative.given

    val _ = summon[CommutativeRing[Rational]]
    val _ = summon[AdditiveCommutativeGroup[Quantity[usd.D]]]
    val _ = summon[AdditiveCommutativeGroup[GridQuantity[usd.D, cents.G]]]
    val _ = summon[Order[Rational]]
    val _ = summon[MultiplicativeCommutativeGroup[DimKey]]
    val _ = summon[MultiplicativeCommutativeGroup[NonZero[Rational]]]

    assertEquals((left + right).coefficient, directSum)
    assertEquals(cents.coordinate(gridLeft + gridRight), directGrid)
    assertEquals(NonZero(Rational.zero), Left(ExpectedNonZero))

  test("optional imports preserve expression results and explicit dimension evidence"):
    val btc                     = trading.quantity.testkit.TestAsset.runtime(AtomId("BTC-algebra-normalization-suite"))
    val amount                  = Quantity(btc.dimension, Rational(1, 10))
    val usdPerBtc               = Rate(btc.dimension, usd.dimension, Rational(6000001, 100))
    val direct: Quantity[usd.D] = amount.applyRate(usdPerBtc)
    val gridValue               = cents.fromCoordinate(7)

    import dimensionAlgebra.given
    import exactOrders.given
    import exactQuantityAlgebra.given
    import exactScalarAlgebra.given
    import gridQuantityAlgebra.given
    import nonZeroRationalMultiplicative.given
    import refinedAdditive.given

    val _                         = summon[MultiplicativeCommutativeGroup[DimKey]]
    val _                         = summon[Order[Rational]]
    val _                         = summon[VectorSpace[Quantity[usd.D], Rational]]
    val _                         = summon[ExactScalarField[Rational]]
    val _                         = summon[MultiplicativeCommutativeGroup[NonZero[Rational]]]
    val _                         = summon[AdditiveCommutativeMonoid[NonNegative[Quantity[usd.D]]]]
    val imported: Quantity[usd.D] = amount.applyRate(usdPerBtc)

    val a = DimRef.atomic(AtomId("algebra-evidence-a"))
    val b = DimRef.atomic(AtomId("algebra-evidence-b"))
    type AB = Times[a.D, b.D]
    val ab: Quantity[AB] = Quantity(DimRef.times(a.dimension, b.dimension), Rational(2))
    val ba               = Quantity(DimRef.times(b.dimension, a.dimension), Rational(3))
    val staticSum        = ab + ba.alignTo[AB]

    val runtimeLeft  = DimRef.atomic(AtomId("algebra-checked-evidence"))
    val runtimeRight = DimRef.atomic(AtomId("algebra-checked-evidence"))
    val checked      = SameDimension.between(runtimeRight.dimension, runtimeLeft.dimension).get
    val alignedRight = Quantity(runtimeRight.dimension, 5).alignTo[runtimeLeft.D](using checked)
    val checkedSum   = Quantity(runtimeLeft.dimension, 4) + alignedRight

    val gridModule = summon[LeftModule[GridQuantity[usd.D, cents.G], BigInt]]

    assertEquals(imported.coefficient, direct.coefficient)
    assertEquals(imported.coefficient, Rational(6000001, 1000))
    assertEquals(staticSum.coefficient, Rational(5))
    assertEquals(checkedSum.coefficient, Rational(9))
    assertEquals(cents.coordinate(gridModule.timesl(BigInt(3), gridValue)), BigInt(21))
    assertEquals(cents.coordinate(gridValue + gridValue), BigInt(14))

  test("instances are absent without their documented opt-in imports"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import algebra.ring.AdditiveCommutativeGroup
      sealed trait GTag
      type D = Atom["algebra:no-import"]
      type G = GTag
      summon[AdditiveCommutativeGroup[GridQuantity[D, G]]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import cats.kernel.Order
      summon[Order[Rational]]
      """

  test("combine-only refined semigroups are generic while identity-bearing structures remain gated"):
    assertCompiles:
      """
      import _root_.algebra.ring.AdditiveCommutativeSemigroup
      import trading.quantity.*
      import trading.quantity.algebra.refinedAdditive.given
      import trading.quantity.refinement.*

      def quantity[D <: Dim] = summon[AdditiveCommutativeSemigroup[Positive[Quantity[D]]]]
      def grid[D <: Dim, G] = summon[AdditiveCommutativeSemigroup[Positive[GridQuantity[D, G]]]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.algebra.*
      import trading.quantity.algebra.exactQuantityAlgebra.given

      def identity[D <: Dim] = summon[VectorSpace[Quantity[D], Rational]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.algebra.*
      import trading.quantity.algebra.gridQuantityAlgebra.given

      def identity[D <: Dim, G] = summon[LeftModule[GridQuantity[D, G], BigInt]]
      """

  test("unsupported rings, fields, numerics, categories, and refined groups remain absent"):
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.algebra.exactScalarAlgebra.given
      import algebra.ring.Field
      summon[Field[Rational]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.algebra.exactQuantityAlgebra.given
      import algebra.ring.Ring
      type D = Atom["algebra:ring"]
      summon[Ring[Quantity[D]]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.algebra.exactQuantityAlgebra.given
      type D = Atom["algebra:numeric"]
      summon[Numeric[Quantity[D]]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.algebra.gridQuantityAlgebra.given
      import algebra.ring.Ring
      sealed trait GTag
      type D = Atom["algebra:grid-ring"]
      type G = GTag
      summon[Ring[GridQuantity[D, G]]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.algebra.gridQuantityAlgebra.given
      sealed trait GTag
      type D = Atom["algebra:grid-numeric"]
      type G = GTag
      summon[Numeric[GridQuantity[D, G]]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import cats.arrow.Category
      summon[Category[Rate]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      import trading.quantity.algebra.refinedAdditive.given
      import algebra.ring.AdditiveCommutativeGroup
      type D = Atom["algebra:refined-group"]
      summon[AdditiveCommutativeGroup[NonNegative[Quantity[D]]]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      import trading.quantity.algebra.refinedAdditive.given
      import algebra.ring.AdditiveCommutativeGroup
      type D = Atom["algebra:nonzero-group"]
      summon[AdditiveCommutativeGroup[NonZero[Quantity[D]]]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.refinement.*
      import trading.quantity.algebra.refinedAdditive.given
      type D = Atom["algebra:left-module"]
      summon[LeftModule[NonNegative[Quantity[D]], Rational]]
      """
    assertDoesNotCompile:
      """
      import trading.quantity.*
      import trading.quantity.algebra.dimensionAlgebra.given
      import cats.kernel.Order
      summon[Order[DimKey]]
      """

end AlgebraInstancesSuite
