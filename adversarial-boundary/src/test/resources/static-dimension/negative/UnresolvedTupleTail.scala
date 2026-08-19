package external.fixtures.negative

import trading.quantity.*

object UnresolvedTupleTail:
  type Open[Tail <: Tuple] = Dim[Power["tuple:head", 1] *: Tail]

  // OFFENDING-BEGIN
  def normalize[Tail <: Tuple] = Normalize.derived[Open[Tail]]
  // OFFENDING-END

end UnresolvedTupleTail
