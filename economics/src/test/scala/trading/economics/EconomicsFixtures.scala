package trading.economics

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

final class EconomicsFixtures:
  val registry = new QuantityRegistry

  val btc      = asset("btc")
  val usd      = asset("usd")
  val contract = asset("contract")
  val eur      = asset("eur")
  val token    = asset("fee-token")

  val contractLots = grid(contract, "contract-lots", Rational(1, 1000))
  val usdCents     = grid(usd, "usd-cents", Rational(1, 100))
  val btcSatoshis  = grid(btc, "btc-satoshis", Rational(1, 100_000_000))
  val eurCents     = grid(eur, "eur-cents", Rational(1, 100))
  val tokenMillis  = grid(token, "token-millis", Rational(1, 1000))

  val usdPerBtcDimension =
    registry
      .registerDimension(DimRef.divide(usd.dimension.asDimensionRef, btc.dimension.asDimensionRef).key)
      .toOption
      .get
  val usdPerBtcTicks =
    registry
      .registerGrid(usdPerBtcDimension)(
        GridDefinition(
          usdPerBtcDimension.dimension.key,
          GridId("usd-per-btc-half-dollar"),
          GridVersion(1),
          PositiveRational(Rational(1, 2)).toOption.get
        )
      )
      .toOption
      .get

  val linear: Instrument =
    Instrument
      .create(
        InstrumentId("linear-btcusd"),
        UnderlyingId("bitcoin-index"),
        btc,
        usd,
        contract,
        usd
      )(
        contractLots,
        usdPerBtcTicks,
        Rate(contract.dimension.asDimensionRef, btc.dimension.asDimensionRef, Rational.one),
        Rate(contract.dimension.asDimensionRef, usd.dimension.asDimensionRef, Rational.zero)
      )
      .toOption
      .get

  val inverse: Instrument =
    Instrument
      .create(
        InstrumentId("inverse-btcusd"),
        UnderlyingId("bitcoin-index"),
        btc,
        usd,
        contract,
        btc
      )(
        contractLots,
        usdPerBtcTicks,
        Rate(contract.dimension.asDimensionRef, btc.dimension.asDimensionRef, Rational.zero),
        Rate(contract.dimension.asDimensionRef, usd.dimension.asDimensionRef, Rational(-100))
      )
      .toOption
      .get

  val quanto: Instrument =
    Instrument
      .create(
        InstrumentId("quanto-btcusd-eur"),
        UnderlyingId("bitcoin-index"),
        btc,
        usd,
        contract,
        eur
      )(
        contractLots,
        usdPerBtcTicks,
        Rate(contract.dimension.asDimensionRef, btc.dimension.asDimensionRef, Rational.one),
        Rate(contract.dimension.asDimensionRef, usd.dimension.asDimensionRef, Rational.zero)
      )
      .toOption
      .get

  val spotLike: Instrument =
    Instrument
      .create(
        InstrumentId("spot-like-btcusd"),
        UnderlyingId("bitcoin"),
        btc,
        usd,
        btc,
        usd
      )(
        btcSatoshis,
        usdPerBtcTicks,
        Rate(btc.dimension.asDimensionRef, btc.dimension.asDimensionRef, Rational.one),
        Rate(btc.dimension.asDimensionRef, usd.dimension.asDimensionRef, Rational.zero)
      )
      .toOption
      .get

  def price(instrument: Instrument, dollars: BigInt): instrument.Price =
    instrument.prices.exact(Rational(dollars)).toOption.get

  private def asset(name: String): AssetRef =
    registry
      .registerAsset(AssetDefinition(AssetId(name), AtomId(s"asset:$name")))
      .toOption
      .get

  private def grid(asset: AssetRef, name: String, quantum: Rational): RegisteredGridRef[asset.D] =
    registry
      .registerGrid(asset)(
        GridDefinition(
          asset.dimension.key,
          GridId(name),
          GridVersion(1),
          PositiveRational(quantum).toOption.get
        )
      )
      .toOption
      .get

end EconomicsFixtures
