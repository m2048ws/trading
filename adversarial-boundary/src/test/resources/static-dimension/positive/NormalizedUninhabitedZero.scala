package external.fixtures.positive

import trading.quantity.*

object NormalizedUninhabitedZero:
  object StaticOnlyKey

  type StaticOnly = Atom[StaticOnlyKey.type]

  val normalization: Normalize[StaticOnly] = summon
  val zero: Quantity[StaticOnly]           = Quantity.zero[StaticOnly]

end NormalizedUninhabitedZero
