package trading.execution

import trading.quantity.Dim
import trading.quantity.JavaSerializationUnsupported

enum SourceFactClassification extends JavaSerializationUnsupported:
  case Applied
  case DuplicateSourceEvent
  case DuplicateFillIdentity
  case ConflictingSourceEvent
  case ConflictingFillIdentity
  case ConflictingStreamPosition

final class SourceFactClassifications private (private val values: Vector[SourceFactClassification])
  extends JavaSerializationUnsupported:

  def head: SourceFactClassification                     = values.head
  def toVector: Vector[SourceFactClassification]         = values
  def contains(value: SourceFactClassification): Boolean = values.contains(value)

  override def equals(other: Any): Boolean = other match
    case that: SourceFactClassifications => values == that.toVector
    case _                               => false
  override def hashCode(): Int = values.hashCode

object SourceFactClassifications:
  private def construct(values: Vector[SourceFactClassification]): SourceFactClassifications =
    new SourceFactClassifications(values)

  private[execution] def from(values: Vector[SourceFactClassification]): SourceFactClassifications =
    if values.isEmpty then throw new IllegalArgumentException("source fact classifications must be non-empty")
    else construct(values.distinct)

final case class SourceFactConflict[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  original: SourceFact[D, B, Q],
  conflicting: SourceFact[D, B, Q])
  extends JavaSerializationUnsupported:

  val eventId: QualifiedSourceEventId = original.eventId

final case class FillIdentityConflict[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  original: ExecutionFill[D, B, Q],
  conflicting: ExecutionFill[D, B, Q])
  extends JavaSerializationUnsupported:

  val fillId: QualifiedFillId = original.fillId

final case class StreamPositionConflict[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  position: QualifiedStreamPosition,
  claimants: Vector[SourceFact[D, B, Q]])
  extends JavaSerializationUnsupported

final case class UnresolvedFillReference[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  referencedFillId: QualifiedFillId,
  modifier: FillModifier[D, B, Q])
  extends JavaSerializationUnsupported

sealed trait SourceFactTransition[D <: Dim, B <: Dim, Q <: Dim] extends JavaSerializationUnsupported:
  def state: SourceEvidenceState[D, B, Q]

final case class SourceFactRecorded[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  state: SourceEvidenceState[D, B, Q],
  classifications: SourceFactClassifications)
  extends SourceFactTransition[D, B, Q]

final case class SourceFactRejected[D <: Dim, B <: Dim, Q <: Dim] private[execution] (
  state: SourceEvidenceState[D, B, Q],
  violations: SourceFactViolations)
  extends SourceFactTransition[D, B, Q]

final class SourceEvidenceState[D <: Dim, B <: Dim, Q <: Dim] private (
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
    new SourceEvidenceState(
      lifecycle,
      factsByEvent,
      fillsById,
      eventConflicts,
      fillConflicts,
      positionClaimants,
      positionConflicts,
      unresolvedFillReferences
    )

  private def recorded[D <: Dim, B <: Dim, Q <: Dim](
    state: SourceEvidenceState[D, B, Q],
    classifications: Vector[SourceFactClassification]
  ): SourceFactRecorded[D, B, Q] =
    SourceFactRecorded(state, SourceFactClassifications.from(classifications))

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
      case Some(violations) => SourceFactRejected(state, violations)
      case None             =>
        state.factsByEvent.get(fact.eventId) match
          case Some(existing) if existing == fact =>
            recorded(state, Vector(SourceFactClassification.DuplicateSourceEvent))
          case Some(existing) =>
            val conflict = SourceFactConflict(existing, fact)
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
            val conflict = FillIdentityConflict(existing, fill)
            if !fillConflicts.contains(conflict) then fillConflicts = fillConflicts :+ conflict
            classifications += SourceFactClassification.ConflictingFillIdentity
        unresolved = unresolved.removed(fill.fillId)
      case modifier: FillModifier[D, B, Q] if !fills.contains(modifier.referencedFillId) =>
        val reference = UnresolvedFillReference(modifier.referencedFillId, modifier)
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
        positionConflicts = positionConflicts.updated(position, StreamPositionConflict(position, next))
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
