## ADDED Requirements

### Requirement: Proof-carrying instrument definition validation
The economics artifact SHALL distinguish a raw `Definition` from a `ValidatedDefinition` produced only by the instrument-definition validation boundary. A `ValidatedDefinition` SHALL retain the raw identity, roles, listing rules, and payoff together with checked evidence that the position grid has the exact position dimension, the price grid has the exact quote-per-base dimension, and the payoff endpoints are those exact roles. Constructing an `Instrument` from a `ValidatedDefinition` SHALL be total and SHALL NOT repeat fallible registry, grid, role, or payoff checks.

The validation boundary SHALL expose domain-specific definition violations and SHALL return every independently detectable violation in a deterministic order. Its public invalid result SHALL contain a non-empty ordered domain collection and SHALL NOT expose a validation-library collection type. Checks that require evidence produced by an earlier successful stage SHALL run only after that stage succeeds and SHALL NOT fabricate dependent violations when the prerequisite evidence is absent.

The existing fail-fast `Instrument.create(Definition)` boundary SHALL remain available. It SHALL use the same validation rules and deterministic ordering, return the first corresponding `EconomicsError`, and produce the same successful `Instrument` as validation followed by total construction.

#### Scenario: Validate a coherent raw definition
- **WHEN** a raw definition has coherent roles, registry provenance, grids, and at least one nonzero payoff term
- **THEN** validation returns a `ValidatedDefinition` whose checked grid and payoff evidence can construct the final `Instrument` without another fallible narrowing step

#### Scenario: Accumulate independent definition violations
- **WHEN** one raw definition independently has contradictory component roles, equal base and quote assets, and an empty payoff
- **THEN** the accumulating boundary returns all applicable domain violations in stable order rather than only the first

#### Scenario: Do not invent dependent evidence failures
- **WHEN** a prerequisite registry or dimension check fails and later evidence cannot be soundly produced
- **THEN** validation reports the observable prerequisite violations and does not execute checks that require the missing evidence

#### Scenario: Preserve deterministic fail-fast construction
- **WHEN** the same invalid raw definition is supplied to the accumulating validator and to `Instrument.create`
- **THEN** `Instrument.create` returns the domain error corresponding to the first accumulated violation according to the shared stable ordering

#### Scenario: Keep checked casts private
- **WHEN** downstream Scala validates and constructs an instrument
- **THEN** it neither performs a cast nor receives public authority to retag a grid, payoff, dimension, or registry witness

### Requirement: Settlement conversions preserve typed endpoints
A settle-targeted conversion SHALL retain its registered source asset, target settlement asset, and an endpoint-typed `Rate[source.D, settle.D]`. Its scalar coefficient MAY remain observable as a projection of that rate, but the conversion SHALL NOT discard its endpoint relationship and later reconstruct the typed rate from an untyped stored scalar.

A market state SHALL retain base-to-settle, quote-to-settle, and additional conversions in endpoint-safe form. When converting a heterogeneous source quantity, it SHALL first validate source identity, registry provenance, and runtime dimension identity, then apply the corresponding typed rate. It SHALL NOT infer grid identity from dimension equality or grant callers new quantity-construction or proof authority.

Contract valuation, position valuation, price PnL, fee conversion, fee aggregation, net PnL, and downside-risk calculations SHALL preserve `Rate` and `Quantity` types through arithmetic. Scalar observations SHALL be used only for explicit validation, ordering, diagnostics, or public coefficient observers, and the numerical results SHALL remain exactly equal to the established rational formulas.

#### Scenario: Retain an additional conversion rate
- **WHEN** a caller constructs an additional source-to-settle conversion from an endpoint-typed rate
- **THEN** the resulting conversion exposes that same source-to-settle rate and its exact coefficient

#### Scenario: Reject reversed settlement endpoints statically
- **WHEN** downstream Scala supplies a settle-to-source rate where a source-to-settle rate is required
- **THEN** compilation fails instead of accepting the coefficient and relabeling the result

#### Scenario: Compose contract legs through settlement rates
- **WHEN** base-per-position and quote-per-position rates are valued under coherent base-to-settle and quote-to-settle rates
- **THEN** settle-per-position is the typed sum of the two composed position-to-settle rates and has the exact established coefficient

#### Scenario: Apply a typed conversion after runtime lookup
- **WHEN** a quantity's registered source identity and runtime dimension match a heterogeneous conversion entry
- **THEN** the market state returns the exact settlement quantity by applying that entry's source-to-settle rate

#### Scenario: Preserve grid provenance independence
- **WHEN** source and target dimensions are runtime-compatible but a grid or registry provenance claim is unrelated
- **THEN** typed conversion does not treat dimension compatibility as grid identity or registry ownership
