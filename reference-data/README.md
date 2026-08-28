# trading-reference-data

This Scala 3 module owns stable asset and grid identity above the mathematical `trading-quantities` foundation.

Its public package `trading.reference` provides `AssetId`, `GridId`, `GridVersion`, dimension-scoped `GridIdentity`,
immutable definitions, and non-forgeable `Asset`, `DimensionHandle`, and `GridHandle` capabilities. A grid handle wraps
exactly one anonymous quantity `GridRef`, delegates coordinate and embedding operations to it, and adds stable identity
plus opaque issuer lineage without changing quantity arithmetic.

Stable IDs are private immutable value roots. `AssetId.from`, `GridId.from`, and `GridVersion.from` return precise typed
failures for empty or nonpositive input; no public constructor, case-class `apply`, or `copy` path bypasses validation.
Null is rejected before a result is returned.

The dependency is one way:

```text
trading-quantities <- trading-reference-data <- trading-economics
```

`DimensionHandle.reconcile`, `Asset.reconcile`, and `GridHandle.reconcile` are pure checks over retained immutable
values. Mathematical `SameGrid`, `SameQuantum`, and `Embedding` remain in quantities; numerical compatibility never
implies stable identity or shared lineage.

`QuantityRegistry` is an explicitly unreleased synchronized construction bridge preserving current registration and
lookup behavior. It is not the target catalog API and is not passed into economics. Proposal 2 replaces it with pure
catalog transitions and immutable snapshots.

Invariant-bearing identifiers, definitions, errors, and authority-bearing handles fail Java serialization closed.
Stable records, schema versions, parsing, replay, and durable reconstruction are intentionally absent until Proposal 9
introduces `trading-boundary-codecs`.
