package client

import trading.codec.OrderScenarioRecord
import trading.codec.RoundTripScenarioRecord
import trading.economics.instrument.Instrument
import trading.reference.CatalogSnapshot

object ScenarioRecordAuthorityEscapesAreUnavailable:
  def prelude(
    record: OrderScenarioRecord.V1,
    trip: RoundTripScenarioRecord.V1,
    wire: String,
    instrument: Instrument,
    snapshot: CatalogSnapshot
  ): Unit =
    val stable = OrderScenarioRecord.encode(record)
    val rebuilt = OrderScenarioRecord.decodeAndReconstruct(wire, instrument, snapshot)
    val tripStable = RoundTripScenarioRecord.encode(trip)
    val _ = (stable, rebuilt, tripStable)

  // OFFENDING-BEGIN
  def decodeAsAny(wire: String) = OrderScenarioRecord.decode[Any](wire)
  def decodeKindOptions(wire: String) = OrderScenarioRecord.decode(wire, "fixed", Map("observed" -> None))
  def actualExecution(record: OrderScenarioRecord.V1) = record.actualExecution
  def fillId(record: OrderScenarioRecord.V1) = record.fillId
  def venue(record: OrderScenarioRecord.V1) = record.venue
  def feePolicy(record: OrderScenarioRecord.V1) = record.feePolicy
  def fees(record: OrderScenarioRecord.V1) = record.fees
  def pnl(record: OrderScenarioRecord.V1) = record.pnl
  def lifecycle(record: OrderScenarioRecord.V1) = record.lifecycle
  def catalogRevision(record: OrderScenarioRecord.V1) = record.catalogRevision
  def targetAsset(record: OrderScenarioRecord.AdditionalConversion) = record.targetAssetId
  def heldPosition(record: RoundTripScenarioRecord.V1) = record.heldPosition
  def pricePnl(record: RoundTripScenarioRecord.V1) = record.pricePnl
  def netPnl(record: RoundTripScenarioRecord.V1) = record.netPnl
  def reconstructWithoutSnapshot(record: OrderScenarioRecord.V1, instrument: Instrument) =
    OrderScenarioRecord.reconstruct(record, instrument)
  def reconstructTripWithoutSnapshot(record: RoundTripScenarioRecord.V1, instrument: Instrument) =
    RoundTripScenarioRecord.reconstruct(record, instrument)
  def untypedEvidence(record: OrderScenarioRecord.V1): Any = record.evidence.asInstanceOf[Any]
  // OFFENDING-END
end ScenarioRecordAuthorityEscapesAreUnavailable
