## MODIFIED Requirements

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
The public validated contract/listing aggregate SHALL remain named `Instrument`. Its definition SHALL be expressed
through cohesive semantic components for instrument identity, trusted asset roles, listing rules, and contract payoff
rather than one undifferentiated parameter list. The components SHALL retain a stable instrument identity, an
underlying identity that is not required to be a currency, the trusted `base`, `quote`, `position`, and `settle`
`Asset` roles, the position lot `GridHandle`, the quote-per-base price `GridHandle`, and signed base-per-position and
quote-per-position payoff terms.

The final construction boundary SHALL validate the components together. It SHALL require reference-data-coherent asset
and grid handles, a position lot grid matching the position dimension, a price grid dimensionally equal to quote per
base, distinct base and quote assets, and at least one nonzero payoff term. It SHALL reject foreign issuer lineage,
mismatched grid dimensions, contradictory definitions, and economically empty payoff. Successful construction SHALL
return one `Instrument` without a generative owner type or lasting resolved wrapper. A separate public `Contract` and
`Listing` hierarchy SHALL NOT be introduced by this change.

#### Scenario: Construct a coherent instrument
- **WHEN** identity, asset roles, listing rules, and payoff components agree on issuer lineage, dimensions, grids, and
  endpoints and at least one payoff term is nonzero
- **THEN** construction returns an `Instrument` retaining those exact components and its stable runtime instrument
  identity

#### Scenario: Represent a non-currency underlying
- **WHEN** an instrument definition references an index or basket as its underlying
- **THEN** the underlying identity is retained without manufacturing a fifth currency role

#### Scenario: Reject a foreign grid
- **WHEN** listing rules contain a lot or price grid handle issued by another reference-data lineage or with the wrong
  dimension
- **THEN** final instrument construction fails without retagging the grid or weakening its provenance

#### Scenario: Reject contradictory components
- **WHEN** independently valid definition components disagree at their shared asset, dimension, issuer, or grid boundary
- **THEN** final instrument construction returns a typed contradiction instead of constructing a partially coherent
  instrument

#### Scenario: Reject an empty payoff
- **WHEN** both signed payoff terms are zero
- **THEN** construction fails because the position has no economic value

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

### Requirement: Proof-carrying instrument definition validation
The economics artifact SHALL distinguish a raw `Definition` from a `ValidatedDefinition` produced only by the
instrument-definition validation boundary. A `ValidatedDefinition` SHALL retain the raw identity, trusted asset roles,
listing rules, and payoff together with checked evidence that all handles share the required reference-data lineage,
the position grid has the exact position dimension, the price grid has the exact quote-per-base dimension, and the
payoff endpoints are those exact roles. Constructing an `Instrument` from a `ValidatedDefinition` SHALL be total and
SHALL NOT repeat fallible handle-provenance, grid, role, or payoff checks.

The validation boundary SHALL expose domain-specific definition violations and SHALL return every independently
detectable violation in a deterministic order. Its public invalid result SHALL contain a non-empty ordered domain
collection and SHALL NOT expose a validation-library collection type. Checks that require evidence produced by an
earlier successful stage SHALL run only after that stage succeeds and SHALL NOT fabricate dependent violations when the
prerequisite evidence is absent.

The existing fail-fast `Instrument.create(Definition)` boundary SHALL remain available. It SHALL use the same validation
rules and deterministic ordering, return the first corresponding `EconomicsError`, and produce the same successful
`Instrument` as validation followed by total construction.

#### Scenario: Validate a coherent raw definition
- **WHEN** a raw definition has coherent asset roles, reference-data provenance, grids, and at least one nonzero payoff
  term
- **THEN** validation returns a `ValidatedDefinition` whose checked grid and payoff evidence can construct the final
  `Instrument` without another fallible narrowing step

#### Scenario: Accumulate independent definition violations
- **WHEN** one raw definition independently has contradictory component roles, equal base and quote assets, and an
  empty payoff
- **THEN** the accumulating boundary returns all applicable domain violations in stable order rather than only the first

#### Scenario: Do not invent dependent evidence failures
- **WHEN** a prerequisite issuer-provenance or dimension check fails and later evidence cannot be soundly produced
- **THEN** validation reports the observable prerequisite violations and does not execute checks that require the
  missing evidence

#### Scenario: Preserve deterministic fail-fast construction
- **WHEN** the same invalid raw definition is supplied to the accumulating validator and to `Instrument.create`
- **THEN** `Instrument.create` returns the domain error corresponding to the first accumulated violation according to
  the shared stable ordering

#### Scenario: Keep checked casts private
- **WHEN** downstream Scala validates and constructs an instrument
- **THEN** it neither performs a cast nor receives public authority to retag a grid, payoff, dimension, or
  reference-data handle

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
