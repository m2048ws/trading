package external.fixtures.negative

import trading.quantity.*
import trading.quantity.refinement.*

object MalformedDimensionPreservingArithmetic:
  type Bad = Dim[Power["bad", 0] *: EmptyTuple]
  sealed trait G

  val whole: NonZeroWhole                = NonZeroWhole(2).toOption.get
  val reflexive: SameDimension[Bad, Bad] = summon
  def alignQuantity(value: Quantity[Bad]): Quantity[Bad] = value.alignTo[Bad]
  def alignGrid(value: GridQuantity[Bad, G]): GridQuantity[Bad, G] = value.alignTo[Bad]

  // OFFENDING-BEGIN
  val quantityZero: Quantity[Bad]                                                = Quantity.zero[Bad]
  def quantityAdd(left: Quantity[Bad], right: Quantity[Bad]): Quantity[Bad]      = left + right
  def quantitySubtract(left: Quantity[Bad], right: Quantity[Bad]): Quantity[Bad] = left - right
  def quantityScale(value: Quantity[Bad]): Quantity[Bad]                         = value * Rational(2)
  def quantityDivide(value: Quantity[Bad]): Quantity[Bad]                        = value.exactDivideBy(whole)
  def alignedQuantityAdd(left: Quantity[Bad], right: Quantity[Bad]): Quantity[Bad] =
    left.alignTo[Bad] + right.alignTo[Bad]

  val gridZero: GridQuantity[Bad, G]                                                         = GridQuantity.zero[Bad, G]
  def gridAdd(left: GridQuantity[Bad, G], right: GridQuantity[Bad, G]): GridQuantity[Bad, G] = left + right
  def gridSubtract(left: GridQuantity[Bad, G], right: GridQuantity[Bad, G]): GridQuantity[Bad, G] = left - right
  def gridScale(value: GridQuantity[Bad, G]): GridQuantity[Bad, G]                                = value * BigInt(2)
  def gridNegate(value: GridQuantity[Bad, G]): GridQuantity[Bad, G]                               = -value
  def alignedGridAdd(left: GridQuantity[Bad, G], right: GridQuantity[Bad, G]): GridQuantity[Bad, G] =
    left.alignTo[Bad] + right.alignTo[Bad]
  // OFFENDING-END

end MalformedDimensionPreservingArithmetic
