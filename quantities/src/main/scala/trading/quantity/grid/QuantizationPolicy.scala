package trading.quantity.grid

import trading.quantity.JavaSerializationUnsupported
import trading.quantity.Rational

/**
 * Selects an integer grid coordinate for an exact rational coordinate.
 *
 * The closed policy hierarchy provides directed and nearest rounding modes. Each policy also defines the residual bound
 * that quantization verifies after selecting a coordinate.
 */
sealed abstract class QuantizationPolicy extends JavaSerializationUnsupported:
  def roundCoordinate(v: Rational): BigInt
  def acceptsResidual(v: Rational, c: BigInt): Boolean

/** Predefined directed and nearest-coordinate quantization policies. */
object QuantizationPolicy:

  /** Selects the greatest coordinate not greater than the exact coordinate. */
  case object Floor extends QuantizationPolicy:
    def roundCoordinate(v: Rational): BigInt =
      v.floor

    def acceptsResidual(v: Rational, c: BigInt): Boolean =
      val residual = v - Rational(c)
      residual.signum >= 0 && residual.compare(Rational.one) < 0

  /** Selects the least coordinate not less than the exact coordinate. */
  case object Ceiling extends QuantizationPolicy:
    def roundCoordinate(v: Rational): BigInt =
      v.ceil

    def acceptsResidual(v: Rational, c: BigInt): Boolean =
      val residual = v - Rational(c)
      residual.compare(-Rational.one) > 0 && residual.signum <= 0

  /** Selects the nearest coordinate in the direction of zero. */
  case object TowardZero extends QuantizationPolicy:
    def roundCoordinate(v: Rational): BigInt =
      if v.signum < 0 then
        v.ceil
      else
        v.floor

    def acceptsResidual(v: Rational, c: BigInt): Boolean =
      val residual = v - Rational(c)
      residual.abs.compare(Rational.one) < 0 && (residual.isZero || residual.signum == v.signum)

  /** Selects the nearest coordinate in the direction away from zero. */
  case object AwayFromZero extends QuantizationPolicy:
    def roundCoordinate(v: Rational): BigInt =
      if v.signum < 0 then
        v.floor
      else
        v.ceil

    def acceptsResidual(v: Rational, c: BigInt): Boolean =
      val residual = v - Rational(c)
      residual.abs.compare(Rational.one) < 0 && (residual.isZero || residual.signum == -v.signum)

  /** Selects the nearest coordinate, resolving exact ties toward the even coordinate. */
  case object HalfEven extends NearestPolicy:

    protected def resolveTie(v: Rational, l: BigInt, u: BigInt): BigInt =
      if l % 2 == 0 then
        l
      else
        u

  /** Selects the nearest coordinate, resolving exact ties toward the odd coordinate. */
  case object HalfOdd extends NearestPolicy:

    protected def resolveTie(v: Rational, l: BigInt, u: BigInt): BigInt =
      if l % 2 != 0 then
        l
      else
        u

  /** Nearest, with ties toward positive infinity. */
  case object HalfUp extends NearestPolicy:
    protected def resolveTie(v: Rational, l: BigInt, u: BigInt): BigInt =
      u

  /** Nearest, with ties toward negative infinity. */
  case object HalfDown extends NearestPolicy:
    protected def resolveTie(v: Rational, l: BigInt, u: BigInt): BigInt =
      l

  /** Selects the nearest coordinate, resolving exact ties toward zero. */
  case object HalfTowardZero extends NearestPolicy:

    protected def resolveTie(v: Rational, l: BigInt, u: BigInt): BigInt =
      if v.signum < 0 then
        u
      else
        l

  /** Selects the nearest coordinate, resolving exact ties away from zero. */
  case object HalfAwayFromZero extends NearestPolicy:

    protected def resolveTie(v: Rational, l: BigInt, u: BigInt): BigInt =
      if v.signum < 0 then
        l
      else
        u

  /** Base for nearest-coordinate policies that differ only in how exact ties are resolved. */
  sealed abstract class NearestPolicy extends QuantizationPolicy:
    protected def resolveTie(v: Rational, l: BigInt, u: BigInt): BigInt

    final def roundCoordinate(v: Rational): BigInt =
      val lower = v.floor
      val upper = v.ceil

      if lower == upper then
        lower
      else
        val distanceFromLower = v - Rational(lower)

        distanceFromLower.compare(Rational(1, 2)) match
          case comparison if comparison < 0 => lower
          case comparison if comparison > 0 => upper
          case _                            => resolveTie(v, lower, upper)

    final def acceptsResidual(v: Rational, c: BigInt): Boolean = (v - Rational(c)).abs.compare(Rational(1, 2)) <= 0

  end NearestPolicy

end QuantizationPolicy
