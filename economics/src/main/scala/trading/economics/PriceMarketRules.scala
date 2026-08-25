package trading.economics

import trading.quantity.AssetId
import trading.quantity.Rational

private[economics] object PriceMarketRules:
  def validatePriceCoordinate(coordinate: BigInt): Either[EconomicsError, Unit] =
    if coordinate.signum <= 0 then Left(InvalidPriceCoordinate(coordinate)) else Right(())

  def validateAnchors(
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

  def validateConversion(source: AssetId, target: AssetId, coefficient: Rational): Either[EconomicsError, Unit] =
    if coefficient.signum <= 0 then
      Left(InvalidConversion(source, target, coefficient, "conversion must be positive"))
    else if source == target && coefficient != Rational.one then
      Left(InvalidConversion(source, target, coefficient, "identity conversion must equal one"))
    else Right(())

end PriceMarketRules
