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

  given quantityVectorSpace[D <: Dimension]: VectorSpace[Quantity[D], Rational] with
    val scalar: ExactScalarField[Rational] = exactScalarAlgebra.rationalExactScalar

    def zero: Quantity[D] =
      Quantity.zero

    def plus(l: Quantity[D], r: Quantity[D]): Quantity[D] =
      l + r

    def negate(v: Quantity[D]): Quantity[D] =
      Quantity.zero - v

    def timesl(s: Rational, v: Quantity[D]): Quantity[D] =
      v * s

end exactQuantityAlgebra

/** Grid quantities form one coherent BigInt-module hierarchy. */
object gridQuantityAlgebra:

  given gridModule[D <: Dimension, G]: LeftModule[GridQuantity[D, G], BigInt] with
    val scalar: Ring[BigInt] = summon[Ring[BigInt]]

    def zero: GridQuantity[D, G] =
      GridQuantity.zero

    def plus(l: GridQuantity[D, G], r: GridQuantity[D, G]): GridQuantity[D, G] =
      l + r

    def negate(v: GridQuantity[D, G]): GridQuantity[D, G] =
      -v

    def timesl(s: BigInt, v: GridQuantity[D, G]): GridQuantity[D, G] =
      v * s

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

  given nonNegativeQuantityMonoid[D <: Dimension]: AdditiveCommutativeMonoid[NonNegative[Quantity[D]]] with
    def zero: NonNegative[Quantity[D]] =
      NonNegative.quantityZero

    def plus(l: NonNegative[Quantity[D]], r: NonNegative[Quantity[D]]): NonNegative[Quantity[D]] =
      l.add(r)

  given positiveQuantitySemigroup[D <: Dimension]: AdditiveCommutativeSemigroup[Positive[Quantity[D]]] with
    def plus(l: Positive[Quantity[D]], r: Positive[Quantity[D]]): Positive[Quantity[D]] =
      l.add(r)

  given nonNegativeGridQuantityMonoid[D <: Dimension, G]: AdditiveCommutativeMonoid[NonNegative[GridQuantity[D, G]]]
  with
    def zero: NonNegative[GridQuantity[D, G]] =
      NonNegative.gridQuantityZero

    def plus(l: NonNegative[GridQuantity[D, G]], r: NonNegative[GridQuantity[D, G]]): NonNegative[GridQuantity[D, G]] =
      l.add(r)

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
