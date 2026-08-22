package external.fixtures.negative

import trading.quantity.*

object StaticEvidenceForgery:
  type USD = Atom["asset:USD"]

  // OFFENDING-BEGIN
  val normalization = new Normalize[USD]
  // OFFENDING-END

end StaticEvidenceForgery
