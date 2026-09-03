# order-scenarios Specification
## Requirements
### Requirement: Compositional immutable orders
An order SHALL retain one stable runtime `InstrumentId`, an `OrderIntent` containing side, positive lots, exact signed
position change, and position effect, an explicit activation alternative, and an explicit execution-instruction
alternative. Order types SHALL be ordinary non-owner-parameterized immutable values in the order-model artifact.
Activation SHALL distinguish immediate, fixed-trigger, and trailing-trigger forms by construction. Execution
instructions SHALL distinguish market execution from priced execution by construction.

A market execution instruction SHALL retain only mechanics meaningful to a market order, including a refined
non-resting duration, and SHALL not contain maker-only or visibility state. A priced execution instruction SHALL
contain limit or pegged pricing together with duration, liquidity constraint, and displayed, hidden, or iceberg
visibility. Shapes requiring kind fields plus optional data or not-applicable sentinels SHALL not be representable.

Pure order construction SHALL consume an explicit `Instrument`. Intent construction SHALL combine side and positive
lots into exact `PositionLots` once; later scenarios SHALL consume that retained value. Final order construction SHALL
validate cross-component conditions and ordinary runtime identity. An accumulating boundary SHALL report every
independently detectable violation in deterministic order, and a fail-fast boundary SHALL return its first violation
from the same staged rules. Familiar market, limit, stop-market, and stop-limit constructors SHALL compose the same
canonical boundary. `Instrument` SHALL NOT own an order service.

`PositionEffect.ReduceOnly` SHALL remain an instruction only. Enforcing it requires account position and belongs to a
future application/execution boundary, not the immutable order model.

#### Scenario: Construct an immediate market order
- **WHEN** a caller supplies an instrument, buy or sell side, and positive lots to the market-order constructor
- **THEN** it receives an immutable order with the same instrument identity, exact signed position change, immediate
  activation, and market execution containing no priced-only state

#### Scenario: Construct a stop-limit order compositionally
- **WHEN** a caller combines a fixed or trailing activation with limit-priced execution for one instrument
- **THEN** the resulting order retains both closed alternatives without lifecycle or scenario state

#### Scenario: Reject market maker-only mechanics
- **WHEN** a caller attempts to combine market execution with maker-only liquidity or priced-order visibility
- **THEN** no supported market-execution value can represent that combination

#### Scenario: Reject an invalid market duration
- **WHEN** an adapter attempts to refine a resting duration as a market duration
- **THEN** refinement returns an order-model error before aggregate order construction

#### Scenario: Reject oversized iceberg display
- **WHEN** a priced iceberg instruction displays more lots than its order intent contains
- **THEN** final order construction fails with a typed order diagnostic

#### Scenario: Accumulate invalid iceberg combinations together
- **WHEN** an iceberg both exceeds ordered lots and uses a non-resting priced duration
- **THEN** accumulating order construction reports both applicable violations in stable rule order

#### Scenario: Reject an accidentally foreign component
- **WHEN** construction receives an explicit instrument, lots, trigger price, limit price, or iceberg lots with a
  different runtime `InstrumentId`
- **THEN** it returns a typed order violation with a closed component location

#### Scenario: Retain reduce-only without pretending to enforce it
- **WHEN** an immutable order has reduce-only position effect but no account position is supplied
- **THEN** construction retains the instruction and makes no claim that execution would reduce exposure

#### Scenario: Keep lifecycle state absent
- **WHEN** a caller inspects an immutable order
- **THEN** it contains no venue order ID, submission status, fill quantity, cancellation state, fill record, or reported
  fee

#### Scenario: Keep order construction downstream
- **WHEN** downstream Scala depends only on instrument economics
- **THEN** order-side, time-in-force, trigger, visibility, and order-construction types are absent

### Requirement: Explicit trigger and price mechanics
Immediate, fixed-trigger, and trailing-trigger activation SHALL be direct closed alternatives rather than one record
containing a kind and optional fields. The alternatives SHALL NOT carry a generative owner type. A fixed trigger SHALL
contain its price reference, comparison direction, and positive instrument price. A trailing trigger SHALL contain its
reference, comparison direction, and strictly positive tick offset.

Each activation alternative SHALL associate the exact evidence shape it accepts. Evidence construction SHALL occur
through the corresponding activation value, derive activation-owned fields instead of accepting duplicate claims, and
return a typed semantic failure when observations do not satisfy it. Immediate activation SHALL require no observation,
fixed activation exactly one observed price, and trailing activation exactly a favorable extreme and observed price.
Unsupported evidence-shape pairings SHALL fail to compile. Evidence replayed against a different same-shape activation
SHALL be checked semantically rather than relying on JVM object identity or an issuance token.

Market, limit, and pegged pricing SHALL likewise be closed alternatives with associated resolution shapes. A limit
instruction SHALL contain one positive instrument price. A pegged instruction SHALL contain its reference and signed
tick offset until resolved from evidence built through that instruction. Direct market or limit pricing SHALL require
no peg evidence. Resolution SHALL return an explicit market-or-effective-limit alternative rather than optional ticks.
Trigger activation SHALL not imply execution price or liquidity role.

Activation and pricing instructions, their associated evidence types, and pure semantic verification SHALL belong to
the order model. Execution scenarios SHALL provide observations and consume the verified result.

#### Scenario: Inspect activation by case
- **WHEN** a caller pattern-matches an activation value
- **THEN** each case exposes exactly the fields valid for immediate, fixed, or trailing activation without optional
  fields, downcasts, or owner authority

#### Scenario: Reject a nonpositive trailing offset locally
- **WHEN** a caller supplies zero or negative trailing distance
- **THEN** trailing-trigger construction fails before order or scenario aggregation

#### Scenario: Construct fixed evidence through its activation
- **WHEN** a caller supplies an observed price through one fixed activation's evidence constructor
- **THEN** the evidence has the fixed shape and inherits reference, comparison, and trigger from that activation

#### Scenario: Prevent a fixed and trailing evidence mismatch
- **WHEN** downstream Scala attempts to supply trailing evidence to fixed activation or fixed evidence to trailing
  activation
- **THEN** the supported call does not type-check

#### Scenario: Reject inconsistent same-shape activation evidence
- **WHEN** fixed or trailing evidence built for one value is applied to a same-shape value with different semantic fields
- **THEN** verification returns a typed activation mismatch before a scenario is produced

#### Scenario: Distinguish activation from matched price
- **WHEN** a stop-market scenario activates at one observed trigger price and assumes a different later matched price
- **THEN** activation evidence and matched-slice market state remain distinct values

#### Scenario: Express take profit through comparison
- **WHEN** a sell order is intended to activate at or above a target
- **THEN** the activation records that comparison directly without a product-family or payoff flag

#### Scenario: Resolve a pegged instruction before valuation
- **WHEN** a scenario supplies reference and resolved prices for a pegged instruction
- **THEN** its associated boundary returns either a typed effective limit consistent with the offset or a pricing
  violation

#### Scenario: Reject inconsistent same-shape peg evidence
- **WHEN** peg evidence built for one instruction is applied to a same-shape instruction with a different reference or
  offset
- **THEN** verification returns a typed semantic mismatch without comparing JVM reference identity

#### Scenario: Represent market pricing explicitly
- **WHEN** market execution is prepared for slice validation
- **THEN** effective pricing is the explicit market alternative rather than missing optional ticks

### Requirement: LiquidityRole describes matched quantity
`LiquidityRole` SHALL be owned by the execution-scenario artifact and contain maker and taker classifications for
matched quantity, not order intent. An order SHALL store only a liquidity constraint; each positive matched slice SHALL
store its assumed role. Universal scenario validation SHALL enforce that market slices are taker and maker-only order
slices are maker. An unrestricted priced order MAY have maker slices, taker slices, or both. Venue fee policy MAY apply
further classification rules without changing either artifact.

#### Scenario: Model an all-taker market outcome
- **WHEN** a complete market-order scenario contains matched quantity
- **THEN** every matched slice is classified as taker

#### Scenario: Model a mixed limit outcome
- **WHEN** an unrestricted priced order is assumed to cross for part of its lots and rest for the remainder
- **THEN** its scenario may contain taker and maker slices whose exact lots sum to the order lots

#### Scenario: Enforce maker-only conditionally on a fill
- **WHEN** a maker-only order has a complete hypothetical scenario
- **THEN** every slice is maker while neither the order nor scenario claims the order will actually fill

#### Scenario: Keep liquidity out of the order instruction
- **WHEN** two scenarios interpret the same unrestricted order with different maker/taker allocations
- **THEN** the order is unchanged and the order-model artifact has no `LiquidityRole` type

### Requirement: Complete checked order scenarios
A complete scenario input SHALL own exactly one immutable order together with activation evidence and pricing resolution
whose types are associated with that order's alternatives, plus a domain-named non-empty `MatchedSlices` value. It
SHALL NOT store one target order and then require callers to pass a second order for reconciliation. Missing, extraneous,
fixed-versus-trailing, and direct-versus-pegged evidence pairings SHALL not be representable through the supported
construction path.

Each liquidity slice SHALL carry positive lots, a coherent market state representing its assumed matched price and
conversions, and a liquidity role. Slice construction and complete scenario evaluation SHALL consume an explicit
`Instrument`. Evaluation SHALL validate runtime instrument identity, exact lot conservation, semantic activation,
pricing evidence, effective limit quality, and liquidity-role compatibility without a parallel scalar projection.
Successful output SHALL retain the one input order, its assumptions, effective pricing, matched slices, and the signed
position change already established by order intent.

Validation SHALL use boundary-owned `ScenarioViolation` values with closed semantic locations. An accumulating boundary
SHALL return independently detectable violations in deterministic stage/rule/slice order. Checks depending on valid
identity, activation, pricing, or a non-empty collection SHALL run only after that evidence exists. A fail-fast boundary
SHALL be the head projection of the same result, not a parallel validation implementation.

#### Scenario: Accept multiple assumed prices
- **WHEN** one order is modeled as positive slices at several grid-valid market states for the same instrument
- **THEN** the scenario retains every slice and their exact lot total

#### Scenario: Accept one slice without a collection wrapper
- **WHEN** a caller supplies one head slice and zero or more tail slices
- **THEN** it obtains a domain `MatchedSlices` value whose public vector is non-empty by construction

#### Scenario: Prevent an empty slice collection
- **WHEN** an adapter attempts to reconstruct matched slices from an empty vector
- **THEN** reconstruction returns a typed empty-slices violation

#### Scenario: Eliminate duplicated order reconciliation
- **WHEN** a caller constructs assumptions for an order and evaluates them
- **THEN** evaluation uses the order owned by those assumptions and exposes no second order parameter or target-reference
  mismatch error

#### Scenario: Reject missing trigger evidence
- **WHEN** downstream Scala attempts to construct assumptions for a triggered activation without its associated trigger
  evidence
- **THEN** the supported associated-evidence construction call does not type-check for that invalid pairing

#### Scenario: Reject extraneous trigger evidence
- **WHEN** downstream Scala attempts to construct assumptions for immediate activation with fixed or trailing trigger
  evidence
- **THEN** the supported associated-evidence construction call does not type-check for that invalid pairing

#### Scenario: Reject a buy above its limit
- **WHEN** a buy-limit scenario contains a matched slice above its effective limit
- **THEN** evaluation reports the offending slice with a typed location

#### Scenario: Reject an unsatisfied trigger semantically
- **WHEN** correctly shaped fixed or trailing evidence does not satisfy its instruction
- **THEN** evaluation returns the retained activation cause before producing an order scenario

#### Scenario: Reject mismatched peg resolution
- **WHEN** correctly shaped peg evidence does not satisfy its instruction's reference and offset
- **THEN** evaluation returns the retained pricing cause before slice price-quality checks

#### Scenario: Reject inconsistent same-shape peg resolution replay
- **WHEN** a resolution constructed for one pegged instruction is supplied to a same-shape instruction with a different
  reference or offset
- **THEN** pricing resolution returns a typed semantic mismatch before an `OrderScenario` is produced

#### Scenario: Reject an incomplete lot allocation
- **WHEN** positive slice lots do not sum exactly to the order lots
- **THEN** evaluation fails rather than treating the remainder as live lifecycle state

#### Scenario: Reject a foreign scenario value
- **WHEN** the instrument, order components, evidence prices, slice lots, or market states carry different runtime
  `InstrumentId` values
- **THEN** evaluation reports typed component locations rather than free-form path strings

#### Scenario: Accumulate independent slice violations
- **WHEN** several slices independently violate liquidity-role or effective-limit rules
- **THEN** accumulating evaluation reports every applicable indexed violation in stable slice and rule order

#### Scenario: Preserve deterministic fail-fast scenario construction
- **WHEN** the same invalid input is supplied to accumulating and fail-fast boundaries
- **THEN** fail-fast returns the first accumulated violation reachable through the shared stages

#### Scenario: Avoid a parallel validation model
- **WHEN** evaluation validates activation, pricing, lots, liquidity, and instrument coherence
- **THEN** the algebraic alternatives and associated evidence supply the cases directly, with no kind-plus-option view,
  duplicated target, or owner-authority layer

#### Scenario: Keep epistemic status explicit
- **WHEN** a caller inspects a complete order scenario
- **THEN** it is an immutable hypothetical outcome and contains no claim of venue submission, actual fill, or execution
  provenance

#### Scenario: Keep scenario evaluation downstream
- **WHEN** downstream Scala depends only on instrument economics
- **THEN** activation evidence, liquidity slices, scenario assumptions, and scenario-validation policy are absent

### Requirement: Checked round-trip scenarios
A round-trip scenario SHALL contain complete entry and exit order scenarios for one explicit instrument and SHALL
require their retained signed position changes to combine exactly to flat. Construction SHALL use the checked
same-instrument position operation inherited from instrument economics, preserve each leg's assumptions and slices, and
retain the entry position as the held position. It SHALL NOT infer a closing side, resize a leg, recompute side signs,
or combine different runtime instrument identities.

#### Scenario: Construct a long round trip
- **WHEN** a buy entry and equal-lot sell exit carry the supplied instrument identity
- **THEN** construction returns a round trip whose held position is the entry's positive position change

#### Scenario: Construct a short round trip
- **WHEN** a sell entry and equal-lot buy exit carry the supplied instrument identity
- **THEN** construction returns a round trip whose held position is the entry's negative position change

#### Scenario: Reject unequal closing lots
- **WHEN** entry and exit position changes do not combine to exact flat
- **THEN** construction returns a typed round-trip error preserving both signed coordinates

#### Scenario: Reject cross-instrument legs
- **WHEN** the supplied instrument, entry, and exit do not carry one runtime `InstrumentId`
- **THEN** construction returns a typed scenario identity violation

#### Scenario: Avoid recomputing signed direction
- **WHEN** a valid round trip is constructed
- **THEN** it uses the position changes retained by order intent rather than multiplying side and lot coordinates again

### Requirement: Order and execution scenario artifact boundary
Immutable order instructions SHALL be delivered by a pure order-model artifact that depends on instrument economics.
Hypothetical matched outcomes SHALL be delivered by a separate pure execution-scenario artifact that depends on the
order model and instrument economics. The order-model artifact SHALL NOT contain market states, liquidity outcomes,
scenario validation, fees, risk, submission, or execution lifecycle; the execution-scenario artifact SHALL NOT own or
mutate instrument or order definitions.

Both artifacts SHALL be independently usable without a live catalog, effect type, clock, market-data service,
persistence store, stream, transaction, tracing, or metrics interpreter.

#### Scenario: Compile the order model alone
- **WHEN** downstream Scala depends on the order-model artifact and instrument economics
- **THEN** it can construct immutable orders but cannot construct liquidity slices or complete execution scenarios

#### Scenario: Add scenario interpretation downstream
- **WHEN** downstream Scala additionally depends on the execution-scenario artifact
- **THEN** it can evaluate an immutable order under explicit hypothetical evidence without changing that order

#### Scenario: Keep live execution separate
- **WHEN** a caller inspects either artifact
- **THEN** it finds no venue connection, submission command, order status, fill stream, cancellation workflow, or
  transaction interpreter

### Requirement: Exact round-trip scenario price normalization
The execution-scenario artifact SHALL expose pure price normalization for a complete round-trip scenario and explicit
instrument. For every matched slice, it SHALL derive the slice's signed position change from the order intent's side and
that slice's positive lots through the typed position boundary, value that change at the slice's own market state, and
negate the value as the exact trade cashflow. The exact sum of entry and exit slice cashflows SHALL be returned as one
`PricePnl` in the instrument settlement dimension.

Normalization SHALL preserve typed `PositionLots`, `Rate`, and `Quantity` arithmetic; retain ordinary runtime
instrument coherence; weight every slice by its exact lots; and perform no fee evaluation, quantization, live lookup, or
raw-scalar reconstruction. The execution scenario SHALL own a closed entry/exit leg value for downstream attribution.

#### Scenario: Normalize one slice per leg
- **WHEN** a round trip contains one entry slice and one exit slice
- **THEN** scenario price PnL exactly equals universal exit position value minus entry position value for the held
  position

#### Scenario: Weight several matched prices
- **WHEN** one leg contains slices with different lots and market states
- **THEN** price PnL is the exact sum of each slice's typed signed cashflow rather than an unweighted average price

#### Scenario: Normalize a short round trip
- **WHEN** a sell entry and buy exit close exactly to flat
- **THEN** each slice uses the side-directed signed position change and the resulting PnL is symmetric with the
  corresponding long scenario

#### Scenario: Preserve off-grid exactness
- **WHEN** inverse or third-asset settlement yields a result outside a storage grid
- **THEN** scenario price PnL retains the exact settlement quantity without quantization

#### Scenario: Reject a foreign round trip
- **WHEN** the explicit instrument and round-trip scenario carry different runtime instrument identities
- **THEN** normalization returns a typed scenario-valuation mismatch without performing fee policy

#### Scenario: Keep fee policy out of scenario normalization
- **WHEN** downstream Scala depends on execution scenarios without fee policy
- **THEN** it can calculate exact round-trip `PricePnl` but cannot assess trading fees or construct fee-inclusive PnL

### Requirement: Scenario authority follows from checked semantics

Order and scenario construction SHALL use statically callable domain operations. Constructor secrecy, reflective
issuance, and JVM object identity SHALL NOT establish activation evidence, peg resolution, matched-slice non-emptiness,
scenario validity, or round-trip validity. Before returning a stronger result, the owning operation SHALL establish the
associated evidence shape and values, instrument and order identity, price/grid relationships, non-empty structure,
and exact flatness predicates applicable to that result.

#### Scenario: Replay trigger evidence

- **WHEN** fixed or trailing evidence is replayed against an activation
- **THEN** verification checks both instruction/evidence agreement and trigger satisfaction before a scenario returns

#### Scenario: Rebuild a complete scenario

- **WHEN** supplied assumptions and slices are reconstructed from external data
- **THEN** checked construction rejects associated-shape, identity, grid, pricing, or empty-slice failures without
  relying on the difficulty of instantiating an intermediate representation

#### Scenario: Build a round trip

- **WHEN** entry and exit scenarios are combined
- **THEN** checked construction verifies shared instrument identity and exact flat signed position before returning the
  round trip

## Purpose
Defines immutable, compositional order instructions and complete hypothetical order outcomes whose price and maker/taker assumptions can drive fees and PnL without introducing execution lifecycle state.
