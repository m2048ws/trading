package external.economics.core

import trading.economics.instrument.*
import trading.quantity.*

object MarketStateScopeClient:
  def construct[I <: Instrument](instrument: I)(
    price: instrument.Price,
    base: Rate[instrument.BaseD, instrument.SettleD],
    quote: Rate[instrument.QuoteD, instrument.SettleD],
    conversion: SettlementConversion[instrument.SettleD]
  ): Vector[Either[MarketStateViolations, instrument.MarketState]] =
    val markets = MarketState.forInstrument(instrument)
    val retainedPrice: markets.Price = price
    val retainedBase: markets.BaseRate = base
    val retainedQuote: markets.QuoteRate = quote
    val retainedConversion: markets.Conversion = conversion
    val results: Vector[Either[markets.Violations, markets.State]] = Vector(
      markets.quoteSettled(retainedPrice),
      markets.quoteSettled(price = retainedPrice),
      markets.quoteSettled(retainedPrice, Vector.empty),
      markets.quoteSettled(price = retainedPrice, additional = Vector.empty),
      markets.quoteSettled(retainedPrice, Vector(retainedConversion)),
      markets.quoteSettled(price = retainedPrice, additional = Vector(retainedConversion)),
      MarketState.quoteSettled(instrument)(retainedPrice),
      MarketState.quoteSettled(instrument)(price = retainedPrice),
      MarketState.quoteSettled(instrument)(retainedPrice, Vector.empty),
      MarketState.quoteSettled(instrument)(price = retainedPrice, additional = Vector.empty),
      MarketState.quoteSettled(instrument)(retainedPrice, Vector(retainedConversion)),
      MarketState.quoteSettled(instrument)(price = retainedPrice, additional = Vector(retainedConversion)),
      markets.baseSettled(retainedPrice),
      markets.baseSettled(price = retainedPrice),
      markets.baseSettled(retainedPrice, Vector.empty),
      markets.baseSettled(price = retainedPrice, additional = Vector.empty),
      markets.baseSettled(retainedPrice, Vector(retainedConversion)),
      markets.baseSettled(price = retainedPrice, additional = Vector(retainedConversion)),
      MarketState.baseSettled(instrument)(retainedPrice),
      MarketState.baseSettled(instrument)(price = retainedPrice),
      MarketState.baseSettled(instrument)(retainedPrice, Vector.empty),
      MarketState.baseSettled(instrument)(price = retainedPrice, additional = Vector.empty),
      MarketState.baseSettled(instrument)(retainedPrice, Vector(retainedConversion)),
      MarketState.baseSettled(instrument)(price = retainedPrice, additional = Vector(retainedConversion)),
      markets.fromQuoteAnchor(retainedPrice, quote.coefficient),
      markets.fromQuoteAnchor(price = retainedPrice, quoteToSettle = quote.coefficient),
      markets.fromQuoteAnchor(retainedPrice, quote.coefficient, Vector.empty),
      markets.fromQuoteAnchor(price = retainedPrice, quoteToSettle = quote.coefficient, additional = Vector.empty),
      markets.fromQuoteAnchor(retainedPrice, quote.coefficient, Vector(retainedConversion)),
      markets.fromQuoteAnchor(price = retainedPrice, quoteToSettle = quote.coefficient, additional = Vector(retainedConversion)),
      MarketState.fromQuoteAnchor(instrument)(retainedPrice, quote.coefficient),
      MarketState.fromQuoteAnchor(instrument)(price = retainedPrice, quoteToSettle = quote.coefficient),
      MarketState.fromQuoteAnchor(instrument)(retainedPrice, quote.coefficient, Vector.empty),
      MarketState.fromQuoteAnchor(instrument)(price = retainedPrice, quoteToSettle = quote.coefficient, additional = Vector.empty),
      MarketState.fromQuoteAnchor(instrument)(retainedPrice, quote.coefficient, Vector(retainedConversion)),
      MarketState.fromQuoteAnchor(instrument)(price = retainedPrice, quoteToSettle = quote.coefficient, additional = Vector(retainedConversion)),
      markets.fromBaseAnchor(retainedPrice, base.coefficient),
      markets.fromBaseAnchor(price = retainedPrice, baseToSettle = base.coefficient),
      markets.fromBaseAnchor(retainedPrice, base.coefficient, Vector.empty),
      markets.fromBaseAnchor(price = retainedPrice, baseToSettle = base.coefficient, additional = Vector.empty),
      markets.fromBaseAnchor(retainedPrice, base.coefficient, Vector(retainedConversion)),
      markets.fromBaseAnchor(price = retainedPrice, baseToSettle = base.coefficient, additional = Vector(retainedConversion)),
      MarketState.fromBaseAnchor(instrument)(retainedPrice, base.coefficient),
      MarketState.fromBaseAnchor(instrument)(price = retainedPrice, baseToSettle = base.coefficient),
      MarketState.fromBaseAnchor(instrument)(retainedPrice, base.coefficient, Vector.empty),
      MarketState.fromBaseAnchor(instrument)(price = retainedPrice, baseToSettle = base.coefficient, additional = Vector.empty),
      MarketState.fromBaseAnchor(instrument)(retainedPrice, base.coefficient, Vector(retainedConversion)),
      MarketState.fromBaseAnchor(instrument)(price = retainedPrice, baseToSettle = base.coefficient, additional = Vector(retainedConversion)),
      markets.fromAnchors(retainedPrice, base.coefficient, quote.coefficient),
      markets.fromAnchors(price = retainedPrice, baseToSettle = base.coefficient, quoteToSettle = quote.coefficient),
      markets.fromAnchors(retainedPrice, base.coefficient, quote.coefficient, Vector.empty),
      markets.fromAnchors(price = retainedPrice, baseToSettle = base.coefficient, quoteToSettle = quote.coefficient, additional = Vector.empty),
      markets.fromAnchors(retainedPrice, base.coefficient, quote.coefficient, Vector(retainedConversion)),
      markets.fromAnchors(price = retainedPrice, baseToSettle = base.coefficient, quoteToSettle = quote.coefficient, additional = Vector(retainedConversion)),
      MarketState.fromAnchors(instrument)(retainedPrice, base.coefficient, quote.coefficient),
      MarketState.fromAnchors(instrument)(price = retainedPrice, baseToSettle = base.coefficient, quoteToSettle = quote.coefficient),
      MarketState.fromAnchors(instrument)(retainedPrice, base.coefficient, quote.coefficient, Vector.empty),
      MarketState.fromAnchors(instrument)(price = retainedPrice, baseToSettle = base.coefficient, quoteToSettle = quote.coefficient, additional = Vector.empty),
      MarketState.fromAnchors(instrument)(retainedPrice, base.coefficient, quote.coefficient, Vector(retainedConversion)),
      MarketState.fromAnchors(instrument)(price = retainedPrice, baseToSettle = base.coefficient, quoteToSettle = quote.coefficient, additional = Vector(retainedConversion)),
      markets.fromQuoteRate(retainedPrice, retainedQuote),
      markets.fromQuoteRate(price = retainedPrice, quoteToSettle = retainedQuote),
      markets.fromQuoteRate(retainedPrice, retainedQuote, Vector.empty),
      markets.fromQuoteRate(price = retainedPrice, quoteToSettle = retainedQuote, additional = Vector.empty),
      markets.fromQuoteRate(retainedPrice, retainedQuote, Vector(retainedConversion)),
      markets.fromQuoteRate(price = retainedPrice, quoteToSettle = retainedQuote, additional = Vector(retainedConversion)),
      MarketState.fromQuoteRate(instrument)(retainedPrice, retainedQuote),
      MarketState.fromQuoteRate(instrument)(price = retainedPrice, quoteToSettle = retainedQuote),
      MarketState.fromQuoteRate(instrument)(retainedPrice, retainedQuote, Vector.empty),
      MarketState.fromQuoteRate(instrument)(price = retainedPrice, quoteToSettle = retainedQuote, additional = Vector.empty),
      MarketState.fromQuoteRate(instrument)(retainedPrice, retainedQuote, Vector(retainedConversion)),
      MarketState.fromQuoteRate(instrument)(price = retainedPrice, quoteToSettle = retainedQuote, additional = Vector(retainedConversion)),
      markets.fromBaseRate(retainedPrice, retainedBase),
      markets.fromBaseRate(price = retainedPrice, baseToSettle = retainedBase),
      markets.fromBaseRate(retainedPrice, retainedBase, Vector.empty),
      markets.fromBaseRate(price = retainedPrice, baseToSettle = retainedBase, additional = Vector.empty),
      markets.fromBaseRate(retainedPrice, retainedBase, Vector(retainedConversion)),
      markets.fromBaseRate(price = retainedPrice, baseToSettle = retainedBase, additional = Vector(retainedConversion)),
      MarketState.fromBaseRate(instrument)(retainedPrice, retainedBase),
      MarketState.fromBaseRate(instrument)(price = retainedPrice, baseToSettle = retainedBase),
      MarketState.fromBaseRate(instrument)(retainedPrice, retainedBase, Vector.empty),
      MarketState.fromBaseRate(instrument)(price = retainedPrice, baseToSettle = retainedBase, additional = Vector.empty),
      MarketState.fromBaseRate(instrument)(retainedPrice, retainedBase, Vector(retainedConversion)),
      MarketState.fromBaseRate(instrument)(price = retainedPrice, baseToSettle = retainedBase, additional = Vector(retainedConversion)),
      markets.fromRates(retainedPrice, retainedBase, retainedQuote),
      markets.fromRates(price = retainedPrice, baseToSettle = retainedBase, quoteToSettle = retainedQuote),
      markets.fromRates(retainedPrice, retainedBase, retainedQuote, Vector.empty),
      markets.fromRates(price = retainedPrice, baseToSettle = retainedBase, quoteToSettle = retainedQuote, additional = Vector.empty),
      markets.fromRates(retainedPrice, retainedBase, retainedQuote, Vector(retainedConversion)),
      markets.fromRates(price = retainedPrice, baseToSettle = retainedBase, quoteToSettle = retainedQuote, additional = Vector(retainedConversion)),
      MarketState.fromRates(instrument)(retainedPrice, retainedBase, retainedQuote),
      MarketState.fromRates(instrument)(price = retainedPrice, baseToSettle = retainedBase, quoteToSettle = retainedQuote),
      MarketState.fromRates(instrument)(retainedPrice, retainedBase, retainedQuote, Vector.empty),
      MarketState.fromRates(instrument)(price = retainedPrice, baseToSettle = retainedBase, quoteToSettle = retainedQuote, additional = Vector.empty),
      MarketState.fromRates(instrument)(retainedPrice, retainedBase, retainedQuote, Vector(retainedConversion)),
      MarketState.fromRates(instrument)(price = retainedPrice, baseToSettle = retainedBase, quoteToSettle = retainedQuote, additional = Vector(retainedConversion))
    )
    results

  def run(): Unit =
    val instrument = PureCoreClient.instrument
    val makePrice: Rational => Either[PriceError, instrument.Price] = Price.exact(instrument)
    val price = makePrice(Rational(100)).toOption.get
    assert(makePrice(Rational(101)).toOption.get.coefficient == Rational(101))
    val base = Rate(instrument.roles.base.dimension.ref, instrument.roles.settle.dimension.ref, Rational(100))
    val quote = Rate(instrument.roles.quote.dimension.ref, instrument.roles.settle.dimension.ref, Rational.one)
    // SettlementConversion remains independently owned, including its typed-rate constructor.
    val source = instrument.roles.position
    val conversion = SettlementConversion.exact(instrument)(source)(Rational(2)).toOption.get
    val typed = SettlementConversion.fromRate(instrument)(source)(
      Rate(source.dimension.ref, instrument.roles.settle.dimension.ref, Rational(2))
    ).toOption.get
    assert(typed.coefficient == conversion.coefficient)
    val results: Vector[Either[MarketStateViolations, instrument.MarketState]] =
      construct(instrument)(price, base, quote, conversion)
    assert(results.size == 96)
    results.zipWithIndex.foreach: (result, index) =>
      if index / 12 == 1 then
        assert(result.left.toOption.get.violations == Vector(
          MarketStateViolation.InvalidConversion(instrument.roles.base.id, instrument.roles.settle.id,
            Rational.one, ConversionFailureReason.SettleIsNotBase)
        ))
      else
        val state = result.toOption.get
        assert(state.price == price)
        assert(state.baseToSettle == base)
        assert(state.quoteToSettle == quote)
        assert(state.additionalConversions == (if index % 6 >= 4 then Vector(conversion) else Vector.empty))
        assert(state.instrumentId == instrument.identity.id)
end MarketStateScopeClient
