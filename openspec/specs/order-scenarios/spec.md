# order-scenarios Specification
## Requirements
### Requirement: Compositional immutable orders
An order SHALL retain one stable runtime `InstrumentId`, an `OrderIntent` containing side, positive lots, and position effect, an explicit activation alternative, and an explicit execution-instruction alternative. The order and its components SHALL use ordinary non-owner-parameterized domain types. Activation SHALL distinguish immediate, fixed-trigger, and trailing-trigger forms by construction. Execution instructions SHALL distinguish market execution from priced execution by construction.

A market execution instruction SHALL retain only mechanics meaningful to a market order, including a valid non-resting duration, and SHALL not contain maker-only or visibility state. A priced execution instruction SHALL contain a limit or pegged price instruction together with duration, liquidity constraint, and displayed, hidden, or iceberg visibility. Shapes that were previously represented by `kind` values plus optional fields or by sentinel values such as not-applicable visibility SHALL not be representable as valid domain values.

The instrument's `orders` capability SHALL construct components that need numeric refinement, validate cross-component conditions and runtime instrument identity that cannot be expressed locally, and construct an immutable `Order` from cohesive intent, activation, and execution inputs. Familiar market, limit, stop-market, and stop-limit entry points SHALL remain available. Venue adapters MAY impose additional venue-specific restrictions without weakening core construction.

#### Scenario: Construct an immediate market order
- **WHEN** a caller supplies buy or sell intent with positive lots to the market-order constructor
- **THEN** it receives an immutable order with that runtime instrument identity, immediate activation, and a market execution instruction containing no priced-only state

#### Scenario: Construct a stop-limit order compositionally
- **WHEN** a caller combines a fixed or trailing activation with a limit-priced execution instruction for the same runtime instrument identity
- **THEN** the resulting order retains both explicit alternatives without unrelated lifecycle state

#### Scenario: Reject market maker-only mechanics
- **WHEN** a caller attempts to combine a market execution instruction with maker-only liquidity or priced-order visibility
- **THEN** no valid market instruction can represent that combination because maker-only and visibility state are structurally absent

#### Scenario: Reject an invalid market duration
- **WHEN** an adapter attempts to construct market execution with a resting duration
- **THEN** the market-instruction smart constructor returns a typed failure

#### Scenario: Reject oversized iceberg display
- **WHEN** a priced iceberg instruction displays more lots than its order intent contains
- **THEN** final order construction fails with a typed order diagnostic

#### Scenario: Reject an accidentally foreign component
- **WHEN** final order construction receives lots, a trigger price, a limit price, or iceberg display lots carrying a different runtime `InstrumentId`
- **THEN** construction returns a typed instrument-mismatch failure

#### Scenario: Keep lifecycle state absent
- **WHEN** a caller inspects an immutable order
- **THEN** it contains no venue order ID, submission status, fill quantity, cancellation state, fill record, or reported fee

### Requirement: Explicit trigger and price mechanics
Immediate, fixed-trigger, and trailing-trigger activation SHALL be direct closed alternatives rather than one record containing a kind and optional fields. The alternatives SHALL NOT carry a generative owner type or require owner authority to construct. A fixed trigger SHALL contain its observed reference, comparison direction, and positive instrument price. A trailing trigger SHALL contain its reference, comparison direction, and a strictly positive tick offset. It SHALL be impossible to obtain a valid fixed trigger without its price or a valid trailing trigger without its offset through the supported smart-construction API.

Each activation alternative SHALL define the exact observation or evidence shape it accepts. Supported evidence construction SHALL occur through the corresponding activation value, derive activation-owned fields such as the price reference rather than accepting duplicate caller claims, and return a typed semantic failure when the observed trigger is unsatisfied. Immediate activation SHALL accept no trigger observation, fixed activation SHALL accept exactly one observed price, and trailing activation SHALL accept exactly a favorable extreme and an observed price. Supported construction SHALL NOT produce an immediate/fixed, immediate/trailing, fixed/trailing, or trailing/fixed evidence pairing.

Market, limit, and pegged pricing SHALL likewise be explicit alternatives with an associated resolution shape. A limit instruction SHALL contain one positive instrument price. A pegged instruction SHALL contain its reference and signed tick offset until scenario or venue resolution. Direct market or limit pricing SHALL require no peg resolution; pegged pricing SHALL resolve only from evidence constructed for that pegged instruction. Resolution SHALL return an explicit effective-pricing alternative distinguishing market execution from a typed effective limit rather than using presence or absence of a tick value. Stop-loss and take-profit names SHALL remain convenience interpretations of explicit comparison directions. Trigger activation SHALL not itself imply an execution price or liquidity role.

#### Scenario: Inspect activation by case
- **WHEN** a caller pattern-matches an activation value
- **THEN** each case exposes exactly the fields valid for immediate, fixed-trigger, or trailing-trigger activation without `Option.get`, a private implementation downcast, or an owner authority

#### Scenario: Reject a nonpositive trailing offset locally
- **WHEN** a caller supplies zero or negative trailing distance to the supported constructor
- **THEN** trailing-trigger construction fails before order or scenario aggregation

#### Scenario: Construct fixed evidence through its activation
- **WHEN** a caller observes a fixed activation through that activation's supported evidence constructor
- **THEN** the evidence has the fixed shape and inherits the activation's reference without a second caller-supplied reference

#### Scenario: Prevent a fixed and trailing evidence mismatch
- **WHEN** downstream Scala attempts to supply trailing evidence to a fixed activation or fixed evidence to a trailing activation
- **THEN** the supported call does not type-check rather than reaching complete-scenario validation

#### Scenario: Reject inconsistent same-shape activation evidence
- **WHEN** fixed or trailing evidence constructed for one activation value is supplied to a same-shape activation with a different reference, comparison, trigger price, or trailing offset
- **THEN** activation validation returns a typed semantic mismatch before an `OrderScenario` is produced

#### Scenario: Distinguish activation from matched price
- **WHEN** a stop-market scenario activates at a mark-price trigger and assumes a different later matched price
- **THEN** the activation observation and matched-price market state remain distinct inputs

#### Scenario: Express take profit through comparison
- **WHEN** a sell order is intended to activate at or above a target price
- **THEN** the activation alternative records that comparison directly without a separate payoff formula

#### Scenario: Resolve a pegged instruction before valuation
- **WHEN** a pegged order is evaluated in a complete scenario
- **THEN** its associated resolution boundary either returns a typed limited effective price consistent with the instruction's reference and offset or returns a semantic pricing failure

#### Scenario: Represent market pricing explicitly
- **WHEN** a market execution is prepared for slice validation
- **THEN** its effective pricing is the explicit market alternative rather than an empty optional tick value

### Requirement: LiquidityRole describes matched quantity
`LiquidityRole` SHALL contain maker and taker classifications and SHALL describe the fee treatment of matched quantity, not the instruction stored by an order. An order SHALL store a liquidity constraint, while a complete order scenario SHALL store the assumed role of each positive matched slice. Core validation SHALL enforce universal implications: a market slice is taker; a maker-only order has only maker slices; and an unrestricted limit order MAY contain maker slices, taker slices, or both. Venue fee schedules MAY refine classification for mechanics such as hidden quantity.

#### Scenario: Model an all-taker market outcome
- **WHEN** a complete market-order scenario contains matched quantity
- **THEN** every liquidity slice is classified as taker

#### Scenario: Model a mixed limit outcome
- **WHEN** an unrestricted limit order is assumed to cross for part of its lots and later rest for the remainder
- **THEN** its scenario may contain a taker slice and a maker slice whose lots sum to the order lots

#### Scenario: Enforce maker-only conditionally on a fill
- **WHEN** a maker-only order has a complete filled scenario
- **THEN** every slice is maker, while the order mechanics themselves make no guarantee that any fill occurs

#### Scenario: Keep liquidity out of the order instruction
- **WHEN** two scenarios evaluate the same unrestricted limit order under different maker/taker allocations
- **THEN** the order value remains unchanged and only the scenario outcomes differ

### Requirement: Complete checked order scenarios
A complete order scenario SHALL bind one immutable order and one cohesive `ScenarioAssumptions` value containing activation status, pricing resolution, and a non-empty immutable vector of positive liquidity slices. These types SHALL be ordinary non-owner-parameterized domain types that retain their runtime `InstrumentId` where needed for aggregate coherence. Supported assumption construction SHALL consume activation evidence and pricing resolution associated with the corresponding order shapes, so missing, extraneous, fixed-versus-trailing, and direct-versus-pegged combinations are not representable through that construction path. An adapter starting from untyped or serialized input SHALL establish those associated values through the same typed validation boundaries before constructing assumptions.

Each liquidity slice SHALL carry positive lots, a coherent market state representing its assumed matched price and conversions, and a liquidity role. The instrument's `scenarios` capability SHALL accept the order and cohesive assumptions, validate aggregate invariants, and return a valid `OrderScenario`. It SHALL validate runtime instrument identity, exact lot conservation, semantic activation satisfaction, peg consistency, limit-price quality, and liquidity-role compatibility without projecting the order into a parallel scalar `View` model.

The capability SHALL offer an accumulating diagnostic boundary that returns every independently detectable scenario violation in deterministic order within each validation stage. A stage whose checks depend on a successfully resolved activation, pricing, identity, or collection value SHALL run only after that prerequisite succeeds. The existing fail-fast scenario-construction boundary SHALL remain available and SHALL return the first corresponding `EconomicsError` from the same ordered rules.

#### Scenario: Accept multiple assumed prices
- **WHEN** a complete order is modeled as positive slices at several grid-valid prices for the same runtime instrument identity
- **THEN** the scenario retains every slice and their exact lot total

#### Scenario: Accept one slice without a collection wrapper
- **WHEN** a complete order has one assumed matched slice
- **THEN** a one-element non-empty vector in the scenario assumptions is accepted without a separate emptiness check

#### Scenario: Prevent an empty slice collection
- **WHEN** downstream Scala attempts to construct supported scenario assumptions with no matched slice
- **THEN** it cannot produce the required non-empty slice collection

#### Scenario: Reject a buy above its limit
- **WHEN** a buy-limit scenario includes a matched slice above its effective limit
- **THEN** scenario construction fails with the offending slice identified

#### Scenario: Reject missing trigger evidence
- **WHEN** downstream Scala attempts to construct assumptions for a triggered activation without its associated trigger evidence
- **THEN** the supported associated-evidence construction call does not type-check for that invalid pairing

#### Scenario: Reject extraneous trigger evidence
- **WHEN** downstream Scala attempts to construct assumptions for immediate activation with fixed or trailing trigger evidence
- **THEN** the supported associated-evidence construction call does not type-check for that invalid pairing

#### Scenario: Reject an unsatisfied trigger semantically
- **WHEN** correctly shaped fixed or trailing evidence does not satisfy its activation comparison and threshold
- **THEN** the activation evidence boundary returns a typed semantic failure

#### Scenario: Reject mismatched peg resolution
- **WHEN** a pegged instruction's proposed resolution disagrees with its reference or tick offset
- **THEN** the typed associated-resolution boundary fails before producing effective pricing

#### Scenario: Reject inconsistent same-shape peg resolution replay
- **WHEN** a resolution constructed for one pegged instruction is supplied to a same-shape instruction with a different reference or offset
- **THEN** pricing resolution returns a typed semantic mismatch before an `OrderScenario` is produced

#### Scenario: Reject an incomplete lot allocation
- **WHEN** positive slice lots do not sum exactly to the order lots
- **THEN** complete-scenario construction fails rather than treating the remainder as lifecycle state

#### Scenario: Reject a foreign scenario value
- **WHEN** an order, activation price, peg resolution, slice lots, or slice market state carries a different runtime `InstrumentId`
- **THEN** complete-scenario construction returns a typed instrument-mismatch failure

#### Scenario: Accumulate independent slice violations
- **WHEN** several slices independently violate liquidity-role or effective-limit rules
- **THEN** the accumulating diagnostic boundary reports every applicable indexed violation in stable slice order

#### Scenario: Preserve deterministic fail-fast scenario construction
- **WHEN** the same invalid scenario request is supplied to the accumulating and fail-fast boundaries
- **THEN** the fail-fast boundary returns the error corresponding to the first accumulated violation reachable through the shared validation stages

#### Scenario: Avoid a parallel validation model
- **WHEN** scenario construction validates activation, pricing, lots, liquidity, and runtime instrument coherence
- **THEN** the domain alternatives and their associated evidence provide the data and cases being validated, with no duplicate kind-plus-option projection or owner-authority implementation layer

### Requirement: Checked round-trip scenarios
A round-trip trade scenario SHALL contain complete entry and exit order scenarios carrying the same stable runtime `InstrumentId` and SHALL require their signed position changes to sum exactly to flat. The instrument's `scenarios` capability SHALL perform this checked construction and preserve each leg's activation and liquidity slices. It SHALL NOT infer a closing side, silently resize a leg, or combine values carrying different runtime instrument identities.

#### Scenario: Construct a long round trip
- **WHEN** a buy entry and equal-lot sell exit carry the target instrument's identity
- **THEN** `scenarios` constructs a round trip whose held position is the entry's positive signed position change

#### Scenario: Construct a short round trip
- **WHEN** a sell entry and equal-lot buy exit carry the target instrument's identity
- **THEN** `scenarios` constructs a round trip whose held position is the entry's negative signed position change

#### Scenario: Reject unequal closing lots
- **WHEN** entry and exit position changes do not sum exactly to flat
- **THEN** round-trip construction fails

#### Scenario: Reject cross-instrument legs
- **WHEN** entry and exit scenarios carry different runtime `InstrumentId` values
- **THEN** round-trip construction returns a typed instrument-mismatch failure

## Purpose
Defines immutable, compositional order instructions and complete hypothetical order outcomes whose price and maker/taker assumptions can drive fees and PnL without introducing execution lifecycle state.
