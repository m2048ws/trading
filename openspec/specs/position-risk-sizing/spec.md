# position-risk-sizing Specification

## Purpose
Defines exact downside-risk measurement and deterministic maximum-lot selection by evaluating complete fee-inclusive trade scenarios on an instrument's discrete lot grid.

## Requirements

### Requirement: Downside risk from net PnL
A pure risk operation SHALL consume an explicit instrument and core `Pnl` and return
`NonNegative[Quantity[settle.D]]` exactly as:

```text
max(0, -netPnl)
```

The operation SHALL validate ordinary runtime instrument identity, use the existing quantity/refinement algebra,
preserve exact rational semantics, and perform no quantization, floating-point conversion, or raw-scalar reconstruction.
`Instrument` SHALL NOT expose an owned sizing capability.

#### Scenario: Measure a losing scenario
- **WHEN** net PnL is exact `-17/3` in the supplied instrument's settlement dimension
- **THEN** downside risk is refined nonnegative exact `17/3`

#### Scenario: Clamp profitable risk to zero
- **WHEN** net PnL is zero or positive
- **THEN** downside risk is the lawful refined zero in the same settlement dimension

#### Scenario: Reject foreign PnL
- **WHEN** instrument and PnL carry different runtime `InstrumentId` values
- **THEN** risk evaluation returns a typed identity error before inspecting net PnL

#### Scenario: Keep risk downstream
- **WHEN** Scala depends only on instrument economics
- **THEN** downside-risk and sizing operations are absent from that artifact and remain owned by the pure risk artifact

#### Scenario: Preserve the refinement for later comparison
- **WHEN** downside risk is compared with a nonnegative risk budget
- **THEN** both operands retain typed settlement dimension and nonnegative evidence without another sign check

### Requirement: Sizing failures are explicit
Monotone-model construction failures SHALL be distinct from unaffordable assessed lot sizes. Independent model
structure, identity, domain, breakpoint, slope, boundary, and dimension violations SHALL accumulate in deterministic
source order and prevent construction. Once construction succeeds, primary maximum-affordable sizing SHALL be total
for every refined budget because every observation in the model's declared range is already known to be valid.

The explicit exhaustive fallback SHALL preserve caller-owned typed evaluation causes with their exact positive lot
coordinates and SHALL return no affordability decision after an unknown required observation. Failures SHALL NOT be
thrown, converted to strings, skipped, treated as excessive risk, or replaced by a smaller selected lot size.

#### Scenario: Accumulate independent curve violations
- **WHEN** a proposed piecewise model contains multiple independently detectable domain, breakpoint, and boundary
  violations
- **THEN** construction returns every violation in stable source order and no monotone model

#### Scenario: Search only after validation
- **WHEN** a monotone lot-risk model has been constructed successfully
- **THEN** maximum-affordable sizing returns a decision without an observation-failure branch

#### Scenario: Preserve an exhaustive evaluation failure
- **WHEN** the explicit fallback evaluator returns a typed fee, conversion, scenario, or PnL cause at lot `n`
- **THEN** sizing retains `n` and that cause and returns no partial maximum

#### Scenario: Propagate a missing conversion
- **WHEN** the explicit fallback evaluator reports a missing settlement conversion at lot `n`
- **THEN** sizing returns a located failure retaining `n` and the typed missing-conversion cause rather than a smaller
  affordability decision

#### Scenario: Propagate an invalid adverse exit
- **WHEN** upstream scenario evaluation reports that an adverse exit does not close the proposed position at lot `n`
- **THEN** exhaustive sizing returns the located typed scenario cause and no partial maximum

#### Scenario: Reject a scenario for a different candidate count
- **WHEN** an evaluator requested for lot `n` returns a typed mismatch showing a different held-position coordinate
- **THEN** exhaustive sizing stops at `n`, preserves the requested and observed coordinates in the caller-owned cause,
  and does not evaluate a later lot

#### Scenario: Return the same maximum under equivalent traversal
- **WHEN** equivalent pure exhaustive evaluators produce the same successful assessment or typed failure at every
  required lot coordinate
- **THEN** deterministic ascending traversal returns the same greatest affordable lot or the same first located failure

### Requirement: Position-sizing scope is bounded
Primary maximum-affordable sizing SHALL describe a standalone proposed position in one instrument from flat exposure
under one immutable sizing context. Instrument identity, direction, valuation inputs, adverse-price assumptions, fee
inputs, and the model's lot range SHALL remain fixed while lot count varies. Thresholds, caps, quantization, and nested
liquidity effects MAY vary with lot count only when represented by the checked curve algebra without violating the
nondecreasing downside law.

Neither primary nor exhaustive sizing SHALL imply fill probability, guarantee execution, include an existing account
position, calculate margin or liquidation thresholds, forecast funding, acquire changing data, or make
diversification, concentration, collateral, hedge, or portfolio-risk claims. Account- and portfolio-aware sizing require
a later explicit capability whose feasible set and objective are not mislabeled as an isolated monotone curve.

#### Scenario: Hold sizing context fixed
- **WHEN** primary sizing compares two lot counts
- **THEN** both observations use the same instrument, direction, valuation state, adverse-price assumptions, fee inputs,
  and curve version

#### Scenario: Treat maker-only PnL as conditional
- **WHEN** an upstream model assumes completed maker-only, limit, or sliced execution
- **THEN** sizing evaluates the resulting immutable conditional loss model without asserting that execution will occur

#### Scenario: Exclude current-position effects
- **WHEN** an existing position could make an additional order reduce or hedge account risk
- **THEN** that problem is outside standalone monotone sizing and requires an explicit account/portfolio capability

#### Scenario: Exclude liquidation mechanics
- **WHEN** no liquidation or margin model is present
- **THEN** the decision expresses only the supplied exact downside-budget rule

### Requirement: Pure risk artifact and lot-risk assessment
Exact downside-risk and lot sizing SHALL be delivered by a pure risk artifact depending only on quantities and
instrument economics. Instrument economics, order model, execution scenarios, and fee policy SHALL NOT depend on risk.
The risk artifact SHALL contain no market-data acquisition, catalog lookup, clock, persistence, account service,
concurrency, stream, transaction, tracing, metrics, or other effect interpreter.

A lot-risk assessment SHALL associate one positive `Lots` value for an explicit instrument with one exact
`NonNegative[Quantity[settle.D]]` downside value. It SHALL preserve the instrument identity and typed settlement
dimension. It SHALL be produced by a lot-risk model or by a checked operation that consumes the explicit instrument,
lots, and core `Pnl` and derives downside itself; no public constructor SHALL accept an unrelated risk claim.

#### Scenario: Assess one lot size
- **WHEN** a pure lot-risk model observes a valid positive lot size in its declared range
- **THEN** it returns that exact lot size and its exact refined downside risk under the model's immutable inputs

#### Scenario: Derive an assessment from PnL
- **WHEN** checked construction receives coherent instrument-bound lots and core PnL
- **THEN** it associates the lots with downside derived exactly from that PnL rather than accepting a separate risk

#### Scenario: Reject mixed instrument identity
- **WHEN** model inputs contain lots, PnL, or quantities for a different runtime `InstrumentId`
- **THEN** construction returns a typed risk-location violation rather than creating a lot-risk assessment

#### Scenario: Keep effects outside risk
- **WHEN** downstream Scala depends on the risk artifact
- **THEN** every operation consumes immutable values, checked pure models, or explicitly pure evaluators and no
  operation returns an abstract effect

### Requirement: Constructive monotone lot-risk model
The primary sizing input SHALL be an immutable exact lot-risk model with a finite positive whole-number domain
`1..cap`. Successful construction SHALL establish all of the following:

- every lot count in the domain has exactly one total lot-risk assessment;
- assessment preserves one instrument and settlement dimension throughout the domain; and
- for all domain values `a <= b`, `risk(a) <= risk(b)`.

The monotonic guarantee SHALL be introduced only by library-controlled construction from a closed exact curve
representation, by complete validation of a finite table, or by combinators whose closure under monotonicity is tested
as an algebraic law. Public callers SHALL NOT convert an arbitrary function into the model through a boolean,
assertion, cast, marker subtype, type-class instance, or unchecked proof token.

The exact curve vocabulary SHALL support the fixed isolated-instrument shapes needed by ordinary sizing, including
affine loss with nonnegative marginal loss, checked piecewise loss with ordered boundaries and no downward boundary
jump, pointwise addition and minimum/maximum of compatible monotone curves, and order-preserving grid quantization.
Independent structural violations SHALL accumulate deterministically before construction succeeds. Algebraic
construction SHALL inspect only explicit formula structure and breakpoints rather than enumerating `1..cap`; complete
finite-table validation is the explicit linear-cost exception.

#### Scenario: Construct an affine loss curve
- **WHEN** exact first-lot loss is supplied, possibly signed, and every additional lot contributes a nonnegative exact
  marginal loss
- **THEN** construction produces a monotone model whose risk at `n` is exact `max(0, affineLoss(n))`

#### Scenario: Construct a stepped venue-style curve
- **WHEN** ordered fee or risk thresholds change marginal loss without making it negative and every boundary value is
  no lower than the preceding value
- **THEN** construction produces one exact piecewise monotone model across the declared cap

#### Scenario: Preserve monotonicity through quantization
- **WHEN** an exact monotone curve is quantized by an order-preserving uniform-grid operation
- **THEN** the resulting lot-risk model remains monotone and retains exact grid semantics

#### Scenario: Keep a large affine model compact
- **WHEN** an affine or algebraically composed model declares a cap containing many lot counts
- **THEN** construction validates its explicit formula structure without observing every lot in the domain

#### Scenario: Reject a downward boundary
- **WHEN** a proposed piecewise curve has a negative marginal interval or a boundary whose first value is below the
  preceding interval's last value
- **THEN** construction returns the corresponding non-empty structural violations and no monotone model

#### Scenario: Reject an arbitrary promise
- **WHEN** a caller has only a function from lots to risk and claims that it is monotone
- **THEN** the primary API provides no unchecked way to turn that claim into a monotone lot-risk model

#### Scenario: Validate a finite observed table
- **WHEN** a complete finite table has exactly one coherent instrument-bound PnL observation for every lot from one
  through the cap and the derived risks are nondecreasing
- **THEN** checked construction may produce a monotone model while making the table's linear validation cost explicit

### Requirement: Maximum affordable lot sizing
Maximum-affordable sizing SHALL consume a `NonNegative[Quantity[settle.D]]` risk budget and a constructively validated
monotone lot-risk model. It SHALL return a closed decision with exactly two business alternatives:

- no positive lot size is affordable, retaining the assessed one-lot lower boundary; or
- the affordable lot-risk assessment with the greatest coordinate, retaining either evidence that it equals the cap or
  the assessed immediately following lot whose risk exceeds the budget.

The operation SHALL use exact typed quantity order, SHALL observe no lot size more than once, and SHALL require no more
than `2 + ceil(log2(cap))` distinct model observations. It SHALL NOT manufacture zero/fractional lots, discard the
selected risk assessment, or linearly traverse the declared range when the primary monotone model is supplied.

#### Scenario: Select an interior maximum
- **WHEN** lot `n` is affordable and lot `n + 1` is unaffordable under a monotone model
- **THEN** the decision retains the assessment for `n` and the unaffordable upper-bound assessment for `n + 1`

#### Scenario: Return no affordable lot size
- **WHEN** the assessed one-lot risk exceeds the exact budget
- **THEN** the decision is the explicit no-affordable alternative and retains the one-lot assessment

#### Scenario: Select the cap
- **WHEN** the assessed capped lot size does not exceed the budget
- **THEN** the decision selects the cap and records that the declared upper boundary was reached

#### Scenario: Size a large discrete range efficiently
- **WHEN** the cap contains hundreds, thousands, or more positive lot counts
- **THEN** maximum sizing returns the exact result within the logarithmic distinct-observation bound

#### Scenario: Preserve selected evidence
- **WHEN** maximum sizing selects lot `n`
- **THEN** the result exposes the exact lot-risk assessment used for the decision without reconstructing it from a raw
  scalar

### Requirement: Explicit exhaustive fallback
A separately named exhaustive sizing boundary SHALL support a genuinely arbitrary deterministic pure evaluator over a
finite positive lot range. It SHALL evaluate exact lot sizes in ascending order as required to find the true maximum
without monotonicity, retain the greatest affordable successful assessment, and make its `O(cap)` observation cost
explicit in API documentation.

An evaluator failure SHALL retain its typed cause and exact lot coordinate and SHALL prevent a partial affordability
decision. Evaluation SHALL stop at the first failed coordinate in ascending order, making failure choice deterministic
without evaluating irrelevant later lot sizes. The fallback SHALL NOT reinterpret failure as excessive risk,
manufacture a monotone capability, or be the default path selected implicitly by maximum-affordable sizing.

#### Scenario: Find an affordable size after a decrease
- **WHEN** an arbitrary finite risk evaluation is unaffordable at lot `n` but affordable at lot `n + 1`
- **THEN** explicit exhaustive sizing continues and returns the true greatest affordable lot if every required
  observation succeeds

#### Scenario: Surface an unknown lot evaluation
- **WHEN** exhaustive evaluation fails at a positive lot coordinate
- **THEN** sizing returns that coordinate and typed cause rather than a smaller partial decision

#### Scenario: Return the first ascending failure
- **WHEN** evaluation would fail at two different lot coordinates
- **THEN** exhaustive sizing returns the lower failed coordinate and does not evaluate the later one

#### Scenario: Keep the fallback explicit
- **WHEN** a caller has not constructed a monotone lot-risk model
- **THEN** it must deliberately select the separately named exhaustive operation and accept its linear cost

### Requirement: Monotone risk evidence is established without reflective authority

Risk construction, observation, composition, and sizing SHALL use statically callable owner-defined operations.
Successful construction SHALL establish the closed-curve or complete-table predicate that guarantees exact coherent
assessments and monotonicity; hidden constructors, reflective observers, marker identity, and caller assertions SHALL
NOT substitute for that predicate.

#### Scenario: Construct a monotone model

- **WHEN** a caller supplies a supported closed curve or complete finite table
- **THEN** checked construction establishes structure, coherence, coverage, and monotonicity before returning the model

#### Scenario: Compose risk models

- **WHEN** compatible monotone models are added, minimized, maximized, or quantized
- **THEN** the owner-defined static operation preserves instrument identity, settlement dimension, domain cap, exact
  assessment, and the applicable monotonicity law

#### Scenario: Size from a validated model

- **WHEN** maximum-affordable or exhaustive sizing evaluates a model
- **THEN** it observes exact assessments through a static domain operation and returns the same witness assessment used
  for the decision

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
