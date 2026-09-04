package trading.economics.instrument

import trading.quantity.*
import trading.quantity.refinement.*
import trading.reference.*

/**
 * Equality support for retained reference-data meaning.
 *
 * Opaque issuer lineage deliberately refines equality through reconciliation. Hashes cover the complete public
 * immutable definition; values from different lineages may therefore collide without comparing equal.
 */
private[instrument] object RetainedReferenceEquality:
  def sameAsset(left: Asset, right: Asset): Boolean =
    Asset.reconcile(left, right).isRight

  def assetHash(asset: Asset): Int =
    (asset.id, asset.dimension.key).hashCode

  def sameGrid(left: GridHandle[? <: Dim], right: GridHandle[? <: Dim]): Boolean =
    GridHandle.reconcile(left, right).isRight

  def gridHash(grid: GridHandle[? <: Dim]): Int =
    (grid.identity, grid.quantum.unrefined).hashCode
end RetainedReferenceEquality

/** Strictly positive lots on one instrument's retained position grid. */
final class Lots[D <: Dim] private (
  val instrumentId: InstrumentId,
  val count: PositiveWhole,
  val grid: GridHandle[D],
  val quantity: Quantity[D])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean =
    other match
      case that: Lots[?] =>
        instrumentId == that.instrumentId && count == that.count &&
        RetainedReferenceEquality.sameGrid(grid, that.grid) &&
        quantity.coefficient == that.quantity.coefficient
      case _ => false

  override def hashCode: Int =
    (instrumentId, count, RetainedReferenceEquality.gridHash(grid), quantity.coefficient).hashCode
end Lots

object Lots:
  def fromCount(instrument: Instrument)(count: BigInt): Either[LotError, instrument.Lots] =
    PositiveWhole(count)
      .left
      .map(_ => InvalidLotCount(count))
      .map: positive =>
        val coordinate = instrument.positionLotGrid.fromCoordinate(positive.unrefined)
        new Lots(instrument.identity.id, positive, instrument.positionLotGrid,
          instrument.positionLotGrid.asQuantity(coordinate))
end Lots

/** Signed position coordinate on one instrument's retained position grid, including flat zero. */
final class PositionLots[D <: Dim] private (
  val instrumentId: InstrumentId,
  val coordinate: BigInt,
  val grid: GridHandle[D],
  val quantity: Quantity[D])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean =
    other match
      case that: PositionLots[?] =>
        instrumentId == that.instrumentId && coordinate == that.coordinate &&
        RetainedReferenceEquality.sameGrid(grid, that.grid) &&
        quantity.coefficient == that.quantity.coefficient
      case _ => false

  override def hashCode: Int =
    (instrumentId, coordinate, RetainedReferenceEquality.gridHash(grid), quantity.coefficient).hashCode
end PositionLots

object PositionLots:
  def fromCoordinate(instrument: Instrument)(coordinate: BigInt): instrument.PositionLots =
    val gridValue = instrument.positionLotGrid.fromCoordinate(coordinate)
    new PositionLots(
      instrument.identity.id,
      coordinate,
      instrument.positionLotGrid,
      instrument.positionLotGrid.asQuantity(gridValue)
    )

  def flat(instrument: Instrument): instrument.PositionLots =
    fromCoordinate(instrument)(BigInt(0))

  def combine(
    instrument: Instrument
  )(
    left: instrument.PositionLots,
    right: instrument.PositionLots
  ): Either[PositionError, instrument.PositionLots] =
    if left.instrumentId != instrument.identity.id then
      Left(PositionInstrumentMismatch("left", instrument.identity.id, left.instrumentId))
    else if right.instrumentId != instrument.identity.id then
      Left(PositionInstrumentMismatch("right", instrument.identity.id, right.instrumentId))
    else Right(fromCoordinate(instrument)(left.coordinate + right.coordinate))
end PositionLots

/** One validated instrument aggregate. Runtime identity is ordinary domain data, not issuance authority. */
final class Instrument private (val spec: InstrumentSpec) extends JavaSerializationUnsupported:

  val identity: InstrumentIdentity = spec.identity
  val roles: spec.roles.type       = spec.roles

  val positionLotGrid: GridHandle[roles.position.D]              = spec.positionLotGrid
  val priceGrid: GridHandle[Divide[roles.quote.D, roles.base.D]] = spec.priceGrid
  val basePerPosition: Rate[roles.position.D, roles.base.D]      = spec.basePerPosition
  val quotePerPosition: Rate[roles.position.D, roles.quote.D]    = spec.quotePerPosition

  type Lots         = _root_.trading.economics.instrument.Lots[roles.position.D]
  type PositionLots = _root_.trading.economics.instrument.PositionLots[roles.position.D]
  type Price        = _root_.trading.economics.instrument.Price[roles.base.D, roles.quote.D]
  type MarketState  = _root_.trading.economics.instrument.MarketState[roles.base.D, roles.quote.D, roles.settle.D]
  type PricePnl     = _root_.trading.economics.instrument.PricePnl[roles.settle.D]
  type AttributedPriceChange[A] = _root_.trading.economics.instrument.AttributedPriceChange[
    A,
    roles.position.D,
    roles.base.D,
    roles.quote.D,
    roles.settle.D
  ]
  type PricePnlEndpoint            = _root_.trading.economics.instrument.PricePnlEndpoint[MarketState]
  type SettledPriceContribution[A] = _root_.trading.economics.instrument.SettledPriceContribution[
    A,
    roles.position.D,
    roles.base.D,
    roles.quote.D,
    roles.settle.D
  ]
  type AttributedPricePnl[A] = _root_.trading.economics.instrument.AttributedPricePnl[
    A,
    roles.position.D,
    roles.base.D,
    roles.quote.D,
    roles.settle.D
  ]
  type Pnl = _root_.trading.economics.instrument.Pnl[roles.settle.D]
end Instrument

object Instrument:
  /** Total construction from the proof-carrying assembly result. */
  def fromSpec(spec: InstrumentSpec): Instrument =
    new Instrument(spec)
end Instrument
