package external.reference.negative

import external.reference.fixtures.SharedReferenceDataSetup.*
import trading.reference.*

object TrustedHandleImplementation:
  val canonicalAsset = asset
  val canonicalGrid  = grid

  // OFFENDING-BEGIN
  val attemptedDimension =
    new DimensionHandle[canonicalAsset.D](new Object, new Object, canonicalAsset.dimension.ref)
  val attemptedAsset =
    new Asset(new Object, new Object, canonicalAsset.id, canonicalAsset.dimension)
  val attemptedGrid =
    new GridHandle[canonicalAsset.D](
      new Object,
      new Object,
      canonicalGrid.identity,
      canonicalGrid.dimension,
      canonicalGrid.grid
    )
  // OFFENDING-END

end TrustedHandleImplementation
