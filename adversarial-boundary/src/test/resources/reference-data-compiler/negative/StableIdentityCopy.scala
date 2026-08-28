package trading.reference

import external.reference.fixtures.SharedReferenceDataSetup.*

object StableIdentityCopy:
  val assetId = validAssetId("copy")
  val gridId  = validGridId("copy")
  val version = validGridVersion(1)

  // OFFENDING-BEGIN
  val copiedAssetId = assetId.copy(value = "")
  val copiedGridId  = gridId.copy(value = "")
  val copiedVersion = version.copy(value = 0)
  // OFFENDING-END

end StableIdentityCopy
