package external.economics.negative

import external.economics.fixtures.SharedEconomicsSetup.*

object SpecAuthorityExtraction:
  val observed = spec.priceGridId

  // OFFENDING-BEGIN
  val lineage = spec.lineage
  val snapshotValue = spec.snapshot
  val retag = spec.retag
  val proof = spec.sameDimension
  // OFFENDING-END
end SpecAuthorityExtraction
