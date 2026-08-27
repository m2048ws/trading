## MODIFIED Requirements

### Requirement: Compositional immutable orders
An order SHALL bind one exact instrument owner, an `OrderIntent` containing side, positive instrument-owned lots, and position effect, an explicit activation alternative, and an explicit execution-instruction alternative. Activation SHALL distinguish immediate, fixed-trigger, and trailing-trigger forms by construction. Execution instructions SHALL distinguish market execution from priced execution by construction.

A market execution instruction SHALL retain only mechanics meaningful to a market order, including a valid non-resting duration, and SHALL not contain maker-only or visibility state. A priced execution instruction SHALL contain a limit or pegged price instruction together with duration, liquidity constraint, and displayed, hidden, or iceberg visibility. Shapes that were previously represented by `kind` values plus optional fields or by sentinel values such as not-applicable visibility SHALL not be representable as valid domain values.

The instrument-owned `orders` capability SHALL construct the component alternatives, validate cross-component conditions that cannot be expressed locally, and construct an immutable `Order` from cohesive intent, activation, and execution inputs. Familiar market, limit, stop-market, and stop-limit entry points SHALL remain available. Venue adapters MAY impose additional venue-specific restrictions without weakening core construction.

#### Scenario: Construct an immediate market order
- **WHEN** a caller supplies buy or sell intent with positive lots to the market-order constructor
- **THEN** it receives an immutable order with immediate activation and a market execution instruction containing no priced-only state

#### Scenario: Construct a stop-limit order compositionally
- **WHEN** a caller combines a fixed or trailing activation with a limit-priced execution instruction
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

#### Scenario: Keep lifecycle state absent
- **WHEN** a caller inspects an immutable order
- **THEN** it contains no venue order ID, submission status, fill quantity, cancellation state, fill record, or reported fee

### Requirement: Explicit trigger and price mechanics
Immediate, fixed-trigger, and trailing-trigger activation SHALL be closed owner-aware alternatives rather than one record containing a kind and optional fields. A fixed trigger SHALL contain its observed reference, comparison direction, and positive instrument price. A trailing trigger SHALL contain its reference, comparison direction, and a strictly positive tick offset. It SHALL be impossible to obtain a valid fixed trigger without its price or a valid trailing trigger without its offset.

Market, limit, and pegged pricing SHALL likewise be explicit alternatives. A limit instruction SHALL contain one positive instrument price. A pegged instruction SHALL contain its reference and signed tick offset until scenario or venue resolution. Stop-loss and take-profit names SHALL remain convenience interpretations of explicit comparison directions. Trigger activation SHALL not itself imply an execution price or liquidity role.

#### Scenario: Inspect activation by case
- **WHEN** a caller pattern-matches or folds an activation value
- **THEN** each case exposes exactly the fields valid for immediate, fixed-trigger, or trailing-trigger activation without `Option.get`

#### Scenario: Reject a nonpositive trailing offset locally
- **WHEN** a caller supplies zero or negative trailing distance
- **THEN** trailing-trigger construction fails before order or scenario aggregation

#### Scenario: Distinguish activation from matched price
- **WHEN** a stop-market scenario activates at a mark-price trigger and assumes a different later matched price
- **THEN** the activation observation and matched-price market state remain distinct inputs

#### Scenario: Express take profit through comparison
- **WHEN** a sell order is intended to activate at or above a target price
- **THEN** the activation alternative records that comparison directly without a separate payoff formula

#### Scenario: Resolve a pegged instruction before valuation
- **WHEN** a pegged order is evaluated in a complete scenario
- **THEN** the scenario supplies a grid-valid peg resolution consistent with the instruction's reference and offset

### Requirement: Complete checked order scenarios
A complete order scenario SHALL bind one immutable order and one cohesive `ScenarioAssumptions` value containing activation status, pricing resolution, and one or more positive liquidity slices. Scenario assumptions SHALL represent activation status and pricing resolution through explicit alternatives: immediate activation versus supplied trigger evidence, and direct pricing versus supplied peg resolution. Trigger evidence SHALL itself distinguish fixed and trailing forms so a favorable extremum is present exactly for trailing evidence. Missing, extraneous, or mismatched evidence SHALL remain constructible only as an unvalidated request and SHALL be rejected by complete-scenario construction.

Each liquidity slice SHALL carry positive instrument-owned lots, a coherent market state representing its assumed matched price and conversions, and a liquidity role. The instrument-owned `scenarios` capability SHALL accept the order and cohesive assumptions, validate aggregate invariants, and return a valid `OrderScenario`. It SHALL validate exact lot conservation, activation satisfaction, peg resolution, limit-price quality, and liquidity-role compatibility without projecting the order into a parallel scalar `View` model.

#### Scenario: Accept multiple assumed prices
- **WHEN** a complete order is modeled as positive slices at several grid-valid prices
- **THEN** the scenario retains every slice and their exact lot total

#### Scenario: Accept one slice without a collection wrapper
- **WHEN** a complete order has one assumed matched slice
- **THEN** a one-element vector in the scenario assumptions is accepted after aggregate validation

#### Scenario: Reject a buy above its limit
- **WHEN** a buy-limit scenario includes a matched slice above its effective limit
- **THEN** scenario construction fails with the offending slice identified

#### Scenario: Reject missing trigger evidence
- **WHEN** a triggered order is paired with the immediate/no-trigger assumption
- **THEN** complete-scenario construction returns a typed failure

#### Scenario: Reject extraneous trigger evidence
- **WHEN** an immediate order is paired with fixed or trailing trigger evidence
- **THEN** complete-scenario construction returns a typed failure

#### Scenario: Reject mismatched peg resolution
- **WHEN** a pegged order's supplied resolution disagrees with its reference or tick offset
- **THEN** scenario construction fails rather than repairing the resolution

#### Scenario: Reject an incomplete lot allocation
- **WHEN** positive slice lots do not sum exactly to the order lots
- **THEN** complete-scenario construction fails rather than treating the remainder as lifecycle state

#### Scenario: Avoid a parallel validation model
- **WHEN** scenario construction validates activation, pricing, lots, and liquidity
- **THEN** the checked domain alternatives themselves provide the data and cases being validated, with no duplicate kind-plus-option projection required
