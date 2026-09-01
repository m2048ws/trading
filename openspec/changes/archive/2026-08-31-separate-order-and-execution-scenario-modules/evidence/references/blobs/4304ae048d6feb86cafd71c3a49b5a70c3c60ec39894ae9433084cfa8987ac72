package trading.order

import trading.economics.instrument.*

sealed abstract class OrderError extends Product with Serializable

enum OrderFailureReason:
  case NonRestingIceberg
  case IcebergExceedsOrder(displayed: BigInt, ordered: BigInt)
  case PositionChangeMismatch(expected: BigInt, supplied: BigInt)

final case class InvalidMarketDuration(value: TimeInForce)  extends OrderError
final case class InvalidTrailingOffset(offsetTicks: BigInt) extends OrderError
final case class InvalidOrder(reason: OrderFailureReason)   extends OrderError
final case class OrderInstrumentMismatch(context: String, expected: InstrumentId, supplied: InstrumentId)
  extends OrderError

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

private[order] object OrderIdentityChecks:
  def check(
    context: String,
    expected: InstrumentId,
    supplied: (String, InstrumentId)*
  ): Either[OrderError, Unit] =
    supplied.collectFirst:
      case (name, id) if id != expected => OrderInstrumentMismatch(s"$context.$name", expected, id)
    match
      case Some(error) => Left(error)
      case None        => Right(())
