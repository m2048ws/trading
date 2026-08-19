package external.fixtures.negative

import trading.quantity.*

object UnsupportedDimensionShapes:
  type A = Atom["shape:A"]
  type B = Atom["shape:B"]
  type Refined = A { type Marker = String }
  type Intersected = A & B
  type Unioned = A | B
  type Selected[D <: Dimension] <: Dimension = D match
    case One => A
    case _   => B

  // OFFENDING-BEGIN
  val refined = Normalize.derived[Refined]
  val intersected = Normalize.derived[Intersected]
  val unioned = Normalize.derived[Unioned]
  def unresolvedMatch[D <: Dimension] = Normalize.derived[Selected[D]]
  // OFFENDING-END

end UnsupportedDimensionShapes
