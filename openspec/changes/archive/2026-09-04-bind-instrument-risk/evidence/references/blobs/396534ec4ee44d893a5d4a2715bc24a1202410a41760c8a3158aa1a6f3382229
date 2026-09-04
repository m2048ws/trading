package trading.risk

import munit.FunSuite

import trading.economics.instrument.*
import trading.quantity.*
import trading.quantity.refinement.*

class RiskModelBoundarySuite extends FunSuite:
  private val fixtures   = new InstrumentFixtures
  private val instrument = fixtures.linear

  test("checked assessment retains exact lots, downside, identity, and dimensions"):
    val lots       = fixtures.lots(instrument, BigInt(3))
    val pnl        = pnlWithNet(instrument, Rational(-17, 3))
    val assessment = LotRiskAssessment.fromPnl(instrument)(lots, pnl).toOption.get
    assertEquals(assessment.lots, lots)
    assertEquals(assessment.downsideRisk.unrefined.coefficient, Rational(17, 3))
    assertEquals(assessment.lots.instrumentId, instrument.identity.id)
    assertEquals(assessment.positionDimension.key, instrument.roles.position.dimension.key)
    assertEquals(assessment.settlementDimension.key, instrument.roles.settle.dimension.key)

  test("assessment construction reports whether lots or PnL has foreign identity"):
    val foreignLots = fixtures.lots(fixtures.foreignIdentity, BigInt(1)).asInstanceOf[instrument.Lots]
    val localPnl    = pnlWithNet(instrument, Rational(-1))
    assertEquals(
      LotRiskAssessment.fromPnl(instrument)(foreignLots, localPnl),
      Left(
        AssessmentInstrumentMismatch(
          AssessmentInputLocation.Lots,
          instrument.identity.id,
          fixtures.foreignIdentity.identity.id
        )
      )
    )

    val localLots  = fixtures.lots(instrument, BigInt(1))
    val foreignPnl = pnlWithNet(fixtures.foreignIdentity, Rational(-1)).asInstanceOf[instrument.Pnl]
    assertEquals(
      LotRiskAssessment.fromPnl(instrument)(localLots, foreignPnl),
      Left(
        AssessmentInstrumentMismatch(
          AssessmentInputLocation.Pnl,
          instrument.identity.id,
          fixtures.foreignIdentity.identity.id
        )
      )
    )

  test("single-coordinate constructor publishes a total exact monotone model"):
    val lots       = fixtures.lots(instrument, BigInt(1))
    val assessment = LotRiskAssessment.fromPnl(instrument)(lots, pnlWithNet(instrument, Rational(-2))).toOption.get
    val model      = MonotoneLotRisk.single(instrument)(assessment).toOption.get
    assertEquals(model.instrumentId, instrument.identity.id)
    assertEquals(model.cap.unrefined, BigInt(1))
    assertEquals(model.positionDimension.key, instrument.roles.position.dimension.key)
    assertEquals(model.settlementDimension.key, instrument.roles.settle.dimension.key)

  test("independent model violations accumulate in deterministic source order"):
    val foreign     = fixtures.quanto
    val foreignLots = fixtures.lots(foreign, BigInt(2))
    val assessment  = LotRiskAssessment.fromPnl(foreign)(foreignLots, pnlWithNet(foreign, Rational(-2))).toOption.get
    val first       = MonotoneLotRisk.single(instrument)(assessment).left.toOption.get
    val second      = MonotoneLotRisk.single(instrument)(assessment).left.toOption.get
    assertEquals(first, second)
    assertEquals(
      first.toVector.map(_.productPrefix),
      Vector(
        "ModelInstrumentMismatch",
        "ModelDimensionMismatch",
        "MissingCoordinate",
        "CoordinateOutsideDomain"
      )
    )
    assertEquals(first.size, 4)

  test("casting an arbitrary function cannot manufacture a model capability"):
    val arbitrary: instrument.Lots => Rational = _.quantity.coefficient
    intercept[ClassCastException]:
      arbitrary.asInstanceOf[
        MonotoneLotRisk[instrument.roles.position.D, instrument.roles.settle.D]
      ]

  private def pnlWithNet(value: Instrument, coefficient: Rational): value.Pnl =
    val position = PositionLots.fromCoordinate(value)(BigInt(1))
    val zero     = Quantity.zero[value.roles.settle.D](using value.roles.settle.dimension.ref)
    val exit     = Quantity(value.roles.settle.dimension.ref, coefficient)
    val pricePnl = PricePnl.fromValues(value)(position, zero, exit).toOption.get
    Pnl.create(value)(pricePnl, Vector.empty).toOption.get
end RiskModelBoundarySuite
