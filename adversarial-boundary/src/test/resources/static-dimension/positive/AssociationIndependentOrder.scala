package external.fixtures.positive

import trading.quantity.*

object AssociationIndependentOrder:
  type A = Atom["association:A"]
  type B = Atom["association:B"]

  type LeftAssociated  = Times[Times[A, Inverse[A]], Times[B, A]]
  type RightAssociated = Times[A, Times[Inverse[A], Times[B, A]]]
  val equivalent: SameDimension[LeftAssociated, RightAssociated] = summon

end AssociationIndependentOrder
