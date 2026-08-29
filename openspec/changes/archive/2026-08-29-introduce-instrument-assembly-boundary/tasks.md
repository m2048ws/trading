## 1. Portfolio and Prerequisite Gate

- [x] 1.1 Confirm Proposal 0's complete-portfolio gate and apply the accepted quantity/reference-data and functional-
  catalog prerequisites before editing instrument construction.
- [x] 1.2 Refresh Git, OpenSpec, source, and build state and record a clean baseline plus the current
  `Definition`/`ValidatedDefinition`/`Instrument.create` public surface and validation order.
- [x] 1.3 Reconcile `InstrumentDefinition`, `InstrumentSpec`, error, package, and snapshot-consumption names with the
  pure-economics and boundary-codec proposals before implementation.

## 2. Raw Definition and Assembly Error Model

- [x] 2.1 Add guarded `InstrumentId`, `UnderlyingId`, closed asset/listing role enums, `AssetRoleIds`,
  `ListingDefinition`, `PayoffDefinition`, and cohesive stable-ID-only `InstrumentDefinition` values with identity and
  roles stored once.
- [x] 2.2 Add closed contextual `InstrumentAssemblyViolation` cases for asset/grid lookup, equal base/quote, empty payoff,
  and position/price grid dimension failures, retaining typed IDs, keys, revisions, and catalog causes.
- [x] 2.3 Add domain-owned non-empty `InstrumentAssemblyErrors`, deterministic first-error projection, guarded
  construction, and project-owned fail-closed serialization behavior.
- [x] 2.4 Add focused value/error tests proving invalid IDs, contradictory component-role owner states, empty error
  aggregates, raw strings, handles, snapshots, and effects cannot inhabit the raw definition model.

## 3. Pure Snapshot-Based Assembly

- [x] 3.1 Implement independent base/quote/position/settle asset and position/price grid lookup against exactly one
  supplied `CatalogSnapshot`, contextualizing every lookup failure by instrument and semantic role.
- [x] 3.2 Implement independent raw structural validation for base/quote distinction and nonempty payoff, with stable
  rule ordinals shared by accumulating and first-error entry points.
- [x] 3.3 Compose resolution and structural checks applicatively, then sequence dependent position-grid, price-grid, and
  payoff-endpoint proof recovery only when each prerequisite exists and is coherent.
- [x] 3.4 Accumulate eligible position and price branch failures deterministically, suppress misleading dependent
  diagnostics, and ensure no handle/proof escapes on any invalid result.
- [x] 3.5 Add unit/property tests for multiple missing IDs, mixed structural/lookup failures, prerequisite suppression,
  fixed-snapshot behavior under later publications, stable ordering, and fail-fast/accumulating equivalence.

## 4. Proof-Carrying InstrumentSpec and Total Instrument

- [x] 4.1 Implement sealed constructor-private `InstrumentSpec` retaining exact resolved roles, typed position and price
  grid handles, and endpoint-typed payoff rates, with all existential narrowing localized after checked evidence.
- [x] 4.2 Expose domain-readable immutable spec observations without exposing a snapshot, live catalog, lineage token,
  general coercion, reusable proof constructor, or public implementation hook.
- [x] 4.3 Replace fallible raw instrument construction with total `Instrument.fromSpec` (or equivalent) that retains the
  trusted components without lookup, revalidation, recasting, or quantization.
- [x] 4.4 Add downstream compiler/adversarial fixtures proving callers cannot construct/implement `InstrumentSpec`,
  reverse payoff endpoints, pass raw definitions to `Instrument`, extract retagging authority, or use a newer identity
  through an older snapshot.

## 5. Source-Breaking Migration and Error Ownership

- [x] 5.1 Remove current object-linked `Definition`, public `ValidatedDefinition`, component-role contradiction cases,
  `Instrument.validate`, `Instrument.fromValidated`, and `Instrument.create(Definition)` without compatibility aliases.
- [x] 5.2 Remove definition-specific catalog/provenance cases and mappings from `EconomicsError`; retain catalog causes
  only inside contextual assembly violations and leave unrelated market/fee errors for the next proposal.
- [x] 5.3 Migrate economics fixtures and examples to build one pure catalog snapshot, assemble stable-ID definitions, and
  construct instruments totally while preserving every established exact lot/price/market/fee/PnL expected result.
- [x] 5.4 Search production code to verify final instruments/specs retain no `CatalogSnapshot` or `LiveCatalog[F]` and no
  ordinary economics operation performs identity lookup or repeats instrument-definition checks.

## 6. Documentation, Verification, and Independent Review

- [x] 6.1 Update package docs, Scaladoc, examples, and architecture references for the
  `InstrumentDefinition -> InstrumentSpec -> Instrument` trust transition and explicit adapter/codec boundary.
- [x] 6.2 Run Scala and SBT formatting and verify `scalafmtCheckAll`, `scalafmtSbtCheck`, and `git diff --check` pass.
- [x] 6.3 Run focused assembly/economics unit and property suites plus immutable-JAR compiler/adversarial tests covering
  every positive/negative construction and authority case.
- [x] 6.4 Run `sbt -batch clean test`, strict validation for this change, and `openspec validate --all --strict`; audit
  the diff for accidental market, fee, order, scenario, risk, live-catalog, or codec changes.
- [x] 6.5 Stage exactly the intended implementation, tests, documentation, and active-change artifacts in a validated
  commit-ready worktree without committing unless separately authorized.
- [x] 6.6 Obtain fresh independent review of the fully staged implementation and validation evidence; remediation must
  return to another fresh independent review before finalization.
