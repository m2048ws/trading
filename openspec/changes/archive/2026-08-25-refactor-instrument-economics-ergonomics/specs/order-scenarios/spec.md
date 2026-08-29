## MODIFIED Requirements

### Requirement: Compositional immutable orders
An order SHALL bind one exact instrument, a buy or sell side, positive instrument-bound lots, and independent mechanics for activation, price instruction, time in force, liquidity constraint, position effect, and visibility. The model SHALL express at least immediate and triggered activation; market, limit, and pegged price instructions; good-till-cancelled, immediate-or-cancel, fill-or-kill, and day duration; unrestricted and maker-only liquidity constraints; unrestricted and reduce-only position effects; and displayed, hidden, and iceberg visibility.

The instrument-owned `orders` capability SHALL construct activation, price-instruction, visibility, and order values and SHALL provide checked construction plus familiar market, limit, stop-market, and stop-limit entry points. All entry points SHALL reject combinations that are structurally impossible under the selected core mechanics. Venue adapters MAY impose additional venue-specific restrictions without weakening core validation.

#### Scenario: Construct an immediate market order
- **WHEN** a caller supplies an instrument-owned `orders` capability, side, and positive lots to its market-order constructor
- **THEN** it receives an immutable order with immediate activation and a market price instruction

#### Scenario: Construct a stop-limit order compositionally
- **WHEN** a caller supplies a checked trigger and a grid-valid limit price through `orders`
- **THEN** the resulting order combines triggered activation with a limit price instruction rather than requiring unrelated execution state

#### Scenario: Reject market maker-only mechanics
- **WHEN** an order combines a market price instruction with a maker-only constraint
- **THEN** checked construction fails because a market instruction cannot rest passively

#### Scenario: Keep lifecycle state absent
- **WHEN** a caller inspects an immutable order
- **THEN** it contains no venue order ID, submission status, cumulative or remaining fill quantity, cancellation state, fill records, or reported fees

### Requirement: Complete checked order scenarios
A complete order scenario SHALL bind one immutable order, the activation observation required by any trigger, and one or more positive liquidity slices. Each slice SHALL carry instrument-bound lots, a coherent market state representing its assumed matched price and conversions, and a liquidity role. The slice lot counts SHALL sum exactly to the order lot count.

The instrument-owned `scenarios` capability SHALL construct activation evidence, peg resolution, liquidity slices, and complete order scenarios. Its complete-scenario entry point SHALL accept the matched slices directly as an immutable vector because multiple assumed prices and liquidity roles are part of the domain, and SHALL validate all aggregate invariants before returning a scenario. Scenario validation SHALL require every limit slice price to be at the limit or better for its side, every resolved peg to satisfy the stated peg assumption, every trigger to be satisfied by its activation observation, and every liquidity role to comply with the order's core constraints. Invalid or incomplete assumptions SHALL return a typed failure rather than being silently repaired.

#### Scenario: Accept multiple assumed prices
- **WHEN** a complete order is modeled as positive slices at several grid-valid prices
- **THEN** `scenarios` retains every slice in the supplied vector and their exact lot total for valuation and fees

#### Scenario: Accept one slice without a collection wrapper
- **WHEN** a complete order has one assumed matched slice
- **THEN** the caller supplies a one-element vector directly and aggregate validation establishes non-emptiness and lot conservation

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
A round-trip trade scenario SHALL contain complete entry and exit order scenarios for the same stable instrument and SHALL require their signed position changes to sum exactly to flat. The instrument-owned `scenarios` capability SHALL perform this checked construction and preserve each leg's activation and liquidity slices. It SHALL NOT infer a closing side, silently resize a leg, or combine values belonging to different instrument paths.

#### Scenario: Construct a long round trip
- **WHEN** a buy entry and equal-lot sell exit belong to the same instrument
- **THEN** `scenarios` constructs a round trip whose held position is the entry's positive signed position change

#### Scenario: Construct a short round trip
- **WHEN** a sell entry and equal-lot buy exit belong to the same instrument
- **THEN** `scenarios` constructs a round trip whose held position is the entry's negative signed position change

#### Scenario: Reject unequal closing lots
- **WHEN** entry and exit position changes do not sum exactly to flat
- **THEN** round-trip construction fails

#### Scenario: Reject cross-instrument legs
- **WHEN** entry and exit scenarios are bound to distinct instrument paths
- **THEN** they cannot form one round trip without an explicit future checked rebinding boundary
