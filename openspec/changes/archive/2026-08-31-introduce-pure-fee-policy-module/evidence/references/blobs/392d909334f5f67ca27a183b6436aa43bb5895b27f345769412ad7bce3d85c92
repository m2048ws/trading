package trading.fee

import java.util.Objects

import trading.economics.instrument.*
import trading.quantity.*
import trading.scenario.OrderScenario

/** An attempted non-empty policy-error collection was empty. */
case object EmptyPolicyErrors extends JavaSerializationUnsupported

/** Domain-owned non-empty ordered policy failures. */
final class PolicyErrors[+E] private (suppliedHead: E, suppliedTail: Vector[E]) extends JavaSerializationUnsupported:
  val head: E =
    Objects.requireNonNull(suppliedHead.asInstanceOf[AnyRef], "policy error").asInstanceOf[E]
  val tail: Vector[E] =
    val checked = Objects.requireNonNull(suppliedTail, "policy error tail")
    checked.foreach(error => Objects.requireNonNull(error.asInstanceOf[AnyRef], "policy error"))
    checked

  def toVector: Vector[E] =
    head +: tail

  def map[E2](f: E => E2): PolicyErrors[E2] =
    PolicyErrors.checked(f(head), tail.map(f))

  def concat[E2 >: E](other: PolicyErrors[E2]): PolicyErrors[E2] =
    PolicyErrors.checked(head, tail ++ other.toVector)

  override def equals(other: Any): Boolean =
    other match
      case that: PolicyErrors[?] => head == that.head && tail == that.tail
      case _                     => false

  override def hashCode: Int =
    (head, tail).hashCode
end PolicyErrors

object PolicyErrors:
  def one[E](head: E): PolicyErrors[E] =
    checked(head, Vector.empty)

  def of[E](head: E, tail: E*): PolicyErrors[E] =
    checked(head, tail.toVector)

  def fromVector[E](errors: Vector[E]): Either[EmptyPolicyErrors.type, PolicyErrors[E]] =
    errors.headOption match
      case None       => Left(EmptyPolicyErrors)
      case Some(head) => Right(checked(head, errors.tail))

  private def checked[E](head: E, tail: Vector[E]): PolicyErrors[E] =
    new PolicyErrors(head, tail)
end PolicyErrors

/** A matched-slice index must be nonnegative before a policy may request it. */
sealed abstract class SliceIndexError           extends JavaSerializationUnsupported with Product with Serializable
final case class NegativeSliceIndex(value: Int) extends SliceIndexError

/** A locally nonnegative requested matched-slice coordinate. Scenario range is validated by assessment. */
final class SliceIndex private (val value: Int) extends JavaSerializationUnsupported:
  if value < 0 then throw new IllegalArgumentException("slice index must be nonnegative")

  override def equals(other: Any): Boolean =
    other match
      case that: SliceIndex => value == that.value
      case _                => false

  override def hashCode: Int    = value.hashCode
  override def toString: String = s"SliceIndex($value)"
end SliceIndex

object SliceIndex:
  val zero: SliceIndex = new SliceIndex(0)

  def from(value: Int): Either[NegativeSliceIndex, SliceIndex] =
    Either.cond(value >= 0, new SliceIndex(value), NegativeSliceIndex(value))
end SliceIndex

/** One calculated core fee coupled only to its requested source-slice coordinate. */
sealed trait FeeDirective extends JavaSerializationUnsupported:
  type D <: Dim
  def fee: Fee[D]
  def sourceSlice: SliceIndex
end FeeDirective

object FeeDirective:
  type Aux[D0 <: Dim] = FeeDirective { type D = D0 }

  def apply[D0 <: Dim](fee: Fee[D0], sourceSlice: SliceIndex): Aux[D0] =
    new Impl(Objects.requireNonNull(fee, "directive fee"), sourceSlice)

  private final class Impl[D0 <: Dim](suppliedFee: Fee[D0], suppliedSourceSlice: SliceIndex) extends FeeDirective:
    type D = D0
    val fee: Fee[D0]            = Objects.requireNonNull(suppliedFee, "directive fee")
    val sourceSlice: SliceIndex = Objects.requireNonNull(suppliedSourceSlice, "directive source slice")

    override def equals(other: Any): Boolean =
      other match
        case that: FeeDirective => fee == that.fee && sourceSlice == that.sourceSlice
        case _                  => false

    override def hashCode: Int =
      (fee, sourceSlice.value).hashCode
  end Impl
end FeeDirective

/** Open pure scenario strategy with one typed policy-owned failure channel. */
trait FeePolicy[+E, PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim] extends JavaSerializationUnsupported:
  self =>

  def instrumentId: InstrumentId

  def evaluate(
    scenario: OrderScenario[PosD, B, Q, MarketState[B, Q, S]]
  ): Either[PolicyErrors[E], Vector[FeeDirective]]

  final def mapError[E2](f: E => E2): FeePolicy[E2, PosD, B, Q, S] =
    FeePolicy.mapError(self)(f)

  final def widen[E2 >: E]: FeePolicy[E2, PosD, B, Q, S] =
    self
end FeePolicy

/** Checked composition failed because a component belongs to another runtime instrument. */
sealed abstract class PolicyCompositionError extends JavaSerializationUnsupported with Product with Serializable
final case class ForeignPolicyInstrument(componentIndex: Int, expected: InstrumentId, supplied: InstrumentId)
  extends PolicyCompositionError

object FeePolicy:
  def noFees[I <: Instrument](
    instrument: I
  ): FeePolicy[
    Nothing,
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D,
    instrument.roles.settle.D
  ] =
    new NoFeesPolicy(instrument.identity.id)

  def combine[E, I <: Instrument](
    instrument: I
  )(
    components: Vector[
      FeePolicy[
        E,
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D,
        instrument.roles.settle.D
      ]
    ]
  ): Either[
    PolicyErrors[PolicyCompositionError],
    FeePolicy[
      E,
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.roles.settle.D
    ]
  ] =
    val expected = instrument.identity.id
    val foreign  = components.zipWithIndex.collect:
      case (component, index) if component.instrumentId != expected =>
        ForeignPolicyInstrument(index, expected, component.instrumentId): PolicyCompositionError

    PolicyErrors.fromVector(foreign) match
      case Right(errors) => Left(errors)
      case Left(_)       =>
        val normalized = components.flatMap(component => normalizedComponents(component))
        Right(
          normalized.size match
            case 0 => new NoFeesPolicy(expected)
            case 1 => normalized.head
            case _ => new CompositePolicy(expected, normalized)
        )
  end combine

  private[fee] def mapError[E, E2, D <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    policy: FeePolicy[E, D, B, Q, S]
  )(
    f: E => E2
  ): FeePolicy[E2, D, B, Q, S] =
    policy match
      case _: NoFeesPolicy[?, ?, ?, ?]               => new NoFeesPolicy(policy.instrumentId)
      case composite: CompositePolicy[?, ?, ?, ?, ?] =>
        // The scrutinee fixes the component dimensions and error type even though JVM erasure hides them here.
        val components = composite.components
          .asInstanceOf[Vector[FeePolicy[E, D, B, Q, S]]]
          .map(component => mapError(component)(f))
        new CompositePolicy(policy.instrumentId, components)
      case _ => new MappedPolicy(policy, f)

  private def normalizedComponents[E, D <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    policy: FeePolicy[E, D, B, Q, S]
  ): Vector[FeePolicy[E, D, B, Q, S]] =
    policy match
      case _: NoFeesPolicy[?, ?, ?, ?]               => Vector.empty
      case composite: CompositePolicy[?, ?, ?, ?, ?] =>
        // The scrutinee already fixes E/D/B/Q/S. JVM erasure prevents expressing those equalities in this private
        // pattern, while CompositePolicy construction retains exactly that component vector.
        composite.components.asInstanceOf[Vector[FeePolicy[E, D, B, Q, S]]]
      case other => Vector(other)

  private final class NoFeesPolicy[D <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    suppliedInstrumentId: InstrumentId)
    extends FeePolicy[Nothing, D, B, Q, S]:

    val instrumentId: InstrumentId = Objects.requireNonNull(suppliedInstrumentId, "policy instrument")

    def evaluate(
      scenario: OrderScenario[D, B, Q, MarketState[B, Q, S]]
    ): Either[PolicyErrors[Nothing], Vector[FeeDirective]] =
      Right(Vector.empty)
  end NoFeesPolicy

  private final class CompositePolicy[+E, D <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    suppliedInstrumentId: InstrumentId,
    suppliedComponents: Vector[FeePolicy[E, D, B, Q, S]])
    extends FeePolicy[E, D, B, Q, S]:

    val instrumentId: InstrumentId                   = Objects.requireNonNull(suppliedInstrumentId, "policy instrument")
    val components: Vector[FeePolicy[E, D, B, Q, S]] =
      val checked = Objects.requireNonNull(suppliedComponents, "policy components")
      if checked.size < 2 then throw new IllegalArgumentException("a composite policy requires at least two components")
      checked.foreach: component =>
        val nonNull = Objects.requireNonNull(component, "policy component")
        if nonNull.instrumentId != instrumentId then
          throw new IllegalArgumentException("a composite policy requires one instrument identity")
      checked

    def evaluate(
      scenario: OrderScenario[D, B, Q, MarketState[B, Q, S]]
    ): Either[PolicyErrors[E], Vector[FeeDirective]] =
      val errors     = Vector.newBuilder[E]
      val directives = Vector.newBuilder[FeeDirective]
      components.foreach: component =>
        component.evaluate(scenario) match
          case Left(componentErrors)      => errors ++= componentErrors.toVector
          case Right(componentDirectives) => directives ++= componentDirectives
      val accumulatedErrors = errors.result()
      PolicyErrors.fromVector(accumulatedErrors) match
        case Right(nonEmpty) => Left(nonEmpty)
        case Left(_)         => Right(directives.result())
  end CompositePolicy

  private final class MappedPolicy[E, E2, D <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    suppliedPolicy: FeePolicy[E, D, B, Q, S],
    suppliedMapping: E => E2)
    extends FeePolicy[E2, D, B, Q, S]:

    private val policy             = Objects.requireNonNull(suppliedPolicy, "mapped policy")
    private val f                  = Objects.requireNonNull(suppliedMapping, "error mapping")
    val instrumentId: InstrumentId = policy.instrumentId

    def evaluate(
      scenario: OrderScenario[D, B, Q, MarketState[B, Q, S]]
    ): Either[PolicyErrors[E2], Vector[FeeDirective]] =
      policy.evaluate(scenario).left.map(_.map(f))
  end MappedPolicy
end FeePolicy
