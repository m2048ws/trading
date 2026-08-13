package trading.quantity

/** Closed hierarchy of failures from exact numeric construction and arithmetic. */
sealed abstract class NumericError extends JavaSerializationUnsupported with Product with Serializable

/** Exact division was requested with a zero divisor. */
case object DivisionByZero extends NumericError

/** An identifier failed its required validation. */
case object InvalidIdentifier extends NumericError

/** Exact rational construction was requested with a zero denominator. */
case object ZeroRationalDenominator extends NumericError

/** Closed hierarchy of failures converting finite decimal inputs into exact values. */
sealed abstract class ExactDecimalError extends JavaSerializationUnsupported with Product

/** A decimal scale rejected because eagerly constructing its power of ten would exceed the configured bound. */
final case class UnsupportedFiniteDecimalScale(scale: Int, maximumMagnitude: Int) extends ExactDecimalError
