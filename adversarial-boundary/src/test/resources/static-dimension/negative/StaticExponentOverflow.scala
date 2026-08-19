package external.fixtures.negative

import trading.quantity.*

object StaticExponentOverflow:
  type Maximum = Dim[Power["overflow", 2147483647] *: EmptyTuple]
  type Minimum = Dim[Power["underflow", -2147483648] *: EmptyTuple]

  // OFFENDING-BEGIN
  val addition = Normalize.derived[Times[Maximum, Atom["overflow"]]]
  val negation = Normalize.derived[Inverse[Minimum]]
  // OFFENDING-END

end StaticExponentOverflow
