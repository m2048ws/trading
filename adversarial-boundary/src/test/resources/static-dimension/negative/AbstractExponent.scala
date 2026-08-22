package external.fixtures.negative

import trading.quantity.*

object AbstractExponent:
  type Abstract[E <: Int] = Dim[Power["exponent:abstract", E] *: EmptyTuple]
  type NonLiteral = Dim[Power["exponent:nonliteral", Int] *: EmptyTuple]

  // OFFENDING-BEGIN
  def abstractExponent[E <: Int] = SameDimension.derived[Abstract[E], One]
  val nonLiteral = SameDimension.derived[NonLiteral, One]
  // OFFENDING-END

end AbstractExponent
