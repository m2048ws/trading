package external.fixtures.negative

import trading.quantity.*

object AbstractExponent:
  type Abstract[E <: Int] = Canonical[Power["exponent:abstract", E] *: EmptyTuple]
  type NonLiteral = Canonical[Power["exponent:nonliteral", Int] *: EmptyTuple]

  // OFFENDING-BEGIN
  def abstractExponent[E <: Int] = SameDimension.derived[Abstract[E], One]
  val nonLiteral = SameDimension.derived[NonLiteral, One]
  // OFFENDING-END

end AbstractExponent
