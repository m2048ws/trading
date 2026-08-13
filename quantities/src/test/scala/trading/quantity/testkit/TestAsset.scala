package trading.quantity.testkit

import trading.quantity.*

/** Test-only generative atom fixture. Production code uses the interned runtime registry. */
final class TestAsset private (val id: AssetId, val atomic: AtomicDimensionRef):
  type D = atomic.D
  val dimension: DimRef[D] = atomic.dimension

object TestAsset:
  def runtime(id: AssetId): TestAsset = new TestAsset(id, DimRef.atomic(AtomId(id.value)))
