package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.ValidatedDefinition

object ValidatedDefinitionAuthority:
  // OFFENDING-BEGIN
  val positionGrid = validated.positionGrid
  val priceGrid = validated.priceGrid
  val basePerPosition = validated.basePerPosition
  val quotePerPosition = validated.quotePerPosition
  val forged = new ValidatedDefinition(definition) {}
  // OFFENDING-END

end ValidatedDefinitionAuthority
