package external.fixtures.negative

import trading.quantity.*

object InvalidCanonicalKeys:
  type Broad = Canonical[Power[Singleton, 1] *: EmptyTuple]
  type Bottom = Canonical[Power[Nothing, 1] *: EmptyTuple]
  type NullKey = Canonical[Power[Null, 1] *: EmptyTuple]
  type WidenedString = Canonical[Power[String & Singleton, 1] *: EmptyTuple]
  type WidenedNominal = Canonical[Power[DimRef.NominalAtom & Singleton, 1] *: EmptyTuple]

  sealed trait AbstractKeyHolder:
    type Key <: Singleton

  // OFFENDING-BEGIN
  val broad = SameDimension.derived[Broad, One]
  val bottom = SameDimension.derived[Bottom, One]
  val nullKey = SameDimension.derived[NullKey, One]
  val widenedString = SameDimension.derived[WidenedString, One]
  val widenedNominal = SameDimension.derived[WidenedNominal, One]
  def abstractTypeRef(holder: AbstractKeyHolder) = SameDimension.derived[Atom[holder.Key], One]
  // OFFENDING-END

end InvalidCanonicalKeys
