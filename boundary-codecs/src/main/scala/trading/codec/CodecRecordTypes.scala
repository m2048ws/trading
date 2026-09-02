package trading.codec

/** Stable record-type registry used only for explicit cross-family envelope diagnostics. */
private[codec] object CodecRecordTypes:
  val generalGridCoordinate: RecordType =
    RecordType.from("trading.general-grid-coordinate").toOption.get
  val assetGridCoordinate: RecordType =
    RecordType.from("trading.asset-grid-coordinate").toOption.get
  val catalogJournalEntry: RecordType =
    RecordType.from("trading.catalog-journal-entry").toOption.get
  val instrumentDefinition: RecordType =
    RecordType.from("trading.instrument-definition").toOption.get
  val order: RecordType =
    RecordType.from("trading.order").toOption.get
  val orderScenario: RecordType =
    RecordType.from("trading.order-scenario").toOption.get
  val roundTripScenario: RecordType =
    RecordType.from("trading.round-trip-scenario").toOption.get

  val current: Set[RecordType] = Set(
    generalGridCoordinate,
    assetGridCoordinate,
    catalogJournalEntry,
    instrumentDefinition,
    order,
    orderScenario,
    roundTripScenario
  )

  def otherThan(recordType: RecordType): Set[RecordType] =
    current - recordType
end CodecRecordTypes
