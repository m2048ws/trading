## Why

Trusted asset and grid handles need a coherent source of truth, but the current synchronized registry serializes reads
and writes and combines pure definition semantics with live coordination. Modeling the catalog as immutable state plus
explicit transitions and snapshots makes its laws testable, keeps hot reads lock-free, and gives future live/backtest
interpreters one precise application capability.

## What Changes

- Introduce an immutable `CatalogState`, explicit registration command/batch ADTs, and pure state-transition semantics in
  `trading-reference-data`.
- Make the identity catalog append-only and immutable by key: identical definitions are idempotent, conflicting
  definitions fail without changing state, grid corrections use a new `GridVersion`, and an asset-to-dimension
  reassignment requires a new `AssetId`.
- Keep activation, deactivation, delisting, effective dating, and other availability policy outside the identity catalog.
- Validate a non-empty registration batch as one transaction, accumulate independently observable violations in stable
  order, sequence dependent checks only after prerequisites exist, and publish either the whole batch or nothing.
- Add monotonic arbitrary-precision catalog revisions. A successful batch that adds at least one key publishes exactly
  one new revision; an idempotent no-op or failed batch publishes none.
- Issue one canonical `DimensionHandle`, `Asset`, and `GridHandle` per immutable key within a catalog lineage and
  structurally share those same handles across later states and snapshots.
- Add immutable `CatalogSnapshot` values with pure direct lookup, stable revision, coherent membership, and no locking,
  mutation, live service access, or per-record atomic read.
- Define a minimal tagless-final `LiveCatalog[F[_]]` application capability for capturing a snapshot and atomically
  committing a registration batch. It SHALL NOT expose effectful per-ID lookup, implementation state, or a concrete
  concurrency mechanism.
- Add the initial `trading-application` artifact for that genuine effect-polymorphic port; concrete Cats Effect, STM,
  actor, database, or other interpreters remain in the later runtime proposal.
- Add one non-published `benchmarks` SBT project using `sbt-jmh`, initially measuring immutable catalog snapshot lookup;
  later risk, runtime, and codec proposals extend this same project rather than creating ad hoc benchmark programs.
- **BREAKING** Remove the temporary synchronized `QuantityRegistry` construction bridge established by the preceding
  boundary proposal. Pure tests and startup construction use explicit catalog transitions; live code uses the
  application capability once an interpreter is supplied.
- Keep old snapshots and handles valid indefinitely. New definitions are visible only from the published revision that
  introduced them; existing instrument meanings never change retroactively.

## Capabilities

### New Capabilities

- `reference-data-catalog`: Pure immutable catalog state, append-only definition semantics, atomic batch transitions,
  canonical handle issuance, revisions, lookup errors, and coherent lock-free snapshots.
- `live-catalog-capability`: The application-level effect-polymorphic contract for snapshot capture and atomic catalog
  commits, independent of any concrete state or concurrency interpreter.

### Modified Capabilities

None.

## Impact

- Depends on the active `separate-quantity-and-reference-data-boundaries` proposal and its `Asset`,
  `DimensionHandle`, `GridHandle`, definitions, full grid identity, and private lineage model.
- Affected reference-data implementation: the temporary synchronized registry is replaced by immutable state,
  commands, transitions, snapshots, canonical maps, revision/delta values, and catalog-owned errors.
- Affected build structure: add an initial `trading-application` artifact depending on reference data; no effect runtime
  dependency is added to quantities, reference data, economics, or the application port.
- Affected verification structure: add the non-published, non-root-test-aggregated `benchmarks` project and JMH plugin,
  initially depending only on reference data; it creates no production dependency edge or published artifact.
- Affected tests and fixtures: reference-data setup threads pure state or commits batches, snapshot consumers use pure
  lookup, and concurrency/interpreter behavior is tested later against the same port contract.
- Performance contract: decoding and ingress can capture one snapshot per batch and perform ordinary immutable-map
  lookups; metrics can observe snapshot counts without taking a shared registry monitor, and directional JMH evidence
  replaces environment-specific console timing or normative throughput thresholds.
- Follow-on proposals consume this boundary: instrument assembly resolves against a snapshot, boundary codecs decode
  against one snapshot, and the application/runtime proposal supplies the first concrete live interpreter and lifecycle
  wiring.
