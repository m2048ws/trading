package trading.execution

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported

final case class TransitionWork(indexLookups: Int, indexUpdates: Int, fullHistoryScans: Int)
  extends JavaSerializationUnsupported

enum LifecycleTransitionKind extends JavaSerializationUnsupported:
  case Applied
  case IdempotentDuplicate
  case ConflictingEvidence

sealed abstract class LifecycleRejection extends JavaSerializationUnsupported with Product with Serializable
final case class CommandInputRejected(violations: CommandViolations)   extends LifecycleRejection
final case class SourceInputRejected(violations: SourceFactViolations) extends LifecycleRejection

sealed abstract class LifecycleTransition[D <: Dim, B <: Dim, Q <: Dim] protected ()
  extends JavaSerializationUnsupported:
  LifecycleTransition.requireBuiltin(this)
  def state: ExecutionState[D, B, Q]
  def work: TransitionWork

@nowarn("msg=Ignoring.*qualifier")
final class LifecycleAccepted[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val state: ExecutionState[D, B, Q],
  val kind: LifecycleTransitionKind,
  val work: TransitionWork)
  extends LifecycleTransition[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: LifecycleAccepted[?, ?, ?] =>
      state == that.state && kind == that.kind && work == that.work
    case _ => false
  override def hashCode(): Int = (state, kind, work).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class LifecycleRejected[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val state: ExecutionState[D, B, Q],
  val rejection: LifecycleRejection,
  val work: TransitionWork)
  extends LifecycleTransition[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: LifecycleRejected[?, ?, ?] =>
      state == that.state && rejection == that.rejection && work == that.work
    case _ => false
  override def hashCode(): Int = (state, rejection, work).hashCode

object LifecycleTransition:
  private[execution] def requireBuiltin(value: LifecycleTransition[?, ?, ?]): Unit =
    val runtimeClass = value.getClass
    if runtimeClass != classOf[LifecycleAccepted[?, ?, ?]] &&
      runtimeClass != classOf[LifecycleRejected[?, ?, ?]]
    then throw new IllegalAccessError(s"unsupported LifecycleTransition implementation: ${runtimeClass.getName}")

sealed abstract class LifecycleDiagnostic extends JavaSerializationUnsupported with Product with Serializable:
  def location: LifecycleDiagnosticLocation

enum LifecycleDiagnosticLocation extends JavaSerializationUnsupported:
  case Command
  case StreamPosition
  case Event
  case FillReference
  case Completeness

final case class MissingSourceRange(
  stream: QualifiedSourceStreamId,
  first: SourceSequence,
  last: SourceSequence)
  extends LifecycleDiagnostic:
  val location: LifecycleDiagnosticLocation = LifecycleDiagnosticLocation.StreamPosition

final case class StreamPositionConflictObserved(
  position: QualifiedStreamPosition,
  claimantCount: Int)
  extends LifecycleDiagnostic:
  val location: LifecycleDiagnosticLocation = LifecycleDiagnosticLocation.StreamPosition

final case class SourceRewindObserved(
  stream: QualifiedSourceStreamId,
  position: SourceSequence,
  previous: SourceSequence)
  extends LifecycleDiagnostic:
  val location: LifecycleDiagnosticLocation = LifecycleDiagnosticLocation.StreamPosition

final case class CommandConflictObserved(commandId: ApplicationCommandId) extends LifecycleDiagnostic:
  val location: LifecycleDiagnosticLocation = LifecycleDiagnosticLocation.Command

final case class SourceEventConflictObserved(eventId: QualifiedSourceEventId) extends LifecycleDiagnostic:
  val location: LifecycleDiagnosticLocation = LifecycleDiagnosticLocation.Event

final case class FillIdentityConflictObserved(fillId: QualifiedFillId) extends LifecycleDiagnostic:
  val location: LifecycleDiagnosticLocation = LifecycleDiagnosticLocation.FillReference

final case class UnresolvedFillObserved(fillId: QualifiedFillId, modifierEventId: QualifiedSourceEventId)
  extends LifecycleDiagnostic:
  val location: LifecycleDiagnosticLocation = LifecycleDiagnosticLocation.FillReference

final case class CompletenessNotEstablished(stream: QualifiedSourceStreamId) extends LifecycleDiagnostic:
  val location: LifecycleDiagnosticLocation = LifecycleDiagnosticLocation.Completeness

@nowarn("msg=Ignoring.*qualifier")
final class LifecycleDiagnostics private[this] (private val values: Vector[LifecycleDiagnostic])
  extends JavaSerializationUnsupported:

  def head: LifecycleDiagnostic             = values.head
  def toVector: Vector[LifecycleDiagnostic] = values
  def size: Int                             = values.size

  override def equals(other: Any): Boolean = other match
    case that: LifecycleDiagnostics => values == that.toVector
    case _                          => false
  override def hashCode(): Int = values.hashCode

object LifecycleDiagnostics:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[LifecycleDiagnostics], MethodHandles.lookup())
      .findConstructor(classOf[LifecycleDiagnostics], MethodType.methodType(classOf[Unit], classOf[Vector[?]]))

  private def construct(values: Vector[LifecycleDiagnostic]): LifecycleDiagnostics =
    constructor.invoke(values).asInstanceOf[LifecycleDiagnostics]

  private[execution] def from(values: Vector[LifecycleDiagnostic]): Option[LifecycleDiagnostics] =
    Option.when(values.nonEmpty)(construct(values))

@nowarn("msg=Ignoring.*qualifier")
final class LifecycleObservation[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val lifecycle: ExecutionLifecycle[D, B, Q],
  val submissionKnowledge: Option[SubmissionKnowledge[D, B, Q]],
  val cancellationKnowledge: Option[CancellationKnowledge[D, B, Q]],
  val issuedCommands: Map[ApplicationCommandId, ExecutionCommand[D, B, Q]],
  val sourceFacts: Map[QualifiedSourceEventId, SourceFact[D, B, Q]],
  val fills: Map[QualifiedFillId, ExecutionFill[D, B, Q]],
  val effectiveFillLedger: EffectiveFillLedger[D, B, Q],
  val anomalies: ExecutionAnomalies[D, B, Q],
  val commandConflicts: Vector[CommandConflict[D, B, Q]],
  val sourceEventConflicts: Vector[SourceFactConflict[D, B, Q]],
  val fillIdentityConflicts: Vector[FillIdentityConflict[D, B, Q]],
  val streamPositionConflicts: Map[QualifiedStreamPosition, StreamPositionConflict[D, B, Q]],
  val authoritativeCompleteness: Map[QualifiedSourceStreamId, SourceCompleteness],
  val incompleteStreams: Set[QualifiedSourceStreamId],
  val explicitlyUnsequencedEvents: Vector[QualifiedSourceEventId],
  val unresolvedFillReferences: Map[QualifiedFillId, Vector[UnresolvedFillReference[D, B, Q]]],
  val diagnostics: Option[LifecycleDiagnostics])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: LifecycleObservation[?, ?, ?] =>
      lifecycle == that.lifecycle && submissionKnowledge == that.submissionKnowledge &&
      cancellationKnowledge == that.cancellationKnowledge &&
      issuedCommands == that.issuedCommands &&
      sourceFacts == that.sourceFacts && fills == that.fills && effectiveFillLedger == that.effectiveFillLedger &&
      anomalies == that.anomalies &&
      commandConflicts == that.commandConflicts && sourceEventConflicts == that.sourceEventConflicts &&
      fillIdentityConflicts == that.fillIdentityConflicts &&
      streamPositionConflicts == that.streamPositionConflicts &&
      authoritativeCompleteness == that.authoritativeCompleteness &&
      incompleteStreams == that.incompleteStreams &&
      explicitlyUnsequencedEvents == that.explicitlyUnsequencedEvents &&
      unresolvedFillReferences == that.unresolvedFillReferences && diagnostics == that.diagnostics
    case _ => false

  override def hashCode(): Int =
    (
      lifecycle,
      submissionKnowledge,
      cancellationKnowledge,
      issuedCommands,
      sourceFacts,
      fills,
      effectiveFillLedger,
      anomalies,
      commandConflicts,
      sourceEventConflicts,
      fillIdentityConflicts,
      streamPositionConflicts,
      authoritativeCompleteness,
      incompleteStreams,
      explicitlyUnsequencedEvents,
      unresolvedFillReferences,
      diagnostics
    ).hashCode
end LifecycleObservation

@nowarn("msg=Ignoring.*qualifier")
final class ExecutionState[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val lifecycle: ExecutionLifecycle[D, B, Q],
  val commands: CommandState[D, B, Q],
  val source: SourceEvidenceState[D, B, Q],
  val lastWork: TransitionWork)
  extends JavaSerializationUnsupported:

  def record(command: ExecutionCommand[D, B, Q]): LifecycleTransition[D, B, Q] =
    ExecutionState.recordCommand(this, command)

  def observeDispatch(evidence: DispatchEvidence[D, B, Q]): LifecycleTransition[D, B, Q] =
    ExecutionState.recordDispatch(this, evidence)

  def record(fact: SourceFact[D, B, Q]): LifecycleTransition[D, B, Q] =
    ExecutionState.recordSource(this, fact)

  def observation: LifecycleObservation[D, B, Q] = ExecutionState.observe(this)

  override def equals(other: Any): Boolean = other match
    case that: ExecutionState[?, ?, ?] =>
      lifecycle == that.lifecycle && commands == that.commands && source == that.source
    case _ => false

  override def hashCode(): Int = (lifecycle, commands, source).hashCode
end ExecutionState

@nowarn("msg=Ignoring.*qualifier")
final class LifecycleReplayResult[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val state: ExecutionState[D, B, Q],
  val rejections: Vector[LifecycleRejection])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: LifecycleReplayResult[?, ?, ?] =>
      state == that.state && rejections == that.rejections
    case _ => false
  override def hashCode(): Int = (state, rejections).hashCode

object ExecutionState:
  private val stateConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[ExecutionState[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[ExecutionState[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[ExecutionLifecycle[?, ?, ?]],
          classOf[CommandState[?, ?, ?]],
          classOf[SourceEvidenceState[?, ?, ?]],
          classOf[TransitionWork]
        )
      )

  private val acceptedConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[LifecycleAccepted[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[LifecycleAccepted[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[ExecutionState[?, ?, ?]],
          classOf[LifecycleTransitionKind],
          classOf[TransitionWork]
        )
      )

  private val rejectedConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[LifecycleRejected[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[LifecycleRejected[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[ExecutionState[?, ?, ?]],
          classOf[LifecycleRejection],
          classOf[TransitionWork]
        )
      )

  private val diagnosticsConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[LifecycleObservation[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[LifecycleObservation[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[ExecutionLifecycle[?, ?, ?]],
          classOf[Option[?]],
          classOf[Option[?]],
          classOf[Map[?, ?]],
          classOf[Map[?, ?]],
          classOf[Map[?, ?]],
          classOf[EffectiveFillLedger[?, ?, ?]],
          classOf[ExecutionAnomalies[?, ?, ?]],
          classOf[Vector[?]],
          classOf[Vector[?]],
          classOf[Vector[?]],
          classOf[Map[?, ?]],
          classOf[Map[?, ?]],
          classOf[Set[?]],
          classOf[Vector[?]],
          classOf[Map[?, ?]],
          classOf[Option[?]]
        )
      )

  private val replayConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[LifecycleReplayResult[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[LifecycleReplayResult[?, ?, ?]],
        MethodType.methodType(classOf[Unit], classOf[ExecutionState[?, ?, ?]], classOf[Vector[?]])
      )

  private def construct[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q],
    commands: CommandState[D, B, Q],
    source: SourceEvidenceState[D, B, Q],
    work: TransitionWork
  ): ExecutionState[D, B, Q] =
    stateConstructor.invoke(lifecycle, commands, source, work).asInstanceOf[ExecutionState[D, B, Q]]

  private def accepted[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q],
    kind: LifecycleTransitionKind,
    work: TransitionWork
  ): LifecycleAccepted[D, B, Q] =
    acceptedConstructor.invoke(state, kind, work).asInstanceOf[LifecycleAccepted[D, B, Q]]

  private def rejected[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q],
    rejection: LifecycleRejection,
    work: TransitionWork
  ): LifecycleRejected[D, B, Q] =
    rejectedConstructor.invoke(state, rejection, work).asInstanceOf[LifecycleRejected[D, B, Q]]

  def initial[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  ): Either[CommandViolations, ExecutionState[D, B, Q]] =
    CommandState.initial(lifecycle).flatMap: commands =>
      SourceEvidenceState.initial(lifecycle) match
        case Right(source) => Right(construct(lifecycle, commands, source, TransitionWork(0, 0, 0)))
        case Left(_)       =>
          Left(CommandViolations.one(MissingCommandValue(CommandViolationLocation.Lifecycle)))

  private def recordCommand[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q],
    command: ExecutionCommand[D, B, Q]
  ): LifecycleTransition[D, B, Q] =
    val indeterminateCommandId = state.commands.dispatchKnowledge.valuesIterator.flatten.collectFirst:
      case value: IndeterminateDispatch[D, B, Q] => value.submit.commandId
    (command, indeterminateCommandId) match
      case (submit: SubmitOrderCommand[D, B, Q], Some(originalCommandId))
        if submit.executionOrderId == state.lifecycle.executionOrderId &&
          !state.commands.issuedCommands.get(submit.commandId).contains(submit) =>
        val work = TransitionWork(1, 0, 0)
        rejected(
          construct(state.lifecycle, state.commands, state.source, work),
          CommandInputRejected(
            CommandViolations.one(FreshSubmitBlockedByIndeterminate(originalCommandId, submit.commandId))
          ),
          work
        )
      case _ =>
        val result = state.commands.record(command)
        val work   = TransitionWork(1, if result.state == state.commands then 0 else 1, 0)
        val next   = construct(state.lifecycle, result.state, state.source, work)
        result.kind match
          case CommandTransitionKind.Applied             => accepted(next, LifecycleTransitionKind.Applied, work)
          case CommandTransitionKind.IdempotentDuplicate =>
            accepted(next, LifecycleTransitionKind.IdempotentDuplicate, work)
          case CommandTransitionKind.ConflictingCommand | CommandTransitionKind.ConflictingDispatchEvidence =>
            accepted(next, LifecycleTransitionKind.ConflictingEvidence, work)
          case CommandTransitionKind.Rejected =>
            rejected(next, CommandInputRejected(result.violations.get), work)
    end match
  end recordCommand

  private def recordDispatch[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q],
    evidence: DispatchEvidence[D, B, Q]
  ): LifecycleTransition[D, B, Q] =
    val result = state.commands.observeDispatch(evidence)
    val work   = TransitionWork(1, if result.state == state.commands then 0 else 1, 0)
    val next   = construct(state.lifecycle, result.state, state.source, work)
    result.kind match
      case CommandTransitionKind.Applied             => accepted(next, LifecycleTransitionKind.Applied, work)
      case CommandTransitionKind.IdempotentDuplicate =>
        accepted(next, LifecycleTransitionKind.IdempotentDuplicate, work)
      case CommandTransitionKind.ConflictingCommand | CommandTransitionKind.ConflictingDispatchEvidence =>
        accepted(next, LifecycleTransitionKind.ConflictingEvidence, work)
      case CommandTransitionKind.Rejected =>
        rejected(next, CommandInputRejected(result.violations.get), work)

  private def recordSource[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q],
    fact: SourceFact[D, B, Q]
  ): LifecycleTransition[D, B, Q] =
    val result = state.source.record(fact)
    val work   = TransitionWork(2, if result.state == state.source then 0 else 2, 0)
    val next   = construct(state.lifecycle, state.commands, result.state, work)
    result match
      case recorded: SourceFactRecorded[D, B, Q] =>
        val kinds = recorded.classifications
        if
          kinds.contains(SourceFactClassification.ConflictingSourceEvent) ||
          kinds.contains(SourceFactClassification.ConflictingFillIdentity) ||
          kinds.contains(SourceFactClassification.ConflictingStreamPosition)
        then accepted(next, LifecycleTransitionKind.ConflictingEvidence, work)
        else if kinds.toVector == Vector(SourceFactClassification.DuplicateSourceEvent) then
          accepted(next, LifecycleTransitionKind.IdempotentDuplicate, work)
        else accepted(next, LifecycleTransitionKind.Applied, work)
      case rejectedSource: SourceFactRejected[D, B, Q] =>
        rejected(next, SourceInputRejected(rejectedSource.violations), work)

  def replay[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  )(
    commands: Vector[ExecutionCommand[D, B, Q]],
    dispatch: Vector[DispatchEvidence[D, B, Q]],
    facts: Vector[SourceFact[D, B, Q]]
  ): Either[CommandViolations, LifecycleReplayResult[D, B, Q]] =
    initial(lifecycle).map: empty =>
      var state                                                   = empty
      val rejections                                              = Vector.newBuilder[LifecycleRejection]
      def recordCommand(command: ExecutionCommand[D, B, Q]): Unit =
        state.record(command) match
          case value: LifecycleAccepted[D, B, Q] => state = value.state
          case value: LifecycleRejected[D, B, Q] =>
            state = value.state
            rejections += value.rejection

      val sortedCommands      = commands.sortBy(commandSortKey)
      val sortedDispatch      = dispatch.sortBy(dispatchSortKey)
      val dispatchedSubmitIds = sortedDispatch.collect:
        case evidence if evidence != null => evidence.submitCommandId
      .toSet
      val (dispatchedCommands, remainingCommands) = sortedCommands.partition: command =>
        command != null && dispatchedSubmitIds.contains(command.commandId)
      dispatchedCommands.foreach(recordCommand)
      sortedDispatch.foreach: evidence =>
        state.observeDispatch(evidence) match
          case value: LifecycleAccepted[D, B, Q] => state = value.state
          case value: LifecycleRejected[D, B, Q] =>
            state = value.state
            rejections += value.rejection
      remainingCommands.foreach(recordCommand)
      facts.sortBy(factSortKey).foreach: fact =>
        state.record(fact) match
          case value: LifecycleAccepted[D, B, Q] => state = value.state
          case value: LifecycleRejected[D, B, Q] =>
            state = value.state
            rejections += value.rejection
      replayConstructor.invoke(state, rejections.result()).asInstanceOf[LifecycleReplayResult[D, B, Q]]

  private def observe[D <: Dim, B <: Dim, Q <: Dim](
    state: ExecutionState[D, B, Q]
  ): LifecycleObservation[D, B, Q] =
    val facts   = state.source.factsByEvent.values.toVector
    val ordered = facts.flatMap: fact =>
      fact.authoritativePosition.map(position => (position, fact.ordering))
    val checkpointEvidence = facts.collect:
      case value: ReconciliationCheckpoint[D, B, Q] => value.checkpoint
    val completenessEvidence = facts.collect:
      case value: SourceOrderCompleted[D, B, Q] => value.completeness
      case value: SourceOrderAbsent[D, B, Q]    => value.completeness

    val positionsByStream    = state.source.positionClaimants.keys.groupBy(_.stream)
    val checkpointsByStream  = checkpointEvidence.groupBy(_.position.stream)
    val completenessByStream = completenessEvidence.groupBy(_.completeThrough.stream)
    val streams              = positionsByStream.keySet ++ checkpointsByStream.keySet ++ completenessByStream.keySet

    val streamDiagnostics = streams.toVector.flatMap: stream =>
      val observedPositions =
        positionsByStream.getOrElse(stream, Vector.empty).map(_.sequence.value).toSet ++
          checkpointsByStream.getOrElse(stream, Vector.empty).map(_.position.sequence.value)
      val requestedRanges = Vector.newBuilder[(BigInt, BigInt)]
      val rewinds         = Vector.newBuilder[LifecycleDiagnostic]

      ordered.foreach: (position, sourceOrdering) =>
        if position.stream == stream then
          sourceOrdering match
            case sequenced: AuthoritativelySequenced =>
              addContinuationRange(position, sequenced.continuation, requestedRanges, rewinds)
            case ExplicitlyUnsequenced => ()
      checkpointsByStream.getOrElse(stream, Vector.empty).foreach: checkpoint =>
        addContinuationRange(checkpoint.position, checkpoint.continuation, requestedRanges, rewinds)
      completenessByStream.getOrElse(stream, Vector.empty).foreach: complete =>
        requestedRanges += ((BigInt(0), complete.completeThrough.sequence.value))

      val missing = subtractObserved(mergeRanges(requestedRanges.result()), observedPositions).map:
        case (first, last) =>
          MissingSourceRange(stream, SourceSequence.from(first).toOption.get, SourceSequence.from(last).toOption.get)
      val conflicts = state.source.positionConflicts.values.toVector
        .filter(_.position.stream == stream)
        .map(value => StreamPositionConflictObserved(value.position, value.claimants.size))
      val highest           = observedPositions.maxOption
      val checkpointRewinds = highest.toVector.flatMap: high =>
        checkpointsByStream
          .getOrElse(stream, Vector.empty)
          .filter(_.position.sequence.value < high)
          .map: checkpoint =>
            SourceRewindObserved(stream, checkpoint.position.sequence, SourceSequence.from(high).toOption.get)
      missing ++ conflicts ++ rewinds.result() ++ checkpointRewinds

    val nonStreamDiagnostics =
      state.commands.conflicts.map(value => CommandConflictObserved(value.commandId)) ++
        state.source.eventConflicts.map(value => SourceEventConflictObserved(value.eventId)) ++
        state.source.fillConflicts.map(value => FillIdentityConflictObserved(value.fillId)) ++
        state.source.unresolvedFillReferences.values.toVector.flatten.map: value =>
          UnresolvedFillObserved(value.referencedFillId, value.modifier.eventId)

    val highestCompleteness = completenessByStream.view.mapValues: values =>
      values.maxBy(_.completeThrough.sequence.value)
    .toMap
    val streamProblems = streamDiagnostics.collect:
      case value: MissingSourceRange             => value.stream
      case value: StreamPositionConflictObserved => value.position.stream
      case value: SourceRewindObserved           => value.stream
    .toSet
    val unresolvedTargets         = state.source.unresolvedFillReferences.keys.map(_.target).toSet
    val authoritativeCompleteness = highestCompleteness.filter: (stream, _) =>
      !streamProblems.contains(stream) && !unresolvedTargets.contains(stream.target)
    val incompleteStreams  = streams -- authoritativeCompleteness.keySet
    val absentCompleteness = incompleteStreams.diff(streamProblems).toVector.map(CompletenessNotEstablished.apply)
    val diagnostics        = (streamDiagnostics ++ nonStreamDiagnostics ++ absentCompleteness)
      .distinct
      .sortBy(diagnosticKey)
    val unsequenced = facts.collect:
      case fact if fact.ordering == ExplicitlyUnsequenced => fact.eventId
    .sortBy(eventKey)

    val effectiveFillLedger = EffectiveFillLedger.derive(state)
    diagnosticsConstructor
      .invoke(
        state.lifecycle,
        SubmissionKnowledge.derive(state, authoritativeCompleteness.keySet),
        CancellationKnowledge.derive(state),
        state.commands.issuedCommands,
        state.source.factsByEvent,
        state.source.fillsById,
        effectiveFillLedger,
        ExecutionAnomalies.derive(state),
        state.commands.conflicts,
        state.source.eventConflicts,
        state.source.fillConflicts,
        state.source.positionConflicts,
        authoritativeCompleteness,
        incompleteStreams,
        unsequenced,
        state.source.unresolvedFillReferences,
        LifecycleDiagnostics.from(diagnostics)
      )
      .asInstanceOf[LifecycleObservation[D, B, Q]]
  end observe

  private def addContinuationRange(
    position: QualifiedStreamPosition,
    continuation: SourceContinuation,
    ranges: scala.collection.mutable.Builder[(BigInt, BigInt), Vector[(BigInt, BigInt)]],
    rewinds: scala.collection.mutable.Builder[LifecycleDiagnostic, Vector[LifecycleDiagnostic]]
  ): Unit =
    continuation.previous match
      case None =>
        if position.sequence.value > 0 then ranges += ((BigInt(0), position.sequence.value - 1))
      case Some(previous) =>
        if previous.sequence.value >= position.sequence.value then
          rewinds += SourceRewindObserved(position.stream, position.sequence, previous.sequence)
        else if previous.sequence.value + 1 < position.sequence.value then
          ranges += ((previous.sequence.value + 1, position.sequence.value - 1))

  private def mergeRanges(values: Vector[(BigInt, BigInt)]): Vector[(BigInt, BigInt)] =
    values.filter((first, last) => first <= last).sortBy(_._1).foldLeft(Vector.empty[(BigInt, BigInt)]):
      case (Vector(), next) => Vector(next)
      case (acc, next)      =>
        val (first, last) = acc.last
        if next._1 <= last + 1 then acc.updated(acc.size - 1, (first, last.max(next._2)))
        else acc :+ next

  private def subtractObserved(
    ranges: Vector[(BigInt, BigInt)],
    observed: Set[BigInt]
  ): Vector[(BigInt, BigInt)] =
    ranges.flatMap: (first, last) =>
      observed.filter(value => value >= first && value <= last).toVector.sorted
        .foldLeft(Vector((first, last))): (parts, value) =>
          parts.flatMap: (start, end) =>
            if value < start || value > end then Vector((start, end))
            else Vector((start, value - 1), (value + 1, end)).filter((a, b) => a <= b)

  private def commandKey(command: ExecutionCommand[?, ?, ?]): String =
    val kind = command match
      case _: SubmitOrderCommand[?, ?, ?]      => "0-submit"
      case cancel: CancelOrderCommand[?, ?, ?] => s"1-cancel-${cancel.originalSubmitCommandId.value}"
    s"$kind|${command.commandId.value}|${lifecycleKey(command.lifecycle)}"

  private def commandSortKey(command: ExecutionCommand[?, ?, ?]): String =
    if command == null then "0-null" else s"1|${commandKey(command)}"

  private def dispatchKey(evidence: DispatchEvidence[?, ?, ?]): String =
    val kind = evidence match
      case _: ProvenNotDispatched[?, ?, ?]   => "0"
      case _: IndeterminateDispatch[?, ?, ?] => "1"
    s"${evidence.submitCommandId.value}|$kind|${commandKey(evidence.submit)}"

  private def dispatchSortKey(evidence: DispatchEvidence[?, ?, ?]): String =
    if evidence == null then "0-null" else s"1|${dispatchKey(evidence)}"

  private def factKey(fact: SourceFact[?, ?, ?]): String =
    val body = fact match
      case _: OrderAccepted[?, ?, ?]    => "0-accepted"
      case _: OrderRejected[?, ?, ?]    => "1-rejected"
      case fill: ExecutionFill[?, ?, ?] =>
        s"2-fill-${fillKey(fill.fillId)}-${fill.lots.count.unrefined}-${fill.price.coefficient}"
      case correction: FillCorrected[?, ?, ?] =>
        s"3-correction-${fillKey(correction.referencedFillId)}-${correction.replacementLots.count.unrefined}-${correction.replacementPrice.coefficient}"
      case bust: FillBusted[?, ?, ?]                     => s"4-bust-${fillKey(bust.referencedFillId)}"
      case _: CancellationEffective[?, ?, ?]             => "5-cancelled"
      case checkpoint: ReconciliationCheckpoint[?, ?, ?] =>
        s"6-checkpoint-${positionKey(checkpoint.checkpoint.position)}-${continuationKey(checkpoint.checkpoint.continuation)}"
      case complete: SourceOrderCompleted[?, ?, ?] =>
        s"7-complete-${positionKey(complete.completeness.completeThrough)}"
      case absent: SourceOrderAbsent[?, ?, ?] =>
        s"8-absent-${positionKey(absent.completeness.completeThrough)}"
    s"${eventKey(fact.eventId)}|${fact.executionOrderId.value}|${sourceOrderKey(fact.sourceOrderId)}|$body|${orderingKey(fact.ordering)}"

  private def factSortKey(fact: SourceFact[?, ?, ?]): String =
    if fact == null then "0-null" else s"1|${factKey(fact)}"

  private def lifecycleKey(value: ExecutionLifecycle[?, ?, ?]): String =
    s"${value.executionOrderId.value}|${value.lineageId.value}|${value.target.source.value}|${value.target.account.value}|${value.order.intent.side}|${value.orderedLots.count.unrefined}|${value.order.execution}"

  private def orderingKey(value: SourceOrdering): String = value match
    case ExplicitlyUnsequenced               => "unsequenced"
    case sequenced: AuthoritativelySequenced =>
      s"${positionKey(sequenced.position)}|${continuationKey(sequenced.continuation)}"

  private def continuationKey(value: SourceContinuation): String =
    val previous = value.previous.map(positionKey).getOrElse("origin")
    s"${streamKey(value.stream)}|$previous"

  private def diagnosticKey(value: LifecycleDiagnostic): String = value match
    case diagnostic: CommandConflictObserved => s"0|${diagnostic.commandId.value}"
    case diagnostic: MissingSourceRange      =>
      s"1|${streamKey(diagnostic.stream)}|0|${diagnostic.first.value}|${diagnostic.last.value}"
    case diagnostic: StreamPositionConflictObserved =>
      s"1|${streamKey(diagnostic.position.stream)}|1|${diagnostic.position.sequence.value}"
    case diagnostic: SourceRewindObserved =>
      s"1|${streamKey(diagnostic.stream)}|2|${diagnostic.position.value}|${diagnostic.previous.value}"
    case diagnostic: SourceEventConflictObserved  => s"2|${eventKey(diagnostic.eventId)}"
    case diagnostic: FillIdentityConflictObserved =>
      s"3|${targetKey(diagnostic.fillId.target)}|${diagnostic.fillId.native.value}"
    case diagnostic: UnresolvedFillObserved =>
      s"3|${targetKey(diagnostic.fillId.target)}|${diagnostic.fillId.native.value}|${eventKey(diagnostic.modifierEventId)}"
    case diagnostic: CompletenessNotEstablished => s"4|${streamKey(diagnostic.stream)}"

  private def eventKey(value: QualifiedSourceEventId): String =
    s"${targetKey(value.target)}|${value.native.value}"

  private def sourceOrderKey(value: QualifiedSourceOrderId): String =
    s"${targetKey(value.target)}|${value.native.value}"

  private def fillKey(value: QualifiedFillId): String =
    s"${targetKey(value.target)}|${value.native.value}"

  private def positionKey(value: QualifiedStreamPosition): String =
    s"${streamKey(value.stream)}|${value.sequence.value}"

  private def streamKey(value: QualifiedSourceStreamId): String =
    s"${targetKey(value.target)}|${value.native.value}"

  private def targetKey(value: ExecutionTarget): String =
    s"${value.source.value}|${value.account.value}"
end ExecutionState
