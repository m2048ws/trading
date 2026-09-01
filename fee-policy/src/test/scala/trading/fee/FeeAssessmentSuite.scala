package trading.fee

import munit.FunSuite

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.scenario.*
import trading.support.DownstreamFixtures

final class FeeAssessmentSuite extends FunSuite:
  private val fixture    = new DownstreamFixtures
  private val instrument = fixture.linear

  private type Scenario = OrderScenario[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D,
    instrument.MarketState
  ]
  private type Policy[+E] = FeePolicy[
    E,
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D,
    instrument.roles.settle.D
  ]

  private val usdDenomination = FeeDenomination
    .create(instrument)(fixture.usd, fixture.usdCents, QuantizationPolicy.TowardZero)
    .toOption
    .get
  private val tokenDenomination = FeeDenomination
    .create(instrument)(fixture.token, fixture.tokenMillis, QuantizationPolicy.TowardZero)
    .toOption
    .get

  private def oneSliceScenario(
    lots: instrument.Lots = Lots.fromCount(instrument)(2).toOption.get,
    market: instrument.MarketState = fixture.state(instrument, Rational(100))
  ): Scenario =
    val order       = Order.market(instrument)(Side.Buy, lots).toOption.get
    val slice       = LiquiditySlice.create(instrument)(lots, market, LiquidityRole.Taker).toOption.get
    val assumptions = ScenarioAssumptions.one(order)(
      order.activation.evidence,
      order.execution.resolution,
      slice
    ).toOption.get
    OrderScenario.evaluate(instrument)(assumptions).toOption.get

  private def duplicateSliceScenario(): (
    Scenario,
    LiquiditySlice[instrument.Lots, instrument.MarketState],
    LiquiditySlice[instrument.Lots, instrument.MarketState]
  ) =
    val total       = Lots.fromCount(instrument)(2).toOption.get
    val each        = Lots.fromCount(instrument)(1).toOption.get
    val market      = fixture.state(instrument, Rational(100))
    val first       = LiquiditySlice.create(instrument)(each, market, LiquidityRole.Taker).toOption.get
    val second      = LiquiditySlice.create(instrument)(each, market, LiquidityRole.Taker).toOption.get
    val order       = Order.market(instrument)(Side.Buy, total).toOption.get
    val assumptions = ScenarioAssumptions.many(order)(
      order.activation.evidence,
      order.execution.resolution,
      first,
      second
    ).toOption.get
    (OrderScenario.evaluate(instrument)(assumptions).toOption.get, first, second)

  private def usdFee(name: String, amount: Rational): Fee[fixture.usd.D] =
    Fee
      .create(instrument)(
        usdDenomination,
        FeeKind.from(name).toOption.get,
        Quantity(fixture.usd.dimension.ref, amount)
      )
      .toOption
      .get

  private def tokenFee(name: String, amount: Rational): Fee[fixture.token.D] =
    Fee
      .create(instrument)(
        tokenDenomination,
        FeeKind.from(name).toOption.get,
        Quantity(fixture.token.dimension.ref, amount)
      )
      .toOption
      .get

  private def successful(id: InstrumentId, directives: Vector[FeeDirective]): Policy[Nothing] = new Policy[Nothing]:
    val instrumentId: InstrumentId                                                        = id
    def evaluate(scenario: Scenario): Either[PolicyErrors[Nothing], Vector[FeeDirective]] = Right(directives)

  test("empty policy output retains the exact scenario once with no assessed fees"):
    val scenario = oneSliceScenario()
    val result   = FeeAssessment.evaluate(instrument)(scenario, successful(instrument.identity.id, Vector.empty))
      .toOption
      .get

    assert(result.scenario.eq(scenario))
    assertEquals(result.instrumentId, scenario.instrumentId)
    assertEquals(result.fees, Vector.empty)

  test("assessment selects heterogeneous fees from their exact requested immutable slices"):
    val (scenario, firstSlice, secondSlice) = duplicateSliceScenario()
    val firstDirective                      = FeeDirective(usdFee("usd", Rational(-1, 100)), SliceIndex.zero)
    val secondIndex                         = SliceIndex.from(1).toOption.get
    val secondDirective                     = FeeDirective(tokenFee("token", Rational(1, 1000)), secondIndex)
    val result                              = FeeAssessment
      .evaluate(instrument)(
        scenario,
        successful(instrument.identity.id, Vector(firstDirective, secondDirective))
      )
      .toOption
      .get

    assertEquals(firstSlice, secondSlice)
    assert(!firstSlice.eq(secondSlice))
    assert(result.fees(0).sourceSlice.eq(firstSlice))
    assert(result.fees(1).sourceSlice.eq(secondSlice))
    assertEquals(result.fees.map(_.sourceIndex.value), Vector(0, 1))
    assertEquals(result.fees.map(_.fee), Vector(firstDirective.fee, secondDirective.fee))

  test("foreign inputs accumulate in stable location order and suppress policy evaluation"):
    val localScenario = oneSliceScenario()
    val foreignLots   = Lots.fromCount(fixture.foreign)(2).toOption.get
    val foreignOrder  = Order.market(fixture.foreign)(Side.Buy, foreignLots).toOption.get
    val foreignSlice  = LiquiditySlice
      .create(fixture.foreign)(
        foreignLots,
        fixture.state(fixture.foreign, Rational(100)),
        LiquidityRole.Taker
      )
      .toOption
      .get
    val foreignAssumptions = ScenarioAssumptions.one(foreignOrder)(
      foreignOrder.activation.evidence,
      foreignOrder.execution.resolution,
      foreignSlice
    ).toOption.get
    val foreignScenario = OrderScenario
      .evaluate(fixture.foreign)(foreignAssumptions)
      .toOption
      .get
      .asInstanceOf[Scenario]
    var evaluations   = 0
    val foreignPolicy = new Policy[String]:
      val instrumentId: InstrumentId                                                       = fixture.foreign.identity.id
      def evaluate(scenario: Scenario): Either[PolicyErrors[String], Vector[FeeDirective]] =
        evaluations += 1
        Left(PolicyErrors.one("must-not-run"))

    assertEquals(localScenario.instrumentId, instrument.identity.id)
    assertEquals(
      FeeAssessment.evaluate(instrument)(foreignScenario, foreignPolicy).left.map(_.toVector),
      Left(
        Vector(
          FeeAssessmentInputIdentity(
            FeeAssessmentInput.Scenario,
            instrument.identity.id,
            fixture.foreign.identity.id
          ),
          FeeAssessmentInputIdentity(
            FeeAssessmentInput.Policy,
            instrument.identity.id,
            fixture.foreign.identity.id
          )
        )
      )
    )
    assertEquals(evaluations, 0)

  test("policy failures retain exact custom causes with stable evaluation ordinals"):
    final case class Rejected(code: Int)
    val scenario = oneSliceScenario()
    val policy   = new Policy[Rejected]:
      val instrumentId: InstrumentId                                                         = instrument.identity.id
      def evaluate(scenario: Scenario): Either[PolicyErrors[Rejected], Vector[FeeDirective]] =
        Left(PolicyErrors.of(Rejected(7), Rejected(9)))

    assertEquals(
      FeeAssessment.evaluate(instrument)(scenario, policy).left.map(_.toVector),
      Left(
        Vector(
          FeePolicyFailure(FeePolicyLocation.Evaluation(0), Rejected(7)),
          FeePolicyFailure(FeePolicyLocation.Evaluation(1), Rejected(9))
        )
      )
    )

  test("every invalid index is accumulated in directive order with requested index and slice count"):
    val scenario = oneSliceScenario()
    val first    = SliceIndex.from(2).toOption.get
    val second   = SliceIndex.from(7).toOption.get
    val policy   = successful(
      instrument.identity.id,
      Vector(
        FeeDirective(usdFee("first-invalid", Rational(-1, 100)), first),
        FeeDirective(tokenFee("second-invalid", Rational(-1, 1000)), second)
      )
    )

    assertEquals(
      FeeAssessment.evaluate(instrument)(scenario, policy).left.map(_.toVector),
      Left(
        Vector(
          FeeDirectiveIndexOutOfRange(0, first, 1),
          FeeDirectiveIndexOutOfRange(1, second, 1)
        )
      )
    )

  test("foreign fee and denomination identities accumulate independently before attribution"):
    val scenario            = oneSliceScenario()
    val foreignDenomination = FeeDenomination
      .create(fixture.foreign)(fixture.usd, fixture.usdCents, QuantizationPolicy.TowardZero)
      .toOption
      .get
    val foreignFee = Fee
      .create(fixture.foreign)(
        foreignDenomination,
        FeeKind.from("foreign").toOption.get,
        Quantity(fixture.usd.dimension.ref, Rational(-1, 100))
      )
      .toOption
      .get
    val invalidIndex = SliceIndex.from(4).toOption.get
    val policy       = successful(
      instrument.identity.id,
      Vector(FeeDirective(foreignFee, invalidIndex))
    )

    assertEquals(
      FeeAssessment.evaluate(instrument)(scenario, policy).left.map(_.toVector),
      Left(
        Vector(
          FeeDirectiveIdentity(
            0,
            FeeDirectiveIdentityComponent.Fee,
            instrument.identity.id,
            fixture.foreign.identity.id
          ),
          FeeDirectiveIdentity(
            0,
            FeeDirectiveIdentityComponent.Denomination,
            instrument.identity.id,
            fixture.foreign.identity.id
          ),
          FeeDirectiveIndexOutOfRange(0, invalidIndex, 1)
        )
      )
    )

  test("assessment errors map only the policy-owned cause and preserve every location"):
    val original = FeeAssessmentErrors.of[String](
      FeeAssessmentInputIdentity(
        FeeAssessmentInput.Policy,
        instrument.identity.id,
        fixture.foreign.identity.id
      ),
      FeePolicyFailure(FeePolicyLocation.Evaluation(3), "rejected"),
      FeeDirectiveIndexOutOfRange(5, SliceIndex.zero, 0)
    )
    assertEquals(
      original.mapPolicyCause(_.length).toVector,
      Vector(
        FeeAssessmentInputIdentity(
          FeeAssessmentInput.Policy,
          instrument.identity.id,
          fixture.foreign.identity.id
        ),
        FeePolicyFailure(FeePolicyLocation.Evaluation(3), 8),
        FeeDirectiveIndexOutOfRange(5, SliceIndex.zero, 0)
      )
    )
end FeeAssessmentSuite
