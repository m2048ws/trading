package trading.economics.instrument

import trading.quantity.*
import trading.quantity.refinement.NonZero
import trading.reference.*

/** One trusted source and its endpoint-typed conversion into settlement. */
final class SettlementConversion[S <: Dim] private (
  val instrumentId: InstrumentId,
  val source: Asset
)(
  val target: Asset { type D = S },
  val rate: Rate[source.D, S])
  extends JavaSerializationUnsupported:

  def coefficient: Rational = rate.coefficient
end SettlementConversion

object SettlementConversion:
  def exact(
    instrument: Instrument
  )(
    source: Asset
  )(
    coefficient: Rational
  ): Either[MarketStateViolation, SettlementConversion[instrument.roles.settle.D]] =
    fromRate(instrument)(source)(
      Rate(source.dimension.ref, instrument.roles.settle.dimension.ref, coefficient)
    )

  def fromRate(
    instrument: Instrument
  )(
    source: Asset
  )(
    rate: Rate[source.D, instrument.roles.settle.D]
  ): Either[MarketStateViolation, SettlementConversion[instrument.roles.settle.D]] =
    val settle: Asset { type D = instrument.roles.settle.D } = instrument.roles.settle
    DimensionHandle
      .sameLineage(source.dimension, settle.dimension)
      .left
      .map(cause => MarketStateViolation.ReferenceData("conversion.source", cause))
      .flatMap: _ =>
        validate(source.id, settle.id, rate.coefficient)
          .toLeft(())
          .map(_ => new SettlementConversion(instrument.identity.id, source)(settle, rate))

  private[instrument] def validate(
    source: AssetId,
    target: AssetId,
    coefficient: Rational
  ): Option[MarketStateViolation] =
    if coefficient.signum <= 0 then
      Some(
        MarketStateViolation.InvalidConversion(
          source,
          target,
          coefficient,
          ConversionFailureReason.NonPositive
        )
      )
    else if source == target && coefficient != Rational.one then
      Some(
        MarketStateViolation.InvalidConversion(
          source,
          target,
          coefficient,
          ConversionFailureReason.IdentityNotOne
        )
      )
    else None
end SettlementConversion

/** Immutable coherent price and heterogeneous settle-targeted conversion set. */
final class MarketState[B <: Dim, Q <: Dim, S <: Dim] private (
  val instrumentId: InstrumentId,
  val base: Asset { type D = B },
  val quote: Asset { type D = Q },
  val settlement: Asset { type D = S },
  val price: Price[B, Q],
  val baseToSettle: Rate[B, S],
  val quoteToSettle: Rate[Q, S],
  conversions: Vector[SettlementConversion[S]],
  val additionalConversions: Vector[SettlementConversion[S]])
  extends JavaSerializationUnsupported:

  private val byId: Map[AssetId, SettlementConversion[S]] =
    conversions.iterator.map(value => value.source.id -> value).toMap

  val conversionSources: Vector[AssetId] = conversions.map(_.source.id)

  def convertToSettle(
    source: Asset
  )(
    value: Quantity[source.D]
  ): Either[ConversionError, Quantity[S]] =
    byId.get(source.id) match
      case None             => Left(MissingConversion(source.id))
      case Some(conversion) =>
        Asset
          .reconcile(source, conversion.source)
          .left
          .map(cause => ConversionSourceMismatch(source.id, cause))
          .map: same =>
            value.alignTo[conversion.source.D](using same).applyRate(conversion.rate)
end MarketState

object MarketState:
  /** Project accumulated construction diagnostics to the same stable first error. */
  def firstError[A](
    result: Either[MarketStateViolations, A]
  ): Either[MarketStateViolation, A] =
    result.left.map(_.head)

  def quoteSettled(
    instrument: Instrument
  )(
    price: instrument.Price
  ): Either[MarketStateViolations, instrument.MarketState] =
    quoteSettled(instrument)(price, Vector.empty)

  def quoteSettled(
    instrument: Instrument
  )(
    price: instrument.Price,
    additional: Vector[SettlementConversion[instrument.roles.settle.D]]
  ): Either[MarketStateViolations, instrument.MarketState] =
    val quote: Asset { type D = instrument.roles.quote.D }   = instrument.roles.quote
    val settle: Asset { type D = instrument.roles.settle.D } = instrument.roles.settle
    if quote.id != settle.id then
      Left(
        MarketStateViolations.one(
          MarketStateViolation.InvalidConversion(
            quote.id,
            settle.id,
            Rational.one,
            ConversionFailureReason.SettleIsNotQuote
          )
        )
      )
    else
      val quoteRate = Rate(quote.dimension.ref, settle.dimension.ref, Rational.one)
      checked(instrument)(price, price.rate.andThen(quoteRate), quoteRate, additional)
  end quoteSettled

  def baseSettled(
    instrument: Instrument
  )(
    price: instrument.Price
  ): Either[MarketStateViolations, instrument.MarketState] =
    baseSettled(instrument)(price, Vector.empty)

  def baseSettled(
    instrument: Instrument
  )(
    price: instrument.Price,
    additional: Vector[SettlementConversion[instrument.roles.settle.D]]
  ): Either[MarketStateViolations, instrument.MarketState] =
    val base: Asset { type D = instrument.roles.base.D }     = instrument.roles.base
    val settle: Asset { type D = instrument.roles.settle.D } = instrument.roles.settle
    if base.id != settle.id then
      Left(
        MarketStateViolations.one(
          MarketStateViolation.InvalidConversion(
            base.id,
            settle.id,
            Rational.one,
            ConversionFailureReason.SettleIsNotBase
          )
        )
      )
    else
      NonZero(price.rate) match
        case Left(_) =>
          Left(
            MarketStateViolations.one(
              MarketStateViolation.InvalidConversion(
                instrument.roles.quote.id,
                settle.id,
                Rational.zero,
                ConversionFailureReason.NonPositive
              )
            )
          )
        case Right(nonZeroPrice) =>
          val baseRate  = Rate(base.dimension.ref, settle.dimension.ref, Rational.one)
          val quoteRate = nonZeroPrice.reciprocalRate.andThen(baseRate)
          checked(instrument)(price, baseRate, quoteRate, additional)
    end if
  end baseSettled

  def fromQuoteAnchor(
    instrument: Instrument
  )(
    price: instrument.Price,
    quoteToSettle: Rational
  ): Either[MarketStateViolations, instrument.MarketState] =
    fromQuoteAnchor(instrument)(price, quoteToSettle, Vector.empty)

  def fromQuoteAnchor(
    instrument: Instrument
  )(
    price: instrument.Price,
    quoteToSettle: Rational,
    additional: Vector[SettlementConversion[instrument.roles.settle.D]]
  ): Either[MarketStateViolations, instrument.MarketState] =
    fromQuoteRate(instrument)(
      price,
      Rate(
        instrument.roles.quote.dimension.ref,
        instrument.roles.settle.dimension.ref,
        quoteToSettle
      ),
      additional
    )

  def fromBaseAnchor(
    instrument: Instrument
  )(
    price: instrument.Price,
    baseToSettle: Rational
  ): Either[MarketStateViolations, instrument.MarketState] =
    fromBaseAnchor(instrument)(price, baseToSettle, Vector.empty)

  def fromBaseAnchor(
    instrument: Instrument
  )(
    price: instrument.Price,
    baseToSettle: Rational,
    additional: Vector[SettlementConversion[instrument.roles.settle.D]]
  ): Either[MarketStateViolations, instrument.MarketState] =
    fromBaseRate(instrument)(
      price,
      Rate(
        instrument.roles.base.dimension.ref,
        instrument.roles.settle.dimension.ref,
        baseToSettle
      ),
      additional
    )

  def fromAnchors(
    instrument: Instrument
  )(
    price: instrument.Price,
    baseToSettle: Rational,
    quoteToSettle: Rational
  ): Either[MarketStateViolations, instrument.MarketState] =
    fromAnchors(instrument)(price, baseToSettle, quoteToSettle, Vector.empty)

  def fromAnchors(
    instrument: Instrument
  )(
    price: instrument.Price,
    baseToSettle: Rational,
    quoteToSettle: Rational,
    additional: Vector[SettlementConversion[instrument.roles.settle.D]]
  ): Either[MarketStateViolations, instrument.MarketState] =
    fromRates(instrument)(
      price,
      Rate(
        instrument.roles.base.dimension.ref,
        instrument.roles.settle.dimension.ref,
        baseToSettle
      ),
      Rate(
        instrument.roles.quote.dimension.ref,
        instrument.roles.settle.dimension.ref,
        quoteToSettle
      ),
      additional
    )

  def fromQuoteRate(
    instrument: Instrument
  )(
    price: instrument.Price,
    quoteToSettle: Rate[instrument.roles.quote.D, instrument.roles.settle.D]
  ): Either[MarketStateViolations, instrument.MarketState] =
    fromQuoteRate(instrument)(price, quoteToSettle, Vector.empty)

  def fromQuoteRate(
    instrument: Instrument
  )(
    price: instrument.Price,
    quoteToSettle: Rate[instrument.roles.quote.D, instrument.roles.settle.D],
    additional: Vector[SettlementConversion[instrument.roles.settle.D]]
  ): Either[MarketStateViolations, instrument.MarketState] =
    checked(instrument)(price, price.rate.andThen(quoteToSettle), quoteToSettle, additional)

  def fromBaseRate(
    instrument: Instrument
  )(
    price: instrument.Price,
    baseToSettle: Rate[instrument.roles.base.D, instrument.roles.settle.D]
  ): Either[MarketStateViolations, instrument.MarketState] =
    fromBaseRate(instrument)(price, baseToSettle, Vector.empty)

  def fromBaseRate(
    instrument: Instrument
  )(
    price: instrument.Price,
    baseToSettle: Rate[instrument.roles.base.D, instrument.roles.settle.D],
    additional: Vector[SettlementConversion[instrument.roles.settle.D]]
  ): Either[MarketStateViolations, instrument.MarketState] =
    NonZero(price.rate) match
      case Left(_) =>
        Left(
          MarketStateViolations.one(
            MarketStateViolation.InvalidConversion(
              instrument.roles.quote.id,
              instrument.roles.settle.id,
              Rational.zero,
              ConversionFailureReason.NonPositive
            )
          )
        )
      case Right(nonZeroPrice) =>
        val quoteToSettle = nonZeroPrice.reciprocalRate.andThen(baseToSettle)
        checked(instrument)(price, baseToSettle, quoteToSettle, additional)

  def fromRates(
    instrument: Instrument
  )(
    price: instrument.Price,
    baseToSettle: Rate[instrument.roles.base.D, instrument.roles.settle.D],
    quoteToSettle: Rate[instrument.roles.quote.D, instrument.roles.settle.D]
  ): Either[MarketStateViolations, instrument.MarketState] =
    fromRates(instrument)(price, baseToSettle, quoteToSettle, Vector.empty)

  def fromRates(
    instrument: Instrument
  )(
    price: instrument.Price,
    baseToSettle: Rate[instrument.roles.base.D, instrument.roles.settle.D],
    quoteToSettle: Rate[instrument.roles.quote.D, instrument.roles.settle.D],
    additional: Vector[SettlementConversion[instrument.roles.settle.D]]
  ): Either[MarketStateViolations, instrument.MarketState] =
    checked(instrument)(price, baseToSettle, quoteToSettle, additional)

  private def checked(
    instrument: Instrument
  )(
    price: instrument.Price,
    baseRate: Rate[instrument.roles.base.D, instrument.roles.settle.D],
    quoteRate: Rate[instrument.roles.quote.D, instrument.roles.settle.D],
    additional: Vector[SettlementConversion[instrument.roles.settle.D]]
  ): Either[MarketStateViolations, instrument.MarketState] =
    val settle: Asset { type D = instrument.roles.settle.D } = instrument.roles.settle
    val base: Asset { type D = instrument.roles.base.D }     = instrument.roles.base
    val quote: Asset { type D = instrument.roles.quote.D }   = instrument.roles.quote

    val identityViolations =
      Vector.newBuilder[MarketStateViolation]
    if price.instrumentId != instrument.identity.id then
      identityViolations += MarketStateViolation.InstrumentMismatch(
        "price",
        instrument.identity.id,
        price.instrumentId
      )
    additional.zipWithIndex.foreach: (value, index) =>
      if value.instrumentId != instrument.identity.id then
        identityViolations += MarketStateViolation.InstrumentMismatch(
          s"additional[$index]",
          instrument.identity.id,
          value.instrumentId
        )

    val anchorValidityViolations = Vector(
      SettlementConversion.validate(base.id, settle.id, baseRate.coefficient),
      SettlementConversion.validate(quote.id, settle.id, quoteRate.coefficient)
    ).flatten
    val anchorCoherenceViolations =
      Option
        .when(
          anchorValidityViolations.isEmpty &&
            price.rate.andThen(quoteRate).coefficient != baseRate.coefficient
        )(
          MarketStateViolation.IncoherentAnchors(
            price.coefficient,
            baseRate.coefficient,
            quoteRate.coefficient
          )
        )
        .toVector

    val generatedSourceIds        = Set(base.id, quote.id, settle.id)
    val (_, additionalViolations) =
      additional.zipWithIndex.foldLeft(
        generatedSourceIds -> Vector.empty[MarketStateViolation]
      ):
        case ((seen, violations), (candidate, index)) =>
          val targetErrors = Asset
            .reconcile(candidate.target, settle)
            .left
            .toOption
            .map(cause => MarketStateViolation.ReferenceData(s"additional[$index].target", cause))
            .toVector
          val sourceErrors = DimensionHandle
            .sameLineage(candidate.source.dimension, settle.dimension)
            .left
            .toOption
            .map(cause => MarketStateViolation.ReferenceData(s"additional[$index].source", cause))
            .toVector
          val numericErrors = SettlementConversion
            .validate(candidate.source.id, candidate.target.id, candidate.coefficient)
            .toVector
          val targetIdentityErrors = Option
            .when(candidate.target.id != settle.id)(
              MarketStateViolation.InvalidConversion(
                candidate.source.id,
                candidate.target.id,
                candidate.coefficient,
                ConversionFailureReason.TargetIsNotSettle
              )
            )
            .toVector
          val duplicateErrors = Option
            .when(seen.contains(candidate.source.id))(
              MarketStateViolation.DuplicateSource(candidate.source.id)
            )
            .toVector
          (
            seen + candidate.source.id,
            violations ++ targetErrors ++ sourceErrors ++ numericErrors ++ targetIdentityErrors ++ duplicateErrors
          )

    val violations =
      identityViolations.result() ++
        anchorValidityViolations ++
        anchorCoherenceViolations ++
        additionalViolations
    MarketStateViolations.from(violations) match
      case Some(errors) => Left(errors)
      case None         =>
        for
          baseConversion <- SettlementConversion
                              .fromRate(instrument)(base)(baseRate)
                              .left
                              .map(MarketStateViolations.one)
          quoteConversion <- SettlementConversion
                               .fromRate(instrument)(quote)(quoteRate)
                               .left
                               .map(MarketStateViolations.one)
          settleConversion <- SettlementConversion
                                .fromRate(instrument)(settle)(
                                  Rate(settle.dimension.ref, settle.dimension.ref, Rational.one)
                                )
                                .left
                                .map(MarketStateViolations.one)
        yield new MarketState(
          instrument.identity.id,
          base,
          quote,
          settle,
          price,
          baseRate,
          quoteRate,
          deduplicateGenerated(Vector(baseConversion, quoteConversion, settleConversion)) ++ additional,
          additional
        )
    end match
  end checked

  private def deduplicateGenerated[S <: Dim](
    values: Vector[SettlementConversion[S]]
  ): Vector[SettlementConversion[S]] =
    values.foldLeft(Vector.empty[SettlementConversion[S]]): (result, candidate) =>
      if result.exists(_.source.id == candidate.source.id) then result else result :+ candidate
end MarketState
