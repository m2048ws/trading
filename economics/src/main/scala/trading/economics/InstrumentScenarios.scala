package trading.economics

private[economics] object InstrumentScenarios:
  final case class ActivationEvidencePlan[P](
    reference: PriceReference,
    observedPrice: P,
    favorableExtreme: Option[P])
  final case class PegResolutionPlan[P](reference: PriceReference, referencePrice: P, resolvedLimit: P)
  final case class SlicePlan[L, M](lots: L, market: M, role: LiquidityRole)

  final case class ActivationView(
    kind: ActivationKind,
    reference: Option[PriceReference],
    comparison: Option[TriggerComparison],
    triggerCoordinate: Option[BigInt],
    trailingOffsetTicks: Option[BigInt])
  final case class EvidenceView(
    reference: PriceReference,
    observedCoordinate: BigInt,
    favorableExtremeCoordinate: Option[BigInt])
  final case class InstructionView(
    kind: PriceInstructionKind,
    limitCoordinate: Option[BigInt],
    reference: Option[PriceReference],
    offsetTicks: Option[BigInt])
  final case class PegView(reference: PriceReference, referenceCoordinate: BigInt, resolvedCoordinate: BigInt)
  final case class SliceView(lots: BigInt, price: BigInt, role: LiquidityRole)
  final case class OrderView(
    side: Side,
    lots: BigInt,
    activation: ActivationView,
    instruction: InstructionView,
    liquidityConstraint: LiquidityConstraint)

  def fixedEvidence[P](reference: PriceReference, observedPrice: P): ActivationEvidencePlan[P] =
    ActivationEvidencePlan(reference, observedPrice, None)

  def trailingEvidence[P](
    reference: PriceReference,
    favorableExtreme: P,
    activatingObservation: P
  ): ActivationEvidencePlan[P] =
    ActivationEvidencePlan(reference, activatingObservation, Some(favorableExtreme))

  def pegResolution[P](reference: PriceReference, referencePrice: P, resolvedLimit: P): PegResolutionPlan[P] =
    PegResolutionPlan(reference, referencePrice, resolvedLimit)

  def slice[L, M](lots: L, market: M, role: LiquidityRole): SlicePlan[L, M] = SlicePlan(lots, market, role)

  def order(
    order: OrderView,
    slices: Vector[SliceView],
    activationEvidence: Option[EvidenceView],
    pegResolution: Option[PegView]
  ): Either[EconomicsError, BigInt] =
    validateSliceTotals(order.lots, slices.map(_.lots))
      .flatMap(_ => validateActivation(order.activation, activationEvidence))
      .flatMap(_ => validatePeg(order.instruction, pegResolution))
      .flatMap(effectiveLimit => validateSlices(order, slices, effectiveLimit))
      .map(_ => order.side.sign * order.lots)

  def roundTrip(entryChange: BigInt, exitChange: BigInt): Either[EconomicsError, BigInt] =
    if entryChange + exitChange != 0 then Left(InvalidRoundTrip(entryChange, exitChange)) else Right(entryChange)

  private def validateSliceTotals(orderLots: BigInt, sliceLots: Vector[BigInt]): Either[EconomicsError, Unit] =
    if sliceLots.isEmpty then Left(InvalidScenario("complete scenario requires at least one slice"))
    else if sliceLots.sum != orderLots then Left(InvalidScenario("slice lots must sum exactly to order lots"))
    else Right(())

  private def comparisonSatisfied(comparison: TriggerComparison, observed: BigInt, threshold: BigInt): Boolean =
    comparison match
      case TriggerComparison.AtOrAbove => observed >= threshold
      case TriggerComparison.AtOrBelow => observed <= threshold

  private def validateActivation(
    activation: ActivationView,
    evidence: Option[EvidenceView]
  ): Either[EconomicsError, Unit] =
    activation.kind match
      case ActivationKind.Immediate =>
        if evidence.isEmpty then Right(())
        else Left(InvalidScenario("immediate activation must not carry trigger evidence"))
      case ActivationKind.FixedTrigger =>
        evidence match
          case None => Left(InvalidScenario("fixed trigger requires activation evidence"))
          case Some(value) if value.favorableExtremeCoordinate.nonEmpty =>
            Left(InvalidScenario("fixed trigger evidence cannot contain a favorable extremum"))
          case Some(value) =>
            if value.reference != activation.reference.get then
              Left(InvalidScenario("trigger reference does not match"))
            else if comparisonSatisfied(
                activation.comparison.get,
                value.observedCoordinate,
                activation.triggerCoordinate.get
              )
            then Right(())
            else Left(InvalidScenario("fixed trigger observation does not satisfy comparison"))
      case ActivationKind.TrailingTrigger =>
        evidence match
          case None        => Left(InvalidScenario("trailing trigger requires activation evidence"))
          case Some(value) =>
            value.favorableExtremeCoordinate match
              case None          => Left(InvalidScenario("trailing trigger requires a favorable extremum"))
              case Some(extreme) =>
                val threshold = activation.comparison.get match
                  case TriggerComparison.AtOrAbove => extreme + activation.trailingOffsetTicks.get
                  case TriggerComparison.AtOrBelow => extreme - activation.trailingOffsetTicks.get
                if value.reference != activation.reference.get then
                  Left(InvalidScenario("trigger reference does not match"))
                else if threshold.signum <= 0 then Left(InvalidScenario("trailing threshold is not a positive price"))
                else if comparisonSatisfied(activation.comparison.get, value.observedCoordinate, threshold) then
                  Right(())
                else Left(InvalidScenario("trailing observation does not satisfy derived threshold"))

  private def validatePeg(
    instruction: InstructionView,
    evidence: Option[PegView]
  ): Either[EconomicsError, Option[BigInt]] =
    instruction.kind match
      case PriceInstructionKind.Market =>
        if evidence.isEmpty then Right(None)
        else Left(InvalidScenario("market instruction must not carry peg evidence"))
      case PriceInstructionKind.Limit =>
        if evidence.isEmpty then Right(instruction.limitCoordinate)
        else Left(InvalidScenario("fixed limit must not carry peg evidence"))
      case PriceInstructionKind.Pegged =>
        evidence match
          case None        => Left(InvalidScenario("pegged instruction requires resolution evidence"))
          case Some(value) =>
            val difference = value.resolvedCoordinate - value.referenceCoordinate
            if value.reference != instruction.reference.get then Left(InvalidScenario("peg reference does not match"))
            else if difference != instruction.offsetTicks.get then
              Left(InvalidScenario("resolved peg tick offset disagrees"))
            else Right(Some(value.resolvedCoordinate))

  private def validateSlices(
    order: OrderView,
    slices: Vector[SliceView],
    effectiveLimit: Option[BigInt]
  ): Either[EconomicsError, Unit] =
    slices.zipWithIndex.collectFirst:
      case (slice, index)
        if order.instruction.kind == PriceInstructionKind.Market && slice.role != LiquidityRole.Taker =>
        InvalidScenario("market slices must be taker", Some(index))
      case (slice, index)
        if order.liquidityConstraint == LiquidityConstraint.MakerOnly && slice.role != LiquidityRole.Maker =>
        InvalidScenario("maker-only slices must be maker", Some(index))
      case (slice, index)
        if effectiveLimit.exists: limit =>
          order.side match
            case Side.Buy  => slice.price > limit
            case Side.Sell => slice.price < limit
        =>
        InvalidScenario("slice price is worse than the effective limit", Some(index))
    match
      case Some(error) => Left(error)
      case None        => Right(())

end InstrumentScenarios
