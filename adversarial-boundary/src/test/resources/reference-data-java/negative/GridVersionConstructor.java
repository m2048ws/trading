package external.referencejava;

import trading.reference.GridVersion;

final class GridVersionConstructor extends SharedReferenceDataJavaSetup {
  GridVersion bypass() {
    return new GridVersion(0);
  }
}
