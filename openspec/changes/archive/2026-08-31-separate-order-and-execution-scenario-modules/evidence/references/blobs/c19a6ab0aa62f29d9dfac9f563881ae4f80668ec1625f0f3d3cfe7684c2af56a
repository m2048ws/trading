package trading.scenario

import trading.economics.instrument.InstrumentId
import trading.order.*

enum ScenarioSliceComponent:
  case Identity, Lots, Market, Price

enum ScenarioLocation:
  case Order
  case OrderIntent
  case OrderLots
  case OrderPositionChange
  case TriggerPrice
  case LimitPrice
  case DisplayedLots
  case ActivationObserved
  case TrailingExtreme
  case PegReferencePrice
  case PegResolvedLimit
  case SliceInput(component: ScenarioSliceComponent)
  case Slice(index: Int, component: ScenarioSliceComponent)

enum ScenarioViolation:
  case EmptySlices
  case Identity(location: ScenarioLocation, expected: InstrumentId, supplied: InstrumentId)
  case LotTotal(expected: BigInt, supplied: BigInt)
  case Activation(cause: ActivationViolation)
  case Pricing(cause: PricingViolation)
  case MarketSliceNotTaker(index: Int)
  case MakerOnlySliceNotMaker(index: Int)
  case SliceWorseThanLimit(index: Int)

final class ScenarioViolations private (
  val head: ScenarioViolation,
  val tail: Vector[ScenarioViolation]):

  val violations: Vector[ScenarioViolation] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: ScenarioViolations => violations == that.violations
      case _                        => false

  override def hashCode: Int = violations.hashCode

  override def toString: String = violations.mkString("ScenarioViolations(", ",", ")")
end ScenarioViolations

object ScenarioViolations:
  def one(head: ScenarioViolation): ScenarioViolations =
    new ScenarioViolations(head, Vector.empty)

  def from(violations: Vector[ScenarioViolation]): Option[ScenarioViolations] =
    violations match
      case head +: tail => Some(new ScenarioViolations(head, tail))
      case _            => None
end ScenarioViolations

enum RoundTripComponent:
  case Entry, EntryPositionChange, Exit, ExitPositionChange

enum RoundTripViolation:
  case InstrumentMismatch(component: RoundTripComponent, expected: InstrumentId, supplied: InstrumentId)
  case PositionNotFlat(entryChange: BigInt, exitChange: BigInt)
