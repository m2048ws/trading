package external.fixtures.negative

import trading.quantity.*

object InvalidCanonicalKeys:
  type Broad = Dim[Power[Singleton, 1] *: EmptyTuple]
  type Bottom = Dim[Power[Nothing, 1] *: EmptyTuple]
  type NullKey = Dim[Power[Null, 1] *: EmptyTuple]
  type WidenedString = Dim[Power[String & Singleton, 1] *: EmptyTuple]
  type WidenedNominal = Dim[Power[DimRef.NominalAtom & Singleton, 1] *: EmptyTuple]

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
