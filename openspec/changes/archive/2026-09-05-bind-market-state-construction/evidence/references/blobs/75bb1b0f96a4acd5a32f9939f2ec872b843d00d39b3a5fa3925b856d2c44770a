package trading.economics.instrument

import munit.FunSuite

import trading.quantity.*
import trading.reference.*

/** Baseline observations are specified independently of the bound/direct comparison. */
class MarketStateInstrumentScopeSuite extends FunSuite:
  private val fixture = new InstrumentFixtures

  private def direct(
    instrument: Instrument
  )(
    price: instrument.Price,
    base: Rational,
    quote: Rational,
    additional: Vector[SettlementConversion[instrument.SettleD]]
  ): Vector[Either[MarketStateViolations, instrument.MarketState]] =
    val baseRate  = Rate(instrument.roles.base.dimension.ref, instrument.roles.settle.dimension.ref, base)
    val quoteRate = Rate(instrument.roles.quote.dimension.ref, instrument.roles.settle.dimension.ref, quote)
    Vector(
      MarketState.quoteSettled(instrument)(price, additional),
      MarketState.baseSettled(instrument)(price, additional),
      MarketState.fromQuoteAnchor(instrument)(price, quote, additional),
      MarketState.fromBaseAnchor(instrument)(price, base, additional),
      MarketState.fromAnchors(instrument)(price, base, quote, additional),
      MarketState.fromQuoteRate(instrument)(price, quoteRate, additional),
      MarketState.fromBaseRate(instrument)(price, baseRate, additional),
      MarketState.fromRates(instrument)(price, baseRate, quoteRate, additional)
    )

  private def omitted(
    instrument: Instrument
  )(
    price: instrument.Price,
    base: Rational,
    quote: Rational
  ): Vector[Either[MarketStateViolations, instrument.MarketState]] =
    val baseRate  = Rate(instrument.roles.base.dimension.ref, instrument.roles.settle.dimension.ref, base)
    val quoteRate = Rate(instrument.roles.quote.dimension.ref, instrument.roles.settle.dimension.ref, quote)
    Vector(
      MarketState.quoteSettled(instrument)(price),
      MarketState.baseSettled(instrument)(price),
      MarketState.fromQuoteAnchor(instrument)(price, quote),
      MarketState.fromBaseAnchor(instrument)(price, base),
      MarketState.fromAnchors(instrument)(price, base, quote),
      MarketState.fromQuoteRate(instrument)(price, quoteRate),
      MarketState.fromBaseRate(instrument)(price, baseRate),
      MarketState.fromRates(instrument)(price, baseRate, quoteRate)
    )

  private def scoped(
    instrument: Instrument
  )(
    price: instrument.Price,
    base: Rational,
    quote: Rational,
    additional: Vector[SettlementConversion[instrument.SettleD]]
  ): Vector[Either[MarketStateViolations, instrument.MarketState]] =
    val markets                    = MarketState.forInstrument(instrument)
    val baseRate: markets.BaseRate =
      Rate(instrument.roles.base.dimension.ref, instrument.roles.settle.dimension.ref, base)
    val quoteRate: markets.QuoteRate =
      Rate(instrument.roles.quote.dimension.ref, instrument.roles.settle.dimension.ref, quote)
    val retainedPrice: markets.Price                               = price
    val results: Vector[Either[markets.Violations, markets.State]] = Vector(
      markets.quoteSettled(retainedPrice, additional),
      markets.baseSettled(retainedPrice, additional),
      markets.fromQuoteAnchor(retainedPrice, quote, additional),
      markets.fromBaseAnchor(retainedPrice, base, additional),
      markets.fromAnchors(retainedPrice, base, quote, additional),
      markets.fromQuoteRate(retainedPrice, quoteRate, additional),
      markets.fromBaseRate(retainedPrice, baseRate, additional),
      markets.fromRates(retainedPrice, baseRate, quoteRate, additional)
    )
    results
  end scoped

  private def scopedOmitted(
    instrument: Instrument
  )(
    price: instrument.Price,
    base: Rational,
    quote: Rational
  ): Vector[Either[MarketStateViolations, instrument.MarketState]] =
    val markets                    = MarketState.forInstrument(instrument)
    val baseRate: markets.BaseRate =
      Rate(instrument.roles.base.dimension.ref, instrument.roles.settle.dimension.ref, base)
    val quoteRate: markets.QuoteRate =
      Rate(instrument.roles.quote.dimension.ref, instrument.roles.settle.dimension.ref, quote)
    val retainedPrice: markets.Price                               = price
    val results: Vector[Either[markets.Violations, markets.State]] = Vector(
      markets.quoteSettled(retainedPrice),
      markets.baseSettled(retainedPrice),
      markets.fromQuoteAnchor(retainedPrice, quote),
      markets.fromBaseAnchor(retainedPrice, base),
      markets.fromAnchors(retainedPrice, base, quote),
      markets.fromQuoteRate(retainedPrice, quoteRate),
      markets.fromBaseRate(retainedPrice, baseRate),
      markets.fromRates(retainedPrice, baseRate, quoteRate)
    )
    results
  end scopedOmitted

  private def same(
    instrument: Instrument
  )(
    left: Either[MarketStateViolations, instrument.MarketState],
    right: Either[MarketStateViolations, instrument.MarketState]
  ): Unit =
    (left, right) match
      case (Left(a), Left(b))   => assertEquals(a.violations, b.violations)
      case (Right(a), Right(b)) =>
        assertEquals(a.instrumentId, b.instrumentId)
        assert(Asset.reconcile(a.settlement, b.settlement).isRight)
        assertEquals(a.price, b.price)
        assertEquals(a.baseToSettle, b.baseToSettle)
        assertEquals(a.quoteToSettle, b.quoteToSettle)
        assertEquals(a.conversionSources, b.conversionSources)
        assertEquals(a.additionalConversions, b.additionalConversions)
        Vector(instrument.roles.base, instrument.roles.quote, instrument.roles.settle, fixture.token).foreach: source =>
          val quantity = Quantity(source.dimension.ref, Rational(7, 3))
          assertEquals(a.convertToSettle(source)(quantity), b.convertToSettle(source)(quantity))
        val position = fixture.position(instrument, 1000)
        assertEquals(Valuation.positionValue(instrument)(position, a), Valuation.positionValue(instrument)(position, b))
      case _ => fail(s"different results: $left versus $right")

  private def expected(
    instrument: Instrument
  )(
    results: Vector[Either[MarketStateViolations, instrument.MarketState]],
    base: Rational,
    quote: Rational,
    additional: Vector[SettlementConversion[instrument.SettleD]]
  ): Unit =
    assertEquals(results.size, 8)
    results.zipWithIndex.foreach: (result, mode) =>
      val unsupported =
        if mode == 0 && instrument.roles.quote.id != instrument.roles.settle.id then
          Some(instrument.roles.quote.id -> ConversionFailureReason.SettleIsNotQuote)
        else if mode == 1 && instrument.roles.base.id != instrument.roles.settle.id then
          Some(instrument.roles.base.id -> ConversionFailureReason.SettleIsNotBase)
        else None
      unsupported match
        case Some((source, reason)) =>
          assertEquals(result.left.toOption.get.violations,
            Vector(
              MarketStateViolation.InvalidConversion(source, instrument.roles.settle.id, Rational.one, reason)
            ))
        case None =>
          val state = result.toOption.get
          assertEquals(state.instrumentId, instrument.identity.id)
          assert(state.settlement.eq(instrument.roles.settle))
          assertEquals(state.price.coefficient, Rational(100))
          assertEquals(state.price.ticks.unrefined, BigInt(200))
          assertEquals(state.price.grid.identity, instrument.priceGrid.identity)
          assertEquals(state.baseToSettle.coefficient, base)
          assertEquals(state.quoteToSettle.coefficient, quote)
          assertEquals(state.baseToSettle,
            Rate(instrument.roles.base.dimension.ref, instrument.roles.settle.dimension.ref, base))
          assertEquals(state.quoteToSettle,
            Rate(instrument.roles.quote.dimension.ref, instrument.roles.settle.dimension.ref, quote))
          assertEquals(state.conversionSources,
            Vector(instrument.roles.base.id, instrument.roles.quote.id, instrument.roles.settle.id).distinct ++
              additional.map(_.source.id))
          assertEquals(state.additionalConversions, additional)
          Vector(instrument.roles.base -> base, instrument.roles.quote -> quote,
            instrument.roles.settle    -> Rational.one).foreach: (source, coefficient) =>
            assertEquals(
              state.convertToSettle(source)(Quantity(source.dimension.ref, Rational(7, 3))).map(_.coefficient),
              Right(coefficient * Rational(7, 3))
            )
          assertEquals(
            state.convertToSettle(fixture.token)(Quantity(fixture.token.dimension.ref, Rational(7, 3))).map(
              _.coefficient
            ),
            if additional.isEmpty then Left(MissingConversion(fixture.token.id)) else Right(Rational(14, 3))
          )
      end match
  end expected

  Vector((fixture.linear, Rational(100), Rational.one), (fixture.inverse, Rational.one, Rational(1, 100)),
    (fixture.quanto, Rational(90), Rational(9, 10))).foreach: (instrument, base, quote) =>
    test(s"characterized eight modes and empty defaults: ${instrument.identity.id}"):
      val price = fixture.price(instrument, Rational(100))
      val empty = direct(instrument)(price, base, quote, Vector.empty)
      expected(instrument)(empty, base, quote, Vector.empty)
      omitted(instrument)(price, base, quote).zip(empty).foreach((a, b) => same(instrument)(a, b))
      scopedOmitted(instrument)(price, base, quote).zip(empty).foreach((a, b) => same(instrument)(a, b))
      scoped(instrument)(price, base, quote, Vector.empty).zip(empty).foreach((a, b) => same(instrument)(a, b))
      val conversion = SettlementConversion.exact(instrument)(fixture.token)(Rational(2)).toOption.get
      val additional = Vector(conversion)
      expected(instrument)(direct(instrument)(price, base, quote, additional), base, quote, additional)
      val bound = scoped(instrument)(price, base, quote, additional)
      expected(instrument)(bound, base, quote, additional)
      bound.zip(direct(instrument)(price, base, quote, additional)).foreach((a, b) => same(instrument)(a, b))

  test("characterized anchor failures are complete, ordered, and coherence depends on valid anchors"):
    val instrument = fixture.quanto
    val price      = fixture.price(instrument, Rational(100))
    val token      = SettlementConversion.exact(instrument)(fixture.token)(Rational(2)).toOption.get
    val generated  = SettlementConversion.exact(instrument)(instrument.roles.base)(Rational(90)).toOption.get
    val additional = Vector(token, token, generated)
    val invalid    = MarketState.fromAnchors(instrument)(price, Rational(-1), Rational.zero, additional)
    val violations = Vector(
      MarketStateViolation.InvalidConversion(instrument.roles.base.id, instrument.roles.settle.id, Rational(-1),
        ConversionFailureReason.NonPositive),
      MarketStateViolation.InvalidConversion(instrument.roles.quote.id, instrument.roles.settle.id, Rational.zero,
        ConversionFailureReason.NonPositive),
      MarketStateViolation.DuplicateSource(fixture.token.id),
      MarketStateViolation.DuplicateSource(instrument.roles.base.id)
    )
    assertEquals(invalid.left.toOption.get.violations, violations)
    same(instrument)(MarketState.forInstrument(instrument).fromAnchors(price, Rational(-1), Rational.zero, additional),
      invalid)
    scoped(instrument)(price, Rational(-1), Rational.zero, additional)
      .zip(direct(instrument)(price, Rational(-1), Rational.zero, additional)).foreach((a, b) => same(instrument)(a, b))
    assertEquals(MarketState.firstError(invalid), Left(violations.head))
    val incoherent = MarketState.fromAnchors(instrument)(price, Rational(89), Rational(9, 10), additional)
    same(instrument)(
      MarketState.forInstrument(instrument).fromAnchors(price, Rational(89), Rational(9, 10), additional),
      incoherent
    )
    assertEquals(
      incoherent.left.toOption.get.violations,
      Vector(MarketStateViolation.IncoherentAnchors(Rational(100), Rational(89), Rational(9, 10))) ++ violations.drop(2)
    )
    val linear = fixture.linear
    assertEquals(
      MarketState.fromAnchors(linear)(fixture.price(linear, Rational(100)), Rational(200),
        Rational(2)).left.toOption.get.violations,
      Vector(MarketStateViolation.InvalidConversion(linear.roles.quote.id, linear.roles.settle.id, Rational(2),
        ConversionFailureReason.IdentityNotOne))
    )

  test("characterized price boundary and conversion lineage remain checked"):
    val instrument = fixture.linear
    assert(Price.exact(instrument)(Rational(5, 4)).isLeft)
    assertEquals(Price.exact(instrument)(Rational.zero), Left(InvalidPriceCoordinate(0)))
    assertEquals(
      SettlementConversion.exact(instrument)(fixture.foreignUsd)(Rational.one),
      Left(MarketStateViolation.ReferenceData("conversion.source",
        ForeignLineage(fixture.foreignUsd.dimension.key, instrument.roles.settle.dimension.key)))
    )

  test("characterized prerequisites retain their null timing"):
    val instrument = fixture.quanto
    // Unsupported settled modes reject their context before observing a price.
    assert(MarketState.quoteSettled(instrument)(null).isLeft)
    assert(MarketState.baseSettled(instrument)(null).isLeft)
    val _ = intercept[NullPointerException](MarketState.fromQuoteAnchor(instrument)(null, Rational.one))
    val _ = intercept[NullPointerException](MarketState.fromBaseAnchor(instrument)(null, Rational.one))
    val _ = intercept[NullPointerException](MarketState.fromAnchors(instrument)(null, Rational.one, Rational.one))

  // Test-only transport: phantom endpoints are aligned only after authoritative dimension equality.
  // This deliberately preserves the foreign identity/lineage instead of rebuilding a local value.
  private def compatiblePrice(instrument: Instrument)(source: Instrument)(value: source.Price): instrument.Price =
    assert(DimensionHandle.reconcile(source.roles.base.dimension, instrument.roles.base.dimension).isRight)
    assert(DimensionHandle.reconcile(source.roles.quote.dimension, instrument.roles.quote.dimension).isRight)
    value.asInstanceOf[instrument.Price]

  private def compatibleConversion(
    instrument: Instrument
  )(
    value: SettlementConversion[? <: Dim]
  ): SettlementConversion[instrument.SettleD] =
    assert(SameDimension.between(value.target.dimension.ref, instrument.roles.settle.dimension.ref).nonEmpty)
    value.asInstanceOf[SettlementConversion[instrument.SettleD]]

  test("characterized compatible foreign identities precede anchor and duplicate errors"):
    val instrument = fixture.linear
    val foreign    = fixture.foreignIdentity
    val price      = compatiblePrice(instrument)(foreign)(fixture.price(foreign, Rational(100)))
    val conversion =
      compatibleConversion(instrument)(SettlementConversion.exact(foreign)(fixture.token)(Rational(2)).toOption.get)
    val result =
      MarketState.fromAnchors(instrument)(price, Rational.zero, Rational.zero, Vector(conversion, conversion))
    scoped(instrument)(price, Rational.zero, Rational.zero, Vector(conversion, conversion))
      .zip(direct(instrument)(price, Rational.zero, Rational.zero, Vector(conversion, conversion)))
      .foreach((a, b) => same(instrument)(a, b))
    assertEquals(
      result.left.toOption.get.violations,
      Vector(
        MarketStateViolation.InstrumentMismatch("price", instrument.identity.id, foreign.identity.id),
        MarketStateViolation.InstrumentMismatch("additional[0]", instrument.identity.id, foreign.identity.id),
        MarketStateViolation.InstrumentMismatch("additional[1]", instrument.identity.id, foreign.identity.id),
        MarketStateViolation.InvalidConversion(instrument.roles.base.id, instrument.roles.settle.id, Rational.zero,
          ConversionFailureReason.NonPositive),
        MarketStateViolation.InvalidConversion(instrument.roles.quote.id, instrument.roles.settle.id, Rational.zero,
          ConversionFailureReason.NonPositive),
        MarketStateViolation.DuplicateSource(fixture.token.id)
      )
    )

  test("characterized foreign conversion lineage retains indexed target and source failures"):
    val instrument = fixture.linear
    val other      = new InstrumentFixtures
    val conversion =
      compatibleConversion(instrument)(SettlementConversion.exact(other.linear)(other.token)(Rational(2)).toOption.get)
    val result = MarketState.quoteSettled(instrument)(fixture.price(instrument, Rational(100)), Vector(conversion))
    same(instrument)(
      MarketState.forInstrument(instrument).quoteSettled(fixture.price(instrument, Rational(100)), Vector(conversion)),
      result
    )
    assertEquals(
      result.left.toOption.get.violations,
      Vector(
        MarketStateViolation.ReferenceData("additional[0].target",
          ForeignLineage(conversion.target.dimension.key, instrument.roles.settle.dimension.key)),
        MarketStateViolation.ReferenceData("additional[0].source",
          ForeignLineage(conversion.source.dimension.key, instrument.roles.settle.dimension.key))
      )
    )
  test("a field-only scope defers null observation to the same operation"):
    val absent: Instrument = null
    val context            = MarketState.forInstrument(absent)
    assert(context.instrument == null)
    val _          = intercept[NullPointerException](context.quoteSettled(null))
    val instrument = fixture.quanto
    val scope      = MarketState.forInstrument(instrument)
    same(instrument)(scope.quoteSettled(null), MarketState.quoteSettled(instrument)(null))
    same(instrument)(scope.baseSettled(null), MarketState.baseSettled(instrument)(null))
    val _ = intercept[NullPointerException](scope.fromQuoteAnchor(null, Rational.one))
    val _ = intercept[NullPointerException](scope.fromBaseAnchor(null, Rational.one))
    val _ = intercept[NullPointerException](scope.fromAnchors(null, Rational.one, Rational.one))

  test("one scope constructs independent states and is safe for concurrent reuse"):
    import java.util.concurrent.Callable
    import java.util.concurrent.Executors
    import java.util.concurrent.TimeUnit
    val instrument = fixture.quanto
    val scope      = MarketState.forInstrument(instrument)
    val price      = fixture.price(instrument, Rational(100))
    val first      = scope.fromQuoteAnchor(price, Rational(9, 10)).toOption.get
    val second     = scope.fromQuoteAnchor(price, Rational(9, 10)).toOption.get
    assert(!first.eq(second))
    assert(scope.instrument.eq(instrument))
    val pool = Executors.newFixedThreadPool(4)
    try
      val futures = 1.to(32).map: n =>
        pool.submit(new Callable[instrument.MarketState]:
          def call(): instrument.MarketState =
            scope.fromQuoteAnchor(price, Rational(n, 37)).toOption.get
        )
      futures.zipWithIndex.foreach: (future, index) =>
        val state = future.get(10, TimeUnit.SECONDS)
        same(instrument)(Right(state), MarketState.fromQuoteAnchor(instrument)(price, Rational(index + 1, 37)))
      assertEquals(first.baseToSettle.coefficient, Rational(90))
      assertEquals(second.quoteToSettle.coefficient, Rational(9, 10))
    finally
      val _ = pool.shutdownNow()
end MarketStateInstrumentScopeSuite
