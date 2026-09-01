package client

import trading.codec.OrderScenarioRecord
import trading.codec.RoundTripScenarioRecord
import trading.codec.DecodeLimits
import trading.economics.instrument.Instrument
import trading.reference.CatalogSnapshot
import trading.scenario.OrderScenario
import trading.scenario.RoundTripScenario

object ScenarioRecordClient:
  def retain(record: OrderScenarioRecord.V1) =
    (record.order, record.activationEvidence, record.pricingResolution, record.slices)

  def reconstruct(record: OrderScenarioRecord.V1, instrument: Instrument, snapshot: CatalogSnapshot) =
    OrderScenarioRecord.reconstruct(record, instrument, snapshot)

  def decode(input: String, instrument: Instrument, snapshot: CatalogSnapshot) =
    OrderScenarioRecord.decodeAndReconstruct(input, instrument, snapshot)

  def decodeBatch(
    inputs: Vector[String],
    instrument: Instrument,
    snapshot: CatalogSnapshot,
    limits: DecodeLimits
  ) = OrderScenarioRecord.reconstructBatch(inputs, instrument, snapshot, limits)

  def encode(
    instrument: Instrument,
    scenario: OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ) = OrderScenarioRecord.encodeScenario(instrument)(scenario)

  def reconstructTrip(record: RoundTripScenarioRecord.V1, instrument: Instrument, snapshot: CatalogSnapshot) =
    RoundTripScenarioRecord.reconstruct(record, instrument, snapshot)

  def decodeTripBatch(
    inputs: Vector[String],
    instrument: Instrument,
    snapshot: CatalogSnapshot,
    limits: DecodeLimits
  ) = RoundTripScenarioRecord.reconstructBatch(inputs, instrument, snapshot, limits)

  def encodeTrip(
    instrument: Instrument,
    scenario: RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ) = RoundTripScenarioRecord.encodeScenario(instrument)(scenario)
end ScenarioRecordClient
