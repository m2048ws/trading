package external.reference.negative

import external.reference.fixtures.SharedReferenceDataSetup.*

import trading.quantity.Rational
import trading.reference.GridDefinition

object GridDefinitionProductBypass:
  private val identityValue = grid.identity
  private val valid         = gridDefinition(identityValue.dimension, "valid-product-grid")
  private val rawProduct    = (identityValue, Rational.zero)

  // OFFENDING-BEGIN
  val direct = GridDefinition(identityValue, Rational.zero)
  val copied = valid.copy(quantum = Rational.zero)
  val rebuilt = GridDefinition.fromProduct(rawProduct)
  // OFFENDING-END

end GridDefinitionProductBypass
