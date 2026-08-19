package external.fixtures.positive

import trading.quantity.*
import trading.quantity.refinement.*

object NormalizeGeneric:
  def multiply[A <: Dimension, B <: Dimension, O <: Dimension](
    left: Quantity[A],
    right: Quantity[B]
  )(using Normalize.Aux[Times[A, B], O]): Quantity[O] =
    left * right

  def invert[A <: Dimension, O <: Dimension](
    dimension: DimRef[A]
  )(using Normalize.Aux[Inverse[A], O]): DimRef[O] =
    DimRef.inverse(dimension)

  def quotient[A <: Dimension, B <: Dimension, O <: Dimension](
    numerator: Quantity[A],
    denominator: NonZero[Quantity[B]]
  )(using Normalize.Aux[Divide[A, B], O]): Quantity[O] =
    numerator.divideBy(denominator)

  type A = Atom["generic:A"]
  type B = Atom["generic:B"]
  type AB = Dim[Power["generic:A", 1] *: Power["generic:B", 1] *: EmptyTuple]
  type AInverse = Dim[Power["generic:A", -1] *: EmptyTuple]
  type APerB = Dim[Power["generic:A", 1] *: Power["generic:B", -1] *: EmptyTuple]

  val a: DimRef[A] = DimRef.atom["generic:A"]
  val b: DimRef[B] = DimRef.atom["generic:B"]

  given Normalize.Aux[Times[A, B], AB] = Normalize.derived[Times[A, B]]
  given Normalize.Aux[Inverse[A], AInverse] = Normalize.derived[Inverse[A]]
  given Normalize.Aux[Divide[A, B], APerB] = Normalize.derived[Divide[A, B]]

  val product: Quantity[AB] = multiply[A, B, AB](Quantity(a, 2), Quantity(b, 3))
  val inverted: DimRef[AInverse] = invert[A, AInverse](a)
  val divisor: NonZero[Quantity[B]] = NonZero(Quantity(b, 3)).toOption.get
  val divided: Quantity[APerB] = quotient[A, B, APerB](Quantity(a, 6), divisor)

end NormalizeGeneric
