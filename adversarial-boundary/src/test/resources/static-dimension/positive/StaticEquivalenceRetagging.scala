package external.fixtures.positive

import trading.quantity.*

object StaticEquivalenceRetagging:
  type A = Atom["equivalence:A"]
  type B = Atom["equivalence:B"]

  type AB = Times[A, B]
  type BA = Times[B, A]

  val equivalence: SameDimension[AB, BA] = summon
  val source: Quantity[AB]               = Quantity.zero[AB]
  val viaEvidence: Quantity[BA]          = equivalence.coerceQuantity(source)
  val viaQuantity: Quantity[BA]          = source.alignTo[BA]

end StaticEquivalenceRetagging
