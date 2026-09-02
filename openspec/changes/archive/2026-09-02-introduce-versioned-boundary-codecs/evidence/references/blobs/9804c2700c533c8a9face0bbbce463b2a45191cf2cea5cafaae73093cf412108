package trading.codec

import cats.data.NonEmptyChain
import cats.data.Validated
import cats.syntax.validated.*

import trading.economics.instrument.InstrumentId
import trading.economics.instrument.UnderlyingId
import trading.quantity.AtomId
import trading.quantity.DimKey
import trading.quantity.Rational
import trading.reference.AssetId
import trading.reference.GridId
import trading.reference.GridIdentity
import trading.reference.GridKey
import trading.reference.GridVersion

private[codec] final case class CanonicalRationalRecord(numerator: BigInt, denominator: BigInt)
private[codec] final case class CanonicalDimensionFactor(atom: AtomId, power: BigInt)
private[codec] final case class CanonicalGridIdentity(
  dimension: DimKey,
  gridId: GridId,
  gridVersion: GridVersion)

/** Strict reusable exact/domain primitive schemas. No normalizing wire alias is accepted. */
private[codec] object ExactWire:
  private val CanonicalInteger = "(?:0|-[1-9][0-9]*|[1-9][0-9]*)".r

  val canonicalInteger: WireSchema[BigInt] =
    WireSchema.text.refine[BigInt](parseCanonicalInteger)(_.toString)

  val positiveInteger: WireSchema[BigInt] =
    canonicalInteger.refine[BigInt](positive)(identity)

  val rational: WireSchema[Rational] =
    val representation =
      WireRecord
        .field("numerator", canonicalInteger)
        .product(WireRecord.field("denominator", positiveInteger))
        .imap(CanonicalRationalRecord.apply)(value => value.numerator -> value.denominator)
    WireSchema
      .record(representation)
      .refine[Rational](decodeRational)(value => CanonicalRationalRecord(value.numerator, value.denominator))

  val atomId: WireSchema[AtomId] =
    identifier(StableIdentifierKind.DimensionAtom, constructAtomId)(_.value)

  val assetId: WireSchema[AssetId] =
    identifier(StableIdentifierKind.Asset, value => AssetId.from(value).left.map(_ => ()))(_.value())

  val gridId: WireSchema[GridId] =
    identifier(StableIdentifierKind.Grid, value => GridId.from(value).left.map(_ => ()))(_.value())

  val instrumentId: WireSchema[InstrumentId] =
    identifier(StableIdentifierKind.Instrument, value => InstrumentId.from(value).left.map(_ => ()))(_.value)

  val underlyingId: WireSchema[UnderlyingId] =
    identifier(StableIdentifierKind.Underlying, value => UnderlyingId.from(value).left.map(_ => ()))(_.value)

  val gridVersion: WireSchema[GridVersion] =
    positiveInteger.refine[GridVersion](decodeGridVersion)(value => BigInt(value.value()))

  val dimension: WireSchema[DimKey] =
    val factor =
      WireRecord
        .field("atom", atomId)
        .product(WireRecord.field("power", canonicalInteger))
        .imap(CanonicalDimensionFactor.apply)(value => value.atom -> value.power)
    WireSchema
      .vector(WireSchema.record(factor), DecodeLimit.DimensionFactors)
      .refineAccumulating[DimKey](decodeDimension)(value =>
        value.powers.map((atom, power) => CanonicalDimensionFactor(atom, power))
      )

  val gridIdentity: WireSchema[GridIdentity] =
    val representation =
      WireRecord
        .field("dimension", dimension)
        .product(WireRecord.field("gridId", gridId))
        .product(WireRecord.field("gridVersion", gridVersion))
        .imap(value => CanonicalGridIdentity(value._1._1, value._1._2, value._2))(value =>
          ((value.dimension, value.gridId), value.gridVersion)
        )
    WireSchema.record(representation).imap(value =>
      GridIdentity(value.dimension, GridKey(value.gridId, value.gridVersion))
    )(value => CanonicalGridIdentity(value.dimension, value.key.id, value.key.version))

  private def parseCanonicalInteger(
    raw: String,
    context: DecodeContext
  ): Either[WireDecodeViolation, BigInt] =
    val digits = raw.count(character => character >= '0' && character <= '9')
    if digits > context.limits.maxIntegerDigits then
      Left(
        WireDecodeViolation.Limit(
          WireLimitViolation(
            DecodeLimit.IntegerDigits,
            digits.toLong,
            context.limits.maxIntegerDigits,
            context.path,
            context.recordIndex
          )
        )
      )
    else
      raw match
        case CanonicalInteger() => Right(BigInt(raw))
        case _                  =>
          Left(
            WireDecodeViolation.ExactNumber(
              context.path,
              ExactNumberProblem.NonCanonicalInteger(raw),
              context.recordIndex
            )
          )
    end if
  end parseCanonicalInteger

  private def positive(
    value: BigInt,
    context: DecodeContext
  ): Either[WireDecodeViolation, BigInt] =
    Either.cond(
      value > 0,
      value,
      WireDecodeViolation.ExactNumber(
        context.path,
        ExactNumberProblem.NonPositiveInteger(value.toString),
        context.recordIndex
      )
    )

  private def decodeRational(
    value: CanonicalRationalRecord,
    context: DecodeContext
  ): Either[WireDecodeViolation, Rational] =
    val numerator   = value.numerator
    val denominator = value.denominator
    if numerator == 0 && denominator != 1 then
      Left(
        WireDecodeViolation.ExactNumber(
          context.path,
          ExactNumberProblem.NonCanonicalZero(denominator.toString),
          context.recordIndex
        )
      )
    else if numerator.gcd(denominator) != 1 then
      Left(
        WireDecodeViolation.ExactNumber(
          context.path,
          ExactNumberProblem.NonReducedRational(numerator.toString, denominator.toString),
          context.recordIndex
        )
      )
    else
      val result = Rational(numerator, denominator)
      Either.cond(
        result.numerator == numerator && result.denominator == denominator,
        result,
        WireDecodeViolation.ExactNumber(
          context.path,
          ExactNumberProblem.RationalProjectionMismatch(numerator.toString, denominator.toString),
          context.recordIndex
        )
      )
    end if
  end decodeRational

  private def identifier[A](
    kind: StableIdentifierKind,
    construct: String => Either[Unit, A]
  )(
    value: A => String
  ): WireSchema[A] =
    WireSchema.text.refine[A]((supplied, context) =>
      construct(supplied).left.map: _ =>
        WireDecodeViolation.InvalidStableIdentifier(
          context.path,
          kind,
          StableIdentifierProblem.Empty,
          supplied,
          context.recordIndex
        )
    )(value)

  private def constructAtomId(value: String): Either[Unit, AtomId] =
    try Right(AtomId(value))
    catch case _: IllegalArgumentException => Left(())

  private def decodeGridVersion(
    value: BigInt,
    context: DecodeContext
  ): Either[WireDecodeViolation, GridVersion] =
    if !value.isValidLong then
      Left(
        WireDecodeViolation.ExactNumber(
          context.path,
          ExactNumberProblem.OutsideTargetRange("GridVersion", value.toString),
          context.recordIndex
        )
      )
    else
      GridVersion.from(value.longValue).left.map: _ =>
        WireDecodeViolation.ExactNumber(
          context.path,
          ExactNumberProblem.NonPositiveInteger(value.toString),
          context.recordIndex
        )

  private def decodeDimension(
    factors: Vector[CanonicalDimensionFactor],
    context: DecodeContext
  ): DecodeValidation[DimKey] =
    val errors = Vector.newBuilder[WireDecodeViolation]
    var seen   = Set.empty[AtomId]
    factors.zipWithIndex.foreach: (factor, index) =>
      if factor.power == 0 then
        errors += WireDecodeViolation.InvalidDimension(
          context.index(index).field("power").path,
          DimensionProblem.ZeroPower(factor.atom.value),
          context.recordIndex
        )
      if seen.contains(factor.atom) then
        errors += WireDecodeViolation.InvalidDimension(
          context.index(index).field("atom").path,
          DimensionProblem.DuplicateAtom(factor.atom.value),
          context.recordIndex
        )
      else seen += factor.atom
      if index > 0 then
        val previous = factors(index - 1).atom.value
        if previous.compareTo(factor.atom.value) > 0 then
          errors += WireDecodeViolation.InvalidDimension(
            context.index(index).field("atom").path,
            DimensionProblem.AtomOutOfOrder(previous, factor.atom.value),
            context.recordIndex
          )

    NonEmptyChain.fromSeq(errors.result()) match
      case Some(values) => Validated.Invalid(values)
      case None         =>
        val supplied = factors.map(factor => factor.atom -> factor.power)
        val result   = DimKey(supplied)
        if result.powers == supplied then result.validNec
        else
          WireDecodeViolation
            .InvalidDimension(context.path, DimensionProblem.NormalizationMismatch, context.recordIndex)
            .invalidNec
  end decodeDimension
end ExactWire
