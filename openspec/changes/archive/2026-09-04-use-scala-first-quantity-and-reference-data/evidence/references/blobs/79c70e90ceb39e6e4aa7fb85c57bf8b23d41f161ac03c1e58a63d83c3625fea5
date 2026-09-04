package trading.reference

import java.util.Objects

import trading.quantity.*
import trading.quantity.grid.GridError
import trading.quantity.refinement.PositiveRational

/** Closed reference-data failures for identity validation, construction, lookup, and pure handle reconciliation. */
sealed abstract class ReferenceDataError extends JavaSerializationUnsupported with Product with Serializable

/** An asset identifier contained no non-whitespace characters. */
case object EmptyAssetId extends ReferenceDataError

/** A grid identifier contained no non-whitespace characters. */
case object EmptyGridId extends ReferenceDataError

/** A stable grid version was zero or negative. */
final case class NonPositiveGridVersion(value: Long) extends ReferenceDataError

/** A stable grid definition supplied a zero or negative exact quantum. */
final case class NonPositiveGridQuantum(value: Rational) extends ReferenceDataError

/** Stable external identifier for an asset. */
final case class AssetId private (value: String) extends JavaSerializationUnsupported

object AssetId:
  override def fromProduct(product: Product): AssetId =
    throw new UnsupportedOperationException("use AssetId.from")

  /** Validate an external asset identifier. Null is rejected before a result is returned. */
  def from(value: String): Either[EmptyAssetId.type, AssetId] =
    val checked = Objects.requireNonNull(value, "asset ID")
    Either.cond(checked.trim.nonEmpty, new AssetId(checked), EmptyAssetId)

/** Stable external identifier for a grid definition. */
final case class GridId private (value: String) extends JavaSerializationUnsupported

object GridId:
  override def fromProduct(product: Product): GridId =
    throw new UnsupportedOperationException("use GridId.from")

  /** Validate an external grid identifier. Null is rejected before a result is returned. */
  def from(value: String): Either[EmptyGridId.type, GridId] =
    val checked = Objects.requireNonNull(value, "grid ID")
    Either.cond(checked.trim.nonEmpty, new GridId(checked), EmptyGridId)

/** Positive version distinguishing immutable definitions that share a [[GridId]]. */
final case class GridVersion private (value: Long) extends JavaSerializationUnsupported

object GridVersion:
  override def fromProduct(product: Product): GridVersion =
    throw new UnsupportedOperationException("use GridVersion.from")

  /** Validate a positive stable grid version. */
  def from(value: Long): Either[NonPositiveGridVersion, GridVersion] =
    Either.cond(value > 0, new GridVersion(value), NonPositiveGridVersion(value))

/** The dimension-local portion of stable grid identity. */
final case class GridKey(id: GridId, version: GridVersion) extends JavaSerializationUnsupported:
  val _ = Objects.requireNonNull(id, "grid ID")
  val _ = Objects.requireNonNull(version, "grid version")

/** Full stable grid identity: canonical dimension scope together with a dimension-local key. */
final case class GridIdentity(dimension: DimKey, key: GridKey) extends JavaSerializationUnsupported:
  val _ = Objects.requireNonNull(dimension, "grid dimension")
  val _ = Objects.requireNonNull(key, "grid key")

/** Immutable association between an external asset identity and the atom defining its dimension. */
final case class AssetDefinition(id: AssetId, dimensionAtom: AtomId) extends JavaSerializationUnsupported:
  val _ = Objects.requireNonNull(id, "asset ID")
  val _ = Objects.requireNonNull(dimensionAtom, "dimension atom")

/** Immutable stable grid definition over one anonymous mathematical quantum. */
final class GridDefinition private (
  identityValue: GridIdentity,
  quantumValue: PositiveRational)
  extends JavaSerializationUnsupported:
  final val identity: GridIdentity    = Objects.requireNonNull(identityValue, "grid identity")
  final val quantum: PositiveRational = Objects.requireNonNull(quantumValue, "grid quantum")

  def dimension: DimKey    = identity.dimension
  def key: GridKey         = identity.key
  def id: GridId           = key.id
  def version: GridVersion = key.version

  override def equals(other: Any): Boolean =
    other match
      case that: GridDefinition => identity == that.identity && quantum.unrefined == that.quantum.unrefined
      case _                    => false

  override def hashCode: Int = 31 * identity.hashCode + quantum.unrefined.hashCode

  override def toString: String = s"GridDefinition($identity,${quantum.unrefined})"
end GridDefinition

object GridDefinition:
  /** Construct directly from an already refined quantum. */
  def apply(identity: GridIdentity, quantum: PositiveRational): GridDefinition =
    new GridDefinition(identity, quantum)
end GridDefinition

final case class ForeignLineage(left: DimKey, right: DimKey)                         extends ReferenceDataError
final case class AssetIdentityMismatch(left: AssetId, right: AssetId)                extends ReferenceDataError
final case class StableGridIdentityMismatch(left: GridIdentity, right: GridIdentity) extends ReferenceDataError
final case class HandleDimensionMismatch(left: DimKey, right: DimKey)                extends ReferenceDataError
final case class ImmutableGridDefinitionConflict(
  identity: GridIdentity,
  leftQuantum: Rational,
  rightQuantum: Rational,
  cause: Option[GridError])
  extends ReferenceDataError

/** Trusted reference-data handle retaining one authoritative mathematical dimension and opaque issuer lineage. */
final class DimensionHandle[D0 <: Dim] private (
  lineage: AnyRef,
  refValue: DimRef[D0])
  extends JavaSerializationUnsupported:
  private final val lineageToken: AnyRef = Objects.requireNonNull(lineage, "issuer lineage")

  type D = D0
  final val ref: DimRef[D] = Objects.requireNonNull(refValue, "dimension witness")
  final val key: DimKey    = ref.key

object DimensionHandle:
  private[reference] def issue[D <: Dim](lineage: AnyRef, ref: DimRef[D]): DimensionHandle[D] =
    new DimensionHandle(lineage, ref)

  /** Pure issuer-lineage check that grants no dimension or construction authority. */
  def sameLineage[A <: Dim, B <: Dim](
    left: DimensionHandle[A],
    right: DimensionHandle[B]
  ): Either[ReferenceDataError, Unit] =
    val checkedLeft  = Objects.requireNonNull(left, "left dimension handle")
    val checkedRight = Objects.requireNonNull(right, "right dimension handle")
    if checkedLeft.lineageToken.eq(checkedRight.lineageToken) then Right(())
    else Left(ForeignLineage(checkedLeft.key, checkedRight.key))

  /** Reconcile issuer lineage and canonical dimension before issuing ordinary quantity evidence. */
  def reconcile[A <: Dim, B <: Dim](
    left: DimensionHandle[A],
    right: DimensionHandle[B]
  ): Either[ReferenceDataError, SameDimension[A, B]] =
    sameLineage(left, right).flatMap: _ =>
      SameDimension
        .between(left.ref, right.ref)
        .toRight(HandleDimensionMismatch(left.key, right.key))
end DimensionHandle

/** Trusted stable asset identity with one path-dependent dimension handle. */
final class Asset private (
  lineage: AnyRef,
  idValue: AssetId,
  dimensionValue: DimensionHandle[? <: Dim])
  extends JavaSerializationUnsupported:
  private final val lineageToken: AnyRef = Objects.requireNonNull(lineage, "issuer lineage")

  final val dimensionBasis: DimensionHandle[? <: Dim] =
    Objects.requireNonNull(dimensionValue, "asset dimension")
  type D = dimensionBasis.D
  final val id: AssetId                   = Objects.requireNonNull(idValue, "asset ID")
  final val dimension: DimensionHandle[D] = dimensionBasis

object Asset:
  private[reference] def issue(
    lineage: AnyRef,
    id: AssetId,
    dimension: DimensionHandle[? <: Dim]
  ): Asset =
    new Asset(lineage, id, dimension)

  /** Reconcile stable asset identity and dimension without consulting live state. */
  def reconcile(left: Asset, right: Asset): Either[ReferenceDataError, SameDimension[left.D, right.D]] =
    val _ = Objects.requireNonNull(left, "left asset")
    val _ = Objects.requireNonNull(right, "right asset")
    if !left.lineageToken.eq(right.lineageToken) then
      Left(ForeignLineage(left.dimension.key, right.dimension.key))
    else if left.id != right.id then
      Left(AssetIdentityMismatch(left.id, right.id))
    else
      DimensionHandle.reconcile(left.dimension, right.dimension)

/** Trusted stable grid identity composed around one retained anonymous mathematical grid. */
final class GridHandle[D0 <: Dim] private (
  lineage: AnyRef,
  identityValue: GridIdentity,
  dimensionValue: DimensionHandle[D0],
  gridValue: AnyRef)
  extends JavaSerializationUnsupported:
  private final val lineageToken: AnyRef = Objects.requireNonNull(lineage, "issuer lineage")

  type D = D0
  final val identity: GridIdentity        = Objects.requireNonNull(identityValue, "grid identity")
  final val dimension: DimensionHandle[D] = Objects.requireNonNull(dimensionValue, "grid dimension")
  final val gridBasis: GridRef[D]         =
    Objects.requireNonNull(gridValue, "mathematical grid").asInstanceOf[GridRef[D]]
  type G = gridBasis.G
  final val grid: GridRef.Grid[D, G] = gridBasis

  final def key: GridKey              = identity.key
  final def id: GridId                = key.id
  final def version: GridVersion      = key.version
  final def quantum: PositiveRational = grid.quantum

  final def fromCoordinate(coordinate: BigInt): GridQuantity[D, G] =
    grid.fromCoordinate(coordinate)

  final def coordinate(value: GridQuantity[D, G]): BigInt =
    grid.coordinate(value)

  final def asQuantity(value: GridQuantity[D, G]): Quantity[D] =
    grid.asQuantity(value)
end GridHandle

object GridHandle:
  type Grid[D <: Dim, G0]                              = GridHandle[D] { type G = G0 }
  opaque type Reconciliation[A <: Dim, G, B <: Dim, H] = AnyRef

  extension [A <: Dim, G, B <: Dim, H](evidence: Reconciliation[A, G, B, H])
    def retype(value: GridQuantity[A, G]): GridQuantity[B, H] =
      val _ = Objects.requireNonNull(evidence, "grid reconciliation evidence")
      value.asInstanceOf[GridQuantity[B, H]]

  private[reference] def issue[D <: Dim](
    lineage: AnyRef,
    identity: GridIdentity,
    dimension: DimensionHandle[D],
    grid: GridRef[D]
  ): GridHandle[D] =
    new GridHandle(lineage, identity, dimension, grid)

  /** Reconcile lineage, full stable identity, dimension, and immutable definition before same-grid retyping. */
  def reconcile[A <: Dim, B <: Dim](
    left: GridHandle[A],
    right: GridHandle[B]
  ): Either[ReferenceDataError, Reconciliation[A, left.G, B, right.G]] =
    val _ = Objects.requireNonNull(left, "left grid handle")
    val _ = Objects.requireNonNull(right, "right grid handle")
    if !left.lineageToken.eq(right.lineageToken) then
      Left(ForeignLineage(left.dimension.key, right.dimension.key))
    else if left.identity != right.identity then
      Left(StableGridIdentityMismatch(left.identity, right.identity))
    else if left.dimension.key != right.dimension.key then
      Left(HandleDimensionMismatch(left.dimension.key, right.dimension.key))
    else if left.quantum.unrefined != right.quantum.unrefined then
      Left(
        ImmutableGridDefinitionConflict(
          left.identity,
          left.quantum.unrefined,
          right.quantum.unrefined,
          None
        )
      )
    else Right(left.identity)
    end if
  end reconcile
end GridHandle
