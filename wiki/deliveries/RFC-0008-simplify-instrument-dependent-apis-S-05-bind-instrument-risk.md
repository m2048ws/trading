---
type: delivery
updated: 2026-09-04
rfc: RFC-0008-simplify-instrument-dependent-apis
slice: S-05-bind-instrument-risk
change: bind-instrument-risk
status: archived
archived: 2026-09-04
evidence_manifest: sha256:a512c2e68318d9b39f26cb899009f9d524de936ec6d6bbb2151c910f78fb3f5b
source_digest: sha256:114045c8b2c0b08b7aef8ff5acd4cef89a4a2e36bc02c9a355ec271b160a5f44
---

# RFC-0008-simplify-instrument-dependent-apis/S-05-bind-instrument-risk

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
| AC-017 | automated | file:target%2Fcorgi-verify%2Ffocused-risk.log#sha256:3fd90d3ef5df538743c470e0a6de7e155cce78bdd72a87c73b47d38f3a947f42; file:risk%2Fsrc%2Fmain%2Fscala%2Ftrading%2Frisk%2FRisk.scala#sha256:ff46a9fa93df61f52c397e2c4f9cbf6e8c9cb121f8026335f55581a63bfd7221; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskInstrumentScopeSuite.scala#sha256:eed28ba67c16b6c92435c2ce7cf9c20e8e899cd2592c8ef1dade7282ce81bded; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Frisk-compiler%2Fpositive%2FRiskInstrumentScopeClient.scala#sha256:ae2e94ad40bde3c450d1cf7531192dce86707477b8975ae20d9313a3c936df53 | PASS |
| AC-018 | automated | file:target%2Fcorgi-verify%2Ffull-matrix.log#sha256:c840905b6d974ce926724b843c8393fcae7b61d94c0465850cbd0045125ab1b5; file:target%2Fcorgi-verify%2Ffocused-risk.log#sha256:3fd90d3ef5df538743c470e0a6de7e155cce78bdd72a87c73b47d38f3a947f42; file:risk%2Fsrc%2Fmain%2Fscala%2Ftrading%2Frisk%2FRisk.scala#sha256:ff46a9fa93df61f52c397e2c4f9cbf6e8c9cb121f8026335f55581a63bfd7221; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskInstrumentScopeSuite.scala#sha256:eed28ba67c16b6c92435c2ce7cf9c20e8e899cd2592c8ef1dade7282ce81bded; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskCurveSuite.scala#sha256:50996fa957fcd01f8fdeb91e8d7df188d0c19e4a08dad4e7accbaede69cb9c34; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FExhaustiveLotSizingSuite.scala#sha256:47e53b78d4628f9db0e24f8a19195752b0779eb5c01f1ac248532543b723e878 | PASS |
| AC-019 | automated | file:target%2Fcorgi-verify%2Ffull-matrix.log#sha256:c840905b6d974ce926724b843c8393fcae7b61d94c0465850cbd0045125ab1b5; file:target%2Fcorgi-verify%2Ffocused-risk.log#sha256:3fd90d3ef5df538743c470e0a6de7e155cce78bdd72a87c73b47d38f3a947f42; file:risk%2Fsrc%2Fmain%2Fscala%2Ftrading%2Frisk%2FRisk.scala#sha256:ff46a9fa93df61f52c397e2c4f9cbf6e8c9cb121f8026335f55581a63bfd7221; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskInstrumentScopeSuite.scala#sha256:eed28ba67c16b6c92435c2ce7cf9c20e8e899cd2592c8ef1dade7282ce81bded; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskCurveSuite.scala#sha256:50996fa957fcd01f8fdeb91e8d7df188d0c19e4a08dad4e7accbaede69cb9c34; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskModelBoundarySuite.scala#sha256:396534ec4ee44d893a5d4a2715bc24a1202410a41760c8a3158aa1a6f3382229 | PASS |
| AC-020 | automated | file:target%2Fcorgi-verify%2Ffocused-risk.log#sha256:3fd90d3ef5df538743c470e0a6de7e155cce78bdd72a87c73b47d38f3a947f42; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FRiskCompilerBoundarySuite.scala#sha256:a81602943cad15bf19922e1cc472d4de0f126f0576daeed7e3e336a72e37adaa; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Frisk-compiler%2Fpositive%2FRiskInstrumentScopeClient.scala#sha256:ae2e94ad40bde3c450d1cf7531192dce86707477b8975ae20d9313a3c936df53; file:adversarial-boundary%2Fsrc%2Ftest%2Fresources%2Frisk-compiler%2Fnegative%2FRiskInstrumentScopeMismatch.scala#sha256:63f44ea83fe72454b411ae468c98f59f981f89aa12d2704b3823be280f82fd80 | PASS |
| AC-021 | automated | file:target%2Fcorgi-verify%2Ffull-matrix.log#sha256:c840905b6d974ce926724b843c8393fcae7b61d94c0465850cbd0045125ab1b5; file:target%2Fcorgi-verify%2Frisk-jmh.log#sha256:a2da0a3b880c23a202657c954c07cdc6a7058701570a3fdaa99720c5bababc5b; file:target%2Fcorgi-verify%2Freflection-policy.log#sha256:c0b99e32f07b7e69753e224c23149bf8589ed9a027202ee82c4be2cabc231b3d; file:target%2Fcorgi-verify%2Freflection-regression.log#sha256:eb6658545773b5377bc49220f2c819304dfba1b9e738b527917744a24991037b; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskInstrumentScopeSuite.scala#sha256:eed28ba67c16b6c92435c2ce7cf9c20e8e899cd2592c8ef1dade7282ce81bded; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FRiskCurveSuite.scala#sha256:50996fa957fcd01f8fdeb91e8d7df188d0c19e4a08dad4e7accbaede69cb9c34; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FMaximumAffordableSuite.scala#sha256:e8cdeb2a45eabc38755e48c8b2b998d49a8b5d5a7724e773d8637294f39884d5; file:risk%2Fsrc%2Ftest%2Fscala%2Ftrading%2Frisk%2FMaximumAffordablePropertiesSuite.scala#sha256:99ae4fdce8e0e3a7aade9d9fa32374d205064dc166d3016e586ec6491e9244f7; file:adversarial-boundary%2Fsrc%2Ftest%2Fscala%2Fexternal%2FRiskCompilerBoundarySuite.scala#sha256:a81602943cad15bf19922e1cc472d4de0f126f0576daeed7e3e336a72e37adaa; file:benchmarks%2Fsrc%2Fmain%2Fscala%2Ftrading%2Fbenchmark%2FRiskSizingBenchmark.scala#sha256:fed7607e2c1b97ae6c15722b43543e71557d9858e3d8269ef9f64bc88bdd651d; file:build.sbt#sha256:790e4ae7fa4ab9334b36c0273bc4d44d02d59cac352b50f5f0aeaadc026f5db8 | PASS |

## Implementation
- Task Group 1: `819a4c8dc3fc601655790a512ad24df2df7aee91`
- Final HEAD: `819a4c8dc3fc601655790a512ad24df2df7aee91`

## Review and QA
- Human Review: approve by m2048ws
- Human QA: skipped by m2048ws — The human requester explicitly directed Human QA to be skipped and approved this no-runtime-impact rationale. The verified delivery changes only the pure risk-library convenience API: its immutable instrument-bound scope delegates to existing checked operations and adds no runnable application/runtime interpreter, service, UI, API, CLI, live-data, or wire-format user path. All Slice acceptance criteria require automated evidence only.

## Knowledge Promoted
- Registered this verified delivery as provenance in Architecture, Patterns, and permanent Memory indexes.
- No architectural claim, reusable pattern, or pitfall was inferred without explicit evidence.

## Sources
- `rfcs/RFC-0008-simplify-instrument-dependent-apis`
- `openspec/changes/archive/2026-09-04-bind-instrument-risk`
- `openspec/changes/archive/2026-09-04-bind-instrument-risk/evidence/manifest.json`
- https://github.com/m2048ws/trading/issues/61
