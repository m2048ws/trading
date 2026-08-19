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
  val broad = Normalize.derived[Broad]
  val bottom = Normalize.derived[Bottom]
  val nullKey = Normalize.derived[NullKey]
  val widenedString = Normalize.derived[WidenedString]
  val widenedNominal = Normalize.derived[WidenedNominal]
  def abstractTypeRef(holder: AbstractKeyHolder) = Normalize.derived[Atom[holder.Key]]
  // OFFENDING-END

end InvalidCanonicalKeys
