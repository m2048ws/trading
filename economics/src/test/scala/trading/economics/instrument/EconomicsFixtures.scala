package trading.economics.instrument

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
      .registerDimension(DimRef.divide(usd.dimension.ref, btc.dimension.ref).key)
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
    instrument("linear-btcusd", "bitcoin-index", btc, usd, contract, usd)(
      contractLots,
      usdPerBtcTicks,
      Rational.one,
      Rational.zero
    )

  val inverse: Instrument =
    instrument("inverse-btcusd", "bitcoin-index", btc, usd, contract, btc)(
      contractLots,
      usdPerBtcTicks,
      Rational.zero,
      Rational(-100)
    )

  val quanto: Instrument =
    instrument("quanto-btcusd-eur", "bitcoin-index", btc, usd, contract, eur)(
      contractLots,
      usdPerBtcTicks,
      Rational.one,
      Rational.zero
    )

  val spotLike: Instrument =
    instrument("spot-like-btcusd", "bitcoin", btc, usd, btc, usd)(
      btcSatoshis,
      usdPerBtcTicks,
      Rational.one,
      Rational.zero
    )

  def price(instrument: Instrument, dollars: BigInt): instrument.Price =
    instrument.prices.exact(Rational(dollars)).toOption.get

  def state(instrument: Instrument, dollars: BigInt): instrument.MarketState =
    instrument.market.quoteSettled(price(instrument, dollars)).toOption.get

  def scenario(
    instrument: Instrument
  )(
    side: Side,
    lots: instrument.Lots,
    market: instrument.MarketState,
    role: LiquidityRole = LiquidityRole.Taker
  ): instrument.OrderScenario =
    val order       = instrument.orders.market(side, lots).toOption.get
    val slice       = instrument.scenarios.slice(lots, market, role).toOption.get
    val assumptions = instrument.scenarios.assumptions(
      instrument.scenarios.immediate,
      instrument.scenarios.directPricing,
      Vector(slice)
    )
    instrument.scenarios.order(order, assumptions).toOption.get

  def roundTrip(
    instrument: Instrument
  )(
    lots: instrument.Lots,
    entryDollars: BigInt,
    exitDollars: BigInt
  ): instrument.RoundTripScenario =
    val entry = scenario(instrument)(Side.Buy, lots, state(instrument, entryDollars))
    val exit  = scenario(instrument)(Side.Sell, lots, state(instrument, exitDollars))
    instrument.scenarios.roundTrip(entry, exit).toOption.get

  private def instrument(
    id: String,
    underlying: String,
    base: AssetRef,
    quote: AssetRef,
    position: AssetRef,
    settle: AssetRef
  )(
    positionGrid: RegisteredGridRef[? <: Dim],
    priceGrid: RegisteredGridRef[? <: Dim],
    baseCoefficient: Rational,
    quoteCoefficient: Rational
  ): Instrument =
    val roles    = new Roles(base, quote, position, settle)
    val identity = Identity(InstrumentId(id), UnderlyingId(underlying))
    val listing  = new ListingRules(roles)(positionGrid, priceGrid)
    val payoff   = new ContractPayoff(roles)(
      Rate(roles.position.dimension.ref, roles.base.dimension.ref, baseCoefficient),
      Rate(roles.position.dimension.ref, roles.quote.dimension.ref, quoteCoefficient)
    )
    Instrument.create(Definition(identity, roles, listing, payoff)).toOption.get
  end instrument

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
