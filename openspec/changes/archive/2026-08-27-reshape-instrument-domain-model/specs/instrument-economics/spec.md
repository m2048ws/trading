## MODIFIED Requirements

### Requirement: Focused instrument-owned capability surface
A stable `Instrument` SHALL retain its generative ownership identity, external identity, semantic definition components, and convenient aliases for its owner-indexed public value types. Intrinsic observations of one owned value, including lot count, exact lot quantity, position count, exact position quantity, price ticks, price coefficient, and price rate, SHALL be operations of that value rather than functions that require passing the value back to its instrument.

The instrument SHALL expose focused owner-bound capabilities named `prices`, `market`, `orders`, `scenarios`, `fees`, `valuation`, and `sizing` only for construction, checked aggregation, or operations involving multiple owned values or contextual policy. Moving behavior onto values or behind capabilities SHALL NOT weaken private aggregate construction, owner identity, exactness, registry provenance, or typed failures. Superseded root observers, forwarding aliases, and duplicate capability paths SHALL NOT remain in the public API.

#### Scenario: Read an owned value directly
- **WHEN** a downstream caller has instrument-owned lots or a price
- **THEN** it can inspect the count or ticks and exact quantity or typed rate directly from that value without passing it back to a capability

#### Scenario: Discover operations by concern
- **WHEN** a downstream caller needs to construct a price, market state, order, or scenario or to value a position
- **THEN** the corresponding focused capability exposes the checked construction, aggregation, or valuation boundary without requiring unrelated capability paths

#### Scenario: Preserve one owner across capability views
- **WHEN** a caller obtains owned values and several capability views from one stable instrument
- **THEN** every view consumes and produces values carrying the same non-forgeable owner identity, and no result can be substituted with an equal-looking value from another instrument

#### Scenario: Reject a superseded flat call
- **WHEN** downstream source attempts to use a removed root observer, flat operation, or duplicate forwarding path
- **THEN** compilation fails rather than selecting a compatibility alias

### Requirement: Validated Instrument identity and roles
The public validated contract/listing aggregate SHALL remain named `Instrument`. Its definition SHALL be expressed through cohesive semantic components for instrument identity, registered asset roles, listing rules, and contract payoff rather than one undifferentiated parameter list. The components SHALL retain a stable instrument identity, an underlying identity that is not required to be a currency, the registered `base`, `quote`, `position`, and `settle` asset roles, the position lot grid, the quote-per-base price grid, and signed base-per-position and quote-per-position payoff terms.

The final construction boundary SHALL validate the components together. It SHALL require registry-coherent asset and grid witnesses, a position lot grid matching the position dimension, a price grid dimensionally equal to quote per base, distinct base and quote assets, and at least one nonzero payoff term. It SHALL reject foreign registry provenance, mismatched grid dimensions, contradictory definitions, and economically empty payoff. Successful construction SHALL return one generative `Instrument` without a lasting resolved wrapper. A separate public `Contract` and `Listing` hierarchy SHALL NOT be introduced by this change.

#### Scenario: Construct a coherent instrument
- **WHEN** identity, asset roles, listing rules, and payoff components agree on registry, dimensions, grids, and endpoints and at least one payoff term is nonzero
- **THEN** construction returns an `Instrument` retaining those exact components and a fresh owner identity

#### Scenario: Represent a non-currency underlying
- **WHEN** an instrument definition references an index or basket as its underlying
- **THEN** the underlying identity is retained without manufacturing a fifth currency role

#### Scenario: Reject a foreign grid
- **WHEN** listing rules contain a lot or price grid owned by another registry or with the wrong dimension
- **THEN** final instrument construction fails without retagging the grid or weakening its provenance

#### Scenario: Reject contradictory components
- **WHEN** independently valid definition components disagree at their shared asset, dimension, registry, or grid boundary
- **THEN** final instrument construction returns a typed contradiction instead of constructing a partially coherent instrument

#### Scenario: Reject an empty payoff
- **WHEN** both signed payoff terms are zero
- **THEN** construction fails because the position has no economic value

### Requirement: Instrument-bound lots, positions, and prices
Each stable `Instrument` SHALL own distinct `Lots`, `PositionLots`, and `Price` types through one non-forgeable owner identity. `Lots` SHALL be represented as a strictly positive value on the instrument's position lot grid and SHALL expose its positive coordinate and exact position quantity. `PositionLots` SHALL be represented independently as a signed coordinate on the same grid, including flat zero, and SHALL expose its signed coordinate and exact position quantity. `Price` SHALL be represented as a strictly positive value on the instrument's quote-per-base price grid and SHALL expose its positive tick coordinate, exact coefficient, and endpoint-typed quote-per-base rate.

The implementation SHALL preserve these numeric refinements in the owned representations rather than relying only on abstract public type inequality. Operations available on `Lots` or `Price` SHALL NOT manufacture zero or negative values under the same refined type. An instrument SHALL construct `Lots` from a positive count, convert `Lots` plus `Buy` or `Sell` into `PositionLots`, and expose flat position separately.

The `prices` capability SHALL construct `Price` from an exact `Rational` coefficient, an endpoint-typed `Rate[base.D, quote.D]`, or an explicitly low-level positive tick coordinate. Exact scalar and typed-rate constructors SHALL have identical positivity and exact-grid-membership behavior. Quantization SHALL remain explicitly named and residual-bearing. The economics core SHALL NOT accept `String` as price input.

#### Scenario: Construct positive lots through an instrument
- **WHEN** a caller asks one instrument to construct `1,000` lots
- **THEN** the result is positive instrument-owned `Lots` whose own count is `1,000` and whose exact quantity is count multiplied by the instrument lot quantum

#### Scenario: Reject zero or negative lots
- **WHEN** a caller supplies a zero or negative lot count
- **THEN** `Lots` construction fails and no value of the refined lots type is returned

#### Scenario: Derive a signed position change
- **WHEN** buy and sell sides are applied to the same positive lots value
- **THEN** they produce positive and negative `PositionLots` respectively, and neither result is usable as positive `Lots`

#### Scenario: Prevent refinement loss through arithmetic
- **WHEN** a caller subtracts, negates, or otherwise applies an operation that may make a positive lot or price nonpositive
- **THEN** the result is not typed as valid `Lots` or `Price` without a new checked refinement boundary

#### Scenario: Prevent cross-instrument mixing
- **WHEN** two stable instruments have equal-looking grids or definitions but distinct owner identities
- **THEN** their lots, positions, prices, orders, and scenarios are not implicitly interchangeable

#### Scenario: Construct an ordinary exact price from a coefficient
- **WHEN** an adapter supplies a positive rational coefficient that belongs exactly to the instrument price grid
- **THEN** `instrument.prices` returns an owned `Price` whose own observers report the exact ticks and quote-per-base rate

#### Scenario: Preserve listing price grid membership
- **WHEN** an exact coefficient or endpoint-typed quote-per-base rate is not on the instrument price grid
- **THEN** exact price construction fails and no hidden quantization occurs

#### Scenario: Reconstruct a price from stored ticks
- **WHEN** a persistence or venue adapter holds a validated positive whole-number coordinate on the instrument price grid
- **THEN** the explicitly named tick constructor reconstructs the corresponding refined `Price`

#### Scenario: Keep text grammar outside economics
- **WHEN** raw market data contains malformed text or a venue-specific number form
- **THEN** the adapter rejects or parses that text before invoking the exact economics constructor

### Requirement: Coherent market state and settle-targeted conversions
A market state SHALL contain one positive instrument-owned price and an immutable checked set of exact positive conversions from relevant assets into the instrument's `settle` asset. The market state SHALL expose settle conversion as behavior of that state, using the conversions it owns, rather than requiring callers to pass its conversion collection back to the instrument. It SHALL provide base-to-settle and quote-to-settle rates, identity conversion for settle, and explicitly supplied additional conversions. It SHALL NOT fetch market data, choose a route, infer parity, select bid or ask, or round.

The instrument's `market` capability SHALL retain relationship-oriented construction named `quoteSettled`, `baseSettled`, `fromQuoteAnchor`, `fromBaseAnchor`, and `fromAnchors`, with parallel endpoint-typed forms for caller-derived rates. Scalar and typed forms SHALL apply identical positivity, identity, registry, and coherence checks. For price `P`, base-to-settle `B`, and quote-to-settle `Q`, construction SHALL establish exactly `B = P × Q`. Additional conversions SHALL remain a directly supplied immutable vector and SHALL be validated as a whole for target, provenance, positivity, identity, and duplicate source.

#### Scenario: Settle in quote
- **WHEN** settle and quote are the same registered asset
- **THEN** `quoteSettled` makes quote-to-settle identity and base-to-settle exactly the listing price

#### Scenario: Settle in base
- **WHEN** settle and base are the same registered asset
- **THEN** `baseSettled` makes base-to-settle identity and quote-to-settle exactly the reciprocal price

#### Scenario: Reject a nonidentity settlement anchor truthfully
- **WHEN** settle is base or quote and checked dual anchors supply a nonidentity same-asset conversion
- **THEN** construction fails with a diagnostic preserving the supplied source, target, and coefficient

#### Scenario: Settle in a third asset from a scalar anchor
- **WHEN** one positive quote-to-settle or base-to-settle rational coefficient is supplied for a third settlement asset
- **THEN** the other settlement rate is derived exactly through the price

#### Scenario: Preserve a typed derived anchor
- **WHEN** caller-owned typed arithmetic produces a correctly endpoint-typed settlement rate
- **THEN** the typed entry point accepts it and constructs the same market state as the equal scalar coefficient

#### Scenario: Reject incoherent conversions
- **WHEN** independently supplied anchors imply a quote-per-base rate different from the grid-valid price
- **THEN** construction fails with the contradictory exact values preserved

#### Scenario: Validate plural additional conversions
- **WHEN** a caller supplies zero, one, or many additional settle-targeted conversions
- **THEN** the market-state boundary accepts the coherent vector and rejects invalid or duplicate sources

#### Scenario: Require an additional fee conversion
- **WHEN** a fee denomination uses an asset other than base, quote, or settle
- **THEN** settlement valuation succeeds only when the owning market state contains an explicit checked conversion for that asset

#### Scenario: Convert through the owning market state
- **WHEN** a caller asks a market state to convert an exact quantity from a registered source asset
- **THEN** that state uses its own checked settle-targeted conversion or returns a typed missing-conversion failure

## ADDED Requirements

### Requirement: Non-forgeable instrument ownership authority
All public values whose meaning depends on one exact instrument SHALL carry one generative owner identity issued during successful instrument construction. Any internal authority used to construct, inspect, or reconstitute those values SHALL be sealed against caller implementation, unavailable from the public `Instrument` surface, and unusable without the issuing instrument's owner identity. The packaged JVM API SHALL enforce the same boundary for ordinary Java source: Scala-only sealing or package-private metadata is insufficient, the raw JVM gate SHALL have no caller-accessible issuer or constructor, and every trusted carrier and aggregate constructor SHALL require that gate. Separating implementations into concern-named files SHALL NOT make constructors, retagging operations, witness casts, or owner tokens available to ordinary or same-package-spoof downstream source.

#### Scenario: Reject an external owner implementation
- **WHEN** downstream source attempts to implement or manufacture the owner authority used by instrument values
- **THEN** compilation fails because that authority is closed and no public constructor exists

#### Scenario: Reject same-package spoofing
- **WHEN** downstream source declares `package trading.economics` and attempts to construct or retag lots, prices, orders, market states, fees, or scenarios through implementation helpers
- **THEN** compilation fails without relying on package qualification as a trust boundary

#### Scenario: Allow internal modularity without authority leakage
- **WHEN** a capability implementation is moved out of `Instrument.scala`
- **THEN** it can operate only through owner-indexed safe operations and cannot create a value for another owner or bypass a required validation boundary
