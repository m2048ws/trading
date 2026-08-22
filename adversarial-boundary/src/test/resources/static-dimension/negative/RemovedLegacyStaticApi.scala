package external.fixtures.negative

import trading.quantity.*

object RemovedLegacyStaticApi:
  // OFFENDING-BEGIN
  type LegacyNatural = Natural
  type LegacyPowers = Powers
  type LegacyNormalized = NormalizedPowers[One]
  type LegacyProduct = DimensionProduct[One, One]
  type LegacyInverse = DimensionInverse[One]
  type LegacyQuotient = DimensionQuotient[One, One]
  type LegacyAlignment = DimensionAlignment[One, One]
  // OFFENDING-END

end RemovedLegacyStaticApi
