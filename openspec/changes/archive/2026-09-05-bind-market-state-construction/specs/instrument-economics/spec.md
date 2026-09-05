## ADDED Requirements

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
