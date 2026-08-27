package trading.economics

import trading.quantity.*
import trading.quantity.runtime.AssetRef

private[economics] final class InstrumentMarketImpl[
  O,
  B <: Dimension,
  Q <: Dimension,
  S <: Dimension
](
  authority: Instrument.OwnerAuthority[O],
  base: AssetRef { type D = B },
  quote: AssetRef { type D = Q },
  settle: AssetRef { type D = S })
  extends MarketCapability[O, B, Q, S]:

  def conversion(
    source: AssetRef,
    coefficient: Rational
  ): Either[EconomicsError, InstrumentSettlementConversion[O, S]] =
    if !source.dimension.sharesRegistryWith(settle.dimension) then
      Left(ForeignRegistry("settlement conversion", settle.dimension.key, source.dimension.key))
    else
      validateConversion(source.id, coefficient)
        .map(_ => authority.conversion(source, settle, coefficient))

  def conversionFromRate(
    source: AssetRef
  )(
    rate: Rate[source.D, S]
  ): Either[EconomicsError, InstrumentSettlementConversion[O, S]] =
    conversion(source, rate.coefficient)

  def quoteSettled(
    price: InstrumentPrice[O, B, Q],
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]]
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]] =
    if settle.id != quote.id then
      Left(InvalidConversion(quote.id, settle.id, Rational.one, ConversionFailureReason.SettleIsNotQuote))
    else checked(price, price.coefficient, Rational.one, additionalConversions)

  def baseSettled(
    price: InstrumentPrice[O, B, Q],
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]]
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]] =
    if settle.id != base.id then
      Left(InvalidConversion(base.id, settle.id, Rational.one, ConversionFailureReason.SettleIsNotBase))
    else
      Rational.one / price.coefficient match
        case Left(_)                 => Left(InvalidPriceCoordinate(price.ticks.unrefined))
        case Right(quoteCoefficient) => checked(price, Rational.one, quoteCoefficient, additionalConversions)

  def fromQuoteAnchor(
    price: InstrumentPrice[O, B, Q],
    quoteToSettle: Rational,
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]]
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]] =
    fromQuoteRate(
      price,
      Rate(quote.dimension.asDimensionRef, settle.dimension.asDimensionRef, quoteToSettle),
      additionalConversions
    )

  def fromBaseAnchor(
    price: InstrumentPrice[O, B, Q],
    baseToSettle: Rational,
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]]
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]] =
    fromBaseRate(
      price,
      Rate(base.dimension.asDimensionRef, settle.dimension.asDimensionRef, baseToSettle),
      additionalConversions
    )

  def fromAnchors(
    price: InstrumentPrice[O, B, Q],
    baseToSettle: Rational,
    quoteToSettle: Rational,
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]]
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]] =
    fromRates(
      price,
      Rate(base.dimension.asDimensionRef, settle.dimension.asDimensionRef, baseToSettle),
      Rate(quote.dimension.asDimensionRef, settle.dimension.asDimensionRef, quoteToSettle),
      additionalConversions
    )

  def fromQuoteRate(
    price: InstrumentPrice[O, B, Q],
    quoteToSettle: Rate[Q, S],
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]]
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]] =
    checked(price, price.coefficient * quoteToSettle.coefficient, quoteToSettle.coefficient, additionalConversions)

  def fromBaseRate(
    price: InstrumentPrice[O, B, Q],
    baseToSettle: Rate[B, S],
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]]
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]] =
    baseToSettle.coefficient / price.coefficient match
      case Left(_)                 => Left(InvalidPriceCoordinate(price.ticks.unrefined))
      case Right(quoteCoefficient) =>
        checked(price, baseToSettle.coefficient, quoteCoefficient, additionalConversions)

  def fromRates(
    price: InstrumentPrice[O, B, Q],
    baseToSettle: Rate[B, S],
    quoteToSettle: Rate[Q, S],
    additionalConversions: Vector[InstrumentSettlementConversion[O, S]]
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]] =
    checked(price, baseToSettle.coefficient, quoteToSettle.coefficient, additionalConversions)

  private def checked(
    price: InstrumentPrice[O, B, Q],
    baseCoefficient: Rational,
    quoteCoefficient: Rational,
    additional: Vector[InstrumentSettlementConversion[O, S]]
  ): Either[EconomicsError, InstrumentMarketState[O, B, Q, S]] =
    validateAnchors(price.coefficient, baseCoefficient, quoteCoefficient).flatMap: _ =>
      buildConversions(baseCoefficient, quoteCoefficient, additional).map: conversions =>
        authority.marketState(
          price,
          Rate(base.dimension.asDimensionRef, settle.dimension.asDimensionRef, baseCoefficient),
          Rate(quote.dimension.asDimensionRef, settle.dimension.asDimensionRef, quoteCoefficient),
          settle.dimension.asDimensionRef,
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
    additional: Vector[InstrumentSettlementConversion[O, S]]
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

end InstrumentMarketImpl
