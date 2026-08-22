package external.fixtures.negative

import trading.quantity.*

object NullCarrierConstruction:
  sealed trait G

  // OFFENDING-BEGIN
  val quantity: Quantity[One]            = null
  val gridQuantity: GridQuantity[One, G] = null
  // OFFENDING-END

end NullCarrierConstruction
