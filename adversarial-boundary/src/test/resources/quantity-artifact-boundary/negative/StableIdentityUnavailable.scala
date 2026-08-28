package external.quantityboundary.negative

import trading.quantity.*

object StableIdentityUnavailable:
  val dimension = DimRef.atomic(AtomId("quantities-only-negative"))

  // OFFENDING-BEGIN
  val assetId = trading.reference.AssetId.from("USD")
  val gridId = trading.reference.GridId.from("cent")
  val version = trading.reference.GridVersion.from(1)
  val catalog = trading.reference.CatalogRoot.create()
  val packed = trading.quantity.runtime.PackedGridQuantity
  // OFFENDING-END

end StableIdentityUnavailable
