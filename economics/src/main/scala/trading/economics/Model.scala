package trading.economics

import trading.quantity.*
import trading.quantity.grid.NotOnGrid
import trading.quantity.runtime.AssetRef
import trading.quantity.runtime.RuntimeEvidenceError

/** Stable external identity of a validated listing or contract. */
final case class InstrumentId(value: String) extends JavaSerializationUnsupported:
  require(value.trim.nonEmpty, "instrument ID cannot be empty")

/** Stable identity of an instrument's possibly non-currency underlying. */
final case class UnderlyingId(value: String) extends JavaSerializationUnsupported:
  require(value.trim.nonEmpty, "underlying ID cannot be empty")

/** Semantic identity of a trading-fee component. */
final case class FeeKind(value: String) extends JavaSerializationUnsupported:
  require(value.trim.nonEmpty, "fee kind cannot be empty")

/** Order direction and the corresponding account-position sign. */
enum Side extends JavaSerializationUnsupported:
  case Buy, Sell

  def sign: BigInt =
    this match
      case Buy  => BigInt(1)
      case Sell => BigInt(-1)

/** Duration instruction retained by an immutable order. */
enum TimeInForce extends JavaSerializationUnsupported:
  case GoodTillCancelled, ImmediateOrCancel, FillOrKill, Day

/** Whether an order may take liquidity or must remain passive. */
enum LiquidityConstraint extends JavaSerializationUnsupported:
  case Unrestricted, MakerOnly

/** Whether an order may open exposure or may only reduce it. */
enum PositionEffect extends JavaSerializationUnsupported:
  case Unrestricted, ReduceOnly

/** Price source named by a trigger or peg. */
enum PriceReference extends JavaSerializationUnsupported:
  case Last, Mark, Index

/** Exact comparison used by fixed and trailing activation. */
enum TriggerComparison extends JavaSerializationUnsupported:
  case AtOrAbove, AtOrBelow

/** Fee classification of one complete scenario slice. */
enum LiquidityRole extends JavaSerializationUnsupported:
  case Maker, Taker

/** Entry or exit attribution retained on converted fee contributions. */
enum ScenarioLeg extends JavaSerializationUnsupported:
  case Entry, Exit

enum ActivationKind extends JavaSerializationUnsupported:
  case Immediate, FixedTrigger, TrailingTrigger

enum PriceInstructionKind extends JavaSerializationUnsupported:
  case Market, Limit, Pegged

enum VisibilityKind extends JavaSerializationUnsupported:
  case NotApplicable, Displayed, Hidden, Iceberg

/** Quoted fee-policy sign: positive is a charge and negative is a rebate. */
final case class FeeRate(coefficient: Rational) extends JavaSerializationUnsupported:
  require(coefficient != null, "fee rate coefficient")

/** Closed hierarchy of expected economics failures. */
sealed abstract class EconomicsError extends JavaSerializationUnsupported with Product with Serializable

final case class ForeignRegistry(role: String, expected: DimensionKey, supplied: DimensionKey) extends EconomicsError

final case class RuntimeEvidenceFailure(role: String, cause: RuntimeEvidenceError) extends EconomicsError

final case class GridDimensionFailure(role: String, grid: GridKey, expected: DimensionKey, supplied: DimensionKey)
  extends EconomicsError

final case class EmptyContractPayoff(instrumentId: InstrumentId) extends EconomicsError

final case class ContradictoryInstrument(instrumentId: InstrumentId, detail: String) extends EconomicsError

final case class InvalidLots(count: BigInt) extends EconomicsError

final case class InvalidPriceCoordinate(coordinate: BigInt) extends EconomicsError

final case class PriceNotOnGrid(cause: NotOnGrid[? <: Dimension]) extends EconomicsError

final case class InvalidConversion(source: AssetId, target: AssetId, coefficient: Rational, detail: String)
  extends EconomicsError

final case class DuplicateConversion(source: AssetId) extends EconomicsError

final case class MissingConversion(source: AssetId, leg: Option[ScenarioLeg], sliceIndex: Option[Int])
  extends EconomicsError

final case class IncoherentMarketState(price: Rational, baseToSettle: Rational, quoteToSettle: Rational)
  extends EconomicsError

final case class InvalidTrailingOffset(offsetTicks: BigInt) extends EconomicsError

final case class InvalidOrder(detail: String) extends EconomicsError

final case class InvalidScenario(detail: String, sliceIndex: Option[Int] = None) extends EconomicsError

final case class InvalidRoundTrip(entryChange: BigInt, exitChange: BigInt) extends EconomicsError

final case class InvalidFeeGrid(asset: AssetId, grid: GridKey, expected: DimensionKey, supplied: DimensionKey)
  extends EconomicsError

final case class InvalidFeeBasis(asset: AssetId, coefficient: Rational) extends EconomicsError

final case class InvalidFeeAttribution(sliceIndex: Int, sliceCount: Int) extends EconomicsError

final case class FeeScheduleFailure(detail: String) extends EconomicsError

final case class InvalidRiskBudget(coefficient: Rational) extends EconomicsError

final case class SizingScenarioMismatch(candidateLots: BigInt, heldPositionLots: BigInt) extends EconomicsError

/** Explicit, checked conversion from one registered asset into another. */
sealed trait SettlementConversion extends JavaSerializationUnsupported:
  val source: AssetRef
  val target: AssetRef
  def coefficient: Rational

object SettlementConversion:

  private final class SettlementConversionImpl(
    val source: AssetRef,
    val target: AssetRef,
    val coefficient: Rational)
    extends SettlementConversion

  def positive(
    source: AssetRef,
    target: AssetRef,
    coefficient: Rational
  ): Either[EconomicsError, SettlementConversion] =
    checked(source, target, coefficient)

  def fromRate(
    source: AssetRef,
    target: AssetRef
  )(
    rate: Rate[source.D, target.D]
  ): Either[EconomicsError, SettlementConversion] =
    checked(source, target, rate.coefficient)

  private def checked(
    source: AssetRef,
    target: AssetRef,
    coefficient: Rational
  ): Either[EconomicsError, SettlementConversion] =
    if !source.dimension.sharesRegistryWith(target.dimension) then
      Left(ForeignRegistry("settlement conversion", target.dimension.key, source.dimension.key))
    else
      PriceMarketRules.validateConversion(source.id, target.id, coefficient).map: _ =>
        new SettlementConversionImpl(source, target, coefficient)

end SettlementConversion
