package external.fixtures.negative

import trading.quantity.*

object UnresolvedSingletonKeys:
  // OFFENDING-BEGIN
  def compare[K <: Singleton, L <: Singleton] =
    SameDimension.derived[Times[Atom[K], Atom[L]], One]
  // OFFENDING-END

end UnresolvedSingletonKeys
