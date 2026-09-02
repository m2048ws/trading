package trading.codec

import java.util.Objects

import trading.economics.instrument.AssetRoleIds
import trading.economics.instrument.Instrument
import trading.economics.instrument.InstrumentAssembler
import trading.economics.instrument.InstrumentAssemblyErrors
import trading.economics.instrument.InstrumentDefinition
import trading.economics.instrument.InstrumentId
import trading.economics.instrument.InstrumentIdentity
import trading.economics.instrument.ListingDefinition
import trading.economics.instrument.PayoffDefinition
import trading.economics.instrument.UnderlyingId
import trading.quantity.JavaSerializationUnsupported
import trading.quantity.Rational
import trading.reference.AssetId
import trading.reference.CatalogSnapshot
import trading.reference.GridIdentity

/** Typed stage retained when stable instrument syntax cannot become one trusted instrument. */
enum InstrumentDefinitionReconstructionFailure extends JavaSerializationUnsupported:
  case Codec(violations: WireViolations[WireDecodeViolation])
  case Assembly(errors: InstrumentAssemblyErrors)

  private val _ =
    this match
      case Codec(violations) => Objects.requireNonNull(violations, "instrument-definition codec violations")
      case Assembly(errors)  => Objects.requireNonNull(errors, "instrument-definition assembly errors")
end InstrumentDefinitionReconstructionFailure

/** One failed record in an all-valid-or-errors instrument-definition batch. */
final case class IndexedInstrumentDefinitionReconstructionFailure(
  recordIndex: Int,
  failure: InstrumentDefinitionReconstructionFailure)
  extends JavaSerializationUnsupported:

  require(recordIndex >= 0, "instrument-definition record index must be nonnegative")
  Objects.requireNonNull(failure, "instrument-definition reconstruction failure")
end IndexedInstrumentDefinitionReconstructionFailure

/** Frozen stable-data representation and pure reconstruction boundary for instrument definitions. */
object InstrumentDefinitionRecord:
  final case class Identity(instrumentId: InstrumentId, underlyingId: UnderlyingId)
    extends JavaSerializationUnsupported:
    Objects.requireNonNull(instrumentId, "instrument ID")
    Objects.requireNonNull(underlyingId, "underlying ID")
  end Identity

  final case class RoleAssetIds(
    baseAssetId: AssetId,
    quoteAssetId: AssetId,
    positionAssetId: AssetId,
    settleAssetId: AssetId)
    extends JavaSerializationUnsupported:
    Objects.requireNonNull(baseAssetId, "base asset ID")
    Objects.requireNonNull(quoteAssetId, "quote asset ID")
    Objects.requireNonNull(positionAssetId, "position asset ID")
    Objects.requireNonNull(settleAssetId, "settle asset ID")
  end RoleAssetIds

  final case class Listing(
    positionLotGridIdentity: GridIdentity,
    priceGridIdentity: GridIdentity)
    extends JavaSerializationUnsupported:
    Objects.requireNonNull(positionLotGridIdentity, "position-lot grid identity")
    Objects.requireNonNull(priceGridIdentity, "price grid identity")
  end Listing

  final case class Payoff(
    basePerPosition: Rational,
    quotePerPosition: Rational)
    extends JavaSerializationUnsupported:
    Objects.requireNonNull(basePerPosition, "base-per-position coefficient")
    Objects.requireNonNull(quotePerPosition, "quote-per-position coefficient")
  end Payoff

  final case class V1(
    identity: Identity,
    roles: RoleAssetIds,
    listing: Listing,
    payoff: Payoff)
    extends JavaSerializationUnsupported:
    Objects.requireNonNull(identity, "instrument identity")
    Objects.requireNonNull(roles, "instrument role asset IDs")
    Objects.requireNonNull(listing, "instrument listing")
    Objects.requireNonNull(payoff, "instrument payoff")
  end V1

  val recordType: RecordType       = CodecRecordTypes.instrumentDefinition
  val schemaVersion: SchemaVersion = SchemaVersion.one

  private val identitySchema: WireSchema[Identity] =
    val representation =
      WireRecord
        .field("instrumentId", ExactWire.instrumentId)
        .product(WireRecord.field("underlyingId", ExactWire.underlyingId))
        .imap(value => Identity(value._1, value._2))(value => value.instrumentId -> value.underlyingId)
    WireSchema.record(representation)

  private val rolesSchema: WireSchema[RoleAssetIds] =
    val representation =
      WireRecord
        .field("baseAssetId", ExactWire.assetId)
        .product(WireRecord.field("quoteAssetId", ExactWire.assetId))
        .product(WireRecord.field("positionAssetId", ExactWire.assetId))
        .product(WireRecord.field("settleAssetId", ExactWire.assetId))
        .imap(value => RoleAssetIds(value._1._1._1, value._1._1._2, value._1._2, value._2))(value =>
          (((value.baseAssetId, value.quoteAssetId), value.positionAssetId), value.settleAssetId)
        )
    WireSchema.record(representation)

  private val listingSchema: WireSchema[Listing] =
    val representation =
      WireRecord
        .field("positionLotGridIdentity", ExactWire.gridIdentity)
        .product(WireRecord.field("priceGridIdentity", ExactWire.gridIdentity))
        .imap(value => Listing(value._1, value._2))(value =>
          value.positionLotGridIdentity -> value.priceGridIdentity
        )
    WireSchema.record(representation)

  private val payoffSchema: WireSchema[Payoff] =
    val representation =
      WireRecord
        .field("basePerPosition", ExactWire.rational)
        .product(WireRecord.field("quotePerPosition", ExactWire.rational))
        .imap(value => Payoff(value._1, value._2))(value => value.basePerPosition -> value.quotePerPosition)
    WireSchema.record(representation)

  private val v1Schema: WireSchema[V1] =
    val representation =
      WireRecord
        .field("identity", identitySchema)
        .product(WireRecord.field("roles", rolesSchema))
        .product(WireRecord.field("listing", listingSchema))
        .product(WireRecord.field("payoff", payoffSchema))
        .imap(value => V1(value._1._1._1, value._1._1._2, value._1._2, value._2))(value =>
          (((value.identity, value.roles), value.listing), value.payoff)
        )
    WireSchema.record(representation)

  private val codec = EnvelopeCodec(
    recordType,
    schemaVersion,
    Vector(schemaVersion -> v1Schema),
    CodecRecordTypes.otherThan(recordType)
  )

  def fromDefinition(definition: InstrumentDefinition): V1 =
    val checked = Objects.requireNonNull(definition, "instrument definition")
    V1(
      Identity(checked.identity.id, checked.identity.underlying),
      RoleAssetIds(
        checked.roles.base,
        checked.roles.quote,
        checked.roles.position,
        checked.roles.settle
      ),
      Listing(checked.listing.positionLotGrid, checked.listing.priceGrid),
      Payoff(checked.payoff.basePerPosition, checked.payoff.quotePerPosition)
    )

  def toDefinition(record: V1): InstrumentDefinition =
    val checked = Objects.requireNonNull(record, "instrument-definition record")
    InstrumentDefinition(
      InstrumentIdentity(checked.identity.instrumentId, checked.identity.underlyingId),
      AssetRoleIds(
        checked.roles.baseAssetId,
        checked.roles.quoteAssetId,
        checked.roles.positionAssetId,
        checked.roles.settleAssetId
      ),
      ListingDefinition(
        checked.listing.positionLotGridIdentity,
        checked.listing.priceGridIdentity
      ),
      PayoffDefinition(checked.payoff.basePerPosition, checked.payoff.quotePerPosition)
    )

  def encode(record: V1): Either[WireViolations[WireEncodeViolation], String] =
    codec.write(Objects.requireNonNull(record, "instrument-definition record"))

  def encodeDefinition(definition: InstrumentDefinition): Either[WireViolations[WireEncodeViolation], String] =
    encode(fromDefinition(definition))

  def parse(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], V1] =
    codec.read(input, limits, recordIndex)

  /** Decode stable syntax to the domain definition without consulting catalog authority. */
  def decode(
    input: String,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[WireViolations[WireDecodeViolation], InstrumentDefinition] =
    parse(input, limits, recordIndex).map(toDefinition)

  /** Assemble an already decoded definition through the one normative proof-issuing boundary. */
  def assemble(
    definition: InstrumentDefinition,
    snapshot: CatalogSnapshot
  ): Either[InstrumentAssemblyErrors, Instrument] =
    InstrumentAssembler
      .assemble(
        Objects.requireNonNull(definition, "instrument definition"),
        Objects.requireNonNull(snapshot, "catalog snapshot")
      )
      .map(Instrument.fromSpec)

  /** Keep structural codec failures distinct from the owning assembly failure collection. */
  def decodeAndAssemble(
    input: String,
    snapshot: CatalogSnapshot,
    limits: DecodeLimits = DecodeLimits.default,
    recordIndex: Int = 0
  ): Either[InstrumentDefinitionReconstructionFailure, Instrument] =
    decode(input, limits, recordIndex)
      .left
      .map(InstrumentDefinitionReconstructionFailure.Codec.apply)
      .flatMap(definition =>
        assemble(definition, snapshot).left.map(InstrumentDefinitionReconstructionFailure.Assembly.apply)
      )

  /** Evaluate every independent record against exactly one snapshot and expose no partial-success vector. */
  def reconstructBatch(
    inputs: Vector[String],
    snapshot: CatalogSnapshot,
    limits: DecodeLimits = DecodeLimits.default
  ): Either[WireViolations[IndexedInstrumentDefinitionReconstructionFailure], Vector[Instrument]] =
    val checkedInputs   = Objects.requireNonNull(inputs, "instrument-definition inputs")
    val checkedSnapshot = Objects.requireNonNull(snapshot, "catalog snapshot")
    val checkedLimits   = Objects.requireNonNull(limits, "instrument-definition decode limits")
    if checkedInputs.size > checkedLimits.maxBatchRecords then
      Left(
        WireViolations.one(
          IndexedInstrumentDefinitionReconstructionFailure(
            0,
            InstrumentDefinitionReconstructionFailure.Codec(
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
        decodeAndAssemble(
          Objects.requireNonNull(input, s"instrument-definition input $index"),
          checkedSnapshot,
          checkedLimits,
          index
        ).left.map(failure => IndexedInstrumentDefinitionReconstructionFailure(index, failure))
      val failures = results.collect:
        case Left(failure) => failure
      WireViolations.fromVector(failures) match
        case Some(errors) => Left(errors)
        case None         => Right(results.collect { case Right(instrument) => instrument })
    end if
  end reconstructBatch

  def schema(
    id: String = "urn:trading:codec:schema:instrument-definition:v1",
    definitionName: String = "InstrumentDefinitionRecordV1"
  ): Either[WireViolations[WireEncodeViolation], String] =
    codec.schema(id, definitionName)
end InstrumentDefinitionRecord
