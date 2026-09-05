---
type: delivery
updated: 2026-09-04
rfc: RFC-0008-simplify-instrument-dependent-apis
slice: S-03-bind-source-fact-construction
change: bind-source-fact-construction
status: archived
archived: 2026-09-04
evidence_manifest: sha256:336f9805040785ac5aea98745d63ded1121da6e23cab778dd0de44d489250a9e
source_digest: sha256:44cee1e7fc26759bc7720e78681cc6abfc18e11cd7e57e810d74ab07f5adf6e8
---

# RFC-0008-simplify-instrument-dependent-apis/S-03-bind-source-fact-construction

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
| AC-008 | automated | file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FSourceFacts.scala#sha256:f59e9d5cd97fe7d6a8f28ab57b9b450b8642fdbec8f80266cebddadd7c660fd4; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FSourceFactScopeSuite.scala#sha256:ee2ac9a4a7dfd72d5235e1342a26644d2aa876216141ac8f2bcf3efb2e55419c; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fexecution-lifecycle-compiler%2Fpositive%2FLifecycleSourceFactScopeClient.scala#sha256:19ee7cfedada2b5903c469beb9f177d01d6a2ab3a992c54f43b31428c160d1d3 | PASS |
| AC-009 | automated | file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FSourceFacts.scala#sha256:f59e9d5cd97fe7d6a8f28ab57b9b450b8642fdbec8f80266cebddadd7c660fd4; file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FSourceFactScopeSuite.scala#sha256:ee2ac9a4a7dfd72d5235e1342a26644d2aa876216141ac8f2bcf3efb2e55419c | PASS |
| AC-010 | automated | file:execution-lifecycle%2Fsrc%2Ftest%2Fscala%2Ftrading%2Fexecution%2FSourceFactScopeSuite.scala#sha256:ee2ac9a4a7dfd72d5235e1342a26644d2aa876216141ac8f2bcf3efb2e55419c; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fexecution-lifecycle-compiler%2Fpositive%2FLifecycleSourceFactScopeClient.scala#sha256:19ee7cfedada2b5903c469beb9f177d01d6a2ab3a992c54f43b31428c160d1d3; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fexecution-lifecycle-compiler%2Fnegative%2FLifecycleSourceFactScopeMismatch.scala#sha256:0b71242c80c2d0b6b84424bb8efdf216b8f2d08b1decb3374048dd01e2378509; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FExecutionLifecycleCompilerBoundarySuite.scala#sha256:6e97cd6638a4420cb4403963a5892e3f367f91700bea8a038bd7f4e6f3bdfcdb | PASS |
| AC-011 | automated | file:build.sbt#sha256:790e4ae7fa4ab9334b36c0273bc4d44d02d59cac352b50f5f0aeaadc026f5db8; file:execution-lifecycle%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fexecution%2FSourceFacts.scala#sha256:f59e9d5cd97fe7d6a8f28ab57b9b450b8642fdbec8f80266cebddadd7c660fd4; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Fexecution-lifecycle-compiler%2Fnegative%2FExecutionLifecycleHasNoDownstream.scala#sha256:6c2ce12394ff392ffdad46db771a0189589d0b2dc0d7e710b546ad3be98e34ea; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FExecutionLifecycleCompilerBoundarySuite.scala#sha256:6e97cd6638a4420cb4403963a5892e3f367f91700bea8a038bd7f4e6f3bdfcdb | PASS |

## Implementation
- Task Group 1: `ad69a9adf84eb56a80a5e09d07da1e487502530a`
- Final HEAD: `ad69a9adf84eb56a80a5e09d07da1e487502530a`

## Review and QA
- Human Review: approve by requesting-user
- Human QA: skipped by requesting-user — The human requester explicitly directed Human QA to be skipped. The verified delivery changes only the pure execution-lifecycle library construction API: its immutable lifecycle-bound SourceFact scope delegates to existing checked constructors and adds no application/runtime interpreter, service, UI, API, CLI, live-catalog, or wire-format user path. All Slice acceptance criteria require automated evidence only.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0008-simplify-instrument-dependent-apis`
- `openspec/changes/archive/2026-09-04-bind-source-fact-construction`
- `openspec/changes/archive/2026-09-04-bind-source-fact-construction/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/57
