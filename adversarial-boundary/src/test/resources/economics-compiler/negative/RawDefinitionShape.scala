package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*
import trading.economics.instrument.*
import trading.reference.AssetId

object RawDefinitionShape:
  val supported: AssetId = baseDefinition.id

  // OFFENDING-BEGIN
  val directId = new InstrumentId("")
  val directUnderlying = new UnderlyingId("")
  val rawId: InstrumentId = "raw-instrument"
  val trustedHandle: AssetId = base
  val snapshotListing: ListingDefinition = snapshot
  val effectDefinition: InstrumentDefinition = () => definition
  // OFFENDING-END
end RawDefinitionShape
