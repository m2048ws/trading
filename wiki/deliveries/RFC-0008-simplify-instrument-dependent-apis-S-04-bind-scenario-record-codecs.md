---
type: delivery
updated: 2026-09-04
rfc: RFC-0008-simplify-instrument-dependent-apis
slice: S-04-bind-scenario-record-codecs
change: bind-scenario-record-codecs
status: archived
archived: 2026-09-04
evidence_manifest: sha256:db11f417b9b2b52e876c9e618045c0ea2bac19033a63d234ad7cb44c75c8f1f9
source_digest: sha256:ed1e07a7dbeb06ac490bba7fe7fe7cd9de7e393bccb37d2db710b6a62119793a
---

# RFC-0008-simplify-instrument-dependent-apis/S-04-bind-scenario-record-codecs

## Outcome
Make common, already well-typed Scala calls concise when several operations share one assembled instrument, execution
lifecycle, or immutable catalog snapshot. Each owning module shall provide a small immutable scope that captures its
stable context once, retains the exact path-dependent relationships established by that context, and exposes
domain-named operations without requiring callers to repeat deeply nested role-dimension projections.

The pattern shall cover the current high-value surfaces: standard order construction, lifecycle-bound source facts,
scenario-record encoding and reconstruction, instrument-specific risk construction and exhaustive sizing, and market
state construction. `Instrument` shall expose direct aliases for its position, base, quote, and settlement dimensions
so owner-local signatures can name those established types without reopening the roles representation.

Conciseness must not weaken semantics. The new scopes shall share the existing checked implementation, preserve precise
result and error types, retain instrument identity and grid validation, and keep every successful result as
specifically typed as the current direct operation. Ordinary callers shall be able to reuse a bound scope without
explicit dimension type arguments, unchecked casts, structural refinements, or duplicated local aliases.

## Boundary Delivered
This RFC introduces an owner-local binding pattern, not a new architectural layer. A bound scope is an immutable value
created by the companion or facade that already owns the operations. It captures one stable trusted value, or the
smallest coherent product of stable values the operations genuinely share. Its methods retain the captured value's
singleton relationship so Scala can infer dependent parameter and result types. The scope neither changes the captured
value nor acquires resources, performs I/O, coordinates shared state, or memoizes results.

`trading-instrument-economics` shall add direct `PositionD`, `BaseD`, `QuoteD`, and `SettleD` type members to
`Instrument`. Each is exactly an alias of the corresponding role dimension; it creates no new dimension, evidence, or
runtime representation. Existing `Lots`, `PositionLots`, `Price`, `MarketState`, `PricePnl`, and `Pnl` members retain
their meanings and may use the direct aliases internally.

`trading-order-model` shall own an instrument-bound standard-order construction scope. It captures one exact
instrument and provides the existing market, limit, stop-market, and stop-limit conveniences with the same defaults,
validation, activation/execution refinements, and accumulated `OrderViolations`. Generic checked construction that
accepts independently supplied intent, activation, and execution values remains available; binding the conveniences
shall not narrow or bypass that validation surface.

`trading-execution-lifecycle` shall own a lifecycle-bound source-fact construction scope. It captures one exact
`ExecutionLifecycle` and provides construction for accepted, rejected, fill, correction, bust, cancellation-effective,
reconciliation-checkpoint, source-order-completed, and source-order-absent facts. It reuses the existing fact owners and
`SourceFactViolations`, including logical-order, target, instrument, grid, ordering, modifier-reference, checkpoint,
and completeness checks. Commands, dispatch, state initialization, replay, observation, and effective-fill derivation
remain outside this scope because they do not share the same construction context or ownership.

`trading-boundary-codecs` shall distinguish two immutable scenario-record contexts. An encoder captures only the exact
instrument needed to turn typed order and round-trip scenarios into records and canonical wire forms. A decoder
captures that exact instrument plus one immutable `CatalogSnapshot` used coherently for reconstruction. Record parsing,
record-only encoding, and schemas remain context-free. Decode limits and record indices remain explicit operation
inputs. Both scenario families preserve their current wire versions, canonicalization, locations, accumulated errors,
batch ordering, and exact reconstructed dependent types.

`trading-risk` shall own an instrument-bound risk scope for downside measurement, monotone lot-risk construction, and
arbitrary exhaustive lot sizing. It may expose local aliases for the captured instrument's position and settlement
dimensions, loss, budget, model, assessment, and decision types. `single` and complete-table construction shall retain
their deliberately broad existential inputs and runtime identity/dimension validation. Model-to-model combinators and
`MaxAffordableLots.select` remain model-bound operations because their semantics do not require an instrument scope.

`trading-instrument-economics` shall consolidate each pair of market-state overloads into one operation whose
additional conversions default to the empty vector, and shall expose the same eight construction modes through an
instrument-bound market-state scope: quote-settled, base-settled, quote anchor, base anchor, both anchors, quote rate,
base rate, and both rates. Settlement-conversion construction stays separately owned rather than expanding the scope
into a general economics facade.

Current companion entry points may remain as thin delegates where source compatibility is intentionally retained, but
there shall be one implementation of each validation or calculation. Moving a check into scope creation is allowed only
when it depends solely on captured context and preserves observable failure and null behavior; otherwise validation
remains at its current operation. Every scope remains safe to reuse concurrently because it is pure and immutable.

Each Slice shall include completed-artifact Scala compiler fixtures demonstrating the concise supported call and nearby
invalid calls. Positive fixtures shall not spell explicit dimension type arguments or
`instrument.roles.<role>.D` at the call site. Negative fixtures shall prove that binding does not permit values from
incompatible dimensions or contexts. Focused tests shall compare scoped and characterized current operations for exact
success, failure ordering, and result refinement.

## Acceptance Evidence
| AC | Requirement | Evidence | Result |
|---|---|---|---|
| AC-012 | automated | file:boundary-codecs%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fcodec%2FScenarioRecords.scala#sha256:36effb8081b7dc21629135f6fa594f4232b544a4b33e4d428274e4067fcd503f; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FScenarioRecordScopeSuite.scala#sha256:71e5723b8f5565bbdfa9c78589ea1b7e5450d6547aff7afbce6d9c24984e2264; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FBoundaryCodecCompilerBoundarySuite.scala#sha256:6a83782675f536952069630f130d6e61018a69d847109caf926939b723c4cd70 | PASS |
| AC-013 | automated | file:boundary-codecs%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fcodec%2FScenarioRecords.scala#sha256:36effb8081b7dc21629135f6fa594f4232b544a4b33e4d428274e4067fcd503f; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FScenarioRecordScopeSuite.scala#sha256:71e5723b8f5565bbdfa9c78589ea1b7e5450d6547aff7afbce6d9c24984e2264; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fboundary-codec-compiler%2Fpositive%2FScenarioRecordScopeClient.scala#sha256:58bec01c0bcbe6dc088eac482226689b6edb900eb00767dbb8203762a56f437e | PASS |
| AC-014 | automated | file:boundary-codecs%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fcodec%2FScenarioRecords.scala#sha256:36effb8081b7dc21629135f6fa594f4232b544a4b33e4d428274e4067fcd503f; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FScenarioRecordScopeSuite.scala#sha256:71e5723b8f5565bbdfa9c78589ea1b7e5450d6547aff7afbce6d9c24984e2264; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fboundary-codec-compiler%2Fpositive%2FScenarioRecordScopeClient.scala#sha256:58bec01c0bcbe6dc088eac482226689b6edb900eb00767dbb8203762a56f437e | PASS |
| AC-015 | automated | file:boundary-codecs%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fcodec%2FScenarioRecords.scala#sha256:36effb8081b7dc21629135f6fa594f4232b544a4b33e4d428274e4067fcd503f; file:boundary-codecs%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fcodec%2FScenarioRecordScopeSuite.scala#sha256:71e5723b8f5565bbdfa9c78589ea1b7e5450d6547aff7afbce6d9c24984e2264 | PASS |
| AC-016 | automated | file:boundary-codecs%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fcodec%2FScenarioRecords.scala#sha256:36effb8081b7dc21629135f6fa594f4232b544a4b33e4d428274e4067fcd503f; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FBoundaryCodecCompilerBoundarySuite.scala#sha256:6a83782675f536952069630f130d6e61018a69d847109caf926939b723c4cd70; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fboundary-codec-compiler%2Fpositive%2FScenarioRecordScopeClient.scala#sha256:58bec01c0bcbe6dc088eac482226689b6edb900eb00767dbb8203762a56f437e; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fboundary-codec-compiler%2Fnegative%2FScenarioRecordScopeMismatch.scala#sha256:afd0b025fe554c4c4b8beec3ae3c2b98d573feb074bf9b32c07e7670b2e4dd07 | PASS |

## Implementation
- Task Group 1: `fa010240f300abf02ccd0c13b24253674c8d6f35`
- Final HEAD: `fa010240f300abf02ccd0c13b24253674c8d6f35`

## Review and QA
- Human Review: approve by requesting-user
- Human QA: skipped by requesting-user — The human requester explicitly directed Human QA to be skipped. The verified delivery changes only the pure boundary-codec library API: its immutable instrument-bound encoder and immutable-snapshot decoder delegate to existing checked record operations and add no application/runtime interpreter, service, UI, API, CLI, live-catalog, or changed wire-format user path. All Slice acceptance criteria require automated evidence only.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0008-simplify-instrument-dependent-apis`
- `openspec/changes/archive/2026-09-04-bind-scenario-record-codecs`
- `openspec/changes/archive/2026-09-04-bind-scenario-record-codecs/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/59
