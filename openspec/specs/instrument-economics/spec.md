# instrument-economics Specification

## Requirements

### Requirement: Downstream economics boundary
Assembled instruments, lots, positions, prices, market states, fee-value primitives, and exact valuation SHALL be
delivered by a dedicated instrument-economics artifact that depends only on the public quantities and immutable
reference-data artifacts. Order, execution-scenario, fee-policy, risk, application, codec, and runtime artifacts SHALL
depend on instrument economics when they need these values; instrument economics SHALL NOT depend on those artifacts.

The quantities artifact SHALL remain independently usable and SHALL NOT acquire assets, stable grid identity, or
instrument-domain concepts. The reference-data artifact SHALL remain independently usable above quantities and SHALL
NOT acquire instruments or any downstream trading policy.

#### Scenario: Consume economics above quantities
- **WHEN** downstream Scala adds only the instrument-economics artifact
- **THEN** it can assemble and value instruments using public quantity values and trusted reference-data handles

#### Scenario: Keep quantities domain-neutral
- **WHEN** downstream Scala depends only on the quantities artifact
- **THEN** asset, stable-grid, instrument, order, fee, PnL, and position-sizing types are absent from that artifact

#### Scenario: Keep reference data instrument-neutral
- **WHEN** downstream Scala depends only on quantities and reference data
- **THEN** it can work with assets, grids, and catalog snapshots without acquiring instrument economics

#### Scenario: Enforce one-way downstream dependencies
- **WHEN** the instrument-economics artifact is compiled in isolation from higher trading modules
- **THEN** its production classpath contains no order, execution-scenario, fee-policy, risk, application, codec, or
  runtime artifact

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
`Lots`, `PositionLots`, and `Price` SHALL be ordinary non-owner-parameterized domain types that retain the stable
`InstrumentId` for which they were produced. `Lots` SHALL be represented as a strictly positive value on the
instrument's position lot grid and SHALL expose its positive coordinate and exact position quantity. `PositionLots`
SHALL be represented independently as a signed coordinate on the same grid, including flat zero, and SHALL expose its
signed coordinate and exact position quantity. `Price` SHALL be represented as a strictly positive value on the
instrument's quote-per-base price grid and SHALL expose its positive tick coordinate, exact coefficient, and
endpoint-typed quote-per-base rate.

The representations SHALL preserve their numeric refinements and grid evidence. A pure `Lots` constructor SHALL accept
an instrument and positive count. A pure `PositionLots` constructor SHALL accept an instrument and signed coordinate;
applying buy or sell side to lots belongs to the downstream order model. Pure `Price` constructors SHALL accept an
instrument with an exact `Rational` coefficient, an endpoint-typed `Rate[base.D, quote.D]`, or an explicitly low-level
positive tick coordinate. Exact scalar and typed-rate constructors SHALL have identical positivity and exact-grid-
membership behavior. Quantization SHALL remain explicitly named and residual-bearing. The economics core SHALL NOT
accept `String` as price input.

Arithmetic that may destroy a refinement SHALL return an appropriately weaker value or cross another checked
constructor. Aggregate operations SHALL reject ordinary values carrying a different `InstrumentId`; that identity is
domain data and SHALL NOT be advertised as an anti-forgery capability.

#### Scenario: Construct positive lots through an instrument
- **WHEN** a caller supplies one instrument and a positive count of `1,000`
- **THEN** construction returns `Lots` carrying that instrument identity, count `1,000`, and the exact position quantity
  count multiplied by the retained lot quantum

#### Scenario: Reject zero or negative lots
- **WHEN** a caller supplies a zero or negative lot count
- **THEN** `Lots` construction fails and no value of the refined lots type is returned

#### Scenario: Derive a signed position change
- **WHEN** a caller supplies positive, zero, and negative grid coordinates to the position constructor
- **THEN** it obtains the corresponding exact `PositionLots` values without introducing an order-side concept into
  instrument economics

#### Scenario: Prevent refinement loss through arithmetic
- **WHEN** a caller subtracts, negates, or otherwise applies an operation that may make positive lots or price
  nonpositive
- **THEN** the result is not typed as valid `Lots` or `Price` without a new checked refinement boundary

#### Scenario: Prevent cross-instrument mixing
- **WHEN** an aggregate boundary receives lots, positions, prices, or market states carrying a different `InstrumentId`
  from its explicit instrument
- **THEN** it returns a typed instrument-mismatch failure rather than combining the values

#### Scenario: Do not claim anti-forgery from runtime identity
- **WHEN** a benign caller directly constructs an otherwise valid instrument-dependent value with an `InstrumentId`
- **THEN** aggregate boundaries treat that identity as ordinary domain data and continue to check it rather than
  claiming non-forgeable authority

#### Scenario: Construct an ordinary exact price from a coefficient
- **WHEN** an adapter supplies a positive rational coefficient that belongs exactly to the explicit instrument's price
  grid
- **THEN** exact price construction returns a `Price` whose observers preserve that coefficient, its positive ticks,
  and its endpoint-typed quote-per-base rate

#### Scenario: Construct an exact price from a typed rate
- **WHEN** a caller supplies an endpoint-typed positive quote-per-base rate that belongs exactly to the instrument price
  grid
- **THEN** `Price` construction preserves the rate, exact coefficient, positive ticks, and grid membership

#### Scenario: Preserve listing price grid membership
- **WHEN** an exact coefficient or endpoint-typed quote-per-base rate is not on the instrument price grid
- **THEN** exact price construction fails and no hidden quantization occurs

#### Scenario: Reconstruct a price from stored ticks
- **WHEN** a boundary adapter holds a validated positive whole-number coordinate on the instrument price grid
- **THEN** the explicitly named tick constructor reconstructs the corresponding refined `Price`

#### Scenario: Keep text grammar outside economics
- **WHEN** raw market data contains malformed text or a venue-specific number form
- **THEN** the adapter rejects or parses that text before invoking an exact economics constructor

### Requirement: Coherent market state and settle-targeted conversions
A market state SHALL contain one positive price, the `InstrumentId` for which it was produced, and an immutable checked
set of exact positive conversions from relevant trusted assets into the instrument's `settle` asset. It SHALL expose
base-to-settle and quote-to-settle rates, identity conversion for settle, explicitly supplied additional conversions,
and pure conversion of an identified source quantity. It SHALL NOT fetch market data, choose a route, infer parity,
select bid or ask, round, or consult a catalog.

Pure `MarketState` construction SHALL accept an explicit instrument and retain relationship-oriented entry points for
quote-settled, base-settled, quote-anchor, base-anchor, and dual-anchor cases, including endpoint-typed forms for
caller-derived rates. Scalar and typed forms SHALL apply identical positivity, asset-identity, immutable-lineage,
instrument-coherence, and conversion-coherence checks. For price `P`, base-to-settle `B`, and quote-to-settle `Q`,
construction SHALL establish exactly `B = P × Q`. Additional conversions SHALL be validated as a whole for target,
lineage, positivity, identity, and duplicate source.

Successful construction SHALL retain the endpoint evidence required by conversion. Subsequent conversion and valuation
SHALL use that retained immutable evidence and SHALL NOT repeat a live registry or catalog-provenance check.

#### Scenario: Settle in quote
- **WHEN** settle and quote are the same trusted asset
- **THEN** quote-settled construction makes quote-to-settle identity and base-to-settle exactly the listing price

#### Scenario: Settle in base
- **WHEN** settle and base are the same trusted asset
- **THEN** base-settled construction makes base-to-settle identity and quote-to-settle exactly the reciprocal price

#### Scenario: Reject a nonidentity settlement anchor truthfully
- **WHEN** settle is base or quote and checked dual anchors supply a nonidentity same-asset conversion
- **THEN** construction fails with a diagnostic preserving the supplied source, target, and coefficient

#### Scenario: Settle in a third asset from a scalar anchor
- **WHEN** one positive quote-to-settle or base-to-settle rational coefficient is supplied through a scalar anchor
  constructor for a third settlement asset
- **THEN** construction establishes the corresponding endpoint-typed rate and derives the other settlement rate exactly
  through the price

#### Scenario: Preserve a typed derived anchor
- **WHEN** caller-owned typed arithmetic produces a correctly endpoint-typed settlement rate
- **THEN** the typed pure entry point accepts it without scalar erasure and constructs the same market state as the
  equal scalar coefficient

#### Scenario: Settle in a third asset from one anchor
- **WHEN** one positive quote-to-settle or base-to-settle rate is supplied for a third settlement asset
- **THEN** the other settlement rate is derived exactly through the price

#### Scenario: Reject incoherent conversions
- **WHEN** independently supplied anchors imply a quote-per-base rate different from the grid-valid price
- **THEN** construction fails with the contradictory exact values preserved

#### Scenario: Validate plural additional conversions
- **WHEN** a caller supplies zero, one, or many additional settle-targeted conversions
- **THEN** construction accepts the coherent immutable vector and reports independently detectable invalid or duplicate
  sources deterministically

#### Scenario: Require an additional fee conversion
- **WHEN** a quantity uses an asset other than base, quote, or settle
- **THEN** settlement conversion succeeds only when the market state contains an explicit checked conversion for that
  asset

#### Scenario: Convert through the owning market state
- **WHEN** a caller asks a market state to convert an exact quantity from a trusted source asset
- **THEN** that state applies its own retained checked settle-targeted conversion or returns a typed missing-conversion
  failure

#### Scenario: Keep catalog updates out of valuation
- **WHEN** the live catalog publishes a later revision after a market state was constructed
- **THEN** conversion through that state retains its original immutable asset endpoints and result

### Requirement: Universal exact contract valuation
Pure valuation SHALL calculate exact settle value per unit of position as:

```text
basePerPosition.andThen(baseToSettle)
  + quotePerPosition.andThen(quoteToSettle)
```

It SHALL calculate position value by applying that endpoint-typed rate to the exact signed position quantity, including
the instrument lot quantum and coordinate. It SHALL calculate `PricePnl` as exit position value minus entry position
value for the same instrument and signed position. `PricePnl` SHALL retain the `InstrumentId` and exact
`Quantity[settle.D]`; it SHALL NOT be represented as an unqualified scalar or quantized to a settlement, wallet, or
storage grid.

#### Scenario: Include lot count and quantum in valuation
- **WHEN** a position contains `n` lot coordinates and each lot represents exact position quantum `q`
- **THEN** valuation applies settle-per-position to exact signed quantity `n × q`

#### Scenario: Calculate long and short PnL symmetrically
- **WHEN** equal-magnitude long and short positions are valued at the same entry and exit market states
- **THEN** their exact `PricePnl` quantities are negations

#### Scenario: Preserve off-grid inverse PnL
- **WHEN** inverse-style valuation produces a rational settle amount not on the settlement asset's storage grid
- **THEN** `PricePnl` retains that exact quantity without rounding

#### Scenario: Reject foreign valuation values
- **WHEN** an explicit instrument, position, entry state, or exit state carries a different `InstrumentId`
- **THEN** valuation returns a typed mismatch before composing quantities

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
The instrument-economics artifact SHALL optimize its domain model for benign callers and accidental-error prevention.
Its public contract SHALL NOT claim resistance to caller-defined same-package code, hostile Java or JVM bytecode,
implementation subclassing, reflection, or constructor-bypassing deserialization. Direct constructors MAY be visible
where field types express all local invariants; numeric refinement, grid membership, reference-data coherence, and
aggregate instrument identity SHALL remain protected by documented smart constructors and checked aggregate
boundaries.

This trust decision SHALL NOT weaken the quantities artifact's dimension grammar, carrier construction authority,
runtime witness identity, grid provenance, or serialization boundary, and SHALL NOT weaken reference-data handle
construction authority or lineage. Instrument-economic values SHALL NOT be advertised as a stable Java-serialization
persistence format.

#### Scenario: Construct a structurally valid domain alternative directly
- **WHEN** a benign caller constructs a closed economic alternative whose field types already express all local
  invariants
- **THEN** it needs no owner authority or JVM issuance token

#### Scenario: Retain checked economic boundaries
- **WHEN** a caller supplies invalid numeric input, incoherent handles, a wrong grid dimension, contradictory
  conversions, or inconsistent aggregate instrument identities
- **THEN** the corresponding smart constructor or aggregate boundary returns a typed failure

#### Scenario: Keep foundational authority unchanged
- **WHEN** instrument economics adopts the trusted-client boundary
- **THEN** callers gain no new way to manufacture quantity carriers, dimension witnesses, grid identity, reference-data
  handles, or issuer lineage

#### Scenario: Keep quantities authority unchanged
- **WHEN** instrument economics adopts the trusted-client boundary
- **THEN** callers gain no new way to manufacture quantity carriers, dimension witnesses, anonymous grid authority, or
  runtime dimension evidence

#### Scenario: Make no Java-serialization persistence promise
- **WHEN** a caller needs durable economics persistence
- **THEN** it uses a separately specified codec and schema rather than Java object serialization

### Requirement: Settlement conversions preserve typed endpoints
A settle-targeted conversion SHALL retain its trusted source asset, target settlement asset, and an endpoint-typed
`Rate[source.D, settle.D]`. Its scalar coefficient MAY remain observable as a projection of that rate, but the
conversion SHALL NOT discard its endpoint relationship and later reconstruct the typed rate from an untyped stored
scalar.

A market state SHALL retain base-to-settle, quote-to-settle, and additional conversions in endpoint-safe form. Dynamic
selection of a heterogeneous conversion SHALL validate immutable source identity and runtime dimension evidence once
at the selection boundary and then apply the retained typed rate. It SHALL NOT infer grid identity from dimension
equality, consult a catalog, or grant callers new quantity-construction or proof authority.

Contract valuation, position valuation, price PnL, fee conversion, fee aggregation, net PnL, and downside-risk
calculations SHALL preserve `Rate`, `Quantity`, and domain contribution types through arithmetic. Raw scalar projections
SHALL be limited to explicit validation, ordering, diagnostics, and coefficient observation; they SHALL NOT become the
intermediate representation of the calculation.

#### Scenario: Retain an additional conversion rate
- **WHEN** a caller constructs an additional source-to-settle conversion from an endpoint-typed rate
- **THEN** the conversion exposes that same source-to-settle rate and exact coefficient

#### Scenario: Reject reversed settlement endpoints statically
- **WHEN** downstream Scala supplies a settle-to-source rate where a source-to-settle rate is required
- **THEN** compilation fails instead of accepting the coefficient and relabeling the result

#### Scenario: Compose contract legs through settlement rates
- **WHEN** base-per-position and quote-per-position rates are valued under coherent base-to-settle and quote-to-settle
  rates
- **THEN** settle-per-position is the typed sum of the two composed position-to-settle rates with the exact established
  coefficient

#### Scenario: Apply a typed conversion after runtime lookup
- **WHEN** an identified source quantity matches an immutable conversion entry
- **THEN** the market state returns the exact settlement quantity by applying that entry's retained source-to-settle rate

#### Scenario: Preserve grid provenance independence
- **WHEN** source and target dimensions are runtime-compatible but a stable-grid or issuer-lineage claim is unrelated
- **THEN** typed conversion does not treat dimension compatibility as grid identity or shared reference-data provenance

#### Scenario: Keep raw arithmetic from becoming the kernel
- **WHEN** a calculation composes payoff, market conversion, position, price PnL, or fee contribution
- **THEN** the implementation retains the available typed algebra through the composition rather than calculating in
  raw `Rational` and reconstructing the proof afterward

### Requirement: Pure instrument economic surface
An `Instrument` SHALL be an immutable value retaining only its assembled identity, asset roles, listing grids, and
contract payoff. The instrument-economics artifact SHALL expose pure smart constructors with the domain values they
construct and explicit pure valuation operations that consume an `Instrument`; it SHALL NOT expose order, scenario,
fee-schedule, sizing, catalog, persistence, or runtime services through the instrument.

Operations whose inputs already carry all required proofs SHALL be total. Operations that refine external numeric
input or reconcile ordinary runtime instrument identity SHALL return typed domain failures. No operation in this
artifact SHALL fetch market data, consult a live catalog, acquire a lock, require an effect type, or perform I/O.

#### Scenario: Inspect an instrument as a value
- **WHEN** a caller receives an assembled instrument
- **THEN** it can inspect the retained identity, roles, listing grids, and payoff without initializing any service or
  runtime capability

#### Scenario: Construct a value through its domain boundary
- **WHEN** a caller needs lots, a price, a market state, or a fee denomination
- **THEN** it uses the corresponding pure domain constructor with an explicit instrument and receives either a refined
  value or a typed failure

#### Scenario: Keep downstream policy out of the instrument
- **WHEN** downstream Scala has only an `Instrument`
- **THEN** the instrument exposes no order factory, scenario evaluator, fee schedule, risk sizer, or effectful operation

#### Scenario: Evaluate without infrastructure
- **WHEN** a caller supplies one instrument and already validated economic inputs
- **THEN** valuation completes deterministically without a catalog, registry, clock, stream, transaction, metric,
  persistence store, or market-data service

### Requirement: Instrument exposes direct role-dimension aliases
Each assembled `Instrument` SHALL expose `PositionD`, `BaseD`, `QuoteD`, and `SettleD` as exact compile-time aliases of its retained position, base, quote, and settlement role dimensions respectively. The aliases MUST introduce no replacement dimension, runtime representation, lookup, cast, match type, or independently constructible evidence.

Existing instrument-dependent grids, payoff rates, lots, positions, prices, market states, price P&L, and net P&L SHALL preserve their current runtime identity, values, refinements, grid provenance, and dependent result types when named through those aliases.

#### Scenario: Use a direct alias interchangeably with its role projection
- **WHEN** downstream Scala supplies a value typed with one of an instrument's direct dimension aliases where the corresponding `instrument.roles` dimension is required, or vice versa
- **THEN** the program compiles without an explicit type argument, cast, runtime lookup, structural refinement, or duplicated local alias

#### Scenario: Preserve existing dependent members
- **WHEN** a caller observes an instrument's grids, payoff rates, lots, positions, prices, market states, or P&L types through the direct aliases
- **THEN** their endpoints and results remain tied to the same retained role dimensions, instrument identity, and grid evidence as before

#### Scenario: Reject an incompatible instrument or role
- **WHEN** downstream Scala attempts to use a value from a different instrument dimension or a different role dimension where one direct alias is required
- **THEN** compilation fails rather than widening, retagging, or checking the mismatch only at runtime

### Requirement: Dimension aliases preserve instrument-economics ownership
The direct aliases SHALL remain part of the pure instrument-economics value surface. They MUST NOT add order, execution, scenario, fee-policy, risk, codec, application, or runtime behavior to `Instrument`, and the instrument-economics artifact MUST NOT gain a dependency on any of those consumers.

#### Scenario: Compile instrument economics in isolation
- **WHEN** the instrument-economics artifact and a completed-artifact alias client are compiled with only their allowed quantity, reference-data, Scala, and existing admitted support dependencies
- **THEN** the aliases and existing instrument-dependent members compile without any downstream trading, codec, application, or runtime artifact

#### Scenario: Inspect the instrument value surface
- **WHEN** a caller has only an assembled `Instrument`
- **THEN** it gains concise names for the four retained dimensions but no order factory, execution lifecycle, scenario evaluator, fee policy, risk sizer, codec, application capability, or runtime service

### Requirement: Market-state modes accept optional additional conversions

Each of the eight market-state construction modes SHALL expose one direct operation whose additional settlement
conversions default to an empty immutable vector: quote-settled, base-settled, quote anchor, base anchor, both anchors,
quote rate, base rate, and both rates. The existing operation names, other explicit inputs, dependent result types,
and checked construction behavior SHALL remain available without duplicated overload implementations.

Omitted and explicitly empty additional conversions SHALL produce equal observable states or exactly equal
`MarketStateViolations`. Non-empty additional conversions SHALL retain their established validation and input order.
The supported Scala source calls SHALL compile with inferred dimensions for omitted, explicit-empty, and non-empty
conversions; no ordinary Java source or binary compatibility promise is added.

#### Scenario: Omit additional conversions in every mode

- **WHEN** a caller invokes any of the eight direct modes with its existing required arguments
- **THEN** the result equals the same operation with an explicitly empty additional-conversion vector, including exact
  conversion rates, retained endpoints, conversion-source order, and complete failures

#### Scenario: Supply additional conversions explicitly

- **WHEN** a caller invokes any direct mode with a non-empty vector of settlement conversions
- **THEN** it preserves the current additional conversions on success or returns all applicable ordered construction
  violations on failure without ambiguity introduced by the default

#### Scenario: Compile all supported direct argument forms

- **WHEN** downstream Scala calls all eight modes with omitted, explicit-empty, and non-empty additional conversions
- **THEN** each call compiles against the completed instrument-economics artifact with its established exact return type

### Requirement: Market-state construction binds one exact instrument

Instrument economics SHALL expose one immutable market-state construction scope bound to one exact assembled
instrument. It SHALL cover the same eight direct modes with the same per-operation arguments and empty additional-
conversion defaults while preserving the captured instrument's price, base, quote, settlement, rate, conversion,
violation, and state types.

The scope SHALL retain the captured singleton relationship so ordinary callers need no repeated instrument argument,
explicit role-dimension type arguments, structural refinements, unchecked casts, or duplicated local aliases.
Capturing the instrument SHALL NOT cache states or capture changing prices, rates, anchors, or additional conversions.

#### Scenario: Reuse the scope across all construction modes

- **WHEN** a caller binds one instrument and invokes quote-settled, base-settled, quote-anchor, base-anchor, dual-anchor,
  quote-rate, base-rate, and dual-rate construction with their ordinary inputs
- **THEN** every result retains that instrument's exact market-state type or the existing market-state violations,
  including typed failures when a settlement relationship does not support the selected mode

#### Scenario: Name and retain the captured endpoints

- **WHEN** downstream Scala passes a captured instrument's price, correctly directed rates, and settlement conversions
  through one bound scope
- **THEN** its inputs and results are interchangeable with the corresponding instrument-dependent types without
  explicit dependent type arguments or casts

#### Scenario: Construct independent states in any invocation order

- **WHEN** the same scope is reused for equal or different market inputs, including concurrent calls
- **THEN** successful calls construct independent immutable states, no prior result changes, and each observable result
  equals the corresponding direct construction regardless of invocation order

### Requirement: Bound market-state construction preserves exact checked semantics

Direct and bound market-state construction SHALL share the existing implementation of each calculation and check.
They SHALL preserve exact rational conversion derivation, the equation `baseToSettle = price × quoteToSettle`,
positive and identity conversion rules, retained typed endpoints, checked price-grid guarantees, instrument identity,
asset identity and lineage, additional-conversion handling, and deterministic `MarketStateViolations`.

Independent violations SHALL retain their existing accumulation and ordering, and dependent coherence checks SHALL
remain conditional on valid anchors. Binding SHALL NOT move per-operation validation to scope creation, weaken
runtime checks for dimension-compatible foreign values, round values, infer routes, or consult a catalog.

#### Scenario: Derive exactly equivalent anchor and rate results

- **WHEN** coherent quote/base/third-asset settlement inputs are supplied through direct and scoped scalar or
  endpoint-typed construction
- **THEN** their retained price, exact rates, settlement identity, conversion-source order, additional conversions,
  and conversion/valuation results agree

#### Scenario: Preserve full deterministic violations

- **WHEN** supported invalid inputs combine non-positive or nonidentity anchors, incoherent anchors, foreign
  instrument identities, or invalid and duplicate additional conversions
- **THEN** the scoped and direct paths report the same complete ordered violations with unchanged locations and
  original values, and first-error projection selects the same head

#### Scenario: Sequence anchor coherence from valid anchors

- **WHEN** supplied anchors are invalid and would also fail the conversion equation
- **THEN** both paths retain the existing anchor-validity failures without inventing a dependent incoherence failure

#### Scenario: Preserve price-grid and immutable endpoint evidence

- **WHEN** an exact grid-valid price and checked settlement conversions are supplied to either construction path
- **THEN** construction preserves their refinement and endpoint evidence; off-grid price construction still fails at
  the price boundary, and later catalog changes do not alter an already constructed state

#### Scenario: Retain runtime rejection for statically compatible foreign data

- **WHEN** a price or additional conversion has compatible static dimensions but a foreign instrument identity,
  settlement identity, or reference-data lineage through a supported construction path
- **THEN** scoped construction preserves the direct operation's typed rejection and complete diagnostic order

### Requirement: Market-state scopes preserve compiler boundaries and ownership

Completed-artifact Scala clients SHALL demonstrate concise reuse of the market-state scope across all eight modes,
including anchor and rate construction, and SHALL reject incompatible prices, reversed or incompatible rate endpoints,
and conversions with an incompatible settlement dimension at the intended expression.

The scope SHALL remain owned by pure instrument economics and SHALL acquire no downstream trading, codec,
application, effect, registry, or runtime dependency. `SettlementConversion` construction SHALL remain separately
owned and usable, and `Instrument` SHALL remain a value without market-construction methods.

#### Scenario: Compile and run a concise pure client

- **WHEN** a downstream client binds an assembled instrument and uses its scope against the completed pure economics
  artifact and allowed dependencies
- **THEN** anchor and rate calls compile and run with exact dependent results without explicit dimension type
  arguments, role projections at the call sites, unchecked casts, or downstream artifacts

#### Scenario: Reject each incompatible input with an independently valid prelude

- **WHEN** a client supplies an incompatible price, reversed or foreign-endpoint rate, or wrong-settlement conversion
- **THEN** compilation fails at each marked invalid expression with the relevant type diagnostic while its shared
  prelude and nearby valid calls compile, without compiler-internal errors

#### Scenario: Keep conversion construction with its existing owner

- **WHEN** a caller needs a conversion from an additional source asset to settlement
- **THEN** it constructs that value through the existing settlement-conversion boundary and passes it explicitly to
  the direct or scoped market operation; the market scope exposes no conversion factory or catalog service

#### Scenario: Retain the pure dependency boundary

- **WHEN** the instrument-economics artifact and market-state scope are inspected and compiled in isolation
- **THEN** they require no order, execution, scenario, fee-policy, risk, codec, application, runtime, effect, or live
  registry dependency, and reusing the scope introduces no coordination or mutable shared state

## Purpose
Defines validated trading instruments and the exact, grid-aware market inputs needed to value signed positions uniformly in an instrument's settlement asset.
