package trading.quantity.runtime

import trading.quantity.*

/**
 * Logical packed boundary data for an asset grid quantity.
 *
 * Stable runtime identities replace the value's path-dependent types at the boundary. Decoding resolves those
 * identities through a [[QuantityRegistry]] to recover a type-safe value.
 *
 * This is not a stable production wire schema; a separate schema version is required before production persistence.
 */
final case class PackedAssetGridQuantity(
  assetId: AssetId,
  expectedDimension: DimKey,
  gridId: GridId,
  gridVersion: GridVersion,
  coordinate: BigInt)
  extends JavaSerializationUnsupported

/** Packing and registry-backed decoding for [[PackedAssetGridQuantity]]. */
object PackedAssetGridQuantity:

  def pack(a: trading.quantity.runtime.AssetRef)(g: RegisteredGridRef[a.D])(v: GridQuantity[a.D, g.G]): PackedAssetGridQuantity =
    PackedAssetGridQuantity(
      a.id,
      a.dimension.key,
      g.id,
      g.version,
      g.coordinate(v)
    )

  def decode(p: PackedAssetGridQuantity, registry: QuantityRegistry): Either[RegistryError, ResolvedAssetGridQuantity] =
    registry
      .resolveAsset(p.assetId)
      .flatMap: asset =>
        if asset.dimension.key != p.expectedDimension then
          Left(PackedAssetDimensionMismatch(p.assetId, p.expectedDimension, asset.dimension.key))
        else
          registry
            .resolveGridForDecode(asset)(GridKey(p.gridId, p.gridVersion))
            .map(grid => new ResolvedAssetGridQuantity(asset)(grid)(grid.fromCoordinate(p.coordinate)))

end PackedAssetGridQuantity

/**
 * Logical packed boundary data for a grid quantity in a general dimension.
 *
 * The dimension and grid are represented by stable runtime identities. Decoding resolves them through a
 * [[QuantityRegistry]] to recover their path-dependent types safely.
 *
 * This is not a stable production wire schema; a separate schema version is required before production persistence.
 */
final case class PackedGridQuantity(
  dimension: DimKey,
  gridId: GridId,
  gridVersion: GridVersion,
  coordinate: BigInt)
  extends JavaSerializationUnsupported

/** Packing and registry-backed decoding for [[PackedGridQuantity]]. */
object PackedGridQuantity:

  def pack[D <: Dim](g: RegisteredGridRef[D])(v: GridQuantity[D, g.G]): PackedGridQuantity =
    PackedGridQuantity(
      g.dimension.key,
      g.id,
      g.version,
      g.coordinate(v)
    )

  def decode(p: PackedGridQuantity, registry: QuantityRegistry): Either[RegistryError, ResolvedGridQuantity] =
    registry
      .resolveDimension(p.dimension)
      .flatMap: dimension =>
        registry
          .resolveGridForDecode(dimension)(GridKey(p.gridId, p.gridVersion))
          .map(grid => new ResolvedGridQuantity(dimension)(grid)(grid.fromCoordinate(p.coordinate)))

end PackedGridQuantity

/**
 * An asset, registered grid, and grid quantity recovered together from packed runtime identities.
 *
 * The dependent fields ensure that the value's dimension and grid types match the resolved asset and grid witnesses.
 */
final class ResolvedAssetGridQuantity(
  val asset: trading.quantity.runtime.AssetRef
)(
  val grid: RegisteredGridRef[asset.D]
)(
  val value: GridQuantity[asset.D, grid.G])
  extends JavaSerializationUnsupported

/**
 * A registered dimension, grid, and grid quantity recovered together from packed runtime identities.
 *
 * The dependent fields preserve the relationships between the resolved witnesses and the value when the dimension was
 * not statically known by the caller.
 */
final class ResolvedGridQuantity(
  val dimension: DimensionWitness
)(
  val grid: RegisteredGridRef[dimension.D]
)(
  val value: GridQuantity[dimension.D, grid.G])
  extends JavaSerializationUnsupported
