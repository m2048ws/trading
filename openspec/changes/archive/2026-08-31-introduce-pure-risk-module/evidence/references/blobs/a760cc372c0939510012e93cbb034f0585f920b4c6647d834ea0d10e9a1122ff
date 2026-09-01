## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Downside risk from net PnL
A pure risk operation SHALL consume an explicit instrument and core `Pnl` and return
`NonNegative[Quantity[settle.D]]` exactly as:

```text
max(0, -netPnl)
```

The operation SHALL validate ordinary runtime instrument identity, use the existing quantity/refinement algebra,
preserve exact rational semantics, and perform no quantization, floating-point conversion, or raw-scalar reconstruction.
`Instrument` SHALL NOT expose an owned sizing capability.

#### Scenario: Measure a losing result
- **WHEN** net PnL is exact `-17/3` in the supplied instrument's settlement dimension
- **THEN** downside risk is refined nonnegative exact `17/3`

#### Scenario: Clamp profitable risk to refined zero
- **WHEN** net PnL is zero or positive
- **THEN** downside risk is the lawful refined zero in the same settlement dimension

#### Scenario: Reject foreign PnL
- **WHEN** instrument and PnL carry different runtime `InstrumentId` values
- **THEN** risk evaluation returns a typed identity error before inspecting net PnL

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

#### Scenario: Treat execution assumptions as conditional
- **WHEN** an upstream model assumes completed maker-only, limit, or sliced execution
- **THEN** sizing evaluates the resulting immutable conditional loss model without asserting that execution will occur

#### Scenario: Exclude current-position effects
- **WHEN** an existing position could make an additional order reduce or hedge account risk
- **THEN** that problem is outside standalone monotone sizing and requires an explicit account/portfolio capability

#### Scenario: Exclude liquidation and margin
- **WHEN** no liquidation or margin model is present
- **THEN** the decision expresses only the supplied exact downside-budget rule

## REMOVED Requirements

### Requirement: Maximum discrete-lot selection
**Reason**: The old requirement made an arbitrary scenario builder the primary sizing abstraction and required behavior
that was simultaneously exhaustive, first-lot-short-circuiting, and potentially non-monotone. It conflated ordinary
isolated maximum-affordable sizing with arbitrary finite optimization.

**Migration**: Construct the new checked monotone lot-risk model and use maximum-affordable sizing. Callers with a
genuinely non-monotone finite evaluation must opt into the separately named exhaustive fallback.

### Requirement: Nonlinear fee and rounding correctness
**Reason**: The old requirement treated all fee, rounding, and scenario variation as arbitrary non-monotonicity. The
replacement requirements distinguish nonlinear-but-monotone curve operations, which support efficient primary sizing,
from genuinely non-monotone evaluation, which requires explicit exhaustive treatment.

**Migration**: Express affine, stepped, capped, minimum, and order-preserving quantized loss through the checked curve
algebra. Use the explicit exhaustive fallback only when the resulting total downside curve cannot satisfy the monotone
law.
