package trading.quantity

object RuntimeOpaquePackageSpoof:
  val key: DimKey = DimKey.atom(AtomId("spoof:opaque"))

  // OFFENDING-BEGIN
  val forged = DimRef.runtimeOpaque(this, key)
  // OFFENDING-END

end RuntimeOpaquePackageSpoof
