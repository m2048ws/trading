package external.fixtures.negative

import trading.quantity.*

object ClosedMalformedDimension:
  type Zero      = Dim[Power["zero", 0] *: EmptyTuple]
  type Duplicate = Dim[Power["duplicate", 1] *: Power["duplicate", 2] *: EmptyTuple]
  type NonPower  = Dim[String *: EmptyTuple]

  // OFFENDING-BEGIN
  val zero      = SameDimension.derived[Zero, One]
  val duplicate = SameDimension.derived[Duplicate, One]
  val nonPower  = SameDimension.derived[NonPower, One]
  // OFFENDING-END

end ClosedMalformedDimension
