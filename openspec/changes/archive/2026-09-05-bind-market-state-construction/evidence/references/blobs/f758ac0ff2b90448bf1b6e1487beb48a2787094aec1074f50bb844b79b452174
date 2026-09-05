package external.economics.core

import trading.economics.instrument.*
import trading.quantity.*

object MarketStateScopeMismatch:
  def check[I <: Instrument, J <: Instrument](instrument: I, foreign: J)(
    price: instrument.Price,
    foreignPrice: foreign.Price,
    base: Rate[instrument.BaseD, instrument.SettleD],
    quote: Rate[instrument.QuoteD, instrument.SettleD],
    reversedBase: Rate[instrument.SettleD, instrument.BaseD],
    reversedQuote: Rate[instrument.SettleD, instrument.QuoteD],
    foreignBase: Rate[instrument.BaseD, foreign.SettleD],
    foreignQuote: Rate[instrument.QuoteD, foreign.SettleD],
    conversion: SettlementConversion[instrument.SettleD],
    foreignConversion: SettlementConversion[foreign.SettleD]
  ): Unit =
    val markets = MarketState.forInstrument(instrument)
    val valid: Either[MarketStateViolations, instrument.MarketState] =
      markets.fromRates(price, base, quote, additional = Vector(conversion))
    val _ = valid
    // OFFENDING-BEGIN price
    val _ = markets.quoteSettled(foreignPrice, Vector.empty)
    // OFFENDING-END
    // OFFENDING-BEGIN reversed-base
    val _ = markets.fromBaseRate(price, reversedBase, Vector.empty)
    // OFFENDING-END
    // OFFENDING-BEGIN reversed-quote
    val _ = markets.fromQuoteRate(price, reversedQuote, Vector.empty)
    // OFFENDING-END
    // OFFENDING-BEGIN foreign-base
    val _ = markets.fromRates(price, foreignBase, quote, Vector.empty)
    // OFFENDING-END
    // OFFENDING-BEGIN foreign-quote
    val _ = markets.fromRates(price, base, foreignQuote, Vector.empty)
    // OFFENDING-END
    // OFFENDING-BEGIN conversion
    val _ = markets.fromAnchors(price, base.coefficient, quote.coefficient, Vector(foreignConversion))
    // OFFENDING-END
end MarketStateScopeMismatch
