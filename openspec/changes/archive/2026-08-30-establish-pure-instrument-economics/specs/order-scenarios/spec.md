## MODIFIED Requirements

### Requirement: Compositional immutable orders
An order SHALL retain one stable runtime `InstrumentId`, an `OrderIntent` containing side, positive lots, and position
effect, an explicit activation alternative, and an explicit execution-instruction alternative. The order and its
components SHALL use ordinary non-owner-parameterized domain types. Activation SHALL distinguish immediate,
fixed-trigger, and trailing-trigger forms by construction. Execution instructions SHALL distinguish market execution
from priced execution by construction.

A market execution instruction SHALL retain only mechanics meaningful to a market order, including a valid non-resting
duration, and SHALL not contain maker-only or visibility state. A priced execution instruction SHALL contain a limit or
pegged price instruction together with duration, liquidity constraint, and displayed, hidden, or iceberg visibility.
Shapes previously represented by `kind` values plus optional fields or by not-applicable sentinels SHALL not be
representable as valid domain values.

A pure order-construction boundary SHALL consume an explicit `Instrument`; construct side-directed position change from
that instrument's positive `Lots`; construct components that require numeric refinement; validate cross-component
conditions and ordinary runtime instrument identity; and construct an immutable `Order` from cohesive intent,
activation, and execution inputs. `Instrument` SHALL NOT expose this boundary as an owned capability. Familiar market,
limit, stop-market, and stop-limit entry points SHALL remain available. Venue adapters MAY impose additional
venue-specific restrictions without weakening core construction.

#### Scenario: Construct an immediate market order
- **WHEN** a caller supplies an instrument and buy or sell intent with positive lots to the market-order constructor
- **THEN** it receives an immutable order with that runtime instrument identity, the corresponding signed position
  change, immediate activation, and a market execution instruction containing no priced-only state

#### Scenario: Construct a stop-limit order compositionally
- **WHEN** a caller combines an instrument, fixed or trailing activation, and limit-priced execution instruction carrying
  the same runtime identity
- **THEN** the resulting order retains the explicit alternatives without unrelated lifecycle state

#### Scenario: Reject market maker-only mechanics
- **WHEN** a caller attempts to combine a market execution instruction with maker-only liquidity or priced-order
  visibility
- **THEN** no valid market instruction can represent that combination because maker-only and visibility state are
  structurally absent

#### Scenario: Reject an invalid market duration
- **WHEN** an adapter attempts to construct market execution with a resting duration
- **THEN** the market-instruction smart constructor returns a typed failure

#### Scenario: Reject oversized iceberg display
- **WHEN** a priced iceberg instruction displays more lots than its order intent contains
- **THEN** final order construction fails with a typed order diagnostic

#### Scenario: Reject an accidentally foreign component
- **WHEN** final order construction receives an instrument, lots, trigger price, limit price, or iceberg display lots
  carrying different runtime `InstrumentId` values
- **THEN** construction returns a typed instrument-mismatch failure

#### Scenario: Keep lifecycle state absent
- **WHEN** a caller inspects an immutable order
- **THEN** it contains no venue order ID, submission status, fill quantity, cancellation state, fill record, or reported
  fee

#### Scenario: Keep order construction downstream
- **WHEN** downstream Scala depends only on instrument economics
- **THEN** order-side, time-in-force, trigger, visibility, and order-construction types are absent

### Requirement: Complete checked order scenarios
A complete order scenario SHALL bind one immutable order and one cohesive `ScenarioAssumptions` value containing
activation status, pricing resolution, and a non-empty immutable vector of positive liquidity slices. These types SHALL
be ordinary non-owner-parameterized domain types that retain their runtime `InstrumentId` where needed for aggregate
coherence. Supported assumption construction SHALL consume activation evidence and pricing resolution associated with
the corresponding order shapes, so missing, extraneous, fixed-versus-trailing, and direct-versus-pegged combinations
are not representable through that construction path. An adapter starting from untyped or serialized input SHALL
establish those associated values through the same typed validation boundaries before constructing assumptions.

Each liquidity slice SHALL carry positive lots, a coherent market state representing its assumed matched price and
conversions, and a liquidity role. A pure scenario-construction boundary SHALL consume an explicit `Instrument`, order,
and cohesive assumptions; validate aggregate invariants; and return a valid `OrderScenario`. `Instrument` SHALL NOT
expose this boundary as an owned capability. Construction SHALL validate runtime instrument identity, exact lot
conservation, semantic activation satisfaction, peg consistency, limit-price quality, and liquidity-role compatibility
without projecting the order into a parallel scalar `View` model.

The boundary SHALL offer an accumulating diagnostic path that returns every independently detectable scenario violation
in deterministic order within each validation stage. A stage whose checks depend on successfully resolved activation,
pricing, identity, or collection evidence SHALL run only after that prerequisite succeeds. A fail-fast construction
path SHALL remain available and SHALL return the first corresponding scenario error from the same ordered rules.

#### Scenario: Accept multiple assumed prices
- **WHEN** a complete order is modeled as positive slices at several grid-valid prices for the same instrument
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
- **WHEN** downstream Scala attempts to construct assumptions for a triggered activation without its associated trigger
  evidence
- **THEN** the supported associated-evidence construction call does not type-check for that invalid pairing

#### Scenario: Reject extraneous trigger evidence
- **WHEN** downstream Scala attempts to construct assumptions for immediate activation with fixed or trailing trigger
  evidence
- **THEN** the supported associated-evidence construction call does not type-check for that invalid pairing

#### Scenario: Reject an unsatisfied trigger semantically
- **WHEN** correctly shaped fixed or trailing evidence does not satisfy its activation comparison and threshold
- **THEN** the activation evidence boundary returns a typed semantic failure

#### Scenario: Reject mismatched peg resolution
- **WHEN** a pegged instruction's proposed resolution disagrees with its reference or tick offset
- **THEN** the typed associated-resolution boundary fails before producing effective pricing

#### Scenario: Reject inconsistent same-shape peg resolution replay
- **WHEN** a resolution constructed for one pegged instruction is supplied to a same-shape instruction with a different
  reference or offset
- **THEN** pricing resolution returns a typed semantic mismatch before an `OrderScenario` is produced

#### Scenario: Reject an incomplete lot allocation
- **WHEN** positive slice lots do not sum exactly to the order lots
- **THEN** complete-scenario construction fails rather than treating the remainder as lifecycle state

#### Scenario: Reject a foreign scenario value
- **WHEN** the instrument, order, activation price, peg resolution, slice lots, or market state carries a different
  runtime `InstrumentId`
- **THEN** complete-scenario construction returns a typed instrument-mismatch failure

#### Scenario: Accumulate independent slice violations
- **WHEN** several slices independently violate liquidity-role or effective-limit rules
- **THEN** the accumulating diagnostic boundary reports every applicable indexed violation in stable slice order

#### Scenario: Preserve deterministic fail-fast scenario construction
- **WHEN** the same invalid scenario request is supplied to the accumulating and fail-fast boundaries
- **THEN** the fail-fast boundary returns the error corresponding to the first accumulated violation reachable through
  the shared validation stages

#### Scenario: Avoid a parallel validation model
- **WHEN** scenario construction validates activation, pricing, lots, liquidity, and runtime instrument coherence
- **THEN** the domain alternatives and their associated evidence provide the data and cases being validated, with no
  duplicate kind-plus-option projection or owner-authority implementation layer

#### Scenario: Keep scenario evaluation downstream
- **WHEN** downstream Scala depends only on instrument economics
- **THEN** activation evidence, liquidity slices, scenario assumptions, and scenario-validation policy are absent

### Requirement: Checked round-trip scenarios
A round-trip trade scenario SHALL contain complete entry and exit order scenarios carrying the same stable runtime
`InstrumentId` and SHALL require their signed position changes to sum exactly to flat. A pure round-trip constructor
SHALL consume an explicit `Instrument` and preserve each leg's activation and liquidity slices. `Instrument` SHALL NOT
expose that constructor as an owned capability. Construction SHALL NOT infer a closing side, silently resize a leg, or
combine values carrying different runtime instrument identities.

#### Scenario: Construct a long round trip
- **WHEN** a buy entry and equal-lot sell exit carry the supplied instrument identity
- **THEN** round-trip construction returns a value whose held position is the entry's positive signed position change

#### Scenario: Construct a short round trip
- **WHEN** a sell entry and equal-lot buy exit carry the supplied instrument identity
- **THEN** round-trip construction returns a value whose held position is the entry's negative signed position change

#### Scenario: Reject unequal closing lots
- **WHEN** entry and exit position changes do not sum exactly to flat
- **THEN** round-trip construction fails

#### Scenario: Reject cross-instrument legs
- **WHEN** the supplied instrument, entry scenario, and exit scenario do not carry one runtime `InstrumentId`
- **THEN** round-trip construction returns a typed instrument-mismatch failure
