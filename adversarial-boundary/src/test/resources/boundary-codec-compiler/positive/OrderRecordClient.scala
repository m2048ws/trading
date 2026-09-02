package client

import trading.codec.DecodeLimits
import trading.codec.OrderRecord
import trading.codec.OrderReconstructionFailure
import trading.codec.WireViolations
import trading.economics.instrument.Instrument
import trading.order.Order

object OrderRecordClient:
  def retain(record: OrderRecord.V1): (String, BigInt, OrderRecord.Activation, OrderRecord.Execution) =
    (record.instrumentId.value, record.lotCoordinate, record.activation, record.execution)

  def encode(order: Order[?, ?, ?]) = OrderRecord.encodeOrder(order)

  def decode(input: String, instrument: Instrument): Either[OrderReconstructionFailure, Order[?, ?, ?]] =
    OrderRecord.decodeAndReconstruct(input, instrument)

  def batch(input: Vector[String], instrument: Instrument) =
    OrderRecord.reconstructBatch(input, instrument, DecodeLimits.default)

  def project(order: Order[?, ?, ?]): (String, BigInt) =
    order.instrumentId.value -> order.intent.positionChange.coordinate
end OrderRecordClient
