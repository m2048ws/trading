package trading.economics.instrument

import cats.syntax.all.*

import trading.quantity.*
import trading.quantity.refinement.NonZero
import trading.reference.*

/** One trusted source and its endpoint-typed conversion into settlement. */
final class SettlementConversion[S <: Dim] private[instrument] (
  val instrumentId: InstrumentId,
  val source: Asset
)(
  val target: Asset { type D = S },
  val rate: Rate[source.D, S]):

  def coefficient: Rational = rate.coefficient

final class MarketState[B <: Dim, Q <: Dim, S <: Dim] private[instrument] (
  val instrumentId: InstrumentId,
  val price: Price[B, Q],
  val baseToSettle: Rate[B, S],
  val quoteToSettle: Rate[Q, S],
  conversions: Vector[SettlementConversion[S]]):

  private val byId                       = conversions.map(value => value.source.id -> value).toMap
  val conversionSources: Vector[AssetId] = conversions.map(_.source.id)

  def convertToSettle(source: Asset)(value: Quantity[source.D]): Either[EconomicsError, Quantity[S]] =
    byId.get(source.id) match
      case None => Left(MissingConversion(source.id, None, None))
      case Some(conversion) if DimensionHandle.sameLineage(conversion.source.dimension, source.dimension).isLeft =>
        Left(
          ForeignReferenceDataLineage(
            "conversion lookup",
            conversion.source.dimension.key,
            source.dimension.key
          )
        )
      case Some(conversion) =>
        Asset
          .reconcile(source, conversion.source)
          .left
          .map(error => ReferenceDataFailure("conversion lookup", error))
          .map: same =>
            val aligned = value.alignTo[conversion.source.D](using same)
            aligned.applyRate(conversion.rate)

end MarketState

final class Market[B <: Dim, Q <: Dim, S <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  base: Asset { type D = B },
  quote: Asset { type D = Q },
  settle: Asset { type D = S }):

  def conversion(source: Asset, coefficient: Rational): Either[EconomicsError, SettlementConversion[S]] =
    conversionFromRate(source)(Rate(source.dimension.ref, settle.dimension.ref, coefficient))

  def conversionFromRate(
    source: Asset
  )(
    rate: Rate[source.D, S]
  ): Either[EconomicsError, SettlementConversion[S]] =
    if DimensionHandle.sameLineage(source.dimension, settle.dimension).isLeft then
      Left(ForeignReferenceDataLineage("settlement conversion", settle.dimension.key, source.dimension.key))
    else
      validateConversion(source.id, rate.coefficient)
        .map(_ => new SettlementConversion(instrumentId, source)(settle, rate))

  def quoteSettled(
    price: Price[B, Q],
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    if settle.id != quote.id then
      Left(InvalidConversion(quote.id, settle.id, Rational.one, ConversionFailureReason.SettleIsNotQuote))
    else
      val quoteRate = Rate(quote.dimension.ref, settle.dimension.ref, Rational.one)
      checked(price, price.rate.andThen(quoteRate), quoteRate, additionalConversions)

  def baseSettled(
    price: Price[B, Q],
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    if settle.id != base.id then
      Left(InvalidConversion(base.id, settle.id, Rational.one, ConversionFailureReason.SettleIsNotBase))
    else
      val baseRate  = Rate(base.dimension.ref, settle.dimension.ref, Rational.one)
      val quoteRate = NonZero(price.rate).toOption.get.reciprocalRate.andThen(baseRate)
      checked(price, baseRate, quoteRate, additionalConversions)

  def fromQuoteAnchor(
    price: Price[B, Q],
    quoteToSettle: Rational,
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    fromQuoteRate(
      price,
      Rate(quote.dimension.ref, settle.dimension.ref, quoteToSettle),
      additionalConversions
    )

  def fromBaseAnchor(
    price: Price[B, Q],
    baseToSettle: Rational,
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    fromBaseRate(
      price,
      Rate(base.dimension.ref, settle.dimension.ref, baseToSettle),
      additionalConversions
    )

  def fromAnchors(
    price: Price[B, Q],
    baseToSettle: Rational,
    quoteToSettle: Rational,
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    fromRates(
      price,
      Rate(base.dimension.ref, settle.dimension.ref, baseToSettle),
      Rate(quote.dimension.ref, settle.dimension.ref, quoteToSettle),
      additionalConversions
    )

  def fromQuoteRate(
    price: Price[B, Q],
    quoteToSettle: Rate[Q, S],
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    checked(price, price.rate.andThen(quoteToSettle), quoteToSettle, additionalConversions)

  def fromBaseRate(
    price: Price[B, Q],
    baseToSettle: Rate[B, S],
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    val quoteToSettle = NonZero(price.rate).toOption.get.reciprocalRate.andThen(baseToSettle)
    checked(price, baseToSettle, quoteToSettle, additionalConversions)

  def fromRates(
    price: Price[B, Q],
    baseToSettle: Rate[B, S],
    quoteToSettle: Rate[Q, S],
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    checked(price, baseToSettle, quoteToSettle, additionalConversions)

  private def checked(
    price: Price[B, Q],
    baseRate: Rate[B, S],
    quoteRate: Rate[Q, S],
    additional: Vector[SettlementConversion[S]]
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    for
      _ <- IdentityChecks.check(
             "market",
             instrumentId,
             (Vector("price" -> price.instrumentId) ++ additional.zipWithIndex.map((value, index) =>
               s"additional[$index]" -> value.instrumentId
             ))*
           )
      _           <- validateAnchors(price.coefficient, baseRate.coefficient, quoteRate.coefficient)
      conversions <- buildConversions(baseRate, quoteRate, additional)
    yield new MarketState(instrumentId, price, baseRate, quoteRate, conversions)

  private def validateConversion(source: AssetId, coefficient: Rational): Either[EconomicsError, Unit] =
    if coefficient.signum <= 0 then
      Left(InvalidConversion(source, settle.id, coefficient, ConversionFailureReason.NonPositive))
    else if source == settle.id && coefficient != Rational.one then
      Left(InvalidConversion(source, settle.id, coefficient, ConversionFailureReason.IdentityNotOne))
    else Right(())

  private def validateAnchors(
    price: Rational,
    baseCoefficient: Rational,
    quoteCoefficient: Rational
  ): Either[EconomicsError, Unit] =
    if baseCoefficient.signum <= 0 then
      Left(InvalidConversion(base.id, settle.id, baseCoefficient, ConversionFailureReason.NonPositive))
    else if quoteCoefficient.signum <= 0 then
      Left(InvalidConversion(quote.id, settle.id, quoteCoefficient, ConversionFailureReason.NonPositive))
    else if settle.id == base.id && baseCoefficient != Rational.one then
      Left(InvalidConversion(base.id, settle.id, baseCoefficient, ConversionFailureReason.IdentityNotOne))
    else if settle.id == quote.id && quoteCoefficient != Rational.one then
      Left(InvalidConversion(quote.id, settle.id, quoteCoefficient, ConversionFailureReason.IdentityNotOne))
    else if price * quoteCoefficient != baseCoefficient then
      Left(IncoherentMarketState(price, baseCoefficient, quoteCoefficient))
    else Right(())

  private def buildConversions(
    baseRate: Rate[B, S],
    quoteRate: Rate[Q, S],
    additional: Vector[SettlementConversion[S]]
  ): Either[EconomicsError, Vector[SettlementConversion[S]]] =
    val generated = Vector(
      new SettlementConversion(instrumentId, base)(settle, baseRate),
      new SettlementConversion(instrumentId, quote)(settle, quoteRate),
      new SettlementConversion(instrumentId, settle)(
        settle,
        Rate(settle.dimension.ref, settle.dimension.ref, Rational.one)
      )
    )

    val seed = generated.foldM(Vector.empty[SettlementConversion[S]]): (accumulated, candidate) =>
      accumulated.indexWhere(_.source.id == candidate.source.id) match
        case -1                                                               => Right(accumulated :+ candidate)
        case index if accumulated(index).coefficient == candidate.coefficient => Right(accumulated)
        case index                                                            =>
          Left(
            InvalidConversion(
              accumulated(index).source.id,
              settle.id,
              accumulated(index).coefficient,
              ConversionFailureReason.IdentityNotOne
            )
          )

    seed.flatMap: initial =>
      additional.foldM(initial): (accumulated, candidate) =>
        if candidate.target.id != settle.id ||
          Asset.reconcile(candidate.target, settle).isLeft
        then
          Left(
            InvalidConversion(
              candidate.source.id,
              candidate.target.id,
              candidate.coefficient,
              ConversionFailureReason.TargetIsNotSettle
            )
          )
        else if DimensionHandle.sameLineage(candidate.source.dimension, settle.dimension).isLeft then
          Left(
            ForeignReferenceDataLineage("additional conversion", settle.dimension.key, candidate.source.dimension.key)
          )
        else if accumulated.exists(_.source.id == candidate.source.id) then
          Left(DuplicateConversion(candidate.source.id))
        else Right(accumulated :+ candidate)
  end buildConversions

end Market
