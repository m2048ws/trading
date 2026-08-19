package external.fixtures.negative

import trading.quantity.*

object WidenedNominalAtom:
  object First extends DimRef.NominalAtom(AtomId("nominal:first"))
  object Second extends DimRef.NominalAtom(AtomId("nominal:second"))

  type K = DimRef.NominalAtom & Singleton
  type D = Atom[K]

  val firstKey: K = First
  val secondKey: K = Second

  // OFFENDING-BEGIN
  val first: DimRef[D] = DimRef.atom(firstKey)
  val second: DimRef[D] = DimRef.atom(secondKey)
  // OFFENDING-END

end WidenedNominalAtom
