package external.fixtures.negative

import trading.quantity.*

object MissingAlignmentEvidence:
  sealed trait G

  // OFFENDING-BEGIN
  def exact[A <: Dim, B <: Dim](value: Quantity[A]): Quantity[B] =
    value.alignTo[B]

  def grid[A <: Dim, B <: Dim](value: GridQuantity[A, G]): GridQuantity[B, G] =
    value.alignTo[B]
  // OFFENDING-END

end MissingAlignmentEvidence
