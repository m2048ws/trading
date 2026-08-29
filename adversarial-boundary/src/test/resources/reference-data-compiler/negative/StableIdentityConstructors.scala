package trading.reference

object StableIdentityConstructors:
  val supportedAssetId = AssetId.from("supported-constructor")
  val supportedGridId  = GridId.from("supported-constructor")
  val supportedVersion = GridVersion.from(1)

  // OFFENDING-BEGIN
  val assetId = new AssetId("constructor")
  val gridId  = new GridId("constructor")
  val version = new GridVersion(1)
  // OFFENDING-END

end StableIdentityConstructors
