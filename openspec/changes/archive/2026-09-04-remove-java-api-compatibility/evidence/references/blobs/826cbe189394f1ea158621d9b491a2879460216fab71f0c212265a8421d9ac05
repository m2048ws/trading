package trading.scenario

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*

/** Fee classification of one complete scenario slice. */
enum LiquidityRole:
  case Maker, Taker

/** Closed entry/exit location shared by scenario valuation and downstream attribution. */
enum RoundTripLeg:
  case Entry, Exit

final class LiquiditySlice[L, M] private (
  val instrumentId: InstrumentId,
  val lots: L,
  val market: M,
  val role: LiquidityRole):

  override def equals(other: Any): Boolean =
    other match
      case that: LiquiditySlice[?, ?] =>
        instrumentId == that.instrumentId && lots == that.lots && market == that.market &&
        role == that.role
      case _ => false

  override def hashCode: Int = (instrumentId, lots, market, role).hashCode

  override def toString: String = s"LiquiditySlice($instrumentId,$lots,$market,$role)"
end LiquiditySlice

object LiquiditySlice:
  private def construct[L, M](
    instrumentId: InstrumentId,
    lots: L,
    market: M,
    role: LiquidityRole
  ): LiquiditySlice[L, M] =
    new LiquiditySlice(instrumentId, lots, market, role)

  def create[I <: Instrument](
    instrument: I
  )(
    lots: instrument.Lots,
    market: instrument.MarketState,
    role: LiquidityRole
  ): Either[ScenarioViolations, LiquiditySlice[instrument.Lots, instrument.MarketState]] =
    val expected   = instrument.identity.id
    val violations = Vector(
      Option.when(lots.instrumentId != expected)(
        ScenarioViolation.Identity(
          ScenarioLocation.SliceInput(ScenarioSliceComponent.Lots),
          expected,
          lots.instrumentId
        )
      ),
      Option.when(market.instrumentId != expected)(
        ScenarioViolation.Identity(
          ScenarioLocation.SliceInput(ScenarioSliceComponent.Market),
          expected,
          market.instrumentId
        )
      ),
      Option.when(market.price.instrumentId != expected)(
        ScenarioViolation.Identity(
          ScenarioLocation.SliceInput(ScenarioSliceComponent.Price),
          expected,
          market.price.instrumentId
        )
      )
    ).flatten
    ScenarioViolations.from(violations) match
      case Some(errors) => Left(errors)
      case None         => Right(construct(expected, lots, market, role))
  end create

  def createFirst[I <: Instrument](
    instrument: I
  )(
    lots: instrument.Lots,
    market: instrument.MarketState,
    role: LiquidityRole
  ): Either[ScenarioViolation, LiquiditySlice[instrument.Lots, instrument.MarketState]] =
    create(instrument)(lots, market, role).left.map(_.head)
end LiquiditySlice

/** Domain non-empty collection of matched liquidity. */
final class MatchedSlices[L, M] private (
  val head: LiquiditySlice[L, M],
  val tail: Vector[LiquiditySlice[L, M]]):

  val toVector: Vector[LiquiditySlice[L, M]] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: MatchedSlices[?, ?] => toVector == that.toVector
      case _                         => false

  override def hashCode: Int = toVector.hashCode

  override def toString: String = toVector.mkString("MatchedSlices(", ",", ")")
end MatchedSlices

object MatchedSlices:
  private def construct[L, M](
    head: LiquiditySlice[L, M],
    tail: Vector[LiquiditySlice[L, M]]
  ): MatchedSlices[L, M] =
    new MatchedSlices(head, tail)

  def one[L, M](head: LiquiditySlice[L, M]): MatchedSlices[L, M] =
    construct(head, Vector.empty)

  def of[L, M](
    head: LiquiditySlice[L, M],
    tail: LiquiditySlice[L, M]*
  ): MatchedSlices[L, M] =
    construct(head, tail.toVector)

  def fromVector[L, M](
    values: Vector[LiquiditySlice[L, M]]
  ): Either[ScenarioViolation, MatchedSlices[L, M]] =
    values match
      case head +: tail => Right(construct(head, tail))
      case _            => Left(ScenarioViolation.EmptySlices)
end MatchedSlices

/** Cohesive evidence and non-empty matched liquidity for one stable order value. */
final class ScenarioAssumptions[D <: Dim, B <: Dim, Q <: Dim, M] private (
  val order: Order[D, B, Q]
)(
  val activationEvidence: order.activation.Evidence,
  val pricingResolution: order.execution.Resolution,
  val matchedSlices: MatchedSlices[Lots[D], M])

object ScenarioAssumptions:
  private def construct[D <: Dim, B <: Dim, Q <: Dim, M, O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlices: MatchedSlices[Lots[D], M]
  ): ScenarioAssumptions[D, B, Q, M] =
    new ScenarioAssumptions(order)(activationEvidence, pricingResolution, matchedSlices)

  def create[D <: Dim, B <: Dim, Q <: Dim, M, O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlices: MatchedSlices[Lots[D], M]
  ): ScenarioAssumptions[D, B, Q, M] =
    construct(order)(activationEvidence, pricingResolution, matchedSlices)

  def one[D <: Dim, B <: Dim, Q <: Dim, M, O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlice: LiquiditySlice[Lots[D], M]
  ): ScenarioAssumptions[D, B, Q, M] =
    create(order)(activationEvidence, pricingResolution, MatchedSlices.one(matchedSlice))

  def many[D <: Dim, B <: Dim, Q <: Dim, M, O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    head: LiquiditySlice[Lots[D], M],
    tail: LiquiditySlice[Lots[D], M]*
  ): ScenarioAssumptions[D, B, Q, M] =
    create(order)(activationEvidence, pricingResolution, MatchedSlices.of(head, tail*))

  def fromVector[D <: Dim, B <: Dim, Q <: Dim, M, O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlices: Vector[LiquiditySlice[Lots[D], M]]
  ): Either[ScenarioViolation, ScenarioAssumptions[D, B, Q, M]] =
    MatchedSlices
      .fromVector(matchedSlices)
      .map(create(order)(activationEvidence, pricingResolution, _))
end ScenarioAssumptions

final class OrderScenario[D <: Dim, B <: Dim, Q <: Dim, M] private (
  val assumptions: ScenarioAssumptions[D, B, Q, M],
  val checkedActivation: CheckedActivation[B, Q],
  val effectivePricing: EffectivePricing[B, Q],
  val positionChange: PositionLots[D]):

  val order: Order[D, B, Q]                    = assumptions.order
  val matchedSlices: MatchedSlices[Lots[D], M] = assumptions.matchedSlices
  val instrumentId: InstrumentId               = order.instrumentId

  override def equals(other: Any): Boolean =
    other match
      case that: OrderScenario[?, ?, ?, ?] =>
        assumptions == that.assumptions && checkedActivation == that.checkedActivation &&
        effectivePricing == that.effectivePricing && positionChange == that.positionChange
      case _ => false

  override def hashCode: Int =
    (assumptions, checkedActivation, effectivePricing, positionChange).hashCode
end OrderScenario

private[scenario] final case class LocatedIdentity(
  location: ScenarioLocation,
  supplied: InstrumentId,
  rule: Int,
  index: Int = 0)

private[scenario] sealed trait ValidationBranch[+A]:
  def toEither: Either[ScenarioViolation, A]
  def reported: Option[ScenarioViolation]

private[scenario] object ValidationBranch:
  final case class Skipped(blocking: ScenarioViolation) extends ValidationBranch[Nothing]:
    val toEither: Either[ScenarioViolation, Nothing] = Left(blocking)
    val reported: Option[ScenarioViolation]          = None

  final case class Failed(violation: ScenarioViolation) extends ValidationBranch[Nothing]:
    val toEither: Either[ScenarioViolation, Nothing] = Left(violation)
    val reported: Option[ScenarioViolation]          = Some(violation)

  final case class Passed[A](value: A) extends ValidationBranch[A]:
    val toEither: Either[ScenarioViolation, A] = Right(value)
    val reported: Option[ScenarioViolation]    = None

object OrderScenario:
  private def construct[D <: Dim, B <: Dim, Q <: Dim, M](
    assumptions: ScenarioAssumptions[D, B, Q, M],
    checkedActivation: CheckedActivation[B, Q],
    effectivePricing: EffectivePricing[B, Q],
    positionChange: PositionLots[D]
  ): OrderScenario[D, B, Q, M] =
    new OrderScenario(assumptions, checkedActivation, effectivePricing, positionChange)

  def evaluate[I <: Instrument](
    instrument: I
  )(
    assumptions: ScenarioAssumptions[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): Either[
    ScenarioViolations,
    OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ] =
    val expected                      = instrument.identity.id
    val order: assumptions.order.type = assumptions.order
    val slices                        = assumptions.matchedSlices.toVector

    val orderIdentities = Vector(
      LocatedIdentity(ScenarioLocation.Order, order.instrumentId, 0),
      LocatedIdentity(ScenarioLocation.OrderIntent, order.intent.instrumentId, 1),
      LocatedIdentity(ScenarioLocation.OrderLots, order.intent.lots.instrumentId, 2),
      LocatedIdentity(ScenarioLocation.OrderPositionChange, order.intent.positionChange.instrumentId, 3)
    )
    val triggerIdentities = order.activation match
      case FixedActivation(_, _, triggerPrice) =>
        Vector(LocatedIdentity(ScenarioLocation.TriggerPrice, triggerPrice.instrumentId, 4))
      case _ => Vector.empty
    val (pricingInstructionIdentities, visibilityIdentities) = order.execution match
      case PricedExecution(pricing, _, _, visibility) =>
        val pricingIdentity = pricing match
          case LimitPricing(limit) =>
            Vector(LocatedIdentity(ScenarioLocation.LimitPrice, limit.instrumentId, 5))
          case _: PeggedPricing[?, ?] => Vector.empty
        val visibilityIdentity = visibility match
          case IcebergVisibility(displayedLots) =>
            Vector(LocatedIdentity(ScenarioLocation.DisplayedLots, displayedLots.instrumentId, 6))
          case _ => Vector.empty
        pricingIdentity -> visibilityIdentity
      case _ => Vector.empty -> Vector.empty
    val activationEvidenceIdentities = order.activation
      .observations(assumptions.activationEvidence)
      .map:
        case (ActivationObservation.FavorableExtreme, price) =>
          LocatedIdentity(ScenarioLocation.TrailingExtreme, price.instrumentId, 7)
        case (ActivationObservation.Observed, price) =>
          LocatedIdentity(ScenarioLocation.ActivationObserved, price.instrumentId, 8)
    val pricingEvidenceIdentities = order.execution
      .observations(assumptions.pricingResolution)
      .map:
        case (PricingObservation.ReferencePrice, price) =>
          LocatedIdentity(ScenarioLocation.PegReferencePrice, price.instrumentId, 9)
        case (PricingObservation.ResolvedLimit, price) =>
          LocatedIdentity(ScenarioLocation.PegResolvedLimit, price.instrumentId, 10)
    val sliceIdentity = slices.zipWithIndex.map: (slice, index) =>
      LocatedIdentity(
        ScenarioLocation.Slice(index, ScenarioSliceComponent.Identity),
        slice.instrumentId,
        11,
        index
      )
    val sliceLots = slices.zipWithIndex.map: (slice, index) =>
      LocatedIdentity(
        ScenarioLocation.Slice(index, ScenarioSliceComponent.Lots),
        slice.lots.instrumentId,
        12,
        index
      )
    val sliceMarket = slices.zipWithIndex.map: (slice, index) =>
      LocatedIdentity(
        ScenarioLocation.Slice(index, ScenarioSliceComponent.Market),
        slice.market.instrumentId,
        13,
        index
      )
    val slicePrice = slices.zipWithIndex.map: (slice, index) =>
      LocatedIdentity(
        ScenarioLocation.Slice(index, ScenarioSliceComponent.Price),
        slice.market.price.instrumentId,
        14,
        index
      )

    val allIdentities = orderIdentities ++ triggerIdentities ++ pricingInstructionIdentities ++
      visibilityIdentities ++
      activationEvidenceIdentities ++ pricingEvidenceIdentities ++ sliceIdentity ++ sliceLots ++ sliceMarket ++
      slicePrice
    val identityViolations = allIdentities.flatMap: candidate =>
      Option.when(candidate.supplied != expected)(
        Validation.violation(
          0,
          candidate.rule,
          candidate.index,
          ScenarioViolation.Identity(candidate.location, expected, candidate.supplied)
        )
      )

    val commonOrderIdentities = orderIdentities.take(2)
    val lotIdentities         = commonOrderIdentities ++ orderIdentities.slice(2, 3) ++ sliceIdentity ++ sliceLots
    val activationIdentities  = commonOrderIdentities ++ triggerIdentities ++ activationEvidenceIdentities
    val pricingIdentities     = commonOrderIdentities ++ pricingInstructionIdentities ++ pricingEvidenceIdentities

    def coherent(values: Vector[LocatedIdentity]): Boolean =
      values.forall(_.supplied == expected)

    def branch[A](
      identities: Vector[LocatedIdentity]
    )(
      evaluate: => Either[ScenarioViolation, A]
    ): ValidationBranch[A] =
      identities.find(_.supplied != expected) match
        case Some(blocking) =>
          ValidationBranch.Skipped(
            ScenarioViolation.Identity(blocking.location, expected, blocking.supplied)
          )
        case None =>
          evaluate match
            case Left(violation) => ValidationBranch.Failed(violation)
            case Right(value)    => ValidationBranch.Passed(value)

    val activation = branch(activationIdentities)(
      order.activation.verify(assumptions.activationEvidence).left.map(ScenarioViolation.Activation.apply)
    )
    val pricing = branch(pricingIdentities)(
      order.execution.resolve(assumptions.pricingResolution).left.map(ScenarioViolation.Pricing.apply)
    )

    val semanticViolations = Vector.newBuilder[RankedViolation[ScenarioViolation]]
    if coherent(lotIdentities) then
      val supplied = slices.foldLeft(BigInt(0))((total, slice) => total + slice.lots.count.unrefined)
      if supplied != order.intent.lots.count.unrefined then
        semanticViolations += Validation.violation(
          1,
          0,
          0,
          ScenarioViolation.LotTotal(order.intent.lots.count.unrefined, supplied)
        )
    activation.reported.foreach: violation =>
      semanticViolations += Validation.violation(1, 1, 0, violation)
    pricing.reported.foreach: violation =>
      semanticViolations += Validation.violation(1, 2, 0, violation)

    val effectivePricing = pricing.toEither.toOption

    slices.zipWithIndex.foreach: (slice, index) =>
      val roleIdentities = commonOrderIdentities :+ sliceIdentity(index)
      if coherent(roleIdentities) then
        effectivePricing.foreach:
          case EffectivePricing.Market() if slice.role != LiquidityRole.Taker =>
            semanticViolations += Validation.violation(
              2,
              0,
              index,
              ScenarioViolation.MarketSliceNotTaker(index)
            )
          case _ => ()
        if order.execution.requiresMaker && slice.role != LiquidityRole.Maker then
          semanticViolations += Validation.violation(
            2,
            1,
            index,
            ScenarioViolation.MakerOnlySliceNotMaker(index)
          )

      effectivePricing.foreach:
        case EffectivePricing.Limited(limit) =>
          val qualityIdentities = pricingIdentities ++ Vector(
            sliceIdentity(index),
            sliceMarket(index),
            slicePrice(index)
          )
          if coherent(qualityIdentities) then
            val acceptable = order.intent.side match
              case Side.Buy  => slice.market.price.ticks.unrefined <= limit.ticks.unrefined
              case Side.Sell => slice.market.price.ticks.unrefined >= limit.ticks.unrefined
            if !acceptable then
              semanticViolations += Validation.violation(
                3,
                0,
                index,
                ScenarioViolation.SliceWorseThanLimit(index)
              )
        case EffectivePricing.Market() => ()

    val violations = Validation.ordered(identityViolations ++ semanticViolations.result())
    ScenarioViolations.from(violations) match
      case Some(errors) => Left(errors)
      case None         =>
        (for
          checked   <- activation.toEither
          effective <- pricing.toEither
        yield construct(assumptions, checked, effective, order.intent.positionChange))
          .left
          .map(ScenarioViolations.one)
  end evaluate

  def evaluateFirst[I <: Instrument](
    instrument: I
  )(
    assumptions: ScenarioAssumptions[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): Either[
    ScenarioViolation,
    OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ] =
    evaluate(instrument)(assumptions).left.map(_.head)
end OrderScenario

final class RoundTripScenario[D <: Dim, B <: Dim, Q <: Dim, M] private (
  val instrumentId: InstrumentId,
  val entry: OrderScenario[D, B, Q, M],
  val exit: OrderScenario[D, B, Q, M],
  val heldPosition: PositionLots[D]):

  override def equals(other: Any): Boolean =
    other match
      case that: RoundTripScenario[?, ?, ?, ?] =>
        instrumentId == that.instrumentId && entry == that.entry && exit == that.exit &&
        heldPosition == that.heldPosition
      case _ => false

  override def hashCode: Int = (instrumentId, entry, exit, heldPosition).hashCode
end RoundTripScenario

object RoundTripScenario:
  private def construct[D <: Dim, B <: Dim, Q <: Dim, M](
    instrumentId: InstrumentId,
    entry: OrderScenario[D, B, Q, M],
    exit: OrderScenario[D, B, Q, M],
    heldPosition: PositionLots[D]
  ): RoundTripScenario[D, B, Q, M] =
    new RoundTripScenario(instrumentId, entry, exit, heldPosition)

  def create[I <: Instrument](
    instrument: I
  )(
    entry: OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ],
    exit: OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): Either[
    RoundTripViolation,
    RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ] =
    val expected   = instrument.identity.id
    val identities = Vector(
      (RoundTripComponent.Entry, entry.instrumentId),
      (RoundTripComponent.Exit, exit.instrumentId)
    )
    identities.collectFirst:
      case (component, supplied) if supplied != expected =>
        RoundTripViolation.InstrumentMismatch(component, expected, supplied)
    match
      case Some(violation) => Left(violation)
      case None            =>
        PositionLots
          .combine(instrument)(entry.positionChange, exit.positionChange)
          .left
          .map:
            case PositionInstrumentMismatch("left", mismatchExpected, supplied) =>
              RoundTripViolation.InstrumentMismatch(
                RoundTripComponent.EntryPositionChange,
                mismatchExpected,
                supplied
              )
            case PositionInstrumentMismatch(_, mismatchExpected, supplied) =>
              RoundTripViolation.InstrumentMismatch(
                RoundTripComponent.ExitPositionChange,
                mismatchExpected,
                supplied
              )
          .flatMap: combined =>
            if combined == PositionLots.flat(instrument) then
              Right(construct(expected, entry, exit, entry.positionChange))
            else
              Left(
                RoundTripViolation.PositionNotFlat(
                  entry.positionChange.coordinate,
                  exit.positionChange.coordinate
                )
              )
    end match
  end create
end RoundTripScenario
