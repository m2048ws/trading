package trading.quantity.testkit

import trading.quantity.*

/** Test-only generative atom fixture for mathematical quantity tests. */
final class TestAsset private (val id: AtomId, val atomic: AtomicDimensionRef):
  type D = atomic.D
  val dimension: DimRef[D] = atomic.dimension

object TestAsset:
  def runtime(id: AtomId): TestAsset = new TestAsset(id, DimRef.atomic(id))
