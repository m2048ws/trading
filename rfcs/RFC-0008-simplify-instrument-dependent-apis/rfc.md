# RFC-0008-simplify-instrument-dependent-apis: Simplify instrument-dependent Scala APIs

## Goal

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

## Non-goals

- Do not add order, codec, execution, risk, or market-state behavior directly to `Instrument`; it remains an
  instrument-economic value rather than becoming a cross-layer service locator.
- Do not introduce one repository-wide context, universal instrument facade, generic service interface, type class,
  macro, code generator, effect parameter, dependency-injection container, or mutable session for these operations.
- Do not move concepts, errors, validation, or policy between instrument economics, order model, execution lifecycle,
  boundary codecs, and risk, or introduce a dependency from a lower-level owner to one of its consumers.
- Do not change order kinds, source-fact vocabulary, execution replay, scenario record schemas, canonical wire
  encodings, risk mathematics, sizing policy, market conversion mathematics, or PnL.
- Do not hide genuine per-operation inputs such as order instructions, event identities, decode limits, record
  locations, risk budgets, caps, evaluation functions, prices, rates, or additional conversions in ambient state.
- Do not remove runtime identity checks merely because a bound scope fixes compile-time dimensions. Inputs that are
  dimension-compatible but belong to another instrument, target, catalog generation, grid, or record remain checked.
- Do not make a live catalog, application port, runtime interpreter, cache, lock, parser object, or external client part
  of a pure bound scope.
- Do not promise ordinary Java source compatibility or binary compatibility for the Scala-first domain API.

## Boundary

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

## Slices

### S-01-name-instrument-dimensions: Expose direct instrument dimension aliases

- AC-001 [evidence: automated]: `Instrument` exposes `PositionD`, `BaseD`, `QuoteD`, and `SettleD` as exact compile-time
  aliases of its four role dimensions while existing instrument-dependent members preserve their runtime
  representation, identity, grids, and dependent result types.
- AC-002 [evidence: automated]: Completed-artifact positive fixtures use the direct aliases interchangeably with the
  corresponding role dimensions, while negative fixtures reject cross-instrument or cross-role values whose dimensions
  differ; no cast, match type, runtime lookup, or replacement dimension evidence implements the aliases.
- AC-003 [evidence: automated]: Instrument economics gains no dependency on order, execution, scenario, fee, risk,
  codec, application, or runtime code, and `Instrument` exposes no operation owned by those consumers.

### S-02-bind-standard-order-construction: Bind standard order constructors to one instrument

- AC-004 [evidence: automated]: The order model exposes one immutable instrument-bound scope whose market, limit,
  stop-market, and stop-limit methods accept the current arguments and defaults without repeating the instrument or
  explicit position, base, and quote dimension arguments.
- AC-005 [evidence: automated]: Every scoped constructor returns exactly the current refined `Order.Aux` activation and
  execution shape and is behaviorally equal to the characterized direct constructor for valid orders and the full,
  deterministically ordered `OrderViolations` of invalid orders.
- AC-006 [evidence: automated]: Reusing one scope constructs independent immutable orders without cached, mutable,
  effectful, or thread-local state, and invocation order does not change any result.
- AC-007 [evidence: automated]: Completed-artifact fixtures show concise market and priced-order construction and reject
  incompatible lots, prices, triggers, pegs, and visibility; generic checked construction continues to validate
  independently supplied values rather than trusting the scope by association.

### S-03-bind-source-fact-construction: Bind source facts to one execution lifecycle

- AC-008 [evidence: automated]: Execution lifecycle exposes one immutable lifecycle-bound source-fact scope covering
  accepted, rejected, fill, correction, bust, cancellation-effective, reconciliation-checkpoint,
  source-order-completed, and source-order-absent construction without repeated lifecycle dimensions.
- AC-009 [evidence: automated]: Each scoped operation returns the same precisely typed fact or same deterministically
  ordered `SourceFactViolations` as characterized fact-owner construction, including logical-order, target,
  instrument, grid, ordering, reference, checkpoint, and completeness failures.
- AC-010 [evidence: automated]: Completed-artifact fixtures construct all nine fact forms without explicit dimensions,
  reject incompatible lot and price dimensions, and still report runtime identity or target mismatch for a statically
  compatible foreign value.
- AC-011 [evidence: automated]: The scope contains no command, dispatch, state, replay, observation, effective-ledger,
  codec, application, or runtime responsibility, and execution retains its current one-way dependencies and purity.

### S-04-bind-scenario-record-codecs: Separate bound scenario encoders and snapshot decoders

- AC-012 [evidence: automated]: Boundary codecs expose an immutable scenario encoder bound only to one exact instrument
  and an immutable scenario decoder bound to that instrument plus one immutable `CatalogSnapshot`; neither context
  acquires a live catalog or observes another snapshot during an operation or batch.
- AC-013 [evidence: automated]: The encoder covers record construction and canonical wire encoding, while the decoder
  covers record reconstruction, decode-and-reconstruct, and ordered batch reconstruction for order and round-trip
  scenarios with exact dependent result types.
- AC-014 [evidence: automated]: Context-free parsing, record-only encoding, and schema access remain usable without an
  instrument or snapshot; `DecodeLimits` and record indices remain explicit inputs and batch diagnostics retain stable
  input locations.
- AC-015 [evidence: automated]: Characterization, golden, malformed-input, and batch tests prove byte-identical supported
  wire output and exactly equal values, errors, ordering, canonicalization, version handling, null rejection, and
  coherent-snapshot reconstruction between bound and current entry points.
- AC-016 [evidence: automated]: Completed-artifact fixtures reuse each context across both scenario families without
  local D/B/Q aliases, reject incompatible scenario shapes, and keep record, parser, schema, and reconstruction
  behavior in the codec artifact.

### S-05-bind-instrument-risk: Bind instrument-specific risk and exhaustive sizing

- AC-017 [evidence: automated]: Risk exposes one immutable instrument-bound scope for downside measurement, `single`,
  affine, piecewise, and complete-table monotone model construction, and arbitrary exhaustive lot sizing with local
  aliases derived from the captured position and settlement dimensions.
- AC-018 [evidence: automated]: Scoped operations preserve exact `RiskIdentityError`, `ModelViolations`, located caller
  evaluation failures, construction cost, monotonicity, cap, assessment, and exhaustive-decision results.
- AC-019 [evidence: automated]: `single` and complete-table construction retain the least-trusted supported existential
  inputs and deterministically reject foreign instrument, dimension, grid, coordinate, coverage, and monotonicity
  violations rather than narrowing those checks away.
- AC-020 [evidence: automated]: Completed-artifact fixtures build and size risk without explicit dependent type
  arguments, reject incompatible callbacks and budgets, and retain the exact `instrument.Lots` to `instrument.Pnl`
  relationship in exhaustive evaluation without unchecked casts.
- AC-021 [evidence: automated]: Model combinators and model-bound maximum-affordable selection remain independent of the
  scope, risk remains pure and free of codec, execution, application, and runtime dependencies, and existing complexity
  bounds and representative measurements do not regress.

### S-06-bind-market-state-construction: Consolidate and bind market-state constructors

- AC-022 [evidence: automated]: Each of the eight market-state construction modes has one direct operation with an empty
  default for additional settlement conversions and no duplicated overload implementation; omitted and explicit empty
  conversions produce exactly equal results.
- AC-023 [evidence: automated]: Instrument economics exposes an immutable instrument-bound market-state scope covering
  all eight modes with exact captured price, base, quote, settlement, rate, conversion, violation, and state types and
  without repeated instrument or role-dimension arguments.
- AC-024 [evidence: automated]: Characterization and property tests prove identical conversion derivation, validation
  accumulation and ordering, rational values, identity and grid checks, additional-conversion handling, and
  `MarketStateViolations` for direct and scoped construction.
- AC-025 [evidence: automated]: Completed-artifact fixtures reuse one scope across anchor and rate construction, reject
  incompatible prices, rates, and conversions, and confirm that the scope adds no higher-layer, effect, codec,
  registry, or runtime dependency and does not absorb `SettlementConversion` ownership.

## Risks

- A convenience scope could grow into a service locator that obscures ownership. Mitigation: every scope is defined by
  its existing owner, captures only the stable value genuinely shared by its operations, and excludes sibling and
  downstream responsibilities.
- Capturing an instrument may make compile-time compatibility look like runtime identity proof. Mitigation: preserve
  all identity, target, grid, catalog, and record checks for statically compatible foreign values.
- Path-dependent wrapper implementation can widen singleton types or require casts. Mitigation: return the exact
  captured singleton relationship and verify positive and negative use from packaged completed artifacts.
- Moving validation into a factory could change when a null or malformed value fails. Mitigation: keep validation at
  its current semantic operation unless it depends only on captured context, and characterize observable behavior.
- Scoped and direct entry points could diverge. Mitigation: retain one implementation of every check and calculation,
  make compatibility entry points thin delegates, and compare both paths while both are supported.
- A decoder capturing mutable or live reference data could mix catalog generations. Mitigation: capture exactly one
  immutable `CatalogSnapshot` and prohibit live catalog access from the codec scope.
- A risk facade could absorb model-generic algebra. Mitigation: bind only operations whose semantics require an
  instrument and leave model combinators and model-bound sizing with their existing owners.
- Defaulted market-state conversions could introduce ambiguity or change inference. Mitigation: compile representative
  omitted, explicit-empty, and non-empty calls before removing redundant overloads.
- Additional allocation could affect hot loops. Mitigation: scopes are immutable and reusable, add no per-element
  coordination or nested traversal, and preserve existing complexity claims and representative measurements.
