package trading.fee

import munit.FunSuite

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*
import trading.quantity.grid.QuantizationPolicy
import trading.scenario.*
import trading.support.DownstreamFixtures

final class FeeInclusivePnlSuite extends FunSuite:
  private enum EntryCause:
    case Rejected(code: Int)

  private enum ExitCause:
    case Rejected(code: Int)

  private enum CombinedCause:
    case Entry(cause: EntryCause)
    case Exit(cause: ExitCause)

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

  private def scenario[I <: Instrument](
    target: I
  )(
    side: Side,
    slices: Vector[(BigInt, target.MarketState)]
  ): OrderScenario[target.roles.position.D, target.roles.base.D, target.roles.quote.D, target.MarketState] =
    val total   = Lots.fromCount(target)(slices.map(_._1).sum).toOption.get
    val order   = Order.market(target)(side, total).toOption.get
    val matched = slices.map: (count, market) =>
      LiquiditySlice
        .create(target)(Lots.fromCount(target)(count).toOption.get, market, LiquidityRole.Taker)
        .toOption
        .get
    val assumptions = ScenarioAssumptions
      .fromVector(order)(order.activation.evidence, order.execution.resolution, matched)
      .toOption
      .get
    OrderScenario.evaluate(target)(assumptions).toOption.get
  end scenario

  private def roundTrip[I <: Instrument](
    target: I
  )(
    entrySide: Side,
    entry: Vector[(BigInt, target.MarketState)],
    exit: Vector[(BigInt, target.MarketState)]
  ): RoundTripScenario[target.roles.position.D, target.roles.base.D, target.roles.quote.D, target.MarketState] =
    val exitSide = entrySide match
      case Side.Buy  => Side.Sell
      case Side.Sell => Side.Buy
    RoundTripScenario
      .create(target)(scenario(target)(entrySide, entry), scenario(target)(exitSide, exit))
      .toOption
      .get
  end roundTrip

  private def localRoundTrip(
    entry: Vector[(BigInt, instrument.MarketState)],
    exit: Vector[(BigInt, instrument.MarketState)]
  ) =
    roundTrip(instrument)(Side.Buy, entry, exit)

  private def fee[D <: Dim](
    denomination: FeeDenomination[D],
    name: String,
    amount: Rational
  ): Fee[D] =
    Fee
      .create(instrument)(
        denomination,
        FeeKind.from(name).toOption.get,
        Quantity(denomination.asset.dimension.ref, amount)
      )
      .toOption
      .get

  private def successful(
    directives: Vector[FeeDirective]
  ): Policy[Nothing] = new Policy[Nothing]:
    val instrumentId: InstrumentId                                                        = instrument.identity.id
    def evaluate(scenario: Scenario): Either[PolicyErrors[Nothing], Vector[FeeDirective]] =
      Right(directives)

  private def rejecting[E](
    causes: PolicyErrors[E],
    evaluated: () => Unit
  ): Policy[E] = new Policy[E]:
    val instrumentId: InstrumentId                                                  = instrument.identity.id
    def evaluate(scenario: Scenario): Either[PolicyErrors[E], Vector[FeeDirective]] =
      evaluated()
      Left(causes)

  private def market(price: Rational, tokenRate: Option[Rational] = None): instrument.MarketState =
    val conversions = tokenRate.toVector.map: rate =>
      SettlementConversion.exact(instrument)(fixture.token)(rate).toOption.get
    MarketState
      .quoteSettled(instrument)(fixture.price(instrument, price), conversions)
      .toOption
      .get

  test("different leg policies retain charges, rebate, third-asset slice conversion, and sole core totals"):
    val trip = localRoundTrip(
      Vector(BigInt(400) -> market(Rational(100), Some(Rational(2))),
        BigInt(600)      -> market(Rational(100), Some(Rational(3)))),
      Vector(BigInt(300) -> market(Rational(110), Some(Rational(4))),
        BigInt(700)      -> market(Rational(110), Some(Rational(5))))
    )
    val entryTokenCharge = fee(tokenDenomination, "entry-token", Rational(-1, 1000))
    val entryUsdRebate   = fee(usdDenomination, "entry-rebate", Rational(1, 50))
    val exitTokenCharge  = fee(tokenDenomination, "exit-token", Rational(-1, 500))
    val entryPolicy      = successful(
      Vector(
        FeeDirective(entryTokenCharge, SliceIndex.zero),
        FeeDirective(entryUsdRebate, SliceIndex.from(1).toOption.get)
      )
    )
    val exitPolicy = successful(Vector(FeeDirective(exitTokenCharge, SliceIndex.from(1).toOption.get)))
    val policies   = RoundTripFeePolicies(entryPolicy, exitPolicy)
    val result     = FeeInclusivePnl.evaluate(instrument)(trip, policies).toOption.get

    assert(result.roundTrip.eq(trip))
    assert(result.entryFees.scenario.eq(trip.entry))
    assert(result.exitFees.scenario.eq(trip.exit))
    assertEquals(result.entryFees.fees.map(_.fee), Vector(entryTokenCharge, entryUsdRebate))
    assertEquals(result.exitFees.fees.map(_.fee), Vector(exitTokenCharge))
    assertEquals(
      result.attributedContributions.map(value => (value.leg, value.directiveOrdinal, value.sourceIndex.value)),
      Vector(
        (RoundTripLeg.Entry, 0, 0),
        (RoundTripLeg.Entry, 1, 1),
        (RoundTripLeg.Exit, 0, 1)
      )
    )
    assertEquals(
      result.attributedContributions.map(_.contribution.quantity.coefficient),
      Vector(Rational(-1, 500), Rational(1, 50), Rational(-1, 100))
    )
    assertEquals(result.pricePnl.quantity.coefficient, Rational(10))
    assertEquals(result.feePnl.coefficient, Rational(1, 125))
    assertEquals(result.netPnl.coefficient, Rational(1251, 125))
    assert(result.pricePnl.eq(result.pnl.pricePnl))
    assert(result.pricePnl.settlement.eq(instrument.roles.settle))
    assert(result.settledFeeContributions.eq(result.pnl.settledFeeContributions))
    assert(result.settledFeeContributions.forall(_.settlement.eq(instrument.roles.settle)))
    assertEquals(result.attributedContributions.map(_.contribution), result.settledFeeContributions)
    assertEquals(FeeInclusivePnl.evaluate(instrument)(trip, policies), Right(result))

  test("same-policy convenience preserves the explicit product and exact no-fee identity"):
    val policy   = FeePolicy.noFees(instrument)
    val policies = RoundTripFeePolicies.same(policy)
    val trip     = localRoundTrip(
      Vector(BigInt(1000) -> market(Rational(100))),
      Vector(BigInt(1000) -> market(Rational(90)))
    )
    val result = FeeInclusivePnl.evaluate(instrument)(trip, policies).toOption.get

    assert(policies.entry.eq(policy))
    assert(policies.exit.eq(policy))
    assertEquals(result.entryFees.fees, Vector.empty)
    assertEquals(result.exitFees.fees, Vector.empty)
    assertEquals(result.attributedContributions, Vector.empty)
    assertEquals(result.settledFeeContributions, Vector.empty)
    assertEquals(result.feePnl.coefficient, Rational.zero)
    assertEquals(result.netPnl, result.pricePnl.quantity)

  test("multi-slice short valuation retains exact price, fee attribution, settlement, and net PnL"):
    val trip = roundTrip(instrument)(
      Side.Sell,
      Vector(BigInt(400) -> market(Rational(110)), BigInt(600) -> market(Rational(110))),
      Vector(BigInt(300) -> market(Rational(100)), BigInt(700) -> market(Rational(100)))
    )
    val entryCharge = fee(usdDenomination, "short-entry", Rational(-1, 100))
    val exitCharge  = fee(usdDenomination, "short-exit", Rational(-1, 100))
    val policies    = RoundTripFeePolicies(
      successful(Vector(FeeDirective(entryCharge, SliceIndex.zero))),
      successful(Vector(FeeDirective(exitCharge, SliceIndex.zero)))
    )
    val result = FeeInclusivePnl.evaluate(instrument)(trip, policies).toOption.get

    assertEquals(result.pricePnl.quantity.coefficient, Rational(10))
    assertEquals(result.feePnl.coefficient, Rational(-1, 50))
    assertEquals(result.netPnl.coefficient, Rational(499, 50))
    assertEquals(
      result.attributedContributions.map(value => (value.leg, value.directiveOrdinal, value.sourceIndex.value)),
      Vector((RoundTripLeg.Entry, 0, 0), (RoundTripLeg.Exit, 0, 0))
    )
    assertEquals(
      result.settledFeeContributions.map(_.quantity.coefficient),
      Vector(Rational(-1, 100), Rational(-1, 100))
    )
    assert(result.pricePnl.settlement.eq(instrument.roles.settle))
    assert(result.settledFeeContributions.forall(_.settlement.eq(instrument.roles.settle)))

  test("all missing conversions accumulate in stable entry then exit directive order"):
    val trip = localRoundTrip(
      Vector(BigInt(400) -> market(Rational(100)), BigInt(600) -> market(Rational(100))),
      Vector(BigInt(300) -> market(Rational(110)), BigInt(700) -> market(Rational(110)))
    )
    val firstIndex  = SliceIndex.from(1).toOption.get
    val entryPolicy = successful(
      Vector(
        FeeDirective(fee(tokenDenomination, "entry-first", Rational(-1, 1000)), firstIndex),
        FeeDirective(fee(tokenDenomination, "entry-second", Rational(-1, 500)), SliceIndex.zero)
      )
    )
    val exitPolicy = successful(
      Vector(FeeDirective(fee(tokenDenomination, "exit", Rational(1, 1000)), firstIndex))
    )
    val policies                                            = RoundTripFeePolicies(entryPolicy, exitPolicy)
    val expected: Vector[FeeInclusivePnlViolation[Nothing]] = Vector(
      FeeInclusiveConversionFailure(
        RoundTripLeg.Entry,
        0,
        firstIndex,
        ContributionConversionFailure(MissingConversion(fixture.token.id))
      ),
      FeeInclusiveConversionFailure(
        RoundTripLeg.Entry,
        1,
        SliceIndex.zero,
        ContributionConversionFailure(MissingConversion(fixture.token.id))
      ),
      FeeInclusiveConversionFailure(
        RoundTripLeg.Exit,
        0,
        firstIndex,
        ContributionConversionFailure(MissingConversion(fixture.token.id))
      )
    )

    val first  = FeeInclusivePnl.evaluate(instrument)(trip, policies)
    val second = FeeInclusivePnl.evaluate(instrument)(trip, policies)
    assertEquals(first.left.map(_.toVector), Left(expected))
    assertEquals(second, first)
    assertEquals(FeeInclusivePnl.evaluateFirst(instrument)(trip, policies), Left(expected.head))

  test("entry and exit policy failures evaluate independently and retain typed causes in stable order"):
    val trip = localRoundTrip(
      Vector(BigInt(1000) -> market(Rational(100))),
      Vector(BigInt(1000) -> market(Rational(110)))
    )
    var entryEvaluations = 0
    var exitEvaluations  = 0
    val entryPolicy      = rejecting(
      PolicyErrors.of(EntryCause.Rejected(7), EntryCause.Rejected(9)),
      () => entryEvaluations += 1
    ).mapError(CombinedCause.Entry.apply)
    val exitPolicy = rejecting(
      PolicyErrors.one(ExitCause.Rejected(11)),
      () => exitEvaluations += 1
    ).mapError(CombinedCause.Exit.apply)
    val policies                                                  = RoundTripFeePolicies(entryPolicy, exitPolicy)
    val expected: Vector[FeeInclusivePnlViolation[CombinedCause]] = Vector(
      FeeInclusiveAssessmentFailure(
        RoundTripLeg.Entry,
        FeePolicyFailure(
          FeePolicyLocation.Evaluation(0),
          CombinedCause.Entry(EntryCause.Rejected(7))
        )
      ),
      FeeInclusiveAssessmentFailure(
        RoundTripLeg.Entry,
        FeePolicyFailure(
          FeePolicyLocation.Evaluation(1),
          CombinedCause.Entry(EntryCause.Rejected(9))
        )
      ),
      FeeInclusiveAssessmentFailure(
        RoundTripLeg.Exit,
        FeePolicyFailure(
          FeePolicyLocation.Evaluation(0),
          CombinedCause.Exit(ExitCause.Rejected(11))
        )
      )
    )
    val result = FeeInclusivePnl.evaluate(instrument)(trip, policies)

    assertEquals(result.left.map(_.toVector), Left(expected))
    assertEquals(entryEvaluations, 1)
    assertEquals(exitEvaluations, 1)
    def code(cause: CombinedCause): Int =
      cause match
        case CombinedCause.Entry(EntryCause.Rejected(value)) => value
        case CombinedCause.Exit(ExitCause.Rejected(value))   => value
    assertEquals(result.left.map(_.mapPolicyCause(code).toVector), Left(expected.map(_.mapPolicyCause(code))))

  test("a successful leg converts independently beside the other leg's policy failure"):
    val trip = localRoundTrip(
      Vector(BigInt(1000) -> market(Rational(100))),
      Vector(BigInt(1000) -> market(Rational(110)))
    )
    val exitPolicy = successful(
      Vector(FeeDirective(fee(tokenDenomination, "missing-exit", Rational(-1, 1000)), SliceIndex.zero))
    ).widen[EntryCause]
    val policies = RoundTripFeePolicies(
      rejecting(PolicyErrors.one(EntryCause.Rejected(13)), () => ()),
      exitPolicy
    )

    assertEquals(
      FeeInclusivePnl.evaluate(instrument)(trip, policies).left.map(_.toVector),
      Left(
        Vector(
          FeeInclusiveAssessmentFailure(
            RoundTripLeg.Entry,
            FeePolicyFailure(FeePolicyLocation.Evaluation(0), EntryCause.Rejected(13))
          ),
          FeeInclusiveConversionFailure(
            RoundTripLeg.Exit,
            0,
            SliceIndex.zero,
            ContributionConversionFailure(MissingConversion(fixture.token.id))
          )
        )
      )
    )

  test("directive failures on both legs accumulate before any conversion"):
    val trip = localRoundTrip(
      Vector(BigInt(1000) -> market(Rational(100), Some(Rational(2)))),
      Vector(BigInt(1000) -> market(Rational(110), Some(Rational(3))))
    )
    val entryIndex = SliceIndex.from(4).toOption.get
    val exitIndex  = SliceIndex.from(7).toOption.get
    val policies   = RoundTripFeePolicies(
      successful(Vector(FeeDirective(fee(tokenDenomination, "entry-invalid", Rational(-1, 1000)), entryIndex))),
      successful(Vector(FeeDirective(fee(tokenDenomination, "exit-invalid", Rational(-1, 1000)), exitIndex)))
    )

    assertEquals(
      FeeInclusivePnl.evaluate(instrument)(trip, policies).left.map(_.toVector),
      Left(
        Vector(
          FeeInclusiveAssessmentFailure(
            RoundTripLeg.Entry,
            FeeDirectiveIndexOutOfRange(0, entryIndex, 1)
          ),
          FeeInclusiveAssessmentFailure(
            RoundTripLeg.Exit,
            FeeDirectiveIndexOutOfRange(0, exitIndex, 1)
          )
        )
      )
    )

  test("initial identity stage accumulates round-trip and both policy locations and suppresses evaluation"):
    val foreign     = fixture.foreign
    val foreignTrip = roundTrip(foreign)(
      Side.Buy,
      Vector(BigInt(1000) -> fixture.state(foreign, Rational(100))),
      Vector(BigInt(1000) -> fixture.state(foreign, Rational(110)))
    ).asInstanceOf[
      RoundTripScenario[
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D,
        instrument.MarketState
      ]
    ]
    var evaluations   = 0
    val foreignPolicy = new FeePolicy[
      String,
      foreign.roles.position.D,
      foreign.roles.base.D,
      foreign.roles.quote.D,
      foreign.roles.settle.D
    ]:
      val instrumentId: InstrumentId = foreign.identity.id
      def evaluate(
        scenario: OrderScenario[
          foreign.roles.position.D,
          foreign.roles.base.D,
          foreign.roles.quote.D,
          foreign.MarketState
        ]
      ): Either[PolicyErrors[String], Vector[FeeDirective]] =
        evaluations += 1
        Left(PolicyErrors.one("must-not-run"))
    val policies = RoundTripFeePolicies(foreignPolicy, foreignPolicy).asInstanceOf[
      RoundTripFeePolicies[
        String,
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D,
        instrument.roles.settle.D
      ]
    ]

    assertEquals(
      FeeInclusivePnl.evaluate(instrument)(foreignTrip, policies).left.map(_.toVector),
      Left(
        Vector(
          FeeInclusiveIdentityFailure(
            FeeInclusiveIdentityLocation.RoundTrip,
            instrument.identity.id,
            foreign.identity.id
          ),
          FeeInclusiveIdentityFailure(
            FeeInclusiveIdentityLocation.Policy(RoundTripLeg.Entry),
            instrument.identity.id,
            foreign.identity.id
          ),
          FeeInclusiveIdentityFailure(
            FeeInclusiveIdentityLocation.Policy(RoundTripLeg.Exit),
            instrument.identity.id,
            foreign.identity.id
          )
        )
      )
    )
    assertEquals(evaluations, 0)
end FeeInclusivePnlSuite
