package external.fixtures.negative

import trading.quantity.*

object StaticExponentUnderflow:
  type Minimum = Dim[Power["underflow:addition", -2147483648] *: EmptyTuple]

  // OFFENDING-BEGIN
  val underflow = Normalize.derived[Times[Minimum, Inverse[Atom["underflow:addition"]]]]
  // OFFENDING-END

end StaticExponentUnderflow
