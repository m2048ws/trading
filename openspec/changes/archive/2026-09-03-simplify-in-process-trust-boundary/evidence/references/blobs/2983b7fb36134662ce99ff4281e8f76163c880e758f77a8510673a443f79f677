package trading.fee

import java.util.Objects

import trading.economics.instrument.*
import trading.quantity.*
import trading.scenario.*

/** Stable location of one typed failure returned by a policy evaluation. */
enum FeePolicyLocation:
  case Evaluation(errorOrdinal: Int)

/** One canonical assessment input whose runtime identity is validated before policy execution. */
enum FeeAssessmentInput:
  case Scenario
  case Policy

/** One directive identity component independently validated after policy execution. */
enum FeeDirectiveIdentityComponent:
  case Fee
  case Denomination

/** Stable projected location of one assessment input or directive component. */
enum FeeAssessmentLocation:
  case Input(input: FeeAssessmentInput)
  case DirectiveIdentity(directiveOrdinal: Int, component: FeeDirectiveIdentityComponent)
  case DirectiveSlice(directiveOrdinal: Int)

/** One focused assessment failure retaining any policy-owned cause. */
sealed abstract class FeeAssessmentViolation[+E] extends JavaSerializationUnsupported with Product with Serializable:

  private[fee] def mapPolicyCause[E2](f: E => E2): FeeAssessmentViolation[E2]
end FeeAssessmentViolation

/** One canonical assessment input belongs to another runtime instrument. */
final case class FeeAssessmentInputIdentity(
  input: FeeAssessmentInput,
  expected: InstrumentId,
  supplied: InstrumentId)
  extends FeeAssessmentViolation[Nothing]:

  val location: FeeAssessmentLocation = FeeAssessmentLocation.Input(input)

  private[fee] def mapPolicyCause[E2](f: Nothing => E2): FeeAssessmentViolation[E2] = this
end FeeAssessmentInputIdentity

/** One fee or denomination returned by a directive belongs to another runtime instrument. */
final case class FeeDirectiveIdentity(
  directiveOrdinal: Int,
  component: FeeDirectiveIdentityComponent,
  expected: InstrumentId,
  supplied: InstrumentId)
  extends FeeAssessmentViolation[Nothing]:

  val location: FeeAssessmentLocation =
    FeeAssessmentLocation.DirectiveIdentity(directiveOrdinal, component)

  private[fee] def mapPolicyCause[E2](f: Nothing => E2): FeeAssessmentViolation[E2] = this
end FeeDirectiveIdentity

/** One policy-owned cause at its stable evaluation ordinal. */
final case class FeePolicyFailure[+E](location: FeePolicyLocation, cause: E) extends FeeAssessmentViolation[E]:
  private[fee] def mapPolicyCause[E2](f: E => E2): FeeAssessmentViolation[E2] =
    FeePolicyFailure(location, f(cause))
end FeePolicyFailure

/** A directive requested a nonnegative coordinate outside the target scenario. */
final case class FeeDirectiveIndexOutOfRange(
  directiveOrdinal: Int,
  requested: SliceIndex,
  sliceCount: Int)
  extends FeeAssessmentViolation[Nothing]:

  val location: FeeAssessmentLocation = FeeAssessmentLocation.DirectiveSlice(directiveOrdinal)

  private[fee] def mapPolicyCause[E2](f: Nothing => E2): FeeAssessmentViolation[E2] = this
end FeeDirectiveIndexOutOfRange

/** An attempted non-empty fee-assessment violation collection was empty. */
case object EmptyFeeAssessmentErrors extends JavaSerializationUnsupported

/** Domain-owned non-empty ordered assessment failures with an un-erased policy cause. */
final class FeeAssessmentErrors[+E] private (
  suppliedHead: FeeAssessmentViolation[E],
  suppliedTail: Vector[FeeAssessmentViolation[E]])
  extends JavaSerializationUnsupported:

  val head: FeeAssessmentViolation[E] =
    Objects.requireNonNull(suppliedHead, "assessment violation")
  val tail: Vector[FeeAssessmentViolation[E]] =
    val checked = Objects.requireNonNull(suppliedTail, "assessment violation tail")
    checked.foreach(violation => Objects.requireNonNull(violation, "assessment violation"))
    checked

  def toVector: Vector[FeeAssessmentViolation[E]] =
    head +: tail

  def mapPolicyCause[E2](f: E => E2): FeeAssessmentErrors[E2] =
    FeeAssessmentErrors.checked(head.mapPolicyCause(f), tail.map(_.mapPolicyCause(f)))

  override def equals(other: Any): Boolean =
    other match
      case that: FeeAssessmentErrors[?] => head == that.head && tail == that.tail
      case _                            => false

  override def hashCode: Int =
    (head, tail).hashCode
end FeeAssessmentErrors

object FeeAssessmentErrors:
  def one[E](head: FeeAssessmentViolation[E]): FeeAssessmentErrors[E] =
    checked(head, Vector.empty)

  def of[E](
    head: FeeAssessmentViolation[E],
    tail: FeeAssessmentViolation[E]*
  ): FeeAssessmentErrors[E] =
    checked(head, tail.toVector)

  def fromVector[E](
    violations: Vector[FeeAssessmentViolation[E]]
  ): Either[EmptyFeeAssessmentErrors.type, FeeAssessmentErrors[E]] =
    violations.headOption match
      case None       => Left(EmptyFeeAssessmentErrors)
      case Some(head) => Right(checked(head, violations.tail))

  private def checked[E](
    head: FeeAssessmentViolation[E],
    tail: Vector[FeeAssessmentViolation[E]]
  ): FeeAssessmentErrors[E] =
    new FeeAssessmentErrors(head, tail)
end FeeAssessmentErrors

/** One fee coupled to the actual immutable scenario slice selected by its directive. */
sealed trait AssessedFee[PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim] extends JavaSerializationUnsupported:
  type D <: Dim
  def fee: Fee[D]
  def sourceIndex: SliceIndex
  def sourceSlice: LiquiditySlice[Lots[PosD], MarketState[B, Q, S]]
end AssessedFee

private[fee] final class AssessedFeeValue[D0 <: Dim, PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim] private[fee] (
  val fee: Fee[D0],
  val sourceIndex: SliceIndex,
  val sourceSlice: LiquiditySlice[Lots[PosD], MarketState[B, Q, S]])
  extends AssessedFee[PosD, B, Q, S]:

  type D = D0

  override def equals(other: Any): Boolean =
    other match
      case that: AssessedFee[?, ?, ?, ?] =>
        fee == that.fee && sourceIndex == that.sourceIndex && sourceSlice == that.sourceSlice
      case _ => false

  override def hashCode: Int =
    (fee, sourceIndex, sourceSlice).hashCode
end AssessedFeeValue

/** One scenario retained exactly once together with every centrally attributed fee. */
final class ScenarioFees[PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim] private[fee] (
  val scenario: OrderScenario[PosD, B, Q, MarketState[B, Q, S]],
  val fees: Vector[AssessedFee[PosD, B, Q, S]])
  extends JavaSerializationUnsupported:

  def instrumentId: InstrumentId = scenario.instrumentId

  override def equals(other: Any): Boolean =
    other match
      case that: ScenarioFees[?, ?, ?, ?] => scenario == that.scenario && fees == that.fees
      case _                              => false

  override def hashCode: Int =
    (scenario, fees).hashCode
end ScenarioFees

/** Canonical pure boundary from one instrument/scenario/policy context to validated fee attribution. */
object FeeAssessment:
  def evaluate[E, I <: Instrument](
    instrument: I
  )(
    scenario: OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ],
    policy: FeePolicy[
      E,
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ): Either[
    FeeAssessmentErrors[E],
    ScenarioFees[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ] =
    val expected                                           = instrument.identity.id
    val inputViolations: Vector[FeeAssessmentViolation[E]] = Vector(
      Option.when(scenario.instrumentId != expected)(
        FeeAssessmentInputIdentity(FeeAssessmentInput.Scenario, expected, scenario.instrumentId)
      ),
      Option.when(policy.instrumentId != expected)(
        FeeAssessmentInputIdentity(FeeAssessmentInput.Policy, expected, policy.instrumentId)
      )
    ).flatten

    FeeAssessmentErrors.fromVector(inputViolations) match
      case Right(errors) => Left(errors)
      case Left(_)       =>
        policy.evaluate(scenario) match
          case Left(policyErrors) =>
            Left(
              FeeAssessmentErrors.of(
                FeePolicyFailure(FeePolicyLocation.Evaluation(0), policyErrors.head),
                policyErrors.tail.zipWithIndex.map((cause, index) =>
                  FeePolicyFailure(FeePolicyLocation.Evaluation(index + 1), cause)
                )*
              )
            )
          case Right(directives) => assessDirectives(instrument, scenario, directives)
  end evaluate

  private def assessDirectives[E, I <: Instrument](
    instrument: I,
    scenario: OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ],
    directives: Vector[FeeDirective]
  ): Either[
    FeeAssessmentErrors[E],
    ScenarioFees[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ] =
    val expected   = instrument.identity.id
    val slices     = scenario.matchedSlices.toVector
    val violations = Vector.newBuilder[FeeAssessmentViolation[E]]
    val assessed   = Vector.newBuilder[
      AssessedFee[
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D,
        instrument.roles.settle.D
      ]
    ]

    directives.zipWithIndex.foreach: (directive, ordinal) =>
      val feeIdentityValid          = directive.fee.instrumentId == expected
      val denominationIdentityValid = directive.fee.denomination.instrumentId == expected
      val index                     = directive.sourceSlice.value
      val indexValid                = index < slices.size

      if !feeIdentityValid then
        violations += FeeDirectiveIdentity(
          ordinal,
          FeeDirectiveIdentityComponent.Fee,
          expected,
          directive.fee.instrumentId
        )
      if !denominationIdentityValid then
        violations += FeeDirectiveIdentity(
          ordinal,
          FeeDirectiveIdentityComponent.Denomination,
          expected,
          directive.fee.denomination.instrumentId
        )
      if !indexValid then
        violations += FeeDirectiveIndexOutOfRange(
          ordinal,
          directive.sourceSlice,
          slices.size
        )
      if feeIdentityValid && denominationIdentityValid && indexValid then
        assessed += constructAssessedFee(directive, slices(index))

    FeeAssessmentErrors.fromVector(violations.result()) match
      case Right(errors) => Left(errors)
      case Left(_)       => Right(constructScenarioFees(scenario, assessed.result()))
  end assessDirectives

  private def constructAssessedFee[PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    directive: FeeDirective,
    sourceSlice: LiquiditySlice[Lots[PosD], MarketState[B, Q, S]]
  ): AssessedFee[PosD, B, Q, S] =
    constructCapturedAssessedFee(directive, sourceSlice)

  private def constructCapturedAssessedFee[D0 <: Dim, PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    directive: FeeDirective { type D = D0 },
    sourceSlice: LiquiditySlice[Lots[PosD], MarketState[B, Q, S]]
  ): AssessedFee[PosD, B, Q, S] =
    new AssessedFeeValue[D0, PosD, B, Q, S](directive.fee, directive.sourceSlice, sourceSlice)

  private def constructScenarioFees[PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    scenario: OrderScenario[PosD, B, Q, MarketState[B, Q, S]],
    fees: Vector[AssessedFee[PosD, B, Q, S]]
  ): ScenarioFees[PosD, B, Q, S] =
    new ScenarioFees(scenario, fees)
end FeeAssessment
