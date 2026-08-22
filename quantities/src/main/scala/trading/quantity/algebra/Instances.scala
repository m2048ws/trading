package trading.quantity.algebra

import algebra.instances.bigInt.given
import algebra.ring.AdditiveCommutativeMonoid
import algebra.ring.AdditiveCommutativeSemigroup
import algebra.ring.MultiplicativeCommutativeGroup
import algebra.ring.Ring
import cats.kernel.Eq
import cats.kernel.Order

import trading.quantity.*
import trading.quantity.refinement.*

/** The one authoritative exact scalar instance. */
object exactScalarAlgebra:

  given rationalExactScalar: ExactScalarField[Rational] with
    def zero: Rational =
      Rational.zero

    def plus(l: Rational, r: Rational): Rational =
      l + r

    def negate(v: Rational): Rational =
      -v

    def one: Rational =
      Rational.one

    override def fromInt(v: Int): Rational =
      Rational(BigInt(v))

    override def fromBigInt(v: BigInt): Rational =
      Rational(v)

    def times(l: Rational, r: Rational): Rational =
      l * r

    def reciprocal(v: NonZero[Rational]): Rational =
      v.reciprocal.unrefined
  end rationalExactScalar

end exactScalarAlgebra

/** Exact quantities form one coherent rational vector-space hierarchy. */
object exactQuantityAlgebra:

  private object universalQuantityVectorSpace extends VectorSpace[Quantity[One], Rational]:
    val scalar: ExactScalarField[Rational] = exactScalarAlgebra.rationalExactScalar

    def zero: Quantity[One] =
      Quantity.zero(using DimRef.one)

    def plus(l: Quantity[One], r: Quantity[One]): Quantity[One] =
      l + r

    def negate(v: Quantity[One]): Quantity[One] =
      Quantity.zero(using DimRef.one) - v

    def timesl(s: Rational, v: Quantity[One]): Quantity[One] =
      v * s
  end universalQuantityVectorSpace

  given quantityVectorSpace[D <: Dimension](using dimension: DimRef[D]): VectorSpace[Quantity[D], Rational] =
    val _ = dimension.key
    universalQuantityVectorSpace.asInstanceOf[VectorSpace[Quantity[D], Rational]]

end exactQuantityAlgebra

/** Grid quantities form one coherent BigInt-module hierarchy. */
object gridQuantityAlgebra:

  private object universalGridModule extends LeftModule[GridQuantity[One, Any], BigInt]:
    val scalar: Ring[BigInt] = summon[Ring[BigInt]]

    def zero: GridQuantity[One, Any] =
      GridQuantity.zero[One, Any](using DimRef.one)

    def plus(l: GridQuantity[One, Any], r: GridQuantity[One, Any]): GridQuantity[One, Any] =
      l + r

    def negate(v: GridQuantity[One, Any]): GridQuantity[One, Any] =
      -v

    def timesl(s: BigInt, v: GridQuantity[One, Any]): GridQuantity[One, Any] =
      v * s
  end universalGridModule

  given gridModule[D <: Dimension, G](using dimension: DimRef[D]): LeftModule[GridQuantity[D, G], BigInt] =
    val _ = dimension.key
    universalGridModule.asInstanceOf[LeftModule[GridQuantity[D, G], BigInt]]

end gridQuantityAlgebra

/** Runtime dimensions expose their canonical free-abelian-group structure. */
object dimensionAlgebra:

  given dimensionKeyGroup: MultiplicativeCommutativeGroup[DimensionKey] with
    def one: DimensionKey =
      DimensionKey.one

    def times(l: DimensionKey, r: DimensionKey): DimensionKey =
      DimensionKey.multiply(l, r)

    override def reciprocal(v: DimensionKey): DimensionKey =
      DimensionKey.inverse(v)

    def div(l: DimensionKey, r: DimensionKey): DimensionKey =
      DimensionKey.multiply(l, DimensionKey.inverse(r))

  given dimensionKeyEq: Eq[DimensionKey] = Eq.fromUniversalEquals

end dimensionAlgebra

/** Exact total orders delegate to the primitive carrier comparisons. */
object exactOrders:

  given rationalOrder: Order[Rational] with
    def compare(l: Rational, r: Rational): Int =
      l.compare(r)

  given quantityOrder[D <: Dimension]: Order[Quantity[D]] with
    def compare(l: Quantity[D], r: Quantity[D]): Int =
      l.coefficient.compare(r.coefficient)

  given gridQuantityOrder[D <: Dimension, G]: Order[GridQuantity[D, G]] with
    def compare(l: GridQuantity[D, G], r: GridQuantity[D, G]): Int =
      l.compareSameGrid(r)

end exactOrders

/** Exact nonzero rationals form a multiplicative commutative group. */
object nonZeroRationalMultiplicative:

  given nonZeroRationalGroup: MultiplicativeCommutativeGroup[NonZero[Rational]] with
    def one: NonZero[Rational] =
      NonZero.rationalOne

    def times(l: NonZero[Rational], r: NonZero[Rational]): NonZero[Rational] =
      l.multiply(r)

    override def reciprocal(v: NonZero[Rational]): NonZero[Rational] =
      v.reciprocal

    def div(l: NonZero[Rational], r: NonZero[Rational]): NonZero[Rational] =
      l.multiply(r.reciprocal)

  given nonZeroRationalEq: Eq[NonZero[Rational]] =
    Eq.instance((l, r) => l.unrefined == r.unrefined)

end nonZeroRationalMultiplicative

/** Closed refined addition, exposed without widening to unrestricted group structure. */
object refinedAdditive:

  private object universalNonNegativeQuantityMonoid extends AdditiveCommutativeMonoid[NonNegative[Quantity[One]]]:
    def zero: NonNegative[Quantity[One]] =
      NonNegative.quantityZero(using DimRef.one)

    def plus(
      l: NonNegative[Quantity[One]],
      r: NonNegative[Quantity[One]]
    ): NonNegative[Quantity[One]] =
      l.add(r)
  end universalNonNegativeQuantityMonoid

  given nonNegativeQuantityMonoid[D <: Dimension](
    using dimension: DimRef[D]
  ): AdditiveCommutativeMonoid[NonNegative[Quantity[D]]] =
    val _ = dimension.key
    universalNonNegativeQuantityMonoid.asInstanceOf[AdditiveCommutativeMonoid[NonNegative[Quantity[D]]]]

  given positiveQuantitySemigroup[D <: Dimension]: AdditiveCommutativeSemigroup[Positive[Quantity[D]]] with
    def plus(l: Positive[Quantity[D]], r: Positive[Quantity[D]]): Positive[Quantity[D]] =
      l.add(r)

  private object universalNonNegativeGridQuantityMonoid
    extends AdditiveCommutativeMonoid[NonNegative[GridQuantity[One, Any]]]:
    def zero: NonNegative[GridQuantity[One, Any]] =
      NonNegative.gridQuantityZero[One, Any](using DimRef.one)

    def plus(
      l: NonNegative[GridQuantity[One, Any]],
      r: NonNegative[GridQuantity[One, Any]]
    ): NonNegative[GridQuantity[One, Any]] =
      l.add(r)
  end universalNonNegativeGridQuantityMonoid

  given nonNegativeGridQuantityMonoid[D <: Dimension, G](
    using dimension: DimRef[D]
  ): AdditiveCommutativeMonoid[NonNegative[GridQuantity[D, G]]] =
    val _ = dimension.key
    universalNonNegativeGridQuantityMonoid
      .asInstanceOf[AdditiveCommutativeMonoid[NonNegative[GridQuantity[D, G]]]]

  given positiveGridQuantitySemigroup[D <: Dimension, G]: AdditiveCommutativeSemigroup[Positive[GridQuantity[D, G]]]
  with
    def plus(l: Positive[GridQuantity[D, G]], r: Positive[GridQuantity[D, G]]): Positive[GridQuantity[D, G]] =
      l.add(r)

  given nonNegativeQuantityEq[D <: Dimension]: Eq[NonNegative[Quantity[D]]] =
    Eq.instance((l, r) => l.unrefined.coefficient == r.unrefined.coefficient)

  given positiveQuantityEq[D <: Dimension]: Eq[Positive[Quantity[D]]] =
    Eq.instance((l, r) => l.unrefined.coefficient == r.unrefined.coefficient)

  given nonNegativeGridQuantityEq[D <: Dimension, G]: Eq[NonNegative[GridQuantity[D, G]]] =
    Eq.instance((l, r) => l.unrefined.sameGridEquals(r.unrefined))

  given positiveGridQuantityEq[D <: Dimension, G]: Eq[Positive[GridQuantity[D, G]]] =
    Eq.instance((l, r) => l.unrefined.sameGridEquals(r.unrefined))

end refinedAdditive
