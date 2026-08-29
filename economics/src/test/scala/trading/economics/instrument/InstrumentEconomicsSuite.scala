package trading.economics.instrument

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.reference.*

class InstrumentEconomicsSuite extends FunSuite:
  private val fixture    = new EconomicsFixtures
  private val instrument = fixture.linear

  test("owned lots and signed positions retain refined value-local observations"):
    val lots  = instrument.lots(1000).toOption.get
    val long  = instrument.positionLots(Side.Buy, lots).toOption.get
    val short = instrument.positionLots(Side.Sell, lots).toOption.get

    assertEquals(lots.count.unrefined, BigInt(1000))
    assertEquals(lots.quantity.coefficient, Rational.one)
    assertEquals(long.count, BigInt(1000))
    assertEquals(short.count, BigInt(-1000))
    assertEquals(instrument.flatPosition.count, BigInt(0))
    assertEquals(instrument.lots(0), Left(InvalidLots(0)))
    assertEquals(instrument.lots(-1), Left(InvalidLots(-1)))

  test("exact and quantized price construction preserve positivity, grid membership, and residual"):
    val exact = instrument.prices.exact(Rational(3, 2)).toOption.get
    assertEquals(exact.ticks.unrefined, BigInt(3))
    assertEquals(exact.coefficient, Rational(3, 2))
    assertEquals(exact.rate.coefficient, Rational(3, 2))
    assert(instrument.prices.exact(Rational(5, 4)).isLeft)
    assertEquals(instrument.prices.exact(Rational.zero), Left(InvalidPriceCoordinate(0)))

    val (selected, residual) = instrument.prices.quantize(Rational(5, 4), QuantizationPolicy.Floor).toOption.get
    assertEquals(selected.ticks.unrefined, BigInt(2))
    assertEquals(selected.coefficient + residual.coefficient, Rational(5, 4))

    val typed = Rate(
      instrument.roles.base.dimension.ref,
      instrument.roles.quote.dimension.ref,
      Rational(3, 2)
    )
    assertEquals(instrument.prices.fromRate(typed).map(_.ticks.unrefined), Right(BigInt(3)))
    assertEquals(instrument.prices.fromTicks(PositiveWhole(3).toOption.get).coefficient, Rational(3, 2))

  test("stable definition identities and non-empty diagnostics are guarded"):
    assertEquals(InstrumentId.from("  "), Left(EmptyInstrumentId))
    assertEquals(UnderlyingId.from(""), Left(EmptyUnderlyingId))
    assertEquals(InstrumentAssemblyErrors.from(Vector.empty), Left(EmptyInstrumentAssemblyErrors))

    val raw = validDefinition("stable-command")
    assertEquals(raw.roles.base, fixture.btc.id)
    assertEquals(raw.listing.positionLotGrid, fixture.contractLots.identity)
    assertEquals(raw.payoff.basePerPosition, Rational.one)

  test("assembly succeeds exactly and total instrument construction preserves every trusted component"):
    val raw   = validDefinition("assembled")
    val spec  = InstrumentAssembler.assemble(raw, fixture.snapshot).toOption.get
    val built = Instrument.fromSpec(spec)

    assertEquals(spec.identity, raw.identity)
    assert(spec.roles.base.eq(fixture.btc))
    assert(spec.roles.quote.eq(fixture.usd))
    assert(spec.roles.position.eq(fixture.contract))
    assert(spec.roles.settle.eq(fixture.usd))
    assert(spec.positionLotGrid.eq(fixture.contractLots))
    assert(spec.priceGrid.eq(fixture.usdPerBtcTicks))
    assertEquals(spec.basePerPosition.coefficient, Rational.one)
    assertEquals(spec.quotePerPosition.coefficient, Rational.zero)
    assertEquals(spec.positionLotGrid.dimension.key, spec.roles.position.dimension.key)
    assertEquals(
      spec.priceGrid.dimension.key,
      DimRef.divide(spec.roles.quote.dimension.ref, spec.roles.base.dimension.ref).key
    )
    assert(built.spec.eq(spec))
    assert(built.roles.eq(spec.roles))
    assert(built.positionLotGrid.eq(spec.positionLotGrid))
    assert(built.priceGrid.eq(spec.priceGrid))

  test("assembly accumulates structural, lookup, and eligible dependent failures in stable stage order"):
    val missingBase   = AssetId.from("missing-base").toOption.get
    val missingSettle = AssetId.from("missing-settle").toOption.get
    val missingLots   = GridIdentity(
      fixture.contract.dimension.key,
      GridKey(GridId.from("missing-lots").toOption.get, GridVersion.from(1).toOption.get)
    )
    val raw = InstrumentDefinition(
      validIdentity("ordered-errors"),
      AssetRoleIds(missingBase, missingBase, fixture.contract.id, missingSettle),
      ListingDefinition(missingLots, fixture.usdCents.identity),
      PayoffDefinition(Rational.zero, Rational.zero)
    )

    val errors = InstrumentAssembler.assemble(raw, fixture.snapshot).swap.toOption.get
    assertEquals(
      errors.violations.map:
        case InstrumentAssemblyViolation.EqualBaseAndQuote(_, _)           => "structural:equal"
        case InstrumentAssemblyViolation.EmptyPayoff(_)                    => "structural:empty"
        case InstrumentAssemblyViolation.AssetResolution(_, role, _, _, _) => s"asset:$role"
        case InstrumentAssemblyViolation.GridResolution(_, role, _, _, _)  => s"grid:$role"
        case InstrumentAssemblyViolation.GridDimension(_, role, _, _, _)   => s"dimension:$role",
      Vector(
        "structural:equal",
        "structural:empty",
        "asset:Base",
        "asset:Quote",
        "asset:Settle",
        "grid:PositionLot"
      )
    )
    assert(errors.violations.collect { case _: InstrumentAssemblyViolation.GridDimension => () }.isEmpty)
    assertEquals(InstrumentAssembler.assembleFirst(raw, fixture.snapshot), Left(errors.head))
    errors.violations.collect { case value: InstrumentAssemblyViolation.AssetResolution => value }.foreach: value =>
      assertEquals(value.revision, fixture.snapshot.revision)
      assert(value.cause.isInstanceOf[UnknownAsset])

  test("dependent grid checks run only with their own prerequisites"):
    val missingSettle = AssetId.from("missing-dependent-settle").toOption.get
    val raw           = InstrumentDefinition(
      validIdentity("dependent-errors"),
      AssetRoleIds(fixture.btc.id, fixture.usd.id, fixture.contract.id, missingSettle),
      ListingDefinition(fixture.usdCents.identity, fixture.usdCents.identity),
      PayoffDefinition(Rational.one, Rational.zero)
    )
    val violations = InstrumentAssembler.assemble(raw, fixture.snapshot).swap.toOption.get.violations
    assertEquals(
      violations.map:
        case InstrumentAssemblyViolation.AssetResolution(_, role, _, _, _) => s"asset:$role"
        case InstrumentAssemblyViolation.GridDimension(_, role, _, _, _)   => s"dimension:$role"
        case other                                                         => fail(s"unexpected violation $other"),
      Vector("asset:Settle", "dimension:PositionLot", "dimension:Price")
    )

  test("one supplied snapshot fixes membership and later catalog publication is observed only when selected"):
    val raw       = validDefinition("fixed-snapshot")
    val oldResult = InstrumentAssembler.assemble(raw, fixture.snapshotBeforePriceGrid)
    val oldErrors = oldResult.swap.toOption.get
    assertEquals(oldErrors.violations.size, 1)
    oldErrors.head match
      case InstrumentAssemblyViolation.GridResolution(_, ListingGridRole.Price, requested, revision, cause) =>
        assertEquals(requested, fixture.usdPerBtcTicks.identity)
        assertEquals(revision, fixture.snapshotBeforePriceGrid.revision)
        assertEquals(cause, UnknownGrid(requested))
      case other => fail(s"unexpected old-snapshot violation $other")
    assert(InstrumentAssembler.assemble(raw, fixture.snapshot).isRight)
    assertEquals(InstrumentAssembler.assemble(raw, fixture.snapshotBeforePriceGrid), oldResult)

  test("assembly diagnostics, specifications, and instruments reject Java object serialization"):
    val raw       = validDefinition("serialization")
    val spec      = InstrumentAssembler.assemble(raw, fixture.snapshot).toOption.get
    val built     = Instrument.fromSpec(spec)
    val aggregate = InstrumentAssemblyErrors.one(
      InstrumentAssemblyViolation.EmptyPayoff(raw.identity.id)
    )

    Vector(aggregate, spec, built).foreach: value =>
      val bytes = new ByteArrayOutputStream
      val out   = new ObjectOutputStream(bytes)
      val _     = intercept[java.io.NotSerializableException](out.writeObject(value))
      out.close()

    val signatures = InstrumentAssembler.getClass.getMethods.toVector.map(_.toGenericString).mkString("\n")
    assert(!signatures.contains("ValidatedNec"))
    assert(!signatures.contains("NonEmptyChain"))

  test("market states preserve exact anchor equations and own settle conversion"):
    val price        = fixture.price(instrument, 100)
    val quoteSettled = instrument.market.quoteSettled(price).toOption.get
    assertEquals(quoteSettled.baseToSettle.coefficient, Rational(100))
    assertEquals(quoteSettled.quoteToSettle.coefficient, Rational.one)
    assertEquals(
      quoteSettled
        .convertToSettle(fixture.usd)(Quantity(fixture.usd.dimension.ref, Rational(7)))
        .map(_.coefficient),
      Right(Rational(7))
    )

    val token = instrument.market.conversion(fixture.token, Rational(2)).toOption.get
    assertEquals(token.rate.coefficient, Rational(2))
    val withToken = instrument.market.quoteSettled(price, Vector(token)).toOption.get
    assertEquals(
      withToken
        .convertToSettle(fixture.token)(Quantity(fixture.token.dimension.ref, Rational(3)))
        .map(_.coefficient),
      Right(Rational(6))
    )
    assertEquals(
      withToken
        .convertToSettle(fixture.token)(Quantity(fixture.token.dimension.ref, Rational(1, 3)))
        .map(_.coefficient),
      Right(Rational(2, 3))
    )
    val foreign = new EconomicsFixtures
    assert(
      withToken
        .convertToSettle(foreign.token)(Quantity(foreign.token.dimension.ref, Rational.one))
        .swap
        .exists(_.isInstanceOf[ForeignReferenceDataLineage])
    )
    assertEquals(
      instrument.market.fromAnchors(price, Rational(99), Rational.one),
      Left(IncoherentMarketState(Rational(100), Rational(99), Rational.one))
    )

    val scalar    = instrument.market.fromQuoteAnchor(price, Rational.one).toOption.get
    val typedRate = Rate(
      instrument.roles.quote.dimension.ref,
      instrument.roles.settle.dimension.ref,
      Rational.one
    )
    val typed = instrument.market.fromQuoteRate(price, typedRate).toOption.get
    assertEquals(scalar.baseToSettle.coefficient, typed.baseToSettle.coefficient)

  test("linear, inverse, quanto, and spot-like valuation retain exact formulas"):
    val linearLots     = fixture.linear.lots(1000).toOption.get
    val linearPosition = fixture.linear.positionLots(Side.Buy, linearLots).toOption.get
    val linearEntry    = fixture.state(fixture.linear, 100)
    val linearExit     = fixture.state(fixture.linear, 110)
    assertEquals(
      fixture.linear.valuation.pricePnl(linearPosition, linearEntry, linearExit).map(_.coefficient),
      Right(Rational(10))
    )

    val inverseLots     = fixture.inverse.lots(1000).toOption.get
    val inversePosition = fixture.inverse.positionLots(Side.Buy, inverseLots).toOption.get
    val inverseEntry    = fixture.inverse.market.baseSettled(fixture.price(fixture.inverse, 100)).toOption.get
    val inverseExit     = fixture.inverse.market.baseSettled(fixture.price(fixture.inverse, 110)).toOption.get
    assertEquals(
      fixture.inverse.valuation.pricePnl(inversePosition, inverseEntry, inverseExit).map(_.coefficient),
      Right(Rational(1, 11))
    )

    val quantoPrice = fixture.price(fixture.quanto, 100)
    val quoteToEur  = Rate(
      fixture.quanto.roles.quote.dimension.ref,
      fixture.quanto.roles.settle.dimension.ref,
      Rational(9, 10)
    )
    val quantoState = fixture.quanto.market.fromQuoteRate(quantoPrice, quoteToEur).toOption.get
    assertEquals(fixture.quanto.valuation.settlePerPosition(quantoState).map(_.coefficient), Right(Rational(90)))

    val spotLots = fixture.spotLike.lots(100_000_000).toOption.get
    assertEquals(spotLots.quantity.coefficient, Rational.one)

  private def validIdentity(name: String): InstrumentIdentity =
    InstrumentIdentity(
      InstrumentId.from(name).toOption.get,
      UnderlyingId.from(s"underlying:$name").toOption.get
    )

  private def validDefinition(name: String): InstrumentDefinition =
    InstrumentDefinition(
      validIdentity(name),
      AssetRoleIds(fixture.btc.id, fixture.usd.id, fixture.contract.id, fixture.usd.id),
      ListingDefinition(fixture.contractLots.identity, fixture.usdPerBtcTicks.identity),
      PayoffDefinition(Rational.one, Rational.zero)
    )

end InstrumentEconomicsSuite
