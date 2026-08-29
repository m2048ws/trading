## MODIFIED Requirements

### Requirement: Focused instrument-owned capability surface
A stable `Instrument` SHALL retain its external identity and semantic definition components. Instrument-dependent values SHALL use ordinary public domain types rather than types parameterized by a generative instrument owner. Intrinsic observations of one value, including lot count, exact lot quantity, position count, exact position quantity, price ticks, price coefficient, and price rate, SHALL be operations of that value rather than functions that require passing the value back to its instrument.

The instrument SHALL expose focused capabilities named `prices`, `market`, `orders`, `scenarios`, `fees`, `valuation`, and `sizing` only for construction, checked aggregation, or operations involving multiple values or contextual policy. Moving behavior onto values or behind capabilities SHALL NOT weaken exactness, numeric refinements, registry provenance, runtime instrument-coherence checks, or typed failures. Superseded root observers, forwarding aliases, duplicate capability paths, and authority-forwarding APIs SHALL NOT remain in the public API.

#### Scenario: Read an owned value directly
- **WHEN** a downstream caller has lots or a price produced for an instrument
- **THEN** it can inspect the count or ticks and exact quantity or typed rate directly from that value without passing it back to a capability

#### Scenario: Discover operations by concern
- **WHEN** a downstream caller needs to construct a price, market state, order, or scenario or to value a position
- **THEN** the corresponding focused capability exposes the checked construction, aggregation, or valuation boundary without requiring unrelated capability paths

#### Scenario: Preserve one owner across capability views
- **WHEN** a caller obtains values and several capability views from one stable instrument
- **THEN** every view uses the same non-owner-parameterized domain types and aggregate boundaries reject an accidentally different runtime instrument identity

#### Scenario: Reject a superseded flat call
- **WHEN** downstream source attempts to use a removed root observer, flat operation, duplicate forwarding path, or owner-authority API
- **THEN** compilation fails rather than selecting a compatibility alias

### Requirement: Validated Instrument identity and roles
The public validated contract/listing aggregate SHALL remain named `Instrument`. Its definition SHALL be expressed through cohesive semantic components for instrument identity, registered asset roles, listing rules, and contract payoff rather than one undifferentiated parameter list. The components SHALL retain a stable instrument identity, an underlying identity that is not required to be a currency, the registered `base`, `quote`, `position`, and `settle` asset roles, the position lot grid, the quote-per-base price grid, and signed base-per-position and quote-per-position payoff terms.

The final construction boundary SHALL validate the components together. It SHALL require registry-coherent asset and grid witnesses, a position lot grid matching the position dimension, a price grid dimensionally equal to quote per base, distinct base and quote assets, and at least one nonzero payoff term. It SHALL reject foreign registry provenance, mismatched grid dimensions, contradictory definitions, and economically empty payoff. Successful construction SHALL return one `Instrument` without a generative owner type or lasting resolved wrapper. A separate public `Contract` and `Listing` hierarchy SHALL NOT be introduced by this change.

#### Scenario: Construct a coherent instrument
- **WHEN** identity, asset roles, listing rules, and payoff components agree on registry, dimensions, grids, and endpoints and at least one payoff term is nonzero
- **THEN** construction returns an `Instrument` retaining those exact components and its stable runtime instrument identity

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
`Lots`, `PositionLots`, and `Price` SHALL be ordinary non-owner-parameterized domain types that retain the stable `InstrumentId` for which they were produced. `Lots` SHALL be represented as a strictly positive value on the instrument's position lot grid and SHALL expose its positive coordinate and exact position quantity. `PositionLots` SHALL be represented independently as a signed coordinate on the same grid, including flat zero, and SHALL expose its signed coordinate and exact position quantity. `Price` SHALL be represented as a strictly positive value on the instrument's quote-per-base price grid and SHALL expose its positive tick coordinate, exact coefficient, and endpoint-typed quote-per-base rate.

The implementation SHALL preserve these numeric refinements in the representations rather than relying only on abstract public type inequality. Operations available on `Lots` or `Price` SHALL NOT manufacture zero or negative values under the same refined type. An instrument SHALL construct `Lots` from a positive count, convert `Lots` plus `Buy` or `Sell` into `PositionLots`, and expose flat position separately.

The `prices` capability SHALL construct `Price` from an exact `Rational` coefficient, an endpoint-typed `Rate[base.D, quote.D]`, or an explicitly low-level positive tick coordinate. Exact scalar and typed-rate constructors SHALL have identical positivity and exact-grid-membership behavior. Quantization SHALL remain explicitly named and residual-bearing. The economics core SHALL NOT accept `String` as price input.

#### Scenario: Construct positive lots through an instrument
- **WHEN** a caller asks one instrument to construct `1,000` lots
- **THEN** the result is positive `Lots` carrying that instrument's runtime identity, a count of `1,000`, and the exact position quantity count multiplied by the instrument lot quantum

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
- **WHEN** an aggregate boundary receives lots, positions, prices, orders, or scenarios carrying an `InstrumentId` different from the target instrument
- **THEN** it returns a typed instrument-mismatch failure rather than combining the values

#### Scenario: Do not claim anti-forgery from runtime identity
- **WHEN** a trusted caller constructs an instrument-dependent value directly
- **THEN** its `InstrumentId` is treated as ordinary domain data and not as a non-forgeable authority token

#### Scenario: Construct an ordinary exact price from a coefficient
- **WHEN** an adapter supplies a positive rational coefficient that belongs exactly to the instrument price grid
- **THEN** `instrument.prices` returns a `Price` whose own observers report the exact ticks and quote-per-base rate

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
A market state SHALL contain one positive price, the runtime `InstrumentId` for which it was produced, and an immutable checked set of exact positive conversions from relevant assets into the instrument's `settle` asset. The market state SHALL expose settle conversion as behavior of that state, using the conversions it owns, rather than requiring callers to pass its conversion collection back to the instrument. It SHALL provide base-to-settle and quote-to-settle rates, identity conversion for settle, and explicitly supplied additional conversions. It SHALL NOT fetch market data, choose a route, infer parity, select bid or ask, or round.

The instrument's `market` capability SHALL retain relationship-oriented construction named `quoteSettled`, `baseSettled`, `fromQuoteAnchor`, `fromBaseAnchor`, and `fromAnchors`, with parallel endpoint-typed forms for caller-derived rates. Scalar and typed forms SHALL apply identical positivity, identity, registry, instrument-coherence, and conversion-coherence checks. For price `P`, base-to-settle `B`, and quote-to-settle `Q`, construction SHALL establish exactly `B = P × Q`. Additional conversions SHALL remain a directly supplied immutable vector and SHALL be validated as a whole for target, provenance, positivity, identity, and duplicate source.

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
- **THEN** settlement valuation succeeds only when the market state contains an explicit checked conversion for that asset

#### Scenario: Convert through the owning market state
- **WHEN** a caller asks a market state to convert an exact quantity from a registered source asset
- **THEN** that state uses its own checked settle-targeted conversion or returns a typed missing-conversion failure

## ADDED Requirements

### Requirement: Trusted-client economics boundary
The economics artifact SHALL optimize its domain model for benign callers and accidental-error prevention. Its public contract SHALL NOT claim resistance to caller-defined same-package code, hostile Java or JVM bytecode, implementation subclassing, reflective construction, or constructor-bypassing deserialization. Ordinary constructors, direct closed alternatives, and runtime `InstrumentId` data MAY therefore be visible where numeric, grid, registry, and aggregate invariants remain enforced by their documented smart constructors and checked boundaries.

This trust decision SHALL apply only to the economics artifact. It SHALL NOT weaken the quantities artifact's closed dimension grammar, carrier construction authority, runtime witness identity, grid provenance, registry ownership, or serialization boundaries. Economics data types SHALL NOT be advertised as a stable Java-serialization persistence format merely because anti-deserialization hardening is removed.

#### Scenario: Construct a structurally valid domain alternative directly
- **WHEN** a benign caller constructs an activation, pricing, visibility, or scenario alternative whose field types already express its local invariants
- **THEN** it uses the direct domain type without obtaining an owner authority or JVM issuance token

#### Scenario: Retain checked economic boundaries
- **WHEN** a caller supplies invalid numeric input, foreign registry provenance, a wrong grid dimension, incoherent conversions, incomplete scenario lots, or inconsistent aggregate instrument identities
- **THEN** the corresponding smart constructor or aggregate boundary still returns a typed failure

#### Scenario: Keep quantities authority unchanged
- **WHEN** economics adopts the trusted-client boundary
- **THEN** callers gain no new way to manufacture quantity carriers, dimension witnesses, registered grids, or registry provenance

#### Scenario: Make no Java-serialization persistence promise
- **WHEN** a caller needs durable economics persistence
- **THEN** it uses an explicit adapter or schema rather than relying on Java object serialization as a supported compatibility contract

## REMOVED Requirements

### Requirement: Non-forgeable instrument ownership authority
**Reason**: The non-forgeable generative owner and JVM anti-spoof boundary impose most of the domain model's type, constructor, forwarding, and test complexity, while the only planned client is trusted.

**Migration**: Remove owner type parameters and authority arguments, use the direct non-owner-parameterized domain types, and rely on documented smart constructors plus runtime `InstrumentId` checks at multi-value aggregate boundaries for accidental-error detection.
