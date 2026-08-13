package trading.quantity.algebra

import algebra.ring.AdditiveCommutativeGroup
import algebra.ring.CommutativeRing
import algebra.ring.Ring

import trading.quantity.refinement.*

/** An additive commutative group with a left action by the scalar ring. */
trait LeftModule[V, S] extends AdditiveCommutativeGroup[V]:
  def scalar: Ring[S]
  def timesl(s: S, v: V): V

/** Summons the coherent [[LeftModule]] instance available for `V` and `S`. */
object LeftModule:
  def apply[V, S](using i: LeftModule[V, S]): LeftModule[V, S] =
    i

/** Exact scalar operations deliberately excluding Float and Double construction. */
trait ExactScalarField[F] extends CommutativeRing[F]:
  def reciprocal(v: NonZero[F]): F

  def reciprocalChecked(v: F)(using Sign[F]): Either[ExpectedNonZero.type, F] =
    NonZero(v).map(reciprocal)

/** Summons the coherent [[ExactScalarField]] instance available for `F`. */
object ExactScalarField:
  def apply[F](using i: ExactScalarField[F]): ExactScalarField[F] =
    i

/** An additive commutative group with an exact scalar-field action. */
trait VectorSpace[V, F] extends LeftModule[V, F]:
  override def scalar: ExactScalarField[F]

  def divr(v: V, s: NonZero[F]): V =
    timesl(scalar.reciprocal(s), v)

  def divrChecked(v: V, s: F)(using Sign[F]): Either[ExpectedNonZero.type, V] =
    NonZero(s).map(divr(v, _))

/** Summons the coherent [[VectorSpace]] instance available for `V` and `F`. */
object VectorSpace:
  def apply[V, F](using i: VectorSpace[V, F]): VectorSpace[V, F] =
    i
