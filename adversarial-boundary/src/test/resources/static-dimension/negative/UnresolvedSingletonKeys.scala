package external.fixtures.negative

import trading.quantity.*

object UnresolvedSingletonKeys:
  // OFFENDING-BEGIN
  def normalize[K <: Singleton, L <: Singleton] =
    summon[Normalize[Times[Atom[K], Atom[L]]]]
  // OFFENDING-END

end UnresolvedSingletonKeys
