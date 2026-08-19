package external.fixtures.negative

import trading.quantity.*

object StaticEvidenceForgery:
  type USD = Atom["asset:USD"]

  // OFFENDING-BEGIN
  val normalization = new Normalize[USD]:
    type Out = USD
  // OFFENDING-END

end StaticEvidenceForgery
