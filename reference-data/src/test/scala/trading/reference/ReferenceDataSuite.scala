package trading.reference

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.*

class ReferenceDataSuite extends FunSuite:

  private val cent = PositiveRational.exact(1, 100).toOption.get

  private def validAssetId(value: String): AssetId =
    AssetId.from(value).fold(error => fail(s"expected valid asset ID, got $error"), identity)

  private def validGridId(value: String): GridId =
    GridId.from(value).fold(error => fail(s"expected valid grid ID, got $error"), identity)

  private def validGridVersion(value: Long): GridVersion =
    GridVersion.from(value).fold(error => fail(s"expected valid grid version, got $error"), identity)

  private def assetDefinition(name: String, atom: String = ""): AssetDefinition =
    AssetDefinition(validAssetId(name), AtomId(if atom.isEmpty then s"asset:$name" else atom))

  private def gridDefinition(
    dimension: DimKey,
    name: String,
    version: Long = 1,
    quantum: PositiveRational = cent
  ): GridDefinition =
    GridDefinition(
      GridIdentity(dimension, GridKey(validGridId(name), validGridVersion(version))),
      quantum
    )

  private def published(state: CatalogState, commands: CatalogCommand*): CatalogTransition =
    CatalogModel
      .commit(state, CatalogBatch.of(commands.head, commands.tail*))
      .fold(errors => fail(s"expected published catalog transition, got $errors"), identity)

  private def rejectSerialization(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()

  test("stable identifiers are validated and full grid identity is dimension scoped"):
    val _ = intercept[NullPointerException](AssetId.from(null))
    assertEquals(AssetId.from("  "), Left(EmptyAssetId))
    val _ = intercept[NullPointerException](GridId.from(null))
    assertEquals(GridId.from(""), Left(EmptyGridId))
    assertEquals(GridVersion.from(0), Left(NonPositiveGridVersion(0)))
    assertEquals(GridVersion.from(-1), Left(NonPositiveGridVersion(-1)))

    val firstAssetId  = validAssetId("USD")
    val secondAssetId = validAssetId("USD")
    assertEquals(firstAssetId, secondAssetId)
    assertEquals(firstAssetId.hashCode, secondAssetId.hashCode)
    assertEquals(firstAssetId.toString, "AssetId(USD)")

    val version = validGridVersion(1)
    val gridId  = validGridId("cent")
    val _       = intercept[NullPointerException](GridKey(null, version))
    val _       = intercept[NullPointerException](GridIdentity(null, GridKey(gridId, version)))

    val local = GridKey(gridId, version)
    assertNotEquals(GridIdentity(DimKey.one, local), GridIdentity(DimKey.atom(AtomId("USD")), local))

  test("catalog public values are guarded products and fail closed under Java serialization"):
    val definition = assetDefinition("products")
    val command    = CatalogCommand.RegisterAsset(definition)
    val batch      = CatalogBatch.one(command)
    assertEquals(CatalogBatch.from(Vector.empty), Left(EmptyCatalogBatch))
    assertEquals(batch.commands, Vector(command))
    assertEquals(CatalogRevision.from(BigInt(-1)), Left(NegativeCatalogRevision(BigInt(-1))))
    assertEquals(CatalogRevision.from(BigInt(17)).map(_.value), Right(BigInt(17)))
    assertEquals(CatalogDelta.from(Vector.empty), Left(EmptyCatalogDelta))

    val transition = published(CatalogRoot.create().initialState, command)
    val commit     = transition.outcome
    val delta      = commit match
      case CatalogCommit.Published(_, value) => value
      case other                             => fail(s"expected published commit, got $other")

    assertEquals(
      delta.additions,
      Vector(
        CatalogAddition.Dimension(DimKey.atom(definition.dimensionAtom)),
        CatalogAddition.Asset(definition.id)
      )
    )
    assertEquals(commit.snapshot.revision.value, BigInt(1))

    val gridDefinitionValue = gridDefinition(DimKey.atom(definition.dimensionAtom), "products-grid")
    val conflict            = CatalogModel
      .commit(
        transition.state,
        CatalogBatch.one(
          CatalogCommand.RegisterAsset(AssetDefinition(definition.id, AtomId("products:conflict")))
        )
      )
      .left
      .toOption
      .get
    val indexedViolation = conflict.head
    val lookupError      = transition.state.snapshot.resolveAsset(validAssetId("products-unknown")).left.toOption.get

    val serializableValues: Vector[JavaSerializationUnsupported] = Vector(
      definition.id,
      definition,
      command,
      CatalogCommand.RegisterDimension(DimKey.one),
      CatalogCommand.RegisterGrid(gridDefinitionValue),
      batch,
      CatalogRevision.zero,
      NegativeCatalogRevision(BigInt(-1)),
      EmptyCatalogBatch,
      EmptyCatalogDelta,
      DuplicateCatalogAddition(CatalogAddition.Asset(definition.id)),
      CatalogAddition.Dimension(DimKey.one),
      CatalogAddition.Asset(definition.id),
      CatalogAddition.Grid(gridDefinitionValue.identity),
      delta,
      indexedViolation.violation,
      indexedViolation,
      conflict,
      lookupError,
      commit,
      transition,
      transition.state,
      transition.state.snapshot,
      CatalogRoot.create()
    )
    serializableValues.foreach(rejectSerialization)

  test("catalog error values reject null and malformed nested payloads"):
    val firstAsset  = assetDefinition("guarded-first", "asset:guarded-first")
    val secondAsset = AssetDefinition(firstAsset.id, AtomId("asset:guarded-second"))
    val thirdAsset  = AssetDefinition(firstAsset.id, AtomId("asset:guarded-third"))
    val firstGrid   = gridDefinition(DimKey.atom(firstAsset.dimensionAtom), "guarded-grid")
    val secondGrid  = gridDefinition(
      firstGrid.dimension,
      "guarded-grid",
      quantum = PositiveRational.exact(1, 1000).toOption.get
    )
    val thirdGrid = gridDefinition(
      firstGrid.dimension,
      "guarded-grid",
      quantum = PositiveRational.exact(1, 10000).toOption.get
    )
    val otherAsset = assetDefinition("guarded-other", "asset:guarded-other")
    val otherGrid  = gridDefinition(
      firstGrid.dimension,
      "guarded-other-grid"
    )
    val conflictingOtherGrid = gridDefinition(
      otherGrid.dimension,
      "guarded-other-grid",
      quantum = PositiveRational.exact(1, 1000).toOption.get
    )
    val transition           = published(CatalogRoot.create().initialState, CatalogCommand.RegisterAsset(firstAsset))
    val snapshot             = transition.outcome.snapshot
    val nullAssetId: AssetId = null
    val nullAssetDefinition: AssetDefinition          = null
    val nullGridDefinition: GridDefinition            = null
    val nullDimension: DimKey                         = null
    val nullGridIdentity: GridIdentity                = null
    val nullCommand: CatalogCommand                   = null
    val nullAddition: CatalogAddition                 = null
    val nullViolation: CatalogViolation               = null
    val nullIndices: Vector[Int]                      = null
    val nullAssetDefinitions: Vector[AssetDefinition] = null
    val nullGridDefinitions: Vector[GridDefinition]   = null

    val _ = intercept[NullPointerException](NegativeCatalogRevision(null))
    val _ = intercept[IllegalArgumentException](NegativeCatalogRevision(BigInt(0)))
    val _ = intercept[NullPointerException](CatalogRevision.from(null))
    val _ = intercept[NullPointerException](CatalogCommand.RegisterAsset(nullAssetDefinition))
    val _ = intercept[NullPointerException](CatalogCommand.RegisterDimension(nullDimension))
    val _ = intercept[NullPointerException](CatalogCommand.RegisterGrid(nullGridDefinition))
    val _ =
      intercept[NullPointerException](CatalogBatch.from(Vector(CatalogCommand.RegisterAsset(firstAsset), nullCommand)))
    val _ = intercept[NullPointerException](CatalogAddition.Dimension(nullDimension))
    val _ = intercept[NullPointerException](CatalogAddition.Asset(nullAssetId))
    val _ = intercept[NullPointerException](CatalogAddition.Grid(nullGridIdentity))
    val _ =
      intercept[NullPointerException](CatalogDelta.from(Vector(CatalogAddition.Asset(firstAsset.id), nullAddition)))
    assertEquals(
      CatalogDelta.from(Vector(CatalogAddition.Asset(firstAsset.id), CatalogAddition.Asset(firstAsset.id))),
      Left(DuplicateCatalogAddition(CatalogAddition.Asset(firstAsset.id)))
    )
    val _          = intercept[NullPointerException](DuplicateCatalogAddition(nullAddition))
    val validDelta = CatalogDelta
      .from(
        Vector(
          CatalogAddition.Dimension(DimKey.atom(firstAsset.dimensionAtom)),
          CatalogAddition.Asset(firstAsset.id)
        )
      )
      .toOption
      .get
    assertEquals(validDelta.additions.size, 2)

    val _ = intercept[NullPointerException](
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, nullIndices, Vector(firstAsset, secondAsset))
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 1), nullAssetDefinitions)
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 1), Vector(firstAsset, nullAssetDefinition))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0), Vector(firstAsset, secondAsset))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(-1, 1), Vector(firstAsset, secondAsset))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 1), Vector(firstAsset, firstAsset))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 0), Vector(firstAsset, secondAsset))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(3, 0), Vector(firstAsset, secondAsset))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 1), Vector(otherAsset, firstAsset))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateAssetProposal(
        firstAsset.id,
        Vector(0, 1, 2),
        Vector(firstAsset, secondAsset, firstAsset)
      )
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 1), Vector(firstAsset, secondAsset, thirdAsset))
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.DuplicateGridProposal(firstGrid.identity, nullIndices, Vector(firstGrid, secondGrid))
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(0, 1), nullGridDefinitions)
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(0, 1), Vector(firstGrid, nullGridDefinition))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(0), Vector(firstGrid, secondGrid))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(-1, 1), Vector(firstGrid, secondGrid))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(0, 1), Vector(firstGrid, firstGrid))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(0, 0), Vector(firstGrid, secondGrid))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(3, 0), Vector(firstGrid, secondGrid))
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateGridProposal(
        firstGrid.identity,
        Vector(0, 1),
        Vector(otherGrid, conflictingOtherGrid)
      )
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateGridProposal(
        firstGrid.identity,
        Vector(0, 1, 2),
        Vector(firstGrid, secondGrid, firstGrid)
      )
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.DuplicateGridProposal(
        firstGrid.identity,
        Vector(0, 1),
        Vector(firstGrid, secondGrid, thirdGrid)
      )
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.ImmutableAssetConflict(nullAssetId, firstAsset, secondAsset)
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.ImmutableAssetConflict(firstAsset.id, nullAssetDefinition, secondAsset)
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.ImmutableAssetConflict(firstAsset.id, firstAsset, nullAssetDefinition)
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.ImmutableAssetConflict(firstAsset.id, firstAsset, firstAsset)
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.ImmutableAssetConflict(firstAsset.id, otherAsset, secondAsset)
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.ImmutableGridConflict(nullGridIdentity, firstGrid.quantum.unrefined,
        secondGrid.quantum.unrefined)
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.ImmutableGridConflict(firstGrid.identity, null, secondGrid.quantum.unrefined)
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.ImmutableGridConflict(firstGrid.identity, firstGrid.quantum.unrefined, null)
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.ImmutableGridConflict(
        firstGrid.identity,
        firstGrid.quantum.unrefined,
        firstGrid.quantum.unrefined
      )
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.ImmutableGridConflict(firstGrid.identity, Rational.zero, secondGrid.quantum.unrefined)
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.ImmutableGridConflict(firstGrid.identity, firstGrid.quantum.unrefined, -Rational.one)
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.AssetDimensionAlreadyBound(nullDimension, firstAsset.id, secondAsset.id)
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.AssetDimensionAlreadyBound(firstGrid.dimension, nullAssetId, secondAsset.id)
    )
    val _ = intercept[NullPointerException](
      CatalogViolation.AssetDimensionAlreadyBound(firstGrid.dimension, firstAsset.id, nullAssetId)
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.AssetDimensionAlreadyBound(firstGrid.dimension, firstAsset.id, firstAsset.id)
    )
    val _ = intercept[IllegalArgumentException](
      CatalogViolation.AssetDimensionAlreadyBound(DimKey.one, firstAsset.id, secondAsset.id)
    )
    val compositeDimension = DimKey.multiply(firstGrid.dimension, DimKey.atom(otherAsset.dimensionAtom))
    val _                  = intercept[IllegalArgumentException](
      CatalogViolation.AssetDimensionAlreadyBound(compositeDimension, firstAsset.id, secondAsset.id)
    )

    assertEquals(
      CatalogViolation.DuplicateAssetProposal(firstAsset.id, Vector(0, 1), Vector(firstAsset, secondAsset)).id,
      firstAsset.id
    )
    assertEquals(
      CatalogViolation.DuplicateGridProposal(firstGrid.identity, Vector(0, 1), Vector(firstGrid, secondGrid)).identity,
      firstGrid.identity
    )
    assertEquals(
      CatalogViolation.ImmutableAssetConflict(firstAsset.id, firstAsset, secondAsset).id,
      firstAsset.id
    )
    assertEquals(
      CatalogViolation
        .ImmutableGridConflict(firstGrid.identity, firstGrid.quantum.unrefined, secondGrid.quantum.unrefined)
        .identity,
      firstGrid.identity
    )
    assertEquals(
      CatalogViolation.AssetDimensionAlreadyBound(firstGrid.dimension, firstAsset.id, otherAsset.id).dimension,
      firstGrid.dimension
    )
    val _ = intercept[NullPointerException](CatalogViolation.MissingGridDimension(nullGridIdentity))
    val _ = intercept[IllegalArgumentException](
      IndexedCatalogViolation(-1, 0, CatalogViolation.MissingGridDimension(firstGrid.identity))
    )
    val _ = intercept[IllegalArgumentException](
      IndexedCatalogViolation(0, -1, CatalogViolation.MissingGridDimension(firstGrid.identity))
    )
    val _ = intercept[NullPointerException](IndexedCatalogViolation(0, 0, nullViolation))
    val _ = intercept[IllegalArgumentException](
      IndexedCatalogViolation(0, 99, CatalogViolation.MissingGridDimension(firstGrid.identity))
    )
    val _ = intercept[IllegalArgumentException](
      IndexedCatalogViolation(0, 0, CatalogViolation.MissingGridDimension(firstGrid.identity))
    )
    val duplicate = CatalogViolation.DuplicateAssetProposal(
      firstAsset.id,
      Vector(0, 2),
      Vector(firstAsset, secondAsset)
    )
    val _            = intercept[IllegalArgumentException](IndexedCatalogViolation(9, 0, duplicate))
    val validIndexed = Vector(
      IndexedCatalogViolation(0, 0, duplicate),
      IndexedCatalogViolation(
        1,
        1,
        CatalogViolation.ImmutableAssetConflict(firstAsset.id, firstAsset, secondAsset)
      ),
      IndexedCatalogViolation(
        2,
        2,
        CatalogViolation.AssetDimensionAlreadyBound(firstGrid.dimension, firstAsset.id, otherAsset.id)
      ),
      IndexedCatalogViolation(3, 3, CatalogViolation.MissingGridDimension(firstGrid.identity))
    )
    assertEquals(validIndexed.map(_.ruleOrdinal), Vector(0, 1, 2, 3))
    val _ = intercept[NullPointerException](UnknownAsset(nullAssetId))
    val _ = intercept[NullPointerException](UnknownDimension(nullDimension))
    val _ = intercept[NullPointerException](UnknownGrid(nullGridIdentity))
    val _ = intercept[NullPointerException](ForeignDimensionHandle(nullDimension))
    val _ = intercept[NullPointerException](new CatalogCommit.Unchanged(null))
    val _ = intercept[NullPointerException](new CatalogCommit.Published(snapshot, null))
    val _ = intercept[NullPointerException](new CatalogTransition(null, transition.outcome))
    val _ = intercept[NullPointerException](new CatalogTransition(transition.state, null))

  test("mixed batches are order independent, atomic, revisioned once, and idempotent"):
    val root      = CatalogRoot.create()
    val asset     = assetDefinition("USD-order")
    val dimension = DimKey.atom(asset.dimensionAtom)
    val grid      = gridDefinition(dimension, "usd-order-cent")
    val forward   = CatalogBatch.of(
      CatalogCommand.RegisterGrid(grid),
      CatalogCommand.RegisterAsset(asset),
      CatalogCommand.RegisterDimension(dimension)
    )
    val reverse = CatalogBatch.of(
      CatalogCommand.RegisterDimension(dimension),
      CatalogCommand.RegisterAsset(asset),
      CatalogCommand.RegisterGrid(grid)
    )

    val forwardTransition = CatalogModel.commit(root.initialState, forward).toOption.get
    val reverseTransition = CatalogModel.commit(root.initialState, reverse).toOption.get
    assertEquals(forwardTransition.state.revision.value, BigInt(1))
    assertEquals(reverseTransition.state.revision.value, BigInt(1))
    assertEquals(forwardTransition.outcome.snapshot.assetCount, 1)
    assertEquals(forwardTransition.outcome.snapshot.dimensionCount, 1)
    assertEquals(forwardTransition.outcome.snapshot.gridCount, 1)

    val forwardAsset = forwardTransition.outcome.snapshot.resolveAsset(asset.id).toOption.get
    val reverseAsset = reverseTransition.outcome.snapshot.resolveAsset(asset.id).toOption.get
    assert(Asset.reconcile(forwardAsset, reverseAsset).isRight)
    val forwardGrid = forwardTransition.outcome.snapshot.resolveGrid(grid.identity).toOption.get
    val reverseGrid = reverseTransition.outcome.snapshot.resolveGrid(grid.identity).toOption.get
    assert(GridHandle.reconcile(forwardGrid, reverseGrid).isRight)

    val retry = CatalogModel.commit(forwardTransition.state, reverse).toOption.get
    assert(retry.state.eq(forwardTransition.state))
    assertEquals(retry.state.revision.value, BigInt(1))
    assert(retry.outcome.isInstanceOf[CatalogCommit.Unchanged])

  test("validation accumulates independent conflicts and leaves the input unchanged"):
    val usd          = assetDefinition("USD-conflict")
    val usdDimension = DimKey.atom(usd.dimensionAtom)
    val cents        = gridDefinition(usdDimension, "usd-conflict-cent")
    val initial      = published(
      CatalogRoot.create().initialState,
      CatalogCommand.RegisterAsset(usd),
      CatalogCommand.RegisterGrid(cents)
    ).state
    val valid           = assetDefinition("otherwise-valid")
    val unknownGrid     = gridDefinition(DimKey.atom(AtomId("missing:dimension")), "missing-grid")
    val conflictingGrid = gridDefinition(
      usdDimension,
      "usd-conflict-cent",
      quantum = PositiveRational.exact(3, 100).toOption.get
    )
    val batch = CatalogBatch.of(
      CatalogCommand.RegisterAsset(AssetDefinition(usd.id, AtomId("asset:changed"))),
      CatalogCommand.RegisterGrid(conflictingGrid),
      CatalogCommand.RegisterGrid(unknownGrid),
      CatalogCommand.RegisterAsset(valid)
    )

    val failure = CatalogModel.commit(initial, batch).left.toOption.get
    assertEquals(failure.violations.map(_.commandIndex), Vector(0, 1, 2))
    assert(failure.violations(0).violation.isInstanceOf[CatalogViolation.ImmutableAssetConflict])
    assert(failure.violations(1).violation.isInstanceOf[CatalogViolation.ImmutableGridConflict])
    assertEquals(failure.violations(2).violation, CatalogViolation.MissingGridDimension(unknownGrid.identity))
    assertEquals(initial.revision.value, BigInt(1))
    assertEquals(initial.snapshot.resolveAsset(valid.id), Left(UnknownAsset(valid.id)))

  test("canonical retries and aliases produce non-overlapping binding evidence"):
    val canonical = assetDefinition("canonical-binding", "asset:canonical-binding")
    val dimension = DimKey.atom(canonical.dimensionAtom)
    val initial   = published(
      CatalogRoot.create().initialState,
      CatalogCommand.RegisterAsset(canonical)
    ).state
    val alias = assetDefinition("canonical-alias", canonical.dimensionAtom.value)

    val mixedFailure = CatalogModel
      .commit(
        initial,
        CatalogBatch.of(
          CatalogCommand.RegisterAsset(canonical),
          CatalogCommand.RegisterAsset(alias)
        )
      )
      .left
      .toOption
      .get
    assertEquals(
      mixedFailure.violations,
      Vector(
        IndexedCatalogViolation(
          1,
          2,
          CatalogViolation.AssetDimensionAlreadyBound(dimension, canonical.id, alias.id)
        )
      )
    )

    val freshDimension = AtomId("asset:fresh-binding")
    val freshFirst     = assetDefinition("fresh-binding-first", freshDimension.value)
    val freshSecond    = assetDefinition("fresh-binding-second", freshDimension.value)
    val accumulated    = CatalogModel
      .commit(
        initial,
        CatalogBatch.of(
          CatalogCommand.RegisterAsset(canonical),
          CatalogCommand.RegisterAsset(alias),
          CatalogCommand.RegisterAsset(freshFirst),
          CatalogCommand.RegisterAsset(freshSecond)
        )
      )
      .left
      .toOption
      .get
    assertEquals(
      accumulated.violations,
      Vector(
        IndexedCatalogViolation(
          1,
          2,
          CatalogViolation.AssetDimensionAlreadyBound(dimension, canonical.id, alias.id)
        ),
        IndexedCatalogViolation(
          3,
          2,
          CatalogViolation.AssetDimensionAlreadyBound(
            DimKey.atom(freshDimension),
            freshFirst.id,
            freshSecond.id
          )
        )
      )
    )

  test("contradictory duplicates and one-to-one bindings fail deterministically without dependent noise"):
    val first           = assetDefinition("duplicate", "asset:first")
    val second          = AssetDefinition(first.id, AtomId("asset:second"))
    val dependent       = gridDefinition(DimKey.atom(first.dimensionAtom), "dependent-grid")
    val sharedDimension = DimKey.atom(AtomId("asset:shared"))
    val sharedA         = assetDefinition("shared-a", "asset:shared")
    val sharedB         = assetDefinition("shared-b", "asset:shared")
    val batch           = CatalogBatch.of(
      CatalogCommand.RegisterAsset(first),
      CatalogCommand.RegisterGrid(dependent),
      CatalogCommand.RegisterAsset(second),
      CatalogCommand.RegisterAsset(sharedA),
      CatalogCommand.RegisterAsset(sharedB)
    )

    val failure = CatalogModel.commit(CatalogRoot.create().initialState, batch).left.toOption.get
    assertEquals(failure.violations.map(_.commandIndex), Vector(0, 4))
    assert(failure.violations.head.violation.isInstanceOf[CatalogViolation.DuplicateAssetProposal])
    assertEquals(
      failure.violations(1).violation,
      CatalogViolation.AssetDimensionAlreadyBound(sharedDimension, sharedA.id, sharedB.id)
    )

  test("grid versions coexist and handles delegate to retained mathematical grids"):
    val assetDefinitionValue = assetDefinition("USD-grid")
    val dimension            = DimKey.atom(assetDefinitionValue.dimensionAtom)
    val firstDefinition      = gridDefinition(dimension, "cent", version = 1)
    val firstTransition      = published(
      CatalogRoot.create().initialState,
      CatalogCommand.RegisterAsset(assetDefinitionValue),
      CatalogCommand.RegisterGrid(firstDefinition)
    )
    val firstSnapshot    = firstTransition.state.snapshot
    val asset            = firstSnapshot.resolveAsset(assetDefinitionValue.id).toOption.get
    val first            = firstSnapshot.resolveGrid(asset.dimension)(firstDefinition.key).toOption.get
    val secondDefinition = gridDefinition(
      dimension,
      "cent",
      version = 2,
      quantum = PositiveRational.exact(1, 1000).toOption.get
    )
    val secondTransition = published(firstTransition.state, CatalogCommand.RegisterGrid(secondDefinition))
    val retained = secondTransition.state.snapshot.resolveGrid(asset.dimension)(firstDefinition.key).toOption.get
    val second   = secondTransition.state.snapshot.resolveGrid(asset.dimension)(secondDefinition.key).toOption.get

    assert(first.eq(retained))
    assert(GridHandle.reconcile(first, retained).isRight)
    assertNotEquals(first.identity, second.identity)
    val value = first.fromCoordinate(BigInt(123))
    assertEquals(first.coordinate(value), BigInt(123))
    assertEquals(first.asQuantity(value).coefficient, Rational(123, 100))
    assertEquals(first.dimension.key, first.grid.dimension.key)

  test("historical snapshots stay coherent and independently rebuilt roots keep separate lineage"):
    val definition  = assetDefinition("history")
    val oldState    = CatalogRoot.create().initialState
    val oldSnapshot = oldState.snapshot
    val first       = published(oldState, CatalogCommand.RegisterAsset(definition))
    val newAsset    = first.state.snapshot.resolveAsset(definition.id).toOption.get

    assertEquals(oldSnapshot.revision.value, BigInt(0))
    assertEquals(oldSnapshot.resolveAsset(definition.id), Left(UnknownAsset(definition.id)))
    assertEquals(first.state.snapshot.revision.value, BigInt(1))

    val unrelated = assetDefinition("history-later")
    val second    = published(first.state, CatalogCommand.RegisterAsset(unrelated))
    val retained  = second.state.snapshot.resolveAsset(definition.id).toOption.get
    assert(newAsset.eq(retained))
    assert(Asset.reconcile(newAsset, retained).isRight)

    val rebuilt = published(CatalogRoot.create().initialState, CatalogCommand.RegisterAsset(definition))
      .state
      .snapshot
      .resolveAsset(definition.id)
      .toOption
      .get
    assert(Asset.reconcile(newAsset, rebuilt).isLeft)

  test("snapshot lookup is direct, typed, and rejects foreign or unknown handles"):
    val localDefinition   = assetDefinition("local")
    val foreignDefinition = assetDefinition("foreign")
    val localGrid         = gridDefinition(DimKey.atom(localDefinition.dimensionAtom), "local-grid")
    val local             = published(
      CatalogRoot.create().initialState,
      CatalogCommand.RegisterAsset(localDefinition),
      CatalogCommand.RegisterGrid(localGrid)
    ).state.snapshot
    val foreign = published(CatalogRoot.create().initialState, CatalogCommand.RegisterAsset(foreignDefinition))
      .state.snapshot.resolveAsset(foreignDefinition.id).toOption.get

    assertEquals(local.resolveAsset(validAssetId("unknown")), Left(UnknownAsset(validAssetId("unknown"))))
    assertEquals(local.resolveDimension(DimKey.one), Left(UnknownDimension(DimKey.one)))
    val absent = GridIdentity(DimKey.one, GridKey(validGridId("absent"), validGridVersion(1)))
    assertEquals(local.resolveGrid(absent), Left(UnknownGrid(absent)))
    assertEquals(
      local.resolveGrid(foreign.dimension)(localGrid.key),
      Left(UnknownDimension(foreign.dimension.key))
    )

  test("public roots reject null and handles expose no catalog mutation or lookup"):
    val root                                    = CatalogRoot.create()
    val definition                              = assetDefinition("null-roots")
    val transition                              = published(root.initialState, CatalogCommand.RegisterAsset(definition))
    val asset                                   = transition.state.snapshot.resolveAsset(definition.id).toOption.get
    val nullDimension: DimensionHandle[asset.D] = null
    val nullAsset: Asset                        = null
    val nullGrid: GridHandle[asset.D]           = null

    val _ = intercept[NullPointerException](CatalogBatch.one(null))
    val _ = intercept[NullPointerException](
      CatalogModel.commit(null, CatalogBatch.one(CatalogCommand.RegisterAsset(definition)))
    )
    val _ = intercept[NullPointerException](CatalogModel.commit(root.initialState, null))
    val _ = intercept[NullPointerException](transition.state.snapshot.resolveAsset(null))
    val _ = intercept[NullPointerException](transition.state.snapshot.resolveDimension(null))
    val _ = intercept[NullPointerException](transition.state.snapshot.resolveGrid(null))
    val _ = intercept[NullPointerException](DimensionHandle.sameLineage(nullDimension, asset.dimension))
    val _ = intercept[NullPointerException](Asset.reconcile(nullAsset, asset))
    val _ = intercept[NullPointerException](GridHandle.reconcile(nullGrid, nullGrid))

    val forbiddenNames =
      Set("commit", "registerAsset", "registerDimension", "registerGrid", "resolveAsset", "resolveGrid")
    assertEquals(asset.getClass.getMethods.map(_.getName).toSet.intersect(forbiddenNames), Set.empty)
end ReferenceDataSuite
