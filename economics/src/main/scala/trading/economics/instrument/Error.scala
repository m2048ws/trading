package trading.economics.instrument

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.runtime.*

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
  case StopRequiresTrigger

enum ScenarioFailureReason:
  case NoSlices
  case SliceLotsMismatch(expected: BigInt, supplied: BigInt)
  case UnexpectedTriggerEvidence
  case MissingFixedTriggerEvidence
  case FixedEvidenceExpected
  case MissingTrailingTriggerEvidence
  case TrailingEvidenceExpected
  case TriggerReferenceMismatch
  case FixedTriggerUnsatisfied
  case TrailingThresholdNonPositive
  case TrailingTriggerUnsatisfied
  case UnexpectedPegResolution
  case MissingPegResolution
  case PegReferenceMismatch
  case PegOffsetMismatch
  case MarketSliceNotTaker
  case MakerOnlySliceNotMaker
  case SliceWorseThanLimit

enum FeeScheduleFailureReason:
  case ForeignScenarioLine

/** Closed hierarchy of expected economics failures. */
sealed abstract class EconomicsError extends Product with Serializable

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

final case class ForeignRegistry(role: String, expected: DimKey, supplied: DimKey) extends EconomicsError

final case class RuntimeEvidenceFailure(role: String, cause: RuntimeEvidenceError) extends EconomicsError

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
