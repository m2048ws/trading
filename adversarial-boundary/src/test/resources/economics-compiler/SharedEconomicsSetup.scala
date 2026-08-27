package external.economics.fixtures

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

object SharedEconomicsSetup:
  val registry = new QuantityRegistry
  val base = registry.registerAsset(AssetDefinition(AssetId("shape-base"), AtomId("shape:base"))).toOption.get
  val quote = registry.registerAsset(AssetDefinition(AssetId("shape-quote"), AtomId("shape:quote"))).toOption.get
  val position =
    registry.registerAsset(AssetDefinition(AssetId("shape-position"), AtomId("shape:position"))).toOption.get
  val lotsGrid = registry
    .registerGrid(position)(
      GridDefinition(
        position.dimension.key,
        GridId("shape-lots"),
        GridVersion(1),
        PositiveRational(Rational.one).toOption.get
      )
    )
    .toOption
    .get
  val priceDimension =
    registry.registerDimension(DimRef.divide(quote.dimension.ref, base.dimension.ref).key).toOption.get
  val priceGrid = registry
    .registerGrid(priceDimension)(
      GridDefinition(
        priceDimension.dimension.key,
        GridId("shape-prices"),
        GridVersion(1),
        PositiveRational(Rational.one).toOption.get
      )
    )
    .toOption
    .get
  val settleGrid = registry
    .registerGrid(quote)(
      GridDefinition(
        quote.dimension.key,
        GridId("shape-settle"),
        GridVersion(1),
        PositiveRational(Rational(1, 100)).toOption.get
      )
    )
    .toOption
    .get
  val roles      = new Roles(base, quote, position, quote)
  val identity   = Identity(InstrumentId("shape-instrument"), UnderlyingId("shape-underlying"))
  val listing    = new ListingRules(roles)(lotsGrid, priceGrid)
  val payoff     = new ContractPayoff(roles)(
    Rate(roles.position.dimension.ref, roles.base.dimension.ref, Rational.one),
    Rate(roles.position.dimension.ref, roles.quote.dimension.ref, Rational.zero)
  )
  val definition = Definition(identity, roles, listing, payoff)
  val validated  = Instrument.validate(definition).toOption.get
  val instrument = Instrument.fromValidated(validated)
  val lots       = instrument.lots(2).toOption.get
  val price99    = instrument.prices.exact(Rational(99)).toOption.get
  val price100   = instrument.prices.exact(Rational(100)).toOption.get
  val state      = instrument.market.quoteSettled(price100).toOption.get
  val slice      = instrument.scenarios.slice(lots, state, LiquidityRole.Taker).toOption.get

  val marketOrder = instrument.orders.market(Side.Buy, lots).toOption.get
  val fixed = instrument.orders.fixedTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, price100)
  val fixedOrder    = instrument.orders.stopMarket(Side.Buy, lots, fixed).toOption.get
  val fixedEvidence = instrument.orders.fixedEvidence(fixed)(price100).toOption.get
  val trailing = instrument.orders
    .trailingTrigger(PriceReference.Mark, TriggerComparison.AtOrAbove, 1)
    .toOption
    .get
  val trailingOrder = instrument.orders.stopMarket(Side.Buy, lots, trailing).toOption.get
  val trailingEvidence = instrument.orders.trailingEvidence(trailing)(price99, price100).toOption.get
  val directLimit = instrument.orders.limit(Side.Buy, lots, price100).toOption.get
  val peg         = instrument.orders.peggedPricing(PriceReference.Mark, 1)
  val pegExecution = instrument.orders.pricedExecution(
    peg,
    TimeInForce.GoodTillCancelled,
    LiquidityConstraint.Unrestricted,
    instrument.orders.displayed
  )
  val peggedOrder = instrument.orders
    .create(instrument.orders.intent(Side.Buy, lots), instrument.orders.immediate, pegExecution)
    .toOption
    .get
  val pegResolution = instrument.orders.pegResolution(peg)(price99, price100).toOption.get

end SharedEconomicsSetup
