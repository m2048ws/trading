package trading.economics.instrument

import trading.quantity.*
import trading.quantity.grid.*
import trading.reference.*

/** Semantic identity of a trading-fee component. */
final class FeeKind private (val value: String) extends JavaSerializationUnsupported:
  override def equals(other: Any): Boolean =
    other match
      case that: FeeKind => value == that.value
      case _             => false

  override def hashCode: Int    = value.hashCode
  override def toString: String = s"FeeKind($value)"
end FeeKind

object FeeKind:
  def from(value: String): Either[EmptyFeeKind.type, FeeKind] =
    Option(value).filter(_.trim.nonEmpty).toRight(EmptyFeeKind).map(new FeeKind(_))
end FeeKind

/** Reusable, checked asset/grid/quantization context for exact fee values. */
final class FeeDenomination[D0 <: Dim] private (
  val instrumentId: InstrumentId,
  val asset: Asset { type D = D0 },
  val grid: GridHandle[D0],
  val policy: QuantizationPolicy)
  extends JavaSerializationUnsupported:

  val gridKey: GridKey      = grid.key
  val gridQuantum: Rational = grid.quantum.unrefined
end FeeDenomination

object FeeDenomination:
  def create(
    instrument: Instrument
  )(
    asset: Asset,
    grid: GridHandle[? <: Dim],
    policy: QuantizationPolicy
  ): Either[FeeValueError, FeeDenomination[asset.D]] =
    for
      _ <- DimensionHandle
             .sameLineage(asset.dimension, instrument.roles.settle.dimension)
             .left
             .map(cause => FeeReferenceDataMismatch("asset", cause))
      _ <- DimensionHandle
             .sameLineage(grid.dimension, asset.dimension)
             .left
             .map(cause => FeeReferenceDataMismatch("grid", cause))
      _ <- Either.cond(
             grid.dimension.key == asset.dimension.key,
             (),
             InvalidFeeGrid(asset.id, grid.key, asset.dimension.key, grid.dimension.key)
           )
    yield
      // The immediately preceding immutable-lineage and canonical-dimension checks establish that this existential
      // handle is the asset's grid. The cast is lexical, exposes no retyping authority, and the checked handle is
      // retained by the denomination (INV-G2, INV-G5, INV-C7).
      val typedGrid = grid.asInstanceOf[GridHandle[asset.D]]
      new FeeDenomination(instrument.identity.id, asset, typedGrid, policy)
end FeeDenomination

/** Exact signed fee value. Negative is a charge; positive is a rebate. */
final class Fee[D0 <: Dim] private (
  val instrumentId: InstrumentId,
  val denomination: FeeDenomination[D0],
  val kind: FeeKind,
  val asset: Asset { type D = D0 }
)(
  val gridAmount: GridQuantity[D0, denomination.grid.G],
  val residual: Quantity[D0],
  val unrounded: Quantity[D0])
  extends JavaSerializationUnsupported:

  val coordinate: BigInt   = denomination.grid.coordinate(gridAmount)
  val amount: Quantity[D0] = denomination.grid.asQuantity(gridAmount)

  override def equals(other: Any): Boolean =
    other match
      case that: Fee[?] =>
        instrumentId == that.instrumentId && denomination.instrumentId == that.denomination.instrumentId &&
        RetainedReferenceEquality.sameAsset(denomination.asset, that.denomination.asset) &&
        RetainedReferenceEquality.sameGrid(denomination.grid, that.denomination.grid) &&
        denomination.policy == that.denomination.policy &&
        kind == that.kind && coordinate == that.coordinate && amount.coefficient == that.amount.coefficient &&
        residual.coefficient == that.residual.coefficient && unrounded.coefficient == that.unrounded.coefficient
      case _ => false

  override def hashCode: Int =
    (
      instrumentId,
      denomination.instrumentId,
      RetainedReferenceEquality.assetHash(denomination.asset),
      RetainedReferenceEquality.gridHash(denomination.grid),
      denomination.policy,
      kind,
      coordinate,
      amount.coefficient,
      residual.coefficient,
      unrounded.coefficient
    ).hashCode
end Fee

object Fee:
  def create[D <: Dim](
    instrument: Instrument
  )(
    denomination: FeeDenomination[D],
    kind: FeeKind,
    unrounded: Quantity[D]
  ): Either[FeeValueError, Fee[D]] =
    if denomination.instrumentId != instrument.identity.id then
      Left(
        FeeInstrumentMismatch(
          "denomination",
          instrument.identity.id,
          denomination.instrumentId
        )
      )
    else
      val result = unrounded.quantizeTo(denomination.grid.grid, denomination.policy)
      Right(
        new Fee(
          instrument.identity.id,
          denomination,
          kind,
          denomination.asset
        )(
          result.value,
          result.residual,
          unrounded
        )
      )
end Fee
