# instrument-economics Specification
## Requirements
### Requirement: Downstream economics boundary
Instrument, order, fee, PnL, and risk APIs SHALL be delivered by a downstream economics artifact that depends on the
public quantities and immutable reference-data artifacts. The quantities artifact SHALL remain independently usable
and SHALL NOT acquire assets, stable grid identity, these economics concepts, their construction authority, or a
dependency on either downstream artifact. Reference data SHALL NOT acquire instrument, order, fee, PnL, or risk
concepts.

#### Scenario: Consume economics above quantities
- **WHEN** downstream Scala adds the economics artifact
- **THEN** it can use the domain API together with public `trading.quantity` values and trusted reference-data handles

#### Scenario: Keep quantities domain-neutral
- **WHEN** downstream Scala depends only on the quantities artifact
- **THEN** asset, stable-grid, instrument, order, fee, PnL, and position-sizing types are absent from that artifact

#### Scenario: Keep reference data instrument-neutral
- **WHEN** downstream Scala depends only on quantities and reference data
- **THEN** it can resolve assets and stable grids without acquiring instrument economics or execution policy

### Requirement: Focused instrument-owned capability surface
A stable `Instrument` SHALL retain its external identity and semantic definition components. Instrument-dependent values
SHALL use ordinary public domain types rather than types parameterized by a generative instrument owner. Intrinsic
observations of one value, including lot count, exact lot quantity, position count, exact position quantity, price
ticks, price coefficient, and price rate, SHALL be operations of that value rather than functions that require passing
the value back to its instrument.

The instrument SHALL expose focused capabilities named `prices`, `market`, `orders`, `scenarios`, `fees`, `valuation`,
and `sizing` only for construction, checked aggregation, or operations involving multiple values or contextual policy.
Moving behavior onto values or behind capabilities SHALL NOT weaken exactness, numeric refinements, reference-data
provenance, runtime instrument-coherence checks, or typed failures. Superseded root observers, forwarding aliases,
duplicate capability paths, and authority-forwarding APIs SHALL NOT remain in the public API.

#### Scenario: Read an owned value directly
- **WHEN** a downstream caller has lots or a price produced for an instrument
- **THEN** it can inspect the count or ticks and exact quantity or typed rate directly from that value without passing
  it back to a capability

#### Scenario: Discover operations by concern
- **WHEN** a downstream caller needs to construct a price, market state, order, or scenario or to value a position
- **THEN** the corresponding focused capability exposes the checked construction, aggregation, or valuation boundary
  without requiring unrelated capability paths

#### Scenario: Preserve one owner across capability views
- **WHEN** a caller obtains values and several capability views from one stable instrument
- **THEN** every view uses the same non-owner-parameterized domain types and aggregate boundaries reject an accidentally
  different runtime instrument identity

#### Scenario: Reject a superseded flat call
- **WHEN** downstream source attempts to use a removed root observer, flat operation, duplicate forwarding path, or
  owner-authority API
- **THEN** compilation fails rather than selecting a compatibility alias

### Requirement: Validated Instrument identity and roles
The public validated contract/listing aggregate SHALL remain named `Instrument`. It SHALL be constructed totally from
one trusted `InstrumentSpec` and SHALL retain the specification's stable instrument identity, possibly non-currency
underlying identity, base/quote/position/settle `Asset` roles, typed position-lot and quote-per-base `GridHandle`s, and
exact signed base-per-position and quote-per-position payoff rates.

Instrument construction SHALL NOT accept raw stable IDs or an `InstrumentDefinition`; resolve catalog membership;
compare issuer lineage; validate base/quote distinction, grid dimensions, or nonempty payoff; reconstruct rate endpoints
from scalar coefficients; or return catalog/assembly errors. Those obligations belong to the preceding pure assembly
boundary, and the proof-carrying specification makes their repetition unnecessary.

Successful construction SHALL return one `Instrument` without a generative owner type, lasting resolved wrapper,
snapshot, or live catalog dependency. A separate public `Contract` and `Listing` hierarchy SHALL NOT be introduced by
this change.

#### Scenario: Construct a coherent instrument
- **WHEN** a stable-ID definition resolves against one supplied snapshot with coherent roles and grids and at least one
  nonzero payoff coefficient, and assembly returns an `InstrumentSpec`
- **THEN** total construction returns an `Instrument` retaining those exact identities, trusted handles, typed rates,
  and stable runtime instrument identity

#### Scenario: Construct an assembled instrument
- **WHEN** a caller supplies an `InstrumentSpec` whose assembly already established roles, grids, and payoff endpoints
- **THEN** construction returns an `Instrument` retaining those exact trusted components without another fallible step

#### Scenario: Represent a non-currency underlying
- **WHEN** an assembled specification references an index or basket as its underlying
- **THEN** the instrument retains that identity without manufacturing a fifth currency role

#### Scenario: Reject a foreign grid
- **WHEN** a raw definition names a lot or price grid identity that is absent from the supplied snapshot, including one
  known only to another catalog snapshot, or whose resolved handle has the wrong dimension
- **THEN** assembly returns a contextual typed resolution or dimension violation and no `InstrumentSpec`, without
  retagging the grid or weakening its provenance

#### Scenario: Reject contradictory components
- **WHEN** a raw definition has a remaining role or dimension contradiction such as equal base and quote or a listing
  grid that conflicts with its resolved role dimensions
- **THEN** assembly returns a typed structural or dimension violation and no `InstrumentSpec` instead of constructing
  a partially coherent instrument; the cohesive raw product prevents independently owned listing or payoff components

#### Scenario: Reject an empty payoff
- **WHEN** both exact payoff coefficients in a raw definition are zero
- **THEN** assembly returns the typed empty-payoff violation and no `InstrumentSpec`, so final instrument construction
  never receives an economically empty definition

#### Scenario: Reject raw construction statically
- **WHEN** downstream Scala attempts to pass raw stable IDs, a raw definition, or a catalog snapshot to final instrument
  construction
- **THEN** compilation fails because only `InstrumentSpec` is accepted

#### Scenario: Keep assembly failures out of economics
- **WHEN** a raw definition contains unknown identities, a wrong grid dimension, equal base and quote, or an empty
  payoff
- **THEN** instrument assembly rejects it before `Instrument` construction and ordinary economics receives no partial
  value

#### Scenario: Preserve catalog independence after assembly
- **WHEN** an instrument is used after later catalog revisions are published
- **THEN** it retains its assembled handles and meaning without consulting the catalog

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
A market state SHALL contain one positive price, the runtime `InstrumentId` for which it was produced, and an immutable
checked set of exact positive conversions from relevant trusted assets into the instrument's `settle` asset. The market
state SHALL expose settle conversion as behavior of that state, using the conversions it owns, rather than requiring
callers to pass its conversion collection back to the instrument. It SHALL provide base-to-settle and quote-to-settle
rates, identity conversion for settle, and explicitly supplied additional conversions. It SHALL NOT fetch market data,
choose a route, infer parity, select bid or ask, round, or consult a live catalog.

The instrument's `market` capability SHALL retain relationship-oriented construction named `quoteSettled`,
`baseSettled`, `fromQuoteAnchor`, `fromBaseAnchor`, and `fromAnchors`, with parallel endpoint-typed forms for
caller-derived rates. Scalar and typed forms SHALL apply identical positivity, identity, reference-data provenance,
instrument-coherence, and conversion-coherence checks. For price `P`, base-to-settle `B`, and quote-to-settle `Q`,
construction SHALL establish exactly `B = P × Q`. Additional conversions SHALL remain a directly supplied immutable
vector and SHALL be validated as a whole for target, provenance, positivity, identity, and duplicate source.

#### Scenario: Settle in quote
- **WHEN** settle and quote are the same trusted asset
- **THEN** `quoteSettled` makes quote-to-settle identity and base-to-settle exactly the listing price

#### Scenario: Settle in base
- **WHEN** settle and base are the same trusted asset
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
- **THEN** settlement valuation succeeds only when the market state contains an explicit checked conversion for that
  asset

#### Scenario: Convert through the owning market state
- **WHEN** a caller asks a market state to convert an exact quantity from a trusted source asset
- **THEN** that state uses its own checked settle-targeted conversion or returns a typed missing-conversion failure

### Requirement: Universal exact contract valuation
An instrument's `valuation` capability SHALL calculate exact settle value per unit of position as:

```text
basePerPosition.andThen(baseToSettle)
  + quotePerPosition.andThen(quoteToSettle)
```

It SHALL calculate position value by applying that rate to the exact signed position quantity, including the instrument lot quantum and lot coordinate. It SHALL calculate price PnL as exit position value minus entry position value for the same signed position. These calculations SHALL remain exact `Quantity[settle.D]` values and SHALL NOT quantize to a settlement, wallet, or storage grid.

#### Scenario: Include lot count and quantum in valuation
- **WHEN** a position contains `n` lot coordinates and each lot represents exact position quantum `q`
- **THEN** `valuation` applies settle-per-position to exact signed quantity `n × q`, rather than omitting or separately approximating lots

#### Scenario: Calculate long and short PnL symmetrically
- **WHEN** equal-magnitude long and short positions are valued at the same entry and exit market states
- **THEN** their price PnL values are exact negations

#### Scenario: Preserve off-grid inverse PnL
- **WHEN** inverse-style valuation produces a rational settle amount not on the settlement asset's storage grid
- **THEN** price PnL remains that exact rational quantity without rounding

### Requirement: Product-family normalization without core flags
Spot, linear, inverse, and quanto-style definitions SHALL normalize into the same signed base/quote contract terms and market-state conversions. Core valuation SHALL NOT branch on public `isSpot`, `isLinear`, `isInverse`, `isQuanto`, contract-family enums, or venue-specific multiplier fields. Venue adapters MAY interpret source metadata to produce the validated generic terms.

#### Scenario: Normalize a linear payoff
- **WHEN** an adapter supplies a signed base-per-position term and zero quote-per-position term
- **THEN** universal valuation produces the linear price-dependent settle value through base-to-settle conversion

#### Scenario: Normalize an inverse payoff
- **WHEN** an adapter supplies zero base-per-position and a signed quote-per-position term with settle equal to base
- **THEN** universal valuation uses the reciprocal-price quote-to-settle conversion and produces inverse PnL without an inverse branch

#### Scenario: Normalize a quanto-style payoff
- **WHEN** price and an explicit third-asset settlement anchor produce coherent base-to-settle and quote-to-settle rates
- **THEN** universal valuation produces the quanto-style result without a quanto branch

### Requirement: Trusted-client economics boundary
The economics artifact SHALL optimize its domain model for benign callers and accidental-error prevention. Its public
contract SHALL NOT claim resistance to caller-defined same-package code, hostile Java or JVM bytecode, implementation
subclassing, reflective construction, or constructor-bypassing deserialization. Ordinary constructors, direct closed
alternatives, and runtime `InstrumentId` data MAY therefore be visible where numeric, grid, reference-data, and
aggregate invariants remain enforced by their documented smart constructors and checked boundaries.

This trust decision SHALL apply only to the economics artifact. It SHALL NOT weaken the quantities artifact's closed
dimension grammar, carrier construction authority, runtime dimension identity, anonymous grid authority, or
serialization boundaries, nor the reference-data artifact's stable identity, handle issuance, or issuer-provenance
boundaries. Economics data types SHALL NOT be advertised as a stable Java-serialization persistence format merely
because anti-deserialization hardening is removed.

#### Scenario: Construct a structurally valid domain alternative directly
- **WHEN** a benign caller constructs an activation, pricing, visibility, or scenario alternative whose field types
  already express its local invariants
- **THEN** it uses the direct domain type without obtaining an owner authority or JVM issuance token

#### Scenario: Retain checked economic boundaries
- **WHEN** a caller supplies invalid numeric input, foreign reference-data provenance, a wrong grid dimension,
  incoherent conversions, incomplete scenario lots, or inconsistent aggregate instrument identities
- **THEN** the corresponding smart constructor or aggregate boundary still returns a typed failure

#### Scenario: Keep quantities authority unchanged
- **WHEN** economics adopts the trusted-client boundary
- **THEN** callers gain no new way to manufacture quantity carriers, dimension witnesses, anonymous grids, trusted
  assets, stable grid handles, or issuer provenance

#### Scenario: Make no Java-serialization persistence promise
- **WHEN** a caller needs durable economics persistence
- **THEN** it uses an explicit adapter or schema rather than relying on Java object serialization as a supported
  compatibility contract

### Requirement: Settlement conversions preserve typed endpoints
A settle-targeted conversion SHALL retain its trusted source asset, target settlement asset, and an endpoint-typed
`Rate[source.D, settle.D]`. Its scalar coefficient MAY remain observable as a projection of that rate, but the
conversion SHALL NOT discard its endpoint relationship and later reconstruct the typed rate from an untyped stored
scalar.

A market state SHALL retain base-to-settle, quote-to-settle, and additional conversions in endpoint-safe form. When
converting a heterogeneous source quantity, it SHALL first validate source identity, shared reference-data lineage, and
runtime dimension identity, then apply the corresponding typed rate. It SHALL NOT infer grid identity from dimension
equality, consult a live catalog, or grant callers new quantity-construction or proof authority.

Contract valuation, position valuation, price PnL, fee conversion, fee aggregation, net PnL, and downside-risk
calculations SHALL preserve `Rate` and `Quantity` types through arithmetic. Scalar observations SHALL be used only for
explicit validation, ordering, diagnostics, or public coefficient observers, and the numerical results SHALL remain
exactly equal to the established rational formulas.

#### Scenario: Retain an additional conversion rate
- **WHEN** a caller constructs an additional source-to-settle conversion from an endpoint-typed rate
- **THEN** the resulting conversion exposes that same source-to-settle rate and its exact coefficient

#### Scenario: Reject reversed settlement endpoints statically
- **WHEN** downstream Scala supplies a settle-to-source rate where a source-to-settle rate is required
- **THEN** compilation fails instead of accepting the coefficient and relabeling the result

#### Scenario: Compose contract legs through settlement rates
- **WHEN** base-per-position and quote-per-position rates are valued under coherent base-to-settle and quote-to-settle
  rates
- **THEN** settle-per-position is the typed sum of the two composed position-to-settle rates and has the exact
  established coefficient

#### Scenario: Apply a typed conversion after runtime lookup
- **WHEN** a quantity's trusted source identity and runtime dimension match a heterogeneous conversion entry
- **THEN** the market state returns the exact settlement quantity by applying that entry's source-to-settle rate

#### Scenario: Preserve grid provenance independence
- **WHEN** source and target dimensions are runtime-compatible but a stable grid or reference-data provenance claim is
  unrelated
- **THEN** typed conversion does not treat dimension compatibility as grid identity or shared issuer lineage

## Purpose
Defines validated trading instruments and the exact, grid-aware market inputs needed to value signed positions uniformly in an instrument's settlement asset.
