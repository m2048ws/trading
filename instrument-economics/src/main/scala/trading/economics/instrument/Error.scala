package trading.economics.instrument

import trading.quantity.*
import trading.quantity.grid.NotOnGrid
import trading.reference.*

sealed abstract class LotError                  extends JavaSerializationUnsupported with Product with Serializable
final case class InvalidLotCount(count: BigInt) extends LotError

sealed abstract class PositionError extends JavaSerializationUnsupported with Product with Serializable
final case class PositionInstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId)
  extends PositionError

sealed abstract class PriceError extends JavaSerializationUnsupported with Product with Serializable
final case class InvalidPriceCoordinate(coordinate: BigInt) extends PriceError
final case class PriceNotOnGrid(cause: NotOnGrid[? <: Dim]) extends PriceError

enum ConversionFailureReason extends JavaSerializationUnsupported:
  case NonPositive
  case IdentityNotOne
  case SettleIsNotQuote
  case SettleIsNotBase
  case TargetIsNotSettle

enum MarketStateViolation extends JavaSerializationUnsupported:
  case InstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId)
  case InvalidConversion(
    source: AssetId,
    target: AssetId,
    coefficient: Rational,
    reason: ConversionFailureReason)
  case DuplicateSource(source: AssetId)
  case ReferenceData(context: String, cause: ReferenceDataError)
  case IncoherentAnchors(price: Rational, baseToSettle: Rational, quoteToSettle: Rational)

/** Non-empty, stable-order diagnostics from market-state construction. */
final class MarketStateViolations private (
  val head: MarketStateViolation,
  val tail: Vector[MarketStateViolation])
  extends JavaSerializationUnsupported:

  val violations: Vector[MarketStateViolation] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: MarketStateViolations => violations == that.violations
      case _                           => false

  override def hashCode: Int    = violations.hashCode
  override def toString: String = violations.mkString("MarketStateViolations(", ",", ")")
end MarketStateViolations

object MarketStateViolations:
  def one(head: MarketStateViolation): MarketStateViolations =
    new MarketStateViolations(head, Vector.empty)

  def from(values: Vector[MarketStateViolation]): Option[MarketStateViolations] =
    values match
      case head +: tail => Some(new MarketStateViolations(head, tail))
      case _            => None
end MarketStateViolations

sealed abstract class ConversionError               extends JavaSerializationUnsupported with Product with Serializable
final case class MissingConversion(source: AssetId) extends ConversionError
final case class ConversionSourceMismatch(source: AssetId, cause: ReferenceDataError) extends ConversionError

sealed abstract class FeeValueError extends JavaSerializationUnsupported with Product with Serializable
case object EmptyFeeKind            extends FeeValueError
final case class FeeInstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId)
  extends FeeValueError
final case class FeeReferenceDataMismatch(context: String, cause: ReferenceDataError)              extends FeeValueError
final case class InvalidFeeGrid(asset: AssetId, grid: GridKey, expected: DimKey, supplied: DimKey) extends FeeValueError

sealed abstract class ValuationError extends JavaSerializationUnsupported with Product with Serializable
final case class ValuationInstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId)
  extends ValuationError

/** Component of an attributed price-PnL input named by a precise failure location. */
enum AttributedPricePnlComponent extends JavaSerializationUnsupported:
  case Position, PositionGrid, Market, Base, Quote, Settlement, Price, PriceGrid, Value

/** Stable source location for attributed price-PnL validation and valuation failures. */
enum AttributedPricePnlLocation extends JavaSerializationUnsupported:
  case Change(index: Int, component: AttributedPricePnlComponent)
  case Endpoint(component: AttributedPricePnlComponent)

/** Closed failures for the exact finite attributed price-PnL calculation. */
enum AttributedPricePnlViolation extends JavaSerializationUnsupported:
  case InstrumentMismatch(
    location: AttributedPricePnlLocation,
    expected: InstrumentId,
    supplied: InstrumentId)
  case ReferenceMismatch(location: AttributedPricePnlLocation, cause: ReferenceDataError)
  case NonFlatPositionRequiresMark(endingCoordinate: BigInt)
  case FlatPositionRejectsMark
  case ValuationFailure(location: AttributedPricePnlLocation, cause: ValuationError)
  case PricePnlConstruction(cause: ValuationError)

/** Non-empty, stable-order failures from an attributed price-PnL calculation. */
final class AttributedPricePnlErrors private (
  val head: AttributedPricePnlViolation,
  val tail: Vector[AttributedPricePnlViolation])
  extends JavaSerializationUnsupported:

  val violations: Vector[AttributedPricePnlViolation] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: AttributedPricePnlErrors => violations == that.violations
      case _                              => false

  override def hashCode: Int    = violations.hashCode
  override def toString: String = violations.mkString("AttributedPricePnlErrors(", ",", ")")
end AttributedPricePnlErrors

object AttributedPricePnlErrors:
  def one(head: AttributedPricePnlViolation): AttributedPricePnlErrors =
    new AttributedPricePnlErrors(head, Vector.empty)

  def from(values: Vector[AttributedPricePnlViolation]): Option[AttributedPricePnlErrors] =
    values match
      case head +: tail => Some(new AttributedPricePnlErrors(head, tail))
      case _            => None
end AttributedPricePnlErrors

sealed abstract class ContributionError extends JavaSerializationUnsupported with Product with Serializable
final case class ContributionInstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId)
  extends ContributionError
final case class ContributionConversionFailure(cause: ConversionError) extends ContributionError

sealed abstract class PnlError extends JavaSerializationUnsupported with Product with Serializable
final case class PnlInstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId) extends PnlError
final case class PnlSettlementMismatch(context: String, expected: AssetId, supplied: AssetId)           extends PnlError
final case class PnlSettlementReferenceMismatch(context: String, cause: ReferenceDataError)             extends PnlError
