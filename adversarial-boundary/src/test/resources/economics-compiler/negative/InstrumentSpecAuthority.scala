package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*

object InstrumentSpecAuthority:
  val observed = spec.sourceId

  // OFFENDING-BEGIN
  val forged = new InstrumentSpec(identity):
    val roles = spec.roles
    val positionLotGrid = spec.positionLotGrid
    val priceGrid = spec.priceGrid
    val basePerPosition = spec.basePerPosition
    val quotePerPosition = spec.quotePerPosition
  // OFFENDING-END

end InstrumentSpecAuthority
