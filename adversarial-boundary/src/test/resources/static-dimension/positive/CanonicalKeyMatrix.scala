package external.fixtures.positive

import scala.annotation.StaticAnnotation

import trading.quantity.*

object CanonicalKeyMatrix:
  final class Marker extends StaticAnnotation

  object NominalKey extends DimRef.NominalAtom(AtomId("matrix:nominal"))
  object StableModuleKey

  val stableValueKey = new Object

  type Literal = Atom["matrix:literal"]
  type LiteralAlias = "matrix:alias"
  type AliasedLiteral = Atom[LiteralAlias]
  type AnnotatedLiteralKey = "matrix:annotated" @Marker
  type AnnotatedLiteral = Atom[AnnotatedLiteralKey]
  type Nominal = Atom[NominalKey.type]
  type StableModule = Atom[StableModuleKey.type]
  type StableValue = Atom[stableValueKey.type]

  val literalNormalization = Normalize.derived[Literal]
  val aliasNormalization = Normalize.derived[AliasedLiteral]
  val annotatedNormalization = Normalize.derived[AnnotatedLiteral]
  val nominalNormalization = Normalize.derived[Nominal]
  val stableModuleNormalization = Normalize.derived[StableModule]
  val stableValueNormalization = Normalize.derived[StableValue]

  val literal: DimRef[Literal] = DimRef.atom["matrix:literal"]
  val nominalFirst: DimRef[Nominal] = DimRef.atom(NominalKey)
  val nominalSecond: DimRef[Nominal] = DimRef.atom(NominalKey)

  val generated = DimRef.atomic(AtomId("matrix:generated"))
  val generatedNormalization = Normalize.derived[generated.D]

  val fresh = DimRef.fresh(DimensionKey.atom(AtomId("matrix:fresh")))
  val freshNormalization = Normalize.derived[fresh.D]

  final class ThisKey:
    type D = Atom[this.type]
    val normalization = Normalize.derived[D]

end CanonicalKeyMatrix
