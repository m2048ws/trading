package external.reference.negative

import external.reference.fixtures.SharedReferenceDataSetup.*
import trading.reference.*

object CatalogGuardedConstruction:
  val command = CatalogCommand.RegisterAsset(definition)

  // OFFENDING-BEGIN
  val emptyBatch = new CatalogBatch(command, Vector.empty)
  val negativeRevision = new CatalogRevision(BigInt(-1))
  val emptyDelta = new CatalogDelta(CatalogAddition.Asset(asset.id), Vector.empty)
  val emptyViolations = new CatalogViolations(
    IndexedCatalogViolation(0, 0, CatalogViolation.MissingGridDimension(grid.identity)),
    Vector.empty
  )
  // OFFENDING-END
end CatalogGuardedConstruction
