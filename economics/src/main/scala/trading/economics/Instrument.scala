package trading.economics

import trading.quantity.*
import trading.quantity.grid.*
import trading.quantity.refinement.*
import trading.quantity.runtime.*

/** Strictly positive lots owned by one generative instrument. */
sealed abstract class InstrumentLots[O, D <: Dimension] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def count: PositiveWhole
  def quantity: Quantity[D]

/** Signed position lots owned by one generative instrument. */
sealed abstract class InstrumentPosition[O, D <: Dimension] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def count: BigInt
  def quantity: Quantity[D]

/** Strictly positive grid price owned by one generative instrument. */
sealed abstract class InstrumentPrice[O, B <: Dimension, Q <: Dimension] private[economics] (
  authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def ticks: PositiveWhole
  def coefficient: Rational
  def rate: Rate[B, Q]

sealed abstract class InstrumentSettlementConversion[O, S <: Dimension] private[economics] (
  authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  val source: AssetRef
  val target: AssetRef { type D = S }
  def coefficient: Rational

sealed abstract class InstrumentMarketState[O, B <: Dimension, Q <: Dimension, S <: Dimension] private[economics] (
  authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def price: InstrumentPrice[O, B, Q]
  def conversionSources: Vector[AssetId]
  def baseToSettle: Rate[B, S]
  def quoteToSettle: Rate[Q, S]
  def convertToSettle(source: AssetRef)(value: Quantity[source.D]): Either[EconomicsError, Quantity[S]]

sealed abstract class OrderActivation[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
sealed abstract class ImmediateActivation[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends OrderActivation[O, P](authority)
sealed abstract class FixedActivation[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends OrderActivation[O, P](authority):
  def reference: PriceReference
  def comparison: TriggerComparison
  def triggerPrice: P
sealed abstract class TrailingActivation[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends OrderActivation[O, P](authority):
  def reference: PriceReference
  def comparison: TriggerComparison
  def offsetTicks: PositiveWhole

sealed abstract class OrderPricing[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
sealed abstract class LimitPricing[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends OrderPricing[O, P](authority):
  def limit: P
sealed abstract class PeggedPricing[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends OrderPricing[O, P](authority):
  def reference: PriceReference
  def offsetTicks: BigInt

sealed abstract class PricedVisibility[O, L] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
sealed abstract class DisplayedVisibility[O, L] private[economics] (authority: JvmOwnerAuthority)
  extends PricedVisibility[O, L](authority)
sealed abstract class HiddenVisibility[O, L] private[economics] (authority: JvmOwnerAuthority)
  extends PricedVisibility[O, L](authority)
sealed abstract class IcebergVisibility[O, L] private[economics] (authority: JvmOwnerAuthority)
  extends PricedVisibility[O, L](authority):
  def displayedLots: L

sealed abstract class OrderExecution[O, L, P] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
sealed abstract class MarketExecution[O, L, P] private[economics] (authority: JvmOwnerAuthority)
  extends OrderExecution[O, L, P](authority):
  def timeInForce: NonRestingTimeInForce
sealed abstract class PricedExecution[O, L, P] private[economics] (authority: JvmOwnerAuthority)
  extends OrderExecution[O, L, P](authority):
  def pricing: OrderPricing[O, P]
  def timeInForce: TimeInForce
  def liquidityConstraint: LiquidityConstraint
  def visibility: PricedVisibility[O, L]

sealed abstract class OrderIntent[O, L] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def side: Side
  def lots: L
  def positionEffect: PositionEffect

sealed abstract class InstrumentOrder[O, L, P] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def intent: OrderIntent[O, L]
  def activation: OrderActivation[O, P]
  def execution: OrderExecution[O, L, P]

sealed abstract class TriggerEvidence[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def reference: PriceReference
  def observedPrice: P
sealed abstract class FixedTriggerEvidence[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends TriggerEvidence[O, P](authority)
sealed abstract class TrailingTriggerEvidence[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends TriggerEvidence[O, P](authority):
  def favorableExtreme: P

sealed abstract class ActivationAssumption[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
sealed abstract class ImmediateAssumption[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends ActivationAssumption[O, P](authority)
sealed abstract class TriggeredAssumption[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends ActivationAssumption[O, P](authority):
  def evidence: TriggerEvidence[O, P]

sealed abstract class PegResolution[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def reference: PriceReference
  def referencePrice: P
  def resolvedLimit: P

sealed abstract class PricingAssumption[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
sealed abstract class DirectPricingAssumption[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends PricingAssumption[O, P](authority)
sealed abstract class ResolvedPegAssumption[O, P] private[economics] (authority: JvmOwnerAuthority)
  extends PricingAssumption[O, P](authority):
  def resolution: PegResolution[O, P]

sealed abstract class InstrumentLiquiditySlice[O, L, M] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def lots: L
  def market: M
  def role: LiquidityRole

sealed abstract class ScenarioAssumptions[O, L, P, M] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def activation: ActivationAssumption[O, P]
  def pricing: PricingAssumption[O, P]
  def matchedSlices: Vector[InstrumentLiquiditySlice[O, L, M]]

sealed abstract class InstrumentOrderScenario[O, L, P, M, Pos] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def order: InstrumentOrder[O, L, P]
  def assumptions: ScenarioAssumptions[O, L, P, M]
  def positionChange: Pos

sealed abstract class InstrumentRoundTripScenario[O, L, P, M, Pos] private[economics] (
  authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def entry: InstrumentOrderScenario[O, L, P, M, Pos]
  def exit: InstrumentOrderScenario[O, L, P, M, Pos]
  def heldPosition: Pos

sealed abstract class InstrumentFeeDenomination[O] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  type D <: Dimension
  val asset: AssetRef { type D = InstrumentFeeDenomination.this.D }
  def gridKey: GridKey
  def gridQuantum: Rational
  def policy: QuantizationPolicy
  def minimumCharge(
    accountContribution: Quantity[D],
    nonnegativeMinimum: Quantity[D]
  ): Either[EconomicsError, Quantity[D]]
  def quantize(kind: FeeKind, unrounded: Quantity[D]): InstrumentFee[O]
  def percentage(
    kind: FeeKind,
    nonnegativeBasis: Quantity[D],
    rate: FeeRate
  ): Either[EconomicsError, InstrumentFee[O]]

sealed abstract class InstrumentFee[O] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  val denomination: InstrumentFeeDenomination[O]
  val asset: AssetRef
  def kind: FeeKind
  def coordinate: BigInt
  def amount: Quantity[asset.D]
  def residual: Quantity[asset.D]
  def unrounded: Quantity[asset.D]

sealed abstract class InstrumentFeeLine[O, M] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def fee: InstrumentFee[O]
  def sourceSliceIndex: Int
  def sourceMarket: M

trait InstrumentFeeSchedule[O, L, P, M, Pos] extends JavaSerializationUnsupported:
  def assess(
    scenario: InstrumentOrderScenario[O, L, P, M, Pos]
  ): Either[EconomicsError, Vector[InstrumentFeeLine[O, M]]]

sealed abstract class InstrumentConvertedFeeLine[O, S <: Dimension] private[economics] (
  authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def original: InstrumentFee[O]
  def leg: ScenarioLeg
  def sourceSliceIndex: Int
  def settleContribution: Quantity[S]

sealed abstract class InstrumentPnl[O, S <: Dimension] private[economics] (authority: JvmOwnerAuthority)
  extends JavaSerializationUnsupported:
  authority.assertIssued()
  def pricePnl: Quantity[S]
  def convertedFeeLines: Vector[InstrumentConvertedFeeLine[O, S]]
  def feePnl: Quantity[S]
  def netPnl: Quantity[S]

trait PriceCapability[O, B <: Dimension, Q <: Dimension]:
  def exact(coefficient: Rational): Either[EconomicsError, InstrumentPrice[O, B, Q]]
  def fromRate(value: Rate[B, Q]): Either[EconomicsError, InstrumentPrice[O, B, Q]]
  def fromTicks(ticks: PositiveWhole): InstrumentPrice[O, B, Q]
  def quantize(
    coefficient: Rational,
    policy: QuantizationPolicy
  ): Either[EconomicsError, (InstrumentPrice[O, B, Q], Quantity[Divide[Q, B]])]
  def quantizeRate(
    value: Rate[B, Q],
    policy: QuantizationPolicy
  ): Either[EconomicsError, (InstrumentPrice[O, B, Q], Quantity[Divide[Q, B]])]

trait MarketCapability[O, B <: Dimension, Q <: Dimension, S <: Dimension]:
  def conversion(source: AssetRef, coefficient: Rational): Either[EconomicsError, InstrumentSettlementConversion[O, S]]
  def conversionFromRate(
    source: AssetRef
  )(
    rate: Rate[source.D, S]
  ): Either[EconomicsError, InstrumentSettlementConversion[O, S]]
  def quoteSettled(
    price: InstrumentPrice[O, B, Q],
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]] = Vector.empty
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]]
  def baseSettled(
    price: InstrumentPrice[O, B, Q],
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]] = Vector.empty
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]]
  def fromQuoteAnchor(
    price: InstrumentPrice[O, B, Q],
    quoteToSettle: Rational,
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]] = Vector.empty
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]]
  def fromBaseAnchor(
    price: InstrumentPrice[O, B, Q],
    baseToSettle: Rational,
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]] = Vector.empty
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]]
  def fromAnchors(
    price: InstrumentPrice[O, B, Q],
    baseToSettle: Rational,
    quoteToSettle: Rational,
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]] = Vector.empty
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]]
  def fromQuoteRate(
    price: InstrumentPrice[O, B, Q],
    quoteToSettle: Rate[Q, S],
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]] = Vector.empty
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]]
  def fromBaseRate(
    price: InstrumentPrice[O, B, Q],
    baseToSettle: Rate[B, S],
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]] = Vector.empty
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]]
  def fromRates(
    price: InstrumentPrice[O, B, Q],
    baseToSettle: Rate[B, S],
    quoteToSettle: Rate[Q, S],
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]] = Vector.empty
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]]
end MarketCapability

trait OrderCapability[O, L, P]:
  def immediate: ImmediateActivation[O, P]
  def fixedTrigger(
    reference: PriceReference,
    comparison: TriggerComparison,
    triggerPrice: P
  ): FixedActivation[O, P]
  def trailingTrigger(
    reference: PriceReference,
    comparison: TriggerComparison,
    offsetTicks: BigInt
  ): Either[EconomicsError, TrailingActivation[O, P]]
  def limitPricing(limit: P): LimitPricing[O, P]
  def peggedPricing(reference: PriceReference, offsetTicks: BigInt): PeggedPricing[O, P]
  def displayed: DisplayedVisibility[O, L]
  def hidden: HiddenVisibility[O, L]
  def iceberg(displayedLots: L): IcebergVisibility[O, L]
  def marketExecution(timeInForce: NonRestingTimeInForce): MarketExecution[O, L, P]
  def pricedExecution(
    pricing: OrderPricing[O, P],
    timeInForce: TimeInForce,
    liquidityConstraint: LiquidityConstraint,
    visibility: PricedVisibility[O, L]
  ): PricedExecution[O, L, P]
  def intent(side: Side, lots: L, positionEffect: PositionEffect = PositionEffect.Unrestricted): OrderIntent[O, L]
  def create(
    intent: OrderIntent[O, L],
    activation: OrderActivation[O, P],
    execution: OrderExecution[O, L, P]
  ): Either[EconomicsError, InstrumentOrder[O, L, P]]
  def market(
    side: Side,
    lots: L,
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[EconomicsError, InstrumentOrder[O, L, P]]
  def limit(
    side: Side,
    lots: L,
    limit: P,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: PricedVisibility[O, L] = displayed
  ): Either[EconomicsError, InstrumentOrder[O, L, P]]
  def stopMarket(
    side: Side,
    lots: L,
    trigger: OrderActivation[O, P],
    positionEffect: PositionEffect = PositionEffect.Unrestricted
  ): Either[EconomicsError, InstrumentOrder[O, L, P]]
  def stopLimit(
    side: Side,
    lots: L,
    trigger: OrderActivation[O, P],
    limit: P,
    timeInForce: TimeInForce = TimeInForce.GoodTillCancelled,
    liquidityConstraint: LiquidityConstraint = LiquidityConstraint.Unrestricted,
    positionEffect: PositionEffect = PositionEffect.Unrestricted,
    visibility: PricedVisibility[O, L] = displayed
  ): Either[EconomicsError, InstrumentOrder[O, L, P]]
end OrderCapability

trait ScenarioCapability[O, L, P, M, Pos]:
  def immediate: ImmediateAssumption[O, P]
  def fixedEvidence(reference: PriceReference, observedPrice: P): FixedTriggerEvidence[O, P]
  def trailingEvidence(reference: PriceReference, favorableExtreme: P, observedPrice: P): TrailingTriggerEvidence[O, P]
  def triggered(evidence: TriggerEvidence[O, P]): TriggeredAssumption[O, P]
  def directPricing: DirectPricingAssumption[O, P]
  def pegResolution(reference: PriceReference, referencePrice: P, resolvedLimit: P): PegResolution[O, P]
  def resolvedPeg(resolution: PegResolution[O, P]): ResolvedPegAssumption[O, P]
  def slice(lots: L, market: M, role: LiquidityRole): InstrumentLiquiditySlice[O, L, M]
  def assumptions(
    activation: ActivationAssumption[O, P],
    pricing: PricingAssumption[O, P],
    matchedSlices: Vector[InstrumentLiquiditySlice[O, L, M]]
  ): ScenarioAssumptions[O, L, P, M]
  def order(
    order: InstrumentOrder[O, L, P],
    assumptions: ScenarioAssumptions[O, L, P, M]
  ): Either[EconomicsError, InstrumentOrderScenario[O, L, P, M, Pos]]
  def roundTrip(
    entry: InstrumentOrderScenario[O, L, P, M, Pos],
    exit: InstrumentOrderScenario[O, L, P, M, Pos]
  ): Either[EconomicsError, InstrumentRoundTripScenario[O, L, P, M, Pos]]
end ScenarioCapability

trait FeeCapability[O, L, P, M, Pos]:
  def denomination(
    feeAsset: AssetRef
  )(
    grid: RegisteredGridRef[? <: Dimension],
    policy: QuantizationPolicy
  ): Either[EconomicsError, InstrumentFeeDenomination[O] { type D = feeAsset.D }]
  def line(
    scenario: InstrumentOrderScenario[O, L, P, M, Pos],
    sourceSliceIndex: Int,
    fee: InstrumentFee[O]
  ): Either[EconomicsError, InstrumentFeeLine[O, M]]
  def none: InstrumentFeeSchedule[O, L, P, M, Pos]
  def combine(
    componentSchedules: Vector[InstrumentFeeSchedule[O, L, P, M, Pos]]
  ): InstrumentFeeSchedule[O, L, P, M, Pos]

trait ValuationCapability[
  O,
  D <: Dimension,
  B <: Dimension,
  Q <: Dimension,
  S <: Dimension,
  L,
  P,
  M,
  Pos]:
  def settlePerPosition(state: M): Rate[D, S]
  def positionValue(value: Pos, state: M): Quantity[S]
  def pricePnl(value: Pos, entry: M, exit: M): Quantity[S]
  def pnl(
    roundTrip: InstrumentRoundTripScenario[O, L, P, M, Pos],
    feeSchedule: InstrumentFeeSchedule[O, L, P, M, Pos]
  ): Either[EconomicsError, InstrumentPnl[O, S]]

trait SizingCapability[O, S <: Dimension, L, P, M, Pos]:
  def downsideRisk(pnl: InstrumentPnl[O, S]): Quantity[S]
  def maxLots(
    riskBudget: Quantity[S],
    cap: PositiveWhole,
    feeSchedule: InstrumentFeeSchedule[O, L, P, M, Pos]
  )(
    scenarioFor: L => Either[EconomicsError, InstrumentRoundTripScenario[O, L, P, M, Pos]]
  ): Either[EconomicsError, Option[L]]

/**
 * A validated, generative instrument aggregate. Each successful construction owns one fresh type identity shared by all
 * primitive values, alternatives, aggregates, and capabilities.
 */
sealed abstract class Instrument private[economics] (authority: JvmOwnerAuthority) extends JavaSerializationUnsupported:
  authority.assertIssued()
  type Owner

  val identity: InstrumentIdentity
  val roles: InstrumentRoles
  val listingRules: ListingRules
  val contractPayoff: ContractPayoff

  type Lots                 = InstrumentLots[Owner, roles.position.D]
  type PositionLots         = InstrumentPosition[Owner, roles.position.D]
  type Price                = InstrumentPrice[Owner, roles.base.D, roles.quote.D]
  type SettleConversion     = InstrumentSettlementConversion[Owner, roles.settle.D]
  type MarketState          = InstrumentMarketState[Owner, roles.base.D, roles.quote.D, roles.settle.D]
  type Activation           = OrderActivation[Owner, Price]
  type Pricing              = OrderPricing[Owner, Price]
  type Visibility           = PricedVisibility[Owner, Lots]
  type Execution            = OrderExecution[Owner, Lots, Price]
  type Intent               = OrderIntent[Owner, Lots]
  type Order                = InstrumentOrder[Owner, Lots, Price]
  type ActivationEvidence   = TriggerEvidence[Owner, Price]
  type ActivationAssumption = trading.economics.ActivationAssumption[Owner, Price]
  type PricingAssumption    = trading.economics.PricingAssumption[Owner, Price]
  type PegResolution        = trading.economics.PegResolution[Owner, Price]
  type LiquiditySlice       = InstrumentLiquiditySlice[Owner, Lots, MarketState]
  type ScenarioAssumptions  = trading.economics.ScenarioAssumptions[Owner, Lots, Price, MarketState]
  type OrderScenario        = InstrumentOrderScenario[Owner, Lots, Price, MarketState, PositionLots]
  type RoundTripScenario    = InstrumentRoundTripScenario[Owner, Lots, Price, MarketState, PositionLots]
  type FeeDenomination      = InstrumentFeeDenomination[Owner]
  type Fee                  = InstrumentFee[Owner]
  type FeeLine              = InstrumentFeeLine[Owner, MarketState]
  type FeeSchedule          = InstrumentFeeSchedule[Owner, Lots, Price, MarketState, PositionLots]
  type ConvertedFeeLine     = InstrumentConvertedFeeLine[Owner, roles.settle.D]
  type Pnl                  = InstrumentPnl[Owner, roles.settle.D]
  type Prices               = PriceCapability[Owner, roles.base.D, roles.quote.D]
  type Market               = MarketCapability[Owner, roles.base.D, roles.quote.D, roles.settle.D]
  type Orders               = OrderCapability[Owner, Lots, Price]
  type Scenarios            = ScenarioCapability[Owner, Lots, Price, MarketState, PositionLots]
  type Fees                 = FeeCapability[Owner, Lots, Price, MarketState, PositionLots]
  type Valuation            = ValuationCapability[
    Owner,
    roles.position.D,
    roles.base.D,
    roles.quote.D,
    roles.settle.D,
    Lots,
    Price,
    MarketState,
    PositionLots
  ]
  type Sizing = SizingCapability[Owner, roles.settle.D, Lots, Price, MarketState, PositionLots]

  def lots(count: BigInt): Either[EconomicsError, Lots]
  def positionLots(side: Side, lots: Lots): PositionLots
  def flatPosition: PositionLots

  val prices: Prices
  val market: Market
  val orders: Orders
  val scenarios: Scenarios
  val fees: Fees
  val valuation: Valuation
  val sizing: Sizing

end Instrument

object Instrument:

  /** Closed construction authority. It is never returned by the public aggregate. */
  private[economics] final class OwnerAuthority[O] private[Instrument] (private val authority: JvmOwnerAuthority):
    authority.assertIssued()

    private[economics] def jvmGate: JvmOwnerAuthority = authority
    private[economics] def assertIssued(): Unit       = authority.assertIssued()

    final def lots[D <: Dimension](
      grid: RegisteredGridRef[D]
    )(
      payload: Positive[GridQuantity[D, grid.G]]
    ): InstrumentLots[O, D] =
      Instrument.makeLots(this, grid)(payload)

    final def position[D <: Dimension](
      grid: RegisteredGridRef[D]
    )(
      payload: GridQuantity[D, grid.G]
    ): InstrumentPosition[O, D] =
      Instrument.makePosition(this, grid)(payload)

    final def price[B <: Dimension, Q <: Dimension](
      grid: RegisteredGridRef[Divide[Q, B]],
      base: DimRef[B],
      quote: DimRef[Q]
    )(
      payload: Positive[GridQuantity[Divide[Q, B], grid.G]]
    ): InstrumentPrice[O, B, Q] =
      Instrument.makePrice(this, grid, base, quote)(payload)

    final def conversion[S <: Dimension](
      source: AssetRef,
      target: AssetRef { type D = S },
      coefficient: Rational
    ): InstrumentSettlementConversion[O, S] =
      Instrument.ownedConversion(this)(source, target, coefficient)

    final def marketState[B <: Dimension, Q <: Dimension, S <: Dimension](
      price: InstrumentPrice[O, B, Q],
      baseToSettle: Rate[B, S],
      quoteToSettle: Rate[Q, S],
      settleRef: DimRef[S],
      conversions: Vector[(AssetRef, Rational)]
    ): InstrumentMarketState[O, B, Q, S] =
      Instrument.ownedMarketState(this, price, baseToSettle, quoteToSettle, settleRef, conversions)

    final def immediate[P]: ImmediateActivation[O, P] = Instrument.immediate(this)
    final def fixed[P](reference: PriceReference, comparison: TriggerComparison, price: P): FixedActivation[O, P] =
      Instrument.fixed(this)(reference, comparison, price)
    final def trailing[P](
      reference: PriceReference,
      comparison: TriggerComparison,
      ticks: PositiveWhole
    ): TrailingActivation[O, P] =
      Instrument.trailing(this)(reference, comparison, ticks)
    final def limitPricing[P](price: P): LimitPricing[O, P] = Instrument.limitPricing(this)(price)
    final def peggedPricing[P](reference: PriceReference, offset: BigInt): PeggedPricing[O, P] =
      Instrument.peggedPricing(this)(reference, offset)
    final def displayed[L]: DisplayedVisibility[O, L]      = Instrument.displayed(this)
    final def hidden[L]: HiddenVisibility[O, L]            = Instrument.hidden(this)
    final def iceberg[L](lots: L): IcebergVisibility[O, L] = Instrument.iceberg(this)(lots)
    final def marketExecution[L, P](tif: NonRestingTimeInForce): MarketExecution[O, L, P] =
      Instrument.marketExecution(this)(tif)
    final def pricedExecution[L, P](
      pricing: OrderPricing[O, P],
      tif: TimeInForce,
      liquidity: LiquidityConstraint,
      visibility: PricedVisibility[O, L]
    ): PricedExecution[O, L, P] =
      Instrument.pricedExecution(this)(pricing, tif, liquidity, visibility)
    final def orderIntent[L](side: Side, lots: L, effect: PositionEffect): OrderIntent[O, L] =
      Instrument.orderIntent(this)(side, lots, effect)
    final def order[L, P](
      intent: OrderIntent[O, L],
      activation: OrderActivation[O, P],
      execution: OrderExecution[O, L, P]
    ): InstrumentOrder[O, L, P] =
      Instrument.order(this)(intent, activation, execution)

    final def fixedEvidence[P](reference: PriceReference, observed: P): FixedTriggerEvidence[O, P] =
      Instrument.fixedEvidence(this)(reference, observed)
    final def trailingEvidence[P](
      reference: PriceReference,
      extreme: P,
      observed: P
    ): TrailingTriggerEvidence[O, P] =
      Instrument.trailingEvidence(this)(reference, extreme, observed)
    final def immediateAssumption[P]: ImmediateAssumption[O, P] = Instrument.immediateAssumption(this)
    final def triggeredAssumption[P](evidence: TriggerEvidence[O, P]): TriggeredAssumption[O, P] =
      Instrument.triggeredAssumption(this)(evidence)
    final def pegResolution[P](reference: PriceReference, referencePrice: P, resolvedLimit: P): PegResolution[O, P] =
      Instrument.pegResolution(this)(reference, referencePrice, resolvedLimit)
    final def directPricing[P]: DirectPricingAssumption[O, P] = Instrument.directPricing(this)
    final def resolvedPeg[P](resolution: PegResolution[O, P]): ResolvedPegAssumption[O, P] =
      Instrument.resolvedPeg(this)(resolution)
    final def liquiditySlice[L, M](lots: L, market: M, role: LiquidityRole): InstrumentLiquiditySlice[O, L, M] =
      Instrument.liquiditySlice(this)(lots, market, role)
    final def scenarioAssumptions[L, P, M](
      activation: ActivationAssumption[O, P],
      pricing: PricingAssumption[O, P],
      slices: Vector[InstrumentLiquiditySlice[O, L, M]]
    ): ScenarioAssumptions[O, L, P, M] =
      Instrument.scenarioAssumptions(this)(activation, pricing, slices)
    final def orderScenario[L, P, M, Pos](
      order: InstrumentOrder[O, L, P],
      assumptions: ScenarioAssumptions[O, L, P, M],
      change: Pos
    ): InstrumentOrderScenario[O, L, P, M, Pos] =
      Instrument.orderScenario(this)(order, assumptions, change)
    final def roundTrip[L, P, M, Pos](
      entry: InstrumentOrderScenario[O, L, P, M, Pos],
      exit: InstrumentOrderScenario[O, L, P, M, Pos],
      held: Pos
    ): InstrumentRoundTripScenario[O, L, P, M, Pos] =
      Instrument.roundTrip(this)(entry, exit, held)

    final def feeDenomination[DD <: Dimension](
      asset: AssetRef { type D = DD },
      grid: RegisteredGridRef[DD],
      policy: QuantizationPolicy
    ): InstrumentFeeDenomination[O] { type D = DD } =
      Instrument.feeDenomination(this)(asset, grid, policy)
    final def fee[DD <: Dimension](
      denomination: InstrumentFeeDenomination[O],
      asset: AssetRef { type D = DD },
      kind: FeeKind,
      coordinate: BigInt,
      amount: Quantity[DD],
      residual: Quantity[DD],
      unrounded: Quantity[DD]
    ): InstrumentFee[O] =
      Instrument.makeFee(this, denomination, asset, kind, coordinate, amount, residual, unrounded)
    final def feeLine[M](
      scenario: AnyRef,
      fee: InstrumentFee[O],
      index: Int,
      market: M
    ): InstrumentFeeLine[O, M] =
      Instrument.feeLine(this)(scenario, fee, index, market)
    final def feeLineScenario(line: InstrumentFeeLine[?, ?]): AnyRef = Instrument.feeLineScenario(line)
    final def convertedFeeLine[S <: Dimension](
      fee: InstrumentFee[O],
      leg: ScenarioLeg,
      index: Int,
      contribution: Quantity[S]
    ): InstrumentConvertedFeeLine[O, S] =
      Instrument.convertedFeeLine(this)(fee, leg, index, contribution)
    final def pnl[S <: Dimension](
      price: Quantity[S],
      lines: Vector[InstrumentConvertedFeeLine[O, S]],
      fees: Quantity[S],
      net: Quantity[S]
    ): InstrumentPnl[O, S] =
      Instrument.pnl(this)(price, lines, fees, net)
  end OwnerAuthority
  private final class LotsImpl[O, DD <: Dimension, GG](
    authority: OwnerAuthority[O],
    private val payload: Positive[GridQuantity[DD, GG]],
    grid: RegisteredGridRef[DD] { type G = GG })
    extends InstrumentLots[O, DD](authority.jvmGate):
    val count: PositiveWhole   = PositiveWhole(grid.coordinate(payload.unrefined)).toOption.get
    val quantity: Quantity[DD] = grid.asQuantity(payload.unrefined)

  private final class PositionImpl[O, DD <: Dimension, GG](
    authority: OwnerAuthority[O],
    private val payload: GridQuantity[DD, GG],
    grid: RegisteredGridRef[DD] { type G = GG })
    extends InstrumentPosition[O, DD](authority.jvmGate):
    val count: BigInt          = grid.coordinate(payload)
    val quantity: Quantity[DD] = grid.asQuantity(payload)

  private final class PriceImpl[O, B <: Dimension, Q <: Dimension, GG](
    authority: OwnerAuthority[O],
    private val payload: Positive[GridQuantity[Divide[Q, B], GG]],
    grid: RegisteredGridRef[Divide[Q, B]] { type G = GG },
    base: DimRef[B],
    quote: DimRef[Q])
    extends InstrumentPrice[O, B, Q](authority.jvmGate):
    val ticks: PositiveWhole  = PositiveWhole(grid.coordinate(payload.unrefined)).toOption.get
    val coefficient: Rational = grid.asQuantity(payload.unrefined).coefficient
    val rate: Rate[B, Q]      = Rate(base, quote, coefficient)

  private final class ConversionImpl[O, S <: Dimension](
    authority: OwnerAuthority[O],
    val source: AssetRef,
    val target: AssetRef { type D = S },
    val coefficient: Rational)
    extends InstrumentSettlementConversion[O, S](authority.jvmGate)

  private final class MarketStateImpl[O, B <: Dimension, Q <: Dimension, S <: Dimension](
    authority: OwnerAuthority[O],
    val price: InstrumentPrice[O, B, Q],
    val baseToSettle: Rate[B, S],
    val quoteToSettle: Rate[Q, S],
    settleRef: DimRef[S],
    conversions: Vector[(AssetRef, Rational)])
    extends InstrumentMarketState[O, B, Q, S](authority.jvmGate):
    private val byId                       = conversions.map(value => value._1.id -> value).toMap
    val conversionSources: Vector[AssetId] = conversions.map(_._1.id)
    def convertToSettle(source: AssetRef)(value: Quantity[source.D]): Either[EconomicsError, Quantity[S]] =
      byId.get(source.id) match
        case None => Left(MissingConversion(source.id, None, None))
        case Some((registered, _))
          if registered.dimension.key != source.dimension.key ||
            !registered.dimension.sharesRegistryWith(source.dimension) =>
          Left(ForeignRegistry("conversion lookup", registered.dimension.key, source.dimension.key))
        case Some((_, coefficient)) => Right(Quantity(settleRef, value.coefficient * coefficient))

  private final class ImmediateActivationImpl[O, P](authority: OwnerAuthority[O])
    extends ImmediateActivation[O, P](authority.jvmGate)
  private final class FixedActivationImpl[O, P](
    authority: OwnerAuthority[O],
    val reference: PriceReference,
    val comparison: TriggerComparison,
    val triggerPrice: P)
    extends FixedActivation[O, P](authority.jvmGate)
  private final class TrailingActivationImpl[O, P](
    authority: OwnerAuthority[O],
    val reference: PriceReference,
    val comparison: TriggerComparison,
    val offsetTicks: PositiveWhole)
    extends TrailingActivation[O, P](authority.jvmGate)
  private final class LimitPricingImpl[O, P](authority: OwnerAuthority[O], val limit: P)
    extends LimitPricing[O, P](authority.jvmGate)
  private final class PeggedPricingImpl[O, P](
    authority: OwnerAuthority[O],
    val reference: PriceReference,
    val offsetTicks: BigInt)
    extends PeggedPricing[O, P](authority.jvmGate)
  private final class DisplayedVisibilityImpl[O, L](authority: OwnerAuthority[O])
    extends DisplayedVisibility[O, L](authority.jvmGate)
  private final class HiddenVisibilityImpl[O, L](authority: OwnerAuthority[O])
    extends HiddenVisibility[O, L](authority.jvmGate)
  private final class IcebergVisibilityImpl[O, L](authority: OwnerAuthority[O], val displayedLots: L)
    extends IcebergVisibility[O, L](authority.jvmGate)
  private final class MarketExecutionImpl[O, L, P](authority: OwnerAuthority[O], val timeInForce: NonRestingTimeInForce)
    extends MarketExecution[O, L, P](authority.jvmGate)
  private final class PricedExecutionImpl[O, L, P](
    authority: OwnerAuthority[O],
    val pricing: OrderPricing[O, P],
    val timeInForce: TimeInForce,
    val liquidityConstraint: LiquidityConstraint,
    val visibility: PricedVisibility[O, L])
    extends PricedExecution[O, L, P](authority.jvmGate)
  private final class OrderIntentImpl[O, L](
    authority: OwnerAuthority[O],
    val side: Side,
    val lots: L,
    val positionEffect: PositionEffect)
    extends OrderIntent[O, L](authority.jvmGate)
  private final class OrderImpl[O, L, P](
    authority: OwnerAuthority[O],
    val intent: OrderIntent[O, L],
    val activation: OrderActivation[O, P],
    val execution: OrderExecution[O, L, P])
    extends InstrumentOrder[O, L, P](authority.jvmGate)

  private final class FixedEvidenceImpl[O, P](
    authority: OwnerAuthority[O],
    val reference: PriceReference,
    val observedPrice: P)
    extends FixedTriggerEvidence[O, P](authority.jvmGate)
  private final class TrailingEvidenceImpl[O, P](
    authority: OwnerAuthority[O],
    val reference: PriceReference,
    val favorableExtreme: P,
    val observedPrice: P)
    extends TrailingTriggerEvidence[O, P](authority.jvmGate)
  private final class ImmediateAssumptionImpl[O, P](authority: OwnerAuthority[O])
    extends ImmediateAssumption[O, P](authority.jvmGate)
  private final class TriggeredAssumptionImpl[O, P](authority: OwnerAuthority[O], val evidence: TriggerEvidence[O, P])
    extends TriggeredAssumption[O, P](authority.jvmGate)
  private final class PegResolutionImpl[O, P](
    authority: OwnerAuthority[O],
    val reference: PriceReference,
    val referencePrice: P,
    val resolvedLimit: P)
    extends PegResolution[O, P](authority.jvmGate)
  private final class DirectPricingImpl[O, P](authority: OwnerAuthority[O])
    extends DirectPricingAssumption[O, P](authority.jvmGate)
  private final class ResolvedPegImpl[O, P](authority: OwnerAuthority[O], val resolution: PegResolution[O, P])
    extends ResolvedPegAssumption[O, P](authority.jvmGate)
  private final class LiquiditySliceImpl[O, L, M](
    authority: OwnerAuthority[O],
    val lots: L,
    val market: M,
    val role: LiquidityRole)
    extends InstrumentLiquiditySlice[O, L, M](authority.jvmGate)
  private final class ScenarioAssumptionsImpl[O, L, P, M](
    authority: OwnerAuthority[O],
    val activation: ActivationAssumption[O, P],
    val pricing: PricingAssumption[O, P],
    val matchedSlices: Vector[InstrumentLiquiditySlice[O, L, M]])
    extends ScenarioAssumptions[O, L, P, M](authority.jvmGate)
  private final class OrderScenarioImpl[O, L, P, M, Pos](
    authority: OwnerAuthority[O],
    val order: InstrumentOrder[O, L, P],
    val assumptions: ScenarioAssumptions[O, L, P, M],
    val positionChange: Pos)
    extends InstrumentOrderScenario[O, L, P, M, Pos](authority.jvmGate)
  private final class RoundTripImpl[O, L, P, M, Pos](
    authority: OwnerAuthority[O],
    val entry: InstrumentOrderScenario[O, L, P, M, Pos],
    val exit: InstrumentOrderScenario[O, L, P, M, Pos],
    val heldPosition: Pos)
    extends InstrumentRoundTripScenario[O, L, P, M, Pos](authority.jvmGate)

  private final class FeeDenominationImpl[O, DD <: Dimension, GG](
    val asset: AssetRef { type D = DD },
    grid: RegisteredGridRef[DD] { type G = GG },
    val policy: QuantizationPolicy,
    authority: OwnerAuthority[O])
    extends InstrumentFeeDenomination[O](authority.jvmGate):
    type D = DD
    val gridKey: GridKey      = grid.key
    val gridQuantum: Rational = grid.quantum.unrefined
    def minimumCharge(
      accountContribution: Quantity[DD],
      nonnegativeMinimum: Quantity[DD]
    ): Either[EconomicsError, Quantity[DD]] =
      InstrumentFees
        .minimumCharge(accountContribution.coefficient, nonnegativeMinimum.coefficient)
        .left
        .map(coefficient => InvalidFeeBasis(asset.id, coefficient))
        .map(coefficient => Quantity(asset.dimension.asDimensionRef, coefficient))
    def quantize(kind: FeeKind, unrounded: Quantity[DD]): InstrumentFee[O] =
      val result = unrounded.quantizeTo(grid.asGridRef, policy)
      authority.fee(
        this,
        asset,
        kind,
        grid.coordinate(result.value),
        grid.asQuantity(result.value),
        result.residual,
        unrounded
      )
    def percentage(
      kind: FeeKind,
      nonnegativeBasis: Quantity[DD],
      rate: FeeRate
    ): Either[EconomicsError, InstrumentFee[O]] =
      InstrumentFees
        .percentageContribution(nonnegativeBasis.coefficient, rate.coefficient)
        .left
        .map(coefficient => InvalidFeeBasis(asset.id, coefficient))
        .map(coefficient => quantize(kind, Quantity(asset.dimension.asDimensionRef, coefficient)))
  end FeeDenominationImpl

  private final class FeeImpl[O](
    authority: OwnerAuthority[O],
    val denomination: InstrumentFeeDenomination[O],
    val kind: FeeKind,
    val coordinate: BigInt
  )(
    val asset: AssetRef
  )(
    val amount: Quantity[asset.D],
    val residual: Quantity[asset.D],
    val unrounded: Quantity[asset.D])
    extends InstrumentFee[O](authority.jvmGate)

  private final class FeeLineImpl[O, M](
    authority: OwnerAuthority[O],
    private[economics] val scenarioIdentity: AnyRef,
    val fee: InstrumentFee[O],
    val sourceSliceIndex: Int,
    val sourceMarket: M)
    extends InstrumentFeeLine[O, M](authority.jvmGate)
  private final class ConvertedFeeLineImpl[O, S <: Dimension](
    authority: OwnerAuthority[O],
    val original: InstrumentFee[O],
    val leg: ScenarioLeg,
    val sourceSliceIndex: Int,
    val settleContribution: Quantity[S])
    extends InstrumentConvertedFeeLine[O, S](authority.jvmGate)
  private final class PnlImpl[O, S <: Dimension](
    authority: OwnerAuthority[O],
    val pricePnl: Quantity[S],
    val convertedFeeLines: Vector[InstrumentConvertedFeeLine[O, S]],
    val feePnl: Quantity[S],
    val netPnl: Quantity[S])
    extends InstrumentPnl[O, S](authority.jvmGate)

  private def makeLots[O, D <: Dimension](
    authority: OwnerAuthority[O],
    grid: RegisteredGridRef[D]
  )(
    payload: Positive[GridQuantity[D, grid.G]]
  ): InstrumentLots[O, D] =
    val _ = authority
    new LotsImpl[O, D, grid.G](authority, payload, grid)

  private def makePosition[O, D <: Dimension](
    authority: OwnerAuthority[O],
    grid: RegisteredGridRef[D]
  )(
    payload: GridQuantity[D, grid.G]
  ): InstrumentPosition[O, D] =
    val _ = authority
    new PositionImpl[O, D, grid.G](authority, payload, grid)

  private def makePrice[O, B <: Dimension, Q <: Dimension](
    authority: OwnerAuthority[O],
    grid: RegisteredGridRef[Divide[Q, B]],
    base: DimRef[B],
    quote: DimRef[Q]
  )(
    payload: Positive[GridQuantity[Divide[Q, B], grid.G]]
  ): InstrumentPrice[O, B, Q] =
    val _ = authority
    new PriceImpl[O, B, Q, grid.G](authority, payload, grid, base, quote)

  private def ownedConversion[O, S <: Dimension](
    authority: OwnerAuthority[O]
  )(
    source: AssetRef,
    target: AssetRef { type D = S },
    coefficient: Rational
  ): InstrumentSettlementConversion[O, S] =
    val _ = authority
    new ConversionImpl(authority, source, target, coefficient)

  private def ownedMarketState[O, B <: Dimension, Q <: Dimension, S <: Dimension](
    authority: OwnerAuthority[O],
    price: InstrumentPrice[O, B, Q],
    baseToSettle: Rate[B, S],
    quoteToSettle: Rate[Q, S],
    settleRef: DimRef[S],
    conversions: Vector[(AssetRef, Rational)]
  ): InstrumentMarketState[O, B, Q, S] =
    val _ = authority
    new MarketStateImpl(authority, price, baseToSettle, quoteToSettle, settleRef, conversions)

  private def immediate[O, P](authority: OwnerAuthority[O]): ImmediateActivation[O, P] =
    val _ = authority
    new ImmediateActivationImpl(authority)
  private def fixed[O, P](
    authority: OwnerAuthority[O]
  )(
    reference: PriceReference,
    comparison: TriggerComparison,
    price: P
  ): FixedActivation[O, P] =
    val _ = authority
    new FixedActivationImpl(authority, reference, comparison, price)
  private def trailing[O, P](
    authority: OwnerAuthority[O]
  )(
    reference: PriceReference,
    comparison: TriggerComparison,
    ticks: PositiveWhole
  ): TrailingActivation[O, P] =
    val _ = authority
    new TrailingActivationImpl(authority, reference, comparison, ticks)
  private def limitPricing[O, P](authority: OwnerAuthority[O])(price: P): LimitPricing[O, P] =
    val _ = authority
    new LimitPricingImpl(authority, price)
  private def peggedPricing[O, P](
    authority: OwnerAuthority[O]
  )(
    reference: PriceReference,
    offset: BigInt
  ): PeggedPricing[O, P] =
    val _ = authority
    new PeggedPricingImpl(authority, reference, offset)
  private def displayed[O, L](authority: OwnerAuthority[O]): DisplayedVisibility[O, L] =
    val _ = authority
    new DisplayedVisibilityImpl(authority)
  private def hidden[O, L](authority: OwnerAuthority[O]): HiddenVisibility[O, L] =
    val _ = authority
    new HiddenVisibilityImpl(authority)
  private def iceberg[O, L](authority: OwnerAuthority[O])(lots: L): IcebergVisibility[O, L] =
    val _ = authority
    new IcebergVisibilityImpl(authority, lots)
  private def marketExecution[O, L, P](
    authority: OwnerAuthority[O]
  )(
    tif: NonRestingTimeInForce
  ): MarketExecution[O, L, P] =
    val _ = authority
    new MarketExecutionImpl(authority, tif)
  private def pricedExecution[O, L, P](
    authority: OwnerAuthority[O]
  )(
    pricing: OrderPricing[O, P],
    tif: TimeInForce,
    liquidity: LiquidityConstraint,
    visibility: PricedVisibility[O, L]
  ): PricedExecution[O, L, P] =
    val _ = authority
    new PricedExecutionImpl(authority, pricing, tif, liquidity, visibility)
  private def orderIntent[O, L](
    authority: OwnerAuthority[O]
  )(
    side: Side,
    lots: L,
    effect: PositionEffect
  ): OrderIntent[O, L] =
    val _ = authority
    new OrderIntentImpl(authority, side, lots, effect)
  private def order[O, L, P](
    authority: OwnerAuthority[O]
  )(
    intent: OrderIntent[O, L],
    activation: OrderActivation[O, P],
    execution: OrderExecution[O, L, P]
  ): InstrumentOrder[O, L, P] =
    val _ = authority
    new OrderImpl(authority, intent, activation, execution)

  private def fixedEvidence[O, P](
    authority: OwnerAuthority[O]
  )(
    reference: PriceReference,
    observed: P
  ): FixedTriggerEvidence[O, P] =
    val _ = authority
    new FixedEvidenceImpl(authority, reference, observed)
  private def trailingEvidence[O, P](
    authority: OwnerAuthority[O]
  )(
    reference: PriceReference,
    extreme: P,
    observed: P
  ): TrailingTriggerEvidence[O, P] =
    val _ = authority
    new TrailingEvidenceImpl(authority, reference, extreme, observed)
  private def immediateAssumption[O, P](authority: OwnerAuthority[O]): ImmediateAssumption[O, P] =
    val _ = authority
    new ImmediateAssumptionImpl(authority)
  private def triggeredAssumption[O, P](
    authority: OwnerAuthority[O]
  )(
    evidence: TriggerEvidence[O, P]
  ): TriggeredAssumption[O, P] =
    val _ = authority
    new TriggeredAssumptionImpl(authority, evidence)
  private def pegResolution[O, P](
    authority: OwnerAuthority[O]
  )(
    reference: PriceReference,
    referencePrice: P,
    resolvedLimit: P
  ): PegResolution[O, P] =
    val _ = authority
    new PegResolutionImpl(authority, reference, referencePrice, resolvedLimit)
  private def directPricing[O, P](authority: OwnerAuthority[O]): DirectPricingAssumption[O, P] =
    val _ = authority
    new DirectPricingImpl(authority)
  private def resolvedPeg[O, P](
    authority: OwnerAuthority[O]
  )(
    resolution: PegResolution[O, P]
  ): ResolvedPegAssumption[O, P] =
    val _ = authority
    new ResolvedPegImpl(authority, resolution)
  private def liquiditySlice[O, L, M](
    authority: OwnerAuthority[O]
  )(
    lots: L,
    market: M,
    role: LiquidityRole
  ): InstrumentLiquiditySlice[O, L, M] =
    val _ = authority
    new LiquiditySliceImpl(authority, lots, market, role)
  private def scenarioAssumptions[O, L, P, M](
    authority: OwnerAuthority[O]
  )(
    activation: ActivationAssumption[O, P],
    pricing: PricingAssumption[O, P],
    slices: Vector[InstrumentLiquiditySlice[O, L, M]]
  ): ScenarioAssumptions[O, L, P, M] =
    val _ = authority
    new ScenarioAssumptionsImpl(authority, activation, pricing, slices)
  private def orderScenario[O, L, P, M, Pos](
    authority: OwnerAuthority[O]
  )(
    order: InstrumentOrder[O, L, P],
    assumptions: ScenarioAssumptions[O, L, P, M],
    change: Pos
  ): InstrumentOrderScenario[O, L, P, M, Pos] =
    val _ = authority
    new OrderScenarioImpl(authority, order, assumptions, change)
  private def roundTrip[O, L, P, M, Pos](
    authority: OwnerAuthority[O]
  )(
    entry: InstrumentOrderScenario[O, L, P, M, Pos],
    exit: InstrumentOrderScenario[O, L, P, M, Pos],
    held: Pos
  ): InstrumentRoundTripScenario[O, L, P, M, Pos] =
    val _ = authority
    new RoundTripImpl(authority, entry, exit, held)

  private def feeDenomination[O, DD <: Dimension](
    authority: OwnerAuthority[O]
  )(
    asset: AssetRef { type D = DD },
    grid: RegisteredGridRef[DD],
    policy: QuantizationPolicy
  ): InstrumentFeeDenomination[O] { type D = DD } =
    new FeeDenominationImpl[O, DD, grid.G](asset, grid, policy, authority)

  private def makeFee[O, DD <: Dimension](
    authority: OwnerAuthority[O],
    denomination: InstrumentFeeDenomination[O],
    asset: AssetRef { type D = DD },
    kind: FeeKind,
    coordinate: BigInt,
    amount: Quantity[DD],
    residual: Quantity[DD],
    unrounded: Quantity[DD]
  ): InstrumentFee[O] =
    val _                       = authority
    val stableAsset: asset.type = asset
    new FeeImpl(
      authority,
      denomination,
      kind,
      coordinate
    )(
      stableAsset
    )(
      amount,
      residual,
      unrounded
    )
  end makeFee

  private def feeLine[O, M](
    authority: OwnerAuthority[O]
  )(
    scenario: AnyRef,
    fee: InstrumentFee[O],
    index: Int,
    market: M
  ): InstrumentFeeLine[O, M] =
    val _ = authority
    new FeeLineImpl(authority, scenario, fee, index, market)
  private def feeLineScenario(line: InstrumentFeeLine[?, ?]): AnyRef =
    line match
      case value: FeeLineImpl[?, ?] => value.scenarioIdentity
  private def convertedFeeLine[O, S <: Dimension](
    authority: OwnerAuthority[O]
  )(
    fee: InstrumentFee[O],
    leg: ScenarioLeg,
    index: Int,
    contribution: Quantity[S]
  ): InstrumentConvertedFeeLine[O, S] =
    val _ = authority
    new ConvertedFeeLineImpl(authority, fee, leg, index, contribution)
  private def pnl[O, S <: Dimension](
    authority: OwnerAuthority[O]
  )(
    price: Quantity[S],
    lines: Vector[InstrumentConvertedFeeLine[O, S]],
    fees: Quantity[S],
    net: Quantity[S]
  ): InstrumentPnl[O, S] =
    val _ = authority
    new PnlImpl(authority, price, lines, fees, net)

  /** Final validated definition boundary and sole owner-authority issuer. */
  def create(definition: InstrumentDefinition): Either[EconomicsError, Instrument] =
    JvmOwnerAuthority.createInstrument(definition)

  /** JVM entry used only by [[JvmOwnerAuthority.createInstrument]], whose private constructor supplies the gate. */
  private[economics] def createWithAuthority(
    definition: InstrumentDefinition,
    issuedAuthority: JvmOwnerAuthority
  ): Either[EconomicsError, Instrument] =
    issuedAuthority.assertIssued()
    val identity   = definition.identity
    val roles      = definition.roles
    val listing    = definition.listingRules
    val payoff     = definition.contractPayoff
    val roleAssets = Vector("quote" -> roles.quote, "position" -> roles.position, "settle" -> roles.settle)

    roleAssets
      .collectFirst:
        case (role, candidate) if !roles.base.dimension.sharesRegistryWith(candidate.dimension) =>
          ForeignRegistry(role, roles.base.dimension.key, candidate.dimension.key)
      .map(Left(_))
      .getOrElse:
        if !listing.roles.eq(roles) then
          Left(ContradictoryInstrument(identity.id, InstrumentContradiction.ListingRolesDiffer))
        else if !payoff.roles.eq(roles) then
          Left(ContradictoryInstrument(identity.id, InstrumentContradiction.PayoffRolesDiffer))
        else if roles.base.id == roles.quote.id then
          Left(ContradictoryInstrument(identity.id, InstrumentContradiction.BaseEqualsQuote))
        else if !listing.positionLotGrid.dimension.sharesRegistryWith(roles.position.dimension) then
          Left(ForeignRegistry("position grid", roles.position.dimension.key, listing.positionLotGrid.dimension.key))
        else if listing.positionLotGrid.dimension.key != roles.position.dimension.key then
          Left(
            GridDimensionFailure(
              "position grid",
              listing.positionLotGrid.key,
              roles.position.dimension.key,
              listing.positionLotGrid.dimension.key
            )
          )
        else
          val expectedPrice =
            DimRef.divide(roles.quote.dimension.asDimensionRef, roles.base.dimension.asDimensionRef).key
          if !listing.priceGrid.dimension.sharesRegistryWith(roles.base.dimension) then
            Left(ForeignRegistry("price grid", expectedPrice, listing.priceGrid.dimension.key))
          else if listing.priceGrid.dimension.key != expectedPrice then
            Left(
              GridDimensionFailure("price grid", listing.priceGrid.key, expectedPrice, listing.priceGrid.dimension.key)
            )
          else if payoff.basePerPosition.coefficient.isZero && payoff.quotePerPosition.coefficient.isZero then
            Left(EmptyContractPayoff(identity.id))
          else
            val positionGrid = listing.positionLotGrid.asInstanceOf[RegisteredGridRef[roles.position.D]]
            val priceGrid    = listing.priceGrid.asInstanceOf[RegisteredGridRef[Divide[roles.quote.D, roles.base.D]]]
            Right(new InstrumentImpl(issuedAuthority, identity, roles, listing, payoff)(positionGrid, priceGrid))
  end createWithAuthority

  private final class InstrumentImpl(
    issuedAuthority: JvmOwnerAuthority,
    val identity: InstrumentIdentity,
    val roles: InstrumentRoles,
    val listingRules: ListingRules,
    val contractPayoff: ContractPayoff
  )(
    positionGrid: RegisteredGridRef[roles.position.D],
    priceGrid: RegisteredGridRef[Divide[roles.quote.D, roles.base.D]])
    extends Instrument(issuedAuthority):

    type Owner = this.type
    private val authority: OwnerAuthority[Owner] = new OwnerAuthority[Owner](issuedAuthority)

    private val typedBasePerPosition = contractPayoff.basePerPosition
      .asInstanceOf[Rate[roles.position.D, roles.base.D]]
    private val typedQuotePerPosition = contractPayoff.quotePerPosition
      .asInstanceOf[Rate[roles.position.D, roles.quote.D]]

    def lots(count: BigInt): Either[EconomicsError, Lots] =
      val coordinate = positionGrid.fromCoordinate(count)
      Positive(coordinate)
        .left
        .map(_ => InvalidLots(count))
        .map(value => authority.lots(positionGrid)(value))

    def positionLots(side: Side, lots: Lots): PositionLots =
      authority.position(positionGrid)(positionGrid.fromCoordinate(side.sign * lots.count.unrefined))

    val flatPosition: PositionLots = authority.position(positionGrid)(positionGrid.fromCoordinate(0))

    val prices: Prices =
      new InstrumentPricesImpl(authority, roles.base, roles.quote, priceGrid)
    val market: Market =
      new InstrumentMarketImpl(authority, roles.base, roles.quote, roles.settle)
    val orders: Orders =
      new InstrumentOrdersImpl(authority)
    val scenarios: Scenarios =
      new InstrumentScenariosImpl(authority, positionGrid)
    val fees: Fees =
      new InstrumentFeesImpl(authority, roles.settle)
    val valuation: Valuation =
      new InstrumentValuationImpl(
        authority,
        roles.position,
        roles.settle,
        typedBasePerPosition,
        typedQuotePerPosition
      )
    val sizing: Sizing =
      new InstrumentSizingImpl(authority, roles.settle.dimension.asDimensionRef, lots, valuation)
  end InstrumentImpl

end Instrument
