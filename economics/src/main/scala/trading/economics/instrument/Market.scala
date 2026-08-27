package trading.economics.instrument

import trading.quantity.*
import trading.quantity.runtime.AssetRef

final case class SettlementConversion[S <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  source: AssetRef,
  target: AssetRef { type D = S },
  coefficient: Rational)

final class MarketState[B <: Dim, Q <: Dim, S <: Dim] private[instrument] (
  val instrumentId: InstrumentId,
  val price: Price[B, Q],
  val baseToSettle: Rate[B, S],
  val quoteToSettle: Rate[Q, S],
  settleRef: DimRef[S],
  conversions: Vector[(AssetRef, Rational)]):

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

end MarketState

final class Market[B <: Dim, Q <: Dim, S <: Dim] private[instrument] (
  instrumentId: InstrumentId,
  base: AssetRef { type D = B },
  quote: AssetRef { type D = Q },
  settle: AssetRef { type D = S }):

  def conversion(source: AssetRef, coefficient: Rational): Either[EconomicsError, SettlementConversion[S]] =
    if !source.dimension.sharesRegistryWith(settle.dimension) then
      Left(ForeignRegistry("settlement conversion", settle.dimension.key, source.dimension.key))
    else
      validateConversion(source.id, coefficient)
        .map(_ => SettlementConversion(instrumentId, source, settle, coefficient))

  def conversionFromRate(
    source: AssetRef
  )(
    rate: Rate[source.D, S]
  ): Either[EconomicsError, SettlementConversion[S]] =
    conversion(source, rate.coefficient)

  def quoteSettled(
    price: Price[B, Q],
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    if settle.id != quote.id then
      Left(InvalidConversion(quote.id, settle.id, Rational.one, ConversionFailureReason.SettleIsNotQuote))
    else checked(price, price.coefficient, Rational.one, additionalConversions)

  def baseSettled(
    price: Price[B, Q],
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    if settle.id != base.id then
      Left(InvalidConversion(base.id, settle.id, Rational.one, ConversionFailureReason.SettleIsNotBase))
    else
      Rational.one / price.coefficient match
        case Left(_)                 => Left(InvalidPriceCoordinate(price.ticks.unrefined))
        case Right(quoteCoefficient) => checked(price, Rational.one, quoteCoefficient, additionalConversions)

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
    checked(price, price.coefficient * quoteToSettle.coefficient, quoteToSettle.coefficient, additionalConversions)

  def fromBaseRate(
    price: Price[B, Q],
    baseToSettle: Rate[B, S],
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    baseToSettle.coefficient / price.coefficient match
      case Left(_)                 => Left(InvalidPriceCoordinate(price.ticks.unrefined))
      case Right(quoteCoefficient) =>
        checked(price, baseToSettle.coefficient, quoteCoefficient, additionalConversions)

  def fromRates(
    price: Price[B, Q],
    baseToSettle: Rate[B, S],
    quoteToSettle: Rate[Q, S],
    additionalConversions: Vector[SettlementConversion[S]] = Vector.empty
  ): Either[EconomicsError, MarketState[B, Q, S]] =
    checked(price, baseToSettle.coefficient, quoteToSettle.coefficient, additionalConversions)

  private def checked(
    price: Price[B, Q],
    baseCoefficient: Rational,
    quoteCoefficient: Rational,
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
      _           <- validateAnchors(price.coefficient, baseCoefficient, quoteCoefficient)
      conversions <- buildConversions(baseCoefficient, quoteCoefficient, additional)
    yield new MarketState(
      instrumentId,
      price,
      Rate(base.dimension.ref, settle.dimension.ref, baseCoefficient),
      Rate(quote.dimension.ref, settle.dimension.ref, quoteCoefficient),
      settle.dimension.ref,
      conversions
    )

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
    baseCoefficient: Rational,
    quoteCoefficient: Rational,
    additional: Vector[SettlementConversion[S]]
  ): Either[EconomicsError, Vector[(AssetRef, Rational)]] =
    val generated = Vector(base -> baseCoefficient, quote -> quoteCoefficient, settle -> Rational.one)
    val initial   = generated.foldLeft[Either[EconomicsError, Vector[(AssetRef, Rational)]]](Right(Vector.empty)):
      (result, candidate) =>
        result.flatMap: accumulated =>
          accumulated.indexWhere(_._1.id == candidate._1.id) match
            case -1                                             => Right(accumulated :+ candidate)
            case index if accumulated(index)._2 == candidate._2 => Right(accumulated)
            case index                                          =>
              Left(
                InvalidConversion(
                  accumulated(index)._1.id,
                  settle.id,
                  accumulated(index)._2,
                  ConversionFailureReason.IdentityNotOne
                )
              )

    initial.flatMap: seed =>
      additional.foldLeft[Either[EconomicsError, Vector[(AssetRef, Rational)]]](Right(seed)): (result, candidate) =>
        result.flatMap: accumulated =>
          if candidate.target.id != settle.id || candidate.target.dimension.key != settle.dimension.key then
            Left(
              InvalidConversion(
                candidate.source.id,
                candidate.target.id,
                candidate.coefficient,
                ConversionFailureReason.TargetIsNotSettle
              )
            )
          else if !candidate.source.dimension.sharesRegistryWith(settle.dimension) then
            Left(ForeignRegistry("additional conversion", settle.dimension.key, candidate.source.dimension.key))
          else if accumulated.exists(_._1.id == candidate.source.id) then Left(DuplicateConversion(candidate.source.id))
          else Right(accumulated :+ candidate.source -> candidate.coefficient)
  end buildConversions

end Market
