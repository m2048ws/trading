## Context

See `proposal.md` for motivation. Risk behavior is currently split across four deliberately narrow owners:
`Risk.downside` checks an instrument-specific PnL, `MonotoneLotRisk` constructs certified curves,
`ExhaustiveLotSizing.select` traverses an arbitrary pure evaluator, and `MaxAffordableLots` sizes an already certified
model. The first three repeat one stable instrument even though their dependent parameters and results all derive from
that value. The fourth is intentionally model-bound and needs no instrument.

RFC-0008/S-01 is integrated, so an instrument directly exposes exact `PositionD` and `SettleD` aliases. Existing
`single` and complete-table constructors deliberately accept existential assessments or rows so they can validate
runtime identity and dimension evidence before narrowing them. Existing curve constructors also retain structural
construction costs, while exhaustive sizing retains the first located caller failure and performs an explicit linear
traversal.

## Goals / Non-Goals

**Goals:**

- Bind instrument-specific risk calls to one exact, immutable instrument without widening dependent results.
- Preserve the current operation owners as the only implementations of mathematics, validation, traversal, and
  evidence construction.
- Retain broad untrusted inputs at `single` and complete-table boundaries and every existing deterministic failure.
- Prove concise completed-artifact calls, compile-time rejection of incompatible values, pure reuse, and unchanged
  construction and observation costs.

**Non-Goals:**

- Bind model composition, quantization, `MaxAffordableLots.select`, or its observation-bound calculation; those are
  already expressed entirely by a validated model.
- Add a risk builder, portfolio or account context, evaluator cache, mutable session, ambient given, effect, resource,
  live catalog, market-data source, or runtime interpreter.
- Change downside arithmetic, curve vocabulary, validation order, cap semantics, exhaustive traversal, error types,
  public direct entry points, dependency coordinates, or the JDK 25 minimum.
- Move risk behavior onto `Instrument` or add a lower-layer dependency on risk.

## Decisions

### Expose one owner-local scope from `Risk`

`Risk.forInstrument(instrument)` will return a final immutable
`Risk.InstrumentScope[instrument.type]`. The scope captures exactly that assembled instrument and exposes these thin
operations:

- `downside(pnl)`;
- `single(assessment)`;
- `affine(cap, firstLotLoss, additionalLotLoss)`;
- `piecewise(cap, segments)`;
- `fromCompleteTable(cap, observations)`; and
- `selectExhaustively(budget, cap)(evaluate)`.

The scope will publish local aliases derived from the captured singleton: `PositionD`, `SettleD`, `Loss`, `Budget`,
`Assessment`, `Model`, and `Decision`. `Loss` and `Budget` retain exact settlement quantities and nonnegative evidence;
`Assessment`, `Model`, and `Decision` retain the exact position-to-settlement relationship. These aliases are ergonomic
names for existing types, not new values, dimensions, evidence, or wrappers.

`Risk` is the owner of this facade because it is the public entry point for instrument-specific risk use. Adding a
method to `Instrument` would reverse the dependency edge, while putting the scope on `MonotoneLotRisk` would make
downside and arbitrary exhaustive evaluation look model-owned. A top-level context, extension, type class, structural
refinement, or ambient given would obscure ownership or add a mechanism without protecting another invariant.

The factory is total because an assembled instrument is already trusted context. It returns a scope parameterized by
the argument's singleton type and performs no eager validation, normalization, calculation, or allocation beyond the
small immutable scope itself.

### Omit only the captured instrument and delegate one-to-one

Each method calls the corresponding existing operation with `instrument`:

- `downside` delegates to `Risk.downside`;
- `single`, `affine`, `piecewise`, and `fromCompleteTable` delegate to their matching `MonotoneLotRisk` operations; and
- `selectExhaustively` delegates to `ExhaustiveLotSizing.select`.

Current direct calls remain public and canonical. The facade does not reverse those entry points through itself,
duplicate validation, rebuild an error, extract an `Either`, catch expected invalidity, precompute assessments, or
introduce a cast. Defaults do not change because none of these direct calls currently hides an instrument-dependent
default.

The scope signatures use `instrument.PositionD`, `instrument.SettleD`, `instrument.Lots`, and `instrument.Pnl` where
the direct operation is already strongly typed. This retains the exact evaluator relationship
`instrument.Lots => Either[E, instrument.Pnl]` and the exact returned `ExhaustiveLotDecision` without explicit method
type arguments at call sites.

### Preserve existential trust boundaries unchanged

`single` continues to accept `LotRiskAssessment[? <: Dim, ? <: Dim]`, and `fromCompleteTable` continues to accept
`Vector[(Lots[? <: Dim], Pnl[? <: Dim])]`. The scope must not narrow either argument to its aliases: doing so would
remove the public route by which same-shaped foreign identity, dimension, grid, coordinate, coverage, and monotonicity
evidence is rejected at runtime.

Delegating these arguments unchanged preserves the current deterministic `ModelViolations` sequence and the existing
checked narrowing quarantine inside complete-table construction. No new cast or trust assertion is added to the scope.
Similarly, a same-dimensional foreign PnL supplied to `downside` still reaches the canonical `InstrumentId` check, and
an exhaustive evaluator's typed error still returns at its exact first ascending coordinate.

### Keep model algebra and primary sizing independent

`MonotoneLotRisk.add`, `minimum`, `maximum`, and `quantized` remain model-to-model operations. `MaxAffordableLots.select`
and `maximumObservationBound` remain model-bound because a successfully constructed model already carries instrument
identity, dimensions, cap, and monotonicity evidence. Adding those calls to the scope would duplicate an unrelated
receiver and invite the facade to grow into a service locator.

Focused tests will use scoped models with the existing combinators and primary selector to prove interoperability, but
the completed-artifact fixture will call those owners directly. Construction-cost equality will be asserted for
affine, piecewise, and table models. Exhaustive evaluator counts and ascending coordinates will be compared with the
direct path, and the existing logarithmic primary-sizing tests and representative JMH compilation remain unchanged.

### Make reuse observationally inert

`InstrumentScope` contains only one immutable `val instrument`; it retains no PnL, budget, cap, curve, segment,
assessment, table, evaluator, result, mutable collection, memo entry, synchronization primitive, thread-local, clock,
catalog, client, or effect. Every call therefore allocates or computes exactly as its direct counterpart does.

A focused `RiskInstrumentScopeSuite` will compare every scoped method with its direct operation for exact successes,
complete failures, costs, assessments, evaluator coordinates, and decisions. It will interleave different operations,
repeat calls with distinct inputs, and reuse one scope concurrently. The test will also ensure that successful model
objects from separate calls are independently allocated even when structurally equivalent.

### Verify the completed-artifact type and dependency boundary

A positive packaged Scala fixture will bind one instrument, exercise every scoped operation, ascribe the scope's local
aliases and exact returned models and decisions, and retain the evaluator's `instrument.Lots` to `instrument.Pnl`
relationship without explicit dimension arguments, role projections, structural refinements, or casts.

An independently compilable negative fixture will supply an incompatible PnL, loss, segment, budget, and evaluator
from another instrument shape. Its prelude must compile and only marked scope calls must fail with ordinary type
mismatches. Separate focused runtime cases will use broad existential and deliberately same-shaped foreign inputs to
prove that compile-time binding does not replace identity, grid, coordinate, coverage, or monotonicity checks.

`RiskCompilerBoundarySuite` will compile and run the positive fixture, compile the negative prelude, require all marked
calls to fail, and retain completed-JAR/classpath assertions. Source and artifact checks will continue to exclude
codec, order, execution, scenario, fee, application, runtime, effect, catalog, persistence, stream, telemetry, venue,
and benchmark responsibility from production risk.

## Risks / Trade-offs

- [The captured instrument type widens and loses dependent precision] → Parameterize the scope by the factory
  argument's singleton type and compile exact alias, model, evaluator, and decision ascriptions from the packaged JAR.
- [The facade becomes a second risk implementation] → Make every method a one-to-one typed delegate and compare exact
  results, complete failures, costs, coordinates, and decisions with the direct operation.
- [Binding is mistaken for runtime authority] → Retain existential `single` and table inputs and cover same-shaped
  foreign identities, dimensions, grids, coordinates, coverage, and monotonicity failures at runtime.
- [A concise exhaustive call hides its linear cost] → Name it `selectExhaustively`, retain explicit cap and evaluator
  inputs, document the delegation, and assert the same ascending observations as the explicitly linear owner.
- [The scope absorbs model-generic algebra] → Leave combinators, quantization, primary selection, and its bound on their
  current owners and test only that scoped models interoperate with them.
- [Reusable syntax introduces caching or ordering effects] → Keep the class final and field-only and verify distinct
  model allocation plus sequential, interleaved, and concurrent equivalence.
- [The scope expands the pure dependency cone] → Retain risk's completed-classpath, JAR-entry, forbidden-reference, and
  source-import checks while compiling the new public API from the packaged artifact.

## Migration Plan

1. Add `Risk.InstrumentScope`, its aliases, the six direct delegates, and `Risk.forInstrument` while retaining every
   existing entry point.
2. Add focused success, failure, broad-input, cost, traversal, allocation, interleaving, and concurrent-reuse tests.
3. Add completed-artifact positive and negative compiler fixtures and extend the risk boundary suite without weakening
   its dependency assertions.
4. Run formatting, focused risk and compiler-boundary checks, the clean JDK-25 aggregate test/JMH-compile matrix, and
   the automated Task Group review. Roll back by reverting the dedicated Task Group commit; no data, schema, wire,
   dependency, runtime, or deployment migration is required.
