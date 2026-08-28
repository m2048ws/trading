package external.referencejava;

import trading.reference.AssetId;

final class AssetIdConstructor extends SharedReferenceDataJavaSetup {
  AssetId bypass() {
    return new AssetId("");
  }
}
