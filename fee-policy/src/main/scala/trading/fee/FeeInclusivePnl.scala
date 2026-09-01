package trading.fee

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.Objects
import scala.annotation.nowarn

import trading.economics.instrument.*
import trading.quantity.*
import trading.scenario.*

/** Explicit immutable policy selection for the two legs of one round trip. */
final case class RoundTripFeePolicies[+E, PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim](
  entry: FeePolicy[E, PosD, B, Q, S],
  exit: FeePolicy[E, PosD, B, Q, S])
  extends JavaSerializationUnsupported:

  Objects.requireNonNull(entry, "entry fee policy")
  Objects.requireNonNull(exit, "exit fee policy")
end RoundTripFeePolicies

object RoundTripFeePolicies:
  def same[E, PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    policy: FeePolicy[E, PosD, B, Q, S]
  ): RoundTripFeePolicies[E, PosD, B, Q, S] =
    RoundTripFeePolicies(policy, policy)
end RoundTripFeePolicies

/** Closed location of one input checked before any fee-inclusive dependent work. */
enum FeeInclusiveIdentityLocation:
  case RoundTrip
  case Policy(leg: RoundTripLeg)

/** One focused fee-inclusive PnL failure retaining any policy-owned cause. */
sealed abstract class FeeInclusivePnlViolation[+E] extends JavaSerializationUnsupported with Product with Serializable:

  private[fee] def mapPolicyCause[E2](f: E => E2): FeeInclusivePnlViolation[E2]
end FeeInclusivePnlViolation

final case class FeeInclusiveIdentityFailure(
  location: FeeInclusiveIdentityLocation,
  expected: InstrumentId,
  supplied: InstrumentId)
  extends FeeInclusivePnlViolation[Nothing]:

  private[fee] def mapPolicyCause[E2](f: Nothing => E2): FeeInclusivePnlViolation[E2] = this
end FeeInclusiveIdentityFailure

final case class FeeInclusiveScenarioPriceFailure(cause: ScenarioValuationError)
  extends FeeInclusivePnlViolation[Nothing]:

  private[fee] def mapPolicyCause[E2](f: Nothing => E2): FeeInclusivePnlViolation[E2] = this
end FeeInclusiveScenarioPriceFailure

final case class FeeInclusiveAssessmentFailure[+E](
  leg: RoundTripLeg,
  cause: FeeAssessmentViolation[E])
  extends FeeInclusivePnlViolation[E]:

  private[fee] def mapPolicyCause[E2](f: E => E2): FeeInclusivePnlViolation[E2] =
    FeeInclusiveAssessmentFailure(leg, cause.mapPolicyCause(f))
end FeeInclusiveAssessmentFailure

final case class FeeInclusiveConversionFailure(
  leg: RoundTripLeg,
  directiveOrdinal: Int,
  sourceIndex: SliceIndex,
  cause: ContributionError)
  extends FeeInclusivePnlViolation[Nothing]:

  private[fee] def mapPolicyCause[E2](f: Nothing => E2): FeeInclusivePnlViolation[E2] = this
end FeeInclusiveConversionFailure

final case class FeeInclusiveCoreFailure(cause: PnlError) extends FeeInclusivePnlViolation[Nothing]:
  private[fee] def mapPolicyCause[E2](f: Nothing => E2): FeeInclusivePnlViolation[E2] = this
end FeeInclusiveCoreFailure

/** An attempted non-empty fee-inclusive violation collection was empty. */
case object EmptyFeeInclusivePnlErrors extends JavaSerializationUnsupported

/** Domain-owned non-empty stable fee-inclusive failure collection. */
final class FeeInclusivePnlErrors[+E] private (
  suppliedHead: FeeInclusivePnlViolation[E],
  suppliedTail: Vector[FeeInclusivePnlViolation[E]])
  extends JavaSerializationUnsupported:

  val head: FeeInclusivePnlViolation[E] =
    Objects.requireNonNull(suppliedHead, "fee-inclusive PnL violation")
  val tail: Vector[FeeInclusivePnlViolation[E]] =
    val checked = Objects.requireNonNull(suppliedTail, "fee-inclusive PnL violation tail")
    checked.foreach(violation => Objects.requireNonNull(violation, "fee-inclusive PnL violation"))
    checked

  def toVector: Vector[FeeInclusivePnlViolation[E]] =
    head +: tail

  def concat[E2 >: E](other: FeeInclusivePnlErrors[E2]): FeeInclusivePnlErrors[E2] =
    FeeInclusivePnlErrors.checked(head, tail ++ other.toVector)

  def mapPolicyCause[E2](f: E => E2): FeeInclusivePnlErrors[E2] =
    FeeInclusivePnlErrors.checked(head.mapPolicyCause(f), tail.map(_.mapPolicyCause(f)))

  override def equals(other: Any): Boolean =
    other match
      case that: FeeInclusivePnlErrors[?] => head == that.head && tail == that.tail
      case _                              => false

  override def hashCode: Int =
    (head, tail).hashCode
end FeeInclusivePnlErrors

object FeeInclusivePnlErrors:
  def one[E](head: FeeInclusivePnlViolation[E]): FeeInclusivePnlErrors[E] =
    checked(head, Vector.empty)

  def of[E](
    head: FeeInclusivePnlViolation[E],
    tail: FeeInclusivePnlViolation[E]*
  ): FeeInclusivePnlErrors[E] =
    checked(head, tail.toVector)

  def fromVector[E](
    violations: Vector[FeeInclusivePnlViolation[E]]
  ): Either[EmptyFeeInclusivePnlErrors.type, FeeInclusivePnlErrors[E]] =
    violations.headOption match
      case None       => Left(EmptyFeeInclusivePnlErrors)
      case Some(head) => Right(checked(head, violations.tail))

  private def checked[E](
    head: FeeInclusivePnlViolation[E],
    tail: Vector[FeeInclusivePnlViolation[E]]
  ): FeeInclusivePnlErrors[E] =
    new FeeInclusivePnlErrors(head, tail)
end FeeInclusivePnlErrors

/** One assessed fee and its exact settled contribution with retained scenario attribution. */
sealed trait AttributedFeeContribution[PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim] extends JavaSerializationUnsupported:

  type D <: Dim
  def leg: RoundTripLeg
  def directiveOrdinal: Int
  def assessedFee: AssessedFee[PosD, B, Q, S] { type D = AttributedFeeContribution.this.D }
  def contribution: SettledFeeContribution[S]

  final def sourceIndex: SliceIndex                                       = assessedFee.sourceIndex
  final def sourceSlice: LiquiditySlice[Lots[PosD], MarketState[B, Q, S]] = assessedFee.sourceSlice
  final def originalFee: Fee[D]                                           = assessedFee.fee
end AttributedFeeContribution

@nowarn("msg=Ignoring.*qualifier")
private[fee] final class AttributedFeeContributionValue[
  D0 <: Dim,
  PosD <: Dim,
  B <: Dim,
  Q <: Dim,
  S <: Dim
] private[this] (
  val leg: RoundTripLeg,
  val directiveOrdinal: Int,
  val assessedFee: AssessedFee[PosD, B, Q, S] { type D = D0 },
  val contribution: SettledFeeContribution[S])
  extends AttributedFeeContribution[PosD, B, Q, S]:

  type D = D0

  override def equals(other: Any): Boolean =
    other match
      case that: AttributedFeeContribution[?, ?, ?, ?] =>
        leg == that.leg && directiveOrdinal == that.directiveOrdinal && assessedFee == that.assessedFee &&
        contribution == that.contribution
      case _ => false

  override def hashCode: Int =
    (leg, directiveOrdinal, assessedFee, contribution).hashCode
end AttributedFeeContributionValue

/** Exact scenario-level PnL retaining scenario, fee-assessment, and conversion provenance. */
@nowarn("msg=Ignoring.*qualifier")
final class FeeInclusivePnl[PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim] private[this] (
  val roundTrip: RoundTripScenario[PosD, B, Q, MarketState[B, Q, S]],
  val entryFees: ScenarioFees[PosD, B, Q, S],
  val exitFees: ScenarioFees[PosD, B, Q, S],
  val attributedContributions: Vector[AttributedFeeContribution[PosD, B, Q, S]],
  val pnl: Pnl[S])
  extends JavaSerializationUnsupported:

  def instrumentId: InstrumentId                                 = roundTrip.instrumentId
  def pricePnl: PricePnl[S]                                      = pnl.pricePnl
  def settledFeeContributions: Vector[SettledFeeContribution[S]] = pnl.settledFeeContributions
  def feePnl: Quantity[S]                                        = pnl.feePnl
  def netPnl: Quantity[S]                                        = pnl.netPnl

  override def equals(other: Any): Boolean =
    other match
      case that: FeeInclusivePnl[?, ?, ?, ?] =>
        roundTrip == that.roundTrip && entryFees == that.entryFees && exitFees == that.exitFees &&
        attributedContributions == that.attributedContributions && pnl == that.pnl
      case _ => false

  override def hashCode: Int =
    (roundTrip, entryFees, exitFees, attributedContributions, pnl).hashCode
end FeeInclusivePnl

/** Pure staged composition from one checked round trip and explicit leg policies to exact fee-inclusive PnL. */
object FeeInclusivePnl:
  private val attributedContributionConstructor =
    val owner = classOf[AttributedFeeContributionValue[?, ?, ?, ?, ?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[RoundTripLeg],
          java.lang.Integer.TYPE,
          classOf[AssessedFee[?, ?, ?, ?]],
          classOf[SettledFeeContribution[?]]
        )
      )

  private val resultConstructor =
    val owner = classOf[FeeInclusivePnl[?, ?, ?, ?]]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[RoundTripScenario[?, ?, ?, ?]],
          classOf[ScenarioFees[?, ?, ?, ?]],
          classOf[ScenarioFees[?, ?, ?, ?]],
          classOf[Vector[?]],
          classOf[Pnl[?]]
        )
      )

  def evaluate[E, I <: Instrument](
    instrument: I
  )(
    roundTrip: RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ],
    policies: RoundTripFeePolicies[
      E,
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ): Either[
    FeeInclusivePnlErrors[E],
    FeeInclusivePnl[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ] =
    val expected                                                = instrument.identity.id
    val identityViolations: Vector[FeeInclusivePnlViolation[E]] = Vector(
      Option.when(roundTrip.instrumentId != expected)(
        FeeInclusiveIdentityFailure(FeeInclusiveIdentityLocation.RoundTrip, expected, roundTrip.instrumentId)
      ),
      Option.when(policies.entry.instrumentId != expected)(
        FeeInclusiveIdentityFailure(
          FeeInclusiveIdentityLocation.Policy(RoundTripLeg.Entry),
          expected,
          policies.entry.instrumentId
        )
      ),
      Option.when(policies.exit.instrumentId != expected)(
        FeeInclusiveIdentityFailure(
          FeeInclusiveIdentityLocation.Policy(RoundTripLeg.Exit),
          expected,
          policies.exit.instrumentId
        )
      )
    ).flatten

    FeeInclusivePnlErrors.fromVector(identityViolations) match
      case Right(errors) => Left(errors)
      case Left(_)       => evaluateEligible(instrument, roundTrip, policies)
  end evaluate

  def evaluateFirst[E, I <: Instrument](
    instrument: I
  )(
    roundTrip: RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ],
    policies: RoundTripFeePolicies[
      E,
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ): Either[
    FeeInclusivePnlViolation[E],
    FeeInclusivePnl[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ] =
    evaluate(instrument)(roundTrip, policies).left.map(_.head)

  private def evaluateEligible[E, I <: Instrument](
    instrument: I,
    roundTrip: RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ],
    policies: RoundTripFeePolicies[
      E,
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ): Either[
    FeeInclusivePnlErrors[E],
    FeeInclusivePnl[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ] =
    val priceResult = ScenarioValuation
      .pricePnl(instrument)(roundTrip)
      .left
      .map(cause => FeeInclusivePnlErrors.one(FeeInclusiveScenarioPriceFailure(cause)))
    val entryResult = FeeAssessment
      .evaluate(instrument)(roundTrip.entry, policies.entry)
      .left
      .map(assessmentErrors(RoundTripLeg.Entry, _))
      .flatMap: fees =>
        convertLeg[E, I](instrument, RoundTripLeg.Entry, fees).map(attributed => fees -> attributed)
    val exitResult = FeeAssessment
      .evaluate(instrument)(roundTrip.exit, policies.exit)
      .left
      .map(assessmentErrors(RoundTripLeg.Exit, _))
      .flatMap: fees =>
        convertLeg[E, I](instrument, RoundTripLeg.Exit, fees).map(attributed => fees -> attributed)
    val failures = Vector(
      priceResult.left.toOption,
      entryResult.left.toOption,
      exitResult.left.toOption
    ).flatten.reduceOption(_.concat(_))

    failures match
      case Some(errors) => Left(errors)
      case None         =>
        for
          price  <- priceResult
          entry  <- entryResult
          exit   <- exitResult
          result <- compose(instrument, roundTrip, price, entry._1, exit._1, entry._2 ++ exit._2)
        yield result
  end evaluateEligible

  private def assessmentErrors[E](
    leg: RoundTripLeg,
    errors: FeeAssessmentErrors[E]
  ): FeeInclusivePnlErrors[E] =
    FeeInclusivePnlErrors.of(
      FeeInclusiveAssessmentFailure(leg, errors.head),
      errors.tail.map(FeeInclusiveAssessmentFailure(leg, _))*
    )

  private def convertLeg[E, I <: Instrument](
    instrument: I,
    leg: RoundTripLeg,
    fees: ScenarioFees[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ): Either[
    FeeInclusivePnlErrors[E],
    Vector[AttributedFeeContribution[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]]
  ] =
    val converted = fees.fees.zipWithIndex.map: (assessed, ordinal) =>
      convertAssessed[E, I](instrument, leg, ordinal, assessed)
    val conversionViolations = converted.collect:
      case Left(violation) => violation

    FeeInclusivePnlErrors.fromVector(conversionViolations) match
      case Right(errors) => Left(errors)
      case Left(_)       =>
        Right(converted.collect:
          case Right(value) => value
        )
  end convertLeg

  private def compose[E, I <: Instrument](
    instrument: I,
    roundTrip: RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ],
    pricePnl: instrument.PricePnl,
    entryFees: ScenarioFees[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ],
    exitFees: ScenarioFees[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ],
    attributed: Vector[AttributedFeeContribution[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]]
  ): Either[
    FeeInclusivePnlErrors[E],
    FeeInclusivePnl[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ] =
    Pnl
      .create(instrument)(pricePnl, attributed.map(_.contribution))
      .left
      .map(cause => FeeInclusivePnlErrors.one(FeeInclusiveCoreFailure(cause)))
      .map(pnl => constructResult(roundTrip, entryFees, exitFees, attributed, pnl))
  end compose

  private def convertAssessed[E, I <: Instrument](
    instrument: I,
    leg: RoundTripLeg,
    directiveOrdinal: Int,
    assessed: AssessedFee[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ): Either[
    FeeInclusivePnlViolation[E],
    AttributedFeeContribution[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ] =
    convertCaptured(instrument, leg, directiveOrdinal, assessed)

  private def convertCaptured[E, I <: Instrument, FD <: Dim](
    instrument: I,
    leg: RoundTripLeg,
    directiveOrdinal: Int,
    assessed: AssessedFee[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ] { type D = FD }
  ): Either[
    FeeInclusivePnlViolation[E],
    AttributedFeeContribution[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ] =
    SettledFeeContribution
      .convert(instrument)(assessed.fee, assessed.sourceSlice.market)
      .left
      .map(cause => FeeInclusiveConversionFailure(leg, directiveOrdinal, assessed.sourceIndex, cause))
      .map(contribution => constructAttributed(leg, directiveOrdinal, assessed, contribution))

  private def constructAttributed[D0 <: Dim, PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    leg: RoundTripLeg,
    directiveOrdinal: Int,
    assessed: AssessedFee[PosD, B, Q, S] { type D = D0 },
    contribution: SettledFeeContribution[S]
  ): AttributedFeeContribution[PosD, B, Q, S] =
    attributedContributionConstructor
      .invoke(leg, directiveOrdinal, assessed, contribution)
      .asInstanceOf[AttributedFeeContributionValue[D0, PosD, B, Q, S]]

  private def constructResult[PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    roundTrip: RoundTripScenario[PosD, B, Q, MarketState[B, Q, S]],
    entryFees: ScenarioFees[PosD, B, Q, S],
    exitFees: ScenarioFees[PosD, B, Q, S],
    attributed: Vector[AttributedFeeContribution[PosD, B, Q, S]],
    pnl: Pnl[S]
  ): FeeInclusivePnl[PosD, B, Q, S] =
    resultConstructor
      .invoke(roundTrip, entryFees, exitFees, attributed, pnl)
      .asInstanceOf[FeeInclusivePnl[PosD, B, Q, S]]
end FeeInclusivePnl
