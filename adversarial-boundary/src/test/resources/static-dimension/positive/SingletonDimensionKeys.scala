package external.fixtures.positive

import trading.quantity.*

object SingletonDimensionKeys:
  object NominalKey extends DimRef.NominalAtom(AtomId("nominal:key"))

  type Literal = Atom["literal:key"]
  type Nominal = Atom[NominalKey.type]

  val literal: DimRef[Literal] = DimRef.atom["literal:key"]
  val nominal: DimRef[Nominal] = DimRef.atom(NominalKey)

  val generative = DimRef.atomic(AtomId("generative:key"))
  val generated: Quantity[generative.D] = Quantity(generative.dimension, 1)

  val runtime = DimRef.fresh(DimKey.atom(AtomId("runtime:key")))
  val loaded: Quantity[runtime.D] = Quantity(runtime.dimension, 1)

end SingletonDimensionKeys
