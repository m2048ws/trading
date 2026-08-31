package trading.fee.policy

import trading.economics.instrument.*
import trading.fee.*
import trading.quantity.JavaSerializationUnsupported
import trading.scenario.*

/** Temporary integration locations retained only while fee-inclusive orchestration is migrated. */
enum FeeOrchestrationLocation:
  case RoundTrip
  case EntryScenario
  case ExitScenario
  case Policy

/** Generic temporary integration failure; policy causes remain typed through assessment. */
sealed abstract class FeeOrchestrationError[+E] extends JavaSerializationUnsupported with Product with Serializable

final case class FeeOrchestrationIdentity(
  location: FeeOrchestrationLocation,
  expected: InstrumentId,
  supplied: InstrumentId)
  extends FeeOrchestrationError[Nothing]

final case class FeeOrchestrationAssessmentFailure[+E](
  leg: RoundTripLeg,
  causes: FeeAssessmentErrors[E])
  extends FeeOrchestrationError[E]

final case class FeeOrchestrationValuationFailure(cause: ScenarioValuationError) extends FeeOrchestrationError[Nothing]

final case class FeeOrchestrationContributionFailure(
  leg: RoundTripLeg,
  slice: SliceIndex,
  cause: ContributionError)
  extends FeeOrchestrationError[Nothing]

final case class FeeOrchestrationPnlFailure(cause: PnlError) extends FeeOrchestrationError[Nothing]
