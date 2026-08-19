package external.fixtures.positive

import trading.quantity.*

object AssociationIndependentOrder:
  type A = Atom["association:A"]
  type B = Atom["association:B"]

  type LeftAssociated  = Times[Times[A, Inverse[A]], Times[B, A]]
  type RightAssociated = Times[A, Times[Inverse[A], Times[B, A]]]
  type Expected = Dim[Power["association:A", 1] *: Power["association:B", 1] *: EmptyTuple]

  val left: Normalize.Aux[LeftAssociated, Expected] = Normalize.derived[LeftAssociated]
  val right: Normalize.Aux[RightAssociated, Expected] = Normalize.derived[RightAssociated]

end AssociationIndependentOrder
