package trading.quantity.runtime

import trading.quantity.AssetId
import trading.quantity.AtomId
import trading.quantity.DimensionKey
import trading.quantity.GridKey
import trading.quantity.JavaSerializationUnsupported
import trading.quantity.Rational

/** Closed hierarchy of definition conflicts, unknown identities, and registry-provenance failures. */
sealed abstract class RegistryError extends JavaSerializationUnsupported with Product with Serializable

/** No asset has been registered for the requested identity. */
final case class UnknownAsset(id: AssetId) extends RegistryError

/** An asset identity was registered again with a different dimension atom. */
final case class ConflictingAssetDefinition(id: AssetId, existingAtom: AtomId, suppliedAtom: AtomId)
  extends RegistryError

/** An asset witness does not belong to the registry performing the operation. */
final case class ForeignAssetWitness(id: AssetId) extends RegistryError

/** Packed asset data declared a dimension different from the resolved asset dimension. */
final case class PackedAssetDimensionMismatch(assetId: AssetId, expected: DimensionKey, resolved: DimensionKey)
  extends RegistryError

/** No dimension has been registered for the requested canonical key. */
final case class UnknownDimension(key: DimensionKey) extends RegistryError

/** A dimension witness does not belong to the registry performing the operation. */
final case class ForeignDimensionWitness(key: DimensionKey) extends RegistryError

/** A canonical dimension key is already owned by an incompatible registration. */
final case class ConflictingDimensionRegistration(key: DimensionKey) extends RegistryError

/** A grid definition names a dimension different from the supplied dimension witness. */
final case class GridDimensionMismatch(expected: DimensionKey, supplied: DimensionKey) extends RegistryError

/** Packed grid identity was found under a canonical dimension other than the requested one. */
final case class PackedGridDimensionMismatch(requested: DimensionKey, registered: DimensionKey, key: GridKey)
  extends RegistryError

/** No grid with the requested dimension-local key has been registered. */
final case class UnknownGrid(dimension: DimensionKey, key: GridKey) extends RegistryError

/** A grid identity was registered again with a different exact quantum. */
final case class ConflictingGridDefinition(
  dimension: DimensionKey,
  key: GridKey,
  existingQuantum: Rational,
  suppliedQuantum: Rational)
  extends RegistryError
