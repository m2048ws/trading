package trading.scenario

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

object LiquiditySlice:
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
      case None         => Right(LiquiditySlice(expected, lots, market, role))
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
  def one[L, M](head: LiquiditySlice[L, M]): MatchedSlices[L, M] =
    new MatchedSlices(head, Vector.empty)

  def of[L, M](
    head: LiquiditySlice[L, M],
    tail: LiquiditySlice[L, M]*
  ): MatchedSlices[L, M] =
    new MatchedSlices(head, tail.toVector)

  def fromVector[L, M](
    values: Vector[LiquiditySlice[L, M]]
  ): Either[ScenarioViolation, MatchedSlices[L, M]] =
    values match
      case head +: tail => Right(new MatchedSlices(head, tail))
      case _            => Left(ScenarioViolation.EmptySlices)
end MatchedSlices

/** Cohesive evidence and non-empty matched liquidity for one stable order value. */
final class ScenarioAssumptions[D <: Dim, B <: Dim, Q <: Dim, M] private[scenario] (
  val order: Order[D, B, Q]
)(
  val activationEvidence: order.activation.Evidence,
  val pricingResolution: order.execution.Resolution,
  val matchedSlices: MatchedSlices[Lots[D], M])

object ScenarioAssumptions:
  def create[D <: Dim, B <: Dim, Q <: Dim, M, O <: Order[D, B, Q]](
    order: O
  )(
    activationEvidence: order.activation.Evidence,
    pricingResolution: order.execution.Resolution,
    matchedSlices: MatchedSlices[Lots[D], M]
  ): ScenarioAssumptions[D, B, Q, M] =
    new ScenarioAssumptions(order)(activationEvidence, pricingResolution, matchedSlices)

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

final class OrderScenario[D <: Dim, B <: Dim, Q <: Dim, M] private[scenario] (
  val assumptions: ScenarioAssumptions[D, B, Q, M],
  val checkedActivation: CheckedActivation[B, Q],
  val effectivePricing: EffectivePricing[B, Q],
  val positionChange: PositionLots[D]):

  val order: Order[D, B, Q]                    = assumptions.order
  val matchedSlices: MatchedSlices[Lots[D], M] = assumptions.matchedSlices
  val instrumentId: InstrumentId               = order.instrumentId
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
        yield new OrderScenario(assumptions, checked, effective, order.intent.positionChange))
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

final class RoundTripScenario[D <: Dim, B <: Dim, Q <: Dim, M] private[scenario] (
  val instrumentId: InstrumentId,
  val entry: OrderScenario[D, B, Q, M],
  val exit: OrderScenario[D, B, Q, M],
  val heldPosition: PositionLots[D])

object RoundTripScenario:
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
      (RoundTripComponent.EntryPositionChange, entry.positionChange.instrumentId),
      (RoundTripComponent.Exit, exit.instrumentId),
      (RoundTripComponent.ExitPositionChange, exit.positionChange.instrumentId)
    )
    identities.collectFirst:
      case (component, supplied) if supplied != expected =>
        RoundTripViolation.InstrumentMismatch(component, expected, supplied)
    match
      case Some(violation) => Left(violation)
      case None            =>
        val entryChange = entry.positionChange.coordinate
        val exitChange  = exit.positionChange.coordinate
        if entryChange + exitChange != 0 then
          Left(RoundTripViolation.PositionNotFlat(entryChange, exitChange))
        else Right(new RoundTripScenario(expected, entry, exit, entry.positionChange))
  end create
end RoundTripScenario
