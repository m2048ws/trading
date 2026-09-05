package client

import trading.codec.ScenarioRecord
import trading.economics.instrument.Instrument
import trading.scenario.OrderScenario
import trading.scenario.RoundTripScenario

object ScenarioRecordScopeMismatch:
  def rejected[I <: Instrument, J <: Instrument](instrument: I, foreign: J)(
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
    foreignOrderScenario: OrderScenario[
      foreign.PositionD,
      foreign.BaseD,
      foreign.QuoteD,
      foreign.MarketState
    ],
    foreignRoundTripScenario: RoundTripScenario[
      foreign.PositionD,
      foreign.BaseD,
      foreign.QuoteD,
      foreign.MarketState
    ]
  ): Unit =
    val encoder = ScenarioRecord.encoder(instrument)
    val validOrder = encoder.order(orderScenario)
    val validRoundTrip = encoder.roundTrip(roundTripScenario)

    // OFFENDING-BEGIN
    val wrongOrder = encoder.order(foreignOrderScenario)
    val wrongRoundTrip = encoder.roundTrip(foreignRoundTripScenario)
    // OFFENDING-END

    val _ = (validOrder, validRoundTrip)
end ScenarioRecordScopeMismatch
