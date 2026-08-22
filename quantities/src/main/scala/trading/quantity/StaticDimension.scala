package trading.quantity

import scala.annotation.implicitNotFound
import scala.quoted.*

/** One concrete stable singleton-key atom and its nonzero literal `Int` exponent in a declared canonical dimension. */
sealed trait Power[Key <: Singleton, Exponent <: Int]

/**
 * Restricted evidence that two dimension types denote the same dimension and may authorize explicit `alignTo`
 * operations on dimensional values.
 *
 * Reflexive evidence is ordinary Scala type identity and does not certify that either type belongs to the closed static
 * grammar. Non-reflexive static derivation validates both complete representations; runtime recovery issues the same
 * capability only after authoritative [[DimensionKey]] equality. Evidence alone provides no [[DimRef]] and exposes no
 * runtime key.
 */
@implicitNotFound("The requested dimensions are not equivalent; provide checked SameDimension evidence")
sealed trait SameDimension[A <: Dimension, B <: Dimension]

object SameDimension:
  /** Scala type identity is safe without certifying a manually named canonical representation. */
  given reflexive[D <: Dimension]: SameDimension[D, D] with {}

  /** Derive evidence after both closed expressions normalize to equal powers modulo tuple order. */
  transparent inline given derived[A <: Dimension, B <: Dimension]: SameDimension[A, B] =
    ${ StaticDimensionMacros.sameDimension[A, B] }

  /** Recover type evidence only after authoritative runtime identities agree. */
  def between[A <: Dimension, B <: Dimension](l: DimRef[A], r: DimRef[B]): Option[SameDimension[A, B]] =
    Option.when(l.key == r.key):
      new SameDimension[A, B] {}

end SameDimension

/** Atomic quoted implementation for the closed static-dimension grammar. */
private object StaticDimensionMacros:

  def sameDimension[A <: Dimension: Type, B <: Dimension: Type](using Quotes): Expr[SameDimension[A, B]] =
    val engine = new Engine
    val left   = engine.interpret(engine.typeOf[A])
    val right  = engine.interpret(engine.typeOf[B])

    if !engine.equivalent(left, right) then
      quotes.reflect.report.errorAndAbort("The dimensions do not have equivalent canonical powers")

    '{ new SameDimension[A, B] {} }

  private final class Engine(using val quotes: Quotes):
    import quotes.reflect.*

    final case class Entry(key: TypeRepr, exponent: BigInt)

    def typeOf[T: Type]: TypeRepr = TypeRepr.of[T]

    private val dimensionType  = TypeRepr.of[Dimension]
    private val emptyTupleType = TypeRepr.of[EmptyTuple]

    private val dimConstructor       = constructorOf(TypeRepr.of[Dim[EmptyTuple]])
    private val timesConstructor     = constructorOf(TypeRepr.of[Times[One, One]])
    private val inverseConstructor   = constructorOf(TypeRepr.of[Inverse[One]])
    private val powerConstructor     = constructorOf(TypeRepr.of[Power["static-dimension-probe", 1]])
    private val tupleConsConstructor = constructorOf(TypeRepr.of[Int *: EmptyTuple])

    def interpret(raw: TypeRepr): List[Entry] =
      combine(flatten(raw, BigInt(1)))

    private def flatten(raw: TypeRepr, sign: BigInt): List[Entry] =
      val current = expose(raw)

      if current =:= dimensionType then
        invalid("the base Dimension type is outside the closed static grammar")
      else
        current match
          case AppliedType(constructor, List(entries)) if sameConstructor(constructor, dimConstructor) =>
            parseTuple(entries).map(entry => entry.copy(exponent = entry.exponent * sign))
          case AppliedType(constructor, List(left, right)) if sameConstructor(constructor, timesConstructor) =>
            flatten(left, sign) ++ flatten(right, sign)
          case AppliedType(constructor, List(argument)) if sameConstructor(constructor, inverseConstructor) =>
            flatten(argument, -sign)
          case _: AndType | _: OrType | _: Refinement | _: MatchType | _: TypeLambda | _: TypeBounds =>
            invalid("refined, intersected, union, match, and unresolved types are outside the closed static grammar")
          case _ =>
            invalid("expected Dim, Times, Inverse, Atom, One, or Divide")

    private def combine(entries: List[Entry]): List[Entry] =
      val accumulated = entries.foldLeft(List.empty[Entry]): (current, added) =>
        val index = current.indexWhere(existing => existing.key =:= added.key)
        if index < 0 then current :+ added
        else current.updated(index, current(index).copy(exponent = current(index).exponent + added.exponent))

      accumulated.filterNot(_.exponent == 0)

    def equivalent(left: List[Entry], right: List[Entry]): Boolean =
      left.size == right.size && left.forall: entry =>
        right.exists(candidate => candidate.exponent == entry.exponent && candidate.key =:= entry.key)

    private def parseTuple(raw: TypeRepr): List[Entry] =
      def loop(value: TypeRepr, seen: List[TypeRepr]): List[Entry] =
        val current = expose(value)
        if current =:= emptyTupleType then Nil
        else
          current match
            case AppliedType(constructor, List(head, tail)) if sameConstructor(constructor, tupleConsConstructor) =>
              expose(head) match
                case AppliedType(power, List(key, exponent)) if sameConstructor(power, powerConstructor) =>
                  val checkedKey = validateKey(key)
                  if seen.exists(_ =:= checkedKey) then
                    invalid("canonical Dim keys must be unique")
                  val checkedExponent = parseExponent(exponent)
                  Entry(checkedKey, checkedExponent) :: loop(tail, checkedKey :: seen)
                case _ => invalid("every canonical Dim tuple entry must be a Power")
            case _ => invalid("canonical Dim entries must form a concrete Tuple")

      loop(raw, Nil)

    private def parseExponent(raw: TypeRepr): BigInt =
      expose(raw) match
        case ConstantType(IntConstant(value)) if value != 0 => BigInt(value)
        case ConstantType(IntConstant(_)) => invalid("zero exponents cannot be stored in a canonical Dim")
        case _                            => invalid("a Power exponent must be a nonzero singleton Int literal")

    private def validateKey(raw: TypeRepr): TypeRepr =
      val key = expose(raw)
      key match
        case _: ConstantType    => key
        case reference: TermRef =>
          val symbol = reference.termSymbol
          if symbol == Symbol.noSymbol || symbol.flags.is(Flags.Param) || symbol.flags.is(Flags.Deferred) then
            invalid(concreteKeyRequirement)
          key
        case thisType: ThisType =>
          val symbol = thisType.tref.typeSymbol
          if symbol == Symbol.noSymbol || symbol.flags.is(Flags.Param) || symbol.flags.is(Flags.Deferred) then
            invalid(concreteKeyRequirement)
          key
        case _ => invalid(concreteKeyRequirement)

    private val concreteKeyRequirement =
      "a Power key must be a concrete stable singleton identity"

    private def constructorOf(raw: TypeRepr): TypeRepr =
      raw.dealias match
        case AppliedType(constructor, _) => constructor
        case _                           => invalid("internal static-dimension constructor lookup failed")

    private def sameConstructor(left: TypeRepr, right: TypeRepr): Boolean =
      left.typeSymbol == right.typeSymbol

    /** Exposes only definitionally transparent aliases and annotations, guarded against cycles and non-progress. */
    private def expose(raw: TypeRepr): TypeRepr =
      def loop(current: TypeRepr, activeAliases: Set[Symbol]): TypeRepr =
        transparentAliasExpansion(current) match
          case Some((symbol, expanded)) =>
            if activeAliases.contains(symbol) then
              invalid("cyclic transparent aliases cannot define a static dimension")
            if expanded == current then
              invalid("a transparent alias could not be exposed without recursion")
            loop(expanded, activeAliases + symbol)
          case None =>
            current match
              case AnnotatedType(underlying, _) => loop(underlying, activeAliases)
              case _                            => current

      loop(raw, Set.empty)

    private def transparentAliasExpansion(value: TypeRepr): Option[(Symbol, TypeRepr)] =
      value match
        case reference: TypeRef if isTransparentAlias(reference.typeSymbol) =>
          Some(reference.typeSymbol -> reference.translucentSuperType)
        case AppliedType(reference: TypeRef, arguments) if isTransparentAlias(reference.typeSymbol) =>
          Some(reference.typeSymbol -> reference.translucentSuperType.appliedTo(arguments))
        case _ => None

    private def isTransparentAlias(symbol: Symbol): Boolean =
      symbol != Symbol.noSymbol &&
        !symbol.flags.is(Flags.Package) &&
        !symbol.flags.is(Flags.Param) &&
        !symbol.flags.is(Flags.Deferred) &&
        !symbol.flags.is(Flags.Opaque) &&
        (symbol.tree match
          case _: TypeDef => true
          case _          => false)

    private def invalid(reason: String): Nothing =
      report.errorAndAbort(s"Invalid canonical static dimension: $reason")

  end Engine

end StaticDimensionMacros
