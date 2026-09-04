package trading.codec

import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectOutputStream

import munit.FunSuite

import trading.quantity.*
import trading.quantity.refinement.PositiveRational
import trading.reference.*

class CatalogJournalSuite extends FunSuite:
  test("published outcomes create closed immutable V1 entries with exact ordered commands"):
    val asset      = assetDefinition("published")
    val dimension  = DimKey.atom(asset.dimensionAtom)
    val identity   = gridIdentity(dimension, "published-grid", 1)
    val huge       = BigInt(10).pow(1_000) + 39
    val definition = gridDefinition(identity, huge, huge + 2)
    val batch      = CatalogBatch.of(
      CatalogCommand.RegisterGrid(definition),
      CatalogCommand.RegisterAsset(asset),
      CatalogCommand.RegisterAsset(asset)
    )
    val transition = commit(CatalogRoot.create().initialState, batch)
    val entry      = CatalogJournalEntry.fromPublished(batch, publishedOutcome(transition))

    assertEquals(entry.successorRevision, revision(1))
    assertEquals(entry.batch.commands, batch.commands)
    assertEquals(
      classOf[CatalogJournalEntry.V1].getDeclaredFields.map(_.getName).toSet,
      Set("successorRevision", "batch")
    )
    val encoded = CatalogJournalEntry.encode(entry).toOption.get
    assertEquals(CatalogJournalEntry.parse(encoded), Right(entry))
    assert(encoded.contains(s"\"numerator\":\"$huge\""))
    assert(encoded.indexOf("register-grid") < encoded.indexOf("register-asset"))
    assertEquals(CatalogJournalEntry.parse(encoded).toOption.get.batch.commands, batch.commands)
    rejectSerialization(entry)

    val schema = CatalogJournalEntry.schema().toOption.get
    Vector("register-dimension", "register-asset", "register-grid").foreach(tag => assert(schema.contains(tag)))
    Vector("snapshot", "lineage", "timestamp", "checkpoint", "activation", "delisting").foreach(forbidden =>
      assert(!schema.contains(forbidden))
    )

  test("history parsing accumulates every independent indexed wire failure before replay"):
    val invalidRevisionAndBatch =
      """{"payload":{"commands":[],"successorRevision":"0"},"recordType":"trading.catalog-journal-entry","schemaVersion":1}"""
    val negativeQuantum =
      """{"payload":{"commands":[{"gridIdentity":{"dimension":[],"gridId":"bad-grid","gridVersion":"1"},"kind":"register-grid","quantum":{"denominator":"1","numerator":"-1"}}],"successorRevision":"1"},"recordType":"trading.catalog-journal-entry","schemaVersion":1}"""
    val unknownCommand =
      """{"payload":{"commands":[{"kind":"delete-asset"}],"successorRevision":"1"},"recordType":"trading.catalog-journal-entry","schemaVersion":1}"""

    val failures = CatalogJournalEntry
      .parseHistory(Vector(invalidRevisionAndBatch, negativeQuantum, unknownCommand))
      .left
      .toOption
      .get
      .toVector
    assertEquals(failures.map(_.recordIndex), Vector(0, 0, 1, 2))
    assertEquals(
      failures.map(_.path.render),
      Vector(
        "$.payload.commands",
        "$.payload.successorRevision",
        "$.payload.commands[0].quantum",
        "$.payload.commands[0].kind"
      )
    )
    failures(2) match
      case WireDecodeViolation.InvalidValue(_, code, _) =>
        assertEquals(code, "expected-positive-grid-quantum")
      case other => fail(s"expected grid-quantum refinement failure, got $other")
    assert(failures.last.isInstanceOf[WireDecodeViolation.UnknownAlternative])

    val nonFresh = commit(
      CatalogRoot.create().initialState,
      CatalogBatch.one(CatalogCommand.RegisterDimension(DimKey.one))
    ).state
    CatalogReplay.decodeAndRebuild(nonFresh, Vector(invalidRevisionAndBatch)) match
      case Left(CatalogJournalRebuildFailure.Wire(violations)) =>
        assertEquals(violations.head.recordIndex, 0)
      case other => fail(s"wire failures must suppress replay, got $other")

  test("history record limits fail once before entry parsing"):
    val limits = DecodeLimits
      .create(
        maxPayloadCharacters = 1_000_000,
        maxPayloadUtf8Bytes = 4_000_000,
        maxNestingDepth = 32,
        maxBatchRecords = 1,
        maxObjectMembers = 128,
        maxArrayEntries = 10_000,
        maxStringCharacters = 4_096,
        maxIntegerDigits = 4_096,
        maxDimensionFactors = 256,
        maxCatalogCommands = 10_000,
        maxScenarioSlices = 10_000,
        maxMarketConversions = 1_024
      )
      .toOption
      .get
    CatalogJournalEntry.parseHistory(Vector("not-json", "also-not-json"), limits).left.toOption.get.toVector match
      case Vector(WireDecodeViolation.Limit(limit)) =>
        assertEquals(limit.limit, DecodeLimit.BatchRecords)
        assertEquals(limit.actual, 2L)
        assertEquals(limit.maximum, 1)
      case other => fail(s"expected one pre-parse history limit, got $other")

  test("pure replay matches direct publication, preserves versions and prefixes, and creates fresh lineage"):
    val asset       = assetDefinition("USD-history")
    val dimension   = DimKey.atom(asset.dimensionAtom)
    val first       = gridIdentity(dimension, "history-grid", 1)
    val second      = gridIdentity(dimension, "history-grid", 2)
    val independent = DimKey(Vector(AtomId("history:a") -> BigInt(1), AtomId("history:b") -> BigInt(-3)))
    val batches     = Vector(
      CatalogBatch.of(
        CatalogCommand.RegisterAsset(asset),
        CatalogCommand.RegisterAsset(asset),
        CatalogCommand.RegisterGrid(gridDefinition(first, 1, 100))
      ),
      CatalogBatch.one(CatalogCommand.RegisterDimension(independent)),
      CatalogBatch.one(CatalogCommand.RegisterGrid(gridDefinition(second, 1, 1_000)))
    )
    val (direct, entries) = publishedHistory(CatalogRoot.create().initialState, batches)
    val wires             = entries.map(entry => CatalogJournalEntry.encode(entry).toOption.get)
    val replayed          = CatalogReplay
      .decodeAndRebuild(CatalogRoot.create().initialState, wires)
      .toOption
      .get

    assertEquals(replayed.revision, direct.revision)
    assertEquals(replayed.snapshot.assetCount, direct.snapshot.assetCount)
    assertEquals(replayed.snapshot.dimensionCount, direct.snapshot.dimensionCount)
    assertEquals(replayed.snapshot.gridCount, direct.snapshot.gridCount)
    val replayedAsset = replayed.snapshot.resolveAsset(asset.id).toOption.get
    val directAsset   = direct.snapshot.resolveAsset(asset.id).toOption.get
    assert(Asset.reconcile(replayedAsset, directAsset).isLeft)
    assertEquals(
      replayed.snapshot.resolveGrid(first).toOption.get.quantum.unrefined,
      Rational(1, 100)
    )
    assertEquals(
      replayed.snapshot.resolveGrid(second).toOption.get.quantum.unrefined,
      Rational(1, 1_000)
    )
    assertEquals(entries.head.batch.commands, batches.head.commands)
    rejectSerialization(replayed)

    val prefix = CatalogReplay.rebuild(CatalogRoot.create().initialState, entries.take(1)).toOption.get
    assertEquals(prefix.revision, revision(1))
    assert(prefix.snapshot.resolveGrid(first).isRight)
    assertEquals(prefix.snapshot.resolveGrid(second), Left(UnknownGrid(second)))

    val independentlyReplayed = CatalogReplay.rebuild(CatalogRoot.create().initialState, entries).toOption.get
    val firstReplayAsset      = replayed.snapshot.resolveAsset(asset.id).toOption.get
    val secondReplayAsset     = independentlyReplayed.snapshot.resolveAsset(asset.id).toOption.get
    assert(Asset.reconcile(firstReplayAsset, secondReplayAsset).isLeft)

    val emptyState  = CatalogRoot.create().initialState
    val emptyResult = CatalogReplay.rebuild(emptyState, Vector.empty).toOption.get
    assert(emptyResult.state.eq(emptyState))
    assertEquals(emptyResult.revision, CatalogRevision.zero)

  test("replay reports gaps and repeats before attempting their batches"):
    val fresh      = CatalogRoot.create().initialState
    val dimension  = DimKey.atom(AtomId("sequence:first"))
    val firstBatch = CatalogBatch.one(CatalogCommand.RegisterDimension(dimension))
    val gap        = decodedEntry(revision(2), firstBatch)
    assertEquals(
      CatalogReplay.rebuild(fresh, Vector(gap)),
      Left(
        CatalogReplayFailure.RevisionSequenceMismatch(
          0,
          revision(1),
          revision(2),
          CatalogRevision.zero
        )
      )
    )

    val firstTransition = commit(fresh, firstBatch)
    val firstEntry      = CatalogJournalEntry.fromPublished(firstBatch, publishedOutcome(firstTransition))
    val laterBatch      = CatalogBatch.one(
      CatalogCommand.RegisterDimension(DimKey.atom(AtomId("sequence:later")))
    )
    val repeated = decodedEntry(revision(1), laterBatch)
    assertEquals(
      CatalogReplay.rebuild(fresh, Vector(firstEntry, repeated)),
      Left(
        CatalogReplayFailure.RevisionSequenceMismatch(
          1,
          revision(2),
          revision(1),
          revision(1)
        )
      )
    )

  test("replay retains catalog conflicts and rejects unchanged entries without partial authority"):
    val dimension  = DimKey.atom(AtomId("failure:grid"))
    val identity   = gridIdentity(dimension, "failure-grid", 1)
    val firstBatch = CatalogBatch.of(
      CatalogCommand.RegisterDimension(dimension),
      CatalogCommand.RegisterGrid(gridDefinition(identity, 1, 100))
    )
    val firstTransition = commit(CatalogRoot.create().initialState, firstBatch)
    val firstEntry      = CatalogJournalEntry.fromPublished(firstBatch, publishedOutcome(firstTransition))
    val conflictBatch   = CatalogBatch.one(
      CatalogCommand.RegisterGrid(gridDefinition(identity, 1, 1_000))
    )
    val conflictEntry = decodedEntry(revision(2), conflictBatch)
    CatalogReplay.rebuild(CatalogRoot.create().initialState, Vector(firstEntry, conflictEntry)) match
      case Left(
          CatalogReplayFailure.CatalogValidationFailed(
            1,
            expected,
            recorded,
            last,
            violations
          )
        ) =>
        assertEquals(expected, revision(2))
        assertEquals(recorded, revision(2))
        assertEquals(last, revision(1))
        assert(violations.violations.exists(_.violation.isInstanceOf[CatalogViolation.ImmutableGridConflict]))
        assert(!classOf[CatalogReplayFailure].getMethods.exists(_.getReturnType == classOf[CatalogState]))
        rejectSerialization(violations.head)
      case other => fail(s"expected contextual catalog conflict, got $other")

    val noOpEntry  = decodedEntry(revision(2), firstBatch)
    val laterEntry = decodedEntry(
      revision(3),
      CatalogBatch.one(CatalogCommand.RegisterDimension(DimKey.atom(AtomId("must:not-run"))))
    )
    assertEquals(
      CatalogReplay.rebuild(CatalogRoot.create().initialState, Vector(firstEntry, noOpEntry, laterEntry)),
      Left(
        CatalogReplayFailure.UnexpectedUnchanged(
          1,
          revision(2),
          revision(2),
          revision(1)
        )
      )
    )

  test("replay rejects any non-fresh supplied state before entry work"):
    val dimension = DimKey.atom(AtomId("nonfresh:dimension"))
    val nonFresh  = commit(
      CatalogRoot.create().initialState,
      CatalogBatch.one(CatalogCommand.RegisterDimension(dimension))
    ).state
    val impossibleLater = decodedEntry(
      revision(99),
      CatalogBatch.one(CatalogCommand.RegisterDimension(DimKey.one))
    )
    assertEquals(
      CatalogReplay.rebuild(nonFresh, Vector(impossibleLater)),
      Left(CatalogReplayFailure.NonFreshStart(revision(1), 1, 0, 0))
    )

  test("independent command permutations and identical duplicates preserve their supplied order"):
    val commands = Vector("a", "b", "c").map(name =>
      CatalogCommand.RegisterDimension(DimKey.atom(AtomId(s"permutation:$name")))
    )
    commands.permutations.foreach: permutation =>
      val batch      = CatalogBatch.of(permutation.head, permutation.tail*)
      val transition = commit(CatalogRoot.create().initialState, batch)
      val entry      = CatalogJournalEntry.fromPublished(batch, publishedOutcome(transition))
      val decoded    = CatalogJournalEntry.parse(CatalogJournalEntry.encode(entry).toOption.get).toOption.get
      assertEquals(decoded.batch.commands, permutation)
      val replayed = CatalogReplay.rebuild(CatalogRoot.create().initialState, Vector(decoded)).toOption.get
      assertEquals(replayed.revision, revision(1))
      assertEquals(replayed.snapshot.dimensionCount, 3)

    val duplicate  = CatalogCommand.RegisterDimension(DimKey.atom(AtomId("permutation:duplicate")))
    val batch      = CatalogBatch.of(duplicate, duplicate, duplicate)
    val transition = commit(CatalogRoot.create().initialState, batch)
    val entry      = CatalogJournalEntry.fromPublished(batch, publishedOutcome(transition))
    assertEquals(entry.batch.commands, Vector(duplicate, duplicate, duplicate))
    assertEquals(CatalogReplay.rebuild(CatalogRoot.create().initialState, Vector(entry)).map(_.revision),
      Right(revision(1)))

  private def publishedHistory(
    initial: CatalogState,
    batches: Vector[CatalogBatch]
  ): (CatalogState, Vector[CatalogJournalEntry.V1]) =
    batches.foldLeft(initial -> Vector.empty[CatalogJournalEntry.V1]):
      case ((state, entries), batch) =>
        val transition = commit(state, batch)
        transition.state -> (entries :+ CatalogJournalEntry.fromPublished(batch, publishedOutcome(transition)))

  private def decodedEntry(successorRevision: CatalogRevision, batch: CatalogBatch): CatalogJournalEntry.V1 =
    val transition = commit(
      CatalogRoot.create().initialState,
      CatalogBatch.one(CatalogCommand.RegisterDimension(DimKey.one))
    )
    val seed    = CatalogJournalEntry.fromPublished(batch, publishedOutcome(transition))
    val encoded = CatalogJournalEntry.encode(seed).toOption.get
    val revised = encoded.replace(
      s"\"successorRevision\":\"${seed.successorRevision.value}\"",
      s"\"successorRevision\":\"${successorRevision.value}\""
    )
    CatalogJournalEntry.parse(revised).toOption.get

  private def commit(state: CatalogState, batch: CatalogBatch): CatalogTransition =
    CatalogModel.commit(state, batch).fold(errors => fail(s"expected catalog publication, got $errors"), identity)

  private def publishedOutcome(transition: CatalogTransition): CatalogCommit.Published =
    transition.outcome match
      case published: CatalogCommit.Published => published
      case other                              => fail(s"expected published catalog outcome, got $other")

  private def revision(value: BigInt): CatalogRevision =
    CatalogRevision.from(value).fold(error => fail(s"invalid test catalog revision: $error"), identity)

  private def assetId(value: String): AssetId =
    AssetId.from(value).fold(error => fail(s"invalid test asset ID: $error"), identity)

  private def assetDefinition(value: String): AssetDefinition =
    AssetDefinition(assetId(value), AtomId(s"asset:$value"))

  private def gridId(value: String): GridId =
    GridId.from(value).fold(error => fail(s"invalid test grid ID: $error"), identity)

  private def gridVersion(value: Long): GridVersion =
    GridVersion.from(value).fold(error => fail(s"invalid test grid version: $error"), identity)

  private def gridIdentity(dimension: DimKey, id: String, version: Long): GridIdentity =
    GridIdentity(dimension, GridKey(gridId(id), gridVersion(version)))

  private def gridDefinition(
    identity: GridIdentity,
    numerator: BigInt,
    denominator: BigInt
  ): GridDefinition =
    val quantum = PositiveRational.exact(numerator, denominator).fold(
      error => fail(s"invalid test grid quantum: $error"),
      value => value
    )
    GridDefinition(identity, quantum)

  private def rejectSerialization(value: JavaSerializationUnsupported): Unit =
    val bytes  = new ByteArrayOutputStream
    val output = new ObjectOutputStream(bytes)
    try
      val _ = intercept[NotSerializableException](output.writeObject(value))
    finally output.close()
end CatalogJournalSuite
