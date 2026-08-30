package trading.reference

import java.util.Objects

import trading.quantity.*

/** A catalog revision cannot be negative. */
final case class NegativeCatalogRevision(value: BigInt) extends JavaSerializationUnsupported:
  require(Objects.requireNonNull(value, "negative catalog revision").signum < 0, "catalog revision must be negative")

/** The requested registration transaction contained no commands. */
case object EmptyCatalogBatch extends JavaSerializationUnsupported

/** Arbitrary-precision, nonnegative catalog publication sequence. */
final class CatalogRevision private (val value: BigInt) extends JavaSerializationUnsupported:
  require(Objects.requireNonNull(value, "catalog revision").signum >= 0, "catalog revision must be nonnegative")

  override def equals(other: Any): Boolean =
    other match
      case that: CatalogRevision => value == that.value
      case _                     => false

  override def hashCode: Int    = value.hashCode
  override def toString: String = s"CatalogRevision($value)"
end CatalogRevision

object CatalogRevision:
  val zero: CatalogRevision = new CatalogRevision(BigInt(0))

  def from(value: BigInt): Either[NegativeCatalogRevision, CatalogRevision] =
    val checked = Objects.requireNonNull(value, "catalog revision")
    if checked.signum >= 0 then Right(new CatalogRevision(checked))
    else Left(NegativeCatalogRevision(checked))

  private[reference] def next(revision: CatalogRevision): CatalogRevision =
    new CatalogRevision(Objects.requireNonNull(revision, "catalog revision").value + 1)
end CatalogRevision

/** Closed, immutable catalog registration intent. */
enum CatalogCommand extends JavaSerializationUnsupported:
  case RegisterAsset(definition: AssetDefinition)
  case RegisterDimension(key: DimKey)
  case RegisterGrid(definition: GridDefinition)

  private val _ =
    this match
      case RegisterAsset(definition) => Objects.requireNonNull(definition, "asset definition")
      case RegisterDimension(key)    => Objects.requireNonNull(key, "dimension key")
      case RegisterGrid(definition)  => Objects.requireNonNull(definition, "grid definition")
end CatalogCommand

/** Guarded, ordered, non-empty catalog transaction. */
final class CatalogBatch private (
  val head: CatalogCommand,
  val tail: Vector[CatalogCommand])
  extends JavaSerializationUnsupported:
  Objects.requireNonNull(head, "catalog batch head")
  Objects.requireNonNull(tail, "catalog batch tail").foreach(command =>
    Objects.requireNonNull(command, "catalog batch command")
  )

  val commands: Vector[CatalogCommand] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: CatalogBatch => commands == that.commands
      case _                  => false

  override def hashCode: Int    = commands.hashCode
  override def toString: String = commands.mkString("CatalogBatch(", ",", ")")
end CatalogBatch

object CatalogBatch:
  def one(command: CatalogCommand): CatalogBatch =
    new CatalogBatch(Objects.requireNonNull(command, "catalog command"), Vector.empty)

  def of(head: CatalogCommand, tail: CatalogCommand*): CatalogBatch =
    new CatalogBatch(Objects.requireNonNull(head, "catalog batch head"), tail.toVector)

  def from(commands: Vector[CatalogCommand]): Either[EmptyCatalogBatch.type, CatalogBatch] =
    val checked = Objects.requireNonNull(commands, "catalog commands")
    checked.headOption match
      case Some(head) => Right(new CatalogBatch(head, checked.tail))
      case None       => Left(EmptyCatalogBatch)
end CatalogBatch

/** One append-only identity addition published by a catalog commit. */
enum CatalogAddition extends JavaSerializationUnsupported:
  case Dimension(key: DimKey)
  case Asset(id: AssetId)
  case Grid(identity: GridIdentity)

  private val _ =
    this match
      case Dimension(key) => Objects.requireNonNull(key, "added dimension")
      case Asset(id)      => Objects.requireNonNull(id, "added asset")
      case Grid(identity) => Objects.requireNonNull(identity, "added grid")
end CatalogAddition

/** Closed expected failures from checked publication-delta construction. */
sealed abstract class CatalogDeltaError extends JavaSerializationUnsupported with Product with Serializable

/** A published catalog delta cannot be empty. */
case object EmptyCatalogDelta extends CatalogDeltaError

/** A published catalog delta cannot repeat one addition. */
final case class DuplicateCatalogAddition(addition: CatalogAddition) extends CatalogDeltaError:
  Objects.requireNonNull(addition, "duplicate catalog addition")

/** Guarded, deterministically ordered, non-empty append-only publication delta. */
final class CatalogDelta private (
  val head: CatalogAddition,
  val tail: Vector[CatalogAddition])
  extends JavaSerializationUnsupported:
  Objects.requireNonNull(head, "catalog delta head")
  Objects.requireNonNull(tail, "catalog delta tail").foreach(addition =>
    Objects.requireNonNull(addition, "catalog delta addition")
  )

  val additions: Vector[CatalogAddition] = head +: tail
  require(additions.distinct.size == additions.size, "catalog delta additions must be unique")

  override def equals(other: Any): Boolean =
    other match
      case that: CatalogDelta => additions == that.additions
      case _                  => false

  override def hashCode: Int    = additions.hashCode
  override def toString: String = additions.mkString("CatalogDelta(", ",", ")")
end CatalogDelta

object CatalogDelta:
  def from(additions: Vector[CatalogAddition]): Either[CatalogDeltaError, CatalogDelta] =
    val checked = Objects.requireNonNull(additions, "catalog additions")
    checked.foreach(addition => Objects.requireNonNull(addition, "catalog delta addition"))
    val duplicate = checked.indices.collectFirst:
      case index if checked.take(index).contains(checked(index)) => checked(index)
    (checked.headOption, duplicate) match
      case (None, _)           => Left(EmptyCatalogDelta)
      case (_, Some(addition)) => Left(DuplicateCatalogAddition(addition))
      case (Some(head), None)  => Right(new CatalogDelta(head, checked.tail))

  private[reference] def nonEmpty(additions: Vector[CatalogAddition]): CatalogDelta =
    from(additions).fold(
      _ => throw new IllegalStateException("a published transition must contain an addition"),
      identity
    )
end CatalogDelta

/** Closed reasons why a complete catalog batch cannot be committed. */
enum CatalogViolation extends JavaSerializationUnsupported:
  case DuplicateAssetProposal(
    id: AssetId,
    commandIndices: Vector[Int],
    definitions: Vector[AssetDefinition])
  case DuplicateGridProposal(
    identity: GridIdentity,
    commandIndices: Vector[Int],
    definitions: Vector[GridDefinition])
  case ImmutableAssetConflict(
    id: AssetId,
    existing: AssetDefinition,
    supplied: AssetDefinition)
  case ImmutableGridConflict(
    identity: GridIdentity,
    existingQuantum: Rational,
    suppliedQuantum: Rational)
  case AssetDimensionAlreadyBound(
    dimension: DimKey,
    existingAsset: AssetId,
    suppliedAsset: AssetId)
  case MissingGridDimension(identity: GridIdentity)

  private val _ =
    this match
      case DuplicateAssetProposal(id, indices, definitions) =>
        Objects.requireNonNull(id, "asset ID")
        val checkedIndices = Objects.requireNonNull(indices, "duplicate asset indices")
        require(checkedIndices.sizeCompare(2) >= 0, "duplicate asset indices must identify a conflict")
        checkedIndices.foreach(index => require(index >= 0, "duplicate asset indices must be nonnegative"))
        require(checkedIndices == checkedIndices.sorted, "duplicate asset indices must be in command order")
        require(checkedIndices.distinct.size == checkedIndices.size,
          "duplicate asset indices must identify distinct commands")
        val checkedDefinitions = Objects.requireNonNull(definitions, "duplicate asset definitions")
        checkedDefinitions.foreach(definition => Objects.requireNonNull(definition, "duplicate asset definition"))
        require(checkedDefinitions.distinct.size == checkedDefinitions.size,
          "duplicate asset definitions must be distinct")
        require(checkedDefinitions.sizeCompare(2) >= 0, "duplicate asset definitions must conflict")
        require(checkedDefinitions.forall(_.id == id), "duplicate asset definitions must match the named asset ID")
        require(
          checkedIndices.sizeCompare(checkedDefinitions.size) >= 0,
          "duplicate asset indices must cover every conflicting definition"
        )
      case DuplicateGridProposal(identity, indices, definitions) =>
        Objects.requireNonNull(identity, "grid identity")
        val checkedIndices = Objects.requireNonNull(indices, "duplicate grid indices")
        require(checkedIndices.sizeCompare(2) >= 0, "duplicate grid indices must identify a conflict")
        checkedIndices.foreach(index => require(index >= 0, "duplicate grid indices must be nonnegative"))
        require(checkedIndices == checkedIndices.sorted, "duplicate grid indices must be in command order")
        require(checkedIndices.distinct.size == checkedIndices.size,
          "duplicate grid indices must identify distinct commands")
        val checkedDefinitions = Objects.requireNonNull(definitions, "duplicate grid definitions")
        checkedDefinitions.foreach(definition => Objects.requireNonNull(definition, "duplicate grid definition"))
        require(checkedDefinitions.distinct.size == checkedDefinitions.size,
          "duplicate grid definitions must be distinct")
        require(checkedDefinitions.sizeCompare(2) >= 0, "duplicate grid definitions must conflict")
        require(checkedDefinitions.forall(_.identity == identity),
          "duplicate grid definitions must match the named grid identity")
        require(
          checkedIndices.sizeCompare(checkedDefinitions.size) >= 0,
          "duplicate grid indices must cover every conflicting definition"
        )
      case ImmutableAssetConflict(id, existing, supplied) =>
        Objects.requireNonNull(id, "asset ID")
        val checkedExisting = Objects.requireNonNull(existing, "existing asset definition")
        val checkedSupplied = Objects.requireNonNull(supplied, "supplied asset definition")
        require(
          checkedExisting.id == id && checkedSupplied.id == id,
          "immutable asset conflict definitions must match the named asset ID"
        )
        require(checkedExisting != checkedSupplied, "immutable asset conflict definitions must differ")
      case ImmutableGridConflict(identity, existing, supplied) =>
        Objects.requireNonNull(identity, "grid identity")
        val checkedExisting = Objects.requireNonNull(existing, "existing grid quantum")
        val checkedSupplied = Objects.requireNonNull(supplied, "supplied grid quantum")
        require(checkedExisting.signum > 0, "existing grid conflict quantum must be positive")
        require(checkedSupplied.signum > 0, "supplied grid conflict quantum must be positive")
        require(checkedExisting != checkedSupplied, "immutable grid conflict quanta must differ")
      case AssetDimensionAlreadyBound(dimension, existing, supplied) =>
        val checkedDimension = Objects.requireNonNull(dimension, "asset dimension")
        val isAtomic         = checkedDimension.powers match
          case Vector((_, exponent)) => exponent == BigInt(1)
          case _                     => false
        require(isAtomic, "asset dimension binding conflicts require an atomic dimension")
        val checkedExisting = Objects.requireNonNull(existing, "existing asset ID")
        val checkedSupplied = Objects.requireNonNull(supplied, "supplied asset ID")
        require(checkedExisting != checkedSupplied, "asset dimension binding conflict assets must differ")
      case MissingGridDimension(identity) =>
        Objects.requireNonNull(identity, "grid identity")
end CatalogViolation

object CatalogViolation:
  private[reference] def expectedRuleOrdinal(violation: CatalogViolation): Int =
    Objects.requireNonNull(violation, "catalog violation") match
      case _: CatalogViolation.DuplicateAssetProposal     => 0
      case _: CatalogViolation.DuplicateGridProposal      => 0
      case _: CatalogViolation.ImmutableAssetConflict     => 1
      case _: CatalogViolation.ImmutableGridConflict      => 1
      case _: CatalogViolation.AssetDimensionAlreadyBound => 2
      case _: CatalogViolation.MissingGridDimension       => 3

  private[reference] def canonicalCommandIndex(violation: CatalogViolation): Option[Int] =
    Objects.requireNonNull(violation, "catalog violation") match
      case duplicate: CatalogViolation.DuplicateAssetProposal => Some(duplicate.commandIndices.head)
      case duplicate: CatalogViolation.DuplicateGridProposal  => Some(duplicate.commandIndices.head)
      case _                                                  => None
end CatalogViolation

/** A catalog violation with stable source-command and rule positions. */
final case class IndexedCatalogViolation(
  commandIndex: Int,
  ruleOrdinal: Int,
  violation: CatalogViolation)
  extends JavaSerializationUnsupported:
  require(commandIndex >= 0, "catalog command index must be nonnegative")
  require(ruleOrdinal >= 0, "catalog rule ordinal must be nonnegative")
  private val checkedViolation = Objects.requireNonNull(violation, "catalog violation")
  require(
    ruleOrdinal == CatalogViolation.expectedRuleOrdinal(checkedViolation),
    "catalog rule ordinal must match the violation kind"
  )
  CatalogViolation.canonicalCommandIndex(checkedViolation).foreach: canonicalIndex =>
    require(commandIndex == canonicalIndex, "catalog command index must match nested duplicate evidence")

/** Domain-owned, ordered, non-empty catalog failure collection. */
final class CatalogViolations private (
  permit: AnyRef,
  val head: IndexedCatalogViolation,
  val tail: Vector[IndexedCatalogViolation])
  extends JavaSerializationUnsupported:
  Objects.requireNonNull(head, "catalog violations head")
  Objects.requireNonNull(tail, "catalog violations tail").foreach(violation =>
    Objects.requireNonNull(violation, "catalog violation")
  )

  val violations: Vector[IndexedCatalogViolation] = head +: tail
  private val positions = violations.map(violation => (violation.commandIndex, violation.ruleOrdinal))
  require(positions == positions.sorted, "catalog violations must be in command and rule order")
  require(positions.distinct.size == positions.size, "catalog violation positions must be unique")
  private val _ =
    if CatalogState.isHandlePermit(permit) then ()
    else throw new IllegalArgumentException("catalog violation collections require model issuance")

  override def equals(other: Any): Boolean =
    other match
      case that: CatalogViolations => violations == that.violations
      case _                       => false

  override def hashCode: Int    = violations.hashCode
  override def toString: String = violations.mkString("CatalogViolations(", ",", ")")
end CatalogViolations

object CatalogViolations:
  private[reference] def ordered(
    permit: AnyRef,
    values: Vector[IndexedCatalogViolation]
  ): CatalogViolations =
    val sorted = values.sortBy(value => (value.commandIndex, value.ruleOrdinal))
    sorted.headOption match
      case Some(head) => new CatalogViolations(permit, head, sorted.tail)
      case None       => throw new IllegalStateException("catalog validation failures must be non-empty")
end CatalogViolations

/** Closed failures from resolving immutable catalog authority. */
sealed abstract class CatalogLookupError   extends JavaSerializationUnsupported with Product with Serializable
final case class UnknownAsset(id: AssetId) extends CatalogLookupError:
  Objects.requireNonNull(id, "unknown asset ID")
final case class UnknownDimension(key: DimKey) extends CatalogLookupError:
  Objects.requireNonNull(key, "unknown dimension key")
final case class UnknownGrid(identity: GridIdentity) extends CatalogLookupError:
  Objects.requireNonNull(identity, "unknown grid identity")
final case class ForeignDimensionHandle(key: DimKey) extends CatalogLookupError:
  Objects.requireNonNull(key, "foreign dimension key")

/** A generative catalog root. Calling [[initialState]] never changes its lineage. */
final class CatalogRoot private (
  permit: AnyRef,
  initialStateValue: CatalogState)
  extends JavaSerializationUnsupported:
  private val _ =
    if CatalogState.isHandlePermit(permit) then ()
    else throw new IllegalArgumentException("catalog roots require controlled creation")

  val initialState: CatalogState = Objects.requireNonNull(initialStateValue, "initial catalog state")
end CatalogRoot

object CatalogRoot:
  /** Establish a fresh in-memory lineage at an explicit outer boundary. */
  def create(): CatalogRoot = CatalogState.newRoot()

  private[reference] def issue(permit: AnyRef, initialState: CatalogState): CatalogRoot =
    new CatalogRoot(permit, initialState)
end CatalogRoot

/** One immutable catalog state in a root's append-only lineage. */
final class CatalogState private (
  permit: AnyRef,
  private val lineage: AnyRef,
  val revision: CatalogRevision,
  private val dimensions: Map[DimKey, DimensionHandle[? <: Dim]],
  private val assets: Map[AssetId, Asset],
  private val assetDefinitions: Map[AssetId, AssetDefinition],
  private val assetByDimension: Map[DimKey, AssetId],
  private val grids: Map[GridIdentity, GridHandle[? <: Dim]])
  extends JavaSerializationUnsupported:
  private val _ =
    if CatalogState.isHandlePermit(permit) then ()
    else throw new IllegalArgumentException("catalog states require controlled creation")

  Objects.requireNonNull(lineage, "catalog lineage")
  Objects.requireNonNull(revision, "catalog revision")
  Objects.requireNonNull(dimensions, "catalog dimensions")
  Objects.requireNonNull(assets, "catalog assets")
  Objects.requireNonNull(assetDefinitions, "catalog asset definitions")
  Objects.requireNonNull(assetByDimension, "catalog asset bindings")
  Objects.requireNonNull(grids, "catalog grids")

  def assetCount: Int     = assets.size
  def dimensionCount: Int = dimensions.size
  def gridCount: Int      = grids.size

  def snapshot: CatalogSnapshot =
    CatalogSnapshot.issue(
      permit,
      lineage,
      revision,
      dimensions,
      assets,
      grids
    )

  private[reference] def evaluate(batch: CatalogBatch): Either[CatalogViolations, CatalogTransition] =
    CatalogState.commitValues(
      this,
      permit,
      lineage,
      revision,
      dimensions,
      assets,
      assetDefinitions,
      assetByDimension,
      grids,
      batch
    )
end CatalogState

object CatalogState:
  private val handlePermit: AnyRef = new AnyRef

  private[reference] def isHandlePermit(candidate: AnyRef): Boolean =
    handlePermit.eq(candidate)

  private[reference] def newRoot(): CatalogRoot =
    val lineage = new AnyRef
    val initial = new CatalogState(
      handlePermit,
      lineage,
      CatalogRevision.zero,
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty,
      Map.empty
    )
    CatalogRoot.issue(handlePermit, initial)

  private def commitValues(
    state: CatalogState,
    permit: AnyRef,
    lineage: AnyRef,
    revision: CatalogRevision,
    dimensions: Map[DimKey, DimensionHandle[? <: Dim]],
    assets: Map[AssetId, Asset],
    assetDefinitions: Map[AssetId, AssetDefinition],
    assetByDimension: Map[DimKey, AssetId],
    grids: Map[GridIdentity, GridHandle[? <: Dim]],
    batch: CatalogBatch
  ): Either[CatalogViolations, CatalogTransition] =
    val indexed = batch.commands.zipWithIndex.map((command, index) => index -> command)

    val assetCommands = indexed.collect:
      case (index, CatalogCommand.RegisterAsset(definition)) => index -> definition
    val dimensionCommands = indexed.collect:
      case (index, CatalogCommand.RegisterDimension(key)) => index -> key
    val gridCommands = indexed.collect:
      case (index, CatalogCommand.RegisterGrid(definition)) => index -> definition

    val assetGroups = assetCommands.groupBy(_._2.id).values.toVector
    val gridGroups  = gridCommands.groupBy(_._2.identity).values.toVector

    val duplicateAssetViolations = assetGroups.flatMap: group =>
      val ordered     = group.sortBy(_._1)
      val definitions = ordered.map(_._2).distinct
      if definitions.sizeCompare(1) > 0 then
        Vector(
          IndexedCatalogViolation(
            ordered.head._1,
            0,
            CatalogViolation.DuplicateAssetProposal(
              ordered.head._2.id,
              ordered.map(_._1),
              definitions
            )
          )
        )
      else Vector.empty

    val duplicateGridViolations = gridGroups.flatMap: group =>
      val ordered     = group.sortBy(_._1)
      val definitions = ordered.map(_._2).distinct
      if definitions.sizeCompare(1) > 0 then
        Vector(
          IndexedCatalogViolation(
            ordered.head._1,
            0,
            CatalogViolation.DuplicateGridProposal(
              ordered.head._2.identity,
              ordered.map(_._1),
              definitions
            )
          )
        )
      else Vector.empty

    val conflictingAssetIds = duplicateAssetViolations.collect:
      case IndexedCatalogViolation(_, _, CatalogViolation.DuplicateAssetProposal(id, _, _)) => id
    val conflictingGridIdentities = duplicateGridViolations.collect:
      case IndexedCatalogViolation(_, _, CatalogViolation.DuplicateGridProposal(identity, _, _)) => identity

    val assetProposals = assetGroups
      .filterNot(group => conflictingAssetIds.contains(group.head._2.id))
      .map(_.minBy(_._1))
      .sortBy(_._1)
    val gridProposals = gridGroups
      .filterNot(group => conflictingGridIdentities.contains(group.head._2.identity))
      .map(_.minBy(_._1))
      .sortBy(_._1)

    val currentAssetViolations = assetProposals.flatMap: (index, supplied) =>
      assetDefinitions.get(supplied.id) match
        case Some(existing) if existing != supplied =>
          Vector(
            IndexedCatalogViolation(
              index,
              1,
              CatalogViolation.ImmutableAssetConflict(supplied.id, existing, supplied)
            )
          )
        case _ => Vector.empty

    val currentGridViolations = gridProposals.flatMap: (index, supplied) =>
      grids.get(supplied.identity) match
        case Some(existing) if existing.quantum.unrefined != supplied.quantum.unrefined =>
          Vector(
            IndexedCatalogViolation(
              index,
              1,
              CatalogViolation.ImmutableGridConflict(
                supplied.identity,
                existing.quantum.unrefined,
                supplied.quantum.unrefined
              )
            )
          )
        case _ => Vector.empty

    val currentAssetConflictIds = currentAssetViolations.collect:
      case IndexedCatalogViolation(_, _, CatalogViolation.ImmutableAssetConflict(id, _, _)) => id

    val currentGridConflictIds = currentGridViolations.collect:
      case IndexedCatalogViolation(_, _, CatalogViolation.ImmutableGridConflict(identity, _, _)) => identity

    val candidateAssets = assetProposals.filterNot((_, definition) => currentAssetConflictIds.contains(definition.id))

    val stateBindingViolations = candidateAssets.flatMap: (index, definition) =>
      val dimension = DimKey.atom(definition.dimensionAtom)
      assetByDimension.get(dimension) match
        case Some(existing) if existing != definition.id =>
          Vector(
            IndexedCatalogViolation(
              index,
              2,
              CatalogViolation.AssetDimensionAlreadyBound(dimension, existing, definition.id)
            )
          )
        case _ => Vector.empty

    val batchBindingViolations = candidateAssets
      .groupBy((_, definition) => DimKey.atom(definition.dimensionAtom))
      .filterNot((dimension, _) => assetByDimension.contains(dimension))
      .values
      .toVector
      .flatMap: group =>
        val ordered = group.sortBy(_._1)
        val first   = ordered.head
        ordered.tail.collect:
          case (index, definition) if definition.id != first._2.id =>
            IndexedCatalogViolation(
              index,
              2,
              CatalogViolation.AssetDimensionAlreadyBound(
                DimKey.atom(definition.dimensionAtom),
                first._2.id,
                definition.id
              )
            )

    val bindingConflictAssetIds = (stateBindingViolations ++ batchBindingViolations).flatMap:
      case IndexedCatalogViolation(_, _, CatalogViolation.AssetDimensionAlreadyBound(_, existing, supplied)) =>
        Vector(existing, supplied)
      case _ => Vector.empty

    val validAssetProposals = candidateAssets.filterNot((_, definition) =>
      bindingConflictAssetIds.contains(definition.id)
    )

    val standaloneDimensions  = dimensionCommands.groupBy(_._2).values.map(_.minBy(_._1)).toVector.sortBy(_._1)
    val coherentDimensionKeys =
      dimensions.keySet ++
        standaloneDimensions.map(_._2) ++
        validAssetProposals.map((_, definition) => DimKey.atom(definition.dimensionAtom))

    val failedPrerequisiteDimensions =
      assetProposals.diff(validAssetProposals).map((_, definition) => DimKey.atom(definition.dimensionAtom)).toSet ++
        duplicateAssetViolations.flatMap:
          case IndexedCatalogViolation(_, _, CatalogViolation.DuplicateAssetProposal(_, _, definitions)) =>
            definitions.map(definition => DimKey.atom(definition.dimensionAtom))
          case _ => Vector.empty
        --
        dimensions.keySet -- standaloneDimensions.map(_._2)

    val candidateGrids = gridProposals.filterNot((_, definition) =>
      currentGridConflictIds.contains(definition.identity)
    )
    val missingDimensionViolations = candidateGrids.flatMap: (index, definition) =>
      if coherentDimensionKeys.contains(definition.dimension) ||
        failedPrerequisiteDimensions.contains(definition.dimension)
      then Vector.empty
      else
        Vector(
          IndexedCatalogViolation(
            index,
            3,
            CatalogViolation.MissingGridDimension(definition.identity)
          )
        )

    val violations =
      duplicateAssetViolations ++ duplicateGridViolations ++ currentAssetViolations ++ currentGridViolations ++
        stateBindingViolations ++ batchBindingViolations ++ missingDimensionViolations

    if violations.nonEmpty then Left(CatalogViolations.ordered(handlePermit, violations))
    else
      val dimensionOrigins =
        (standaloneDimensions ++ validAssetProposals.map((index, definition) =>
          index -> DimKey.atom(definition.dimensionAtom)
        ))
          .groupBy(_._2)
          .values
          .map(_.minBy(_._1))
          .toVector
          .sortBy(_._1)

      val newDimensionOrigins = dimensionOrigins.filterNot((_, key) => dimensions.contains(key))
      val successorDimensions = newDimensionOrigins.foldLeft(dimensions):
        case (all, (_, key)) =>
          val generated = DimRef.fresh(key).dimension
          all.updated(key, DimensionHandle.issue(permit, lineage, generated))

      val newAssetProposals = validAssetProposals.filterNot((_, definition) => assets.contains(definition.id))
      val (successorAssets, successorAssetDefinitions, successorAssetByDimension) = newAssetProposals.foldLeft(
        (assets, assetDefinitions, assetByDimension)
      ):
        case ((allAssets, allDefinitions, allBindings), (_, definition)) =>
          val dimensionKey = DimKey.atom(definition.dimensionAtom)
          val dimension    = successorDimensions(dimensionKey)
          val asset        = Asset.issue(permit, lineage, definition.id, dimension)
          (
            allAssets.updated(definition.id, asset),
            allDefinitions.updated(definition.id, definition),
            allBindings.updated(dimensionKey, definition.id)
          )

      val newGridProposals = candidateGrids.filterNot((_, definition) => grids.contains(definition.identity))
      val successorGrids   = newGridProposals.foldLeft(grids):
        case (all, (_, definition)) =>
          val dimension = successorDimensions(definition.dimension)
          all.updated(definition.identity, issueGrid(permit, lineage, dimension, definition))

      val additions =
        (newDimensionOrigins.map((index, key) => (index, 0, CatalogAddition.Dimension(key))) ++
          newAssetProposals.map((index, definition) => (index, 1, CatalogAddition.Asset(definition.id))) ++
          newGridProposals.map((index, definition) => (index, 2, CatalogAddition.Grid(definition.identity))))
          .sortBy((index, ordinal, _) => (index, ordinal))
          .map(_._3)

      if additions.isEmpty then
        val snapshot = state.snapshot
        Right(
          CatalogTransition.issue(
            handlePermit,
            state,
            CatalogCommit.unchanged(handlePermit, snapshot)
          )
        )
      else
        val successor = new CatalogState(
          handlePermit,
          lineage,
          CatalogRevision.next(revision),
          successorDimensions,
          successorAssets,
          successorAssetDefinitions,
          successorAssetByDimension,
          successorGrids
        )
        val snapshot = successor.snapshot
        Right(
          CatalogTransition.issue(
            handlePermit,
            successor,
            CatalogCommit.published(handlePermit, snapshot, CatalogDelta.nonEmpty(additions))
          )
        )
      end if
    end if
  end commitValues

  private def issueGrid[D <: Dim](
    permit: AnyRef,
    lineage: AnyRef,
    dimension: DimensionHandle[D],
    definition: GridDefinition
  ): GridHandle[D] =
    val grid = UniformGrid.create(dimension.ref, definition.quantum)
    GridHandle.issue(permit, lineage, definition.identity, dimension, grid)
end CatalogState

/** Immutable direct-lookup view of exactly one catalog revision. */
final class CatalogSnapshot private (
  permit: AnyRef,
  private val lineage: AnyRef,
  val revision: CatalogRevision,
  private val dimensions: Map[DimKey, DimensionHandle[? <: Dim]],
  private val assets: Map[AssetId, Asset],
  private val grids: Map[GridIdentity, GridHandle[? <: Dim]])
  extends JavaSerializationUnsupported:
  private val _ =
    if CatalogState.isHandlePermit(permit) then ()
    else throw new IllegalArgumentException("catalog snapshots require state issuance")

  Objects.requireNonNull(lineage, "catalog lineage")
  Objects.requireNonNull(revision, "catalog revision")
  Objects.requireNonNull(dimensions, "catalog dimensions")
  Objects.requireNonNull(assets, "catalog assets")
  Objects.requireNonNull(grids, "catalog grids")

  def assetCount: Int     = assets.size
  def dimensionCount: Int = dimensions.size
  def gridCount: Int      = grids.size

  def resolveAsset(id: AssetId): Either[CatalogLookupError, Asset] =
    val checked = Objects.requireNonNull(id, "asset ID")
    assets.get(checked).toRight(UnknownAsset(checked))

  def resolveDimension(key: DimKey): Either[CatalogLookupError, DimensionHandle[? <: Dim]] =
    val checked = Objects.requireNonNull(key, "dimension key")
    dimensions.get(checked).toRight(UnknownDimension(checked))

  def resolveGrid(identity: GridIdentity): Either[CatalogLookupError, GridHandle[? <: Dim]] =
    val checked = Objects.requireNonNull(identity, "grid identity")
    grids.get(checked).toRight(UnknownGrid(checked))

  def resolveGrid[D <: Dim](
    dimension: DimensionHandle[D]
  )(
    key: GridKey
  ): Either[CatalogLookupError, GridHandle[D]] =
    val checkedDimension = Objects.requireNonNull(dimension, "dimension handle")
    val checkedKey       = Objects.requireNonNull(key, "grid key")
    dimensions.get(checkedDimension.key) match
      case None            => Left(UnknownDimension(checkedDimension.key))
      case Some(canonical) =>
        DimensionHandle.sameLineage(checkedDimension, canonical) match
          case Left(_)  => Left(ForeignDimensionHandle(checkedDimension.key))
          case Right(_) =>
            val identity = GridIdentity(checkedDimension.key, checkedKey)
            grids.get(identity) match
              case None         => Left(UnknownGrid(identity))
              case Some(handle) =>
                // The direct full-key lookup and checked canonical lineage establish that both handles use D.
                Right(handle.asInstanceOf[GridHandle[D]])
end CatalogSnapshot

object CatalogSnapshot:
  private[reference] def issue(
    permit: AnyRef,
    lineage: AnyRef,
    revision: CatalogRevision,
    dimensions: Map[DimKey, DimensionHandle[? <: Dim]],
    assets: Map[AssetId, Asset],
    grids: Map[GridIdentity, GridHandle[? <: Dim]]
  ): CatalogSnapshot =
    new CatalogSnapshot(permit, lineage, revision, dimensions, assets, grids)
end CatalogSnapshot

/** Observable result of a successful catalog commit. Construction remains owned by the pure catalog model. */
sealed abstract class CatalogCommit private[reference] (snapshotValue: CatalogSnapshot)
  extends JavaSerializationUnsupported with Product:
  final val snapshot: CatalogSnapshot = Objects.requireNonNull(snapshotValue, "catalog commit snapshot")
end CatalogCommit

object CatalogCommit:
  final class Unchanged private[reference] (
    permit: AnyRef,
    val snapshotValue: CatalogSnapshot)
    extends CatalogCommit(snapshotValue):
    private val _ =
      if CatalogState.isHandlePermit(permit) then ()
      else throw new IllegalArgumentException("unchanged catalog commits require model issuance")

    override def canEqual(other: Any): Boolean   = other.isInstanceOf[Unchanged]
    override def productArity: Int               = 1
    override def productPrefix: String           = "Unchanged"
    override def productElement(index: Int): Any =
      index match
        case 0 => snapshotValue
        case _ => throw new IndexOutOfBoundsException(index.toString)

    override def equals(other: Any): Boolean =
      other match
        case that: Unchanged => that.canEqual(this) && snapshotValue == that.snapshotValue
        case _               => false

    override def hashCode: Int    = scala.runtime.ScalaRunTime._hashCode(this)
    override def toString: String = scala.runtime.ScalaRunTime._toString(this)
  end Unchanged

  object Unchanged:
    def unapply(value: Unchanged): Some[CatalogSnapshot] = Some(value.snapshotValue)
  end Unchanged

  final class Published private[reference] (
    permit: AnyRef,
    val snapshotValue: CatalogSnapshot,
    val delta: CatalogDelta)
    extends CatalogCommit(snapshotValue):
    Objects.requireNonNull(delta, "catalog delta")

    private val _ =
      if CatalogState.isHandlePermit(permit) then ()
      else throw new IllegalArgumentException("published catalog commits require model issuance")

    override def canEqual(other: Any): Boolean   = other.isInstanceOf[Published]
    override def productArity: Int               = 2
    override def productPrefix: String           = "Published"
    override def productElement(index: Int): Any =
      index match
        case 0 => snapshotValue
        case 1 => delta
        case _ => throw new IndexOutOfBoundsException(index.toString)

    override def equals(other: Any): Boolean =
      other match
        case that: Published =>
          that.canEqual(this) && snapshotValue == that.snapshotValue && delta == that.delta
        case _ => false

    override def hashCode: Int    = scala.runtime.ScalaRunTime._hashCode(this)
    override def toString: String = scala.runtime.ScalaRunTime._toString(this)
  end Published

  object Published:
    def unapply(value: Published): Some[(CatalogSnapshot, CatalogDelta)] = Some((value.snapshotValue, value.delta))
  end Published

  private[reference] def unchanged(permit: AnyRef, snapshot: CatalogSnapshot): Unchanged =
    new Unchanged(permit, snapshot)

  private[reference] def published(
    permit: AnyRef,
    snapshot: CatalogSnapshot,
    delta: CatalogDelta
  ): Published =
    new Published(permit, snapshot, delta)
end CatalogCommit

/** Successful pure transition with the state to retain and its publication outcome. */
final class CatalogTransition private[reference] (
  permit: AnyRef,
  val state: CatalogState,
  val outcome: CatalogCommit)
  extends JavaSerializationUnsupported with Product:
  Objects.requireNonNull(state, "catalog transition state")
  Objects.requireNonNull(outcome, "catalog transition outcome")

  private val _ =
    if CatalogState.isHandlePermit(permit) then ()
    else throw new IllegalArgumentException("catalog transitions require model issuance")

  override def canEqual(other: Any): Boolean   = other.isInstanceOf[CatalogTransition]
  override def productArity: Int               = 2
  override def productPrefix: String           = "CatalogTransition"
  override def productElement(index: Int): Any =
    index match
      case 0 => state
      case 1 => outcome
      case _ => throw new IndexOutOfBoundsException(index.toString)

  override def equals(other: Any): Boolean =
    other match
      case that: CatalogTransition => that.canEqual(this) && state == that.state && outcome == that.outcome
      case _                       => false

  override def hashCode: Int    = scala.runtime.ScalaRunTime._hashCode(this)
  override def toString: String = scala.runtime.ScalaRunTime._toString(this)
end CatalogTransition

object CatalogTransition:
  def unapply(value: CatalogTransition): Some[(CatalogState, CatalogCommit)] = Some((value.state, value.outcome))

  private[reference] def issue(
    permit: AnyRef,
    state: CatalogState,
    outcome: CatalogCommit
  ): CatalogTransition =
    new CatalogTransition(permit, state, outcome)
end CatalogTransition

/** Normative pure catalog transition model. */
object CatalogModel:
  def commit(
    state: CatalogState,
    batch: CatalogBatch
  ): Either[CatalogViolations, CatalogTransition] =
    Objects.requireNonNull(state, "catalog state").evaluate(Objects.requireNonNull(batch, "catalog batch"))
end CatalogModel
