## ADDED Requirements

### Requirement: Instrument-bound risk scope
The pure risk artifact SHALL expose one final immutable scope bound to one exact `Instrument`. The scope SHALL retain
the captured instrument's position and settlement dimensions as local aliases and SHALL provide instrument-specific
operations for downside measurement, checked single-assessment model construction, affine model construction,
piecewise model construction, checked complete-table model construction, and explicitly exhaustive lot sizing.

Each scoped operation SHALL omit only the captured instrument and SHALL preserve the direct operation's remaining
inputs, exact dependent result type, typed failures, and complexity class. Scope creation SHALL acquire no resource,
perform no effect, observe no external state, and move no operation-time validation into construction.

#### Scenario: Reuse one scope across risk operations
- **WHEN** a caller binds one assembled instrument and invokes downside, affine, piecewise, complete-table, and
  exhaustive sizing operations through the resulting scope
- **THEN** every operation retains that instrument's exact position-to-settlement relationship without explicit
  dimension arguments or repeated instrument parameters

#### Scenario: Build a single-assessment model
- **WHEN** a caller supplies an already checked one-lot assessment to the bound `single` operation
- **THEN** the scope returns the same exact model or same non-empty ordered model violations as direct checked
  construction for the captured instrument

#### Scenario: Keep operation inputs explicit
- **WHEN** a scoped operation requires a cap, loss formula, segment vector, complete observation table, risk budget, or
  arbitrary evaluator
- **THEN** that input remains explicit at the operation and is not cached, defaulted, or hidden in ambient state

### Requirement: Bound risk behavior is exactly preserved
Every bound operation SHALL delegate to the canonical risk operation and SHALL produce exactly equal success or failure
values. In particular, the scope SHALL preserve `RiskIdentityError`, the complete deterministic `ModelViolations`
sequence, `CurveConstructionCost`, model cap and assessments, located caller evaluation causes, and exhaustive decision
evidence. It SHALL preserve exact rational arithmetic and SHALL NOT quantize, catch expected invalidity, manufacture a
partial decision, or introduce unchecked casts at the public boundary.

#### Scenario: Preserve downside identity validation
- **WHEN** a statically compatible PnL carries a foreign runtime instrument identity
- **THEN** scoped downside measurement returns the same typed identity mismatch as the direct operation before
  inspecting its net PnL

#### Scenario: Preserve piecewise structural failures
- **WHEN** a proposed bound piecewise curve has multiple independent domain, breakpoint, marginal-loss, or downward-
  boundary violations
- **THEN** construction returns every violation in the same deterministic source order as direct construction and no
  model

#### Scenario: Preserve complete-table validation and cost
- **WHEN** a complete-table proposal is valid or contains foreign identities, dimensions, grids, coordinates,
  coverage gaps, ordering defects, duplicates, or a monotonicity decrease
- **THEN** the bound operation returns exactly the direct model and linear table-inspection cost or exactly the direct
  complete ordered violations

#### Scenario: Preserve arbitrary evaluation failure
- **WHEN** a scoped exhaustive evaluator fails at a positive lot coordinate
- **THEN** sizing stops at the same first ascending coordinate, retains the caller's typed cause, and returns no partial
  affordability decision

#### Scenario: Preserve exhaustive success
- **WHEN** every scoped exhaustive observation succeeds for a supplied cap and exact budget
- **THEN** sizing evaluates the same coordinates in ascending order and returns the same no-affordable or greatest-
  affordable decision with the same assessments as the direct operation

### Requirement: Least-trusted model inputs remain checked
Binding an instrument SHALL NOT narrow inputs whose broad existential types are required to test runtime coherence.
Single-assessment construction SHALL continue to accept a broadly typed assessment, and complete-table construction
SHALL continue to accept broadly typed lot and PnL rows. Both SHALL validate the captured instrument identity, position
and settlement dimensions, grids and coordinates, domain coverage, and monotonicity before returning a precisely typed
model.

#### Scenario: Reject a foreign single assessment
- **WHEN** a broadly typed assessment belongs to another instrument or carries incompatible position or settlement
  dimension evidence
- **THEN** scoped `single` returns the same non-empty, deterministically ordered identity, dimension, and coordinate
  violations as direct construction

#### Scenario: Reject incoherent table rows
- **WHEN** complete-table rows mix a foreign instrument, position grid, settlement dimension, coordinate, ordering,
  coverage, or monotonicity defect
- **THEN** scoped construction reports the same indexed and structural violations as direct construction rather than
  trusting the rows because the scope is bound

### Requirement: Bound risk API remains type-safe, pure, and owner-local
Completed-artifact Scala clients SHALL be able to construct and exhaustively size risk through one bound scope without
explicit dependent type arguments, nested role-dimension projections, structural refinements, or unchecked casts. The
compiler SHALL reject callbacks, budgets, losses, segments, and other dimension-dependent values that do not match the
captured instrument, while successful exhaustive evaluation SHALL retain the exact relationship from
`instrument.Lots` to `instrument.Pnl`.

The scope SHALL be safely reusable under sequential, interleaved, and concurrent pure invocation. It SHALL retain no
operation input or result, perform no memoization or synchronization, and produce no order-dependent result. Model
composition, quantization, model-bound maximum-affordable selection, and its logarithmic observation bound SHALL remain
on their existing model owners and independent of an instrument scope. The risk artifact SHALL gain no codec,
execution, application, runtime, effect, catalog, persistence, stream, telemetry, or venue dependency.

#### Scenario: Compile a concise completed-artifact client
- **WHEN** downstream Scala binds one instrument and uses the scope for downside, model construction, and exhaustive
  sizing
- **THEN** the completed risk artifact typechecks exact model, assessment, failure, and decision results without local
  dimension aliases or explicit dependent type arguments

#### Scenario: Reject incompatible callbacks and budgets
- **WHEN** a completed-artifact client supplies an evaluator or risk budget whose position or settlement dimension is
  incompatible with the captured instrument
- **THEN** compilation fails with an ordinary type mismatch before the operation can run

#### Scenario: Preserve independent model operations
- **WHEN** compatible models are combined, quantized, or passed to maximum-affordable selection
- **THEN** those operations remain usable without a bound scope and retain their existing validation, assessment
  evidence, and observation-complexity guarantees

#### Scenario: Reuse the scope concurrently
- **WHEN** one bound scope is invoked repeatedly, interleaved, or concurrently with immutable inputs
- **THEN** each result equals its direct counterpart and no prior or concurrent invocation changes another result

#### Scenario: Preserve the pure dependency cone
- **WHEN** the completed risk artifact and the bound scope are inspected
- **THEN** risk continues to depend only on quantities and instrument economics and contains no effectful or higher-
  layer responsibility
