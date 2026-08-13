package trading.quantity.laws

import algebra.laws.RingLaws
import cats.kernel.Eq
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

import trading.quantity.algebra.ExactScalarField
import trading.quantity.algebra.LeftModule
import trading.quantity.algebra.VectorSpace
import trading.quantity.refinement.*

private object LawEquality:
  def equal[A](l: A, r: A)(using e: Eq[A]): Prop =
    Prop(e.eqv(l, r))

/**
 * Reusable laws for the refinement-aware operations added by `ExactScalarField`.
 *
 * The expected model is reconstruction through the inherited commutative-ring operations; checked reciprocal is
 * compared with an independently checked `NonZero` result.
 */
final class ExactScalarFieldLaws[F](
  field: ExactScalarField[F]
)(using
  Eq[F],
  Arbitrary[F],
  Arbitrary[NonZero[F]],
  Sign[F])
  extends Laws:
  import LawEquality.equal

  private val ringRules =
    RingLaws[F](using summon[Eq[F]], summon[Arbitrary[F]], field).commutativeRing(using field)

  val exactScalarField: RuleSet = new RuleSet:
    val name    = "exact scalar field"
    val bases   = Seq("commutativeRing" -> ringRules)
    val parents = Seq.empty
    val props   = Seq(
      "left reciprocal" -> forAll: (value: NonZero[F]) =>
        equal(field.times(field.reciprocal(value), value.unrefined), field.one),
      "right reciprocal" -> forAll: (value: NonZero[F]) =>
        equal(field.times(value.unrefined, field.reciprocal(value)), field.one),
      "reciprocal involution" -> forAll: (value: NonZero[F]) =>
        NonZero(field.reciprocal(value)) match
          case Right(reciprocal) => equal(field.reciprocal(reciprocal), value.unrefined)
          case Left(_)           => Prop.falsified,
      "division reconstruction" -> forAll: (value: F, divisor: NonZero[F]) =>
        val quotient = field.times(value, field.reciprocal(divisor))
        equal(field.times(quotient, divisor.unrefined), value)
      ,
      "checked reciprocal agrees with NonZero" -> forAll: (value: F) =>
        (NonZero(value), field.reciprocalChecked(value)) match
          case (Left(expected), Left(actual))      => Prop(expected == actual)
          case (Right(nonZero), Right(reciprocal)) => equal(reciprocal, field.reciprocal(nonZero))
          case _                                   => Prop.falsified
    )

end ExactScalarFieldLaws

/**
 * Reusable module laws, including the carrier's standard additive-group laws as a base.
 *
 * The expected sides are built only from the supplied scalar `Ring` and additive group, independently of any concrete
 * quantity representation.
 */
final class LeftModuleLaws[V, S](module: LeftModule[V, S])(using Eq[V], Arbitrary[V], Arbitrary[S]) extends Laws:
  import LawEquality.equal

  private val additiveGroupRules =
    RingLaws[V](using summon[Eq[V]], summon[Arbitrary[V]], module).additiveCommutativeGroup(using module)

  val leftModule: RuleSet = new RuleSet:
    val name    = "left module"
    val bases   = Seq("additiveCommutativeGroup" -> additiveGroupRules)
    val parents = Seq.empty
    val props   = Seq(
      "scalar identity" -> forAll: (value: V) =>
        equal(module.timesl(module.scalar.one, value), value),
      "scalar associativity" -> forAll: (first: S, second: S, value: V) =>
        equal(
          module.timesl(module.scalar.times(first, second), value),
          module.timesl(first, module.timesl(second, value))
        ),
      "scalar distributivity" -> forAll: (first: S, second: S, value: V) =>
        equal(
          module.timesl(module.scalar.plus(first, second), value),
          module.plus(module.timesl(first, value), module.timesl(second, value))
        ),
      "vector distributivity" -> forAll: (scalar: S, left: V, right: V) =>
        equal(
          module.timesl(scalar, module.plus(left, right)),
          module.plus(module.timesl(scalar, left), module.timesl(scalar, right))
        ),
      "zero scalar" -> forAll: (value: V) =>
        equal(module.timesl(module.scalar.zero, value), module.zero),
      "zero vector" -> forAll: (scalar: S) =>
        equal(module.timesl(scalar, module.zero), module.zero)
    )

end LeftModuleLaws

/**
 * Reusable vector-space laws composed over the complete `LeftModuleLaws` RuleSet.
 *
 * Division is modeled as the inverse of the module action by a checked nonzero scalar.
 */
final class VectorSpaceLaws[V, F](
  space: VectorSpace[V, F]
)(using
  Eq[V],
  Arbitrary[V],
  Arbitrary[F],
  Arbitrary[NonZero[F]],
  Sign[F])
  extends Laws:
  import LawEquality.equal

  private val moduleRules = new LeftModuleLaws[V, F](space).leftModule

  val vectorSpace: RuleSet = new RuleSet:
    val name    = "vector space"
    val bases   = Seq("leftModule" -> moduleRules)
    val parents = Seq.empty
    val props   = Seq(
      "divide by one" -> forAll: (value: V) =>
        NonZero(space.scalar.one) match
          case Right(one) => equal(space.divr(value, one), value)
          case Left(_)    => Prop.falsified,
      "division reconstruction" -> forAll: (value: V, divisor: NonZero[F]) =>
        equal(space.timesl(divisor.unrefined, space.divr(value, divisor)), value)
    )

end VectorSpaceLaws
