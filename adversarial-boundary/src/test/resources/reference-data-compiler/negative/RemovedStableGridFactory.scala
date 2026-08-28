package trading.reference

import external.reference.fixtures.SharedReferenceDataSetup.*

import trading.quantity.*
import trading.quantity.refinement.*

object RemovedStableGridFactory:
  val stableId = validGridId("removed-factory")
  val version  = validGridVersion(1)
  val quantum  = PositiveRational.exact(1, 100).fold(error => throw new AssertionError(error.toString), identity)
  val supported = UniformGrid.create(DimRef.one, quantum)

  // OFFENDING-BEGIN
  val forbidden = UniformGrid.create(stableId, version, DimRef.one, quantum)
  // OFFENDING-END

end RemovedStableGridFactory
