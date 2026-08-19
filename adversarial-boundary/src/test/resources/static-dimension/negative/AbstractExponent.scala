package external.fixtures.negative

import trading.quantity.*

object AbstractExponent:
  type Abstract[E <: Int] = Dim[Power["exponent:abstract", E] *: EmptyTuple]
  type NonLiteral = Dim[Power["exponent:nonliteral", Int] *: EmptyTuple]

  // OFFENDING-BEGIN
  def abstractExponent[E <: Int] = Normalize.derived[Abstract[E]]
  val nonLiteral = Normalize.derived[NonLiteral]
  // OFFENDING-END

end AbstractExponent
