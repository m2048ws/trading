package external.fixtures.positive

import trading.quantity.*

object ClosedDimensionCaller:
  type BTC = Atom["asset:BTC"]
  type USD = Atom["asset:USD"]
  type EUR = Atom["asset:EUR"]

  val btc: DimRef[BTC] = DimRef.atom["asset:BTC"]
  val usd: DimRef[USD] = DimRef.atom["asset:USD"]
  val eur: DimRef[EUR] = DimRef.atom["asset:EUR"]

  val amount: Quantity[BTC]   = Quantity(btc, Rational(1, 10))
  val btcToUsd: Rate[BTC, USD] = Rate(btc, usd, Rational(60000))
  val notional: Quantity[USD] = amount * btcToUsd

  val usdToEur: Rate[USD, EUR] = Rate(usd, eur, Rational(9, 10))
  val btcToEur: Rate[BTC, EUR] = btcToUsd.andThen(usdToEur)
  val euros: Quantity[EUR]     = amount.applyRate(btcToEur)

  val identity: DimRef[One] = DimRef.divide(usd, usd)

  val commuted: SameDimension[Times[BTC, USD], Times[USD, BTC]] = summon

end ClosedDimensionCaller
