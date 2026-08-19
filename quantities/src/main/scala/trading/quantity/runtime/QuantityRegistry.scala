package trading.quantity.runtime

import scala.collection.mutable

import trading.quantity.AssetId
import trading.quantity.Atom
import trading.quantity.AtomId
import trading.quantity.Dimension
import trading.quantity.DimensionKey
import trading.quantity.DimRef
import trading.quantity.GridId
import trading.quantity.GridKey
import trading.quantity.GridQuantity
import trading.quantity.GridRef
import trading.quantity.GridVersion
import trading.quantity.Quantity
import trading.quantity.Rational
import trading.quantity.UniformGrid
import trading.quantity.refinement.*

/**
 * Registry-owned evidence that runtime dimension identity corresponds to type `D`.
 *
 * The registry provenance prevents a plain core witness—or a witness issued by another registry—from being substituted
 * even when its dimension key is equal.
 *
 * @tparam D the registered dimension
 */
sealed trait RegisteredDimensionRef[D <: Dimension]:
  def key: DimensionKey
  def asDimensionRef: DimRef[D]
  def sharesRegistryWith(r: RegisteredDimensionRef[? <: Dimension]): Boolean

/**
 * Registry-owned evidence for a grid resolved from its runtime identity.
 *
 * Its path-dependent type `G` turns the resolved grid identity into a compile-time distinction, preventing quantities
 * from different grids from being mixed accidentally.
 *
 * @tparam D the dimension inhabited by quantities on this grid
 */
sealed trait RegisteredGridRef[D <: Dimension]:
  /** Static identity of coordinates created by this registered grid. */
  type G

  def id: GridId
  def version: GridVersion
  def dimension: RegisteredDimensionRef[D]
  def quantum: PositiveRational
  def asGridRef: GridRef.Grid[D, G]

  final def key: GridKey =
    GridKey(id, version)

  final def fromCoordinate(c: BigInt): GridQuantity[D, G] =
    asGridRef.fromCoordinate(c)

  final def coordinate(v: GridQuantity[D, G]): BigInt =
    asGridRef.coordinate(v)

  final def asQuantity(v: GridQuantity[D, G]): Quantity[D] =
    asGridRef.asQuantity(v)
end RegisteredGridRef

/**
 * A dimension resolved from runtime identity together with its path-dependent type.
 *
 * The existential package preserves dimensional safety when the concrete dimension was not known at compile time.
 */
sealed trait DimensionWitness:
  /** Fresh static dimension represented by this runtime witness. */
  type D = Atom[this.type]
  def dimension: RegisteredDimensionRef[D]

/** A resolved runtime asset identity paired with its registry-owned dimension witness. */
sealed trait AssetRef extends DimensionWitness:
  def id: AssetId

/**
 * Resolves runtime asset, dimension, and grid identities into type-safe witnesses.
 *
 * Runtime identities allow values discovered from configuration or persisted data to participate in dimension-safe
 * arithmetic without source-defined phantom types.
 *
 * Identical definitions are interned, conflicting definitions are rejected, and every witness remains owned by the
 * registry that created it.
 */
final class QuantityRegistry:
  private final class InternedRegisteredDimensionRef[D <: Dimension](val asDimensionRef: DimRef[D])
    extends RegisteredDimensionRef[D]:
    private val registry  = QuantityRegistry.this
    val key: DimensionKey = asDimensionRef.key

    def sharesRegistryWith(r: RegisteredDimensionRef[? <: Dimension]): Boolean =
      r match
        case candidate: InternedRegisteredDimensionRef[?] => registry.eq(candidate.registry)
        case _                                            => false

  private final class InternedRegisteredGridRef[D <: Dimension, G0](
    val dimension: RegisteredDimensionRef[D],
    val asGridRef: GridRef.Grid[D, G0])
    extends RegisteredGridRef[D]:
    type G = G0
    val id: GridId                = asGridRef.id
    val version: GridVersion      = asGridRef.version
    val quantum: PositiveRational = asGridRef.quantum

  private final class InternedAssetRef(val id: AssetId, val dimensionAtom: AtomId) extends AssetRef:
    val dimension: RegisteredDimensionRef[D] =
      val generated = DimRef.atomic(dimensionAtom)
      new InternedRegisteredDimensionRef(generated.dimension.asInstanceOf[DimRef[D]])

  private final class InternedDimensionWitness(canonicalKey: DimensionKey) extends DimensionWitness:
    val dimension: RegisteredDimensionRef[D] =
      val generated = DimRef.fresh(canonicalKey)
      new InternedRegisteredDimensionRef(generated.dimension.asInstanceOf[DimRef[D]])

  private val assets     = mutable.Map.empty[AssetId, (AtomId, AssetRef)]
  private val dimensions = mutable.Map.empty[DimensionKey, DimensionWitness]
  private val grids      =
    mutable.Map.empty[DimensionKey, mutable.Map[GridKey, (Rational, RegisteredGridRef[? <: Dimension])]]

  private def isCanonical(w: DimensionWitness): Boolean =
    dimensions
      .get(w.dimension.key)
      .exists(canonical => canonical.asInstanceOf[AnyRef].eq(w.asInstanceOf[AnyRef]))

  def registerAsset(d: AssetDefinition): Either[RegistryError, AssetRef] =
    synchronized:
      assets.get(d.id) match
        case Some((existingAtom, witness)) if existingAtom == d.dimensionAtom =>
          Right(witness)
        case Some((existingAtom, _)) =>
          Left(ConflictingAssetDefinition(d.id, existingAtom, d.dimensionAtom))
        case None =>
          val dimensionKey = DimensionKey.atom(d.dimensionAtom)

          dimensions.get(dimensionKey) match
            case Some(_) =>
              Left(ConflictingDimensionRegistration(dimensionKey))
            case None =>
              val witness = new InternedAssetRef(d.id, d.dimensionAtom)
              val _       = assets.put(d.id, d.dimensionAtom -> witness)
              val _       = dimensions.put(witness.dimension.key, witness)
              Right(witness)

  def resolveAsset(id: AssetId): Either[RegistryError, AssetRef] =
    synchronized:
      assets
        .get(id)
        .map(_._2)
        .toRight(UnknownAsset(id))

  def registerDimension(key: DimensionKey): Either[RegistryError, DimensionWitness] =
    synchronized:
      dimensions.get(key) match
        case Some(witness) =>
          Right(witness)
        case None =>
          val witness = new InternedDimensionWitness(key)
          val _       = dimensions.put(key, witness)
          Right(witness)

  def resolveDimension(key: DimensionKey): Either[RegistryError, DimensionWitness] =
    synchronized:
      dimensions
        .get(key)
        .toRight(UnknownDimension(key))

  def registerGrid(w: DimensionWitness)(d: GridDefinition): Either[RegistryError, RegisteredGridRef[w.D]] =
    synchronized:
      if !isCanonical(w) then
        Left(ForeignDimensionWitness(w.dimension.key))
      else if d.dimension != w.dimension.key then
        Left(GridDimensionMismatch(w.dimension.key, d.dimension))
      else
        val dimensionGrids = grids.getOrElseUpdate(w.dimension.key, mutable.Map.empty)

        dimensionGrids.get(d.key) match
          case Some((existingQuantum, witness)) if existingQuantum == d.quantum.unrefined =>
            Right(witness.asInstanceOf[RegisteredGridRef[w.D]])
          case Some((existingQuantum, _)) =>
            Left(
              ConflictingGridDefinition(
                w.dimension.key,
                d.key,
                existingQuantum,
                d.quantum.unrefined
              )
            )
          case None =>
            val grid    = UniformGrid.create(d.id, d.version, w.dimension.asDimensionRef, d.quantum)
            val witness = new InternedRegisteredGridRef(w.dimension, grid)
            val _       = dimensionGrids.put(d.key, d.quantum.unrefined -> witness)
            Right(witness)

  def resolveGrid(w: DimensionWitness)(key: GridKey): Either[RegistryError, RegisteredGridRef[w.D]] =
    synchronized:
      if !isCanonical(w) then
        Left(ForeignDimensionWitness(w.dimension.key))
      else
        grids
          .get(w.dimension.key)
          .flatMap(_.get(key))
          .map((_, witness) => witness.asInstanceOf[RegisteredGridRef[w.D]])
          .toRight(UnknownGrid(w.dimension.key, key))

  def resolveGridForDecode(w: DimensionWitness)(key: GridKey): Either[RegistryError, RegisteredGridRef[w.D]] =
    synchronized:
      resolveGrid(w)(key) match
        case success @ Right(_)         => success
        case Left(unknown: UnknownGrid) =>
          val conflictingOwner =
            grids.iterator.collectFirst:
              case (dimension, dimensionGrids) if dimension != w.dimension.key && dimensionGrids.contains(key) =>
                dimension

          conflictingOwner match
            case Some(other) => Left(PackedGridDimensionMismatch(w.dimension.key, other, key))
            case None        => Left(unknown)
        case Left(error) =>
          Left(error)

  def registeredAssetCount: Int =
    synchronized:
      assets.size

  def registeredDimensionCount: Int =
    synchronized:
      dimensions.size

  def registeredGridCount: Int =
    synchronized:
      grids.valuesIterator.map(_.size).sum

end QuantityRegistry
