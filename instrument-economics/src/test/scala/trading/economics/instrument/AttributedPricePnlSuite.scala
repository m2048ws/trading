package trading.economics.instrument

import munit.FunSuite

import trading.quantity.*

class AttributedPricePnlSuite extends FunSuite:
  private val fixture    = new InstrumentFixtures
  private val instrument = fixture.linear

  test("empty flat input is the typed exact zero"):
    val result = AttributedPricePnl
      .calculate(instrument)(
        Vector.empty[instrument.AttributedPriceChange[String]],
        PricePnlEndpoint.Flat
      )
      .toOption
      .get

    assertEquals(result.instrumentId, instrument.identity.id)
    assertEquals(result.settlement.id, instrument.roles.settle.id)
    assertEquals(result.endingPosition, PositionLots.flat(instrument))
    assertEquals(result.settledContributions, Vector.empty)
    assertEquals(result.pricePnl.quantity.coefficient, Rational.zero)
    assertEquals(result.endpoint, PricePnlEndpoint.Flat)
    assertEquals(result.traversalCost, AttributedPricePnlTraversalCost(0, 0, 0, 0))

  test("three attributed changes preserve order and produce exact marked PnL"):
    val mark    = fixture.quoteState(instrument, Rational(125))
    val changes = Vector(
      change("open", 1000, 100),
      change("scale-in", 500, 110),
      change("reduce", -300, 120)
    )
    val result = AttributedPricePnl
      .calculate(instrument)(changes, PricePnlEndpoint.Marked(mark))
      .toOption
      .get

    assertEquals(result.endingPosition.coordinate, BigInt(1200))
    assertEquals(result.settledContributions.map(_.attribution), Vector("open", "scale-in", "reduce"))
    assertEquals(
      result.settledContributions.map(_.quantity.coefficient),
      Vector(Rational(-100), Rational(-55), Rational(36))
    )
    assertEquals(result.pricePnl.quantity.coefficient, Rational(31))
    assertEquals(result.endpoint, PricePnlEndpoint.Marked(mark))
    assertEquals(result.settledContributions.map(_.original), changes)

  test("multi-change flat input sums exact execution cashflows"):
    val changes = Vector(
      change("open", 1000, 100),
      change("scale-in", 500, 110),
      change("close", -1500, 120)
    )
    val result = AttributedPricePnl
      .calculate(instrument)(changes, PricePnlEndpoint.Flat)
      .toOption
      .get

    assertEquals(result.endingPosition.coordinate, BigInt(0))
    assertEquals(result.settledContributions.map(_.attribution), changes.map(_.attribution))
    assertEquals(result.pricePnl.quantity.coefficient, Rational(25))

  test("flat and marked endpoint mismatches are precise typed failures"):
    val nonFlat = AttributedPricePnl.calculate(instrument)(Vector(change("open", 1000, 100)), PricePnlEndpoint.Flat)
    assertEquals(
      nonFlat.left.map(_.violations),
      Left(Vector(AttributedPricePnlViolation.NonFlatPositionRequiresMark(1000)))
    )

    val flatMarked = AttributedPricePnl.calculate(instrument)(
      Vector.empty[instrument.AttributedPriceChange[String]],
      PricePnlEndpoint.Marked(fixture.quoteState(instrument, Rational(100)))
    )
    assertEquals(
      flatMarked.left.map(_.violations),
      Left(Vector(AttributedPricePnlViolation.FlatPositionRejectsMark))
    )

  test("foreign identities and references accumulate stably before dependent valuation"):
    val otherFixture                                                                           = new InstrumentFixtures
    val foreignIdentity                                                                        = fixture.foreignIdentity
    val changes: Vector[AttributedPriceChange[String, ? <: Dim, ? <: Dim, ? <: Dim, ? <: Dim]] = Vector(
      AttributedPriceChange(
        "foreign-identity",
        fixture.position(foreignIdentity, 1000),
        fixture.quoteState(foreignIdentity, Rational(100))
      ),
      AttributedPriceChange(
        "foreign-references",
        otherFixture.position(otherFixture.linear, 1000),
        otherFixture.quoteState(otherFixture.linear, Rational(100))
      )
    )

    val violations = AttributedPricePnl
      .calculate(instrument)(changes, PricePnlEndpoint.Flat)
      .swap
      .toOption
      .get
      .violations

    assertEquals(
      violations.take(3),
      Vector(
        AttributedPricePnlViolation.InstrumentMismatch(
          AttributedPricePnlLocation.Change(0, AttributedPricePnlComponent.Position),
          instrument.identity.id,
          foreignIdentity.identity.id
        ),
        AttributedPricePnlViolation.InstrumentMismatch(
          AttributedPricePnlLocation.Change(0, AttributedPricePnlComponent.Market),
          instrument.identity.id,
          foreignIdentity.identity.id
        ),
        AttributedPricePnlViolation.InstrumentMismatch(
          AttributedPricePnlLocation.Change(0, AttributedPricePnlComponent.Price),
          instrument.identity.id,
          foreignIdentity.identity.id
        )
      )
    )
    assertEquals(
      violations.drop(3).map:
        case AttributedPricePnlViolation.ReferenceMismatch(location, _) => location
        case other => fail(s"expected reference mismatch, got $other"),
      Vector(
        AttributedPricePnlLocation.Change(1, AttributedPricePnlComponent.PositionGrid),
        AttributedPricePnlLocation.Change(1, AttributedPricePnlComponent.Base),
        AttributedPricePnlLocation.Change(1, AttributedPricePnlComponent.Quote),
        AttributedPricePnlLocation.Change(1, AttributedPricePnlComponent.Settlement),
        AttributedPricePnlLocation.Change(1, AttributedPricePnlComponent.PriceGrid)
      )
    )
    assert(!violations.exists:
      case AttributedPricePnlViolation.NonFlatPositionRequiresMark(_) => true
      case AttributedPricePnlViolation.ValuationFailure(_, _)         => true
      case _                                                          => false
    )

  test("foreign terminal references fail before marked valuation"):
    val otherFixture = new InstrumentFixtures
    val result       = AttributedPricePnl.calculate(instrument)(
      Vector(change("open", 1000, 100)),
      PricePnlEndpoint.Marked(otherFixture.quoteState(otherFixture.linear, Rational(110)))
    )
    val violations = result.swap.toOption.get.violations

    assertEquals(
      violations.map:
        case AttributedPricePnlViolation.ReferenceMismatch(location, _) => location
        case other => fail(s"expected reference mismatch, got $other"),
      Vector(
        AttributedPricePnlLocation.Endpoint(AttributedPricePnlComponent.Base),
        AttributedPricePnlLocation.Endpoint(AttributedPricePnlComponent.Quote),
        AttributedPricePnlLocation.Endpoint(AttributedPricePnlComponent.Settlement),
        AttributedPricePnlLocation.Endpoint(AttributedPricePnlComponent.PriceGrid)
      )
    )
    assert(!violations.exists:
      case AttributedPricePnlViolation.ValuationFailure(_, _) => true
      case _                                                  => false
    )

  test("arbitrarily large coordinates remain exact without overflow or approximation"):
    val coordinate = BigInt(2).pow(256)
    val market     = fixture.quoteState(instrument, Rational(100))
    val result     = AttributedPricePnl
      .calculate(instrument)(
        Vector(
          AttributedPriceChange(
            "large",
            PositionLots.fromCoordinate(instrument)(coordinate),
            market
          )
        ),
        PricePnlEndpoint.Marked(market)
      )
      .toOption
      .get

    assertEquals(result.endingPosition.coordinate, coordinate)
    assertEquals(result.pricePnl.quantity.coefficient, Rational.zero)

  test("successful traversal cost grows exactly once per change in each owning phase"):
    Vector(1, 10, 1000).foreach: size =>
      val market  = fixture.quoteState(instrument, Rational(100))
      val changes = Vector.tabulate(size): index =>
        AttributedPriceChange(index, PositionLots.fromCoordinate(instrument)(1), market)
      val result = AttributedPricePnl
        .calculate(instrument)(changes, PricePnlEndpoint.Marked(market))
        .toOption
        .get
      val expectedVisits = BigInt(size)

      assertEquals(
        result.traversalCost,
        AttributedPricePnlTraversalCost(expectedVisits, expectedVisits, expectedVisits, expectedVisits)
      )

  private def change(
    attribution: String,
    coordinate: BigInt,
    price: BigInt
  ): instrument.AttributedPriceChange[String] =
    AttributedPriceChange(
      attribution,
      PositionLots.fromCoordinate(instrument)(coordinate),
      fixture.quoteState(instrument, Rational(price))
    )
end AttributedPricePnlSuite
