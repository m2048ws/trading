package trading.economics.instrument

import external.economics.fixtures.SharedEconomicsSetup.*

object PackageSpoofInstrumentSpec:
  val observed = spec.underlyingId

  // OFFENDING-BEGIN
  val forged = new InstrumentSpec:
    val identity = spec.identity
    val roles = spec.roles
    val positionLotGrid = spec.positionLotGrid
    val priceGrid = spec.priceGrid
    val basePerPosition = spec.basePerPosition
    val quotePerPosition = spec.quotePerPosition
  // OFFENDING-END
end PackageSpoofInstrumentSpec
