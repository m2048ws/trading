package trading.codec

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.Objects
import scala.annotation.nowarn

import trading.economics.instrument.Instrument
import trading.economics.instrument.InstrumentId
import trading.economics.instrument.InvalidPriceCoordinate
import trading.economics.instrument.LotError
import trading.economics.instrument.Lots
import trading.economics.instrument.Price
import trading.economics.instrument.PriceError
import trading.order.DisplayedVisibility
import trading.order.FixedActivation
import trading.order.HiddenVisibility
import trading.order.IcebergVisibility
import trading.order.ImmediateActivation
import trading.order.LimitPricing
import trading.order.LiquidityConstraint as DomainLiquidityConstraint
import trading.order.MarketExecution
import trading.order.NonRestingTimeInForce
import trading.order.Order
import trading.order.OrderActivation
import trading.order.OrderExecution
import trading.order.OrderIntent
import trading.order.OrderPricing
import trading.order.OrderViolation
import trading.order.OrderViolations
import trading.order.PeggedPricing
import trading.order.PositionEffect as DomainPositionEffect
import trading.order.PricedExecution
import trading.order.PricedVisibility
import trading.order.PriceReference as DomainPriceReference
import trading.order.Side as DomainSide
import trading.order.TimeInForce as DomainTimeInForce
import trading.order.TrailingActivation
import trading.order.TriggerComparison as DomainTriggerComparison
import trading.quantity.JavaSerializationUnsupported

/** Local smart-constructor failures retained before canonical order validation. */
enum OrderRefinementFailure extends JavaSerializationUnsupported:
  case Lots(path: WirePath, cause: LotError)
  case TriggerPrice(path: WirePath, cause: PriceError)
  case TrailingOffset(path: WirePath, cause: OrderViolation)
  case MarketDuration(path: WirePath, cause: OrderViolation)
  case LimitPrice(path: WirePath, cause: PriceError)
  case DisplayedLots(path: WirePath, cause: LotError)
  case Intent(path: WirePath, cause: OrderViolation)

  private val _ =
    this match
      case Lots(path, cause)           => requireParts(path, cause)
      case TriggerPrice(path, cause)   => requireParts(path, cause)
      case TrailingOffset(path, cause) => requireParts(path, cause)
      case MarketDuration(path, cause) => requireParts(path, cause)
      case LimitPrice(path, cause)     => requireParts(path, cause)
      case DisplayedLots(path, cause)  => requireParts(path, cause)
      case Intent(path, cause)         => requireParts(path, cause)

  private def requireParts(path: WirePath, cause: AnyRef): Unit =
    Objects.requireNonNull(path, "order refinement path")
    val _ = Objects.requireNonNull(cause, "order refinement cause")
end OrderRefinementFailure

/** Non-empty deterministic collection of independent local order-refinement failures. */
@nowarn("msg=Ignoring.*qualifier")
final class OrderRefinementFailures private[this] (
  val head: OrderRefinementFailure,
  val tail: Vector[OrderRefinementFailure])
  extends JavaSerializationUnsupported:

  val failures: Vector[OrderRefinementFailure] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: OrderRefinementFailures => failures == that.failures
      case _                             => false

  override def hashCode: Int    = failures.hashCode
  override def toString: String = failures.mkString("OrderRefinementFailures(", ",", ")")
end OrderRefinementFailures

object OrderRefinementFailures:
  private val constructor =
    val owner = classOf[OrderRefinementFailures]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(
          java.lang.Void.TYPE,
          classOf[OrderRefinementFailure],
          classOf[Vector[?]]
        )
      )

  def one(head: OrderRefinementFailure): OrderRefinementFailures =
    construct(head, Vector.empty)

  def from(failures: Vector[OrderRefinementFailure]): Option[OrderRefinementFailures] =
    Objects.requireNonNull(failures, "order refinement failures") match
      case head +: tail => Some(construct(head, tail))
      case _            => None

  private def construct(
    head: OrderRefinementFailure,
    tail: Vector[OrderRefinementFailure]
  ): OrderRefinementFailures =
    constructor
      .invoke(
        Objects.requireNonNull(head, "order refinement failure"),
        Objects.requireNonNull(tail, "order refinement failure tail")
      )
      .asInstanceOf[OrderRefinementFailures]
end OrderRefinementFailures

/** Closed stages from stable order syntax to one validated immutable instruction. */
enum OrderReconstructionFailure extends JavaSerializationUnsupported:
  case Codec(violations: WireViolations[WireDecodeViolation])
  case ForeignInstrument(recorded: InstrumentId, supplied: InstrumentId)
  case Refinement(failures: OrderRefinementFailures)
  case Validation(violations: OrderViolations)

  private val _ =
    this match
      case Codec(violations)                     => Objects.requireNonNull(violations, "order codec violations")
      case ForeignInstrument(recorded, supplied) =>
        Objects.requireNonNull(recorded, "recorded order instrument ID")
        Objects.requireNonNull(supplied, "supplied order instrument ID")
        require(recorded != supplied, "foreign instrument failure requires distinct IDs")
      case Refinement(failures)   => Objects.requireNonNull(failures, "order refinement failures")
      case Validation(violations) => Objects.requireNonNull(violations, "order validation violations")
end OrderReconstructionFailure

/** One failed record in an all-valid-or-errors immutable-order batch. */
final case class IndexedOrderReconstructionFailure(recordIndex: Int, failure: OrderReconstructionFailure)
  extends JavaSerializationUnsupported:

  require(recordIndex >= 0, "order record index must be nonnegative")
  Objects.requireNonNull(failure, "order reconstruction failure")
end IndexedOrderReconstructionFailure

/** Frozen stable-data representation and smart-constructor reconstruction boundary for immutable orders. */
object OrderRecord:
  enum Side extends JavaSerializationUnsupported:
    case Buy, Sell

  enum PositionEffect extends JavaSerializationUnsupported:
    case Unrestricted, ReduceOnly

  enum PriceReference extends JavaSerializationUnsupported:
    case Last, Mark, Index

  enum TriggerComparison extends JavaSerializationUnsupported:
    case AtOrAbove, AtOrBelow

  enum TimeInForce extends JavaSerializationUnsupported:
    case GoodTillCancelled, ImmediateOrCancel, FillOrKill, Day

  enum LiquidityConstraint extends JavaSerializationUnsupported:
    case Unrestricted, MakerOnly

  sealed trait Activation extends JavaSerializationUnsupported

  object Activation:
    case object Immediate extends Activation

    final case class Fixed(
      reference: PriceReference,
      comparison: TriggerComparison,
      triggerPriceCoordinate: BigInt)
      extends Activation:
      Objects.requireNonNull(reference, "fixed activation reference")
      Objects.requireNonNull(comparison, "fixed activation comparison")
      Objects.requireNonNull(triggerPriceCoordinate, "fixed activation trigger-price coordinate")
    end Fixed

    final case class Trailing(
      reference: PriceReference,
      comparison: TriggerComparison,
      offsetTicks: BigInt)
      extends Activation:
      Objects.requireNonNull(reference, "trailing activation reference")
      Objects.requireNonNull(comparison, "trailing activation comparison")
      Objects.requireNonNull(offsetTicks, "trailing activation offset")
    end Trailing
  end Activation

  sealed trait Pricing extends JavaSerializationUnsupported

  object Pricing:
    final case class Limit(priceCoordinate: BigInt) extends Pricing:
      Objects.requireNonNull(priceCoordinate, "limit price coordinate")
    end Limit

    final case class Pegged(reference: PriceReference, offsetTicks: BigInt) extends Pricing:
      Objects.requireNonNull(reference, "peg reference")
      Objects.requireNonNull(offsetTicks, "peg offset")
    end Pegged
  end Pricing

  sealed trait Visibility extends JavaSerializationUnsupported

  object Visibility:
    case object Displayed extends Visibility
    case object Hidden    extends Visibility

    final case class Iceberg(displayedLotsCoordinate: BigInt) extends Visibility:
      Objects.requireNonNull(displayedLotsCoordinate, "iceberg displayed-lots coordinate")
    end Iceberg
  end Visibility

  sealed trait Execution extends JavaSerializationUnsupported

  object Execution:
    final case class Market(timeInForce: TimeInForce) extends Execution:
      Objects.requireNonNull(timeInForce, "market time in force")
    end Market

    final case class Priced(
      pricing: Pricing,
      timeInForce: TimeInForce,
      liquidityConstraint: LiquidityConstraint,
      visibility: Visibility)
      extends Execution:
      Objects.requireNonNull(pricing, "priced execution pricing")
      Objects.requireNonNull(timeInForce, "priced execution time in force")
      Objects.requireNonNull(liquidityConstraint, "priced execution liquidity constraint")
      Objects.requireNonNull(visibility, "priced execution visibility")
    end Priced
  end Execution

  final case class V1(
    instrumentId: InstrumentId,
    side: Side,
    lotCoordinate: BigInt,
    positionEffect: PositionEffect,
    activation: Activation,
    execution: Execution)
    extends JavaSerializationUnsupported:

    Objects.requireNonNull(instrumentId, "order instrument ID")
    Objects.requireNonNull(side, "order side")
    Objects.requireNonNull(lotCoordinate, "order lot coordinate")
    Objects.requireNonNull(positionEffect, "order position effect")
    Objects.requireNonNull(activation, "order activation")
    Objects.requireNonNull(execution, "order execution")
  end V1

  val recordType: RecordType       = CodecRecordTypes.order
  val schemaVersion: SchemaVersion = SchemaVersion.one

  private val sideSchema: WireSchema[Side] = closedText(
    "side",
    {
      case Side.Buy  => "buy"
      case Side.Sell => "sell"
    },
    {
      case "buy"  => Some(Side.Buy)
      case "sell" => Some(Side.Sell)
      case _      => None
    }
  )

  private val positionEffectSchema: WireSchema[PositionEffect] = closedText(
    "position-effect",
    {
      case PositionEffect.Unrestricted => "unrestricted"
      case PositionEffect.ReduceOnly   => "reduceOnly"
    },
    {
      case "unrestricted" => Some(PositionEffect.Unrestricted)
      case "reduceOnly"   => Some(PositionEffect.ReduceOnly)
      case _              => None
    }
  )

  private val priceReferenceSchema: WireSchema[PriceReference] = closedText(
    "price-reference",
    {
      case PriceReference.Last  => "last"
      case PriceReference.Mark  => "mark"
      case PriceReference.Index => "index"
    },
    {
      case "last"  => Some(PriceReference.Last)
      case "mark"  => Some(PriceReference.Mark)
      case "index" => Some(PriceReference.Index)
      case _       => None
    }
  )

  private val triggerComparisonSchema: WireSchema[TriggerComparison] = closedText(
    "trigger-comparison",
    {
      case TriggerComparison.AtOrAbove => "atOrAbove"
      case TriggerComparison.AtOrBelow => "atOrBelow"
    },
    {
      case "atOrAbove" => Some(TriggerComparison.AtOrAbove)
      case "atOrBelow" => Some(TriggerComparison.AtOrBelow)
      case _           => None
    }
  )

  private val timeInForceSchema: WireSchema[TimeInForce] = closedText(
    "time-in-force",
    {
      case TimeInForce.GoodTillCancelled => "goodTillCancelled"
      case TimeInForce.ImmediateOrCancel => "immediateOrCancel"
      case TimeInForce.FillOrKill        => "fillOrKill"
      case TimeInForce.Day               => "day"
    },
    {
      case "goodTillCancelled" => Some(TimeInForce.GoodTillCancelled)
      case "immediateOrCancel" => Some(TimeInForce.ImmediateOrCancel)
      case "fillOrKill"        => Some(TimeInForce.FillOrKill)
      case "day"               => Some(TimeInForce.Day)
      case _                   => None
    }
  )

  private val liquidityConstraintSchema: WireSchema[LiquidityConstraint] = closedText(
    "liquidity-constraint",
    {
      case LiquidityConstraint.Unrestricted => "unrestricted"
      case LiquidityConstraint.MakerOnly    => "makerOnly"
    },
    {
      case "unrestricted" => Some(LiquidityConstraint.Unrestricted)
      case "makerOnly"    => Some(LiquidityConstraint.MakerOnly)
      case _              => None
    }
  )

  private val activationSchema: WireSchema[Activation] =
    val fixed =
      WireRecord
        .field("reference", priceReferenceSchema)
        .product(WireRecord.field("comparison", triggerComparisonSchema))
        .product(WireRecord.field("triggerPriceCoordinate", ExactWire.canonicalInteger))
    val trailing =
      WireRecord
        .field("reference", priceReferenceSchema)
        .product(WireRecord.field("comparison", triggerComparisonSchema))
        .product(WireRecord.field("offsetTicks", ExactWire.canonicalInteger))
    WireSchema.tagged(
      "kind",
      Vector(
        WireCase[Activation, Unit]("immediate", WireRecord.unit) {
          case Activation.Immediate => Some(())
          case _                    => None
        }(_ => Activation.Immediate),
        WireCase[Activation, ((PriceReference, TriggerComparison), BigInt)]("fixed", fixed) {
          case Activation.Fixed(reference, comparison, coordinate) => Some(reference -> comparison -> coordinate)
          case _                                                   => None
        }(value => Activation.Fixed(value._1._1, value._1._2, value._2)),
        WireCase[Activation, ((PriceReference, TriggerComparison), BigInt)]("trailing", trailing) {
          case Activation.Trailing(reference, comparison, offset) => Some(reference -> comparison -> offset)
          case _                                                  => None
        }(value => Activation.Trailing(value._1._1, value._1._2, value._2))
      )
    )
  end activationSchema

  private val pricingSchema: WireSchema[Pricing] =
    WireSchema.tagged(
      "kind",
      Vector(
        WireCase[Pricing, BigInt](
          "limit",
          WireRecord.field("priceCoordinate", ExactWire.canonicalInteger)
        ) {
          case Pricing.Limit(coordinate) => Some(coordinate)
          case _                         => None
        }(Pricing.Limit.apply),
        WireCase[Pricing, (PriceReference, BigInt)](
          "pegged",
          WireRecord
            .field("reference", priceReferenceSchema)
            .product(WireRecord.field("offsetTicks", ExactWire.canonicalInteger))
        ) {
          case Pricing.Pegged(reference, offset) => Some(reference -> offset)
          case _                                 => None
        }(value => Pricing.Pegged(value._1, value._2))
      )
    )

  private val visibilitySchema: WireSchema[Visibility] =
    WireSchema.tagged(
      "kind",
      Vector(
        WireCase[Visibility, Unit]("displayed", WireRecord.unit) {
          case Visibility.Displayed => Some(())
          case _                    => None
        }(_ => Visibility.Displayed),
        WireCase[Visibility, Unit]("hidden", WireRecord.unit) {
          case Visibility.Hidden => Some(())
          case _                 => None
        }(_ => Visibility.Hidden),
        WireCase[Visibility, BigInt](
          "iceberg",
          WireRecord.field("displayedLotsCoordinate", ExactWire.canonicalInteger)
        ) {
          case Visibility.Iceberg(coordinate) => Some(coordinate)
          case _                              => None
        }(Visibility.Iceberg.apply)
      )
    )

  private val executionSchema: WireSchema[Execution] =
    val priced =
      WireRecord
        .field("pricing", pricingSchema)
        .product(WireRecord.field("timeInForce", timeInForceSchema))
        .product(WireRecord.field("liquidityConstraint", liquidityConstraintSchema))
        .product(WireRecord.field("visibility", visibilitySchema))
    WireSchema.tagged(
      "kind",
      Vector(
        WireCase[Execution, TimeInForce]("market", WireRecord.field("timeInForce", timeInForceSchema)) {
          case Execution.Market(timeInForce) => Some(timeInForce)
          case _                             => None
        }(Execution.Market.apply),
        WireCase[
          Execution,
          (((Pricing, TimeInForce), LiquidityConstraint), Visibility)
        ]("priced", priced) {
          case Execution.Priced(pricing, timeInForce, liquidityConstraint, visibility) =>
            Some(pricing -> timeInForce -> liquidityConstraint -> visibility)
          case _ => None
        }(value => Execution.Priced(value._1._1._1, value._1._1._2, value._1._2, value._2))
      )
    )
  end executionSchema

  private val v1Schema: WireSchema[V1] =
    val representation =
      WireRecord
        .field("instrumentId", ExactWire.instrumentId)
        .product(WireRecord.field("side", sideSchema))
        .product(WireRecord.field("lotCoordinate", ExactWire.canonicalInteger))
        .product(WireRecord.field("positionEffect", positionEffectSchema))
        .product(WireRecord.field("activation", activationSchema))
        .product(WireRecord.field("execution", executionSchema))
        .imap(value =>
          V1(value._1._1._1._1._1, value._1._1._1._1._2, value._1._1._1._2, value._1._1._2,
            value._1._2, value._2)
        )(value =>
          (((((value.instrumentId, value.side), value.lotCoordinate), value.positionEffect), value.activation),
            value.execution)
        )
    WireSchema.record(representation)

  private val codec = EnvelopeCodec(
    recordType,
    schemaVersion,
    Vector(schemaVersion -> v1Schema),
    CodecRecordTypes.otherThan(recordType)
  )

  /** Project one canonical immutable order to stable primitive data without derived position or context. */
  def fromOrder(order: Order[?, ?, ?]): V1 =
    val checked = Objects.requireNonNull(order, "order")
    V1(
      checked.instrumentId,
      fromDomainSide(checked.intent.side),
      checked.intent.lots.count.unrefined,
      fromDomainPositionEffect(checked.intent.positionEffect),
      fromDomainActivation(checked.activation),
      fromDomainExecution(checked.execution)
    )

  def encode(record: V1): Either[WireViolations[WireEncodeViolation], String] =
    codec.write(Objects.requireNonNull(record, "order record"))

  def encodeOrder(order: Order[?, ?, ?]): Either[WireViolations[WireEncodeViolation], String] =
    encode(fromOrder(order))

  def parse(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], V1] =
    codec.read(input, limits, recordIndex)

  /** Rebuild local refinements and delegate final compatibility checks to the canonical order constructor. */
  def reconstruct[I <: Instrument](
    record: V1,
    instrument: I
  ): Either[OrderReconstructionFailure, Order[?, ?, ?]] =
    val checkedRecord     = Objects.requireNonNull(record, "order record")
    val checkedInstrument = Objects.requireNonNull(instrument, "order instrument")
    if checkedRecord.instrumentId != checkedInstrument.identity.id then
      Left(
        OrderReconstructionFailure.ForeignInstrument(
          checkedRecord.instrumentId,
          checkedInstrument.identity.id
        )
      )
    else reconstructForInstrument(checkedRecord, checkedInstrument)

  /** Keep structural syntax, local refinement, and aggregate order validation as closed separate stages. */
  def decodeAndReconstruct[I <: Instrument](
    input: String,
    instrument: I,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[OrderReconstructionFailure, Order[?, ?, ?]] =
    parse(input, limits, recordIndex)
      .left
      .map(OrderReconstructionFailure.Codec.apply)
      .flatMap(record => reconstruct(record, instrument))

  /** Evaluate every independent record against one explicit instrument and expose no partial-success vector. */
  def reconstructBatch[I <: Instrument](
    inputs: Vector[String],
    instrument: I,
    limits: DecodeLimits = DecodeLimits.default
  ): Either[WireViolations[IndexedOrderReconstructionFailure], Vector[Order[?, ?, ?]]] =
    val checkedInputs     = Objects.requireNonNull(inputs, "order inputs")
    val checkedInstrument = Objects.requireNonNull(instrument, "order instrument")
    val checkedLimits     = Objects.requireNonNull(limits, "order decode limits")
    if checkedInputs.size > checkedLimits.maxBatchRecords then
      Left(
        WireViolations.one(
          IndexedOrderReconstructionFailure(
            0,
            OrderReconstructionFailure.Codec(
              WireViolations.one(
                WireDecodeViolation.Limit(
                  WireLimitViolation(
                    DecodeLimit.BatchRecords,
                    checkedInputs.size.toLong,
                    checkedLimits.maxBatchRecords,
                    WirePath.root,
                    0
                  )
                )
              )
            )
          )
        )
      )
    else
      val results = checkedInputs.zipWithIndex.map: (input, index) =>
        decodeAndReconstruct(
          Objects.requireNonNull(input, s"order input $index"),
          checkedInstrument,
          checkedLimits,
          index
        ).left.map(failure => IndexedOrderReconstructionFailure(index, failure))
      val failures = results.collect:
        case Left(failure) => failure
      WireViolations.fromVector(failures) match
        case Some(errors) => Left(errors)
        case None         => Right(results.collect { case Right(order) => order })
    end if
  end reconstructBatch

  def schema(
    id: String = "urn:trading:codec:schema:order:v1",
    definitionName: String = "OrderRecordV1"
  ): Either[WireViolations[WireEncodeViolation], String] =
    codec.schema(id, definitionName)

  private def reconstructForInstrument[I <: Instrument](
    record: V1,
    instrument: I
  ): Either[OrderReconstructionFailure, Order[?, ?, ?]] =
    type D = instrument.roles.position.D
    type B = instrument.roles.base.D
    type Q = instrument.roles.quote.D

    val lotsResult: Either[OrderRefinementFailure, instrument.Lots] =
      Lots
        .fromCount(instrument)(record.lotCoordinate)
        .left
        .map(OrderRefinementFailure.Lots(payloadPath.field("lotCoordinate"), _))
    val activationResult: Either[Vector[OrderRefinementFailure], OrderActivation[B, Q]] =
      buildActivation(instrument, record.activation)
    val executionResult: Either[Vector[OrderRefinementFailure], OrderExecution[D, B, Q]] =
      buildExecution(instrument, record.execution)
    val failures =
      lotsResult.left.toOption.toVector ++ activationResult.left.toOption.toVector.flatten ++
        executionResult.left.toOption.toVector.flatten

    (lotsResult, activationResult, executionResult) match
      case (Right(lots), Right(activation), Right(execution)) =>
        OrderIntent
          .create(instrument)(toDomainSide(record.side), lots, toDomainPositionEffect(record.positionEffect))
          .left
          .map(cause =>
            OrderReconstructionFailure.Refinement(
              OrderRefinementFailures.one(OrderRefinementFailure.Intent(payloadPath, cause))
            )
          )
          .flatMap(intent =>
            Order
              .create[D, B, Q, OrderActivation[B, Q], OrderExecution[D, B, Q]](instrument)(
                intent,
                activation,
                execution
              )
              .left
              .map(OrderReconstructionFailure.Validation.apply)
          )
      case _ =>
        OrderRefinementFailures.from(failures) match
          case Some(errors) => Left(OrderReconstructionFailure.Refinement(errors))
          case None         =>
            throw new IllegalStateException("failed order reconstruction did not retain a refinement failure")
    end match
  end reconstructForInstrument

  private def buildActivation[I <: Instrument](
    instrument: I,
    activation: Activation
  ): Either[
    Vector[OrderRefinementFailure],
    OrderActivation[instrument.roles.base.D, instrument.roles.quote.D]
  ] =
    type B = instrument.roles.base.D
    type Q = instrument.roles.quote.D
    activation match
      case Activation.Immediate                                => Right(ImmediateActivation[B, Q]())
      case Activation.Fixed(reference, comparison, coordinate) =>
        priceFromCoordinate(instrument)(coordinate)
          .left
          .map(cause =>
            Vector(OrderRefinementFailure.TriggerPrice(
              payloadPath.field("activation").field(
                "triggerPriceCoordinate"
              ),
              cause
            ))
          )
          .map(price => FixedActivation(toDomainPriceReference(reference), toDomainComparison(comparison), price))
      case Activation.Trailing(reference, comparison, offset) =>
        TrailingActivation
          .create[B, Q](toDomainPriceReference(reference), toDomainComparison(comparison), offset)
          .left
          .map(cause =>
            Vector(OrderRefinementFailure.TrailingOffset(
              payloadPath.field("activation").field("offsetTicks"),
              cause
            ))
          )
    end match
  end buildActivation

  private def buildExecution[I <: Instrument](
    instrument: I,
    execution: Execution
  ): Either[
    Vector[OrderRefinementFailure],
    OrderExecution[instrument.roles.position.D, instrument.roles.base.D, instrument.roles.quote.D]
  ] =
    type D = instrument.roles.position.D
    type B = instrument.roles.base.D
    type Q = instrument.roles.quote.D
    execution match
      case Execution.Market(timeInForce) =>
        NonRestingTimeInForce
          .from(toDomainTimeInForce(timeInForce))
          .left
          .map(cause =>
            Vector(OrderRefinementFailure.MarketDuration(
              payloadPath.field("execution").field("timeInForce"),
              cause
            ))
          )
          .map(value => MarketExecution[D, B, Q](value))
      case Execution.Priced(pricing, timeInForce, liquidityConstraint, visibility) =>
        val pricingResult: Either[OrderRefinementFailure, OrderPricing[B, Q]] =
          pricing match
            case Pricing.Limit(coordinate) =>
              priceFromCoordinate(instrument)(coordinate)
                .left
                .map(OrderRefinementFailure.LimitPrice(
                  payloadPath.field("execution").field("pricing").field("priceCoordinate"),
                  _
                ))
                .map(LimitPricing.apply)
            case Pricing.Pegged(reference, offset) =>
              Right(PeggedPricing[B, Q](toDomainPriceReference(reference), offset))
        val visibilityResult: Either[OrderRefinementFailure, PricedVisibility[D]] =
          visibility match
            case Visibility.Displayed           => Right(DisplayedVisibility)
            case Visibility.Hidden              => Right(HiddenVisibility)
            case Visibility.Iceberg(coordinate) =>
              Lots
                .fromCount(instrument)(coordinate)
                .left
                .map(OrderRefinementFailure.DisplayedLots(
                  payloadPath.field("execution").field("visibility").field("displayedLotsCoordinate"),
                  _
                ))
                .map(IcebergVisibility.apply)
        val failures = pricingResult.left.toOption.toVector ++ visibilityResult.left.toOption.toVector
        (pricingResult, visibilityResult) match
          case (Right(checkedPricing), Right(checkedVisibility)) =>
            Right(
              PricedExecution[D, B, Q, OrderPricing[B, Q]](
                checkedPricing,
                toDomainTimeInForce(timeInForce),
                toDomainLiquidityConstraint(liquidityConstraint),
                checkedVisibility
              )
            )
          case _ => Left(failures)
    end match
  end buildExecution

  private def priceFromCoordinate[I <: Instrument](
    instrument: I
  )(
    coordinate: BigInt
  ): Either[PriceError, instrument.Price] =
    val gridValue   = instrument.priceGrid.fromCoordinate(coordinate)
    val coefficient = instrument.priceGrid.asQuantity(gridValue).coefficient
    Price.exact(instrument)(coefficient).left.map:
      case InvalidPriceCoordinate(_) => InvalidPriceCoordinate(coordinate)
      case other                     => other

  private def fromDomainActivation(value: OrderActivation[?, ?]): Activation =
    value match
      case _: ImmediateActivation[?, ?]                         => Activation.Immediate
      case FixedActivation(reference, comparison, triggerPrice) =>
        Activation.Fixed(
          fromDomainPriceReference(reference),
          fromDomainComparison(comparison),
          triggerPrice.ticks.unrefined
        )
      case TrailingActivation(reference, comparison, offsetTicks) =>
        Activation.Trailing(
          fromDomainPriceReference(reference),
          fromDomainComparison(comparison),
          offsetTicks.unrefined
        )

  private def fromDomainExecution(value: OrderExecution[?, ?, ?]): Execution =
    value match
      case MarketExecution(timeInForce) => Execution.Market(fromDomainNonRestingTimeInForce(timeInForce))
      case PricedExecution(pricing, timeInForce, liquidityConstraint, visibility) =>
        Execution.Priced(
          fromDomainPricing(pricing),
          fromDomainTimeInForce(timeInForce),
          fromDomainLiquidityConstraint(liquidityConstraint),
          fromDomainVisibility(visibility)
        )

  private def fromDomainPricing(value: OrderPricing[?, ?]): Pricing =
    value match
      case LimitPricing(limit)                   => Pricing.Limit(limit.ticks.unrefined)
      case PeggedPricing(reference, offsetTicks) =>
        Pricing.Pegged(fromDomainPriceReference(reference), offsetTicks)

  private def fromDomainVisibility(value: PricedVisibility[?]): Visibility =
    value match
      case DisplayedVisibility              => Visibility.Displayed
      case HiddenVisibility                 => Visibility.Hidden
      case IcebergVisibility(displayedLots) => Visibility.Iceberg(displayedLots.count.unrefined)

  private def fromDomainSide(value: DomainSide): Side =
    value match
      case DomainSide.Buy  => Side.Buy
      case DomainSide.Sell => Side.Sell

  private def toDomainSide(value: Side): DomainSide =
    value match
      case Side.Buy  => DomainSide.Buy
      case Side.Sell => DomainSide.Sell

  private def fromDomainPositionEffect(value: DomainPositionEffect): PositionEffect =
    value match
      case DomainPositionEffect.Unrestricted => PositionEffect.Unrestricted
      case DomainPositionEffect.ReduceOnly   => PositionEffect.ReduceOnly

  private def toDomainPositionEffect(value: PositionEffect): DomainPositionEffect =
    value match
      case PositionEffect.Unrestricted => DomainPositionEffect.Unrestricted
      case PositionEffect.ReduceOnly   => DomainPositionEffect.ReduceOnly

  private def fromDomainPriceReference(value: DomainPriceReference): PriceReference =
    value match
      case DomainPriceReference.Last  => PriceReference.Last
      case DomainPriceReference.Mark  => PriceReference.Mark
      case DomainPriceReference.Index => PriceReference.Index

  private def toDomainPriceReference(value: PriceReference): DomainPriceReference =
    value match
      case PriceReference.Last  => DomainPriceReference.Last
      case PriceReference.Mark  => DomainPriceReference.Mark
      case PriceReference.Index => DomainPriceReference.Index

  private def fromDomainComparison(value: DomainTriggerComparison): TriggerComparison =
    value match
      case DomainTriggerComparison.AtOrAbove => TriggerComparison.AtOrAbove
      case DomainTriggerComparison.AtOrBelow => TriggerComparison.AtOrBelow

  private def toDomainComparison(value: TriggerComparison): DomainTriggerComparison =
    value match
      case TriggerComparison.AtOrAbove => DomainTriggerComparison.AtOrAbove
      case TriggerComparison.AtOrBelow => DomainTriggerComparison.AtOrBelow

  private def fromDomainTimeInForce(value: DomainTimeInForce): TimeInForce =
    value match
      case DomainTimeInForce.GoodTillCancelled => TimeInForce.GoodTillCancelled
      case DomainTimeInForce.ImmediateOrCancel => TimeInForce.ImmediateOrCancel
      case DomainTimeInForce.FillOrKill        => TimeInForce.FillOrKill
      case DomainTimeInForce.Day               => TimeInForce.Day

  private def fromDomainNonRestingTimeInForce(value: NonRestingTimeInForce): TimeInForce =
    value match
      case NonRestingTimeInForce.ImmediateOrCancel => TimeInForce.ImmediateOrCancel
      case NonRestingTimeInForce.FillOrKill        => TimeInForce.FillOrKill

  private def toDomainTimeInForce(value: TimeInForce): DomainTimeInForce =
    value match
      case TimeInForce.GoodTillCancelled => DomainTimeInForce.GoodTillCancelled
      case TimeInForce.ImmediateOrCancel => DomainTimeInForce.ImmediateOrCancel
      case TimeInForce.FillOrKill        => DomainTimeInForce.FillOrKill
      case TimeInForce.Day               => DomainTimeInForce.Day

  private def fromDomainLiquidityConstraint(value: DomainLiquidityConstraint): LiquidityConstraint =
    value match
      case DomainLiquidityConstraint.Unrestricted => LiquidityConstraint.Unrestricted
      case DomainLiquidityConstraint.MakerOnly    => LiquidityConstraint.MakerOnly

  private def toDomainLiquidityConstraint(value: LiquidityConstraint): DomainLiquidityConstraint =
    value match
      case LiquidityConstraint.Unrestricted => DomainLiquidityConstraint.Unrestricted
      case LiquidityConstraint.MakerOnly    => DomainLiquidityConstraint.MakerOnly

  private def closedText[A](
    name: String,
    encode: A => String,
    decode: String => Option[A]
  ): WireSchema[A] =
    WireSchema.text.refine[A]((supplied, context) =>
      decode(supplied).toRight(
        WireDecodeViolation.InvalidValue(context.path, s"unknown-$name:$supplied", context.recordIndex)
      )
    )(encode)

  private val payloadPath: WirePath = WirePath.root.field("payload")
end OrderRecord
