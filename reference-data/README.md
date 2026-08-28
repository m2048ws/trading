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
trading-quantities <- trading-reference-data <- trading-application
        ^                    ^
        |                    |
        +---- trading-economics
```

Application contains only the effect-polymorphic live-catalog port. Concrete runtime interpretation and boundary codecs
remain future-owned.

`CatalogCommand` and guarded non-empty `CatalogBatch` values describe inspectable registration intent. A caller creates
one generative `CatalogRoot`, threads its immutable `CatalogState`, and evaluates `CatalogModel.commit`. The catalog is
append-only: identical definitions are idempotent, conflicts fail atomically, asset/dimension binding is one-to-one,
grid corrections use a new `GridVersion`, and an asset/dimension correction uses a new `AssetId`. Availability,
activation, delisting, eligibility, and effective time are deliberately separate policy concerns.

A non-empty successful addition advances the arbitrary-precision `CatalogRevision` exactly once and returns a
non-empty deterministic `CatalogDelta`. No-op and failed commits do not advance it. Validation accumulates independent
violations in command/rule order, sequences dependent grid validation after coherent dimensions exist, and issues no
handles on failure.

`CatalogSnapshot` is the only read boundary. It provides direct immutable asset, dimension, and full-grid lookup plus
revision and counts, with typed failures and no scan, lock, live pointer, mutable cache, or per-record coordination.
Capture one snapshot for an ingress/replay/assembly batch, then resolve every record purely against that coherent view.

`DimensionHandle.reconcile`, `Asset.reconcile`, and `GridHandle.reconcile` are pure checks over retained immutable
values. Mathematical `SameGrid`, `SameQuantum`, and `Embedding` remain in quantities; numerical compatibility never
implies stable identity or shared lineage. Successor states structurally retain old handles and grids, while checked
stable-handle evidence—not JVM reference equality—defines public canonicality.

Catalog roots, states, snapshots, identifiers, definitions, errors, and authority-bearing handles fail Java
serialization closed. They are in-memory authority, not persistence formats. Proposal 9 introduces stable records,
schema versions, parsing, replay, and checked reconstruction against a selected snapshot; Proposal 8 owns the first
live interpreter, runtime lifecycle, resources, streams, and durability semantics.
