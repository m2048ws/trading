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
  // Stable identity, immutable contract metadata, and path-owned public values.
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

  sealed trait Prices extends JavaSerializationUnsupported:
    def exact(coefficient: Rational): Either[EconomicsError, Price]
    def fromRate(value: Rate[base.D, quote.D]): Either[EconomicsError, Price]
    def fromTicks(ticks: PositiveWhole): Price
    def quantize(
      coefficient: Rational,
      policy: QuantizationPolicy
    ): Either[EconomicsError, (Price, Quantity[Divide[quote.D, base.D]])]
    def quantizeRate(
      value: Rate[base.D, quote.D],
      policy: QuantizationPolicy
    ): Either[EconomicsError, (Price, Quantity[Divide[quote.D, base.D]])]
    def ticks(value: Price): BigInt
    def coefficient(value: Price): Rational
    def rate(value: Price): Rate[base.D, quote.D]

  sealed trait SettlementConversions extends JavaSerializationUnsupported:
    def sources: Vector[AssetId]

  sealed trait MarketState extends JavaSerializationUnsupported:
    def price: Price
    def conversions: SettlementConversions

  sealed trait Market extends JavaSerializationUnsupported:
    def quoteSettled(
      price: Price,
      additionalConversions: Vector[SettlementConversion] = Vector.empty
    ): Either[EconomicsError, MarketState]
    def baseSettled(
      price: Price,
      additionalConversions: Vector[SettlementConversion] = Vector.empty
    ): Either[EconomicsError, MarketState]
    def fromQuoteAnchor(
      price: Price,
      quoteToSettle: Rational,
      additionalConversions: Vector[SettlementConversion] = Vector.empty
    ): Either[EconomicsError, MarketState]
    def fromBaseAnchor(
      price: Price,
      baseToSettle: Rational,
      additionalConversions: Vector[SettlementConversion] = Vector.empty
    ): Either[EconomicsError, MarketState]
    def fromAnchors(
      price: Price,
      baseToSettle: Rational,
      quoteToSettle: Rational,
      additionalConversions: Vector[SettlementConversion] = Vector.empty
    ): Either[EconomicsError, MarketState]
    def fromQuoteRate(
      price: Price,
      quoteToSettle: Rate[quote.D, settle.D],
      additionalConversions: Vector[SettlementConversion] = Vector.empty
    ): Either[EconomicsError, MarketState]
    def fromBaseRate(
      price: Price,
      baseToSettle: Rate[base.D, settle.D],
      additionalConversions: Vector[SettlementConversion] = Vector.empty
    ): Either[EconomicsError, MarketState]
    def fromRates(
      price: Price,
      baseToSettle: Rate[base.D, settle.D],
      quoteToSettle: Rate[quote.D, settle.D],
      additionalConversions: Vector[SettlementConversion] = Vector.empty
    ): Either[EconomicsError, MarketState]
    def convertToSettle(
      source: AssetRef,
      conversions: SettlementConversions
    )(
      value: Quantity[source.D]
    ): Either[EconomicsError, Quantity[settle.D]]
  end Market

  sealed trait Visibility extends JavaSerializationUnsupported:
    def kind: VisibilityKind
    def displayedLots: Option[Lots]

  sealed trait Activation extends JavaSerializationUnsupported:
    def kind: ActivationKind
    def reference: Option[PriceReference]
    def comparison: Option[TriggerComparison]
    def triggerPrice: Option[Price]
    def trailingOffsetTicks: Option[BigInt]

  sealed trait PriceInstruction extends JavaSerializationUnsupported:
    def kind: PriceInstructionKind
    def limit: Option[Price]
    def reference: Option[PriceReference]
    def offsetTicks: Option[BigInt]

  sealed trait Order extends JavaSerializationUnsupported:
    def side: Side
    def lots: Lots
    def activation: Activation
    def priceInstruction: PriceInstruction
    def timeInForce: TimeInForce
    def liquidityConstraint: LiquidityConstraint
    def positionEffect: PositionEffect
    def visibility: Visibility

  sealed trait Orders extends JavaSerializationUnsupported:
    def notApplicableVisibility: Visibility
    def displayedVisibility: Visibility
    def hiddenVisibility: Visibility
    def icebergVisibility(displayedLots: Lots): Visibility
    def immediateActivation: Activation
    def fixedTrigger(reference: PriceReference, comparison: TriggerComparison, triggerPrice: Price): Activation
    def trailingTrigger(
      reference: PriceReference,
      comparison: TriggerComparison,
      offsetTicks: BigInt
    ): Either[EconomicsError, Activation]
    def marketPriceInstruction: PriceInstruction
    def limitPriceInstruction(limit: Price): PriceInstruction
    def peggedPriceInstruction(reference: PriceReference, offsetTicks: BigInt): PriceInstruction
    def checked(
      side: Side,
      lots: Lots,
      activation: Activation,
      priceInstruction: PriceInstruction,
      timeInForce: TimeInForce,
      liquidityConstraint: LiquidityConstraint,
      positionEffect: PositionEffect,
      visibility: Visibility
    ): Either[EconomicsError, Order]
    def market(
      side: Side,
      lots: Lots,
      positionEffect: PositionEffect = PositionEffect.Unrestricted
    ): Either[EconomicsError, Order]
    def limit(
      side: Side,
      lots: Lots,
      limit: Price,
      timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
      liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
      positionEffect: PositionEffect = PositionEffect.Unrestricted,
      visibility: Visibility = displayedVisibility
    ): Either[EconomicsError, Order]
    def stopMarket(
      side: Side,
      lots: Lots,
      trigger: Activation,
      positionEffect: PositionEffect = PositionEffect.Unrestricted
    ): Either[EconomicsError, Order]
    def stopLimit(
      side: Side,
      lots: Lots,
      trigger: Activation,
      limit: Price,
      timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
      liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
      positionEffect: PositionEffect = PositionEffect.Unrestricted,
      visibility: Visibility = displayedVisibility
    ): Either[EconomicsError, Order]
  end Orders

  sealed trait ActivationEvidence extends JavaSerializationUnsupported:
    def reference: PriceReference
    def observedPrice: Price
    def favorableExtreme: Option[Price]

  sealed trait PegResolution extends JavaSerializationUnsupported:
    def reference: PriceReference
    def referencePrice: Price
    def resolvedLimit: Price

  sealed trait LiquiditySlice extends JavaSerializationUnsupported:
    def lots: Lots
    def market: MarketState
    def role: LiquidityRole

  sealed trait OrderScenario extends JavaSerializationUnsupported:
    def order: Order
    def activationEvidence: Option[ActivationEvidence]
    def pegResolution: Option[PegResolution]
    def slices: Vector[LiquiditySlice]
    def positionChange: PositionLots

  sealed trait RoundTripScenario extends JavaSerializationUnsupported:
    def entry: OrderScenario
    def exit: OrderScenario
    def heldPosition: PositionLots

  sealed trait Scenarios extends JavaSerializationUnsupported:
    def fixedTriggerEvidence(reference: PriceReference, observedPrice: Price): ActivationEvidence
    def trailingTriggerEvidence(
      reference: PriceReference,
      favorableExtreme: Price,
      activatingObservation: Price
    ): ActivationEvidence
    def pegResolution(reference: PriceReference, referencePrice: Price, resolvedLimit: Price): PegResolution
    def slice(lots: Lots, market: MarketState, role: LiquidityRole): LiquiditySlice
    def order(
      order: Order,
      matchedSlices: Vector[LiquiditySlice],
      activationEvidence: Option[ActivationEvidence] = None,
      pegResolution: Option[PegResolution] = None
    ): Either[EconomicsError, OrderScenario]
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

  sealed trait FeeLine extends JavaSerializationUnsupported:
    def fee: Fee
    def sourceSliceIndex: Int
    def sourceMarket: MarketState

  trait FeeSchedule extends JavaSerializationUnsupported:
    def assess(scenario: OrderScenario): Either[EconomicsError, Vector[FeeLine]]

  sealed trait Fees extends JavaSerializationUnsupported:
    def minimumCharge(
      asset: AssetRef
    )(
      accountContribution: Quantity[asset.D],
      nonnegativeMinimum: Quantity[asset.D]
    ): Either[EconomicsError, Quantity[asset.D]]
    def quantize(
      asset: AssetRef
    )(
      grid: RegisteredGridRef[? <: Dimension],
      kind: FeeKind,
      unrounded: Quantity[asset.D],
      policy: QuantizationPolicy
    ): Either[EconomicsError, Fee]
    def percentage(
      asset: AssetRef
    )(
      grid: RegisteredGridRef[? <: Dimension],
      kind: FeeKind,
      nonnegativeBasis: Quantity[asset.D],
      rate: FeeRate,
      policy: QuantizationPolicy
    ): Either[EconomicsError, Fee]
    def line(scenario: OrderScenario, sourceSliceIndex: Int, fee: Fee): Either[EconomicsError, FeeLine]
    def none: FeeSchedule
    def combine(componentSchedules: Vector[FeeSchedule]): FeeSchedule
  end Fees

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

  sealed trait Valuation extends JavaSerializationUnsupported:
    def settlePerPosition(state: MarketState): Rate[position.D, settle.D]
    def positionValue(value: PositionLots, state: MarketState): Quantity[settle.D]
    def pricePnl(value: PositionLots, entry: MarketState, exit: MarketState): Quantity[settle.D]
    def pnl(roundTrip: RoundTripScenario, feeSchedule: FeeSchedule): Either[EconomicsError, Pnl]

  sealed trait Sizing extends JavaSerializationUnsupported:
    def downsideRisk(pnl: Pnl): Quantity[settle.D]
    def maxLots(
      riskBudget: Quantity[settle.D],
      cap: PositiveWhole,
      feeSchedule: FeeSchedule
    )(
      scenarioFor: Lots => Either[EconomicsError, RoundTripScenario]
    ): Either[EconomicsError, Option[Lots]]

  val prices: Prices
  val market: Market
  val orders: Orders
  val scenarios: Scenarios
  val fees: Fees
  val valuation: Valuation
  val sizing: Sizing

end Instrument

object Instrument {

  // Trusted aggregate construction validates registry, grid, and payoff authority before the private implementation.
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

    // Stable capability wiring. These values only namespace operations for this exact instrument path.
    val prices: Prices = new Prices:
      def exact(coefficient: Rational): Either[EconomicsError, Price] =
        fromRate(Rate(base.dimension.asDimensionRef, quote.dimension.asDimensionRef, coefficient))

      def fromRate(value: Rate[base.D, quote.D]): Either[EconomicsError, Price] = priceExactly(value)

      def fromTicks(ticks: PositiveWhole): Price = priceGrid.fromCoordinate(ticks.unrefined)

      def quantize(
        coefficient: Rational,
        policy: QuantizationPolicy
      ): Either[EconomicsError, (Price, Quantity[Divide[quote.D, base.D]])] =
        quantizeRate(Rate(base.dimension.asDimensionRef, quote.dimension.asDimensionRef, coefficient), policy)

      def quantizeRate(
        value: Rate[base.D, quote.D],
        policy: QuantizationPolicy
      ): Either[EconomicsError, (Price, Quantity[Divide[quote.D, base.D]])] = quantizePrice(value, policy)

      def ticks(value: Price): BigInt = priceCoordinate(value)

      def coefficient(value: Price): Rational = priceRate(value).coefficient

      def rate(value: Price): Rate[base.D, quote.D] = priceRate(value)

    val market: Market = new Market:
      def quoteSettled(
        price: Price,
        additionalConversions: Vector[SettlementConversion]
      ): Either[EconomicsError, MarketState] = marketStateForQuote(price, additionalConversions)

      def baseSettled(
        price: Price,
        additionalConversions: Vector[SettlementConversion]
      ): Either[EconomicsError, MarketState] = marketStateForBase(price, additionalConversions)

      def fromQuoteAnchor(
        price: Price,
        quoteToSettle: Rational,
        additionalConversions: Vector[SettlementConversion]
      ): Either[EconomicsError, MarketState] =
        fromQuoteRate(
          price,
          Rate(quote.dimension.asDimensionRef, settle.dimension.asDimensionRef, quoteToSettle),
          additionalConversions
        )

      def fromBaseAnchor(
        price: Price,
        baseToSettle: Rational,
        additionalConversions: Vector[SettlementConversion]
      ): Either[EconomicsError, MarketState] =
        fromBaseRate(
          price,
          Rate(base.dimension.asDimensionRef, settle.dimension.asDimensionRef, baseToSettle),
          additionalConversions
        )

      def fromAnchors(
        price: Price,
        baseToSettle: Rational,
        quoteToSettle: Rational,
        additionalConversions: Vector[SettlementConversion]
      ): Either[EconomicsError, MarketState] =
        fromRates(
          price,
          Rate(base.dimension.asDimensionRef, settle.dimension.asDimensionRef, baseToSettle),
          Rate(quote.dimension.asDimensionRef, settle.dimension.asDimensionRef, quoteToSettle),
          additionalConversions
        )

      def fromQuoteRate(
        price: Price,
        quoteToSettle: Rate[quote.D, settle.D],
        additionalConversions: Vector[SettlementConversion]
      ): Either[EconomicsError, MarketState] =
        marketStateFromQuote(price, quoteToSettle, additionalConversions)

      def fromBaseRate(
        price: Price,
        baseToSettle: Rate[base.D, settle.D],
        additionalConversions: Vector[SettlementConversion]
      ): Either[EconomicsError, MarketState] =
        marketStateFromBase(price, baseToSettle, additionalConversions)

      def fromRates(
        price: Price,
        baseToSettle: Rate[base.D, settle.D],
        quoteToSettle: Rate[quote.D, settle.D],
        additionalConversions: Vector[SettlementConversion]
      ): Either[EconomicsError, MarketState] =
        marketStateChecked(price, baseToSettle, quoteToSettle, additionalConversions)

      def convertToSettle(
        source: AssetRef,
        conversions: SettlementConversions
      )(
        value: Quantity[source.D]
      ): Either[EconomicsError, Quantity[settle.D]] =
        InstrumentImpl.this.convertToSettle(conversions, source)(value)

    val orders: Orders = new Orders:
      def notApplicableVisibility: Visibility                = InstrumentImpl.this.notApplicableVisibility
      def displayedVisibility: Visibility                    = InstrumentImpl.this.displayedVisibility
      def hiddenVisibility: Visibility                       = InstrumentImpl.this.hiddenVisibility
      def icebergVisibility(displayedLots: Lots): Visibility = InstrumentImpl.this.icebergVisibility(displayedLots)
      def immediateActivation: Activation                    = InstrumentImpl.this.immediateActivation
      def fixedTrigger(reference: PriceReference, comparison: TriggerComparison, triggerPrice: Price): Activation =
        InstrumentImpl.this.fixedTrigger(reference, comparison, triggerPrice)
      def trailingTrigger(
        reference: PriceReference,
        comparison: TriggerComparison,
        offsetTicks: BigInt
      ): Either[EconomicsError, Activation] = InstrumentImpl.this.trailingTrigger(reference, comparison, offsetTicks)
      def marketPriceInstruction: PriceInstruction              = InstrumentImpl.this.marketPriceInstruction
      def limitPriceInstruction(limit: Price): PriceInstruction = InstrumentImpl.this.limitPriceInstruction(limit)
      def peggedPriceInstruction(reference: PriceReference, offsetTicks: BigInt): PriceInstruction =
        InstrumentImpl.this.peggedPriceInstruction(reference, offsetTicks)
      def checked(
        side: Side,
        lots: Lots,
        activation: Activation,
        priceInstruction: PriceInstruction,
        timeInForce: TimeInForce,
        liquidityConstraint: LiquidityConstraint,
        positionEffect: PositionEffect,
        visibility: Visibility
      ): Either[EconomicsError, Order] =
        InstrumentImpl.this.order(
          side,
          lots,
          activation,
          priceInstruction,
          timeInForce,
          liquidityConstraint,
          positionEffect,
          visibility
        )
      def market(side: Side, lots: Lots, positionEffect: PositionEffect): Either[EconomicsError, Order] =
        marketOrder(side, lots, positionEffect)
      def limit(
        side: Side,
        lots: Lots,
        limit: Price,
        timeInForce: TimeInForce,
        liquidityConstraint: LiquidityConstraint,
        positionEffect: PositionEffect,
        visibility: Visibility
      ): Either[EconomicsError, Order] =
        limitOrder(side, lots, limit, timeInForce, liquidityConstraint, positionEffect, visibility)
      def stopMarket(
        side: Side,
        lots: Lots,
        trigger: Activation,
        positionEffect: PositionEffect
      ): Either[EconomicsError, Order] = stopMarketOrder(side, lots, trigger, positionEffect)
      def stopLimit(
        side: Side,
        lots: Lots,
        trigger: Activation,
        limit: Price,
        timeInForce: TimeInForce,
        liquidityConstraint: LiquidityConstraint,
        positionEffect: PositionEffect,
        visibility: Visibility
      ): Either[EconomicsError, Order] =
        stopLimitOrder(side, lots, trigger, limit, timeInForce, liquidityConstraint, positionEffect, visibility)

    val scenarios: Scenarios = new Scenarios:
      def fixedTriggerEvidence(reference: PriceReference, observedPrice: Price): ActivationEvidence =
        InstrumentImpl.this.fixedTriggerEvidence(reference, observedPrice)
      def trailingTriggerEvidence(
        reference: PriceReference,
        favorableExtreme: Price,
        activatingObservation: Price
      ): ActivationEvidence =
        InstrumentImpl.this.trailingTriggerEvidence(reference, favorableExtreme, activatingObservation)
      def pegResolution(reference: PriceReference, referencePrice: Price, resolvedLimit: Price): PegResolution =
        InstrumentImpl.this.pegResolution(reference, referencePrice, resolvedLimit)
      def slice(lots: Lots, market: MarketState, role: LiquidityRole): LiquiditySlice =
        liquiditySlice(lots, market, role)
      def order(
        order: Order,
        matchedSlices: Vector[LiquiditySlice],
        activationEvidence: Option[ActivationEvidence],
        pegResolution: Option[PegResolution]
      ): Either[EconomicsError, OrderScenario] =
        orderScenario(order, matchedSlices, activationEvidence, pegResolution)
      def roundTrip(entry: OrderScenario, exit: OrderScenario): Either[EconomicsError, RoundTripScenario] =
        InstrumentImpl.this.roundTrip(entry, exit)

    val fees: Fees = new Fees:
      def minimumCharge(
        asset: AssetRef
      )(
        accountContribution: Quantity[asset.D],
        nonnegativeMinimum: Quantity[asset.D]
      ): Either[EconomicsError, Quantity[asset.D]] =
        applyMinimumCharge(asset)(accountContribution, nonnegativeMinimum)
      def quantize(
        asset: AssetRef
      )(
        grid: RegisteredGridRef[? <: Dimension],
        kind: FeeKind,
        unrounded: Quantity[asset.D],
        policy: QuantizationPolicy
      ): Either[EconomicsError, Fee] = quantizeFee(asset)(grid, kind, unrounded, policy)
      def percentage(
        asset: AssetRef
      )(
        grid: RegisteredGridRef[? <: Dimension],
        kind: FeeKind,
        nonnegativeBasis: Quantity[asset.D],
        rate: FeeRate,
        policy: QuantizationPolicy
      ): Either[EconomicsError, Fee] = percentageFee(asset)(grid, kind, nonnegativeBasis, rate, policy)
      def line(scenario: OrderScenario, sourceSliceIndex: Int, fee: Fee): Either[EconomicsError, FeeLine] =
        feeLine(scenario, sourceSliceIndex, fee)
      def none: FeeSchedule                                             = noFees
      def combine(componentSchedules: Vector[FeeSchedule]): FeeSchedule = combineFeeSchedules(componentSchedules)

    val valuation: Valuation = new Valuation:
      def settlePerPosition(state: MarketState): Rate[position.D, settle.D]          = settleValuePerPosition(state)
      def positionValue(value: PositionLots, state: MarketState): Quantity[settle.D] =
        InstrumentImpl.this.positionValue(value, state)
      def pricePnl(value: PositionLots, entry: MarketState, exit: MarketState): Quantity[settle.D] =
        InstrumentImpl.this.pricePnl(value, entry, exit)
      def pnl(roundTrip: RoundTripScenario, feeSchedule: FeeSchedule): Either[EconomicsError, Pnl] =
        calculatePnl(roundTrip, feeSchedule)

    val sizing: Sizing = new Sizing:
      def downsideRisk(pnl: Pnl): Quantity[settle.D] = InstrumentImpl.this.downsideRisk(pnl)
      def maxLots(
        riskBudget: Quantity[settle.D],
        cap: PositiveWhole,
        feeSchedule: FeeSchedule
      )(
        scenarioFor: Lots => Either[EconomicsError,
          RoundTripScenario]
      ): Either[EconomicsError, Option[Lots]] = sizePosition(riskBudget, cap, feeSchedule)(scenarioFor)

    // Trusted path-owned construction and witness-backed implementation remain private to this compilation unit.

    def price(coordinate: BigInt): Either[EconomicsError, Price] =
      InstrumentPrices.fromCoordinate(coordinate)(priceGrid.fromCoordinate)

    def priceExactly(value: Rate[base.D, quote.D]): Either[EconomicsError, Price] =
      InstrumentPrices.exact(() => value.narrowExactlyTo(priceGrid.asGridRef))(priceGrid.coordinate)

    def quantizePrice(
      value: Rate[base.D, quote.D],
      policy: QuantizationPolicy
    ): Either[EconomicsError, (Price, Quantity[Divide[quote.D, base.D]])] =
      InstrumentPrices.quantized(policy)(selectedPolicy =>
        val result = value.quantizeTo(priceGrid.asGridRef, selectedPolicy)
        result.value -> result.residual
      )(priceGrid.coordinate)

    def priceObservation(value: Price): InstrumentPrices.Observation =
      InstrumentPrices.observe(value)(priceGrid.coordinate)(selected => priceGrid.asQuantity(selected).coefficient)

    def priceCoordinate(value: Price): BigInt = priceObservation(value).coordinate

    def priceRate(value: Price): Rate[base.D, quote.D] =
      Rate(base.dimension.asDimensionRef, quote.dimension.asDimensionRef, priceObservation(value).coefficient)

    private final class SettlementConversionsImpl(private val values: Vector[InstrumentMarket.ConversionPlan])
      extends SettlementConversions:
      val sources: Vector[AssetId]                            = values.map(_.source.id)
      val byId: Map[AssetId, InstrumentMarket.ConversionPlan] = values.map(value => value.source.id -> value).toMap

    private final class MarketStateImpl(val price: Price, val conversions: SettlementConversions) extends MarketState

    private def marketState(price: Price, plan: InstrumentMarket.StatePlan): MarketState =
      new MarketStateImpl(price, new SettlementConversionsImpl(plan.conversions))

    def marketStateForQuote(
      price: Price,
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, MarketState] =
      InstrumentMarket
        .quoteSettled(base, quote, settle, priceObservation(price).coefficient, additional)
        .map(marketState(price, _))

    def marketStateForBase(
      price: Price,
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, MarketState] =
      val observation = priceObservation(price)
      InstrumentMarket
        .baseSettled(base, quote, settle, observation.coefficient, observation.coordinate, additional)
        .map(marketState(price, _))

    def marketStateFromQuote(
      price: Price,
      quoteToSettle: Rate[quote.D, settle.D],
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, MarketState] =
      InstrumentMarket
        .fromQuote(base, quote, settle, priceObservation(price).coefficient, quoteToSettle.coefficient, additional)
        .map(marketState(price, _))

    def marketStateFromBase(
      price: Price,
      baseToSettle: Rate[base.D, settle.D],
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, MarketState] =
      val observation = priceObservation(price)
      InstrumentMarket
        .fromBase(
          base,
          quote,
          settle,
          observation.coefficient,
          observation.coordinate,
          baseToSettle.coefficient,
          additional
        )
        .map(marketState(price, _))

    def marketStateChecked(
      price: Price,
      baseToSettle: Rate[base.D, settle.D],
      quoteToSettle: Rate[quote.D, settle.D],
      additional: Vector[SettlementConversion]
    ): Either[EconomicsError, MarketState] =
      InstrumentMarket
        .checked(
          base,
          quote,
          settle,
          priceObservation(price).coefficient,
          baseToSettle.coefficient,
          quoteToSettle.coefficient,
          additional
        )
        .map(marketState(price, _))
    end marketStateChecked

    def convertToSettle(
      conversions: SettlementConversions,
      source: AssetRef
    )(
      value: Quantity[source.D]
    ): Either[EconomicsError, Quantity[settle.D]] =
      val stored = conversions.asInstanceOf[SettlementConversionsImpl].byId
      InstrumentMarket
        .lookup(source, stored)
        .map(coefficient => Quantity(settle.dimension.asDimensionRef, value.coefficient * coefficient))

    def settleValuePerPosition(state: MarketState): Rate[position.D, settle.D] =
      val conversions = state.conversions.asInstanceOf[SettlementConversionsImpl]
      val baseRate    = conversions.byId(base.id).coefficient
      val quoteRate   = conversions.byId(quote.id).coefficient
      val coefficient = InstrumentValuation.settlePerPosition(
        basePerPosition.coefficient,
        baseRate,
        quotePerPosition.coefficient,
        quoteRate
      )
      Rate(position.dimension.asDimensionRef, settle.dimension.asDimensionRef, coefficient)

    def positionValue(value: PositionLots, state: MarketState): Quantity[settle.D] =
      Quantity(
        settle.dimension.asDimensionRef,
        InstrumentValuation.positionValue(
          positionQuantity(value).coefficient,
          settleValuePerPosition(state).coefficient
        )
      )

    def pricePnl(value: PositionLots, entry: MarketState, exit: MarketState): Quantity[settle.D] =
      Quantity(
        settle.dimension.asDimensionRef,
        InstrumentValuation.pricePnl(
          positionQuantity(value).coefficient,
          settleValuePerPosition(entry).coefficient,
          settleValuePerPosition(exit).coefficient
        )
      )

    private final class VisibilityImpl(val kind: VisibilityKind, val displayedLots: Option[Lots]) extends Visibility

    private def visibility(plan: InstrumentOrders.VisibilityPlan[Lots]): Visibility =
      new VisibilityImpl(plan.kind, plan.displayedLots)

    val notApplicableVisibility: Visibility = visibility(InstrumentOrders.visibility(VisibilityKind.NotApplicable))
    val displayedVisibility: Visibility     = visibility(InstrumentOrders.visibility(VisibilityKind.Displayed))
    val hiddenVisibility: Visibility        = visibility(InstrumentOrders.visibility(VisibilityKind.Hidden))

    def icebergVisibility(displayedLots: Lots): Visibility =
      visibility(InstrumentOrders.visibility(VisibilityKind.Iceberg, Some(displayedLots)))

    private final class ActivationImpl(
      val kind: ActivationKind,
      val reference: Option[PriceReference],
      val comparison: Option[TriggerComparison],
      val triggerPrice: Option[Price],
      val trailingOffsetTicks: Option[BigInt])
      extends Activation

    val immediateActivation: Activation =
      val plan = InstrumentOrders.immediate[Price]
      new ActivationImpl(plan.kind, plan.reference, plan.comparison, plan.triggerPrice, plan.trailingOffsetTicks)

    def fixedTrigger(
      reference: PriceReference,
      comparison: TriggerComparison,
      triggerPrice: Price
    ): Activation =
      val plan = InstrumentOrders.fixedTrigger(reference, comparison, triggerPrice)
      new ActivationImpl(plan.kind, plan.reference, plan.comparison, plan.triggerPrice, plan.trailingOffsetTicks)

    def trailingTrigger(
      reference: PriceReference,
      comparison: TriggerComparison,
      offsetTicks: BigInt
    ): Either[EconomicsError, Activation] =
      InstrumentOrders.trailingTrigger[Price](reference, comparison, offsetTicks).map: plan =>
        new ActivationImpl(plan.kind, plan.reference, plan.comparison, plan.triggerPrice, plan.trailingOffsetTicks)

    private final class PriceInstructionImpl(
      val kind: PriceInstructionKind,
      val limit: Option[Price],
      val reference: Option[PriceReference],
      val offsetTicks: Option[BigInt])
      extends PriceInstruction

    val marketPriceInstruction: PriceInstruction =
      val plan = InstrumentOrders.marketInstruction[Price]
      new PriceInstructionImpl(plan.kind, plan.limit, plan.reference, plan.offsetTicks)

    def limitPriceInstruction(limit: Price): PriceInstruction =
      val plan = InstrumentOrders.limitInstruction(limit)
      new PriceInstructionImpl(plan.kind, plan.limit, plan.reference, plan.offsetTicks)

    def peggedPriceInstruction(reference: PriceReference, offsetTicks: BigInt): PriceInstruction =
      val plan = InstrumentOrders.peggedInstruction[Price](reference, offsetTicks)
      new PriceInstructionImpl(plan.kind, plan.limit, plan.reference, plan.offsetTicks)

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

    private def constructOrder(
      plan: InstrumentOrders.OrderPlan[Lots, Activation, PriceInstruction, Visibility]
    ): Order =
      new OrderImpl(
        plan.side,
        plan.lots,
        plan.activation,
        plan.priceInstruction,
        plan.timeInForce,
        plan.liquidityConstraint,
        plan.positionEffect,
        plan.visibility
      )

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
      InstrumentOrders
        .checked(
          side,
          lots,
          activation,
          priceInstruction,
          priceInstruction.kind == PriceInstructionKind.Market,
          timeInForce,
          liquidityConstraint,
          positionEffect,
          visibility,
          visibility.kind,
          visibility.displayedLots.map(lotCount),
          lotCount(lots)
        )
        .map(constructOrder)
    end order

    def marketOrder(
      side: Side,
      lots: Lots,
      positionEffect: PositionEffect
    ): Either[EconomicsError, Order] =
      InstrumentOrders
        .market(
          side,
          lots,
          positionEffect,
          immediateActivation,
          marketPriceInstruction,
          notApplicableVisibility,
          lotCount(lots)
        )
        .map(constructOrder)

    def limitOrder(
      side: Side,
      lots: Lots,
      limit: Price,
      timeInForce: TimeInForce,
      liquidityConstraint: LiquidityConstraint,
      positionEffect: PositionEffect,
      visibility: Visibility
    ): Either[EconomicsError, Order] =
      InstrumentOrders
        .limit(
          side,
          lots,
          limitPriceInstruction(limit),
          timeInForce,
          liquidityConstraint,
          positionEffect,
          visibility,
          visibility.kind,
          visibility.displayedLots.map(lotCount),
          lotCount(lots),
          immediateActivation
        )
        .map(constructOrder)

    def stopMarketOrder(
      side: Side,
      lots: Lots,
      trigger: Activation,
      positionEffect: PositionEffect
    ): Either[EconomicsError, Order] =
      InstrumentOrders
        .stopMarket(
          side,
          lots,
          trigger,
          trigger.kind,
          positionEffect,
          marketPriceInstruction,
          notApplicableVisibility,
          lotCount(lots)
        )
        .map(constructOrder)

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
      InstrumentOrders
        .stopLimit(
          side,
          lots,
          trigger,
          trigger.kind,
          limitPriceInstruction(limit),
          timeInForce,
          liquidityConstraint,
          positionEffect,
          visibility,
          visibility.kind,
          visibility.displayedLots.map(lotCount),
          lotCount(lots)
        )
        .map(constructOrder)

    private final class ActivationEvidenceImpl(
      val reference: PriceReference,
      val observedPrice: Price,
      val favorableExtreme: Option[Price])
      extends ActivationEvidence

    def fixedTriggerEvidence(reference: PriceReference, observedPrice: Price): ActivationEvidence =
      val plan = InstrumentScenarios.fixedEvidence(reference, observedPrice)
      new ActivationEvidenceImpl(plan.reference, plan.observedPrice, plan.favorableExtreme)

    def trailingTriggerEvidence(
      reference: PriceReference,
      favorableExtreme: Price,
      activatingObservation: Price
    ): ActivationEvidence =
      val plan = InstrumentScenarios.trailingEvidence(reference, favorableExtreme, activatingObservation)
      new ActivationEvidenceImpl(plan.reference, plan.observedPrice, plan.favorableExtreme)

    private final class PegResolutionImpl(
      val reference: PriceReference,
      val referencePrice: Price,
      val resolvedLimit: Price)
      extends PegResolution

    def pegResolution(reference: PriceReference, referencePrice: Price, resolvedLimit: Price): PegResolution =
      val plan = InstrumentScenarios.pegResolution(reference, referencePrice, resolvedLimit)
      new PegResolutionImpl(plan.reference, plan.referencePrice, plan.resolvedLimit)

    private final class LiquiditySliceImpl(val lots: Lots, val market: MarketState, val role: LiquidityRole)
      extends LiquiditySlice

    def liquiditySlice(lots: Lots, market: MarketState, role: LiquidityRole): LiquiditySlice =
      val plan = InstrumentScenarios.slice(lots, market, role)
      new LiquiditySliceImpl(plan.lots, plan.market, plan.role)

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
      val activation  = order.activation
      val instruction = order.priceInstruction
      val orderView   = InstrumentScenarios.OrderView(
        order.side,
        lotCount(order.lots),
        InstrumentScenarios.ActivationView(
          activation.kind,
          activation.reference,
          activation.comparison,
          activation.triggerPrice.map(priceCoordinate),
          activation.trailingOffsetTicks
        ),
        InstrumentScenarios.InstructionView(
          instruction.kind,
          instruction.limit.map(priceCoordinate),
          instruction.reference,
          instruction.offsetTicks
        ),
        order.liquidityConstraint
      )
      val sliceViews = slices.map: slice =>
        InstrumentScenarios.SliceView(lotCount(slice.lots), priceCoordinate(slice.market.price), slice.role)
      val evidenceView = activationEvidence.map: evidence =>
        InstrumentScenarios.EvidenceView(
          evidence.reference,
          priceCoordinate(evidence.observedPrice),
          evidence.favorableExtreme.map(priceCoordinate)
        )
      val pegView = pegResolution.map: peg =>
        InstrumentScenarios.PegView(
          peg.reference,
          priceCoordinate(peg.referencePrice),
          priceCoordinate(peg.resolvedLimit)
        )

      InstrumentScenarios.order(orderView, sliceViews, evidenceView, pegView).map: positionCoordinate =>
        new OrderScenarioImpl(
          order,
          activationEvidence,
          pegResolution,
          slices,
          positionGrid.fromCoordinate(positionCoordinate)
        )
    end orderScenario

    private final class RoundTripScenarioImpl(
      val entry: OrderScenario,
      val exit: OrderScenario,
      val heldPosition: PositionLots)
      extends RoundTripScenario

    def roundTrip(entry: OrderScenario, exit: OrderScenario): Either[EconomicsError, RoundTripScenario] =
      val entryCount = positionLotCount(entry.positionChange)
      val exitCount  = positionLotCount(exit.positionChange)
      InstrumentScenarios
        .roundTrip(entryCount, exitCount)
        .map(_ => new RoundTripScenarioImpl(entry, exit, entry.positionChange))

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
      InstrumentFees
        .minimumCharge(accountContribution.coefficient, nonnegativeMinimum.coefficient)
        .left
        .map(coefficient => InvalidFeeBasis(asset.id, coefficient))
        .map(coefficient => Quantity(asset.dimension.asDimensionRef, coefficient))

    def quantizeFee(
      asset: AssetRef
    )(
      grid: RegisteredGridRef[? <: Dimension],
      kind: FeeKind,
      unrounded: Quantity[asset.D],
      policy: QuantizationPolicy
    ): Either[EconomicsError, Fee] =
      InstrumentFees
        .validateQuantization(
          asset.id,
          settle.dimension.key,
          asset.dimension.key,
          asset.dimension.sharesRegistryWith(settle.dimension),
          grid.key,
          grid.dimension.key,
          grid.dimension.sharesRegistryWith(asset.dimension)
        )
        .map: _ =>
          val typedGrid = grid.asInstanceOf[RegisteredGridRef[asset.D]]
          val result    = unrounded.quantizeTo(typedGrid.asGridRef, policy)
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

    def percentageFee(
      asset: AssetRef
    )(
      grid: RegisteredGridRef[? <: Dimension],
      kind: FeeKind,
      nonnegativeBasis: Quantity[asset.D],
      rate: FeeRate,
      policy: QuantizationPolicy
    ): Either[EconomicsError, Fee] =
      InstrumentFees
        .percentageContribution(nonnegativeBasis.coefficient, rate.coefficient)
        .left
        .map(coefficient => InvalidFeeBasis(asset.id, coefficient))
        .flatMap: coefficient =>
          val accountContribution = Quantity(asset.dimension.asDimensionRef, coefficient)
          quantizeFee(asset)(grid, kind, accountContribution, policy)

    private final class FeeLineImpl(
      val scenario: OrderScenario,
      val fee: Fee,
      val sourceSliceIndex: Int,
      val sourceMarket: MarketState)
      extends FeeLine

    def feeLine(scenario: OrderScenario, sourceSliceIndex: Int, fee: Fee): Either[EconomicsError, FeeLine] =
      InstrumentFees.validateAttribution(sourceSliceIndex, scenario.slices.size).map: index =>
        new FeeLineImpl(scenario, fee, index, scenario.slices(index).market)

    val noFees: FeeSchedule = new FeeSchedule:
      def assess(scenario: OrderScenario): Either[EconomicsError, Vector[FeeLine]] = Right(Vector.empty)

    def combineFeeSchedules(schedules: Vector[FeeSchedule]): FeeSchedule = new FeeSchedule:
      def assess(scenario: OrderScenario): Either[EconomicsError, Vector[FeeLine]] =
        InstrumentFees.combine(schedules, scenario)((schedule, value) => schedule.assess(value))

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

      InstrumentValuation
        .calculatePnl(roundTrip.entry, roundTrip.exit, exactPricePnl.coefficient)(
          assessAndValidate(feeSchedule, _),
          convertFeeLine,
          _.settleContribution.coefficient
        )
        .map: plan =>
          val pricePnl = Quantity(settle.dimension.asDimensionRef, plan.pricePnl)
          val feePnl   = Quantity(settle.dimension.asDimensionRef, plan.feePnl)
          val netPnl   = Quantity(settle.dimension.asDimensionRef, plan.netPnl)
          new PnlImpl(pricePnl, plan.convertedFeeLines, feePnl, netPnl)

    private def scenarioPricePnl(scenario: OrderScenario): Quantity[settle.D] =
      val slices = scenario.slices.map: slice =>
        val change = positionLots(scenario.order.side, slice.lots)
        positionQuantity(change).coefficient -> settleValuePerPosition(slice.market).coefficient
      Quantity(settle.dimension.asDimensionRef, InstrumentValuation.scenarioPricePnl(slices))

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

    private def convertFeeLine(
      leg: ScenarioLeg,
      line: FeeLine
    ): Either[EconomicsError, ConvertedFeeLine] =
      val fee = line.fee
      convertToSettle(line.sourceMarket.conversions, fee.asset)(fee.amount)
        .left
        .map:
          case MissingConversion(source, _, _) => MissingConversion(source, Some(leg), Some(line.sourceSliceIndex))
          case other                           => other
        .map(contribution => new ConvertedFeeLineImpl(line.fee, leg, line.sourceSliceIndex, contribution))

    def downsideRisk(pnl: Pnl): Quantity[settle.D] =
      Quantity(settle.dimension.asDimensionRef, InstrumentSizing.downsideRisk(pnl.netPnl.coefficient))

    def sizePosition(
      riskBudget: Quantity[settle.D],
      cap: PositiveWhole,
      feeSchedule: FeeSchedule
    )(
      scenario: Lots => Either[EconomicsError, RoundTripScenario]
    ): Either[EconomicsError, Option[Lots]] =
      InstrumentSizing.maxLots(riskBudget.coefficient, cap.unrefined)(
        lots,
        scenario,
        roundTrip => positionLotCount(roundTrip.heldPosition),
        roundTrip => calculatePnl(roundTrip, feeSchedule).map(pnl => downsideRisk(pnl).coefficient)
      )

  }
}
