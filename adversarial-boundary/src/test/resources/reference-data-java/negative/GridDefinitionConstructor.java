package external.referencejava;

import trading.quantity.DimKey;
import trading.quantity.Rational;
import trading.reference.GridDefinition;
import trading.reference.GridIdentity;
import trading.reference.GridKey;

public final class GridDefinitionConstructor extends SharedReferenceDataJavaSetup {
  GridDefinition invalid =
      new GridDefinition(
          new GridIdentity(DimKey.one(), new GridKey(gridId("zero"), gridVersion(1))),
          Rational.zero());
}
