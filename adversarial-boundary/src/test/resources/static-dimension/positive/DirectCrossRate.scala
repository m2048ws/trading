package external.fixtures.positive

import trading.quantity.*
import trading.quantity.refinement.*

object DirectCrossRate:
  type BTC = Atom["cross:BTC"]
  type USD = Atom["cross:USD"]
  type ETH = Atom["cross:ETH"]

  val btc: DimRef[BTC] = DimRef.atom["cross:BTC"]
  val usd: DimRef[USD] = DimRef.atom["cross:USD"]
  val eth: DimRef[ETH] = DimRef.atom["cross:ETH"]

  val usdPerBtc: Rate[BTC, USD] = Rate(btc, usd, Rational(60000))
  val usdPerEth: Rate[ETH, USD] = Rate(eth, usd, Rational(3000))
  val divisor: NonZero[Rate[ETH, USD]] = NonZero(usdPerEth).toOption.get

  val ethPerBtc: Rate[BTC, ETH] = usdPerBtc.crossRate(divisor)

end DirectCrossRate
