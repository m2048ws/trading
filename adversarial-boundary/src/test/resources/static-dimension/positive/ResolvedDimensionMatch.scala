package external.fixtures.positive

import trading.quantity.*

object ResolvedDimensionMatch:
  type Select[D <: Dim] <: Dim = D match
    case One => Atom["resolved:match"]

  type Selected = Select[One]

  val equivalent: SameDimension[Times[Selected, One], Atom["resolved:match"]] = summon

end ResolvedDimensionMatch
