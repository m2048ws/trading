package client

import trading.codec.*
import trading.economics.instrument.Instrument
import trading.reference.CatalogSnapshot
import trading.scenario.OrderScenario
import trading.scenario.RoundTripScenario

object ScenarioRecordScopeClient:
  def translate[I <: Instrument](instrument: I, snapshot: CatalogSnapshot)(
    orderScenario: OrderScenario[
      instrument.PositionD,
      instrument.BaseD,
      instrument.QuoteD,
      instrument.MarketState
    ],
    roundTripScenario: RoundTripScenario[
      instrument.PositionD,
      instrument.BaseD,
      instrument.QuoteD,
      instrument.MarketState
    ],
    orderRecord: OrderScenarioRecord.V1,
    roundTripRecord: RoundTripScenarioRecord.V1,
    orderWire: String,
    roundTripWire: String,
    limits: DecodeLimits
  ): Unit =
    val encoder = ScenarioRecord.encoder(instrument)
    val decoder = ScenarioRecord.decoder(instrument, snapshot)

    val projectedOrder: OrderScenarioRecord.V1         = encoder.order(orderScenario)
    val encodedOrder: Either[WireViolations[WireEncodeViolation], String] =
      encoder.encodeOrder(orderScenario)
    val projectedRoundTrip: RoundTripScenarioRecord.V1 = encoder.roundTrip(roundTripScenario)
    val encodedRoundTrip: Either[WireViolations[WireEncodeViolation], String] =
      encoder.encodeRoundTrip(roundTripScenario)

    val reconstructedOrder: Either[
      OrderScenarioReconstructionFailure,
      OrderScenario[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        instrument.MarketState
      ]
    ] = decoder.order(orderRecord)
    val decodedOrder: Either[
      OrderScenarioReconstructionFailure,
      OrderScenario[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        instrument.MarketState
      ]
    ] = decoder.decodeOrder(orderWire, limits, 4)
    val orderBatch: Either[
      WireViolations[IndexedOrderScenarioReconstructionFailure],
      Vector[
        OrderScenario[
          instrument.PositionD,
          instrument.BaseD,
          instrument.QuoteD,
          instrument.MarketState
        ]
      ]
    ] = decoder.orderBatch(Vector(orderWire), limits)

    val reconstructedRoundTrip: Either[
      RoundTripScenarioReconstructionFailure,
      RoundTripScenario[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        instrument.MarketState
      ]
    ] = decoder.roundTrip(roundTripRecord)
    val decodedRoundTrip: Either[
      RoundTripScenarioReconstructionFailure,
      RoundTripScenario[
        instrument.PositionD,
        instrument.BaseD,
        instrument.QuoteD,
        instrument.MarketState
      ]
    ] = decoder.decodeRoundTrip(roundTripWire, limits, 5)
    val roundTripBatch: Either[
      WireViolations[IndexedRoundTripScenarioReconstructionFailure],
      Vector[
        RoundTripScenario[
          instrument.PositionD,
          instrument.BaseD,
          instrument.QuoteD,
          instrument.MarketState
        ]
      ]
    ] = decoder.roundTripBatch(Vector(roundTripWire), limits)

    val parsedOrder       = OrderScenarioRecord.parse(orderWire, limits, 4)
    val encodedRecord     = OrderScenarioRecord.encode(orderRecord)
    val orderSchema       = OrderScenarioRecord.schema()
    val parsedRoundTrip   = RoundTripScenarioRecord.parse(roundTripWire, limits, 5)
    val encodedTripRecord = RoundTripScenarioRecord.encode(roundTripRecord)
    val roundTripSchema   = RoundTripScenarioRecord.schema()

    val _ = (
      projectedOrder,
      encodedOrder,
      projectedRoundTrip,
      encodedRoundTrip,
      reconstructedOrder,
      decodedOrder,
      orderBatch,
      reconstructedRoundTrip,
      decodedRoundTrip,
      roundTripBatch,
      parsedOrder,
      encodedRecord,
      orderSchema,
      parsedRoundTrip,
      encodedTripRecord,
      roundTripSchema
    )
end ScenarioRecordScopeClient
