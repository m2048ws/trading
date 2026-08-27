package trading.quantity.runtime

import trading.quantity.AssetId
import trading.quantity.AtomId
import trading.quantity.DimKey
import trading.quantity.GridId
import trading.quantity.GridKey
import trading.quantity.GridVersion
import trading.quantity.JavaSerializationUnsupported
import trading.quantity.refinement.PositiveRational

/** Immutable association between an external asset identity and the atom that defines its dimension. */
final case class AssetDefinition(id: AssetId, dimensionAtom: AtomId) extends JavaSerializationUnsupported

/**
 * Immutable grid definition scoped to a canonical dimension.
 *
 * The grid's local identity is its identifier and version; its positive quantum determines the exact value represented
 * by one coordinate unit.
 */
final case class GridDefinition(dimension: DimKey, id: GridId, version: GridVersion, quantum: PositiveRational)
  extends JavaSerializationUnsupported:
  val key: GridKey = GridKey(id, version)
