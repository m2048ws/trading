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
  permit: AnyRef,
  identityValue: GridIdentity,
  quantumValue: PositiveRational)
  extends JavaSerializationUnsupported:
  private val _ =
    if GridDefinition.isConstructionPermit(permit) then ()
    else throw new IllegalArgumentException("grid definitions require checked construction")

  final val identity: GridIdentity    = Objects.requireNonNull(identityValue, "grid identity")
  final val quantum: PositiveRational = GridDefinition.requirePositive(quantumValue)

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
  private val constructionPermit: AnyRef = new AnyRef

  private def isConstructionPermit(candidate: AnyRef): Boolean =
    constructionPermit.eq(candidate)

  private def requirePositive(value: PositiveRational): PositiveRational =
    val checked = Objects.requireNonNull(value, "grid quantum")
    PositiveRational(checked.unrefined).fold(
      _ => throw new IllegalArgumentException("grid quantum must be positive"),
      identity
    )

  /** Construct from an already refined quantum; erased JVM calls are defensively revalidated. */
  def apply(identity: GridIdentity, quantum: PositiveRational): GridDefinition =
    new GridDefinition(constructionPermit, Objects.requireNonNull(identity, "grid identity"), requirePositive(quantum))

  /** Checked raw/JVM boundary for expected invalid quantum input. */
  def from(
    identity: GridIdentity,
    quantum: Rational
  ): Either[NonPositiveGridQuantum, GridDefinition] =
    val checkedIdentity = Objects.requireNonNull(identity, "grid identity")
    val checkedQuantum  = Objects.requireNonNull(quantum, "grid quantum")
    PositiveRational(checkedQuantum)
      .left
      .map(_ => NonPositiveGridQuantum(checkedQuantum))
      .map(positive => new GridDefinition(constructionPermit, checkedIdentity, positive))
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
  permit: AnyRef,
  lineage: AnyRef,
  refValue: DimRef[D0])
  extends JavaSerializationUnsupported:
  private final val lineageToken: AnyRef =
    if CatalogState.isHandlePermit(permit) then Objects.requireNonNull(lineage, "issuer lineage")
    else throw new IllegalArgumentException("dimension handles require catalog issuance")

  type D = D0
  final val ref: DimRef[D] = Objects.requireNonNull(refValue, "dimension witness")
  final val key: DimKey    = ref.key

object DimensionHandle:
  private[reference] def issue[D <: Dim](permit: AnyRef, lineage: AnyRef, ref: DimRef[D]): DimensionHandle[D] =
    new DimensionHandle(permit, lineage, ref)

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
  permit: AnyRef,
  lineage: AnyRef,
  idValue: AssetId,
  dimensionValue: DimensionHandle[? <: Dim])
  extends JavaSerializationUnsupported:
  private final val lineageToken: AnyRef =
    if CatalogState.isHandlePermit(permit) then Objects.requireNonNull(lineage, "issuer lineage")
    else throw new IllegalArgumentException("asset handles require catalog issuance")

  final val dimensionBasis: DimensionHandle[? <: Dim] =
    Objects.requireNonNull(dimensionValue, "asset dimension")
  type D = dimensionBasis.D
  final val id: AssetId                   = Objects.requireNonNull(idValue, "asset ID")
  final val dimension: DimensionHandle[D] = dimensionBasis

object Asset:
  private[reference] def issue(
    permit: AnyRef,
    lineage: AnyRef,
    id: AssetId,
    dimension: DimensionHandle[? <: Dim]
  ): Asset =
    new Asset(permit, lineage, id, dimension)

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
  permit: AnyRef,
  lineage: AnyRef,
  identityValue: GridIdentity,
  dimensionValue: DimensionHandle[D0],
  gridValue: AnyRef)
  extends JavaSerializationUnsupported:
  private final val lineageToken: AnyRef =
    if CatalogState.isHandlePermit(permit) then Objects.requireNonNull(lineage, "issuer lineage")
    else throw new IllegalArgumentException("grid handles require catalog issuance")

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

  private val reconciliationPermit: AnyRef = new AnyRef

  extension [A <: Dim, G, B <: Dim, H](evidence: Reconciliation[A, G, B, H])
    def retype(value: GridQuantity[A, G]): GridQuantity[B, H] =
      if reconciliationPermit.eq(evidence) then value.asInstanceOf[GridQuantity[B, H]]
      else throw new IllegalArgumentException("grid reconciliation requires checked issuance")

  private[reference] def issue[D <: Dim](
    permit: AnyRef,
    lineage: AnyRef,
    identity: GridIdentity,
    dimension: DimensionHandle[D],
    grid: GridRef[D]
  ): GridHandle[D] =
    new GridHandle(permit, lineage, identity, dimension, grid)

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
    else Right(reconciliationPermit)
    end if
  end reconcile
end GridHandle
