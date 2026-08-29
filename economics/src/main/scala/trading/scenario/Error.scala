package trading.scenario

import trading.economics.instrument.*
import trading.order.*

enum ScenarioFailureReason:
  case NoSlices
  case AssumptionOrderMismatch
  case SliceLotsMismatch(expected: BigInt, supplied: BigInt)
  case FixedTriggerUnsatisfied
  case FixedEvidenceMismatch
  case TrailingThresholdNonPositive
  case TrailingTriggerUnsatisfied
  case TrailingEvidenceMismatch
  case PegOffsetMismatch
  case PegResolutionMismatch
  case MarketSliceNotTaker
  case MakerOnlySliceNotMaker
  case SliceWorseThanLimit

enum ScenarioViolation:
  case EmptySlices
  case OrderTargetMismatch
  case Identity(context: String, expected: InstrumentId, supplied: InstrumentId)
  case LotTotal(expected: BigInt, supplied: BigInt)
  case Activation(cause: ActivationViolation)
  case Pricing(cause: PricingViolation)
  case Slice(index: Int, reason: ScenarioFailureReason)

final case class InvalidScenarioDiagnostics(head: ScenarioViolation, tail: Vector[ScenarioViolation]):
  def violations: Vector[ScenarioViolation] = head +: tail

sealed abstract class ScenarioError extends Product with Serializable
final case class ScenarioInstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId)
  extends ScenarioError
final case class InvalidScenario(reason: ScenarioFailureReason, sliceIndex: Option[Int] = None) extends ScenarioError
final case class InvalidRoundTrip(entryChange: BigInt, exitChange: BigInt)                      extends ScenarioError

private[scenario] object ScenarioIdentityChecks:
  def check(
    context: String,
    expected: InstrumentId,
    supplied: (String, InstrumentId)*
  ): Either[ScenarioError, Unit] =
    supplied.collectFirst:
      case (name, id) if id != expected => ScenarioInstrumentMismatch(s"$context.$name", expected, id)
    match
      case Some(error) => Left(error)
      case None        => Right(())

private[scenario] object ScenarioViolationMapping:
  def scenario(violation: ScenarioViolation): ScenarioError =
    violation match
      case ScenarioViolation.EmptySlices         => InvalidScenario(ScenarioFailureReason.NoSlices)
      case ScenarioViolation.OrderTargetMismatch => InvalidScenario(ScenarioFailureReason.AssumptionOrderMismatch)
      case ScenarioViolation.Identity(context, expected, supplied) =>
        ScenarioInstrumentMismatch(context, expected, supplied)
      case ScenarioViolation.LotTotal(expected, supplied) =>
        InvalidScenario(ScenarioFailureReason.SliceLotsMismatch(expected, supplied))
      case ScenarioViolation.Activation(cause) =>
        cause match
          case ActivationViolation.FixedTriggerUnsatisfied =>
            InvalidScenario(ScenarioFailureReason.FixedTriggerUnsatisfied)
          case ActivationViolation.FixedEvidenceMismatch =>
            InvalidScenario(ScenarioFailureReason.FixedEvidenceMismatch)
          case ActivationViolation.TrailingThresholdNonPositive =>
            InvalidScenario(ScenarioFailureReason.TrailingThresholdNonPositive)
          case ActivationViolation.TrailingTriggerUnsatisfied =>
            InvalidScenario(ScenarioFailureReason.TrailingTriggerUnsatisfied)
          case ActivationViolation.TrailingEvidenceMismatch =>
            InvalidScenario(ScenarioFailureReason.TrailingEvidenceMismatch)
      case ScenarioViolation.Pricing(cause) =>
        cause match
          case PricingViolation.PegOffsetMismatch(_, _) =>
            InvalidScenario(ScenarioFailureReason.PegOffsetMismatch)
          case PricingViolation.PegResolutionMismatch =>
            InvalidScenario(ScenarioFailureReason.PegResolutionMismatch)
      case ScenarioViolation.Slice(index, reason) => InvalidScenario(reason, Some(index))
end ScenarioViolationMapping
