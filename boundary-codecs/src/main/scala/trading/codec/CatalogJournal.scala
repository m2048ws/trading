package trading.codec

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.Objects
import scala.annotation.nowarn

import trading.quantity.JavaSerializationUnsupported
import trading.quantity.refinement.PositiveRational
import trading.reference.AssetDefinition
import trading.reference.CatalogBatch
import trading.reference.CatalogCommand
import trading.reference.CatalogCommit
import trading.reference.CatalogModel
import trading.reference.CatalogRevision
import trading.reference.CatalogState
import trading.reference.CatalogViolations
import trading.reference.GridDefinition

/** One immutable published catalog batch in the frozen V1 journal representation. */
object CatalogJournalEntry:
  @nowarn("msg=Ignoring.*qualifier")
  final class V1 private[this] (
    val successorRevision: CatalogRevision,
    val batch: CatalogBatch)
    extends JavaSerializationUnsupported:

    Objects.requireNonNull(successorRevision, "catalog journal successor revision")
    require(successorRevision.value > 0, "catalog journal successor revision must be positive")
    Objects.requireNonNull(batch, "catalog journal batch")

    override def equals(other: Any): Boolean =
      other match
        case that: V1 => successorRevision == that.successorRevision && batch == that.batch
        case _        => false

    override def hashCode: Int =
      (successorRevision, batch).hashCode

    override def toString: String =
      s"CatalogJournalEntry.V1($successorRevision,$batch)"
  end V1

  private val constructor =
    val owner = classOf[V1]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[CatalogRevision],
          classOf[CatalogBatch]
        )
      )

  val recordType: RecordType       = CodecRecordTypes.catalogJournalEntry
  val schemaVersion: SchemaVersion = SchemaVersion.one

  private val revisionSchema: WireSchema[CatalogRevision] =
    ExactWire.positiveInteger.imap(value => CatalogRevision.from(value).toOption.get)(_.value)

  private val positiveQuantumSchema: WireSchema[PositiveRational] =
    ExactWire.rational.refine[PositiveRational]((value, context) =>
      PositiveRational(value).left.map(_ =>
        WireDecodeViolation.InvalidValue(context.path, "expected-positive-grid-quantum", context.recordIndex)
      )
    )(_.unrefined)

  private val assetDefinitionRecord: WireRecord[AssetDefinition] =
    WireRecord
      .field("assetId", ExactWire.assetId)
      .product(WireRecord.field("dimensionAtom", ExactWire.atomId))
      .imap(value => AssetDefinition(value._1, value._2))(value => value.id -> value.dimensionAtom)

  private val gridDefinitionRecord: WireRecord[GridDefinition] =
    WireRecord
      .field("gridIdentity", ExactWire.gridIdentity)
      .product(WireRecord.field("quantum", positiveQuantumSchema))
      .imap(value => GridDefinition(value._1, value._2))(value => value.identity -> value.quantum)

  private val commandSchema: WireSchema[CatalogCommand] =
    WireSchema.tagged[CatalogCommand](
      "kind",
      Vector(
        WireCase[CatalogCommand, trading.quantity.DimKey](
          "register-dimension",
          WireRecord.field("dimension", ExactWire.dimension)
        ):
          case CatalogCommand.RegisterDimension(key) => Some(key)
          case _                                     =>
            None
        (CatalogCommand.RegisterDimension.apply),
        WireCase[CatalogCommand, AssetDefinition]("register-asset", assetDefinitionRecord):
          case CatalogCommand.RegisterAsset(definition) => Some(definition)
          case _                                        =>
            None
        (CatalogCommand.RegisterAsset.apply),
        WireCase[CatalogCommand, GridDefinition]("register-grid", gridDefinitionRecord):
          case CatalogCommand.RegisterGrid(definition) => Some(definition)
          case _                                       =>
            None
        (CatalogCommand.RegisterGrid.apply)
      )
    )

  private val batchSchema: WireSchema[CatalogBatch] =
    WireSchema
      .vector(commandSchema, DecodeLimit.CatalogCommands)
      .refine[CatalogBatch]((commands, context) =>
        CatalogBatch.from(commands).left.map(_ =>
          WireDecodeViolation.InvalidValue(context.path, "empty-catalog-batch", context.recordIndex)
        )
      )(_.commands)

  private val v1Schema: WireSchema[V1] =
    val representation =
      WireRecord
        .field("successorRevision", revisionSchema)
        .product(WireRecord.field("commands", batchSchema))
        .imap(value => construct(value._1, value._2))(value => value.successorRevision -> value.batch)
    WireSchema.record(representation)

  private val codec = EnvelopeCodec(
    recordType,
    schemaVersion,
    Vector(schemaVersion -> v1Schema),
    CodecRecordTypes.otherThan(recordType)
  )

  /** Record exactly the submitted batch and revision issued by a successful publication outcome. */
  def fromPublished(batch: CatalogBatch, published: CatalogCommit.Published): V1 =
    val checkedBatch     = Objects.requireNonNull(batch, "published catalog batch")
    val checkedPublished = Objects.requireNonNull(published, "published catalog outcome")
    construct(checkedPublished.snapshot.revision, checkedBatch)

  def encode(entry: V1): Either[WireViolations[WireEncodeViolation], String] =
    codec.write(Objects.requireNonNull(entry, "catalog journal entry"))

  def parse(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], V1] =
    codec.read(input, limits, recordIndex)

  /** Parse every independent envelope before permitting any catalog transition. */
  def parseHistory(
    inputs: Vector[String],
    limits: DecodeLimits = DecodeLimits.default
  ): Either[WireViolations[WireDecodeViolation], Vector[V1]] =
    val checkedInputs = Objects.requireNonNull(inputs, "catalog journal inputs")
    val checkedLimits = Objects.requireNonNull(limits, "catalog journal decode limits")
    if checkedInputs.size > checkedLimits.maxBatchRecords then
      Left(
        WireViolations.one(
          WireDecodeViolation.Limit(
            WireLimitViolation(
              DecodeLimit.BatchRecords,
              checkedInputs.size.toLong,
              checkedLimits.maxBatchRecords,
              WirePath.root,
              0
            )
          )
        )
      )
    else
      val decoded = checkedInputs.zipWithIndex.map: (input, index) =>
        parse(Objects.requireNonNull(input, s"catalog journal input $index"), checkedLimits, index)
      val errors = decoded.collect:
        case Left(violations) => violations.toVector
      .flatten
      if errors.nonEmpty then Left(WireViolations.orderedDecode(errors))
      else Right(decoded.collect { case Right(entry) => entry })
    end if
  end parseHistory

  def schema(
    id: String = "urn:trading:codec:schema:catalog-journal-entry:v1",
    definitionName: String = "CatalogJournalEntryV1"
  ): Either[WireViolations[WireEncodeViolation], String] =
    codec.schema(id, definitionName)

  private def construct(successorRevision: CatalogRevision, batch: CatalogBatch): V1 =
    constructor
      .invoke(successorRevision, batch)
      .asInstanceOf[V1]
end CatalogJournalEntry

/** State-independent versus sequential replay failure remains explicit at the journal boundary. */
enum CatalogJournalRebuildFailure extends JavaSerializationUnsupported:
  case Wire(violations: WireViolations[WireDecodeViolation])
  case Replay(failure: CatalogReplayFailure)

  private val _ =
    this match
      case Wire(violations) => Objects.requireNonNull(violations, "catalog journal wire violations")
      case Replay(failure)  => Objects.requireNonNull(failure, "catalog replay failure")
end CatalogJournalRebuildFailure

/** Closed failures from replaying decoded journal data through the normative pure catalog transition. */
enum CatalogReplayFailure extends JavaSerializationUnsupported:
  case NonFreshStart(
    revision: CatalogRevision,
    dimensionCount: Int,
    assetCount: Int,
    gridCount: Int)
  case RevisionSequenceMismatch(
    entryIndex: Int,
    expectedRevision: CatalogRevision,
    recordedRevision: CatalogRevision,
    lastSuccessfulRevision: CatalogRevision)
  case CatalogValidationFailed(
    entryIndex: Int,
    expectedRevision: CatalogRevision,
    recordedRevision: CatalogRevision,
    lastSuccessfulRevision: CatalogRevision,
    violations: CatalogViolations)
  case UnexpectedUnchanged(
    entryIndex: Int,
    expectedRevision: CatalogRevision,
    recordedRevision: CatalogRevision,
    lastSuccessfulRevision: CatalogRevision)
  case PublishedRevisionMismatch(
    entryIndex: Int,
    expectedRevision: CatalogRevision,
    recordedRevision: CatalogRevision,
    actualStateRevision: CatalogRevision,
    actualSnapshotRevision: CatalogRevision,
    lastSuccessfulRevision: CatalogRevision)

  private val _ =
    this match
      case NonFreshStart(revision, dimensionCount, assetCount, gridCount) =>
        Objects.requireNonNull(revision, "non-fresh catalog revision")
        require(dimensionCount >= 0 && assetCount >= 0 && gridCount >= 0, "catalog counts must be nonnegative")
        require(
          revision != CatalogRevision.zero || dimensionCount != 0 || assetCount != 0 || gridCount != 0,
          "non-fresh start must retain observable non-fresh state"
        )
      case RevisionSequenceMismatch(index, expected, recorded, last) =>
        requireEntryContext(index, expected, recorded, last)
        require(expected != recorded, "revision mismatch requires distinct revisions")
      case CatalogValidationFailed(index, expected, recorded, last, violations) =>
        requireEntryContext(index, expected, recorded, last)
        Objects.requireNonNull(violations, "catalog replay violations")
      case UnexpectedUnchanged(index, expected, recorded, last) =>
        requireEntryContext(index, expected, recorded, last)
      case PublishedRevisionMismatch(index, expected, recorded, actualState, actualSnapshot, last) =>
        requireEntryContext(index, expected, recorded, last)
        Objects.requireNonNull(actualState, "actual catalog state revision")
        Objects.requireNonNull(actualSnapshot, "actual published snapshot revision")
        require(
          expected != actualState || expected != actualSnapshot,
          "published revision mismatch requires a distinct state or snapshot revision"
        )

  private def requireEntryContext(
    entryIndex: Int,
    expectedRevision: CatalogRevision,
    recordedRevision: CatalogRevision,
    lastSuccessfulRevision: CatalogRevision
  ): Unit =
    require(entryIndex >= 0, "catalog replay entry index must be nonnegative")
    Objects.requireNonNull(expectedRevision, "expected catalog revision")
    Objects.requireNonNull(recordedRevision, "recorded catalog revision")
    val _ = Objects.requireNonNull(lastSuccessfulRevision, "last successful catalog revision")
end CatalogReplayFailure

/** Successful replay authority under exactly the caller-supplied fresh process-local lineage. */
@nowarn("msg=Ignoring.*qualifier")
final class CatalogReplayResult private[this] (val state: CatalogState) extends JavaSerializationUnsupported:
  Objects.requireNonNull(state, "replayed catalog state")

  def snapshot                  = state.snapshot
  def revision: CatalogRevision = state.revision
end CatalogReplayResult

/** Deterministic pure replay from a caller-owned fresh catalog state. */
object CatalogReplay:
  private val resultConstructor =
    val owner = classOf[CatalogReplayResult]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(owner, MethodType.methodType(java.lang.Void.TYPE, classOf[CatalogState]))

  def rebuild(
    fresh: CatalogState,
    entries: Vector[CatalogJournalEntry.V1]
  ): Either[CatalogReplayFailure, CatalogReplayResult] =
    val checkedFresh   = Objects.requireNonNull(fresh, "fresh catalog state")
    val checkedEntries = Objects.requireNonNull(entries, "catalog journal entries")
    checkedEntries.zipWithIndex.foreach: (entry, index) =>
      Objects.requireNonNull(entry, s"catalog journal entry $index")

    val initialSnapshot = checkedFresh.snapshot
    if checkedFresh.revision != CatalogRevision.zero ||
      initialSnapshot.dimensionCount != 0 || initialSnapshot.assetCount != 0 || initialSnapshot.gridCount != 0
    then
      Left(
        CatalogReplayFailure.NonFreshStart(
          checkedFresh.revision,
          initialSnapshot.dimensionCount,
          initialSnapshot.assetCount,
          initialSnapshot.gridCount
        )
      )
    else replayFrom(checkedFresh, checkedEntries, 0)
  end rebuild

  /** Decode the complete independent input sequence before checking or mutating replay state. */
  def decodeAndRebuild(
    fresh: CatalogState,
    inputs: Vector[String],
    limits: DecodeLimits = DecodeLimits.default
  ): Either[CatalogJournalRebuildFailure, CatalogReplayResult] =
    CatalogJournalEntry
      .parseHistory(inputs, limits)
      .left
      .map(CatalogJournalRebuildFailure.Wire.apply)
      .flatMap(entries => rebuild(fresh, entries).left.map(CatalogJournalRebuildFailure.Replay.apply))

  private def replayFrom(
    state: CatalogState,
    entries: Vector[CatalogJournalEntry.V1],
    index: Int
  ): Either[CatalogReplayFailure, CatalogReplayResult] =
    entries.lift(index) match
      case None        => Right(resultFrom(state))
      case Some(entry) =>
        val lastSuccessful = state.revision
        val expected       = nextRevision(lastSuccessful)
        if entry.successorRevision != expected then
          Left(
            CatalogReplayFailure.RevisionSequenceMismatch(
              index,
              expected,
              entry.successorRevision,
              lastSuccessful
            )
          )
        else
          CatalogModel.commit(state, entry.batch) match
            case Left(violations) =>
              Left(
                CatalogReplayFailure.CatalogValidationFailed(
                  index,
                  expected,
                  entry.successorRevision,
                  lastSuccessful,
                  violations
                )
              )
            case Right(transition) =>
              transition.outcome match
                case _: CatalogCommit.Unchanged =>
                  Left(
                    CatalogReplayFailure.UnexpectedUnchanged(
                      index,
                      expected,
                      entry.successorRevision,
                      lastSuccessful
                    )
                  )
                case published: CatalogCommit.Published =>
                  val actualState    = transition.state.revision
                  val actualSnapshot = published.snapshot.revision
                  if actualState != expected || actualSnapshot != expected then
                    Left(
                      CatalogReplayFailure.PublishedRevisionMismatch(
                        index,
                        expected,
                        entry.successorRevision,
                        actualState,
                        actualSnapshot,
                        lastSuccessful
                      )
                    )
                  else replayFrom(transition.state, entries, index + 1)
        end if

  private def nextRevision(revision: CatalogRevision): CatalogRevision =
    CatalogRevision.from(revision.value + 1).toOption.get

  private def resultFrom(state: CatalogState): CatalogReplayResult =
    resultConstructor.invoke(state).asInstanceOf[CatalogReplayResult]
end CatalogReplay
