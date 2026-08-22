package external.fixtures.negative

import trading.quantity.*

object DimRefForgery:
  type D = Atom["authority:forged"]

  val authoritative: DimRef[D] = DimRef.atom["authority:forged"]

  // OFFENDING-BEGIN
  val forged = new DimRef[D]:
    def key: DimensionKey = DimensionKey.atom(AtomId("authority:contradictory"))
  // OFFENDING-END

end DimRefForgery
