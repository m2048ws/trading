package trading.quantity

object RuntimeOpaquePackageSpoof:
  val key: DimensionKey = DimensionKey.atom(AtomId("spoof:opaque"))

  // OFFENDING-BEGIN
  val forged = DimRef.runtimeOpaque(this, key)
  // OFFENDING-END

end RuntimeOpaquePackageSpoof
