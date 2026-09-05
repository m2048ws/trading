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
  Vector(fixture.linear, fixture.inverse, fixture.quanto).foreach: instrument =>
    property(s"market scalar/rate/direct/scoped construction preserves exact coherence: ${instrument.identity.id}"):
      forAll { (rawPrice: Int, numerator: Int, denominator: Int) =>
        val coefficient      = Rational(BigInt(rawPrice).abs + 1, 2)
        val quoteCoefficient =
          if instrument.roles.settle.id == instrument.roles.quote.id then Rational.one
          else if instrument.roles.settle.id == instrument.roles.base.id then Rational(2, BigInt(rawPrice).abs + 1)
          else Rational(BigInt(numerator).abs + 1, BigInt(denominator).abs + 1)
        val baseCoefficient = coefficient * quoteCoefficient
        val price           = fixture.price(instrument, coefficient)
        val base  = Rate(instrument.roles.base.dimension.ref, instrument.roles.settle.dimension.ref, baseCoefficient)
        val quote = Rate(instrument.roles.quote.dimension.ref, instrument.roles.settle.dimension.ref, quoteCoefficient)
        val conversion = SettlementConversion.exact(instrument)(fixture.token)(Rational(2, 7)).toOption.get
        val additional = Vector(conversion)
        val markets    = MarketState.forInstrument(instrument)
        val results    = Vector(
          MarketState.fromQuoteAnchor(instrument)(price, quoteCoefficient, additional),
          MarketState.fromBaseAnchor(instrument)(price, baseCoefficient, additional),
          MarketState.fromAnchors(instrument)(price, baseCoefficient, quoteCoefficient, additional),
          MarketState.fromQuoteRate(instrument)(price, quote, additional),
          MarketState.fromBaseRate(instrument)(price, base, additional),
          MarketState.fromRates(instrument)(price, base, quote, additional),
          markets.fromQuoteAnchor(price, quoteCoefficient, additional),
          markets.fromBaseAnchor(price, baseCoefficient, additional),
          markets.fromAnchors(price, baseCoefficient, quoteCoefficient, additional),
          markets.fromQuoteRate(price, quote, additional),
          markets.fromBaseRate(price, base, additional),
          markets.fromRates(price, base, quote, additional)
        )
        results.forall(_.exists: state =>
          state.price == price && state.baseToSettle == base && state.quoteToSettle == quote &&
            state.price.rate.andThen(state.quoteToSettle) == state.baseToSettle &&
            state.additionalConversions == additional &&
            state.conversionSources ==
            Vector(instrument.roles.base.id, instrument.roles.quote.id,
              instrument.roles.settle.id).distinct :+ fixture.token.id &&
            state.convertToSettle(fixture.token)(Quantity(fixture.token.dimension.ref, Rational(7, 3)))
              .map(_.coefficient) == Right(Rational(2, 3))
        )
      }

  property("invalid anchors suppress coherence and duplicate errors retain deterministic order"):
    forAll { (raw: Int, rawPrice: Int) =>
      val instrument = fixture.quanto
      val markets    = MarketState.forInstrument(instrument)
      val price      = fixture.price(instrument, Rational(BigInt(rawPrice).abs + 1, 2))
      val invalid    = Rational(-BigInt(raw).abs)
      val conversion = SettlementConversion.exact(instrument)(fixture.token)(Rational.one).toOption.get
      val additional = Vector(conversion, conversion, conversion)
      val expected   = Vector(
        MarketStateViolation.InvalidConversion(instrument.roles.base.id, instrument.roles.settle.id,
          invalid, ConversionFailureReason.NonPositive),
        MarketStateViolation.InvalidConversion(instrument.roles.quote.id, instrument.roles.settle.id,
          invalid, ConversionFailureReason.NonPositive),
        MarketStateViolation.DuplicateSource(fixture.token.id),
        MarketStateViolation.DuplicateSource(fixture.token.id)
      )
      val direct = MarketState.fromAnchors(instrument)(price, invalid, invalid, additional)
      val scoped = markets.fromAnchors(price, invalid, invalid, additional)
      direct.left.toOption.exists(_.violations == expected) &&
      scoped.left.toOption.exists(_.violations == expected) &&
      MarketState.firstError(scoped) == Left(expected.head)
    }
end EconomicsPropertiesSuite
