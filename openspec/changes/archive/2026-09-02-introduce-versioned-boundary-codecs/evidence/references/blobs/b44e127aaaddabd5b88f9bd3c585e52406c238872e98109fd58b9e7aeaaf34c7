package external.codec.negative

import trading.codec.*
import trading.economics.instrument.*
import trading.reference.*

object InstrumentDefinitionAuthorityEscapesAreUnavailable:
  def retained(definition: InstrumentDefinition): InstrumentDefinitionRecord.V1 =
    InstrumentDefinitionRecord.fromDefinition(definition)

  // OFFENDING-BEGIN
  def encodeInstrument(instrument: Instrument) =
    InstrumentDefinitionRecord.encode(instrument)

  def encodeSpec(spec: InstrumentSpec) =
    InstrumentDefinitionRecord.encode(spec)

  def assembleFromRoot(input: String, root: CatalogRoot) =
    InstrumentDefinitionRecord.decodeAndAssemble(input, root)

  def batchFromRoot(inputs: Vector[String], root: CatalogRoot) =
    InstrumentDefinitionRecord.reconstructBatch(inputs, root)

  def authority(record: InstrumentDefinitionRecord.V1) =
    (
      record.instrument,
      record.instrumentSpec,
      record.catalogRevision,
      record.lineage,
      record.snapshot,
      record.positionLotGridHandle,
      record.priceGridHandle,
      record.market,
      record.venue,
      record.productFamily
    )
  // OFFENDING-END
end InstrumentDefinitionAuthorityEscapesAreUnavailable
