package trading.quantity

import java.util.Objects

/** Stable external identifier for a grid definition. */
final case class GridId(value: String) extends JavaSerializationUnsupported:
  require(value.trim.nonEmpty, "grid ID cannot be empty")

/** Stable external identifier for an asset resolved by a runtime registry. */
final case class AssetId(value: String) extends JavaSerializationUnsupported:
  require(value.trim.nonEmpty, "asset ID cannot be empty")

/** Stable identifier for an atomic component of a runtime dimension. */
final case class AtomId(value: String) extends JavaSerializationUnsupported:
  require(value.trim.nonEmpty, "atom ID cannot be empty")

/** Positive version distinguishing immutable definitions that share a [[GridId]]. */
final case class GridVersion(value: Long) extends JavaSerializationUnsupported:
  require(value > 0, "grid version must be positive")

/** The dimension-local portion of grid identity. Registry lookup additionally requires the owning canonical dimension. */
final case class GridKey(id: GridId, version: GridVersion) extends JavaSerializationUnsupported:
  val _ = Objects.requireNonNull(id, "grid ID")
  val _ = Objects.requireNonNull(version, "grid version")
