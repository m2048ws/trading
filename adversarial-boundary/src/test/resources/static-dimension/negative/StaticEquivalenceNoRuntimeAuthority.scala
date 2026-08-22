package external.fixtures.negative

import trading.quantity.*

object StaticEquivalenceNoRuntimeAuthority:
  type A = Times[Atom["equivalence:A"], Atom["equivalence:B"]]
  type B = Times[Atom["equivalence:B"], Atom["equivalence:A"]]

  val equivalence: SameDimension[A, B] = summon

  // OFFENDING-BEGIN
  val key: DimensionKey  = equivalence.key
  val witness: DimRef[B] = summon[DimRef[B]]
  // OFFENDING-END

end StaticEquivalenceNoRuntimeAuthority
