package trading.fee

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.scenario.*
import trading.support.DownstreamFixtures

sealed trait FirstPolicyFailure
case object FirstRejected extends FirstPolicyFailure

sealed trait SecondPolicyFailure
case object SecondRejected extends SecondPolicyFailure

sealed trait ClientPolicyFailure
object ClientPolicyFailure:
  final case class First(cause: FirstPolicyFailure)   extends ClientPolicyFailure
  final case class Second(cause: SecondPolicyFailure) extends ClientPolicyFailure

final class FeePolicyStrategySuite extends ScalaCheckSuite:
  private val fixture    = new DownstreamFixtures
  private val instrument = fixture.linear

  private type Policy[+E] = FeePolicy[
    E,
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D,
    instrument.roles.settle.D
  ]

  private val lots        = Lots.fromCount(instrument)(2).toOption.get
  private val order       = Order.market(instrument)(Side.Buy, lots).toOption.get
  private val state       = fixture.state(instrument, Rational(100))
  private val slice       = LiquiditySlice.create(instrument)(lots, state, LiquidityRole.Taker).toOption.get
  private val assumptions = ScenarioAssumptions.one(order)(
    order.activation.evidence,
    order.execution.resolution,
    slice
  ).toOption.get
  private val scenario     = OrderScenario.evaluate(instrument)(assumptions).toOption.get
  private val denomination = FeeDenomination
    .create(instrument)(fixture.usd, fixture.usdCents, QuantizationPolicy.TowardZero)
    .toOption
    .get

  private def directive(
    name: String,
    amount: Rational,
    source: SliceIndex = SliceIndex.zero
  ): FeeDirective.Aux[fixture.usd.D] =
    val fee = Fee
      .create(instrument)(
        denomination,
        FeeKind.from(name).toOption.get,
        Quantity(fixture.usd.dimension.ref, amount)
      )
      .toOption
      .get
    FeeDirective(fee, source)

  private def successful(id: InstrumentId, directives: Vector[FeeDirective]): Policy[Nothing] = new Policy[Nothing]:
    val instrumentId: InstrumentId                                                           = id
    def evaluate(value: policyScenario): Either[PolicyErrors[Nothing], Vector[FeeDirective]] =
      Right(directives)

  private def failed[E](id: InstrumentId, errors: PolicyErrors[E]): Policy[E] = new Policy[E]:
    val instrumentId: InstrumentId                                                     = id
    def evaluate(value: policyScenario): Either[PolicyErrors[E], Vector[FeeDirective]] =
      Left(errors)

  private type policyScenario = OrderScenario[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D,
    instrument.MarketState
  ]

  test("policy errors retain a public non-empty head and stable tail without leaking a validation collection"):
    val errors = PolicyErrors.of("first", "second", "third")
    assertEquals(errors.head, "first")
    assertEquals(errors.tail, Vector("second", "third"))
    assertEquals(errors.toVector, Vector("first", "second", "third"))
    assertEquals(errors.map(_.length).toVector, Vector(5, 6, 5))
    assertEquals(errors.concat(PolicyErrors.one("fourth")).toVector, Vector("first", "second", "third", "fourth"))
    assertEquals(PolicyErrors.fromVector(Vector.empty[String]), Left(EmptyPolicyErrors))
    assertEquals(PolicyErrors.fromVector(Vector("only")).map(_.toVector), Right(Vector("only")))

  test("slice indices refine nonnegativity and directives retain only typed fee plus requested coordinate"):
    assertEquals(SliceIndex.from(-1), Left(NegativeSliceIndex(-1)))
    assertEquals(SliceIndex.from(0).map(_.value), Right(0))
    assertEquals(SliceIndex.from(3).map(_.value), Right(3))

    val index                     = SliceIndex.from(2).toOption.get
    val typed: Fee[fixture.usd.D] = directive("typed", Rational(-1, 100), index).fee
    val packaged: FeeDirective    = FeeDirective(typed, index)
    assert(packaged.fee == typed)
    assertEquals(packaged.sourceSlice.value, 2)

  test("empty composition is the total no-fee identity"):
    val noFees = FeePolicy.noFees(instrument)
    val empty  = FeePolicy.combine(instrument)(Vector.empty).toOption.get
    assertEquals(noFees.instrumentId, instrument.identity.id)
    assertEquals(noFees.evaluate(scenario), Right(Vector.empty))
    assertEquals(empty.evaluate(scenario), Right(Vector.empty))

  test("composition concatenates successful directives and accumulates every failure in component order"):
    val first     = directive("first", Rational(-1, 100))
    val second    = directive("second", Rational(1, 100))
    val third     = directive("third", Rational(-1, 50))
    val successes = FeePolicy
      .combine(instrument)(
        Vector(
          successful(instrument.identity.id, Vector(first, second)),
          successful(instrument.identity.id, Vector.empty),
          successful(instrument.identity.id, Vector(third))
        )
      )
      .toOption
      .get
    assertEquals(successes.evaluate(scenario), Right(Vector(first, second, third)))

    val failures = FeePolicy
      .combine[String, instrument.type](instrument)(
        Vector(
          failed(instrument.identity.id, PolicyErrors.of("a1", "a2")),
          successful(instrument.identity.id, Vector(first)),
          failed(instrument.identity.id, PolicyErrors.one("c1"))
        )
      )
      .toOption
      .get
    assertEquals(failures.evaluate(scenario).left.map(_.toVector), Left(Vector("a1", "a2", "c1")))

  test("checked composition reports every foreign component before constructing a policy"):
    val firstForeign  = fixture.foreign.identity.id
    val secondForeign = InstrumentId.from("another-foreign").toOption.get
    val result        = FeePolicy.combine(instrument)(
      Vector(
        successful(instrument.identity.id, Vector.empty),
        successful(firstForeign, Vector.empty),
        successful(secondForeign, Vector.empty)
      )
    )
    assertEquals(
      result.left.map(_.toVector),
      Left(
        Vector(
          ForeignPolicyInstrument(1, instrument.identity.id, firstForeign),
          ForeignPolicyInstrument(2, instrument.identity.id, secondForeign)
        )
      )
    )

  test("distinct policy failures map into a caller-owned sum without erasure"):
    val first: Policy[ClientPolicyFailure] =
      failed(instrument.identity.id, PolicyErrors.one(FirstRejected))
        .mapError(ClientPolicyFailure.First.apply)
    val second: Policy[ClientPolicyFailure] =
      failed(instrument.identity.id, PolicyErrors.one(SecondRejected))
        .mapError(ClientPolicyFailure.Second.apply)
    val combined = FeePolicy.combine(instrument)(Vector(first, second)).toOption.get

    assertEquals(
      combined.evaluate(scenario).left.map(_.toVector),
      Left(
        Vector(
          ClientPolicyFailure.First(FirstRejected),
          ClientPolicyFailure.Second(SecondRejected)
        )
      )
    )

  property("contextual composition is observationally associative with no-fee as both identities"):
    forAll { (firstFails: Boolean, secondFails: Boolean, thirdFails: Boolean) =>
      def component(name: String, fails: Boolean): Policy[String] =
        if fails then failed(instrument.identity.id, PolicyErrors.one(name))
        else successful(instrument.identity.id, Vector(directive(name, Rational(-1, 100)))).widen[String]

      val first             = component("first", firstFails)
      val second            = component("second", secondFails)
      val third             = component("third", thirdFails)
      val ab                = FeePolicy.combine(instrument)(Vector(first, second)).toOption.get
      val bc                = FeePolicy.combine(instrument)(Vector(second, third)).toOption.get
      val left              = FeePolicy.combine(instrument)(Vector(ab, third)).toOption.get
      val right             = FeePolicy.combine(instrument)(Vector(first, bc)).toOption.get
      val none              = FeePolicy.noFees(instrument).widen[String]
      val withLeftIdentity  = FeePolicy.combine(instrument)(Vector(none, left)).toOption.get
      val withRightIdentity = FeePolicy.combine(instrument)(Vector(left, none)).toOption.get
      val observed          = left.evaluate(scenario)

      right.evaluate(scenario) == observed &&
      withLeftIdentity.evaluate(scenario) == observed &&
      withRightIdentity.evaluate(scenario) == observed
    }
end FeePolicyStrategySuite
