package external.fixtures.negative

import trading.quantity.*

object ContradictoryNominalAuthority:
  object NominalKey extends DimRef.NominalAtom(AtomId("nominal:authoritative"))
  type Nominal = Atom[NominalKey.type]

  val authoritative: DimRef[Nominal] = DimRef.atom(NominalKey)

  // OFFENDING-BEGIN
  val first: DimRef[Nominal] = DimRef.atom(NominalKey, AtomId("nominal:first"))
  val second: DimRef[Nominal] = DimRef.atom(NominalKey, AtomId("nominal:second"))
  // OFFENDING-END

end ContradictoryNominalAuthority
