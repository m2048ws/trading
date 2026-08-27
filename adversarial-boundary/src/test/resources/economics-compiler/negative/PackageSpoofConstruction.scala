package trading.economics

import trading.quantity.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

object PackageSpoofConstructionPrelude:
  val registry = new QuantityRegistry
  val asset = registry.registerAsset(AssetDefinition(AssetId("spoof"), AtomId("spoof:asset"))).toOption.get
  val grid = registry
    .registerGrid(asset)(
      GridDefinition(
        asset.dimension.key,
        GridId("spoof-grid"),
        GridVersion(1),
        PositiveRational(Rational.one).toOption.get
      )
    )
    .toOption
    .get
  val payload = Positive(grid.fromCoordinate(1)).toOption.get

// OFFENDING-BEGIN
object PackageSpoofConstruction:
  import PackageSpoofConstructionPrelude.*

  def obtain(instrument: Instrument) = instrument.ownerAuthority
  def widen[O](authority: Instrument.OwnerAuthority[O]): Instrument.OwnerAuthority[Any] = authority
  val implementedAuthority = new Instrument.OwnerAuthority[String] {}
  val issuedAuthority = new Instrument.OwnerAuthorityImpl[String]
  val forgedLots = Instrument.makeLots(implementedAuthority, grid)(payload)
  val directLots = new Instrument.LotsImpl[String, asset.D, grid.G](payload, grid)
  val directOwner = new Instrument.OwnerTag
  val directOrder = new Instrument.OrderImpl[String, Int, Int](???, ???, ???)
  val directInstrument = new Instrument.InstrumentImpl(???, ???, ???, ???)(???, ???)
// OFFENDING-END
