package trading.economics.instrument

import trading.quantity.*
import trading.quantity.grid.*
import trading.reference.*

enum Contradiction:
  case BaseEqualsQuote
  case ListingRolesDiffer
  case PayoffRolesDiffer

enum ConversionFailureReason:
  case NonPositive
  case IdentityNotOne
  case SettleIsNotQuote
  case SettleIsNotBase
  case TargetIsNotSettle

enum OrderFailureReason:
  case RestingMarketDuration(value: TimeInForce)
  case NonRestingIceberg
  case IcebergExceedsOrder(displayed: BigInt, ordered: BigInt)

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

enum FeeScheduleFailureReason:
  case ForeignScenarioLine

/** Closed hierarchy of expected economics failures. */
sealed abstract class EconomicsError extends Product with Serializable

/** Closed, domain-owned diagnostics emitted by raw instrument-definition validation. */
enum DefinitionViolation:
  case Lineage(role: String, expected: DimKey, supplied: DimKey)
  case ComponentRoles(instrumentId: InstrumentId, reason: Contradiction)
  case GridDimension(role: String, grid: GridKey, expected: DimKey, supplied: DimKey)
  case EmptyPayoff(instrumentId: InstrumentId)

/** A non-empty, deterministically ordered set of definition violations. */
final case class InvalidDefinition(head: DefinitionViolation, tail: Vector[DefinitionViolation]):
  def violations: Vector[DefinitionViolation] = head +: tail

/** Semantic failures from correctly shaped activation evidence. */
enum ActivationViolation:
  case FixedTriggerUnsatisfied
  case FixedEvidenceMismatch
  case TrailingThresholdNonPositive
  case TrailingTriggerUnsatisfied
  case TrailingEvidenceMismatch

/** Semantic failures from correctly shaped pricing resolution. */
enum PricingViolation:
  case PegOffsetMismatch(expectedOffset: BigInt, suppliedOffset: BigInt)
  case PegResolutionMismatch

/** Closed, domain-owned diagnostics emitted by complete-scenario validation. */
enum ScenarioViolation:
  case EmptySlices
  case OrderTargetMismatch
  case Identity(context: String, expected: InstrumentId, supplied: InstrumentId)
  case LotTotal(expected: BigInt, supplied: BigInt)
  case Activation(cause: ActivationViolation)
  case Pricing(cause: PricingViolation)
  case Slice(index: Int, reason: ScenarioFailureReason)

/** A non-empty, deterministically ordered set of scenario violations. */
final case class InvalidScenarioDiagnostics(head: ScenarioViolation, tail: Vector[ScenarioViolation]):
  def violations: Vector[ScenarioViolation] = head +: tail

private[instrument] object ViolationMapping:
  def definition(violation: DefinitionViolation): EconomicsError =
    violation match
      case DefinitionViolation.Lineage(role, expected, supplied) =>
        ForeignReferenceDataLineage(role, expected, supplied)
      case DefinitionViolation.ComponentRoles(instrumentId, reason) => ContradictoryInstrument(instrumentId, reason)
      case DefinitionViolation.GridDimension(role, grid, expected, supplied) =>
        GridDimensionFailure(role, grid, expected, supplied)
      case DefinitionViolation.EmptyPayoff(instrumentId) => EmptyContractPayoff(instrumentId)

  def activation(violation: ActivationViolation): EconomicsError =
    violation match
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

  def pricing(violation: PricingViolation): EconomicsError =
    violation match
      case PricingViolation.PegOffsetMismatch(_, _) => InvalidScenario(ScenarioFailureReason.PegOffsetMismatch)
      case PricingViolation.PegResolutionMismatch   => InvalidScenario(ScenarioFailureReason.PegResolutionMismatch)

  def scenario(violation: ScenarioViolation): EconomicsError =
    violation match
      case ScenarioViolation.EmptySlices         => InvalidScenario(ScenarioFailureReason.NoSlices)
      case ScenarioViolation.OrderTargetMismatch => InvalidScenario(ScenarioFailureReason.AssumptionOrderMismatch)
      case ScenarioViolation.Identity(context, expected, supplied) => Mismatch(context, expected, supplied)
      case ScenarioViolation.LotTotal(expected, supplied)          =>
        InvalidScenario(ScenarioFailureReason.SliceLotsMismatch(expected, supplied))
      case ScenarioViolation.Activation(cause)    => activation(cause)
      case ScenarioViolation.Pricing(cause)       => pricing(cause)
      case ScenarioViolation.Slice(index, reason) => InvalidScenario(reason, Some(index))

end ViolationMapping

/** One ordinary runtime-coherence diagnostic shared by all economics aggregate boundaries. */
final case class Mismatch(context: String, expected: InstrumentId, supplied: InstrumentId) extends EconomicsError

private[instrument] object IdentityChecks:
  /** Compare named ordinary identities. This helper issues no token and performs no cast. */
  def check(
    context: String,
    expected: InstrumentId,
    supplied: (String, InstrumentId)*
  ): Either[EconomicsError, Unit] =
    supplied.collectFirst:
      case (name, id) if id != expected => Mismatch(s"$context.$name", expected, id)
    match
      case Some(error) => Left(error)
      case None        => Right(())

final case class ForeignReferenceDataLineage(role: String, expected: DimKey, supplied: DimKey) extends EconomicsError

final case class ReferenceDataFailure(role: String, cause: ReferenceDataError) extends EconomicsError

final case class GridDimensionFailure(role: String, grid: GridKey, expected: DimKey, supplied: DimKey)
  extends EconomicsError

final case class EmptyContractPayoff(instrumentId: InstrumentId) extends EconomicsError

final case class ContradictoryInstrument(instrumentId: InstrumentId, reason: Contradiction) extends EconomicsError

final case class InvalidLots(count: BigInt) extends EconomicsError

final case class InvalidPriceCoordinate(coordinate: BigInt) extends EconomicsError

final case class PriceNotOnGrid(cause: NotOnGrid[? <: Dim]) extends EconomicsError

final case class InvalidConversion(
  source: AssetId,
  target: AssetId,
  coefficient: Rational,
  reason: ConversionFailureReason)
  extends EconomicsError

final case class DuplicateConversion(source: AssetId) extends EconomicsError

final case class MissingConversion(source: AssetId, leg: Option[ScenarioLeg], sliceIndex: Option[Int])
  extends EconomicsError

final case class IncoherentMarketState(price: Rational, baseToSettle: Rational, quoteToSettle: Rational)
  extends EconomicsError

final case class InvalidTrailingOffset(offsetTicks: BigInt) extends EconomicsError

final case class InvalidOrder(reason: OrderFailureReason) extends EconomicsError

final case class InvalidScenario(reason: ScenarioFailureReason, sliceIndex: Option[Int] = None) extends EconomicsError

final case class InvalidRoundTrip(entryChange: BigInt, exitChange: BigInt) extends EconomicsError

final case class InvalidFeeGrid(asset: AssetId, grid: GridKey, expected: DimKey, supplied: DimKey)
  extends EconomicsError

final case class InvalidFeeBasis(asset: AssetId, coefficient: Rational) extends EconomicsError

final case class InvalidFeeAttribution(sliceIndex: Int, sliceCount: Int) extends EconomicsError

final case class FeeScheduleFailure(reason: FeeScheduleFailureReason) extends EconomicsError

final case class InvalidRiskBudget(coefficient: Rational) extends EconomicsError

final case class SizingScenarioMismatch(candidateLots: BigInt, heldPositionLots: BigInt) extends EconomicsError
