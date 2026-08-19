package external.fixtures.negative

import trading.quantity.*

object SameDimensionForgery:
  type USD = Atom["asset:USD"]

  // OFFENDING-BEGIN
  val equality = new SameDimension[USD, USD] {}
  // OFFENDING-END

end SameDimensionForgery
