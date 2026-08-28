package external.referencejava;

import trading.reference.GridId;

final class GridIdConstructor extends SharedReferenceDataJavaSetup {
  GridId bypass() {
    return new GridId("");
  }
}
