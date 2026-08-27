package trading.economics

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.runtime.*

/** Stable external identity of a validated listing or contract. */
final case class InstrumentId(value: String):
  require(value.trim.nonEmpty, "instrument ID cannot be empty")

/** Stable identity of an instrument's possibly non-currency underlying. */
final case class UnderlyingId(value: String):
  require(value.trim.nonEmpty, "underlying ID cannot be empty")

/** Semantic identity of a trading-fee component. */
final case class FeeKind(value: String):
  require(value.trim.nonEmpty, "fee kind cannot be empty")

/** Cohesive semantic identity of one instrument. */
final case class InstrumentIdentity(id: InstrumentId, underlying: UnderlyingId)

/** The registered asset roles shared by listing rules and payoff terms. */
final class InstrumentRoles(
  val base: AssetRef,
  val quote: AssetRef,
  val position: AssetRef,
  val settle: AssetRef)

/** Contextual grids for the exact role value supplied at construction. */
final class ListingRules(
  val roles: InstrumentRoles
)(
  val positionLotGrid: RegisteredGridRef[? <: Dimension],
  val priceGrid: RegisteredGridRef[? <: Dimension])

/** Product-family-neutral two-leg payoff for the exact role value supplied at construction. */
final class ContractPayoff(
  val roles: InstrumentRoles
)(
  val basePerPosition: Rate[roles.position.D, roles.base.D],
  val quotePerPosition: Rate[roles.position.D, roles.quote.D])

/** One cohesive input to the final validated instrument boundary. */
final case class InstrumentDefinition(
  identity: InstrumentIdentity,
  roles: InstrumentRoles,
  listingRules: ListingRules,
  contractPayoff: ContractPayoff)

/** Order direction and the corresponding account-position sign. */
enum Side:
  case Buy, Sell

  def sign: BigInt =
    this match
      case Buy  => BigInt(1)
      case Sell => BigInt(-1)

/** Duration instruction retained by a priced immutable order. */
enum TimeInForce:
  case GoodTillCancelled, ImmediateOrCancel, FillOrKill, Day

/** The only durations structurally accepted by market execution. */
enum NonRestingTimeInForce:
  case ImmediateOrCancel, FillOrKill

object NonRestingTimeInForce:
  def from(value: TimeInForce): Either[EconomicsError, NonRestingTimeInForce] =
    value match
      case TimeInForce.ImmediateOrCancel => Right(NonRestingTimeInForce.ImmediateOrCancel)
      case TimeInForce.FillOrKill        => Right(NonRestingTimeInForce.FillOrKill)
      case supplied                      => Left(InvalidOrder(OrderFailureReason.RestingMarketDuration(supplied)))

/** Whether a priced order may take liquidity or must remain passive. */
enum LiquidityConstraint:
  case Unrestricted, MakerOnly

/** Whether an order may open exposure or may only reduce it. */
enum PositionEffect:
  case Unrestricted, ReduceOnly

/** Price source named by a trigger or peg. */
enum PriceReference:
  case Last, Mark, Index

/** Exact comparison used by fixed and trailing activation. */
enum TriggerComparison:
  case AtOrAbove, AtOrBelow

/** Fee classification of one complete scenario slice. */
enum LiquidityRole:
  case Maker, Taker

/** Entry or exit attribution retained on converted fee contributions. */
enum ScenarioLeg:
  case Entry, Exit

/** Quoted fee-policy sign: positive is a charge and negative is a rebate. */
final case class FeeRate(coefficient: Rational):
  require(coefficient != null, "fee rate coefficient")

enum InstrumentContradiction:
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
final case class InstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId)
  extends EconomicsError

private[economics] object InstrumentIdentityChecks:
  /** Compare named ordinary identities. This helper issues no token and performs no cast. */
  def check(
    context: String,
    expected: InstrumentId,
    supplied: (String, InstrumentId)*
  ): Either[EconomicsError, Unit] =
    supplied.collectFirst:
      case (name, id) if id != expected => InstrumentMismatch(s"$context.$name", expected, id)
    match
      case Some(error) => Left(error)
      case None        => Right(())

final case class ForeignRegistry(role: String, expected: DimensionKey, supplied: DimensionKey) extends EconomicsError

final case class RuntimeEvidenceFailure(role: String, cause: RuntimeEvidenceError) extends EconomicsError

final case class GridDimensionFailure(role: String, grid: GridKey, expected: DimensionKey, supplied: DimensionKey)
  extends EconomicsError

final case class EmptyContractPayoff(instrumentId: InstrumentId) extends EconomicsError

final case class ContradictoryInstrument(instrumentId: InstrumentId, reason: InstrumentContradiction)
  extends EconomicsError

final case class InvalidLots(count: BigInt) extends EconomicsError

final case class InvalidPriceCoordinate(coordinate: BigInt) extends EconomicsError

final case class PriceNotOnGrid(cause: NotOnGrid[? <: Dimension]) extends EconomicsError

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

final case class InvalidFeeGrid(asset: AssetId, grid: GridKey, expected: DimensionKey, supplied: DimensionKey)
  extends EconomicsError

final case class InvalidFeeBasis(asset: AssetId, coefficient: Rational) extends EconomicsError

final case class InvalidFeeAttribution(sliceIndex: Int, sliceCount: Int) extends EconomicsError

final case class FeeScheduleFailure(reason: FeeScheduleFailureReason) extends EconomicsError

final case class InvalidRiskBudget(coefficient: Rational) extends EconomicsError

final case class SizingScenarioMismatch(candidateLots: BigInt, heldPositionLots: BigInt) extends EconomicsError
