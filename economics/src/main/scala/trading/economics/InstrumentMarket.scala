package trading.economics

import trading.quantity.*
import trading.quantity.runtime.AssetRef

private[economics] object InstrumentMarket:
  final case class ConversionPlan(source: AssetRef, coefficient: Rational)
  final case class StatePlan(conversions: Vector[ConversionPlan])

  def validateConversion(source: AssetId, target: AssetId, coefficient: Rational): Either[EconomicsError, Unit] =
    if coefficient.signum <= 0 then
      Left(InvalidConversion(source, target, coefficient, "conversion must be positive"))
    else if source == target && coefficient != Rational.one then
      Left(InvalidConversion(source, target, coefficient, "identity conversion must equal one"))
    else Right(())

  def quoteSettled(
    base: AssetRef,
    quote: AssetRef,
    settle: AssetRef,
    price: Rational,
    additional: Vector[SettlementConversion]
  ): Either[EconomicsError, StatePlan] =
    if settle.id != quote.id then
      Left(InvalidConversion(quote.id, settle.id, Rational.one, "settle asset is not quote"))
    else checked(base, quote, settle, price, price, Rational.one, additional)

  def baseSettled(
    base: AssetRef,
    quote: AssetRef,
    settle: AssetRef,
    price: Rational,
    priceCoordinate: BigInt,
    additional: Vector[SettlementConversion]
  ): Either[EconomicsError, StatePlan] =
    if settle.id != base.id then
      Left(InvalidConversion(base.id, settle.id, Rational.one, "settle asset is not base"))
    else
      Rational.one / price match
        case Left(_)            => Left(InvalidPriceCoordinate(priceCoordinate))
        case Right(coefficient) => checked(base, quote, settle, price, Rational.one, coefficient, additional)

  def fromQuote(
    base: AssetRef,
    quote: AssetRef,
    settle: AssetRef,
    price: Rational,
    quoteToSettle: Rational,
    additional: Vector[SettlementConversion]
  ): Either[EconomicsError, StatePlan] =
    checked(base, quote, settle, price, price * quoteToSettle, quoteToSettle, additional)

  def fromBase(
    base: AssetRef,
    quote: AssetRef,
    settle: AssetRef,
    price: Rational,
    priceCoordinate: BigInt,
    baseToSettle: Rational,
    additional: Vector[SettlementConversion]
  ): Either[EconomicsError, StatePlan] =
    baseToSettle / price match
      case Left(_)            => Left(InvalidPriceCoordinate(priceCoordinate))
      case Right(coefficient) => checked(base, quote, settle, price, baseToSettle, coefficient, additional)

  def checked(
    base: AssetRef,
    quote: AssetRef,
    settle: AssetRef,
    price: Rational,
    baseToSettle: Rational,
    quoteToSettle: Rational,
    additional: Vector[SettlementConversion]
  ): Either[EconomicsError, StatePlan] =
    validateAnchors(base.id, quote.id, settle.id, price, baseToSettle, quoteToSettle)
      .flatMap(_ => buildConversions(base, quote, settle, baseToSettle, quoteToSettle, additional))

  def lookup(
    source: AssetRef,
    conversions: Map[AssetId, ConversionPlan]
  ): Either[EconomicsError, Rational] =
    conversions.get(source.id) match
      case None => Left(MissingConversion(source.id, None, None))
      case Some(conversion)
        if conversion.source.dimension.key != source.dimension.key ||
          !conversion.source.dimension.sharesRegistryWith(source.dimension) =>
        Left(ForeignRegistry("conversion lookup", conversion.source.dimension.key, source.dimension.key))
      case Some(conversion) => Right(conversion.coefficient)

  private def validateAnchors(
    base: AssetId,
    quote: AssetId,
    settle: AssetId,
    price: Rational,
    baseToSettle: Rational,
    quoteToSettle: Rational
  ): Either[EconomicsError, Unit] =
    if baseToSettle.signum <= 0 then
      Left(InvalidConversion(base, settle, baseToSettle, "conversion must be positive"))
    else if quoteToSettle.signum <= 0 then
      Left(InvalidConversion(quote, settle, quoteToSettle, "conversion must be positive"))
    else if settle == base && baseToSettle != Rational.one then
      Left(InvalidConversion(base, settle, baseToSettle, "settlement identity conversion must equal one"))
    else if settle == quote && quoteToSettle != Rational.one then
      Left(InvalidConversion(quote, settle, quoteToSettle, "settlement identity conversion must equal one"))
    else if price * quoteToSettle != baseToSettle then
      Left(IncoherentMarketState(price, baseToSettle, quoteToSettle))
    else Right(())

  private def buildConversions(
    base: AssetRef,
    quote: AssetRef,
    settle: AssetRef,
    baseToSettle: Rational,
    quoteToSettle: Rational,
    additional: Vector[SettlementConversion]
  ): Either[EconomicsError, StatePlan] =
    val generated = Vector(
      ConversionPlan(base, baseToSettle),
      ConversionPlan(quote, quoteToSettle),
      ConversionPlan(settle, Rational.one)
    )
    val generatedResult = generated.foldLeft[Either[EconomicsError, Vector[ConversionPlan]]](Right(Vector.empty)):
      (result, candidate) =>
        result.flatMap: accumulated =>
          accumulated.indexWhere(_.source.id == candidate.source.id) match
            case -1                                                               => Right(accumulated :+ candidate)
            case index if accumulated(index).coefficient == candidate.coefficient => Right(accumulated)
            case index                                                            =>
              Left(
                InvalidConversion(
                  accumulated(index).source.id,
                  settle.id,
                  accumulated(index).coefficient,
                  "settlement identity conversion must equal one"
                )
              )

    generatedResult
      .flatMap: initial =>
        additional.foldLeft[Either[EconomicsError, Vector[ConversionPlan]]](Right(initial)): (result, candidate) =>
          result.flatMap: accumulated =>
            if candidate.target.id != settle.id || candidate.target.dimension.key != settle.dimension.key then
              Left(
                InvalidConversion(
                  candidate.source.id,
                  candidate.target.id,
                  candidate.coefficient,
                  "conversion target is not settle"
                )
              )
            else if !candidate.source.dimension.sharesRegistryWith(settle.dimension) then
              Left(ForeignRegistry("additional conversion", settle.dimension.key, candidate.source.dimension.key))
            else if candidate.coefficient.signum <= 0 then
              Left(
                InvalidConversion(
                  candidate.source.id,
                  candidate.target.id,
                  candidate.coefficient,
                  "conversion must be positive"
                )
              )
            else if accumulated.exists(_.source.id == candidate.source.id) then
              Left(DuplicateConversion(candidate.source.id))
            else Right(accumulated :+ ConversionPlan(candidate.source, candidate.coefficient))
      .map(StatePlan.apply)
  end buildConversions

end InstrumentMarket
