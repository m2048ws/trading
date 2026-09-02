package external.codec.positive

import trading.codec.*
import trading.economics.instrument.*
import trading.reference.*

object InstrumentDefinitionRecordClient:
  def retain(definition: InstrumentDefinition): InstrumentDefinitionRecord.V1 =
    InstrumentDefinitionRecord.fromDefinition(definition)

  def decode(
    input: String
  ): Either[WireViolations[WireDecodeViolation], InstrumentDefinition] =
    InstrumentDefinitionRecord.decode(input)

  def assemble(
    input: String,
    snapshot: CatalogSnapshot
  ): Either[InstrumentDefinitionReconstructionFailure, Instrument] =
    InstrumentDefinitionRecord.decodeAndAssemble(input, snapshot)

  def assembleBatch(
    inputs: Vector[String],
    snapshot: CatalogSnapshot
  ): Either[WireViolations[IndexedInstrumentDefinitionReconstructionFailure], Vector[Instrument]] =
    InstrumentDefinitionRecord.reconstructBatch(inputs, snapshot)

  def stableProjection(record: InstrumentDefinitionRecord.V1): (InstrumentId, AssetId, GridIdentity) =
    (record.identity.instrumentId, record.roles.baseAssetId, record.listing.priceGridIdentity)
end InstrumentDefinitionRecordClient
