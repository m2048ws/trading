## 1. Portfolio and Dependency Gate

- [ ] 1.1 Confirm Proposal 0's complete-portfolio gate has passed and the applied quantity/reference-data boundary
  supplies the accepted identity, handle, lineage, and anonymous-grid contracts before changing production code.
- [ ] 1.2 Refresh Git, OpenSpec, source, and build state and record a clean baseline compile/test plus the exact temporary
  registry surface this change will remove.
- [ ] 1.3 Reconcile this change's snapshot and live-port names with the instrument-assembly, boundary-codec, and
  application/runtime proposals; resolve any ownership or implementation-order conflict before implementation.

## 2. Catalog Algebra and Public Domain Values

- [ ] 2.1 Add closed asset/dimension/grid `CatalogCommand` alternatives and a guarded non-empty `CatalogBatch` that
  retains ordered inspectable definitions without trusted handles or effects.
- [ ] 2.2 Add nonnegative arbitrary-precision `CatalogRevision`, non-empty append-only `CatalogDelta`, and explicit
  unchanged/published `CatalogCommit` outcomes with guarded construction and fail-closed serialization behavior.
- [ ] 2.3 Add closed indexed `CatalogViolation`, domain-owned non-empty `CatalogViolations`, and `CatalogLookupError`
  hierarchies covering duplicate proposals, immutable conflicts, one-to-one asset/dimension conflicts, missing
  prerequisites, foreign lineage, and unknown identities.
- [ ] 2.4 Add focused product/equality/null/serialization tests for every new public value and compiler checks preventing
  construction of empty batches, empty deltas, negative revisions, or caller-owned authority.

## 3. Immutable State and Staged Transition Semantics

- [ ] 3.1 Implement lexically controlled generative catalog-root/lineage creation outside `CatalogModel.commit` and
  immutable `CatalogState` indexes for dimensions, assets, reverse asset-dimension binding, and full grid identities;
  verify repeated transition evaluation never creates a new lineage.
- [ ] 3.2 Implement batch indexing and deterministic duplicate/current-state conflict detection while retaining command
  indices and stable rule ordinals independent of immutable-map iteration order.
- [ ] 3.3 Implement complete proposed-dimension formation and one-to-one asset/dimension validation so asset-before-
  dimension and dimension-before-asset batches have equal meaning and standalone dimensions can be reused safely.
- [ ] 3.4 Implement dependent grid validation only after coherent dimensions exist, including idempotent exact
  definitions, immutable full-key conflicts, and coexistence of corrected `GridVersion`s.
- [ ] 3.5 Compose independent checks applicatively and dependent stages sequentially, converting internal validation
  containers to deterministic public `CatalogViolations` without issuing handles on failure.
- [ ] 3.6 Implement atomic successor construction, one-revision publication, complete deterministic deltas, and explicit
  unchanged outcomes while leaving the input state untouched on every path.

## 4. Canonical Handles and Immutable Snapshots

- [ ] 4.1 Issue canonical `DimensionHandle`, `Asset`, and `GridHandle` values only after successful validation, localizing
  existential boxes and justified casts inside catalog-owned helpers.
- [ ] 4.2 Reuse existing canonical handles and anonymous mathematical grids through immutable structural sharing across
  successor states; return existing handles for idempotent definitions and distinct handles for new versions.
- [ ] 4.3 Implement immutable `CatalogSnapshot` with revision, counts, and direct pure asset/dimension/full-grid lookup,
  including dependent return packaging and typed unknown/foreign-lineage failures without map scans or mutable caches.
- [ ] 4.4 Add tests proving historical snapshots remain unchanged, old handles remain valid after additions, new keys are
  unknown to old snapshots, equal rebuilt catalogs have different lineages, and public semantics never require JVM
  reference equality.
- [ ] 4.5 Add property tests for append-only state, transition/input immutability, idempotence, revision monotonicity,
  delta/state conservation, batch atomicity, deterministic validation order, and valid command-order permutations.

## 5. Application-Level Live Catalog Port

- [ ] 5.1 Add the initial `application` SBT project and `trading-application` artifact depending on reference data, wire it
  into the root and immutable-JAR adversarial classpaths, and add no concrete effect-runtime dependency.
- [ ] 5.2 Define the minimal `LiveCatalog[F[_]]` port with snapshot capture and atomic batch commit only, returning the
  reference-data domain outcomes without effectful point lookup, reset, subscription, telemetry, or interpreter state.
- [ ] 5.3 Define a reusable interpreter-conformance test contract for linearized snapshot/commit behavior, concurrent
  independent and conflicting batches, idempotent retry, no lost updates, and equivalence with the pure catalog model.
- [ ] 5.4 Add compiler/module tests proving pure reference data and every domain artifact are independent of
  `LiveCatalog[F]`, while application workflows can remain polymorphic over it without importing a concrete runtime.

## 6. Remove the Transitional Registry and Migrate Construction

- [ ] 6.1 Replace reference-data and economics fixture setup with explicit catalog root/state/batch transitions and pure
  snapshot resolution, retaining existing stable definitions and expected handles.
- [ ] 6.2 Remove the temporary synchronized `QuantityRegistry`, mutable maps, shared monitor, grid-scan decode helper,
  count-under-lock methods, and registry-object provenance checks without leaving compatibility aliases.
- [ ] 6.3 Rehome or remove obsolete registry errors and tests so reference data exposes only catalog definition,
  transition, handle-evidence, and lookup failures owned by the new model.
- [ ] 6.4 Search production signatures to verify quantities, economics, orders, scenarios, fee values, valuation, and risk
  receive neither catalog state nor `LiveCatalog[F]`, and snapshot consumers never retain a pointer to live state.

## 7. Documentation, Performance Contract, and Follow-On Boundaries

- [ ] 7.1 Update reference-data and application documentation with the command/transition/port distinction,
  append-only correction policy, activation/delisting exclusion, snapshot capture pattern, and interpreter-neutral
  guarantees.
- [ ] 7.2 Add the non-published `benchmarks` SBT project using the `sbt-jmh` plugin, initially depending only on reference
  data, exclude benchmark execution from root test aggregation, and wire explicit JMH compilation into verification
  without creating a production or publication dependency.
- [ ] 7.3 Add a focused JMH benchmark demonstrating immutable snapshot lookup under representative reader counts without
  a serialized registry-monitor path; record JDK, forks, warmup, measurement, threads, and directional results without
  making environment-specific throughput a normative test.
- [ ] 7.4 Document that snapshots/handles are in-memory authority rather than persistence formats and link the accepted
  boundary-codec proposal for reconstruction and the runtime proposal for concrete state, resources, streams, and
  durability.

## 8. Verification and Independent Review

- [ ] 8.1 Run Scala and SBT formatting for every changed source and verify `scalafmtCheckAll`, `scalafmtSbtCheck`, and
  `git diff --check` pass.
- [ ] 8.2 Run focused reference-data transition/snapshot suites, application port/compiler suites, economics fixtures,
  and immutable-JAR adversarial tests, including every new positive and negative authority path.
- [ ] 8.3 Compile the JMH project explicitly, run the focused catalog benchmark with recorded parameters, then run
  `sbt -batch clean test` and confirm the complete aggregate passes with no synchronized registry remaining.
- [ ] 8.4 Run strict validation for this change and `openspec validate --all --strict`, then audit the diff against the
  pure-reference/application-port/runtime-interpreter ownership boundary.
- [ ] 8.5 Stage exactly the intended implementation, tests, build, documentation, and active-change artifacts in a
  validated commit-ready worktree without committing unless separately authorized.
- [ ] 8.6 Obtain fresh independent review of the fully staged implementation and validation evidence; remediation must
  return to another fresh independent review before finalization.
