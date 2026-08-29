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

  val literalInterpretation: SameDimension[Times[Literal, One], Literal] = summon
  val aliasInterpretation: SameDimension[Times[AliasedLiteral, One], AliasedLiteral] = summon
  val annotatedInterpretation: SameDimension[Times[AnnotatedLiteral, One], AnnotatedLiteral] = summon
  val nominalInterpretation: SameDimension[Times[Nominal, One], Nominal] = summon
  val stableModuleInterpretation: SameDimension[Times[StableModule, One], StableModule] = summon
  val stableValueInterpretation: SameDimension[Times[StableValue, One], StableValue] = summon

  val literal: DimRef[Literal] = DimRef.atom["matrix:literal"]
  val nominalFirst: DimRef[Nominal] = DimRef.atom(NominalKey)
  val nominalSecond: DimRef[Nominal] = DimRef.atom(NominalKey)

  val generated = DimRef.atomic(AtomId("matrix:generated"))
  val generatedInterpretation: SameDimension[Times[generated.D, One], generated.D] = summon

  val fresh = DimRef.fresh(DimKey.atom(AtomId("matrix:fresh")))
  val freshInterpretation: SameDimension[Times[fresh.D, One], fresh.D] = summon

  final class ThisKey:
    type D = Atom[this.type]
    val interpretation: SameDimension[Times[D, One], D] = summon

end CanonicalKeyMatrix
