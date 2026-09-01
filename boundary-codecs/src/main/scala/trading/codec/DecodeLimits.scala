package trading.codec

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import scala.annotation.nowarn

/** Immutable operational policy for bounded strict decoding. */
@nowarn("msg=Ignoring.*qualifier")
final class DecodeLimits private[this] (
  val maxPayloadCharacters: Int,
  val maxPayloadUtf8Bytes: Int,
  val maxNestingDepth: Int,
  val maxBatchRecords: Int,
  val maxObjectMembers: Int,
  val maxArrayEntries: Int,
  val maxStringCharacters: Int,
  val maxIntegerDigits: Int,
  val maxDimensionFactors: Int,
  val maxCatalogCommands: Int,
  val maxScenarioSlices: Int,
  val maxMarketConversions: Int):

  private[codec] def maximum(limit: DecodeLimit): Int =
    limit match
      case DecodeLimit.PayloadCharacters => maxPayloadCharacters
      case DecodeLimit.PayloadUtf8Bytes  => maxPayloadUtf8Bytes
      case DecodeLimit.NestingDepth      => maxNestingDepth
      case DecodeLimit.BatchRecords      => maxBatchRecords
      case DecodeLimit.ObjectMembers     => maxObjectMembers
      case DecodeLimit.ArrayEntries      => maxArrayEntries
      case DecodeLimit.StringCharacters  => maxStringCharacters
      case DecodeLimit.IntegerDigits     => maxIntegerDigits
      case DecodeLimit.DimensionFactors  => maxDimensionFactors
      case DecodeLimit.CatalogCommands   => maxCatalogCommands
      case DecodeLimit.ScenarioSlices    => maxScenarioSlices
      case DecodeLimit.MarketConversions => maxMarketConversions

  override def equals(other: Any): Boolean =
    other match
      case that: DecodeLimits =>
        maxPayloadCharacters == that.maxPayloadCharacters &&
        maxPayloadUtf8Bytes == that.maxPayloadUtf8Bytes &&
        maxNestingDepth == that.maxNestingDepth &&
        maxBatchRecords == that.maxBatchRecords &&
        maxObjectMembers == that.maxObjectMembers &&
        maxArrayEntries == that.maxArrayEntries &&
        maxStringCharacters == that.maxStringCharacters &&
        maxIntegerDigits == that.maxIntegerDigits &&
        maxDimensionFactors == that.maxDimensionFactors &&
        maxCatalogCommands == that.maxCatalogCommands &&
        maxScenarioSlices == that.maxScenarioSlices &&
        maxMarketConversions == that.maxMarketConversions
      case _ => false

  override def hashCode: Int =
    (
      maxPayloadCharacters,
      maxPayloadUtf8Bytes,
      maxNestingDepth,
      maxBatchRecords,
      maxObjectMembers,
      maxArrayEntries,
      maxStringCharacters,
      maxIntegerDigits,
      maxDimensionFactors,
      maxCatalogCommands,
      maxScenarioSlices,
      maxMarketConversions
    ).hashCode
end DecodeLimits

object DecodeLimits:
  private val constructor =
    val owner = classOf[DecodeLimits]
    MethodHandles
      .privateLookupIn(owner, MethodHandles.lookup())
      .findConstructor(
        owner,
        MethodType.methodType(java.lang.Void.TYPE, Array.fill[Class[?]](12)(java.lang.Integer.TYPE))
      )

  val default: DecodeLimits =
    construct(
      maxPayloadCharacters = 1_000_000,
      maxPayloadUtf8Bytes = 4_000_000,
      maxNestingDepth = 32,
      maxBatchRecords = 10_000,
      maxObjectMembers = 128,
      maxArrayEntries = 10_000,
      maxStringCharacters = 4_096,
      maxIntegerDigits = 4_096,
      maxDimensionFactors = 256,
      maxCatalogCommands = 10_000,
      maxScenarioSlices = 10_000,
      maxMarketConversions = 1_024
    )

  def create(
    maxPayloadCharacters: Int,
    maxPayloadUtf8Bytes: Int,
    maxNestingDepth: Int,
    maxBatchRecords: Int,
    maxObjectMembers: Int,
    maxArrayEntries: Int,
    maxStringCharacters: Int,
    maxIntegerDigits: Int,
    maxDimensionFactors: Int,
    maxCatalogCommands: Int,
    maxScenarioSlices: Int,
    maxMarketConversions: Int
  ): Either[WireViolations[DecodeLimitConfigurationViolation], DecodeLimits] =
    val supplied = Vector(
      DecodeLimit.PayloadCharacters -> maxPayloadCharacters,
      DecodeLimit.PayloadUtf8Bytes  -> maxPayloadUtf8Bytes,
      DecodeLimit.NestingDepth      -> maxNestingDepth,
      DecodeLimit.BatchRecords      -> maxBatchRecords,
      DecodeLimit.ObjectMembers     -> maxObjectMembers,
      DecodeLimit.ArrayEntries      -> maxArrayEntries,
      DecodeLimit.StringCharacters  -> maxStringCharacters,
      DecodeLimit.IntegerDigits     -> maxIntegerDigits,
      DecodeLimit.DimensionFactors  -> maxDimensionFactors,
      DecodeLimit.CatalogCommands   -> maxCatalogCommands,
      DecodeLimit.ScenarioSlices    -> maxScenarioSlices,
      DecodeLimit.MarketConversions -> maxMarketConversions
    )
    val nonPositive = supplied.collect:
      case (limit, value) if value <= 0 => DecodeLimitConfigurationViolation.NonPositive(limit, value)

    val relationships =
      if nonPositive.nonEmpty then Vector.empty
      else
        Vector(
          relationship(
            DecodeLimit.PayloadCharacters,
            maxPayloadCharacters,
            DecodeLimit.PayloadUtf8Bytes,
            maxPayloadUtf8Bytes
          ),
          relationship(
            DecodeLimit.IntegerDigits,
            maxIntegerDigits,
            DecodeLimit.StringCharacters,
            maxStringCharacters
          ),
          relationship(DecodeLimit.BatchRecords, maxBatchRecords, DecodeLimit.ArrayEntries, maxArrayEntries),
          relationship(
            DecodeLimit.DimensionFactors,
            maxDimensionFactors,
            DecodeLimit.ArrayEntries,
            maxArrayEntries
          ),
          relationship(
            DecodeLimit.CatalogCommands,
            maxCatalogCommands,
            DecodeLimit.ArrayEntries,
            maxArrayEntries
          ),
          relationship(DecodeLimit.ScenarioSlices, maxScenarioSlices, DecodeLimit.ArrayEntries, maxArrayEntries),
          relationship(
            DecodeLimit.MarketConversions,
            maxMarketConversions,
            DecodeLimit.ArrayEntries,
            maxArrayEntries
          )
        ).flatten

    WireViolations.fromVector(nonPositive ++ relationships) match
      case Some(errors) => Left(errors)
      case None         =>
        Right(
          construct(
            maxPayloadCharacters,
            maxPayloadUtf8Bytes,
            maxNestingDepth,
            maxBatchRecords,
            maxObjectMembers,
            maxArrayEntries,
            maxStringCharacters,
            maxIntegerDigits,
            maxDimensionFactors,
            maxCatalogCommands,
            maxScenarioSlices,
            maxMarketConversions
          )
        )
  end create

  private def relationship(
    limit: DecodeLimit,
    supplied: Int,
    container: DecodeLimit,
    maximum: Int
  ): Option[DecodeLimitConfigurationViolation] =
    Option.when(supplied > maximum):
      DecodeLimitConfigurationViolation.ExceedsContainer(limit, supplied, container, maximum)

  private def construct(
    maxPayloadCharacters: Int,
    maxPayloadUtf8Bytes: Int,
    maxNestingDepth: Int,
    maxBatchRecords: Int,
    maxObjectMembers: Int,
    maxArrayEntries: Int,
    maxStringCharacters: Int,
    maxIntegerDigits: Int,
    maxDimensionFactors: Int,
    maxCatalogCommands: Int,
    maxScenarioSlices: Int,
    maxMarketConversions: Int
  ): DecodeLimits =
    constructor
      .invoke(
        maxPayloadCharacters,
        maxPayloadUtf8Bytes,
        maxNestingDepth,
        maxBatchRecords,
        maxObjectMembers,
        maxArrayEntries,
        maxStringCharacters,
        maxIntegerDigits,
        maxDimensionFactors,
        maxCatalogCommands,
        maxScenarioSlices,
        maxMarketConversions
      )
      .asInstanceOf[DecodeLimits]
end DecodeLimits
