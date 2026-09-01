package client

import trading.codec.OrderRecord
import trading.economics.instrument.Instrument
import trading.order.Order
import trading.reference.CatalogRoot
import trading.reference.CatalogSnapshot

object OrderRecordAuthorityEscapesAreUnavailable:
  def prelude(record: OrderRecord.V1, wire: String, instrument: Instrument, order: Order[?, ?, ?]): Unit =
    val stable = OrderRecord.encode(record)
    val rebuilt = OrderRecord.decodeAndReconstruct(wire, instrument)
    val encoded = OrderRecord.encodeOrder(order)
    val _ = (stable, rebuilt, encoded)

  // OFFENDING-BEGIN
  def encodeTrusted(instrument: Instrument) = OrderRecord.encode(instrument)
  def reconstructFromRoot(record: OrderRecord.V1, root: CatalogRoot) = OrderRecord.reconstruct(record, root)
  def reconstructFromSnapshot(record: OrderRecord.V1, snapshot: CatalogSnapshot) =
    OrderRecord.reconstruct(record, snapshot)
  def decodeAsAny(wire: String) = OrderRecord.decode[Any](wire)
  def decodeKindOptions(wire: String) = OrderRecord.decode(wire, "market", Map("duration" -> None))
  def positionChange(record: OrderRecord.V1) = record.positionChange
  def componentInstrumentIds(record: OrderRecord.V1) = record.componentInstrumentIds
  def scenarios(record: OrderRecord.V1) = record.scenarios
  def venueLifecycle(record: OrderRecord.V1) = record.venueLifecycle
  def fills(record: OrderRecord.V1) = record.fills
  def reportedFees(record: OrderRecord.V1) = record.reportedFees
  def accountState(record: OrderRecord.V1) = record.accountState
  def catalogRevision(record: OrderRecord.V1) = record.catalogRevision
  def lineage(record: OrderRecord.V1) = record.lineage
  def snapshot(record: OrderRecord.V1) = record.snapshot
  def lotGridHandle(record: OrderRecord.V1) = record.lotGridHandle
  def priceGridHandle(record: OrderRecord.V1) = record.priceGridHandle
  // OFFENDING-END
end OrderRecordAuthorityEscapesAreUnavailable
