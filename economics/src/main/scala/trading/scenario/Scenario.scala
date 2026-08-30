package trading.scenario

import cats.data.NonEmptyVector
import cats.syntax.all.*

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*

/** Fee classification of one complete scenario slice. */
enum LiquidityRole:
  case Maker, Taker

/** Entry or exit attribution retained on converted fee contributions. */
enum ScenarioLeg:
  case Entry, Exit

final case class LiquiditySlice[L, M] private[scenario] (
  instrumentId: InstrumentId,
  lots: L,
  market: M,
  role: LiquidityRole)

/** Cohesive evidence and non-empty matched liquidity for one stable order value. */
final class ScenarioAssumptions[L, P, M] private[scenario] (
  val instrumentId: InstrumentId,
  val target: Order[L, P]
)(
  val activationEvidence: target.activation.Evidence,
  val pricingResolution: target.execution.Resolution,
  val matchedSlices: NonEmptyVector[LiquiditySlice[L, M]])

final case class OrderScenario[L, P, M, Pos] private[scenario] (
  instrumentId: InstrumentId,
  order: Order[L, P],
  assumptions: ScenarioAssumptions[L, P, M],
  effectivePricing: EffectivePricing[P],
  positionChange: Pos)

final case class RoundTripScenario[L, P, M, Pos] private[scenario] (
  instrumentId: InstrumentId,
  entry: OrderScenario[L, P, M, Pos],
  exit: OrderScenario[L, P, M, Pos],
  heldPosition: Pos)

final class Scenarios[I <: Instrument] private[scenario] (val instrument: I):

  private val instrumentId = instrument.identity.id

  type Lots     = instrument.Lots
  type Price    = instrument.Price
  type Market   = instrument.MarketState
  type Position = instrument.PositionLots

  def slice(
    lots: Lots,
    market: Market,
    role: LiquidityRole
  ): Either[ScenarioError, LiquiditySlice[Lots, Market]] =
    ScenarioIdentityChecks
      .check("scenario.slice", instrumentId, "lots" -> lots.instrumentId, "market" -> market.instrumentId)
      .map(_ => LiquiditySlice(instrumentId, lots, market, role))

  def assumptions[O <: Order[Lots, Price]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlices: NonEmptyVector[LiquiditySlice[Lots, Market]]
  ): ScenarioAssumptions[Lots, Price, Market] =
    new ScenarioAssumptions(instrumentId, order)(activationEvidence, pricingResolution, matchedSlices)

  def assumptionsOne[O <: Order[Lots, Price]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlice: LiquiditySlice[Lots, Market]
  ): ScenarioAssumptions[Lots, Price, Market] =
    assumptions(order)(activationEvidence, pricingResolution, NonEmptyVector.one(matchedSlice))

  def assumptionsMany[O <: Order[Lots, Price]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    head: LiquiditySlice[Lots, Market],
    tail: LiquiditySlice[Lots, Market]*
  ): ScenarioAssumptions[Lots, Price, Market] =
    assumptions(order)(activationEvidence, pricingResolution, NonEmptyVector(head, tail.toVector))

  def assumptionsFromVector[O <: Order[Lots, Price]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlices: Vector[LiquiditySlice[Lots, Market]]
  ): Either[InvalidScenarioDiagnostics, ScenarioAssumptions[Lots, Price, Market]] =
    NonEmptyVector
      .fromVector(matchedSlices)
      .toRight(InvalidScenarioDiagnostics(ScenarioViolation.EmptySlices, Vector.empty))
      .map(assumptions(order)(activationEvidence, pricingResolution, _))

  /** Existing deterministic fail-fast complete-scenario boundary. */
  def order(
    order: Order[Lots, Price],
    assumptions: ScenarioAssumptions[Lots, Price, Market]
  ): Either[ScenarioError, OrderScenario[Lots, Price, Market, Position]] =
    diagnose(order, assumptions)
      .left
      .map(error => ScenarioViolationMapping.scenario(error.head))

  /** Accumulating diagnostic boundary derived from the same ordered validation stages. */
  def diagnose(
    order: Order[Lots, Price],
    assumptions: ScenarioAssumptions[Lots, Price, Market]
  ): Either[InvalidScenarioDiagnostics, OrderScenario[Lots, Price, Market, Position]] =
    val evaluated =
      for
        _                <- validateIdentities(order, assumptions)
        _                <- validateSliceTotals(order.intent.lots, assumptions.matchedSlices)
        _                <- validateTarget(order, assumptions)
        _                <- validateActivation(assumptions)
        effectivePricing <- validatePricing(assumptions)
        _                <- validateSlices(order, assumptions.matchedSlices, effectivePricing)
      yield
        val coordinate = order.intent.side.sign * order.intent.lots.count.unrefined
        val change     = PositionLots.fromCoordinate(instrument)(coordinate)
        OrderScenario(instrumentId, order, assumptions, effectivePricing, change)

    evaluated.left.map(violations => InvalidScenarioDiagnostics(violations.head, violations.tail))
  end diagnose

  def roundTrip(
    entry: OrderScenario[Lots, Price, Market, Position],
    exit: OrderScenario[Lots, Price, Market, Position]
  ): Either[ScenarioError, RoundTripScenario[Lots, Price, Market, Position]] =
    for
      _ <- ScenarioIdentityChecks.check(
             "roundTrip",
             instrumentId,
             "entry"                -> entry.instrumentId,
             "entry.positionChange" -> entry.positionChange.instrumentId,
             "exit"                 -> exit.instrumentId,
             "exit.positionChange"  -> exit.positionChange.instrumentId
           )
      result <-
        val entryCount = entry.positionChange.coordinate
        val exitCount  = exit.positionChange.coordinate
        if entryCount + exitCount != 0 then Left(InvalidRoundTrip(entryCount, exitCount))
        else Right(RoundTripScenario(instrumentId, entry, exit, entry.positionChange))
    yield result

  private def validateIdentities(
    order: Order[Lots, Price],
    assumptions: ScenarioAssumptions[Lots, Price, Market]
  ): Either[Vector[ScenarioViolation], Unit] =
    val supplied =
      Vector(
        "order"              -> order.instrumentId,
        "order.intent"       -> order.intent.instrumentId,
        "order.intent.lots"  -> order.intent.lots.instrumentId,
        "assumptions"        -> assumptions.instrumentId,
        "assumptions.target" -> assumptions.target.instrumentId
      ) ++
        assumptions.target.activation
          .observations(assumptions.activationEvidence)
          .map((name, value) => name -> value.instrumentId) ++
        assumptions.target.execution
          .observations(assumptions.pricingResolution)
          .map((name, value) => name -> value.instrumentId) ++
        assumptions.matchedSlices.toVector.zipWithIndex.flatMap: (slice, index) =>
          Vector(
            s"slices[$index]"        -> slice.instrumentId,
            s"slices[$index].lots"   -> slice.lots.instrumentId,
            s"slices[$index].market" -> slice.market.instrumentId,
            s"slices[$index].price"  -> slice.market.price.instrumentId
          )

    val accumulated = Validation.indexed(supplied): (candidate, ordinal) =>
      val (name, id) = candidate
      Validation.ensure(ordinal, id == instrumentId)(
        ScenarioViolation.Identity(s"scenario.$name", instrumentId, id)
      )
    Validation.ordered(accumulated)
  end validateIdentities

  private def validateSliceTotals(
    expected: Lots,
    slices: NonEmptyVector[LiquiditySlice[Lots, Market]]
  ): Either[Vector[ScenarioViolation], Unit] =
    val supplied = slices.toVector.foldLeft(BigInt(0))((total, slice) => total + slice.lots.count.unrefined)
    Validation.ordered(
      Validation.ensure(0, supplied == expected.count.unrefined)(
        ScenarioViolation.LotTotal(expected.count.unrefined, supplied)
      )
    )

  private def validateTarget(
    order: Order[Lots, Price],
    assumptions: ScenarioAssumptions[Lots, Price, Market]
  ): Either[Vector[ScenarioViolation], Unit] =
    Validation.ordered(
      Validation.ensure(0, assumptions.target.eq(order))(ScenarioViolation.OrderTargetMismatch)
    )

  private def validateActivation(
    assumptions: ScenarioAssumptions[Lots, Price, Market]
  ): Either[Vector[ScenarioViolation], CheckedActivation[Price]] =
    assumptions.target.activation
      .validate(assumptions.activationEvidence)
      .left
      .map(cause => Vector(ScenarioViolation.Activation(cause)))

  private def validatePricing(
    assumptions: ScenarioAssumptions[Lots, Price, Market]
  ): Either[Vector[ScenarioViolation], EffectivePricing[Price]] =
    assumptions.target.execution
      .resolve(assumptions.pricingResolution)
      .left
      .map(cause => Vector(ScenarioViolation.Pricing(cause)))

  private def validateSlices(
    order: Order[Lots, Price],
    slices: NonEmptyVector[LiquiditySlice[Lots, Market]],
    effectivePricing: EffectivePricing[Price]
  ): Either[Vector[ScenarioViolation], Unit] =
    val accumulated = Validation.indexed(slices.toVector): (slice, index) =>
      val marketRole = Validation.ensure(
        index * 3,
        effectivePricing != EffectivePricing.Market || slice.role == LiquidityRole.Taker
      )(ScenarioViolation.Slice(index, ScenarioFailureReason.MarketSliceNotTaker))
      val makerRole = Validation.ensure(
        index * 3 + 1,
        !order.execution.requiresMaker || slice.role == LiquidityRole.Maker
      )(ScenarioViolation.Slice(index, ScenarioFailureReason.MakerOnlySliceNotMaker))
      val priceQuality = Validation.ensure(
        index * 3 + 2,
        effectivePricing match
          case EffectivePricing.Market         => true
          case EffectivePricing.Limited(limit) =>
            order.intent.side match
              case Side.Buy  => slice.market.price.ticks.unrefined <= limit.ticks.unrefined
              case Side.Sell => slice.market.price.ticks.unrefined >= limit.ticks.unrefined
      )(ScenarioViolation.Slice(index, ScenarioFailureReason.SliceWorseThanLimit))
      (marketRole, makerRole, priceQuality).mapN((_, _, _) => ())

    Validation.ordered(accumulated)
  end validateSlices

end Scenarios

object Scenarios:
  def apply[I <: Instrument](instrument: I): Scenarios[instrument.type] =
    new Scenarios[instrument.type](instrument)
