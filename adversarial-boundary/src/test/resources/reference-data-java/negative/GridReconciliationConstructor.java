package external.referencejava;

import trading.quantity.Dim;
import trading.reference.GridHandle;

public final class GridReconciliationConstructor {
  // OFFENDING-BEGIN
  GridHandle.Reconciliation<Dim, Object, Dim, Object> evidence =
      new GridHandle.Reconciliation<>();
  // OFFENDING-END
}
