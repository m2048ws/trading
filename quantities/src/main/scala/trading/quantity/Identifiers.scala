package trading.quantity

/** Stable identifier for an atomic component of a runtime dimension. */
final case class AtomId(value: String) extends JavaSerializationUnsupported:
  require(value.trim.nonEmpty, "atom ID cannot be empty")
