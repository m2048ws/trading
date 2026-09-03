package trading.codec

import java.util.Objects

import trading.economics.instrument.*
import trading.order.*
import trading.quantity.*
import trading.reference.*
import trading.scenario.*

/** Closed observation shapes associated with the immutable instruction alternatives. */
enum ScenarioEvidenceShape extends JavaSerializationUnsupported:
  case ImmediateActivation, FixedActivation, TrailingActivation
  case DirectPricing, PeggedPricing

/** One typed, located failure while rebuilding associated evidence and matched liquidity. */
enum ScenarioPreparationFailure extends JavaSerializationUnsupported:
  case EvidenceShape(path: WirePath, expected: ScenarioEvidenceShape, supplied: ScenarioEvidenceShape)
  case Price(path: WirePath, cause: PriceError)
  case Activation(path: WirePath, cause: ActivationViolation)
  case Pricing(path: WirePath, cause: PricingViolation)
  case Lots(path: WirePath, cause: LotError)
  case Catalog(path: WirePath, source: AssetId, cause: CatalogLookupError)
  case Conversion(path: WirePath, cause: MarketStateViolation)
  case Market(path: WirePath, cause: MarketStateViolations)
  case Slice(path: WirePath, cause: ScenarioViolations)
  case MatchedSlices(path: WirePath, cause: ScenarioViolation)

  private val _ =
    this match
      case EvidenceShape(path, expected, supplied) =>
        requireParts(path, expected, supplied)
        require(expected != supplied, "scenario evidence-shape failure requires distinct shapes")
      case Price(path, cause)           => requireParts(path, cause)
      case Activation(path, cause)      => requireParts(path, cause)
      case Pricing(path, cause)         => requireParts(path, cause)
      case Lots(path, cause)            => requireParts(path, cause)
      case Catalog(path, source, cause) => requireParts(path, source, cause)
      case Conversion(path, cause)      => requireParts(path, cause)
      case Market(path, cause)          => requireParts(path, cause)
      case Slice(path, cause)           => requireParts(path, cause)
      case MatchedSlices(path, cause)   => requireParts(path, cause)

  private def requireParts(path: WirePath, values: AnyRef*): Unit =
    Objects.requireNonNull(path, "scenario preparation path")
    values.foreach(value => Objects.requireNonNull(value, "scenario preparation detail"))
end ScenarioPreparationFailure

/** Stable non-empty ordering of independent scenario-preparation failures. */
final class ScenarioPreparationFailures private (
  val head: ScenarioPreparationFailure,
  val tail: Vector[ScenarioPreparationFailure])
  extends JavaSerializationUnsupported:

  val failures: Vector[ScenarioPreparationFailure] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: ScenarioPreparationFailures => failures == that.failures
      case _                                 => false

  override def hashCode: Int    = failures.hashCode
  override def toString: String = failures.mkString("ScenarioPreparationFailures(", ",", ")")
end ScenarioPreparationFailures

object ScenarioPreparationFailures:
  def one(head: ScenarioPreparationFailure): ScenarioPreparationFailures =
    construct(head, Vector.empty)

  def from(failures: Vector[ScenarioPreparationFailure]): Option[ScenarioPreparationFailures] =
    Objects.requireNonNull(failures, "scenario preparation failures") match
      case head +: tail => Some(construct(head, tail))
      case _            => None

  private def construct(
    head: ScenarioPreparationFailure,
    tail: Vector[ScenarioPreparationFailure]
  ): ScenarioPreparationFailures =
    new ScenarioPreparationFailures(
      Objects.requireNonNull(head, "scenario preparation failure"),
      Objects.requireNonNull(tail, "scenario preparation failure tail")
    )
end ScenarioPreparationFailures

/** Closed stages from one hypothetical scenario record to canonical evaluation. */
enum OrderScenarioReconstructionFailure extends JavaSerializationUnsupported:
  case Codec(violations: WireViolations[WireDecodeViolation])
  case Order(cause: OrderReconstructionFailure)
  case Preparation(failures: ScenarioPreparationFailures)
  case Validation(violations: ScenarioViolations)

  private val _ =
    this match
      case Codec(violations)      => Objects.requireNonNull(violations, "scenario codec violations")
      case Order(cause)           => Objects.requireNonNull(cause, "scenario order failure")
      case Preparation(failures)  => Objects.requireNonNull(failures, "scenario preparation failures")
      case Validation(violations) => Objects.requireNonNull(violations, "scenario validation violations")
end OrderScenarioReconstructionFailure

/** One indexed hypothetical-scenario failure in an atomic encoded batch. */
final case class IndexedOrderScenarioReconstructionFailure(
  recordIndex: Int,
  failure: OrderScenarioReconstructionFailure)
  extends JavaSerializationUnsupported:
  require(recordIndex >= 0, "scenario record index must be nonnegative")
  Objects.requireNonNull(failure, "indexed scenario reconstruction failure")
end IndexedOrderScenarioReconstructionFailure

/** One failed entry/exit branch in stable leg order. */
final case class RoundTripLegReconstructionFailure(
  leg: RoundTripLeg,
  cause: OrderScenarioReconstructionFailure)
  extends JavaSerializationUnsupported:
  Objects.requireNonNull(leg, "round-trip reconstruction leg")
  Objects.requireNonNull(cause, "round-trip leg reconstruction cause")
end RoundTripLegReconstructionFailure

/** Stable non-empty entry-before-exit reconstruction diagnostics. */
final class RoundTripLegReconstructionFailures private (
  val head: RoundTripLegReconstructionFailure,
  val tail: Vector[RoundTripLegReconstructionFailure])
  extends JavaSerializationUnsupported:

  val failures: Vector[RoundTripLegReconstructionFailure] = head +: tail

  override def equals(other: Any): Boolean =
    other match
      case that: RoundTripLegReconstructionFailures => failures == that.failures
      case _                                        => false

  override def hashCode: Int    = failures.hashCode
  override def toString: String = failures.mkString("RoundTripLegReconstructionFailures(", ",", ")")
end RoundTripLegReconstructionFailures

object RoundTripLegReconstructionFailures:
  def from(
    failures: Vector[RoundTripLegReconstructionFailure]
  ): Option[RoundTripLegReconstructionFailures] =
    Objects.requireNonNull(failures, "round-trip leg reconstruction failures") match
      case head +: tail => Some(construct(head, tail))
      case _            => None

  private def construct(
    head: RoundTripLegReconstructionFailure,
    tail: Vector[RoundTripLegReconstructionFailure]
  ): RoundTripLegReconstructionFailures =
    new RoundTripLegReconstructionFailures(
      Objects.requireNonNull(head, "round-trip leg reconstruction failure"),
      Objects.requireNonNull(tail, "round-trip leg reconstruction failure tail")
    )
end RoundTripLegReconstructionFailures

/** Closed stages from entry × exit syntax to the canonical flat-position proof. */
enum RoundTripScenarioReconstructionFailure extends JavaSerializationUnsupported:
  case Codec(violations: WireViolations[WireDecodeViolation])
  case Legs(failures: RoundTripLegReconstructionFailures)
  case Validation(cause: RoundTripViolation)

  private val _ =
    this match
      case Codec(violations) => Objects.requireNonNull(violations, "round-trip codec violations")
      case Legs(failures)    => Objects.requireNonNull(failures, "round-trip leg failures")
      case Validation(cause) => Objects.requireNonNull(cause, "round-trip validation failure")
end RoundTripScenarioReconstructionFailure

/** One indexed round-trip failure in an atomic encoded batch. */
final case class IndexedRoundTripScenarioReconstructionFailure(
  recordIndex: Int,
  failure: RoundTripScenarioReconstructionFailure)
  extends JavaSerializationUnsupported:
  require(recordIndex >= 0, "round-trip record index must be nonnegative")
  Objects.requireNonNull(failure, "indexed round-trip reconstruction failure")
end IndexedRoundTripScenarioReconstructionFailure

/** Frozen stable assumptions for one complete hypothetical order scenario. */
object OrderScenarioRecord:
  sealed trait ActivationEvidence extends JavaSerializationUnsupported

  object ActivationEvidence:
    case object Immediate extends ActivationEvidence

    final case class Fixed(observedPriceCoordinate: BigInt) extends ActivationEvidence:
      Objects.requireNonNull(observedPriceCoordinate, "fixed observed-price coordinate")
    end Fixed

    final case class Trailing(
      favorableExtremePriceCoordinate: BigInt,
      observedPriceCoordinate: BigInt)
      extends ActivationEvidence:
      Objects.requireNonNull(favorableExtremePriceCoordinate, "trailing favorable-extreme coordinate")
      Objects.requireNonNull(observedPriceCoordinate, "trailing observed-price coordinate")
    end Trailing
  end ActivationEvidence

  sealed trait PricingResolution extends JavaSerializationUnsupported

  object PricingResolution:
    case object Direct extends PricingResolution

    final case class Pegged(
      referencePriceCoordinate: BigInt,
      resolvedLimitCoordinate: BigInt)
      extends PricingResolution:
      Objects.requireNonNull(referencePriceCoordinate, "peg reference-price coordinate")
      Objects.requireNonNull(resolvedLimitCoordinate, "peg resolved-limit coordinate")
    end Pegged
  end PricingResolution

  enum Liquidity extends JavaSerializationUnsupported:
    case Maker, Taker

  final case class AdditionalConversion(sourceAssetId: AssetId, sourceToSettle: Rational)
    extends JavaSerializationUnsupported:
    Objects.requireNonNull(sourceAssetId, "additional conversion source asset ID")
    Objects.requireNonNull(sourceToSettle, "additional conversion rate")
  end AdditionalConversion

  final case class Market(
    priceCoordinate: BigInt,
    baseToSettle: Rational,
    quoteToSettle: Rational,
    additionalConversions: Vector[AdditionalConversion])
    extends JavaSerializationUnsupported:
    Objects.requireNonNull(priceCoordinate, "scenario market price coordinate")
    Objects.requireNonNull(baseToSettle, "scenario market base-to-settle rate")
    Objects.requireNonNull(quoteToSettle, "scenario market quote-to-settle rate")
    Objects.requireNonNull(additionalConversions, "scenario additional conversions")
  end Market

  final case class Slice(lotCoordinate: BigInt, liquidity: Liquidity, market: Market)
    extends JavaSerializationUnsupported:
    Objects.requireNonNull(lotCoordinate, "scenario slice lot coordinate")
    Objects.requireNonNull(liquidity, "scenario slice liquidity")
    Objects.requireNonNull(market, "scenario slice market")
  end Slice

  final case class V1(
    order: OrderRecord.V1,
    activationEvidence: ActivationEvidence,
    pricingResolution: PricingResolution,
    slices: Vector[Slice])
    extends JavaSerializationUnsupported:
    Objects.requireNonNull(order, "scenario order")
    Objects.requireNonNull(activationEvidence, "scenario activation evidence")
    Objects.requireNonNull(pricingResolution, "scenario pricing resolution")
    Objects.requireNonNull(slices, "scenario slices")
  end V1

  val recordType: RecordType       = CodecRecordTypes.orderScenario
  val schemaVersion: SchemaVersion = SchemaVersion.one

  private val activationEvidenceSchema: WireSchema[ActivationEvidence] =
    WireSchema.tagged(
      "kind",
      Vector(
        WireCase[ActivationEvidence, Unit]("immediate", WireRecord.unit) {
          case ActivationEvidence.Immediate => Some(())
          case _                            => None
        }(_ => ActivationEvidence.Immediate),
        WireCase[ActivationEvidence, BigInt](
          "fixed",
          WireRecord.field("observedPriceCoordinate", ExactWire.canonicalInteger)
        ) {
          case ActivationEvidence.Fixed(value) => Some(value)
          case _                               => None
        }(ActivationEvidence.Fixed.apply),
        WireCase[ActivationEvidence, (BigInt, BigInt)](
          "trailing",
          WireRecord
            .field("favorableExtremePriceCoordinate", ExactWire.canonicalInteger)
            .product(WireRecord.field("observedPriceCoordinate", ExactWire.canonicalInteger))
        ) {
          case ActivationEvidence.Trailing(extreme, observed) => Some(extreme -> observed)
          case _                                              => None
        }(value => ActivationEvidence.Trailing(value._1, value._2))
      )
    )

  private val pricingResolutionSchema: WireSchema[PricingResolution] =
    WireSchema.tagged(
      "kind",
      Vector(
        WireCase[PricingResolution, Unit]("direct", WireRecord.unit) {
          case PricingResolution.Direct => Some(())
          case _                        => None
        }(_ => PricingResolution.Direct),
        WireCase[PricingResolution, (BigInt, BigInt)](
          "pegged",
          WireRecord
            .field("referencePriceCoordinate", ExactWire.canonicalInteger)
            .product(WireRecord.field("resolvedLimitCoordinate", ExactWire.canonicalInteger))
        ) {
          case PricingResolution.Pegged(reference, limit) => Some(reference -> limit)
          case _                                          => None
        }(value => PricingResolution.Pegged(value._1, value._2))
      )
    )

  private val liquiditySchema: WireSchema[Liquidity] =
    closedText(
      "liquidity",
      {
        case Liquidity.Maker => "maker"
        case Liquidity.Taker => "taker"
      },
      {
        case "maker" => Some(Liquidity.Maker)
        case "taker" => Some(Liquidity.Taker)
        case _       => None
      }
    )

  private val additionalConversionSchema: WireSchema[AdditionalConversion] =
    WireSchema.record(
      WireRecord
        .field("sourceAssetId", ExactWire.assetId)
        .product(WireRecord.field("sourceToSettle", ExactWire.rational))
        .imap(value => AdditionalConversion(value._1, value._2))(value =>
          value.sourceAssetId -> value.sourceToSettle
        )
    )

  private val marketSchema: WireSchema[Market] =
    WireSchema.record(
      WireRecord
        .field("priceCoordinate", ExactWire.canonicalInteger)
        .product(WireRecord.field("baseToSettle", ExactWire.rational))
        .product(WireRecord.field("quoteToSettle", ExactWire.rational))
        .product(
          WireRecord.field(
            "additionalConversions",
            WireSchema.vector(additionalConversionSchema, DecodeLimit.MarketConversions)
          )
        )
        .imap(value => Market(value._1._1._1, value._1._1._2, value._1._2, value._2))(value =>
          (((value.priceCoordinate, value.baseToSettle), value.quoteToSettle), value.additionalConversions)
        )
    )

  private val sliceSchema: WireSchema[Slice] =
    WireSchema.record(
      WireRecord
        .field("lotCoordinate", ExactWire.canonicalInteger)
        .product(WireRecord.field("liquidity", liquiditySchema))
        .product(WireRecord.field("market", marketSchema))
        .imap(value => Slice(value._1._1, value._1._2, value._2))(value =>
          ((value.lotCoordinate, value.liquidity), value.market)
        )
    )

  private[codec] val v1Schema: WireSchema[V1] =
    WireSchema.record(
      WireRecord
        .field("order", OrderRecord.v1Schema)
        .product(WireRecord.field("activationEvidence", activationEvidenceSchema))
        .product(WireRecord.field("pricingResolution", pricingResolutionSchema))
        .product(WireRecord.field("slices", WireSchema.vector(sliceSchema, DecodeLimit.ScenarioSlices)))
        .imap(value => V1(value._1._1._1, value._1._1._2, value._1._2, value._2))(value =>
          (((value.order, value.activationEvidence), value.pricingResolution), value.slices)
        )
    )

  private val codec = EnvelopeCodec(
    recordType,
    schemaVersion,
    Vector(schemaVersion -> v1Schema),
    CodecRecordTypes.otherThan(recordType)
  )

  /** Project only retained hypothetical assumptions; no execution fact or valuation is introduced. */
  def fromScenario[I <: Instrument](
    instrument: I
  )(
    scenario: OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): V1 =
    Objects.requireNonNull(instrument, "scenario instrument")
    val checked                = Objects.requireNonNull(scenario, "order scenario")
    val assumptions            = checked.assumptions
    val order                  = assumptions.order
    val activationObservations =
      assumptions.order.activation.observations(assumptions.activationEvidence)
    val pricingObservations =
      assumptions.order.execution.observations(assumptions.pricingResolution)

    val activation = order.activation match
      case _: ImmediateActivation[?, ?] => ActivationEvidence.Immediate
      case _: FixedActivation[?, ?]     =>
        val observed = requiredObservation(
          activationObservations,
          trading.order.ActivationObservation.Observed,
          "validated fixed activation"
        )
        ActivationEvidence.Fixed(observed.ticks.unrefined)
      case _: TrailingActivation[?, ?] =>
        val extreme = requiredObservation(
          activationObservations,
          trading.order.ActivationObservation.FavorableExtreme,
          "validated trailing activation extreme"
        )
        val observed = requiredObservation(
          activationObservations,
          trading.order.ActivationObservation.Observed,
          "validated trailing activation observation"
        )
        ActivationEvidence.Trailing(extreme.ticks.unrefined, observed.ticks.unrefined)

    val pricing = order.execution match
      case _: MarketExecution[?, ?, ?]            => PricingResolution.Direct
      case execution: PricedExecution[?, ?, ?, ?] =>
        execution.pricing match
          case _: LimitPricing[?, ?]  => PricingResolution.Direct
          case _: PeggedPricing[?, ?] =>
            val reference = requiredPricingObservation(
              pricingObservations,
              trading.order.PricingObservation.ReferencePrice,
              "validated peg reference"
            )
            val resolved = requiredPricingObservation(
              pricingObservations,
              trading.order.PricingObservation.ResolvedLimit,
              "validated peg resolved limit"
            )
            PricingResolution.Pegged(reference.ticks.unrefined, resolved.ticks.unrefined)

    V1(
      OrderRecord.fromOrder(order),
      activation,
      pricing,
      checked.matchedSlices.toVector.map(fromSlice)
    )
  end fromScenario

  def encode(record: V1): Either[WireViolations[WireEncodeViolation], String] =
    codec.write(Objects.requireNonNull(record, "order-scenario record"))

  def encodeScenario[I <: Instrument](
    instrument: I
  )(
    scenario: OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): Either[WireViolations[WireEncodeViolation], String] =
    encode(fromScenario(instrument)(scenario))

  def parse(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], V1] =
    codec.read(input, limits, recordIndex)

  def reconstruct[I <: Instrument](
    record: V1,
    instrument: I,
    snapshot: CatalogSnapshot
  ): Either[
    OrderScenarioReconstructionFailure,
    OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ] =
    Objects.requireNonNull(record, "order-scenario record")
    Objects.requireNonNull(instrument, "order-scenario instrument")
    Objects.requireNonNull(snapshot, "order-scenario catalog snapshot")
    OrderRecord
      .reconstructForScenario(record.order, instrument)
      .left
      .map(OrderScenarioReconstructionFailure.Order.apply)
      .flatMap(_.visit(reconstructionVisitor(record, instrument, snapshot)))
  end reconstruct

  def decodeAndReconstruct[I <: Instrument](
    input: String,
    instrument: I,
    snapshot: CatalogSnapshot,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[
    OrderScenarioReconstructionFailure,
    OrderScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ] =
    parse(input, limits, recordIndex)
      .left
      .map(OrderScenarioReconstructionFailure.Codec.apply)
      .flatMap(record => reconstruct(record, instrument, snapshot))

  /** Decode every independent record against one instrument and one captured snapshot, with no partial success. */
  def reconstructBatch[I <: Instrument](
    inputs: Vector[String],
    instrument: I,
    snapshot: CatalogSnapshot,
    limits: DecodeLimits = DecodeLimits.default
  ): Either[
    WireViolations[IndexedOrderScenarioReconstructionFailure],
    Vector[
      OrderScenario[
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D,
        instrument.MarketState
      ]
    ]
  ] =
    val _               = Objects.requireNonNull(instrument, "scenario batch instrument")
    val checkedSnapshot = Objects.requireNonNull(snapshot, "scenario batch catalog snapshot")
    AllOrErrorsBatch.decode(inputs, limits, "order-scenario")(
      OrderScenarioReconstructionFailure.Codec.apply,
      IndexedOrderScenarioReconstructionFailure.apply
    )((input, index) => decodeAndReconstruct(input, instrument, checkedSnapshot, limits, index))
  end reconstructBatch

  def schema(
    id: String = "urn:trading:codec:schema:order-scenario:v1",
    definitionName: String = "OrderScenarioRecordV1"
  ): Either[WireViolations[WireEncodeViolation], String] =
    codec.schema(id, definitionName)

  private def reconstructionVisitor[I <: Instrument](
    record: V1,
    instrument: I,
    snapshot: CatalogSnapshot
  ): OrderRecord.ReconstructedOrderVisitor[
    instrument.roles.position.D,
    instrument.roles.base.D,
    instrument.roles.quote.D,
    Either[
      OrderScenarioReconstructionFailure,
      OrderScenario[
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D,
        instrument.MarketState
      ]
    ]
  ] =
    new OrderRecord.ReconstructedOrderVisitor[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      Either[
        OrderScenarioReconstructionFailure,
        OrderScenario[
          instrument.roles.position.D,
          instrument.roles.base.D,
          instrument.roles.quote.D,
          instrument.MarketState
        ]
      ]
    ] {
      private type D      = instrument.roles.position.D
      private type B      = instrument.roles.base.D
      private type Q      = instrument.roles.quote.D
      private type Result = Either[
        OrderScenarioReconstructionFailure,
        OrderScenario[D, B, Q, instrument.MarketState]
      ]

      def immediateMarket(
        order: Order.Aux[D, B, Q, ImmediateActivation[B, Q],
          MarketExecution[D, B,
            Q]]
      ): Result =
        prepare(order)(immediateEvidence(order), marketPricing(order))

      def immediateLimit(
        order: Order.Aux[D, B, Q, ImmediateActivation[B, Q],
          PricedExecution[D, B, Q,
            LimitPricing[B,
              Q]]]
      ): Result =
        prepare(order)(immediateEvidence(order), limitPricing(order))

      def immediatePegged(
        order: Order.Aux[D, B, Q, ImmediateActivation[B, Q],
          PricedExecution[D, B, Q,
            PeggedPricing[B,
              Q]]]
      ): Result =
        prepare(order)(immediateEvidence(order), peggedPricing(order))

      def fixedMarket(
        order: Order.Aux[D, B, Q, FixedActivation[B, Q],
          MarketExecution[D, B,
            Q]]
      ): Result =
        prepare(order)(fixedEvidence(order), marketPricing(order))

      def fixedLimit(
        order: Order.Aux[D, B, Q, FixedActivation[B, Q],
          PricedExecution[D, B, Q,
            LimitPricing[B,
              Q]]]
      ): Result =
        prepare(order)(fixedEvidence(order), limitPricing(order))

      def fixedPegged(
        order: Order.Aux[D, B, Q, FixedActivation[B, Q],
          PricedExecution[D, B, Q,
            PeggedPricing[B,
              Q]]]
      ): Result =
        prepare(order)(fixedEvidence(order), peggedPricing(order))

      def trailingMarket(
        order: Order.Aux[D, B, Q, TrailingActivation[B, Q],
          MarketExecution[D, B,
            Q]]
      ): Result =
        prepare(order)(trailingEvidence(order), marketPricing(order))

      def trailingLimit(
        order: Order.Aux[D, B, Q, TrailingActivation[B, Q],
          PricedExecution[D, B, Q,
            LimitPricing[B,
              Q]]]
      ): Result =
        prepare(order)(trailingEvidence(order), limitPricing(order))

      def trailingPegged(
        order: Order.Aux[D, B, Q, TrailingActivation[B, Q],
          PricedExecution[D, B, Q,
            PeggedPricing[B,
              Q]]]
      ): Result =
        prepare(order)(trailingEvidence(order), peggedPricing(order))

      private def immediateEvidence[E <: OrderExecution[D, B, Q]](
        order: Order.Aux[D, B, Q, ImmediateActivation[B, Q],
          E]
      ): Prepared[order.activation.Evidence] =
        record.activationEvidence match
          case ActivationEvidence.Immediate => Right(order.activation.evidence)
          case supplied                     => shapeFailure(
              activationPath,
              ScenarioEvidenceShape.ImmediateActivation,
              activationShape(supplied)
            )

      private def fixedEvidence[E <: OrderExecution[D, B, Q]](
        order: Order.Aux[D, B, Q, FixedActivation[B, Q],
          E]
      ): Prepared[order.activation.Evidence] =
        record.activationEvidence match
          case ActivationEvidence.Fixed(coordinate) =>
            price(coordinate, activationPath.field("observedPriceCoordinate"))
              .flatMap(value =>
                order.activation
                  .evidence(value)
                  .left
                  .map(cause => Vector(ScenarioPreparationFailure.Activation(activationPath, cause)))
              )
          case supplied =>
            shapeFailure(activationPath, ScenarioEvidenceShape.FixedActivation, activationShape(supplied))

      private def trailingEvidence[E <: OrderExecution[D, B, Q]](
        order: Order.Aux[D, B, Q, TrailingActivation[B, Q],
          E]
      ): Prepared[order.activation.Evidence] =
        record.activationEvidence match
          case ActivationEvidence.Trailing(extremeCoordinate, observedCoordinate) =>
            val extreme = price(
              extremeCoordinate,
              activationPath.field("favorableExtremePriceCoordinate")
            )
            val observed = price(observedCoordinate, activationPath.field("observedPriceCoordinate"))
            combine(extreme, observed).flatMap: (checkedExtreme, checkedObserved) =>
              order.activation
                .evidence(checkedExtreme, checkedObserved)
                .left
                .map(cause => Vector(ScenarioPreparationFailure.Activation(activationPath, cause)))
          case supplied =>
            shapeFailure(activationPath, ScenarioEvidenceShape.TrailingActivation, activationShape(supplied))

      private def marketPricing[A <: OrderActivation[B, Q]](
        order: Order.Aux[D, B, Q, A,
          MarketExecution[D, B,
            Q]]
      ): Prepared[order.execution.Resolution] =
        record.pricingResolution match
          case PricingResolution.Direct => Right(order.execution.resolution)
          case supplied                 =>
            shapeFailure(pricingPath, ScenarioEvidenceShape.DirectPricing, pricingShape(supplied))

      private def limitPricing[A <: OrderActivation[B, Q]](
        order: Order.Aux[D, B, Q, A,
          PricedExecution[D, B, Q,
            LimitPricing[B,
              Q]]]
      ): Prepared[order.execution.Resolution] =
        record.pricingResolution match
          case PricingResolution.Direct => Right(order.execution.pricing.resolution)
          case supplied                 =>
            shapeFailure(pricingPath, ScenarioEvidenceShape.DirectPricing, pricingShape(supplied))

      private def peggedPricing[A <: OrderActivation[B, Q]](
        order: Order.Aux[D, B, Q, A,
          PricedExecution[D, B, Q,
            PeggedPricing[B,
              Q]]]
      ): Prepared[order.execution.Resolution] =
        record.pricingResolution match
          case PricingResolution.Pegged(referenceCoordinate, limitCoordinate) =>
            val reference = price(referenceCoordinate, pricingPath.field("referencePriceCoordinate"))
            val limit     = price(limitCoordinate, pricingPath.field("resolvedLimitCoordinate"))
            combine(reference, limit).flatMap: (checkedReference, checkedLimit) =>
              order.execution.pricing
                .resolution(checkedReference, checkedLimit)
                .left
                .map(cause => Vector(ScenarioPreparationFailure.Pricing(pricingPath, cause)))
          case supplied =>
            shapeFailure(pricingPath, ScenarioEvidenceShape.PeggedPricing, pricingShape(supplied))

      private def prepare[A <: OrderActivation[B, Q], E <: OrderExecution[D, B, Q]](
        order: Order.Aux[D, B, Q, A, E]
      )(
        activation: Prepared[order.activation.Evidence],
        pricing: Prepared[order.execution.Resolution]
      ): Result =
        val slices   = matchedSlices()
        val failures = activation.left.toOption.toVector.flatten ++
          pricing.left.toOption.toVector.flatten ++ slices.left.toOption.toVector.flatten
        ScenarioPreparationFailures.from(failures) match
          case Some(errors) => Left(OrderScenarioReconstructionFailure.Preparation(errors))
          case None         =>
            val checkedActivation = activation.toOption.getOrElse(
              throw new IllegalStateException("successful scenario preparation lost activation evidence")
            )
            val checkedPricing = pricing.toOption.getOrElse(
              throw new IllegalStateException("successful scenario preparation lost pricing resolution")
            )
            val checkedSlices = slices.toOption.getOrElse(
              throw new IllegalStateException("successful scenario preparation lost matched slices")
            )
            ScenarioAssumptions
              .create(order)(checkedActivation, checkedPricing, checkedSlices)
              .left
              .map(value => OrderScenarioReconstructionFailure.Validation(ScenarioViolations.one(value)))
              .flatMap(assumptions =>
                OrderScenario
                  .evaluate(instrument)(assumptions)
                  .left
                  .map(OrderScenarioReconstructionFailure.Validation.apply)
              )
        end match
      end prepare

      private def matchedSlices(): Prepared[MatchedSlices[instrument.Lots, instrument.MarketState]] =
        val results = record.slices.zipWithIndex.map: (slice, index) =>
          buildSlice(slice, index)
        val failures = results.collect { case Left(values) => values }.flatten
        if failures.nonEmpty then Left(failures)
        else
          MatchedSlices
            .fromVector(results.collect { case Right(value) => value })
            .left
            .map(cause => Vector(ScenarioPreparationFailure.MatchedSlices(slicesPath, cause)))

      private def buildSlice(
        slice: Slice,
        index: Int
      ): Prepared[LiquiditySlice[instrument.Lots, instrument.MarketState]] =
        val path = slicesPath.index(index)
        val lots = Lots
          .fromCount(instrument)(slice.lotCoordinate)
          .left
          .map(cause => Vector(ScenarioPreparationFailure.Lots(path.field("lotCoordinate"), cause)))
        val market = buildMarket(slice.market, path.field("market"))
        combine(lots, market).flatMap: (checkedLots, checkedMarket) =>
          LiquiditySlice
            .create(instrument)(checkedLots, checkedMarket, toDomainLiquidity(slice.liquidity))
            .left
            .map(cause => Vector(ScenarioPreparationFailure.Slice(path, cause)))

      private def buildMarket(value: Market, path: WirePath): Prepared[instrument.MarketState] =
        val checkedPrice = price(value.priceCoordinate, path.field("priceCoordinate"))
        val conversions  = value.additionalConversions.zipWithIndex.map: (conversion, index) =>
          val conversionPath = path.field("additionalConversions").index(index)
          snapshot
            .resolveAsset(conversion.sourceAssetId)
            .left
            .map(cause =>
              Vector(
                ScenarioPreparationFailure.Catalog(
                  conversionPath.field("sourceAssetId"),
                  conversion.sourceAssetId,
                  cause
                )
              )
            )
            .flatMap(source =>
              SettlementConversion
                .exact(instrument)(source)(conversion.sourceToSettle)
                .left
                .map(cause =>
                  Vector(ScenarioPreparationFailure.Conversion(conversionPath.field("sourceToSettle"), cause))
                )
            )
        val conversionFailures = conversions.collect { case Left(failures) => failures }.flatten
        val checkedConversions =
          if conversionFailures.nonEmpty then Left(conversionFailures)
          else Right(conversions.collect { case Right(conversion) => conversion })
        combine(checkedPrice, checkedConversions).flatMap: (priceValue, additional) =>
          MarketState
            .fromAnchors(instrument)(
              priceValue,
              value.baseToSettle,
              value.quoteToSettle,
              additional
            )
            .left
            .map(cause => Vector(ScenarioPreparationFailure.Market(path, cause)))
      end buildMarket

      private def price(coordinate: BigInt, path: WirePath): Prepared[instrument.Price] =
        OrderRecord
          .priceFromCoordinate(instrument)(coordinate)
          .left
          .map(cause => Vector(ScenarioPreparationFailure.Price(path, cause)))

      private def shapeFailure[A](
        path: WirePath,
        expected: ScenarioEvidenceShape,
        supplied: ScenarioEvidenceShape
      ): Prepared[A] =
        Left(Vector(ScenarioPreparationFailure.EvidenceShape(path, expected, supplied)))

      private val activationPath = payloadPath.field("activationEvidence")
      private val pricingPath    = payloadPath.field("pricingResolution")
      private val slicesPath     = payloadPath.field("slices")
    }

  private type Prepared[A] = Either[Vector[ScenarioPreparationFailure], A]

  private def combine[A, B](left: Prepared[A], right: Prepared[B]): Prepared[(A, B)] =
    (left, right) match
      case (Right(a), Right(b)) => Right(a -> b)
      case _                    => Left(
          left.left.toOption.toVector.flatten ++ right.left.toOption.toVector.flatten
        )

  private def activationShape(value: ActivationEvidence): ScenarioEvidenceShape =
    value match
      case ActivationEvidence.Immediate   => ScenarioEvidenceShape.ImmediateActivation
      case _: ActivationEvidence.Fixed    => ScenarioEvidenceShape.FixedActivation
      case _: ActivationEvidence.Trailing => ScenarioEvidenceShape.TrailingActivation

  private def pricingShape(value: PricingResolution): ScenarioEvidenceShape =
    value match
      case PricingResolution.Direct    => ScenarioEvidenceShape.DirectPricing
      case _: PricingResolution.Pegged => ScenarioEvidenceShape.PeggedPricing

  private def fromSlice[D <: Dim, B <: Dim, Q <: Dim, S <: Dim](
    value: LiquiditySlice[Lots[D], MarketState[B, Q, S]]
  ): Slice =
    val market = value.market
    Slice(
      value.lots.count.unrefined,
      fromDomainLiquidity(value.role),
      Market(
        market.price.ticks.unrefined,
        market.baseToSettle.coefficient,
        market.quoteToSettle.coefficient,
        market.additionalConversions.map(conversion =>
          AdditionalConversion(conversion.source.id, conversion.coefficient)
        )
      )
    )

  private def fromDomainLiquidity(value: LiquidityRole): Liquidity =
    value match
      case LiquidityRole.Maker => Liquidity.Maker
      case LiquidityRole.Taker => Liquidity.Taker

  private def toDomainLiquidity(value: Liquidity): LiquidityRole =
    value match
      case Liquidity.Maker => LiquidityRole.Maker
      case Liquidity.Taker => LiquidityRole.Taker

  private def requiredObservation[B <: Dim, Q <: Dim](
    values: Vector[(trading.order.ActivationObservation, Price[B, Q])],
    kind: trading.order.ActivationObservation,
    invariant: String
  ): Price[B, Q] =
    values.collectFirst { case (`kind`, price) => price }.getOrElse(
      throw new IllegalStateException(s"$invariant is missing its retained observation")
    )

  private def requiredPricingObservation[B <: Dim, Q <: Dim](
    values: Vector[(trading.order.PricingObservation, Price[B, Q])],
    kind: trading.order.PricingObservation,
    invariant: String
  ): Price[B, Q] =
    values.collectFirst { case (`kind`, price) => price }.getOrElse(
      throw new IllegalStateException(s"$invariant is missing its retained observation")
    )

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

  private val payloadPath = WirePath.root.field("payload")
end OrderScenarioRecord

/** Frozen V1 entry × exit record with no cached held position, fees, or valuation. */
object RoundTripScenarioRecord:
  final case class V1(entry: OrderScenarioRecord.V1, exit: OrderScenarioRecord.V1) extends JavaSerializationUnsupported:
    Objects.requireNonNull(entry, "round-trip entry scenario")
    Objects.requireNonNull(exit, "round-trip exit scenario")
  end V1

  val recordType: RecordType       = CodecRecordTypes.roundTripScenario
  val schemaVersion: SchemaVersion = SchemaVersion.one

  private val v1Schema: WireSchema[V1] =
    WireSchema.record(
      WireRecord
        .field("entry", OrderScenarioRecord.v1Schema)
        .product(WireRecord.field("exit", OrderScenarioRecord.v1Schema))
        .imap(value => V1(value._1, value._2))(value => value.entry -> value.exit)
    )

  private val codec = EnvelopeCodec(
    recordType,
    schemaVersion,
    Vector(schemaVersion -> v1Schema),
    CodecRecordTypes.otherThan(recordType)
  )

  def fromScenario[I <: Instrument](
    instrument: I
  )(
    scenario: RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): V1 =
    val checked = Objects.requireNonNull(scenario, "round-trip scenario")
    V1(
      OrderScenarioRecord.fromScenario(instrument)(checked.entry),
      OrderScenarioRecord.fromScenario(instrument)(checked.exit)
    )

  def encode(record: V1): Either[WireViolations[WireEncodeViolation], String] =
    codec.write(Objects.requireNonNull(record, "round-trip scenario record"))

  def encodeScenario[I <: Instrument](
    instrument: I
  )(
    scenario: RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ): Either[WireViolations[WireEncodeViolation], String] =
    encode(fromScenario(instrument)(scenario))

  def parse(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], V1] =
    codec.read(input, limits, recordIndex)

  def reconstruct[I <: Instrument](
    record: V1,
    instrument: I,
    snapshot: CatalogSnapshot
  ): Either[
    RoundTripScenarioReconstructionFailure,
    RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ] =
    Objects.requireNonNull(record, "round-trip scenario record")
    Objects.requireNonNull(instrument, "round-trip scenario instrument")
    Objects.requireNonNull(snapshot, "round-trip scenario catalog snapshot")
    val entry = OrderScenarioRecord
      .reconstruct(record.entry, instrument, snapshot)
      .left
      .map(RoundTripLegReconstructionFailure(RoundTripLeg.Entry, _))
    val exit = OrderScenarioRecord
      .reconstruct(record.exit, instrument, snapshot)
      .left
      .map(RoundTripLegReconstructionFailure(RoundTripLeg.Exit, _))
    val failures = entry.left.toOption.toVector ++ exit.left.toOption.toVector
    RoundTripLegReconstructionFailures.from(failures) match
      case Some(errors) => Left(RoundTripScenarioReconstructionFailure.Legs(errors))
      case None         =>
        val checkedEntry = entry.toOption.getOrElse(
          throw new IllegalStateException("successful round-trip preparation lost entry")
        )
        val checkedExit = exit.toOption.getOrElse(
          throw new IllegalStateException("successful round-trip preparation lost exit")
        )
        RoundTripScenario
          .create(instrument)(checkedEntry, checkedExit)
          .left
          .map(RoundTripScenarioReconstructionFailure.Validation.apply)
    end match
  end reconstruct

  def decodeAndReconstruct[I <: Instrument](
    input: String,
    instrument: I,
    snapshot: CatalogSnapshot,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[
    RoundTripScenarioReconstructionFailure,
    RoundTripScenario[
      instrument.roles.position.D,
      instrument.roles.base.D,
      instrument.roles.quote.D,
      instrument.MarketState
    ]
  ] =
    parse(input, limits, recordIndex)
      .left
      .map(RoundTripScenarioReconstructionFailure.Codec.apply)
      .flatMap(record => reconstruct(record, instrument, snapshot))

  /** Decode every round trip against one instrument and one captured snapshot, with no partial success. */
  def reconstructBatch[I <: Instrument](
    inputs: Vector[String],
    instrument: I,
    snapshot: CatalogSnapshot,
    limits: DecodeLimits = DecodeLimits.default
  ): Either[
    WireViolations[IndexedRoundTripScenarioReconstructionFailure],
    Vector[
      RoundTripScenario[
        instrument.roles.position.D,
        instrument.roles.base.D,
        instrument.roles.quote.D,
        instrument.MarketState
      ]
    ]
  ] =
    val _               = Objects.requireNonNull(instrument, "round-trip batch instrument")
    val checkedSnapshot = Objects.requireNonNull(snapshot, "round-trip batch catalog snapshot")
    AllOrErrorsBatch.decode(inputs, limits, "round-trip scenario")(
      RoundTripScenarioReconstructionFailure.Codec.apply,
      IndexedRoundTripScenarioReconstructionFailure.apply
    )((input, index) => decodeAndReconstruct(input, instrument, checkedSnapshot, limits, index))
  end reconstructBatch

  def schema(
    id: String = "urn:trading:codec:schema:round-trip-scenario:v1",
    definitionName: String = "RoundTripScenarioRecordV1"
  ): Either[WireViolations[WireEncodeViolation], String] =
    codec.schema(id, definitionName)
end RoundTripScenarioRecord
