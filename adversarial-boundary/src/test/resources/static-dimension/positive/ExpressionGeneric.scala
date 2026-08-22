package external.fixtures.positive

import trading.quantity.*
import trading.quantity.refinement.*

object ExpressionGeneric:
  def multiply[A <: Dimension, B <: Dimension](
    left: Quantity[A],
    right: Quantity[B]
  ): Quantity[Times[A, B]] =
    left * right

  def invert[A <: Dimension](dimension: DimRef[A]): DimRef[Inverse[A]] =
    DimRef.inverse(dimension)

  def quotient[A <: Dimension, B <: Dimension](
    numerator: Quantity[A],
    denominator: NonZero[Quantity[B]]
  ): Quantity[Divide[A, B]] =
    numerator.divideBy(denominator)

  def nominate[A <: Dimension, B <: Dimension, O <: Dimension](
    left: Quantity[A],
    right: Quantity[B]
  )(using SameDimension[Times[A, B], O]): Quantity[O] =
    multiply(left, right).alignTo[O]

  type A = Atom["generic:A"]
  type B = Atom["generic:B"]
  type AB = Dim[Power["generic:A", 1] *: Power["generic:B", 1] *: EmptyTuple]

  val a: DimRef[A] = DimRef.atom["generic:A"]
  val b: DimRef[B] = DimRef.atom["generic:B"]

  val product: Quantity[Times[A, B]] = multiply(Quantity(a, 2), Quantity(b, 3))
  val named: Quantity[AB] = nominate[A, B, AB](Quantity(a, 2), Quantity(b, 3))
  val inverted: DimRef[Inverse[A]] = invert(a)
  val divisor: NonZero[Quantity[B]] = NonZero(Quantity(b, 3)).toOption.get
  val divided: Quantity[Divide[A, B]] = quotient(Quantity(a, 6), divisor)

end ExpressionGeneric
