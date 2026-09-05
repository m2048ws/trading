---
type: delivery
updated: 2026-09-04
rfc: RFC-0008-simplify-instrument-dependent-apis
slice: S-02-bind-standard-order-construction
change: bind-standard-order-construction
status: archived
archived: 2026-09-04
evidence_manifest: sha256:e0cb40a5a2bbfb4b3c5ae954d21e7be212433eab7114ed6adbfd81038d228a35
source_digest: sha256:336fb9cc40c59e4cf28a44a8210ed5db6ee2bbdc22025a1fcd71f1d32fb5359d
---

# RFC-0008-simplify-instrument-dependent-apis/S-02-bind-standard-order-construction

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
| AC-004 | automated | file:order-model%2Fsrc%2Fmain%2Fscala%2Ftrading%2Forder%2FOrder.scala#sha256:91df553f518dc02e5ecff4c5fdacd23c32dbcae002daa6257c1631d28acfafc3; file:order-model%2Fsrc%2Ftest%2Fscala%2Ftrading%2Forder%2FInstrumentOrderScopeSuite.scala#sha256:e04784abd05e4537dbe86937f0b1a46d851835732c709d3889d9d7481f1823df; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Forder-model-compiler%2Fpositive%2FInstrumentOrderScopeClient.scala#sha256:5d07bb116d874991a8954cc2af556817b0ed3ce7c1e21dde4aeeb7b29e328b43 | PASS |
| AC-005 | automated | file:order-model%2Fsrc%2Fmain%2Fscala%2Ftrading%2Forder%2FOrder.scala#sha256:91df553f518dc02e5ecff4c5fdacd23c32dbcae002daa6257c1631d28acfafc3; file:order-model%2Fsrc%2Ftest%2Fscala%2Ftrading%2Forder%2FInstrumentOrderScopeSuite.scala#sha256:e04784abd05e4537dbe86937f0b1a46d851835732c709d3889d9d7481f1823df | PASS |
| AC-006 | automated | file:order-model%2Fsrc%2Fmain%2Fscala%2Ftrading%2Forder%2FOrder.scala#sha256:91df553f518dc02e5ecff4c5fdacd23c32dbcae002daa6257c1631d28acfafc3; file:order-model%2Fsrc%2Ftest%2Fscala%2Ftrading%2Forder%2FInstrumentOrderScopeSuite.scala#sha256:e04784abd05e4537dbe86937f0b1a46d851835732c709d3889d9d7481f1823df | PASS |
| AC-007 | automated | file:order-model%2Fsrc%2Ftest%2Fscala%2Ftrading%2Forder%2FInstrumentOrderScopeSuite.scala#sha256:e04784abd05e4537dbe86937f0b1a46d851835732c709d3889d9d7481f1823df; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Forder-model-compiler%2Fpositive%2FInstrumentOrderScopeClient.scala#sha256:5d07bb116d874991a8954cc2af556817b0ed3ce7c1e21dde4aeeb7b29e328b43; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Forder-model-compiler%2Fnegative%2FInstrumentOrderScopeMismatch.scala#sha256:d5e3f0af51bc2c172a1e01ff13ce139c810783b7424bbf208947167f926ad756; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FEconomicsCompilerBoundarySuite.scala#sha256:94127fb0bea280618de7408b8c01e3857fb03d7196a34af7fd96ee63f299f87b | PASS |

## Implementation
- Task Group 1: `804f32e3e0ed8ccda8245ee3530779c47904169c`
- Final HEAD: `804f32e3e0ed8ccda8245ee3530779c47904169c`

## Review and QA
- Human Review: approve by requesting-user
- Human QA: skipped by requesting-user — The human requester explicitly directed S-02 Human QA to be skipped. The verified delivery changes only the pure order-model library construction API: its immutable instrument-bound scope delegates to existing checked constructors and adds no application/runtime interpreter, service, UI, API, CLI, or wire-format user path. All Slice acceptance criteria require automated evidence only.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0008-simplify-instrument-dependent-apis`
- `openspec/changes/archive/2026-09-04-bind-standard-order-construction`
- `openspec/changes/archive/2026-09-04-bind-standard-order-construction/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/55
