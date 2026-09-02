package trading.execution

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported

enum SourceFactClassification extends JavaSerializationUnsupported:
  case Applied
  case DuplicateSourceEvent
  case DuplicateFillIdentity
  case ConflictingSourceEvent
  case ConflictingFillIdentity
  case ConflictingStreamPosition

@nowarn("msg=Ignoring.*qualifier")
final class SourceFactClassifications private[this] (private val values: Vector[SourceFactClassification])
  extends JavaSerializationUnsupported:

  def head: SourceFactClassification                     = values.head
  def toVector: Vector[SourceFactClassification]         = values
  def contains(value: SourceFactClassification): Boolean = values.contains(value)

  override def equals(other: Any): Boolean = other match
    case that: SourceFactClassifications => values == that.toVector
    case _                               => false
  override def hashCode(): Int = values.hashCode

object SourceFactClassifications:
  private val constructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SourceFactClassifications], MethodHandles.lookup())
      .findConstructor(classOf[SourceFactClassifications], MethodType.methodType(classOf[Unit], classOf[Vector[?]]))

  private def construct(values: Vector[SourceFactClassification]): SourceFactClassifications =
    constructor.invoke(values).asInstanceOf[SourceFactClassifications]

  private[execution] def from(values: Vector[SourceFactClassification]): SourceFactClassifications =
    if values.isEmpty then throw new IllegalArgumentException("source fact classifications must be non-empty")
    else construct(values.distinct)

@nowarn("msg=Ignoring.*qualifier")
final class SourceFactConflict[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val original: SourceFact[D, B, Q],
  val conflicting: SourceFact[D, B, Q])
  extends JavaSerializationUnsupported:

  val eventId: QualifiedSourceEventId = original.eventId

  override def equals(other: Any): Boolean = other match
    case that: SourceFactConflict[?, ?, ?] =>
      original == that.original && conflicting == that.conflicting
    case _ => false
  override def hashCode(): Int = (original, conflicting).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class FillIdentityConflict[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val original: ExecutionFill[D, B, Q],
  val conflicting: ExecutionFill[D, B, Q])
  extends JavaSerializationUnsupported:

  val fillId: QualifiedFillId = original.fillId

  override def equals(other: Any): Boolean = other match
    case that: FillIdentityConflict[?, ?, ?] =>
      original == that.original && conflicting == that.conflicting
    case _ => false
  override def hashCode(): Int = (original, conflicting).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class StreamPositionConflict[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val position: QualifiedStreamPosition,
  val claimants: Vector[SourceFact[D, B, Q]])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: StreamPositionConflict[?, ?, ?] =>
      position == that.position && claimants == that.claimants
    case _ => false
  override def hashCode(): Int = (position, claimants).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class UnresolvedFillReference[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val referencedFillId: QualifiedFillId,
  val modifier: FillModifier[D, B, Q])
  extends JavaSerializationUnsupported:

  override def equals(other: Any): Boolean = other match
    case that: UnresolvedFillReference[?, ?, ?] =>
      referencedFillId == that.referencedFillId && modifier == that.modifier
    case _ => false
  override def hashCode(): Int = (referencedFillId, modifier).hashCode

sealed abstract class SourceFactTransition[D <: Dim, B <: Dim, Q <: Dim] protected ()
  extends JavaSerializationUnsupported:
  SourceFactTransition.requireBuiltin(this)
  def state: SourceEvidenceState[D, B, Q]

@nowarn("msg=Ignoring.*qualifier")
final class SourceFactRecorded[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val state: SourceEvidenceState[D, B, Q],
  val classifications: SourceFactClassifications)
  extends SourceFactTransition[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: SourceFactRecorded[?, ?, ?] =>
      state == that.state && classifications == that.classifications
    case _ => false
  override def hashCode(): Int = (state, classifications).hashCode

@nowarn("msg=Ignoring.*qualifier")
final class SourceFactRejected[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val state: SourceEvidenceState[D, B, Q],
  val violations: SourceFactViolations)
  extends SourceFactTransition[D, B, Q]():

  override def equals(other: Any): Boolean = other match
    case that: SourceFactRejected[?, ?, ?] =>
      state == that.state && violations == that.violations
    case _ => false
  override def hashCode(): Int = (state, violations).hashCode

object SourceFactTransition:
  private[execution] def requireBuiltin(value: SourceFactTransition[?, ?, ?]): Unit =
    val runtimeClass = value.getClass
    if runtimeClass != classOf[SourceFactRecorded[?, ?, ?]] &&
      runtimeClass != classOf[SourceFactRejected[?, ?, ?]]
    then throw new IllegalAccessError(s"unsupported SourceFactTransition implementation: ${runtimeClass.getName}")

@nowarn("msg=Ignoring.*qualifier")
final class SourceEvidenceState[D <: Dim, B <: Dim, Q <: Dim] private[this] (
  val lifecycle: ExecutionLifecycle[D, B, Q],
  val factsByEvent: Map[QualifiedSourceEventId, SourceFact[D, B, Q]],
  val fillsById: Map[QualifiedFillId, ExecutionFill[D, B, Q]],
  val eventConflicts: Vector[SourceFactConflict[D, B, Q]],
  val fillConflicts: Vector[FillIdentityConflict[D, B, Q]],
  val positionClaimants: Map[QualifiedStreamPosition, Vector[SourceFact[D, B, Q]]],
  val positionConflicts: Map[QualifiedStreamPosition, StreamPositionConflict[D, B, Q]],
  val unresolvedFillReferences: Map[QualifiedFillId, Vector[UnresolvedFillReference[D, B, Q]]])
  extends JavaSerializationUnsupported:

  def record(fact: SourceFact[D, B, Q]): SourceFactTransition[D, B, Q] =
    SourceEvidenceState.record(this, fact)

  override def equals(other: Any): Boolean = other match
    case that: SourceEvidenceState[?, ?, ?] =>
      lifecycle == that.lifecycle && factsByEvent == that.factsByEvent && fillsById == that.fillsById &&
      eventConflicts == that.eventConflicts && fillConflicts == that.fillConflicts &&
      positionClaimants == that.positionClaimants && positionConflicts == that.positionConflicts &&
      unresolvedFillReferences == that.unresolvedFillReferences
    case _ => false

  override def hashCode(): Int =
    (
      lifecycle,
      factsByEvent,
      fillsById,
      eventConflicts,
      fillConflicts,
      positionClaimants,
      positionConflicts,
      unresolvedFillReferences
    ).hashCode
end SourceEvidenceState

object SourceEvidenceState:
  private val stateConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SourceEvidenceState[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[SourceEvidenceState[?, ?, ?]],
        MethodType.methodType(
          classOf[Unit],
          classOf[ExecutionLifecycle[?, ?, ?]],
          classOf[Map[?, ?]],
          classOf[Map[?, ?]],
          classOf[Vector[?]],
          classOf[Vector[?]],
          classOf[Map[?, ?]],
          classOf[Map[?, ?]],
          classOf[Map[?, ?]]
        )
      )

  private val eventConflictConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SourceFactConflict[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[SourceFactConflict[?, ?, ?]],
        MethodType.methodType(classOf[Unit], classOf[SourceFact[?, ?, ?]], classOf[SourceFact[?, ?, ?]])
      )

  private val fillConflictConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[FillIdentityConflict[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[FillIdentityConflict[?, ?, ?]],
        MethodType.methodType(classOf[Unit], classOf[ExecutionFill[?, ?, ?]], classOf[ExecutionFill[?, ?, ?]])
      )

  private val positionConflictConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[StreamPositionConflict[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[StreamPositionConflict[?, ?, ?]],
        MethodType.methodType(classOf[Unit], classOf[QualifiedStreamPosition], classOf[Vector[?]])
      )

  private val unresolvedConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[UnresolvedFillReference[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[UnresolvedFillReference[?, ?, ?]],
        MethodType.methodType(classOf[Unit], classOf[QualifiedFillId], classOf[FillModifier[?, ?, ?]])
      )

  private val recordedConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SourceFactRecorded[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[SourceFactRecorded[?, ?, ?]],
        MethodType.methodType(classOf[Unit], classOf[SourceEvidenceState[?, ?, ?]], classOf[SourceFactClassifications])
      )

  private val rejectedConstructor: MethodHandle =
    MethodHandles
      .privateLookupIn(classOf[SourceFactRejected[?, ?, ?]], MethodHandles.lookup())
      .findConstructor(
        classOf[SourceFactRejected[?, ?, ?]],
        MethodType.methodType(classOf[Unit], classOf[SourceEvidenceState[?, ?, ?]], classOf[SourceFactViolations])
      )

  private def constructState[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q],
    factsByEvent: Map[QualifiedSourceEventId, SourceFact[D, B, Q]],
    fillsById: Map[QualifiedFillId, ExecutionFill[D, B, Q]],
    eventConflicts: Vector[SourceFactConflict[D, B, Q]],
    fillConflicts: Vector[FillIdentityConflict[D, B, Q]],
    positionClaimants: Map[QualifiedStreamPosition, Vector[SourceFact[D, B, Q]]],
    positionConflicts: Map[QualifiedStreamPosition, StreamPositionConflict[D, B, Q]],
    unresolvedFillReferences: Map[QualifiedFillId, Vector[UnresolvedFillReference[D, B, Q]]]
  ): SourceEvidenceState[D, B, Q] =
    stateConstructor
      .invoke(
        lifecycle,
        factsByEvent,
        fillsById,
        eventConflicts,
        fillConflicts,
        positionClaimants,
        positionConflicts,
        unresolvedFillReferences
      )
      .asInstanceOf[SourceEvidenceState[D, B, Q]]

  private def constructEventConflict[D <: Dim, B <: Dim, Q <: Dim](
    original: SourceFact[D, B, Q],
    conflicting: SourceFact[D, B, Q]
  ): SourceFactConflict[D, B, Q] =
    eventConflictConstructor.invoke(original, conflicting).asInstanceOf[SourceFactConflict[D, B, Q]]

  private def constructFillConflict[D <: Dim, B <: Dim, Q <: Dim](
    original: ExecutionFill[D, B, Q],
    conflicting: ExecutionFill[D, B, Q]
  ): FillIdentityConflict[D, B, Q] =
    fillConflictConstructor.invoke(original, conflicting).asInstanceOf[FillIdentityConflict[D, B, Q]]

  private def constructPositionConflict[D <: Dim, B <: Dim, Q <: Dim](
    position: QualifiedStreamPosition,
    claimants: Vector[SourceFact[D, B, Q]]
  ): StreamPositionConflict[D, B, Q] =
    positionConflictConstructor.invoke(position, claimants).asInstanceOf[StreamPositionConflict[D, B, Q]]

  private def constructUnresolved[D <: Dim, B <: Dim, Q <: Dim](
    modifier: FillModifier[D, B, Q]
  ): UnresolvedFillReference[D, B, Q] =
    unresolvedConstructor
      .invoke(modifier.referencedFillId, modifier)
      .asInstanceOf[UnresolvedFillReference[D, B, Q]]

  private def recorded[D <: Dim, B <: Dim, Q <: Dim](
    state: SourceEvidenceState[D, B, Q],
    classifications: Vector[SourceFactClassification]
  ): SourceFactRecorded[D, B, Q] =
    recordedConstructor
      .invoke(state, SourceFactClassifications.from(classifications))
      .asInstanceOf[SourceFactRecorded[D, B, Q]]

  private def rejected[D <: Dim, B <: Dim, Q <: Dim](
    state: SourceEvidenceState[D, B, Q],
    violations: SourceFactViolations
  ): SourceFactRejected[D, B, Q] =
    rejectedConstructor.invoke(state, violations).asInstanceOf[SourceFactRejected[D, B, Q]]

  def initial[D <: Dim, B <: Dim, Q <: Dim](
    lifecycle: ExecutionLifecycle[D, B, Q]
  ): Either[SourceFactViolations, SourceEvidenceState[D, B, Q]] =
    if lifecycle == null then
      Left(SourceFactViolations.one(MissingSourceFactValue(SourceFactLocation.Lifecycle)))
    else
      Right(
        constructState(
          lifecycle,
          Map.empty,
          Map.empty,
          Vector.empty,
          Vector.empty,
          Map.empty,
          Map.empty,
          Map.empty
        )
      )

  private def record[D <: Dim, B <: Dim, Q <: Dim](
    state: SourceEvidenceState[D, B, Q],
    fact: SourceFact[D, B, Q]
  ): SourceFactTransition[D, B, Q] =
    SourceFactViolations.from(SourceFactValidation.fact(state.lifecycle, fact)) match
      case Some(violations) => rejected(state, violations)
      case None             =>
        state.factsByEvent.get(fact.eventId) match
          case Some(existing) if existing == fact =>
            recorded(state, Vector(SourceFactClassification.DuplicateSourceEvent))
          case Some(existing) =>
            val conflict = constructEventConflict(existing, fact)
            val retained =
              if state.eventConflicts.contains(conflict) then state.eventConflicts
              else state.eventConflicts :+ conflict
            recorded(
              constructState(
                state.lifecycle,
                state.factsByEvent,
                state.fillsById,
                retained,
                state.fillConflicts,
                state.positionClaimants,
                state.positionConflicts,
                state.unresolvedFillReferences
              ),
              Vector(SourceFactClassification.ConflictingSourceEvent)
            )
          case None => recordNewEvent(state, fact)

  private def recordNewEvent[D <: Dim, B <: Dim, Q <: Dim](
    state: SourceEvidenceState[D, B, Q],
    fact: SourceFact[D, B, Q]
  ): SourceFactRecorded[D, B, Q] =
    var fills           = state.fillsById
    var fillConflicts   = state.fillConflicts
    var unresolved      = state.unresolvedFillReferences
    val classifications = Vector.newBuilder[SourceFactClassification]
    classifications += SourceFactClassification.Applied

    fact match
      case fill: ExecutionFill[D, B, Q] =>
        fills.get(fill.fillId) match
          case None => fills = fills.updated(fill.fillId, fill)
          case Some(existing) if SourceFactEquality.sameFillBody(existing, fill) =>
            classifications += SourceFactClassification.DuplicateFillIdentity
          case Some(existing) =>
            val conflict = constructFillConflict(existing, fill)
            if !fillConflicts.contains(conflict) then fillConflicts = fillConflicts :+ conflict
            classifications += SourceFactClassification.ConflictingFillIdentity
        unresolved = unresolved.removed(fill.fillId)
      case modifier: FillModifier[D, B, Q] if !fills.contains(modifier.referencedFillId) =>
        val reference = constructUnresolved(modifier)
        val current   = unresolved.getOrElse(modifier.referencedFillId, Vector.empty)
        if !current.contains(reference) then
          unresolved = unresolved.updated(modifier.referencedFillId, current :+ reference)
      case _ => ()

    var claimants         = state.positionClaimants
    var positionConflicts = state.positionConflicts
    fact.authoritativePosition.foreach: position =>
      val current = claimants.getOrElse(position, Vector.empty)
      if current.nonEmpty && !current.contains(fact) then
        val next = current :+ fact
        claimants = claimants.updated(position, next)
        positionConflicts = positionConflicts.updated(position, constructPositionConflict(position, next))
        classifications += SourceFactClassification.ConflictingStreamPosition
      else if current.isEmpty then claimants = claimants.updated(position, Vector(fact))

    val next = constructState(
      state.lifecycle,
      state.factsByEvent.updated(fact.eventId, fact),
      fills,
      state.eventConflicts,
      fillConflicts,
      claimants,
      positionConflicts,
      unresolved
    )
    recorded(next, classifications.result())
  end recordNewEvent
end SourceEvidenceState
