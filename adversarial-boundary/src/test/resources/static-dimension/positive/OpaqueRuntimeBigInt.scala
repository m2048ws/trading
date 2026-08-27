package external.fixtures.positive

import trading.quantity.*

object OpaqueRuntimeBigInt:
  val exponent: BigInt = BigInt(Int.MaxValue) + 1
  val key: DimKey = DimKey(List(AtomId("runtime:bigint") -> exponent))
  val runtime = DimRef.fresh(key)

  val value: Quantity[runtime.D] = Quantity(runtime.dimension, 1)
  val inverse = DimRef.inverse(runtime.dimension)
  val product = DimRef.times(runtime.dimension, runtime.dimension)

end OpaqueRuntimeBigInt
