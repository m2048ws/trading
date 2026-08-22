package external.fixtures.positive

import trading.quantity.*

object LargeExpressionInterpretation:
  type Maximum = Dim[Power["large:power", 2147483647] *: EmptyTuple]
  type BeyondIntLeft = Times[Maximum, Atom["large:power"]]
  type BeyondIntRight = Times[Atom["large:power"], Maximum]
  type Cancelled = Times[BeyondIntLeft, Inverse[Maximum]]

  val exactBeyondInt: SameDimension[BeyondIntLeft, BeyondIntRight] = summon
  val exactCancellation: SameDimension[Cancelled, Atom["large:power"]] = summon

  val runtime = DimRef.fresh(
    DimensionKey(List(AtomId("large:runtime") -> (BigInt(Int.MaxValue) + 1)))
  )
  val cancelled = DimRef.times(runtime.dimension, DimRef.inverse(runtime.dimension))
  assert(cancelled.key == DimensionKey.one)

end LargeExpressionInterpretation
