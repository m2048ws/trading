package trading.economics.instrument

import cats.data.ValidatedNec
import cats.syntax.all.*

private[instrument] final case class IndexedViolation[+E](ordinal: Int, value: E)

/** Small validation primitives shared by the two public diagnostic boundaries. */
private[instrument] object Validation:
  type Accumulating[E, A] = ValidatedNec[IndexedViolation[E], A]

  def ensure[E](ordinal: Int, condition: Boolean)(violation: => E): Accumulating[E, Unit] =
    if condition then ().validNec else IndexedViolation(ordinal, violation).invalidNec

  def stage[E, A, B](first: Accumulating[E, A])(next: A => Accumulating[E, B]): Accumulating[E, B] =
    first.toEither.flatMap(value => next(value).toEither).toValidated

  def indexed[E, A](values: Vector[A])(rule: (A, Int) => Accumulating[E, Unit]): Accumulating[E, Unit] =
    values.zipWithIndex.traverse_((value, index) => rule(value, index))

  def ordered[E, A](result: Accumulating[E, A]): Either[Vector[E], A] =
    result
      .leftMap(_.toChain.toList.sortBy(_.ordinal).map(_.value).toVector)
      .toEither

end Validation
