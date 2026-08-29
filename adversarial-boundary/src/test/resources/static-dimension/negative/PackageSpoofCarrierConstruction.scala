package trading.quantity

object PackageSpoofCarrierConstruction:
  type Bad = Canonical[Power["spoof:construction:bad", 0] *: EmptyTuple]
  sealed trait G

  // OFFENDING-BEGIN
  val rawQuantity: Quantity[Bad]             = Rational.one
  val rawCoordinate: GridQuantity[Bad, G]    = BigInt(1)
  val hiddenQuantity: Quantity[Bad]          = Quantity.fromCoefficient(Rational.one)
  val hiddenCoordinate: GridQuantity[Bad, G] = GridQuantity.fromCoordinate(BigInt(1))
  val removedNormalization                   = new Normalize[Bad]
  val forgedDimension = new DimRef[Bad]:
    val key = DimKey.one
  // OFFENDING-END

end PackageSpoofCarrierConstruction
