package external.fixtures.negative

import trading.quantity.*

object UnsupportedDimensionShapes:
  type A = Atom["shape:A"]
  type B = Atom["shape:B"]
  type Refined = A { type Marker = String }
  type Intersected = A & B
  type Unioned = A | B
  type Selected[D <: Dim] <: Dim = D match
    case One => A
    case _   => B

  // OFFENDING-BEGIN
  val refined = SameDimension.derived[Refined, One]
  val intersected = SameDimension.derived[Intersected, One]
  val unioned = SameDimension.derived[Unioned, One]
  def unresolvedMatch[D <: Dim] = SameDimension.derived[Selected[D], One]
  // OFFENDING-END

end UnsupportedDimensionShapes
