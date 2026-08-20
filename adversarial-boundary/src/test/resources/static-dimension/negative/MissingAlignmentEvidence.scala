package external.fixtures.negative

import trading.quantity.*

object MissingAlignmentEvidence:
  sealed trait G

  // OFFENDING-BEGIN
  def exact[A <: Dimension, B <: Dimension](value: Quantity[A]): Quantity[B] =
    value.alignTo[B]

  def grid[A <: Dimension, B <: Dimension](value: GridQuantity[A, G]): GridQuantity[B, G] =
    value.alignTo[B]
  // OFFENDING-END

end MissingAlignmentEvidence
