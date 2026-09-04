package trading.execution

import trading.economics.instrument.Instrument
import trading.economics.instrument.Lots
import trading.economics.instrument.PositionLots
import trading.economics.instrument.Price
import trading.order.*
import trading.quantity.DimKey
import trading.quantity.Rational
import trading.reference.GridHandle
import trading.reference.GridIdentity

/** Owner-local deterministic orderings for execution evidence and derived observations. */
private[execution] object ExecutionOrderings:
  val command: Ordering[ExecutionCommand[?, ?, ?]] =
    Ordering.fromLessThan((left, right) => compareCommand(left, right) < 0)

  val dispatchEvidence: Ordering[DispatchEvidence[?, ?, ?]] =
    Ordering.fromLessThan((left, right) => compareDispatchEvidence(left, right) < 0)

  val sourceFact: Ordering[SourceFact[?, ?, ?]] =
    Ordering.fromLessThan((left, right) => compareSourceFact(left, right) < 0)

  val fillModifier: Ordering[FillModifier[?, ?, ?]] =
    Ordering.fromLessThan((left, right) => compareFillModifier(left, right) < 0)

  val sourceFactConflict: Ordering[SourceFactConflict[?, ?, ?]] =
    Ordering.fromLessThan((left, right) => compareSourceFactConflict(left, right) < 0)

  val fillIdentityConflict: Ordering[FillIdentityConflict[?, ?, ?]] =
    Ordering.fromLessThan((left, right) => compareFillIdentityConflict(left, right) < 0)

  val diagnostic: Ordering[LifecycleDiagnostic] =
    Ordering.fromLessThan((left, right) => compareDiagnostic(left, right) < 0)

  val qualifiedSourceEventId: Ordering[QualifiedSourceEventId] =
    Ordering.fromLessThan((left, right) => compareEventId(left, right) < 0)

  val qualifiedSourceOrderId: Ordering[QualifiedSourceOrderId] =
    Ordering.fromLessThan((left, right) => compareSourceOrderId(left, right) < 0)

  val qualifiedFillId: Ordering[QualifiedFillId] =
    Ordering.fromLessThan((left, right) => compareFillId(left, right) < 0)

  val qualifiedSourceStreamId: Ordering[QualifiedSourceStreamId] =
    Ordering.fromLessThan((left, right) => compareStream(left, right) < 0)

  val qualifiedStreamPosition: Ordering[QualifiedStreamPosition] =
    Ordering.fromLessThan((left, right) => comparePosition(left, right) < 0)

  val postCancellationFillAnomaly: Ordering[PostCancellationFillAnomaly[?, ?, ?]] =
    Ordering.fromLessThan((left, right) => comparePostCancellationFill(left, right) < 0)

  def comparePosition(left: QualifiedStreamPosition, right: QualifiedStreamPosition): Int =
    compareStream(left.stream, right.stream)
      .orElseCompare(compareBigInt(left.sequence.value, right.sequence.value))

  def compareFillModifier(left: FillModifier[?, ?, ?], right: FillModifier[?, ?, ?]): Int =
    compareOption(left.authoritativePosition, right.authoritativePosition)(comparePosition)
      .orElseCompare(compareSourceFact(left, right))

  def compareCancellation(
    left: CancellationEffective[?, ?, ?],
    right: CancellationEffective[?, ?, ?]
  ): Int = compareSourceFact(left, right)

  extension (first: Int)
    private inline def orElseCompare(second: => Int): Int =
      if first != 0 then first else second

  private def compareCommand(left: ExecutionCommand[?, ?, ?], right: ExecutionCommand[?, ?, ?]): Int =
    compareNullable(left, right): (nonNullLeft, nonNullRight) =>
      compareInt(commandRank(nonNullLeft), commandRank(nonNullRight))
        .orElseCompare(compareCommandAlternative(nonNullLeft, nonNullRight))
        .orElseCompare(compareText(nonNullLeft.commandId.value, nonNullRight.commandId.value))
        .orElseCompare(compareLifecycle(nonNullLeft.lifecycle, nonNullRight.lifecycle))

  private def commandRank(value: ExecutionCommand[?, ?, ?]): Int = value match
    case _: SubmitOrderCommand[?, ?, ?] => 0
    case _: CancelOrderCommand[?, ?, ?] => 1

  private def compareCommandAlternative(
    left: ExecutionCommand[?, ?, ?],
    right: ExecutionCommand[?, ?, ?]
  ): Int = (left, right) match
    case (_: SubmitOrderCommand[?, ?, ?], _: SubmitOrderCommand[?, ?, ?])                    => 0
    case (leftCancel: CancelOrderCommand[?, ?, ?], rightCancel: CancelOrderCommand[?, ?, ?]) =>
      compareText(leftCancel.originalSubmitCommandId.value, rightCancel.originalSubmitCommandId.value)
    case _ => 0

  private def compareDispatchEvidence(
    left: DispatchEvidence[?, ?, ?],
    right: DispatchEvidence[?, ?, ?]
  ): Int =
    compareNullable(left, right): (nonNullLeft, nonNullRight) =>
      compareText(nonNullLeft.submitCommandId.value, nonNullRight.submitCommandId.value)
        .orElseCompare(compareInt(dispatchRank(nonNullLeft), dispatchRank(nonNullRight)))
        .orElseCompare(compareCommand(nonNullLeft.submit, nonNullRight.submit))

  private def dispatchRank(value: DispatchEvidence[?, ?, ?]): Int = value match
    case _: ProvenNotDispatched[?, ?, ?]   => 0
    case _: IndeterminateDispatch[?, ?, ?] => 1

  private def compareSourceFact(left: SourceFact[?, ?, ?], right: SourceFact[?, ?, ?]): Int =
    compareNullable(left, right): (nonNullLeft, nonNullRight) =>
      compareEventId(nonNullLeft.eventId, nonNullRight.eventId)
        .orElseCompare(compareText(nonNullLeft.executionOrderId.value, nonNullRight.executionOrderId.value))
        .orElseCompare(compareSourceOrderId(nonNullLeft.sourceOrderId, nonNullRight.sourceOrderId))
        .orElseCompare(compareInt(sourceFactRank(nonNullLeft), sourceFactRank(nonNullRight)))
        .orElseCompare(compareSourceFactBody(nonNullLeft, nonNullRight))
        .orElseCompare(compareSourceOrdering(nonNullLeft.ordering, nonNullRight.ordering))

  private def sourceFactRank(value: SourceFact[?, ?, ?]): Int = value match
    case _: OrderAccepted[?, ?, ?]            => 0
    case _: OrderRejected[?, ?, ?]            => 1
    case _: ExecutionFill[?, ?, ?]            => 2
    case _: FillCorrected[?, ?, ?]            => 3
    case _: FillBusted[?, ?, ?]               => 4
    case _: CancellationEffective[?, ?, ?]    => 5
    case _: ReconciliationCheckpoint[?, ?, ?] => 6
    case _: SourceOrderCompleted[?, ?, ?]     => 7
    case _: SourceOrderAbsent[?, ?, ?]        => 8

  private def compareSourceFactBody(left: SourceFact[?, ?, ?], right: SourceFact[?, ?, ?]): Int =
    (left, right) match
      case (_: OrderAccepted[?, ?, ?], _: OrderAccepted[?, ?, ?])                 => 0
      case (_: OrderRejected[?, ?, ?], _: OrderRejected[?, ?, ?])                 => 0
      case (_: CancellationEffective[?, ?, ?], _: CancellationEffective[?, ?, ?]) => 0
      case (leftFill: ExecutionFill[?, ?, ?], rightFill: ExecutionFill[?, ?, ?])  =>
        compareFillId(leftFill.fillId, rightFill.fillId)
          .orElseCompare(compareBigInt(leftFill.lots.count.unrefined, rightFill.lots.count.unrefined))
          .orElseCompare(compareRational(leftFill.price.coefficient, rightFill.price.coefficient))
          .orElseCompare(compareLots(leftFill.lots, rightFill.lots))
          .orElseCompare(comparePrice(leftFill.price, rightFill.price))
      case (leftCorrection: FillCorrected[?, ?, ?], rightCorrection: FillCorrected[?, ?, ?]) =>
        compareFillId(leftCorrection.referencedFillId, rightCorrection.referencedFillId)
          .orElseCompare(
            compareBigInt(
              leftCorrection.replacementLots.count.unrefined,
              rightCorrection.replacementLots.count.unrefined
            )
          )
          .orElseCompare(
            compareRational(leftCorrection.replacementPrice.coefficient, rightCorrection.replacementPrice.coefficient)
          )
          .orElseCompare(compareLots(leftCorrection.replacementLots, rightCorrection.replacementLots))
          .orElseCompare(comparePrice(leftCorrection.replacementPrice, rightCorrection.replacementPrice))
      case (leftBust: FillBusted[?, ?, ?], rightBust: FillBusted[?, ?, ?]) =>
        compareFillId(leftBust.referencedFillId, rightBust.referencedFillId)
      case (
          leftCheckpoint: ReconciliationCheckpoint[?, ?, ?],
          rightCheckpoint: ReconciliationCheckpoint[?, ?, ?]
        ) =>
        compareCheckpoint(leftCheckpoint.checkpoint, rightCheckpoint.checkpoint)
      case (leftComplete: SourceOrderCompleted[?, ?, ?], rightComplete: SourceOrderCompleted[?, ?, ?]) =>
        comparePosition(leftComplete.completeness.completeThrough, rightComplete.completeness.completeThrough)
      case (leftAbsent: SourceOrderAbsent[?, ?, ?], rightAbsent: SourceOrderAbsent[?, ?, ?]) =>
        comparePosition(leftAbsent.completeness.completeThrough, rightAbsent.completeness.completeThrough)
      case _ => 0

  private def compareSourceFactConflict(
    left: SourceFactConflict[?, ?, ?],
    right: SourceFactConflict[?, ?, ?]
  ): Int =
    compareSourceFact(left.original, right.original)
      .orElseCompare(compareSourceFact(left.conflicting, right.conflicting))

  private def compareFillIdentityConflict(
    left: FillIdentityConflict[?, ?, ?],
    right: FillIdentityConflict[?, ?, ?]
  ): Int =
    compareSourceFact(left.original, right.original)
      .orElseCompare(compareSourceFact(left.conflicting, right.conflicting))

  private def compareDiagnostic(left: LifecycleDiagnostic, right: LifecycleDiagnostic): Int =
    compareInt(diagnosticGroupRank(left), diagnosticGroupRank(right))
      .orElseCompare(compareDiagnosticBody(left, right))

  private def diagnosticGroupRank(value: LifecycleDiagnostic): Int = value match
    case _: CommandConflictObserved        => 0
    case _: MissingSourceRange             => 1
    case _: StreamPositionConflictObserved => 1
    case _: SourceRewindObserved           => 1
    case _: SourceEventConflictObserved    => 2
    case _: FillIdentityConflictObserved   => 3
    case _: UnresolvedFillObserved         => 3
    case _: CompletenessNotEstablished     => 4

  private def compareDiagnosticBody(left: LifecycleDiagnostic, right: LifecycleDiagnostic): Int =
    (left, right) match
      case (leftValue: CommandConflictObserved, rightValue: CommandConflictObserved) =>
        compareText(leftValue.commandId.value, rightValue.commandId.value)
      case (
          leftValue: (MissingSourceRange | StreamPositionConflictObserved | SourceRewindObserved),
          rightValue: (MissingSourceRange | StreamPositionConflictObserved | SourceRewindObserved)
        ) => compareStreamDiagnostic(leftValue, rightValue)
      case (leftValue: SourceEventConflictObserved, rightValue: SourceEventConflictObserved) =>
        compareEventId(leftValue.eventId, rightValue.eventId)
      case (
          leftValue: (FillIdentityConflictObserved | UnresolvedFillObserved),
          rightValue: (FillIdentityConflictObserved | UnresolvedFillObserved)
        ) => compareFillDiagnostic(leftValue, rightValue)
      case (leftValue: CompletenessNotEstablished, rightValue: CompletenessNotEstablished) =>
        compareStream(leftValue.stream, rightValue.stream)
      case _ => 0

  private def compareStreamDiagnostic(
    left: MissingSourceRange | StreamPositionConflictObserved | SourceRewindObserved,
    right: MissingSourceRange | StreamPositionConflictObserved | SourceRewindObserved
  ): Int =
    compareStream(streamOf(left), streamOf(right))
      .orElseCompare(compareInt(streamDiagnosticRank(left), streamDiagnosticRank(right)))
      .orElseCompare:
        (left, right) match
          case (leftValue: MissingSourceRange, rightValue: MissingSourceRange) =>
            compareBigInt(leftValue.first.value, rightValue.first.value)
              .orElseCompare(compareBigInt(leftValue.last.value, rightValue.last.value))
          case (leftValue: StreamPositionConflictObserved, rightValue: StreamPositionConflictObserved) =>
            compareBigInt(leftValue.position.sequence.value, rightValue.position.sequence.value)
              .orElseCompare(compareInt(leftValue.claimantCount, rightValue.claimantCount))
          case (leftValue: SourceRewindObserved, rightValue: SourceRewindObserved) =>
            compareBigInt(leftValue.position.value, rightValue.position.value)
              .orElseCompare(compareBigInt(leftValue.previous.value, rightValue.previous.value))
          case _ => 0

  private def streamOf(
    value: MissingSourceRange | StreamPositionConflictObserved | SourceRewindObserved
  ): QualifiedSourceStreamId = value match
    case diagnostic: MissingSourceRange             => diagnostic.stream
    case diagnostic: StreamPositionConflictObserved => diagnostic.position.stream
    case diagnostic: SourceRewindObserved           => diagnostic.stream

  private def streamDiagnosticRank(
    value: MissingSourceRange | StreamPositionConflictObserved | SourceRewindObserved
  ): Int = value match
    case _: MissingSourceRange             => 0
    case _: StreamPositionConflictObserved => 1
    case _: SourceRewindObserved           => 2

  private def compareFillDiagnostic(
    left: FillIdentityConflictObserved | UnresolvedFillObserved,
    right: FillIdentityConflictObserved | UnresolvedFillObserved
  ): Int =
    compareFillId(fillIdOf(left), fillIdOf(right))
      .orElseCompare(compareInt(fillDiagnosticRank(left), fillDiagnosticRank(right)))
      .orElseCompare:
        (left, right) match
          case (leftValue: UnresolvedFillObserved, rightValue: UnresolvedFillObserved) =>
            compareEventId(leftValue.modifierEventId, rightValue.modifierEventId)
          case _ => 0

  private def fillIdOf(value: FillIdentityConflictObserved | UnresolvedFillObserved): QualifiedFillId = value match
    case diagnostic: FillIdentityConflictObserved => diagnostic.fillId
    case diagnostic: UnresolvedFillObserved       => diagnostic.fillId

  private def fillDiagnosticRank(value: FillIdentityConflictObserved | UnresolvedFillObserved): Int = value match
    case _: FillIdentityConflictObserved => 0
    case _: UnresolvedFillObserved       => 1

  private def comparePostCancellationFill(
    left: PostCancellationFillAnomaly[?, ?, ?],
    right: PostCancellationFillAnomaly[?, ?, ?]
  ): Int =
    compareOption(left.effectiveFill.original.authoritativePosition,
      right.effectiveFill.original.authoritativePosition)(
      comparePosition
    )
      .orElseCompare(compareFillId(left.fillId, right.fillId))
      .orElseCompare(compareActiveEffectiveFill(left.effectiveFill, right.effectiveFill))
      .orElseCompare(compareVector(left.priorCancellations, right.priorCancellations)(compareCancellation))
      .orElseCompare(comparePositionLots(left.exactExposure, right.exactExposure))

  private def compareActiveEffectiveFill(
    left: ActiveEffectiveFill[?, ?, ?],
    right: ActiveEffectiveFill[?, ?, ?]
  ): Int =
    compareSourceFact(left.original, right.original)
      .orElseCompare(compareLots(left.effectiveLots, right.effectiveLots))
      .orElseCompare(comparePrice(left.effectivePrice, right.effectivePrice))
      .orElseCompare(compareVector(left.modifiers, right.modifiers)(compareFillModifier))

  private def compareLifecycle(left: ExecutionLifecycle[?, ?, ?], right: ExecutionLifecycle[?, ?, ?]): Int =
    compareText(left.executionOrderId.value, right.executionOrderId.value)
      .orElseCompare(compareText(left.lineageId.value, right.lineageId.value))
      .orElseCompare(compareTarget(left.target, right.target))
      .orElseCompare(compareInt(left.order.intent.side.ordinal, right.order.intent.side.ordinal))
      .orElseCompare(compareBigInt(left.orderedLots.count.unrefined, right.orderedLots.count.unrefined))
      .orElseCompare(compareExecution(left.order.execution, right.order.execution))
      .orElseCompare(compareInstrument(left.instrument, right.instrument))
      .orElseCompare(compareOrder(left.order, right.order))
      .orElseCompare(compareGrid(left.positionGrid, right.positionGrid))

  private def compareInstrument(left: Instrument, right: Instrument): Int =
    compareText(left.identity.id.value, right.identity.id.value)
      .orElseCompare(compareText(left.identity.underlying.value, right.identity.underlying.value))
      .orElseCompare(compareText(left.roles.base.id.value, right.roles.base.id.value))
      .orElseCompare(compareText(left.roles.quote.id.value, right.roles.quote.id.value))
      .orElseCompare(compareText(left.roles.position.id.value, right.roles.position.id.value))
      .orElseCompare(compareText(left.roles.settle.id.value, right.roles.settle.id.value))
      .orElseCompare(compareGrid(left.positionLotGrid, right.positionLotGrid))
      .orElseCompare(compareGrid(left.priceGrid, right.priceGrid))
      .orElseCompare(compareRational(left.basePerPosition.coefficient, right.basePerPosition.coefficient))
      .orElseCompare(compareRational(left.quotePerPosition.coefficient, right.quotePerPosition.coefficient))

  private def compareOrder(left: Order[?, ?, ?], right: Order[?, ?, ?]): Int =
    compareText(left.instrumentId.value, right.instrumentId.value)
      .orElseCompare(compareIntent(left.intent, right.intent))
      .orElseCompare(compareActivation(left.activation, right.activation))
      .orElseCompare(compareExecution(left.execution, right.execution))

  private def compareIntent(left: OrderIntent[?], right: OrderIntent[?]): Int =
    compareText(left.instrumentId.value, right.instrumentId.value)
      .orElseCompare(compareInt(left.side.ordinal, right.side.ordinal))
      .orElseCompare(compareLots(left.lots, right.lots))
      .orElseCompare(compareInt(left.positionEffect.ordinal, right.positionEffect.ordinal))
      .orElseCompare(comparePositionLots(left.positionChange, right.positionChange))

  private def compareActivation(left: OrderActivation[?, ?], right: OrderActivation[?, ?]): Int =
    compareInt(activationRank(left), activationRank(right))
      .orElseCompare:
        (left, right) match
          case (_: ImmediateActivation[?, ?], _: ImmediateActivation[?, ?])          => 0
          case (leftFixed: FixedActivation[?, ?], rightFixed: FixedActivation[?, ?]) =>
            compareInt(leftFixed.reference.ordinal, rightFixed.reference.ordinal)
              .orElseCompare(compareInt(leftFixed.comparison.ordinal, rightFixed.comparison.ordinal))
              .orElseCompare(comparePrice(leftFixed.triggerPrice, rightFixed.triggerPrice))
          case (leftTrailing: TrailingActivation[?, ?], rightTrailing: TrailingActivation[?, ?]) =>
            compareInt(leftTrailing.reference.ordinal, rightTrailing.reference.ordinal)
              .orElseCompare(compareInt(leftTrailing.comparison.ordinal, rightTrailing.comparison.ordinal))
              .orElseCompare(compareBigInt(leftTrailing.offsetTicks.unrefined, rightTrailing.offsetTicks.unrefined))
          case _ => 0

  private def activationRank(value: OrderActivation[?, ?]): Int = value match
    case _: ImmediateActivation[?, ?] => 0
    case _: FixedActivation[?, ?]     => 1
    case _: TrailingActivation[?, ?]  => 2

  private def compareExecution(left: OrderExecution[?, ?, ?], right: OrderExecution[?, ?, ?]): Int =
    compareInt(executionRank(left), executionRank(right))
      .orElseCompare:
        (left, right) match
          case (leftMarket: MarketExecution[?, ?, ?], rightMarket: MarketExecution[?, ?, ?]) =>
            compareInt(leftMarket.timeInForce.ordinal, rightMarket.timeInForce.ordinal)
          case (leftPriced: PricedExecution[?, ?, ?, ?], rightPriced: PricedExecution[?, ?, ?, ?]) =>
            comparePricing(leftPriced.pricing, rightPriced.pricing)
              .orElseCompare(compareInt(leftPriced.timeInForce.ordinal, rightPriced.timeInForce.ordinal))
              .orElseCompare(
                compareInt(leftPriced.liquidityConstraint.ordinal, rightPriced.liquidityConstraint.ordinal)
              )
              .orElseCompare(compareVisibility(leftPriced.visibility, rightPriced.visibility))
          case _ => 0

  private def executionRank(value: OrderExecution[?, ?, ?]): Int = value match
    case _: MarketExecution[?, ?, ?]    => 0
    case _: PricedExecution[?, ?, ?, ?] => 1

  private def comparePricing(left: OrderPricing[?, ?], right: OrderPricing[?, ?]): Int =
    compareInt(pricingRank(left), pricingRank(right))
      .orElseCompare:
        (left, right) match
          case (leftLimit: LimitPricing[?, ?], rightLimit: LimitPricing[?, ?]) =>
            comparePrice(leftLimit.limit, rightLimit.limit)
          case (leftPegged: PeggedPricing[?, ?], rightPegged: PeggedPricing[?, ?]) =>
            compareInt(leftPegged.reference.ordinal, rightPegged.reference.ordinal)
              .orElseCompare(compareBigInt(leftPegged.offsetTicks, rightPegged.offsetTicks))
          case _ => 0

  private def pricingRank(value: OrderPricing[?, ?]): Int = value match
    case _: LimitPricing[?, ?]  => 0
    case _: PeggedPricing[?, ?] => 1

  private def compareVisibility(left: PricedVisibility[?], right: PricedVisibility[?]): Int =
    compareInt(visibilityRank(left), visibilityRank(right))
      .orElseCompare:
        (left, right) match
          case (leftIceberg: IcebergVisibility[?], rightIceberg: IcebergVisibility[?]) =>
            compareLots(leftIceberg.displayedLots, rightIceberg.displayedLots)
          case _ => 0

  private def visibilityRank(value: PricedVisibility[?]): Int = value match
    case DisplayedVisibility     => 0
    case HiddenVisibility        => 1
    case _: IcebergVisibility[?] => 2

  private def compareLots(left: Lots[?], right: Lots[?]): Int =
    compareText(left.instrumentId.value, right.instrumentId.value)
      .orElseCompare(compareBigInt(left.count.unrefined, right.count.unrefined))
      .orElseCompare(compareGrid(left.grid, right.grid))
      .orElseCompare(compareRational(left.quantity.coefficient, right.quantity.coefficient))

  private def comparePositionLots(left: PositionLots[?], right: PositionLots[?]): Int =
    compareText(left.instrumentId.value, right.instrumentId.value)
      .orElseCompare(compareBigInt(left.coordinate, right.coordinate))
      .orElseCompare(compareGrid(left.grid, right.grid))
      .orElseCompare(compareRational(left.quantity.coefficient, right.quantity.coefficient))

  private def comparePrice(left: Price[?, ?], right: Price[?, ?]): Int =
    compareText(left.instrumentId.value, right.instrumentId.value)
      .orElseCompare(compareBigInt(left.ticks.unrefined, right.ticks.unrefined))
      .orElseCompare(compareGrid(left.grid, right.grid))
      .orElseCompare(compareRational(left.coefficient, right.coefficient))

  private def compareGrid(left: GridHandle[?], right: GridHandle[?]): Int =
    compareGridIdentity(left.identity, right.identity)
      .orElseCompare(compareRational(left.quantum.unrefined, right.quantum.unrefined))

  private def compareGridIdentity(left: GridIdentity, right: GridIdentity): Int =
    compareDimKey(left.dimension, right.dimension)
      .orElseCompare(compareText(left.key.id.value, right.key.id.value))
      .orElseCompare(compareLong(left.key.version.value, right.key.version.value))

  private def compareDimKey(left: DimKey, right: DimKey): Int =
    compareVector(left.powers, right.powers): (leftPower, rightPower) =>
      compareText(leftPower._1.value, rightPower._1.value)
        .orElseCompare(compareBigInt(leftPower._2, rightPower._2))

  private def compareSourceOrdering(left: SourceOrdering, right: SourceOrdering): Int =
    (left, right) match
      case (ExplicitlyUnsequenced, ExplicitlyUnsequenced)                                      => 0
      case (ExplicitlyUnsequenced, _)                                                          => -1
      case (_, ExplicitlyUnsequenced)                                                          => 1
      case (leftSequenced: AuthoritativelySequenced, rightSequenced: AuthoritativelySequenced) =>
        comparePosition(leftSequenced.position, rightSequenced.position)
          .orElseCompare(compareContinuation(leftSequenced.continuation, rightSequenced.continuation))

  private def compareCheckpoint(left: SourceCheckpoint, right: SourceCheckpoint): Int =
    comparePosition(left.position, right.position)
      .orElseCompare(compareContinuation(left.continuation, right.continuation))

  private def compareContinuation(left: SourceContinuation, right: SourceContinuation): Int =
    compareStream(left.stream, right.stream)
      .orElseCompare(compareOption(left.previous, right.previous)(comparePosition))

  private def compareEventId(left: QualifiedSourceEventId, right: QualifiedSourceEventId): Int =
    compareTarget(left.target, right.target)
      .orElseCompare(compareText(left.native.value, right.native.value))

  private def compareSourceOrderId(left: QualifiedSourceOrderId, right: QualifiedSourceOrderId): Int =
    compareTarget(left.target, right.target)
      .orElseCompare(compareText(left.native.value, right.native.value))

  private def compareFillId(left: QualifiedFillId, right: QualifiedFillId): Int =
    compareTarget(left.target, right.target)
      .orElseCompare(compareText(left.native.value, right.native.value))

  private def compareStream(left: QualifiedSourceStreamId, right: QualifiedSourceStreamId): Int =
    compareTarget(left.target, right.target)
      .orElseCompare(compareText(left.native.value, right.native.value))

  private def compareTarget(left: ExecutionTarget, right: ExecutionTarget): Int =
    compareText(left.source.value, right.source.value)
      .orElseCompare(compareText(left.account.value, right.account.value))

  private def compareRational(left: Rational, right: Rational): Int =
    compareBigInt(left.numerator, right.numerator)
      .orElseCompare(compareBigInt(left.denominator, right.denominator))

  private def compareNullable[A <: AnyRef](left: A, right: A)(compare: (A, A) => Int): Int =
    if left == null then if right == null then 0 else -1
    else if right == null then 1
    else compare(left, right)

  private def compareOption[A](left: Option[A], right: Option[A])(compare: (A, A) => Int): Int =
    (left, right) match
      case (None, None)                        => 0
      case (None, Some(_))                     => -1
      case (Some(_), None)                     => 1
      case (Some(leftValue), Some(rightValue)) => compare(leftValue, rightValue)

  private def compareVector[A](left: Vector[A], right: Vector[A])(compare: (A, A) => Int): Int =
    val shared = left.size.min(right.size)
    var index  = 0
    var result = 0
    while index < shared && result == 0 do
      result = compare(left(index), right(index))
      index += 1
    if result != 0 then result else compareInt(left.size, right.size)

  private def compareText(left: String, right: String): Int   = left.compareTo(right)
  private def compareBigInt(left: BigInt, right: BigInt): Int = left.compare(right)
  private def compareInt(left: Int, right: Int): Int          = java.lang.Integer.compare(left, right)
  private def compareLong(left: Long, right: Long): Int       = java.lang.Long.compare(left, right)
end ExecutionOrderings
