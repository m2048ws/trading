package external.fixtures.negative

import trading.quantity.*

object NormalizationDoesNotInhabit:
  object StaticOnlyKey

  type StaticOnly = Atom[StaticOnlyKey.type]

  val normalization: Normalize[StaticOnly] = summon
  val zero: Quantity[StaticOnly]           = Quantity.zero[StaticOnly]

  // OFFENDING-BEGIN
  val inferred: DimRef[StaticOnly]    = summon[DimRef[StaticOnly]]
  val constructed: DimRef[StaticOnly] = DimRef.atom(StaticOnlyKey)
  val converted: DimRef[StaticOnly]   = normalization
  // OFFENDING-END

end NormalizationDoesNotInhabit
