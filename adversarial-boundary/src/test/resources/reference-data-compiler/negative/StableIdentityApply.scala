package trading.reference

object StableIdentityApply:
  val supportedAssetId = AssetId.from("supported-apply")
  val supportedGridId  = GridId.from("supported-apply")
  val supportedVersion = GridVersion.from(1)

  // OFFENDING-BEGIN
  val assetId = AssetId("apply")
  val gridId  = GridId("apply")
  val version = GridVersion(1)
  // OFFENDING-END

end StableIdentityApply
