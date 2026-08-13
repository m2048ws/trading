package trading.quantity.runtime

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*

class PackedAssetDimensionRegressionSuite extends FunSuite:

  test("asset decoding rejects remapping to another atom after restart"):
    val assetId = AssetId("remapped-asset")
    val gridId  = GridId("remapped-grid")
    val version = GridVersion(1)
    val quantum = PositiveRational.exact(1, 100).toOption.get

    val writer      = new QuantityRegistry
    val writerAsset = writer.registerAsset(AssetDefinition(assetId, AtomId("atom:A"))).toOption.get
    val writerGrid  =
      writer.registerGrid(writerAsset)(GridDefinition(writerAsset.dimension.key, gridId, version, quantum)).toOption.get
    val packed = PackedAssetGridQuantity.pack(writerAsset)(writerGrid)(writerGrid.fromCoordinate(7))

    val reader      = new QuantityRegistry
    val readerAsset = reader.registerAsset(AssetDefinition(assetId, AtomId("atom:B"))).toOption.get
    val _           =
      reader.registerGrid(readerAsset)(GridDefinition(readerAsset.dimension.key, gridId, version, quantum)).toOption.get

    assertEquals(
      PackedAssetGridQuantity.decode(packed, reader),
      Left(PackedAssetDimensionMismatch(assetId, writerAsset.dimension.key, readerAsset.dimension.key))
    )

end PackedAssetDimensionRegressionSuite
