package trading.economics

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

/**
 * A validated, generative instrument aggregate.
 *
 * Each stable value owns its lots, positions, prices, orders, scenarios, fees, PnL, and sizing types. Complete
 * scenarios are conditional calculation inputs: they are not executions, fills, reports, or account state.
 */
sealed trait Instrument extends JavaSerializationUnsupported:
  val id: InstrumentId
  val underlying: UnderlyingId
  val base: AssetRef
  val quote: AssetRef
  val position: AssetRef
  val settle: AssetRef

  def positionGridKey: GridKey
  def priceGridKey: GridKey
  def positionLotQuantum: Rational
  def priceQuantum: Rational
  def basePerPosition: Rate[position.D, base.D]
  def quotePerPosition: Rate[position.D, quote.D]

  type Lots
  type PositionLots
  type Price

  def lots(count: BigInt): Either[EconomicsError, Lots]
  def lotCount(value: Lots): BigInt
  def lotsQuantity(value: Lots): Quantity[position.D]
  def positionLots(side: Side, value: Lots): PositionLots
  def positionLotCount(value: PositionLots): BigInt
  def positionQuantity(value: PositionLots): Quantity[position.D]
  def flatPosition: PositionLots

  def price(coordinate: BigInt): Either[EconomicsError, Price]
  def priceExactly(value: Rate[base.D, quote.D]): Either[EconomicsError, Price]
  def quantizePrice(
    value: Rate[base.D, quote.D],
    policy: QuantizationPolicy
  ): Either[EconomicsError, (Price, Quantity[Divide[quote.D, base.D]])]
  def priceCoordinate(value: Price): BigInt
  def priceRate(value: Price): Rate[base.D, quote.D]

  sealed trait SettlementConversions extends JavaSerializationUnsupported:
    def sources: Vector[AssetId]

  sealed trait MarketState extends JavaSerializationUnsupported:
    def price: Price
    def conversions: SettlementConversions

  def marketStateForQuote(
    price: Price,
    additional: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def marketStateForBase(
    price: Price,
    additional: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def marketStateFromQuote(
    price: Price,
    quoteToSettle: Rate[quote.D, settle.D],
    additional: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def marketStateFromBase(
    price: Price,
    baseToSettle: Rate[base.D, settle.D],
    additional: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def marketStateChecked(
    price: Price,
    baseToSettle: Rate[base.D, settle.D],
    quoteToSettle: Rate[quote.D, settle.D],
    additional: Vector[SettlementConversion] = Vector.empty
  ): Either[EconomicsError, MarketState]

  def convertToSettle(
    conversions: SettlementConversions,
    source: AssetRef
  )(
    value: Quantity[source.D]
  ): Either[EconomicsError, Quantity[settle.D]]

  def settleValuePerPosition(state: MarketState): Rate[position.D, settle.D]
  def positionValue(value: PositionLots, state: MarketState): Quantity[settle.D]
  def pricePnl(value: PositionLots, entry: MarketState, exit: MarketState): Quantity[settle.D]

  sealed trait Visibility extends JavaSerializationUnsupported:
    def kind: VisibilityKind
    def displayedLots: Option[Lots]

  def notApplicableVisibility: Visibility
  def displayedVisibility: Visibility
  def hiddenVisibility: Visibility
  def icebergVisibility(displayedLots: Lots): Visibility

  sealed trait Activation extends JavaSerializationUnsupported:
    def kind: ActivationKind
    def reference: Option[PriceReference]
    def comparison: Option[TriggerComparison]
    def triggerPrice: Option[Price]
    def trailingOffsetTicks: Option[BigInt]

  def immediateActivation: Activation
  def fixedTrigger(
    reference: PriceReference,
    comparison: TriggerComparison,
    triggerPrice: Price
  ): Activation
  def trailingTrigger(
    reference: PriceReference,
    comparison: TriggerComparison,
    offsetTicks: BigInt
  ): Either[EconomicsError, Activation]

  sealed trait PriceInstruction extends JavaSerializationUnsupported:
    def kind: PriceInstructionKind
    def limit: Option[Price]
    def reference: Option[PriceReference]
    def offsetTicks: Option[BigInt]

  def marketPriceInstruction: PriceInstruction
  def limitPriceInstruction(limit: Price): PriceInstruction
  def peggedPriceInstruction(reference: PriceReference, offsetTicks: BigInt): PriceInstruction

  sealed trait Order extends JavaSerializationUnsupported:
    def side: Side
    def lots: Lots
    def activation: Activation
    def priceInstruction: PriceInstruction
    def timeInForce: TimeInForce
    def liquidityConstraint: LiquidityConstraint
    def positionEffect: PositionEffect
    def visibility: Visibility

  def order(
    side: Side,
    lots: Lots,
    activation: Activation,
    priceInstruction: PriceInstruction,
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    positionEffect: PositionEffect,
    visibility: Visibility
  ): Either[EconomicsError, Order]

  def marketOrder(
    side: Side,
    lots: Lots,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[EconomicsError, Order]

  def limitOrder(
    side: Side,
    lots: Lots,
    limit: Price,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: Visibility = displayedVisibility
  ): Either[EconomicsError, Order]

  def stopMarketOrder(
    side: Side,
    lots: Lots,
    trigger: Activation,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[EconomicsError, Order]

  def stopLimitOrder(
    side: Side,
    lots: Lots,
    trigger: Activation,
    limit: Price,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: Visibility = displayedVisibility
  ): Either[EconomicsError, Order]

  sealed trait ActivationEvidence extends JavaSerializationUnsupported:
    def reference: PriceReference
    def observedPrice: Price
    def favorableExtreme: Option[Price]

  def fixedTriggerEvidence(reference: PriceReference, observedPrice: Price): ActivationEvidence
  def trailingTriggerEvidence(
    reference: PriceReference,
    favorableExtreme: Price,
    activatingObservation: Price
  ): ActivationEvidence

  sealed trait PegResolution extends JavaSerializationUnsupported:
    def reference: PriceReference
    def referencePrice: Price
    def resolvedLimit: Price

  def pegResolution(reference: PriceReference, referencePrice: Price, resolvedLimit: Price): PegResolution

  sealed trait LiquiditySlice extends JavaSerializationUnsupported:
    def lots: Lots
    def market: MarketState
    def role: LiquidityRole

  def liquiditySlice(lots: Lots, market: MarketState, role: LiquidityRole): LiquiditySlice

  sealed trait OrderScenario extends JavaSerializationUnsupported:
    def order: Order
    def activationEvidence: Option[ActivationEvidence]
    def pegResolution: Option[PegResolution]
    def slices: Vector[LiquiditySlice]
    def positionChange: PositionLots

  def orderScenario(
    order: Order,
    slices: Vector[LiquiditySlice],
    activationEvidence: Option[ActivationEvidence] = None,
    pegResolution: Option[PegResolution] = None
  ): Either[EconomicsError, OrderScenario]

  sealed trait RoundTripScenario extends JavaSerializationUnsupported:
    def entry: OrderScenario
    def exit: OrderScenario
    def heldPosition: PositionLots

  def roundTrip(entry: OrderScenario, exit: OrderScenario): Either[EconomicsError, RoundTripScenario]

  sealed trait Fee extends JavaSerializationUnsupported:
    val asset: AssetRef
    def kind: FeeKind
    def gridKey: GridKey
    def gridQuantum: Rational
    def coordinate: BigInt
    def amount: Quantity[asset.D]
    def residual: Quantity[asset.D]
    def unrounded: Quantity[asset.D]

  def applyMinimumCharge(
    asset: AssetRef
  )(
    accountContribution: Quantity[asset.D],
    nonnegativeMinimum: Quantity[asset.D]
  ): Either[EconomicsError, Quantity[asset.D]]

  def quantizeFee(
    asset: AssetRef
  )(
    grid: RegisteredGridRef[? <: Dimension],
    kind: FeeKind,
    unrounded: Quantity[asset.D],
    policy: QuantizationPolicy
  ): Either[EconomicsError, Fee]

  def percentageFee(
    asset: AssetRef
  )(
    grid: RegisteredGridRef[? <: Dimension],
    kind: FeeKind,
    nonnegativeBasis: Quantity[asset.D],
    rate: FeeRate,
    policy: QuantizationPolicy
  ): Either[EconomicsError, Fee]

  sealed trait FeeLine extends JavaSerializationUnsupported:
    def fee: Fee
    def sourceSliceIndex: Int
    def sourceMarket: MarketState

  def feeLine(scenario: OrderScenario, sourceSliceIndex: Int, fee: Fee): Either[EconomicsError, FeeLine]

  trait FeeSchedule extends JavaSerializationUnsupported:
    def assess(scenario: OrderScenario): Either[EconomicsError, Vector[FeeLine]]

  def noFees: FeeSchedule
  def combineFeeSchedules(schedules: Vector[FeeSchedule]): FeeSchedule

  sealed trait ConvertedFeeLine extends JavaSerializationUnsupported:
    def original: Fee
    def leg: ScenarioLeg
    def sourceSliceIndex: Int
    def settleContribution: Quantity[settle.D]

  sealed trait Pnl extends JavaSerializationUnsupported:
    def pricePnl: Quantity[settle.D]
    def convertedFeeLines: Vector[ConvertedFeeLine]
    def feePnl: Quantity[settle.D]
    def netPnl: Quantity[settle.D]

  def calculatePnl(roundTrip: RoundTripScenario, feeSchedule: FeeSchedule): Either[EconomicsError, Pnl]
  def downsideRisk(pnl: Pnl): Quantity[settle.D]

  def sizePosition(
    riskBudget: Quantity[settle.D],
    cap: PositiveWhole,
    feeSchedule: FeeSchedule
  )(
    scenario: Lots => Either[EconomicsError, RoundTripScenario]
  ): Either[EconomicsError, Option[Lots]]

end Instrument

object Instrument {

  def create(
    id: InstrumentId,
    underlying: UnderlyingId,
    base: AssetRef,
    quote: AssetRef,
    position: AssetRef,
    settle: AssetRef
  )(
    positionGrid: RegisteredGridRef[? <: Dimension],
    priceGrid: RegisteredGridRef[? <: Dimension],
    basePerPosition: Rate[position.D, base.D],
    quotePerPosition: Rate[position.D, quote.D]
  ): Either[EconomicsError, Instrument] =
    val roles = Vector("quote" -> quote, "position" -> position, "settle" -> settle)

    roles
      .collectFirst:
        case (role, candidate) if !base.dimension.sharesRegistryWith(candidate.dimension) =>
          ForeignRegistry(role, base.dimension.key, candidate.dimension.key)
      .map(Left(_))
      .getOrElse:
        if base.id == quote.id then
          Left(ContradictoryInstrument(id, "base and quote assets must be distinct"))
        else if !positionGrid.dimension.sharesRegistryWith(position.dimension) then
          Left(ForeignRegistry("position grid", position.dimension.key, positionGrid.dimension.key))
        else if positionGrid.dimension.key != position.dimension.key then
          Left(
            GridDimensionFailure(
              "position grid",
              positionGrid.key,
              position.dimension.key,
              positionGrid.dimension.key
            )
          )
        else
          val expectedPrice = DimRef.divide(quote.dimension.asDimensionRef, base.dimension.asDimensionRef).key

          if !priceGrid.dimension.sharesRegistryWith(base.dimension) then
            Left(ForeignRegistry("price grid", expectedPrice, priceGrid.dimension.key))
          else if priceGrid.dimension.key != expectedPrice then
            Left(GridDimensionFailure("price grid", priceGrid.key, expectedPrice, priceGrid.dimension.key))
          else if basePerPosition.coefficient.isZero && quotePerPosition.coefficient.isZero then
            Left(EmptyContractPayoff(id))
          else
            Right(
              new InstrumentImpl(
                id,
                underlying,
                base,
                quote,
                position,
                settle
              )(
                positionGrid,
                priceGrid,
                basePerPosition,
                quotePerPosition
              )
            )
          end if
  end create

  private final class InstrumentImpl(
    val id: InstrumentId,
    val underlying: UnderlyingId,
    val base: AssetRef,
    val quote: AssetRef,
    val position: AssetRef,
    val settle: AssetRef
  )(
    positionGridInput: RegisteredGridRef[? <: Dimension],
    priceGridInput: RegisteredGridRef[? <: Dimension],
    val basePerPosition: Rate[position.D, base.D],
    val quotePerPosition: Rate[position.D, quote.D])
    extends Instrument {

    val positionGrid = positionGridInput.asInstanceOf[RegisteredGridRef[position.D]]
    val priceGrid    = priceGridInput.asInstanceOf[RegisteredGridRef[Divide[quote.D, base.D]]]

    val positionGridKey: GridKey     = positionGrid.key
    val priceGridKey: GridKey        = priceGrid.key
    val positionLotQuantum: Rational = positionGrid.quantum.unrefined
    val priceQuantum: Rational       = priceGrid.quantum.unrefined

    type Lots         = GridQuantity[position.D, positionGrid.G]
    type PositionLots = GridQuantity[position.D, positionGrid.G]
    type Price        = GridQuantity[Divide[quote.D, base.D], priceGrid.G]

    def lots(count: BigInt): Either[EconomicsError, Lots] =
      if count.signum <= 0 then Left(InvalidLots(count)) else Right(positionGrid.fromCoordinate(count))

    def lotCount(value: Lots): BigInt = positionGrid.coordinate(value)

    def lotsQuantity(value: Lots): Quantity[position.D] = positionGrid.asQuantity(value)

    def positionLots(side: Side, value: Lots): PositionLots = positionGrid.fromCoordinate(side.sign * lotCount(value))

    def positionLotCount(value: PositionLots): BigInt = positionGrid.coordinate(value)

    def positionQuantity(value: PositionLots): Quantity[position.D] = positionGrid.asQuantity(value)

    val flatPosition: PositionLots = positionGrid.fromCoordinate(0)

    def price(coordinate: BigInt): Either[EconomicsError, Price] =
      if coordinate.signum <= 0 then Left(InvalidPriceCoordinate(coordinate))
      else Right(priceGrid.fromCoordinate(coordinate))

    def priceExactly(value: Rate[base.D, quote.D]): Either[EconomicsError, Price] =
      value
        .narrowExactlyTo(priceGrid.asGridRef)
        .left
        .map(PriceNotOnGrid(_))
        .flatMap: selected =>
          if priceGrid.coordinate(selected).signum <= 0 then
            Left(InvalidPriceCoordinate(priceGrid.coordinate(selected)))
          else Right(selected)

    def quantizePrice(
      value: Rate[base.D, quote.D],
      policy: QuantizationPolicy
    ): Either[EconomicsError, (Price, Quantity[Divide[quote.D, base.D]])] =
      val result = value.quantizeTo(priceGrid.asGridRef, policy)
      if priceGrid.coordinate(result.value).signum <= 0 then
        Left(InvalidPriceCoordinate(priceGrid.coordinate(result.value)))
      else Right(result.value -> result.residual)

    def priceCoordinate(value: Price): BigInt = priceGrid.coordinate(value)

    def priceRate(value: Price): Rate[base.D, quote.D] = priceGrid.asQuantity(value)

    private final case class ConversionData(source: AssetRef, coefficient: Rational)

    private final class SettlementConversionsImpl(private val values: Vector[ConversionData])
      extends SettlementConversions:
      val sources: Vector[AssetId]           = values.map(_.source.id)
      val byId: Map[AssetId, ConversionData] = values.map(value => value.source.id -> value).toMap

    private final class MarketStateImpl(val price: Price, val conversions: SettlementConversions) extends MarketState

    def marketStateForQuote(
      price: Price,
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, MarketState] =
      if settle.id != quote.id then
        Left(InvalidConversion(quote.id, settle.id, Rational.one, "settle asset is not quote"))
      else
        marketStateChecked(
          price,
          Rate(base.dimension.asDimensionRef, settle.dimension.asDimensionRef, priceRate(price).coefficient),
          Rate(quote.dimension.asDimensionRef, settle.dimension.asDimensionRef, Rational.one),
          additional
        )

    def marketStateForBase(
      price: Price,
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, MarketState] =
      if settle.id != base.id then
        Left(InvalidConversion(base.id, settle.id, Rational.one, "settle asset is not base"))
      else
        val reciprocal = Rational.one / priceRate(price).coefficient
        reciprocal match
          case Left(_)            => Left(InvalidPriceCoordinate(priceCoordinate(price)))
          case Right(coefficient) =>
            marketStateChecked(
              price,
              Rate(base.dimension.asDimensionRef, settle.dimension.asDimensionRef, Rational.one),
              Rate(quote.dimension.asDimensionRef, settle.dimension.asDimensionRef, coefficient),
              additional
            )

    def marketStateFromQuote(
      price: Price,
      quoteToSettle: Rate[quote.D, settle.D],
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, MarketState] =
      val baseCoefficient = priceRate(price).coefficient * quoteToSettle.coefficient
      marketStateChecked(
        price,
        Rate(base.dimension.asDimensionRef, settle.dimension.asDimensionRef, baseCoefficient),
        quoteToSettle,
        additional
      )

    def marketStateFromBase(
      price: Price,
      baseToSettle: Rate[base.D, settle.D],
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, MarketState] =
      val quoteCoefficient = baseToSettle.coefficient / priceRate(price).coefficient
      quoteCoefficient match
        case Left(_)            => Left(InvalidPriceCoordinate(priceCoordinate(price)))
        case Right(coefficient) =>
          marketStateChecked(
            price,
            baseToSettle,
            Rate(quote.dimension.asDimensionRef, settle.dimension.asDimensionRef, coefficient),
            additional
          )

    def marketStateChecked(
      price: Price,
      baseToSettle: Rate[base.D, settle.D],
      quoteToSettle: Rate[quote.D, settle.D],
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, MarketState] =
      val expectedBase = priceRate(price).coefficient * quoteToSettle.coefficient

      if baseToSettle.coefficient.signum <= 0 then
        Left(InvalidConversion(base.id, settle.id, baseToSettle.coefficient, "conversion must be positive"))
      else if quoteToSettle.coefficient.signum <= 0 then
        Left(InvalidConversion(quote.id, settle.id, quoteToSettle.coefficient, "conversion must be positive"))
      else if settle.id == base.id && baseToSettle.coefficient != Rational.one then
        Left(
          InvalidConversion(
            base.id,
            settle.id,
            baseToSettle.coefficient,
            "settlement identity conversion must equal one"
          )
        )
      else if settle.id == quote.id && quoteToSettle.coefficient != Rational.one then
        Left(
          InvalidConversion(
            quote.id,
            settle.id,
            quoteToSettle.coefficient,
            "settlement identity conversion must equal one"
          )
        )
      else if expectedBase != baseToSettle.coefficient then
        Left(IncoherentMarketState(priceRate(price).coefficient, baseToSettle.coefficient, quoteToSettle.coefficient))
      else
        buildConversions(baseToSettle.coefficient, quoteToSettle.coefficient, additional)
          .map(conversions => new MarketStateImpl(price, conversions))
      end if
    end marketStateChecked

    private def buildConversions(
      baseToSettle: Rational,
      quoteToSettle: Rational,
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, SettlementConversions] =
      val generated = Vector(
        ConversionData(base, baseToSettle),
        ConversionData(quote, quoteToSettle),
        ConversionData(settle, Rational.one)
      )
      val generatedResult = generated.foldLeft[Either[EconomicsError, Vector[ConversionData]]](Right(Vector.empty)):
        (result, candidate) =>
          result.flatMap: accumulated =>
            accumulated.indexWhere(_.source.id == candidate.source.id) match
              case -1                                                               => Right(accumulated :+ candidate)
              case index if accumulated(index).coefficient == candidate.coefficient => Right(accumulated)
              case index                                                            =>
                Left(
                  InvalidConversion(
                    accumulated(index).source.id,
                    settle.id,
                    accumulated(index).coefficient,
                    "settlement identity conversion must equal one"
                  )
                )

      generatedResult
        .flatMap: initial =>
          additional.foldLeft[Either[EconomicsError, Vector[ConversionData]]](Right(initial)): (result, candidate) =>
            result.flatMap: accumulated =>
              if candidate.target.id != settle.id || candidate.target.dimension.key != settle.dimension.key then
                Left(
                  InvalidConversion(
                    candidate.source.id,
                    candidate.target.id,
                    candidate.coefficient,
                    "conversion target is not settle"
                  )
                )
              else if !candidate.source.dimension.sharesRegistryWith(settle.dimension) then
                Left(ForeignRegistry("additional conversion", settle.dimension.key, candidate.source.dimension.key))
              else if candidate.coefficient.signum <= 0 then
                Left(
                  InvalidConversion(
                    candidate.source.id,
                    candidate.target.id,
                    candidate.coefficient,
                    "conversion must be positive"
                  )
                )
              else if accumulated.exists(_.source.id == candidate.source.id) then
                Left(DuplicateConversion(candidate.source.id))
              else
                Right(accumulated :+ ConversionData(candidate.source, candidate.coefficient))
        .map(values => new SettlementConversionsImpl(values))
    end buildConversions

    def convertToSettle(
      conversions: SettlementConversions,
      source: AssetRef
    )(
      value: Quantity[source.D]
    ): Either[EconomicsError, Quantity[settle.D]] =
      val stored = conversions.asInstanceOf[SettlementConversionsImpl].byId.get(source.id)
      stored match
        case None => Left(MissingConversion(source.id, None, None))
        case Some(conversion)
          if conversion.source.dimension.key != source.dimension.key ||
            !conversion.source.dimension.sharesRegistryWith(source.dimension) =>
          Left(ForeignRegistry("conversion lookup", conversion.source.dimension.key, source.dimension.key))
        case Some(conversion) =>
          Right(Quantity(settle.dimension.asDimensionRef, value.coefficient * conversion.coefficient))

    def settleValuePerPosition(state: MarketState): Rate[position.D, settle.D] =
      val conversions = state.conversions.asInstanceOf[SettlementConversionsImpl]
      val baseRate    = conversions.byId(base.id).coefficient
      val quoteRate   = conversions.byId(quote.id).coefficient
      val coefficient = basePerPosition.coefficient * baseRate + quotePerPosition.coefficient * quoteRate
      Rate(position.dimension.asDimensionRef, settle.dimension.asDimensionRef, coefficient)

    def positionValue(value: PositionLots, state: MarketState): Quantity[settle.D] =
      positionQuantity(value).applyRate(settleValuePerPosition(state))

    def pricePnl(value: PositionLots, entry: MarketState, exit: MarketState): Quantity[settle.D] =
      positionValue(value, exit) - positionValue(value, entry)

    private final class VisibilityImpl(val kind: VisibilityKind, val displayedLots: Option[Lots]) extends Visibility

    val notApplicableVisibility: Visibility = new VisibilityImpl(VisibilityKind.NotApplicable, None)
    val displayedVisibility: Visibility     = new VisibilityImpl(VisibilityKind.Displayed, None)
    val hiddenVisibility: Visibility        = new VisibilityImpl(VisibilityKind.Hidden, None)

    def icebergVisibility(displayedLots: Lots): Visibility =
      new VisibilityImpl(VisibilityKind.Iceberg, Some(displayedLots))

    private final class ActivationImpl(
      val kind: ActivationKind,
      val reference: Option[PriceReference],
      val comparison: Option[TriggerComparison],
      val triggerPrice: Option[Price],
      val trailingOffsetTicks: Option[BigInt])
      extends Activation

    val immediateActivation: Activation =
      new ActivationImpl(ActivationKind.Immediate, None, None, None, None)

    def fixedTrigger(
      reference: PriceReference,
      comparison: TriggerComparison,
      triggerPrice: Price
    ): Activation =
      new ActivationImpl(ActivationKind.FixedTrigger, Some(reference), Some(comparison), Some(triggerPrice), None)

    def trailingTrigger(
      reference: PriceReference,
      comparison: TriggerComparison,
      offsetTicks: BigInt
    ): Either[EconomicsError, Activation] =
      if offsetTicks.signum <= 0 then Left(InvalidTrailingOffset(offsetTicks))
      else
        Right(
          new ActivationImpl(ActivationKind.TrailingTrigger, Some(reference), Some(comparison), None, Some(offsetTicks))
        )

    private final class PriceInstructionImpl(
      val kind: PriceInstructionKind,
      val limit: Option[Price],
      val reference: Option[PriceReference],
      val offsetTicks: Option[BigInt])
      extends PriceInstruction

    val marketPriceInstruction: PriceInstruction =
      new PriceInstructionImpl(PriceInstructionKind.Market, None, None, None)

    def limitPriceInstruction(limit: Price): PriceInstruction =
      new PriceInstructionImpl(PriceInstructionKind.Limit, Some(limit), None, None)

    def peggedPriceInstruction(reference: PriceReference, offsetTicks: BigInt): PriceInstruction =
      new PriceInstructionImpl(PriceInstructionKind.Pegged, None, Some(reference), Some(offsetTicks))

    private final class OrderImpl(
      val side: Side,
      val lots: Lots,
      val activation: Activation,
      val priceInstruction: PriceInstruction,
      val timeInForce: TimeInForce,
      val liquidityConstraint: LiquidityConstraint,
      val positionEffect: PositionEffect,
      val visibility: Visibility)
      extends Order

    def order(
      side: Side,
      lots: Lots,
      activation: Activation,
      priceInstruction: PriceInstruction,
      timeInForce: TimeInForce,
      liquidityConstraint: LiquidityConstraint,
      positionEffect: PositionEffect,
      visibility: Visibility
    ): Either[EconomicsError, Order] =
      val isMarket   = priceInstruction.kind == PriceInstructionKind.Market
      val nonResting = timeInForce == TimeInForce.ImmediateOrCancel || timeInForce == TimeInForce.FillOrKill

      if isMarket && liquidityConstraint == LiquidityConstraint.MakerOnly then
        Left(InvalidOrder("market orders cannot be maker-only"))
      else if isMarket && !nonResting then
        Left(InvalidOrder("market orders require immediate-or-cancel or fill-or-kill"))
      else if isMarket && visibility.kind != VisibilityKind.NotApplicable then
        Left(InvalidOrder("market orders require not-applicable visibility"))
      else if !isMarket && visibility.kind == VisibilityKind.NotApplicable then
        Left(InvalidOrder("priced orders require explicit visibility"))
      else if nonResting && visibility.kind == VisibilityKind.Iceberg then
        Left(InvalidOrder("non-resting orders cannot be iceberg"))
      else
        visibility.displayedLots match
          case Some(displayed) if lotCount(displayed) > lotCount(lots) =>
            Left(InvalidOrder("iceberg displayed lots cannot exceed order lots"))
          case _ =>
            Right(
              new OrderImpl(
                side,
                lots,
                activation,
                priceInstruction,
                timeInForce,
                liquidityConstraint,
                positionEffect,
                visibility
              )
            )
      end if
    end order

    def marketOrder(
      side: Side,
      lots: Lots,
      positionEffect: PositionEffect
    ): Either[EconomicsError, Order] =
      order(
        side,
        lots,
        immediateActivation,
        marketPriceInstruction,
        TimeInForce.ImmediateOrCancel,
        LiquidityConstraint.Unrestricted,
        positionEffect,
        notApplicableVisibility
      )

    def limitOrder(
      side: Side,
      lots: Lots,
      limit: Price,
      timeInForce: TimeInForce,
      liquidityConstraint: LiquidityConstraint,
      positionEffect: PositionEffect,
      visibility: Visibility
    ): Either[EconomicsError, Order] =
      order(
        side,
        lots,
        immediateActivation,
        limitPriceInstruction(limit),
        timeInForce,
        liquidityConstraint,
        positionEffect,
        visibility
      )

    def stopMarketOrder(
      side: Side,
      lots: Lots,
      trigger: Activation,
      positionEffect: PositionEffect
    ): Either[EconomicsError, Order] =
      if trigger.kind == ActivationKind.Immediate then Left(InvalidOrder("stop-market requires a trigger"))
      else
        order(
          side,
          lots,
          trigger,
          marketPriceInstruction,
          TimeInForce.ImmediateOrCancel,
          LiquidityConstraint.Unrestricted,
          positionEffect,
          notApplicableVisibility
        )

    def stopLimitOrder(
      side: Side,
      lots: Lots,
      trigger: Activation,
      limit: Price,
      timeInForce: TimeInForce,
      liquidityConstraint: LiquidityConstraint,
      positionEffect: PositionEffect,
      visibility: Visibility
    ): Either[EconomicsError, Order] =
      if trigger.kind == ActivationKind.Immediate then Left(InvalidOrder("stop-limit requires a trigger"))
      else
        order(
          side,
          lots,
          trigger,
          limitPriceInstruction(limit),
          timeInForce,
          liquidityConstraint,
          positionEffect,
          visibility
        )

    private final class ActivationEvidenceImpl(
      val reference: PriceReference,
      val observedPrice: Price,
      val favorableExtreme: Option[Price])
      extends ActivationEvidence

    def fixedTriggerEvidence(reference: PriceReference, observedPrice: Price): ActivationEvidence =
      new ActivationEvidenceImpl(reference, observedPrice, None)

    def trailingTriggerEvidence(
      reference: PriceReference,
      favorableExtreme: Price,
      activatingObservation: Price
    ): ActivationEvidence =
      new ActivationEvidenceImpl(reference, activatingObservation, Some(favorableExtreme))

    private final class PegResolutionImpl(
      val reference: PriceReference,
      val referencePrice: Price,
      val resolvedLimit: Price)
      extends PegResolution

    def pegResolution(reference: PriceReference, referencePrice: Price, resolvedLimit: Price): PegResolution =
      new PegResolutionImpl(reference, referencePrice, resolvedLimit)

    private final class LiquiditySliceImpl(val lots: Lots, val market: MarketState, val role: LiquidityRole)
      extends LiquiditySlice

    def liquiditySlice(lots: Lots, market: MarketState, role: LiquidityRole): LiquiditySlice =
      new LiquiditySliceImpl(lots, market, role)

    private final class OrderScenarioImpl(
      val order: Order,
      val activationEvidence: Option[ActivationEvidence],
      val pegResolution: Option[PegResolution],
      val slices: Vector[LiquiditySlice],
      val positionChange: PositionLots)
      extends OrderScenario

    def orderScenario(
      order: Order,
      slices: Vector[LiquiditySlice],
      activationEvidence: Option[ActivationEvidence],
      pegResolution: Option[PegResolution]
    ): Either[EconomicsError, OrderScenario] =
      if slices.isEmpty then Left(InvalidScenario("complete scenario requires at least one slice"))
      else if slices.map(slice => lotCount(slice.lots)).sum != lotCount(order.lots) then
        Left(InvalidScenario("slice lots must sum exactly to order lots"))
      else
        validateActivation(order.activation, activationEvidence)
          .flatMap(_ => validatePeg(order.priceInstruction, pegResolution))
          .flatMap: effectiveLimit =>
            validateSlices(order, slices, effectiveLimit).map: _ =>
              new OrderScenarioImpl(
                order,
                activationEvidence,
                pegResolution,
                slices,
                positionLots(order.side, order.lots)
              )

    private def validateActivation(
      activation: Activation,
      evidence: Option[ActivationEvidence]
    ): Either[EconomicsError, Unit] =
      activation.kind match
        case ActivationKind.Immediate =>
          if evidence.isEmpty then Right(())
          else Left(InvalidScenario("immediate activation must not carry trigger evidence"))
        case ActivationKind.FixedTrigger =>
          evidence match
            case None => Left(InvalidScenario("fixed trigger requires activation evidence"))
            case Some(value) if value.favorableExtreme.nonEmpty =>
              Left(InvalidScenario("fixed trigger evidence cannot contain a favorable extremum"))
            case Some(value) =>
              val expectedReference = activation.reference.get
              val trigger           = priceCoordinate(activation.triggerPrice.get)
              val observed          = priceCoordinate(value.observedPrice)
              if value.reference != expectedReference then Left(InvalidScenario("trigger reference does not match"))
              else if comparisonSatisfied(activation.comparison.get, observed, trigger) then Right(())
              else Left(InvalidScenario("fixed trigger observation does not satisfy comparison"))
        case ActivationKind.TrailingTrigger =>
          evidence match
            case None        => Left(InvalidScenario("trailing trigger requires activation evidence"))
            case Some(value) =>
              value.favorableExtreme match
                case None          => Left(InvalidScenario("trailing trigger requires a favorable extremum"))
                case Some(extreme) =>
                  val expectedReference = activation.reference.get
                  val offset            = activation.trailingOffsetTicks.get
                  val threshold         = activation.comparison.get match
                    case TriggerComparison.AtOrAbove => priceCoordinate(extreme) + offset
                    case TriggerComparison.AtOrBelow => priceCoordinate(extreme) - offset
                  if value.reference != expectedReference then Left(InvalidScenario("trigger reference does not match"))
                  else if threshold.signum <= 0 then Left(InvalidScenario("trailing threshold is not a positive price"))
                  else if comparisonSatisfied(
                      activation.comparison.get,
                      priceCoordinate(value.observedPrice),
                      threshold
                    )
                  then Right(())
                  else Left(InvalidScenario("trailing observation does not satisfy derived threshold"))

    private def validatePeg(
      instruction: PriceInstruction,
      evidence: Option[PegResolution]
    ): Either[EconomicsError, Option[Price]] =
      instruction.kind match
        case PriceInstructionKind.Market =>
          if evidence.isEmpty then Right(None)
          else Left(InvalidScenario("market instruction must not carry peg evidence"))
        case PriceInstructionKind.Limit =>
          if evidence.isEmpty then Right(instruction.limit)
          else Left(InvalidScenario("fixed limit must not carry peg evidence"))
        case PriceInstructionKind.Pegged =>
          evidence match
            case None        => Left(InvalidScenario("pegged instruction requires resolution evidence"))
            case Some(value) =>
              val difference = priceCoordinate(value.resolvedLimit) - priceCoordinate(value.referencePrice)
              if value.reference != instruction.reference.get then Left(InvalidScenario("peg reference does not match"))
              else if difference != instruction.offsetTicks.get then
                Left(InvalidScenario("resolved peg tick offset disagrees"))
              else Right(Some(value.resolvedLimit))

    private def validateSlices(
      order: Order,
      slices: Vector[LiquiditySlice],
      effectiveLimit: Option[Price]
    ): Either[EconomicsError, Unit] =
      slices.zipWithIndex.collectFirst:
        case (slice, index)
          if order.priceInstruction.kind == PriceInstructionKind.Market && slice.role != LiquidityRole.Taker =>
          InvalidScenario("market slices must be taker", Some(index))
        case (slice, index)
          if order.liquidityConstraint == LiquidityConstraint.MakerOnly && slice.role != LiquidityRole.Maker =>
          InvalidScenario("maker-only slices must be maker", Some(index))
        case (slice, index)
          if effectiveLimit.exists: limit =>
            order.side match
              case Side.Buy  => priceCoordinate(slice.market.price) > priceCoordinate(limit)
              case Side.Sell => priceCoordinate(slice.market.price) < priceCoordinate(limit)
          =>
          InvalidScenario("slice price is worse than the effective limit", Some(index))
      match
        case Some(error) => Left(error)
        case None        => Right(())

    private def comparisonSatisfied(comparison: TriggerComparison, observed: BigInt, threshold: BigInt): Boolean =
      comparison match
        case TriggerComparison.AtOrAbove => observed >= threshold
        case TriggerComparison.AtOrBelow => observed <= threshold

    private final class RoundTripScenarioImpl(
      val entry: OrderScenario,
      val exit: OrderScenario,
      val heldPosition: PositionLots)
      extends RoundTripScenario

    def roundTrip(entry: OrderScenario, exit: OrderScenario): Either[EconomicsError, RoundTripScenario] =
      val entryCount = positionLotCount(entry.positionChange)
      val exitCount  = positionLotCount(exit.positionChange)
      if entryCount + exitCount != 0 then Left(InvalidRoundTrip(entryCount, exitCount))
      else Right(new RoundTripScenarioImpl(entry, exit, entry.positionChange))

    private final class FeeImpl(
      val asset: AssetRef,
      val kind: FeeKind,
      val gridKey: GridKey,
      val gridQuantum: Rational,
      val coordinate: BigInt
    )(
      val amount: Quantity[asset.D],
      val residual: Quantity[asset.D],
      val unrounded: Quantity[asset.D])
      extends Fee

    def applyMinimumCharge(
      asset: AssetRef
    )(
      accountContribution: Quantity[asset.D],
      nonnegativeMinimum: Quantity[asset.D]
    ): Either[EconomicsError, Quantity[asset.D]] =
      if nonnegativeMinimum.coefficient.signum < 0 then
        Left(InvalidFeeBasis(asset.id, nonnegativeMinimum.coefficient))
      else if accountContribution.coefficient.signum < 0 &&
        accountContribution.coefficient.abs.compare(nonnegativeMinimum.coefficient) < 0
      then
        Right(Quantity(asset.dimension.asDimensionRef, -nonnegativeMinimum.coefficient))
      else
        Right(accountContribution)

    def quantizeFee(
      asset: AssetRef
    )(
      grid: RegisteredGridRef[? <: Dimension],
      kind: FeeKind,
      unrounded: Quantity[asset.D],
      policy: QuantizationPolicy
    ): Either[EconomicsError, Fee] =
      if !asset.dimension.sharesRegistryWith(settle.dimension) then
        Left(ForeignRegistry("fee asset", settle.dimension.key, asset.dimension.key))
      else if !grid.dimension.sharesRegistryWith(asset.dimension) then
        Left(ForeignRegistry("fee grid", asset.dimension.key, grid.dimension.key))
      else if grid.dimension.key != asset.dimension.key then
        Left(InvalidFeeGrid(asset.id, grid.key, asset.dimension.key, grid.dimension.key))
      else
        val typedGrid = grid.asInstanceOf[RegisteredGridRef[asset.D]]
        val result    = unrounded.quantizeTo(typedGrid.asGridRef, policy)
        Right(
          new FeeImpl(
            asset,
            kind,
            typedGrid.key,
            typedGrid.quantum.unrefined,
            typedGrid.coordinate(result.value)
          )(
            typedGrid.asQuantity(result.value),
            result.residual,
            unrounded
          )
        )

    def percentageFee(
      asset: AssetRef
    )(
      grid: RegisteredGridRef[? <: Dimension],
      kind: FeeKind,
      nonnegativeBasis: Quantity[asset.D],
      rate: FeeRate,
      policy: QuantizationPolicy
    ): Either[EconomicsError, Fee] =
      if nonnegativeBasis.coefficient.signum < 0 then Left(InvalidFeeBasis(asset.id, nonnegativeBasis.coefficient))
      else
        val accountContribution = Quantity(
          asset.dimension.asDimensionRef,
          nonnegativeBasis.coefficient * -rate.coefficient
        )
        quantizeFee(asset)(grid, kind, accountContribution, policy)

    private final class FeeLineImpl(
      val scenario: OrderScenario,
      val fee: Fee,
      val sourceSliceIndex: Int,
      val sourceMarket: MarketState)
      extends FeeLine

    def feeLine(scenario: OrderScenario, sourceSliceIndex: Int, fee: Fee): Either[EconomicsError, FeeLine] =
      scenario.slices.lift(sourceSliceIndex) match
        case None         => Left(InvalidFeeAttribution(sourceSliceIndex, scenario.slices.size))
        case Some(source) => Right(new FeeLineImpl(scenario, fee, sourceSliceIndex, source.market))

    val noFees: FeeSchedule = new FeeSchedule:
      def assess(scenario: OrderScenario): Either[EconomicsError, Vector[FeeLine]] = Right(Vector.empty)

    def combineFeeSchedules(schedules: Vector[FeeSchedule]): FeeSchedule = new FeeSchedule:
      def assess(scenario: OrderScenario): Either[EconomicsError, Vector[FeeLine]] =
        schedules.foldLeft[Either[EconomicsError, Vector[FeeLine]]](Right(Vector.empty)): (result, schedule) =>
          for
            accumulated <- result
            next        <- schedule.assess(scenario)
          yield accumulated ++ next

    private final class ConvertedFeeLineImpl(
      val original: Fee,
      val leg: ScenarioLeg,
      val sourceSliceIndex: Int,
      val settleContribution: Quantity[settle.D])
      extends ConvertedFeeLine

    private final class PnlImpl(
      val pricePnl: Quantity[settle.D],
      val convertedFeeLines: Vector[ConvertedFeeLine],
      val feePnl: Quantity[settle.D],
      val netPnl: Quantity[settle.D])
      extends Pnl

    def calculatePnl(roundTrip: RoundTripScenario, feeSchedule: FeeSchedule): Either[EconomicsError, Pnl] =
      val exactPricePnl = scenarioPricePnl(roundTrip.entry) + scenarioPricePnl(roundTrip.exit)

      for
        entryLines     <- assessAndValidate(feeSchedule, roundTrip.entry)
        exitLines      <- assessAndValidate(feeSchedule, roundTrip.exit)
        convertedEntry <- convertFeeLines(ScenarioLeg.Entry, entryLines)
        convertedExit  <- convertFeeLines(ScenarioLeg.Exit, exitLines)
      yield
        val converted = convertedEntry ++ convertedExit
        val feeTotal  = converted.foldLeft(Quantity.zero[settle.D](using settle.dimension.asDimensionRef)):
          (total, line) => total + line.settleContribution
        new PnlImpl(exactPricePnl, converted, feeTotal, exactPricePnl + feeTotal)

    private def scenarioPricePnl(scenario: OrderScenario): Quantity[settle.D] =
      scenario.slices.foldLeft(Quantity.zero[settle.D](using settle.dimension.asDimensionRef)): (total, slice) =>
        val change = positionLots(scenario.order.side, slice.lots)
        total - positionValue(change, slice.market)

    private def assessAndValidate(
      schedule: FeeSchedule,
      scenario: OrderScenario
    ): Either[EconomicsError, Vector[FeeLine]] =
      schedule.assess(scenario).flatMap: lines =>
        lines.collectFirst:
          case line
            if !line.asInstanceOf[FeeLineImpl].scenario.asInstanceOf[AnyRef].eq(scenario.asInstanceOf[AnyRef]) =>
            FeeScheduleFailure("fee line belongs to a different order scenario")
        match
          case Some(error) => Left(error)
          case None        => Right(lines)

    private def convertFeeLines(
      leg: ScenarioLeg,
      lines: Vector[FeeLine]
    ): Either[EconomicsError, Vector[ConvertedFeeLine]] =
      lines.foldLeft[Either[EconomicsError, Vector[ConvertedFeeLine]]](Right(Vector.empty)): (result, line) =>
        result.flatMap: accumulated =>
          val fee = line.fee
          convertToSettle(line.sourceMarket.conversions, fee.asset)(fee.amount)
            .left
            .map:
              case MissingConversion(source, _, _) => MissingConversion(source, Some(leg), Some(line.sourceSliceIndex))
              case other                           => other
            .map: contribution =>
              accumulated :+ new ConvertedFeeLineImpl(line.fee, leg, line.sourceSliceIndex, contribution)

    def downsideRisk(pnl: Pnl): Quantity[settle.D] =
      val coefficient = pnl.netPnl.coefficient
      Quantity(settle.dimension.asDimensionRef, if coefficient.signum < 0 then -coefficient else Rational.zero)

    def sizePosition(
      riskBudget: Quantity[settle.D],
      cap: PositiveWhole,
      feeSchedule: FeeSchedule
    )(
      scenario: Lots => Either[EconomicsError, RoundTripScenario]
    ): Either[EconomicsError, Option[Lots]] =
      if riskBudget.coefficient.signum < 0 then Left(InvalidRiskBudget(riskBudget.coefficient))
      else
        var candidate = BigInt(1)
        var selected  = Option.empty[Lots]

        while candidate <= cap.unrefined do
          val evaluated =
            for
              candidateLots   <- lots(candidate)
              roundTrip       <- scenario(candidateLots)
              heldPositionLots = positionLotCount(roundTrip.heldPosition)
              _               <-
                if heldPositionLots.abs == lotCount(candidateLots) then Right(())
                else Left(SizingScenarioMismatch(lotCount(candidateLots), heldPositionLots))
              pnl <- calculatePnl(roundTrip, feeSchedule)
            yield candidateLots -> downsideRisk(pnl)

          evaluated match
            case Left(error)                  => return Left(error)
            case Right((candidateLots, risk)) =>
              if risk.coefficient.compare(riskBudget.coefficient) <= 0 then selected = Some(candidateLots)
          candidate += 1

        Right(selected)

  }
}
