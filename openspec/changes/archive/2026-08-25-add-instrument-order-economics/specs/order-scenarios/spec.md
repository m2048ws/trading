## Purpose

Defines immutable, compositional order instructions and complete hypothetical order outcomes whose price and maker/taker assumptions can drive fees and PnL without introducing execution lifecycle state.

## ADDED Requirements

### Requirement: Compositional immutable orders
An order SHALL bind one exact instrument, a buy or sell side, positive instrument-bound lots, and independent mechanics for activation, price instruction, time in force, liquidity constraint, position effect, and visibility. The model SHALL express at least immediate and triggered activation; market, limit, and pegged price instructions; good-till-cancelled, immediate-or-cancel, fill-or-kill, and day duration; unrestricted and maker-only liquidity constraints; unrestricted and reduce-only position effects; and displayed, hidden, and iceberg visibility.

Checked construction and familiar market, limit, stop-market, and stop-limit constructors SHALL reject combinations that are structurally impossible under the selected core mechanics. Venue adapters MAY impose additional venue-specific restrictions without weakening core validation.

#### Scenario: Construct an immediate market order
- **WHEN** a caller supplies an instrument, side, and positive lots to the market-order constructor
- **THEN** it receives an immutable order with immediate activation and a market price instruction

#### Scenario: Construct a stop-limit order compositionally
- **WHEN** a caller supplies a checked trigger and a grid-valid limit price
- **THEN** the resulting order combines triggered activation with a limit price instruction rather than requiring unrelated execution state

#### Scenario: Reject market maker-only mechanics
- **WHEN** an order combines a market price instruction with a maker-only constraint
- **THEN** checked construction fails because a market instruction cannot rest passively

#### Scenario: Keep lifecycle state absent
- **WHEN** a caller inspects an immutable order
- **THEN** it contains no venue order ID, submission status, cumulative or remaining fill quantity, cancellation state, fill records, or reported fees

### Requirement: Explicit trigger and price mechanics
A fixed trigger SHALL identify its observed reference, exact comparison direction, and positive grid-valid trigger price. Supported references SHALL include last, mark, and index; supported comparisons SHALL include at-or-above and at-or-below. A trailing trigger SHALL retain its reference, direction, and explicit grid-aware offset. A pegged price instruction SHALL retain its reference and offset until a scenario or venue boundary resolves its effective limit price.

Stop-loss and take-profit names SHALL be convenience interpretations of explicit trigger comparisons rather than distinct hidden arithmetic. Trigger observation SHALL activate the associated price instruction but SHALL NOT itself imply an execution price or liquidity role.

#### Scenario: Distinguish activation from matched price
- **WHEN** a stop-market scenario activates at its mark-price trigger and assumes a worse later market price
- **THEN** the activation observation and matched-price market state remain distinct inputs

#### Scenario: Express take profit through comparison
- **WHEN** a sell order is intended to activate at or above a target price
- **THEN** the order represents that condition explicitly without requiring a separate fee or PnL formula for take-profit orders

#### Scenario: Resolve a pegged instruction before valuation
- **WHEN** a pegged order is evaluated in a complete scenario
- **THEN** the scenario supplies a grid-valid resolved price consistent with the peg rule used by that scenario

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
A complete order scenario SHALL bind one immutable order, the activation observation required by any trigger, and one or more positive liquidity slices. Each slice SHALL carry instrument-bound lots, a coherent market state representing its assumed matched price and conversions, and a liquidity role. The slice lot counts SHALL sum exactly to the order lot count.

Scenario validation SHALL require every limit slice price to be at the limit or better for its side, every resolved peg to satisfy the stated peg assumption, every trigger to be satisfied by its activation observation, and every liquidity role to comply with the order's core constraints. Invalid or incomplete assumptions SHALL return a typed failure rather than being silently repaired.

#### Scenario: Accept multiple assumed prices
- **WHEN** a complete order is modeled as positive slices at several grid-valid prices
- **THEN** the scenario retains every slice and their exact lot total for valuation and fees

#### Scenario: Reject a buy above its limit
- **WHEN** a buy-limit scenario includes a matched slice above the order's limit price
- **THEN** scenario construction fails

#### Scenario: Reject missing trigger evidence
- **WHEN** a triggered order scenario supplies matched slices without an activation observation satisfying its trigger
- **THEN** scenario construction fails

#### Scenario: Reject an incomplete lot allocation
- **WHEN** the positive slice lots do not sum exactly to the order lots
- **THEN** complete-scenario construction fails rather than treating the remainder as an execution record

### Requirement: Checked round-trip scenarios
A round-trip trade scenario SHALL contain complete entry and exit order scenarios for the same stable instrument and SHALL require their signed position changes to sum exactly to flat. It SHALL preserve each leg's activation and liquidity slices. It SHALL NOT infer a closing side, silently resize a leg, or combine values belonging to different instrument paths.

#### Scenario: Construct a long round trip
- **WHEN** a buy entry and equal-lot sell exit belong to the same instrument
- **THEN** round-trip construction succeeds and the held position is the entry's positive signed position change

#### Scenario: Construct a short round trip
- **WHEN** a sell entry and equal-lot buy exit belong to the same instrument
- **THEN** round-trip construction succeeds and the held position is the entry's negative signed position change

#### Scenario: Reject unequal closing lots
- **WHEN** entry and exit position changes do not sum exactly to flat
- **THEN** round-trip construction fails

#### Scenario: Reject cross-instrument legs
- **WHEN** entry and exit scenarios are bound to distinct instrument paths
- **THEN** they cannot form one round trip without an explicit future checked rebinding boundary
