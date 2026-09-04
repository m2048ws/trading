package trading.economics.instrument

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.quantity.*

class EconomicsPropertiesSuite extends ScalaCheckSuite:
  private val fixture    = new InstrumentFixtures
  private val instrument = fixture.linear

  property("long and short price PnL are exact negations"):
    forAll { (raw: Int, entryRaw: Int, deltaRaw: Int) =>
      val count            = BigInt(raw).abs + 1
      val entryCoefficient = Rational(BigInt(entryRaw).abs + 1)
      val delta            = Rational(BigInt(deltaRaw).abs + 1)
      val entry            = fixture.quoteState(instrument, entryCoefficient)
      val exit             = fixture.quoteState(instrument, entryCoefficient + delta)
      val long             = PricePnl
        .calculate(instrument)(PositionLots.fromCoordinate(instrument)(count), entry, exit)
        .toOption
        .get
      val short = PricePnl
        .calculate(instrument)(PositionLots.fromCoordinate(instrument)(-count), entry, exit)
        .toOption
        .get
      long.quantity + short.quantity == Quantity.zero[instrument.roles.settle.D](
        using instrument.roles.settle.dimension.ref
      )
    }

  property("fee quantization conserves exact signed amounts"):
    forAll { (numerator: Int) =>
      val denomination = FeeDenomination
        .create(instrument)(fixture.usd, fixture.usdCents, trading.quantity.grid.QuantizationPolicy.HalfEven)
        .toOption
        .get
      val fee = Fee
        .create(instrument)(
          denomination,
          FeeKind.from("property").toOption.get,
          Quantity(fixture.usd.dimension.ref, Rational(numerator, 997))
        )
        .toOption
        .get
      fee.amount + fee.residual == fee.unrounded
    }

  property("three-change marked totals are permutation invariant while attribution follows input order"):
    forAll { (firstRaw: Int, secondRaw: Int, reductionRaw: Int, priceRaw: Int) =>
      val first     = BigInt(firstRaw).abs + 1
      val second    = BigInt(secondRaw).abs + 1
      val available = first + second
      val reduction = BigInt(reductionRaw).abs % available
      val price     = BigInt(priceRaw).abs + 1
      val mark      = fixture.quoteState(instrument, Rational(price + 3))
      val changes   = Vector(
        AttributedPriceChange(
          "first",
          PositionLots.fromCoordinate(instrument)(first),
          fixture.quoteState(instrument, Rational(price))
        ),
        AttributedPriceChange(
          "second",
          PositionLots.fromCoordinate(instrument)(second),
          fixture.quoteState(instrument, Rational(price + 1))
        ),
        AttributedPriceChange(
          "reduction",
          PositionLots.fromCoordinate(instrument)(-reduction),
          fixture.quoteState(instrument, Rational(price + 2))
        )
      )
      val results = changes.permutations.map: permutation =>
        permutation ->
          AttributedPricePnl
            .calculate(instrument)(permutation, PricePnlEndpoint.Marked(mark))
            .toOption
            .get
      val allResults = results.toVector
      val expected   = allResults.head._2

      allResults.forall: (permutation, result) =>
        result.endingPosition == expected.endingPosition &&
          result.pricePnl == expected.pricePnl &&
          result.settledContributions.map(_.attribution) == permutation.map(_.attribution)
    }

  property("three-change cross-zero reversals remain total, exact, and permutation invariant"):
    forAll { (firstRaw: Int, secondRaw: Int, shortRaw: Int, priceRaw: Int) =>
      val first       = BigInt(firstRaw).abs + 1
      val second      = BigInt(secondRaw).abs + 1
      val shortEnding = BigInt(shortRaw).abs + 1
      val reversal    = -(first + second + shortEnding)
      val price       = BigInt(priceRaw).abs + 1
      val mark        = fixture.quoteState(instrument, Rational(price + 3))
      val changes     = Vector(
        AttributedPriceChange(
          "first-long",
          PositionLots.fromCoordinate(instrument)(first),
          fixture.quoteState(instrument, Rational(price))
        ),
        AttributedPriceChange(
          "second-long",
          PositionLots.fromCoordinate(instrument)(second),
          fixture.quoteState(instrument, Rational(price + 1))
        ),
        AttributedPriceChange(
          "reverse-short",
          PositionLots.fromCoordinate(instrument)(reversal),
          fixture.quoteState(instrument, Rational(price + 2))
        )
      )
      val results = changes.permutations.map: permutation =>
        permutation -> AttributedPricePnl.calculate(instrument)(permutation, PricePnlEndpoint.Marked(mark))
      val allResults = results.toVector
      val expected   = allResults.head._2.toOption.get

      allResults.forall: (permutation, result) =>
        result.exists: value =>
          value.endingPosition.coordinate == -shortEnding &&
            value.endingPosition == expected.endingPosition &&
            value.pricePnl == expected.pricePnl &&
            value.settledContributions.map(_.attribution) == permutation.map(_.attribution)
    }

  property("settled execution cost is linear in same-price position changes"):
    forAll { (firstRaw: Int, secondRaw: Int, priceRaw: Int, markDeltaRaw: Int) =>
      val first     = BigInt(firstRaw).abs + 1
      val second    = BigInt(secondRaw).abs + 1
      val price     = BigInt(priceRaw).abs + 1
      val markDelta = BigInt(markDeltaRaw).abs + 1
      val execution = fixture.quoteState(instrument, Rational(price))
      val mark      = fixture.quoteState(instrument, Rational(price + markDelta))
      val split     = Vector(
        AttributedPriceChange("first", PositionLots.fromCoordinate(instrument)(first), execution),
        AttributedPriceChange("second", PositionLots.fromCoordinate(instrument)(second), execution)
      )
      val combined = Vector(
        AttributedPriceChange("combined", PositionLots.fromCoordinate(instrument)(first + second), execution)
      )
      val splitResult = AttributedPricePnl
        .calculate(instrument)(split, PricePnlEndpoint.Marked(mark))
        .toOption
        .get
      val combinedResult = AttributedPricePnl
        .calculate(instrument)(combined, PricePnlEndpoint.Marked(mark))
        .toOption
        .get

      splitResult.settledContributions.map(_.quantity).reduce(_ + _) ==
        combinedResult.settledContributions.head.quantity &&
        splitResult.pricePnl == combinedResult.pricePnl
    }
end EconomicsPropertiesSuite
