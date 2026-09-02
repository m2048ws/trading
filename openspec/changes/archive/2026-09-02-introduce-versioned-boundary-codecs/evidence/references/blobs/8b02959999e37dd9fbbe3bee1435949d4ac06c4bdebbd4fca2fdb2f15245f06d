package trading.codec

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.Objects
import scala.annotation.nowarn

import trading.quantity.Dim
import trading.quantity.DimKey
import trading.quantity.GridQuantity
import trading.quantity.JavaSerializationUnsupported
import trading.reference.Asset
import trading.reference.AssetId
import trading.reference.CatalogLookupError
import trading.reference.CatalogSnapshot
import trading.reference.DimensionHandle
import trading.reference.ForeignDimensionHandle
import trading.reference.GridHandle
import trading.reference.GridIdentity
import trading.reference.UnknownAsset as CatalogUnknownAsset
import trading.reference.UnknownDimension as CatalogUnknownDimension
import trading.reference.UnknownGrid as CatalogUnknownGrid

/** Closed, stage-specific failures from rebuilding an exact coordinate against one immutable snapshot. */
enum GridCoordinateReconstructionFailure extends JavaSerializationUnsupported:
  case UnknownDimension(key: DimKey)
  case UnknownAsset(id: AssetId)
  case AssetDimensionMismatch(assetId: AssetId, recorded: DimKey, actual: DimKey)
  case UnknownGrid(identity: GridIdentity)
  case SnapshotLineageInconsistency(cause: CatalogLookupError)

  private val _ =
    this match
      case UnknownDimension(key) =>
        Objects.requireNonNull(key, "unknown grid-coordinate dimension")
      case UnknownAsset(id) =>
        Objects.requireNonNull(id, "unknown grid-coordinate asset")
      case AssetDimensionMismatch(assetId, recorded, actual) =>
        Objects.requireNonNull(assetId, "grid-coordinate asset")
        val checkedRecorded = Objects.requireNonNull(recorded, "recorded grid-coordinate dimension")
        val checkedActual   = Objects.requireNonNull(actual, "actual asset dimension")
        require(checkedRecorded != checkedActual, "asset dimension mismatch requires distinct dimensions")
      case UnknownGrid(identity) =>
        Objects.requireNonNull(identity, "unknown grid identity")
      case SnapshotLineageInconsistency(cause) =>
        Objects.requireNonNull(cause, "snapshot/lineage inconsistency")
end GridCoordinateReconstructionFailure

/** One failed record in an all-valid-or-errors batch reconstruction. */
final case class IndexedGridCoordinateReconstructionFailure(
  recordIndex: Int,
  failure: GridCoordinateReconstructionFailure)
  extends JavaSerializationUnsupported:

  if recordIndex < 0 then throw new IllegalArgumentException("record index must be nonnegative")
  Objects.requireNonNull(failure, "grid-coordinate reconstruction failure")
end IndexedGridCoordinateReconstructionFailure

/** Canonical dimension, full grid witness, and value rebuilt from one general coordinate record. */
@nowarn("msg=Ignoring.*qualifier")
final class DecodedGridQuantity private[this] (
  val dimension: DimensionHandle[? <: Dim]
)(
  val grid: GridHandle[dimension.D]
)(
  val value: GridQuantity[dimension.D, grid.G])
  extends JavaSerializationUnsupported

object DecodedGridQuantity:
  private val constructor =
    val owner = classOf[DecodedGridQuantity]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[DimensionHandle[?]],
          classOf[GridHandle[?]],
          classOf[BigInt]
        )
      )

  private[codec] def fromCoordinate[D <: Dim](
    dimension: DimensionHandle[D]
  )(
    grid: GridHandle[D]
  )(
    coordinate: BigInt
  ): DecodedGridQuantity =
    constructor
      .invoke(dimension, grid, grid.fromCoordinate(coordinate))
      .asInstanceOf[DecodedGridQuantity]
end DecodedGridQuantity

/** Canonical asset, full grid witness, and value rebuilt from one asset-qualified coordinate record. */
@nowarn("msg=Ignoring.*qualifier")
final class DecodedAssetGridQuantity private[this] (
  val asset: Asset
)(
  val grid: GridHandle[asset.D]
)(
  val value: GridQuantity[asset.D, grid.G])
  extends JavaSerializationUnsupported

object DecodedAssetGridQuantity:
  private val constructor =
    val owner = classOf[DecodedAssetGridQuantity]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[Asset],
          classOf[GridHandle[?]],
          classOf[BigInt]
        )
      )

  private[codec] def fromCoordinate(
    asset: Asset
  )(
    grid: GridHandle[asset.D]
  )(
    coordinate: BigInt
  ): DecodedAssetGridQuantity =
    constructor
      .invoke(asset, grid, grid.fromCoordinate(coordinate))
      .asInstanceOf[DecodedAssetGridQuantity]
end DecodedAssetGridQuantity

private[codec] object GridCoordinateRecordTypes:
  val general: RecordType = RecordType.from("trading.general-grid-coordinate").toOption.get
  val asset: RecordType   = RecordType.from("trading.asset-grid-coordinate").toOption.get
end GridCoordinateRecordTypes

/** V1 persistence family for a full stable grid identity and its exact signed coordinate. */
object GeneralGridCoordinateRecord:
  final case class V1(gridIdentity: GridIdentity, coordinate: BigInt) extends JavaSerializationUnsupported:
    Objects.requireNonNull(gridIdentity, "grid identity")
    Objects.requireNonNull(coordinate, "grid coordinate")
  end V1

  val recordType: RecordType       = GridCoordinateRecordTypes.general
  val schemaVersion: SchemaVersion = SchemaVersion.one

  private val v1Schema: WireSchema[V1] =
    val representation =
      WireRecord
        .field("gridIdentity", ExactWire.gridIdentity)
        .product(WireRecord.field("coordinate", ExactWire.canonicalInteger))
        .imap(value => V1(value._1, value._2))(value => value.gridIdentity -> value.coordinate)
    WireSchema.record(representation)

  private val codec = EnvelopeCodec(
    recordType,
    schemaVersion,
    Vector(schemaVersion -> v1Schema),
    Set(GridCoordinateRecordTypes.asset)
  )

  /** Project one already exact grid value to stable identity plus coordinate; no lookup or projection occurs. */
  def pack[D <: Dim](grid: GridHandle[D])(value: GridQuantity[D, grid.G]): V1 =
    Objects.requireNonNull(grid, "grid handle")
    V1(grid.identity, grid.coordinate(value))

  def encode(record: V1): Either[WireViolations[WireEncodeViolation], String] =
    codec.write(Objects.requireNonNull(record, "general grid-coordinate record"))

  def parse(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], V1] =
    codec.read(input, limits, recordIndex)

  def schema(
    id: String = "urn:trading:codec:schema:general-grid-coordinate:v1",
    definitionName: String = "GeneralGridCoordinateRecordV1"
  ): Either[WireViolations[WireEncodeViolation], String] =
    codec.schema(id, definitionName)

  /** Resolve dimension first, then its full grid identity, against exactly the supplied immutable snapshot. */
  def reconstruct(
    record: V1,
    snapshot: CatalogSnapshot
  ): Either[GridCoordinateReconstructionFailure, DecodedGridQuantity] =
    val checkedRecord   = Objects.requireNonNull(record, "general grid-coordinate record")
    val checkedSnapshot = Objects.requireNonNull(snapshot, "catalog snapshot")
    checkedSnapshot
      .resolveDimension(checkedRecord.gridIdentity.dimension)
      .left
      .map(mapDimensionFailure)
      .flatMap(dimension => reconstructWithDimension(checkedRecord, checkedSnapshot, dimension))

  /** Traverse every record against one captured snapshot and return either all values or every indexed failure. */
  def reconstructBatch(
    records: Vector[V1],
    snapshot: CatalogSnapshot
  ): Either[WireViolations[IndexedGridCoordinateReconstructionFailure], Vector[DecodedGridQuantity]] =
    GridCoordinateReconstruction.batch(records, snapshot)(reconstruct)

  private def reconstructWithDimension[D <: Dim](
    record: V1,
    snapshot: CatalogSnapshot,
    dimension: DimensionHandle[D]
  ): Either[GridCoordinateReconstructionFailure, DecodedGridQuantity] =
    snapshot
      .resolveGrid(dimension)(record.gridIdentity.key)
      .left
      .map(error => mapGridFailure(record.gridIdentity, error))
      .map(grid => DecodedGridQuantity.fromCoordinate(dimension)(grid)(record.coordinate))
end GeneralGridCoordinateRecord

/** V1 persistence family for an asset-qualified full grid identity and exact signed coordinate. */
object AssetGridCoordinateRecord:
  final case class V1(assetId: AssetId, gridIdentity: GridIdentity, coordinate: BigInt)
    extends JavaSerializationUnsupported:
    Objects.requireNonNull(assetId, "asset ID")
    Objects.requireNonNull(gridIdentity, "grid identity")
    Objects.requireNonNull(coordinate, "grid coordinate")
  end V1

  val recordType: RecordType       = GridCoordinateRecordTypes.asset
  val schemaVersion: SchemaVersion = SchemaVersion.one

  private val v1Schema: WireSchema[V1] =
    val representation =
      WireRecord
        .field("assetId", ExactWire.assetId)
        .product(WireRecord.field("gridIdentity", ExactWire.gridIdentity))
        .product(WireRecord.field("coordinate", ExactWire.canonicalInteger))
        .imap(value => V1(value._1._1, value._1._2, value._2))(value =>
          ((value.assetId, value.gridIdentity), value.coordinate)
        )
    WireSchema.record(representation)

  private val codec = EnvelopeCodec(
    recordType,
    schemaVersion,
    Vector(schemaVersion -> v1Schema),
    Set(GridCoordinateRecordTypes.general)
  )

  /** Project one value already typed to the asset's exact grid; no lookup, quantization, or projection occurs. */
  def pack(
    asset: Asset
  )(
    grid: GridHandle[asset.D]
  )(
    value: GridQuantity[asset.D, grid.G]
  ): V1 =
    Objects.requireNonNull(asset, "asset")
    Objects.requireNonNull(grid, "grid handle")
    V1(asset.id, grid.identity, grid.coordinate(value))

  def encode(record: V1): Either[WireViolations[WireEncodeViolation], String] =
    codec.write(Objects.requireNonNull(record, "asset grid-coordinate record"))

  def parse(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], V1] =
    codec.read(input, limits, recordIndex)

  def schema(
    id: String = "urn:trading:codec:schema:asset-grid-coordinate:v1",
    definitionName: String = "AssetGridCoordinateRecordV1"
  ): Either[WireViolations[WireEncodeViolation], String] =
    codec.schema(id, definitionName)

  /** Resolve asset, verify its recorded dimension, then resolve its full grid against one supplied snapshot. */
  def reconstruct(
    record: V1,
    snapshot: CatalogSnapshot
  ): Either[GridCoordinateReconstructionFailure, DecodedAssetGridQuantity] =
    val checkedRecord   = Objects.requireNonNull(record, "asset grid-coordinate record")
    val checkedSnapshot = Objects.requireNonNull(snapshot, "catalog snapshot")
    checkedSnapshot
      .resolveAsset(checkedRecord.assetId)
      .left
      .map(mapAssetFailure)
      .flatMap(asset => reconstructForAsset(checkedRecord, checkedSnapshot, asset))

  /** Traverse every record against one captured snapshot and return either all values or every indexed failure. */
  def reconstructBatch(
    records: Vector[V1],
    snapshot: CatalogSnapshot
  ): Either[WireViolations[IndexedGridCoordinateReconstructionFailure], Vector[DecodedAssetGridQuantity]] =
    GridCoordinateReconstruction.batch(records, snapshot)(reconstruct)

  private def reconstructForAsset(
    record: V1,
    snapshot: CatalogSnapshot,
    asset: Asset
  ): Either[GridCoordinateReconstructionFailure, DecodedAssetGridQuantity] =
    if asset.dimension.key != record.gridIdentity.dimension then
      Left(
        GridCoordinateReconstructionFailure.AssetDimensionMismatch(
          asset.id,
          record.gridIdentity.dimension,
          asset.dimension.key
        )
      )
    else
      snapshot
        .resolveGrid(asset.dimension)(record.gridIdentity.key)
        .left
        .map(error => mapGridFailure(record.gridIdentity, error))
        .map(grid => DecodedAssetGridQuantity.fromCoordinate(asset)(grid)(record.coordinate))
end AssetGridCoordinateRecord

private[codec] object GridCoordinateReconstruction:
  def batch[A, B](
    records: Vector[A],
    snapshot: CatalogSnapshot
  )(
    reconstruct: (A, CatalogSnapshot) => Either[GridCoordinateReconstructionFailure, B]
  ): Either[WireViolations[IndexedGridCoordinateReconstructionFailure], Vector[B]] =
    val checkedRecords  = Objects.requireNonNull(records, "grid-coordinate records")
    val checkedSnapshot = Objects.requireNonNull(snapshot, "catalog snapshot")
    val results         = checkedRecords.zipWithIndex.map: (record, index) =>
      reconstruct(Objects.requireNonNull(record, s"grid-coordinate record $index"), checkedSnapshot)
        .left
        .map(failure => IndexedGridCoordinateReconstructionFailure(index, failure))
    val failures = results.collect:
      case Left(failure) => failure
    WireViolations.fromVector(failures) match
      case Some(errors) => Left(errors)
      case None         => Right(results.collect { case Right(value) => value })
end GridCoordinateReconstruction

private def mapDimensionFailure(error: CatalogLookupError): GridCoordinateReconstructionFailure =
  error match
    case CatalogUnknownDimension(key) => GridCoordinateReconstructionFailure.UnknownDimension(key)
    case other                        => GridCoordinateReconstructionFailure.SnapshotLineageInconsistency(other)

private def mapAssetFailure(error: CatalogLookupError): GridCoordinateReconstructionFailure =
  error match
    case CatalogUnknownAsset(id) => GridCoordinateReconstructionFailure.UnknownAsset(id)
    case other                   => GridCoordinateReconstructionFailure.SnapshotLineageInconsistency(other)

private def mapGridFailure(
  identity: GridIdentity,
  error: CatalogLookupError
): GridCoordinateReconstructionFailure =
  error match
    case CatalogUnknownGrid(_)      => GridCoordinateReconstructionFailure.UnknownGrid(identity)
    case _: CatalogUnknownDimension => GridCoordinateReconstructionFailure.SnapshotLineageInconsistency(error)
    case _: ForeignDimensionHandle  => GridCoordinateReconstructionFailure.SnapshotLineageInconsistency(error)
    case _: CatalogUnknownAsset     => GridCoordinateReconstructionFailure.SnapshotLineageInconsistency(error)
