package external.fixtures.negative

import trading.quantity.*

object UnresolvedTupleTail:
  type Open[Tail <: Tuple] = Canonical[Power["tuple:head", 1] *: Tail]

  // OFFENDING-BEGIN
  def compare[Tail <: Tuple] = SameDimension.derived[Open[Tail], One]
  // OFFENDING-END

end UnresolvedTupleTail
