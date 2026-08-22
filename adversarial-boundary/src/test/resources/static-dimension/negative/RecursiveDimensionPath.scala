package external.fixtures.negative

import trading.quantity.*

type RecursiveDimensionPath[D <: Dimension] <: Dimension = D match
  case One => RecursiveDimensionPath[D]

object RecursiveDimensionPath:
  def derive(): Unit =
    val _ = ()
    // OFFENDING-BEGIN
    val _ = SameDimension.derived[RecursiveDimensionPath[One], One]
    // OFFENDING-END

end RecursiveDimensionPath
