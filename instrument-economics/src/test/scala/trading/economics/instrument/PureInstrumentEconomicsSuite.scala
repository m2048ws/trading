package trading.economics.instrument

import munit.FunSuite

import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.quantity.refinement.PositiveWhole
import trading.reference.GridHandle

class PureInstrumentEconomicsSuite extends FunSuite:
  private val fixture    = new InstrumentFixtures
  private val instrument = fixture.linear

  private def assertSameType[A, B](using forward: A =:= B, backward: B =:= A): Unit =
    assert(forward != null)
    assert(backward != null)

  test("Instrument dimension aliases preserve role projections and retained values"):
    assertSameType[instrument.PositionD, instrument.roles.position.D]
    assertSameType[instrument.BaseD, instrument.roles.base.D]
    assertSameType[instrument.QuoteD, instrument.roles.quote.D]
    assertSameType[instrument.SettleD, instrument.roles.settle.D]

    val positionGrid: GridHandle[instrument.PositionD]                              = instrument.positionLotGrid
    val priceGrid: GridHandle[Divide[instrument.QuoteD, instrument.BaseD]]          = instrument.priceGrid
    val basePerPosition: Rate[instrument.PositionD, instrument.BaseD]               = instrument.basePerPosition
    val quotePerPosition: Rate[instrument.PositionD, instrument.QuoteD]             = instrument.quotePerPosition
    val lots: Lots[instrument.PositionD]                                            = fixture.lots(instrument, 1000)
    val position: PositionLots[instrument.PositionD]                                = fixture.position(instrument, 1000)
    val entry: MarketState[instrument.BaseD, instrument.QuoteD, instrument.SettleD] =
      fixture.quoteState(instrument, Rational(100))
    val exit: MarketState[instrument.BaseD, instrument.QuoteD, instrument.SettleD] =
      fixture.quoteState(instrument, Rational(110))
    val price: Price[instrument.BaseD, instrument.QuoteD] = fixture.price(instrument, Rational(100))
    val pricePnl: PricePnl[instrument.SettleD]            =
      PricePnl.calculate(instrument)(position, entry, exit).toOption.get
    val pnl: Pnl[instrument.SettleD] = Pnl.create(instrument)(pricePnl, Vector.empty).toOption.get

    val roleLots: Lots[instrument.roles.position.D]                         = lots
    val rolePrice: Price[instrument.roles.base.D, instrument.roles.quote.D] = price
    val roleMarket: MarketState[
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]                                                = entry
    val roleNet: Quantity[instrument.roles.settle.D] = pnl.netPnl

    assertEquals(instrument.identity, instrument.spec.identity)
    assertEquals(positionGrid, instrument.spec.positionLotGrid)
    assertEquals(priceGrid, instrument.spec.priceGrid)
    assertEquals(basePerPosition, instrument.spec.basePerPosition)
    assertEquals(quotePerPosition, instrument.spec.quotePerPosition)
    assertEquals(roleLots.quantity, position.quantity)
    assertEquals(rolePrice.coefficient, Rational(100))
    assertEquals(roleMarket.instrumentId, instrument.identity.id)
    assertEquals(pricePnl.quantity.coefficient, Rational(10))
    assertEquals(roleNet.coefficient, Rational(10))

    val publicNames = classOf[Instrument].getMethods.toVector.map(_.getName).toSet
    assert(!Set("PositionD", "BaseD", "QuoteD", "SettleD").exists(publicNames))

  test("Instrument is static and dependent lot/position constructors preserve grid meaning"):
    val lots = Lots.fromCount(instrument)(1000).toOption.get
    val long = PositionLots.fromCoordinate(instrument)(1000)
    val flat = PositionLots.flat(instrument)
    val sum  = PositionLots.combine(instrument)(long, PositionLots.fromCoordinate(instrument)(-1000))

    assertEquals(lots.count.unrefined, BigInt(1000))
    assertEquals(lots.grid.identity, instrument.positionLotGrid.identity)
    assertEquals(lots.quantity.coefficient, Rational.one)
    assertEquals(flat.coordinate, BigInt(0))
    assertEquals(sum.map(_.coordinate), Right(BigInt(0)))
    assertEquals(Lots.fromCount(instrument)(0), Left(InvalidLotCount(0)))
    assertEquals(Lots.fromCount(instrument)(-1), Left(InvalidLotCount(-1)))

    val publicNames = classOf[Instrument].getMethods.toVector.map(_.getName).toSet
    assert(
      !Set("prices", "market", "orders", "scenarios", "fees", "valuation", "sizing", "services").exists(publicNames)
    )

  test("price constructors agree, preserve endpoints, and quantize only when named"):
    val exact = Price.exact(instrument)(Rational(3, 2)).toOption.get
    val typed = Rate(
      instrument.roles.base.dimension.ref,
      instrument.roles.quote.dimension.ref,
      Rational(3, 2)
    )
    val fromRate  = Price.fromRate(instrument)(typed).toOption.get
    val fromTicks = Price.fromTicks(instrument)(PositiveWhole(3).toOption.get)
    val quantized = Price.quantize(instrument)(Rational(5, 4), QuantizationPolicy.Floor).toOption.get

    assertEquals(exact.ticks.unrefined, BigInt(3))
    assertEquals(fromRate.rate.coefficient, exact.rate.coefficient)
    assertEquals(fromTicks.coefficient, exact.coefficient)
    assertEquals(Price.exact(instrument)(Rational(5, 4)).isLeft, true)
    assertEquals(Price.exact(instrument)(Rational.zero), Left(InvalidPriceCoordinate(0)))
    assertEquals(
      quantized.price.coefficient + quantized.residual.coefficient,
      Rational(5, 4)
    )

  test("market-state constructors cover quote, base, and third-asset settlement exactly"):
    val price = fixture.price(instrument, Rational(100))
    val quote = MarketState.quoteSettled(instrument)(price).toOption.get
    assertEquals(quote.baseToSettle.coefficient, Rational(100))
    assertEquals(quote.quoteToSettle.coefficient, Rational.one)

    val inversePrice = fixture.price(fixture.inverse, Rational(100))
    val base         = MarketState.baseSettled(fixture.inverse)(inversePrice).toOption.get
    assertEquals(base.baseToSettle.coefficient, Rational.one)
    assertEquals(base.quoteToSettle.coefficient, Rational(1, 100))

    val quantoPrice = fixture.price(fixture.quanto, Rational(100))
    val quoteToEur  = Rate(
      fixture.quanto.roles.quote.dimension.ref,
      fixture.quanto.roles.settle.dimension.ref,
      Rational(9, 10)
    )
    val quanto = MarketState.fromQuoteRate(fixture.quanto)(quantoPrice, quoteToEur).toOption.get
    assertEquals(quanto.baseToSettle.coefficient, Rational(90))
    assertEquals(quanto.quoteToSettle.coefficient, Rational(9, 10))

  test("additional conversions retain endpoints, reject duplicates deterministically, and require explicit lookup"):
    val conversion = SettlementConversion.exact(instrument)(fixture.token)(Rational(2)).toOption.get
    val state      = MarketState
      .quoteSettled(instrument)(fixture.price(instrument, Rational(100)), Vector(conversion))
      .toOption
      .get
    val quantity = Quantity(fixture.token.dimension.ref, Rational(1, 3))
    assertEquals(state.convertToSettle(fixture.token)(quantity).map(_.coefficient), Right(Rational(2, 3)))
    assertEquals(
      state.convertToSettle(fixture.eur)(Quantity(fixture.eur.dimension.ref, Rational.one)),
      Left(MissingConversion(fixture.eur.id))
    )

    val duplicate = MarketState
      .quoteSettled(instrument)(fixture.price(instrument, Rational(100)), Vector(conversion, conversion))
      .swap
      .toOption
      .get
    assertEquals(duplicate.violations, Vector(MarketStateViolation.DuplicateSource(fixture.token.id)))

  test("anchor validation is exact and shared fail-fast projection is the accumulated head"):
    val result = MarketState.fromAnchors(instrument)(
      fixture.price(instrument, Rational(100)),
      Rational(99),
      Rational.one
    )
    val errors = result.swap.toOption.get
    assertEquals(
      errors.head,
      MarketStateViolation.IncoherentAnchors(Rational(100), Rational(99), Rational.one)
    )
    assertEquals(MarketState.firstError(result), Left(errors.head))

    val staged = MarketState.fromAnchors(instrument)(
      fixture.price(instrument, Rational(100)),
      Rational.zero,
      Rational.zero
    )
    val stagedErrors = staged.swap.toOption.get.violations
    assertEquals(stagedErrors.size, 2)
    assert(stagedErrors.forall:
      case MarketStateViolation.InvalidConversion(_, _, _, ConversionFailureReason.NonPositive) => true
      case _                                                                                    => false
    )

  test("immutable old-snapshot instruments and market states remain catalog-independent"):
    val state  = fixture.quoteState(instrument, Rational(100))
    val before = state.convertToSettle(fixture.usd)(Quantity(fixture.usd.dimension.ref, Rational(7)))
    val after  = state.convertToSettle(fixture.usd)(Quantity(fixture.usd.dimension.ref, Rational(7)))
    assertEquals(before, after)
    assertEquals(before.map(_.coefficient), Right(Rational(7)))

  test("linear, inverse, quanto, signed, flat, and off-grid valuation stays typed and exact"):
    val linearLong  = fixture.position(instrument, 1000)
    val linearShort = fixture.position(instrument, -1000)
    val entry       = fixture.quoteState(instrument, Rational(100))
    val exit        = fixture.quoteState(instrument, Rational(110))
    val long        = PricePnl.calculate(instrument)(linearLong, entry, exit).toOption.get
    val short       = PricePnl.calculate(instrument)(linearShort, entry, exit).toOption.get
    val flat        = PricePnl.calculate(instrument)(PositionLots.flat(instrument), entry, exit).toOption.get
    assertEquals(long.quantity.coefficient, Rational(10))
    assertEquals(short.quantity.coefficient, Rational(-10))
    assertEquals(flat.quantity.coefficient, Rational.zero)

    val inversePosition = fixture.position(fixture.inverse, 1000)
    val inverseEntry    = MarketState
      .baseSettled(fixture.inverse)(fixture.price(fixture.inverse, Rational(100)))
      .toOption
      .get
    val inverseExit = MarketState
      .baseSettled(fixture.inverse)(fixture.price(fixture.inverse, Rational(110)))
      .toOption
      .get
    assertEquals(
      PricePnl
        .calculate(fixture.inverse)(inversePosition, inverseEntry, inverseExit)
        .map(_.quantity.coefficient),
      Right(Rational(1, 11))
    )

    val quantoPosition = fixture.position(fixture.quanto, 1000)
    val quantoEntry    = MarketState
      .fromQuoteAnchor(fixture.quanto)(fixture.price(fixture.quanto, Rational(100)), Rational(9, 10))
      .toOption
      .get
    val quantoExit = MarketState
      .fromQuoteAnchor(fixture.quanto)(fixture.price(fixture.quanto, Rational(110)), Rational(9, 10))
      .toOption
      .get
    assertEquals(
      PricePnl
        .calculate(fixture.quanto)(quantoPosition, quantoEntry, quantoExit)
        .map(_.quantity.coefficient),
      Right(Rational(9))
    )

  test("fee values preserve charge, rebate, zero, third-asset grids, and exact conservation"):
    val kind         = FeeKind.from("trading").toOption.get
    val denomination = FeeDenomination
      .create(instrument)(fixture.token, fixture.tokenMillis, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val amounts = Vector(Rational(-1, 2500), Rational.zero, Rational(1, 2500))
    amounts.foreach: coefficient =>
      val fee = Fee
        .create(instrument)(denomination, kind, Quantity(fixture.token.dimension.ref, coefficient))
        .toOption
        .get
      assertEquals(fee.amount + fee.residual, fee.unrounded)
      assertEquals(fee.asset.id, fixture.token.id)
      assertEquals(fee.denomination.grid.asQuantity(fee.gridAmount), fee.amount)

    assert(
      FeeDenomination
        .create(instrument)(fixture.foreignUsd, fixture.foreignUsdCents, QuantizationPolicy.TowardZero)
        .isLeft
    )

    val foreignDenomination = FeeDenomination
      .create(fixture.foreignIdentity)(fixture.usd, fixture.usdCents, QuantizationPolicy.TowardZero)
      .toOption
      .get
    assertEquals(
      Fee.create(instrument)(
        foreignDenomination,
        kind,
        Quantity(fixture.usd.dimension.ref, Rational(-1, 100))
      ),
      Left(
        FeeInstrumentMismatch(
          "denomination",
          instrument.identity.id,
          fixture.foreignIdentity.identity.id
        )
      )
    )
    val policies = Vector(
      QuantizationPolicy.Floor,
      QuantizationPolicy.Ceiling,
      QuantizationPolicy.TowardZero,
      QuantizationPolicy.AwayFromZero,
      QuantizationPolicy.HalfEven,
      QuantizationPolicy.HalfOdd,
      QuantizationPolicy.HalfUp,
      QuantizationPolicy.HalfDown,
      QuantizationPolicy.HalfTowardZero,
      QuantizationPolicy.HalfAwayFromZero
    )
    policies.foreach: policy =>
      val checked = FeeDenomination
        .create(instrument)(fixture.usd, fixture.usdCents, policy)
        .toOption
        .get
      val fee = Fee
        .create(instrument)(checked, kind, Quantity(fixture.usd.dimension.ref, Rational(-1, 200)))
        .toOption
        .get
      assertEquals(fee.amount + fee.residual, fee.unrounded)

  test("settled fee contributions and PnL retain exact visible components"):
    val kind         = FeeKind.from("token").toOption.get
    val denomination = FeeDenomination
      .create(instrument)(fixture.token, fixture.tokenMillis, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val fee = Fee
      .create(instrument)(
        denomination,
        kind,
        Quantity(fixture.token.dimension.ref, Rational(-1, 1000))
      )
      .toOption
      .get
    val conversion = SettlementConversion.exact(instrument)(fixture.token)(Rational(2)).toOption.get
    val market     = MarketState
      .quoteSettled(instrument)(fixture.price(instrument, Rational(100)), Vector(conversion))
      .toOption
      .get
    val contribution = SettledFeeContribution.convert(instrument)(fee, market).toOption.get
    val pricePnl     = PricePnl
      .calculate(
        instrument
      )(
        fixture.position(instrument, 1000),
        fixture.quoteState(instrument, Rational(100)),
        fixture.quoteState(instrument, Rational(110))
      )
      .toOption
      .get
    val noFees  = Pnl.create(instrument)(pricePnl, Vector.empty).toOption.get
    val withFee = Pnl.create(instrument)(pricePnl, Vector(contribution)).toOption.get

    assertEquals(contribution.original, fee)
    assertEquals(contribution.quantity.coefficient, Rational(-1, 500))
    assertEquals(noFees.feePnl.coefficient, Rational.zero)
    assertEquals(noFees.netPnl, pricePnl.quantity)
    assertEquals(withFee.settledFeeContributions, Vector(contribution))
    assertEquals(withFee.feePnl.coefficient, Rational(-1, 500))
    assertEquals(withFee.netPnl.coefficient, Rational(4999, 500))

    val repeated       = Vector(contribution, contribution, contribution)
    val leftAssociated =
      repeated(0).quantity + repeated(1).quantity + repeated(2).quantity
    val rightAssociated =
      repeated(0).quantity + (repeated(1).quantity + repeated(2).quantity)
    assertEquals(leftAssociated, rightAssociated)
    val firstPnl  = Pnl.create(instrument)(pricePnl, repeated).toOption.get
    val secondPnl = Pnl.create(instrument)(pricePnl, repeated).toOption.get
    assertEquals(firstPnl, secondPnl)

    val foreignFee = Fee
      .create(fixture.foreignIdentity)(
        FeeDenomination
          .create(fixture.foreignIdentity)(fixture.token, fixture.tokenMillis, QuantizationPolicy.TowardZero)
          .toOption
          .get,
        kind,
        Quantity(fixture.token.dimension.ref, Rational(-1, 1000))
      )
      .toOption
      .get
    assertEquals(
      SettledFeeContribution.convert(instrument)(foreignFee, market),
      Left(
        ContributionInstrumentMismatch(
          "fee",
          instrument.identity.id,
          fixture.foreignIdentity.identity.id
        )
      )
    )

  test("fee and retained PnL equality distinguish observable quantization policy"):
    val kind              = FeeKind.from("policy-equality").toOption.get
    val floorDenomination = FeeDenomination
      .create(instrument)(fixture.usd, fixture.usdCents, QuantizationPolicy.Floor)
      .toOption
      .get
    val ceilingDenomination = FeeDenomination
      .create(instrument)(fixture.usd, fixture.usdCents, QuantizationPolicy.Ceiling)
      .toOption
      .get
    val zero         = Quantity(fixture.usd.dimension.ref, Rational.zero)
    val floorFee     = Fee.create(instrument)(floorDenomination, kind, zero).toOption.get
    val sameFloorFee = Fee.create(instrument)(floorDenomination, kind, zero).toOption.get
    val ceilingFee   = Fee.create(instrument)(ceilingDenomination, kind, zero).toOption.get

    assertEquals(floorFee, sameFloorFee)
    assertEquals(floorFee.hashCode, sameFloorFee.hashCode)
    assertNotEquals(floorFee, ceilingFee)
    assertNotEquals(floorFee.hashCode, ceilingFee.hashCode)

    val market   = fixture.quoteState(instrument, Rational(100))
    val pricePnl = PricePnl
      .calculate(instrument)(PositionLots.flat(instrument), market, market)
      .toOption
      .get
    val floorContribution     = SettledFeeContribution.convert(instrument)(floorFee, market).toOption.get
    val sameFloorContribution = SettledFeeContribution.convert(instrument)(sameFloorFee, market).toOption.get
    val ceilingContribution   = SettledFeeContribution.convert(instrument)(ceilingFee, market).toOption.get
    val floorPnl              = Pnl.create(instrument)(pricePnl, Vector(floorContribution)).toOption.get
    val sameFloorPnl          = Pnl.create(instrument)(pricePnl, Vector(sameFloorContribution)).toOption.get
    val ceilingPnl            = Pnl.create(instrument)(pricePnl, Vector(ceilingContribution)).toOption.get

    assertEquals(floorPnl, sameFloorPnl)
    assertEquals(floorPnl.hashCode, sameFloorPnl.hashCode)
    assertNotEquals(floorPnl, ceilingPnl)
    assertNotEquals(floorPnl.hashCode, ceilingPnl.hashCode)

  test("missing fee conversion is a typed contribution failure"):
    val denomination = FeeDenomination
      .create(instrument)(fixture.token, fixture.tokenMillis, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val fee = Fee
      .create(instrument)(
        denomination,
        FeeKind.from("missing").toOption.get,
        Quantity(fixture.token.dimension.ref, Rational(-1, 1000))
      )
      .toOption
      .get
    assertEquals(
      SettledFeeContribution.convert(instrument)(fee, fixture.quoteState(instrument, Rational(100))),
      Left(ContributionConversionFailure(MissingConversion(fixture.token.id)))
    )
end PureInstrumentEconomicsSuite
