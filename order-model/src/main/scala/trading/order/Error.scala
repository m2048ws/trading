package trading.order

import trading.economics.instrument.InstrumentId

enum OrderComponent:
  case Intent, Lots, TriggerPrice, LimitPrice, DisplayedLots

enum OrderViolation:
  case InstrumentMismatch(component: OrderComponent, expected: InstrumentId, supplied: InstrumentId)
  case RestingMarketDuration(value: TimeInForce)
  case NonRestingIceberg
  case IcebergExceedsOrder(displayed: BigInt, ordered: BigInt)
  case InvalidTrailingOffset(value: BigInt)

final class OrderViolations private (
  val head: OrderViolation,
  val tail: Vector[OrderViolation]):

  val violations: Vector[OrderViolation] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: OrderViolations => violations == that.violations
      case _                     => false

  override def hashCode: Int = violations.hashCode

  override def toString: String = violations.mkString("OrderViolations(", ",", ")")
end OrderViolations

object OrderViolations:
  def one(head: OrderViolation): OrderViolations =
    new OrderViolations(head, Vector.empty)

  def from(violations: Vector[OrderViolation]): Option[OrderViolations] =
    violations match
      case head +: tail => Some(new OrderViolations(head, tail))
      case _            => None
end OrderViolations

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
