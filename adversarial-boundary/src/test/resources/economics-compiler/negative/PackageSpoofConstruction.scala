package trading.economics

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

object PackageSpoofConstructionPrelude:
  val registry = new QuantityRegistry
  val base = registry.registerAsset(AssetDefinition(AssetId("spoof-base"), AtomId("spoof:base"))).toOption.get
  val quote = registry.registerAsset(AssetDefinition(AssetId("spoof-quote"), AtomId("spoof:quote"))).toOption.get
  val position =
    registry.registerAsset(AssetDefinition(AssetId("spoof-position"), AtomId("spoof:position"))).toOption.get
  val quoteGrid = registry
    .registerGrid(quote)(
      GridDefinition(
        quote.dimension.key,
        GridId("spoof-quote-grid"),
        GridVersion(1),
        PositiveRational(Rational(1, 5)).toOption.get
      )
    )
    .toOption
    .get
  val zeroBaseTerm =
    Rate(position.dimension.asDimensionRef, base.dimension.asDimensionRef, Rational.zero)
  val zeroQuoteTerm =
    Rate(position.dimension.asDimensionRef, quote.dimension.asDimensionRef, Rational.zero)

// OFFENDING-BEGIN
object PackageSpoofConstruction:
  import PackageSpoofConstructionPrelude.*

  // Former top-level names must stay unavailable even from a spoofed package.
  val forgedInstrument = new InstrumentImpl(
    InstrumentId("forged-instrument"),
    UnderlyingId("forged-underlying"),
    base,
    quote,
    position,
    quote
  )(
    quoteGrid,
    quoteGrid,
    zeroBaseTerm,
    zeroQuoteTerm
  )
  val forgedConversion = new SettlementConversionImpl(base, quote, Rational(-1))

  // The companion-nested implementations and constructors must also stay private.
  val nestedInstrument = new Instrument.InstrumentImpl(
    InstrumentId("nested-forged-instrument"),
    UnderlyingId("nested-forged-underlying"),
    base,
    quote,
    position,
    quote
  )(
    quoteGrid,
    quoteGrid,
    zeroBaseTerm,
    zeroQuoteTerm
  )
  val nestedConversion = new SettlementConversion.SettlementConversionImpl(base, quote, Rational(-1))
// OFFENDING-END
