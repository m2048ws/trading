## Purpose

Defines the pure trust boundary that resolves stable instrument-definition identities through one catalog snapshot,
validates their dependent economic relationships, and returns a proof-carrying `InstrumentSpec` for total construction.

## ADDED Requirements

### Requirement: Raw instrument definitions contain stable boundary data
An `InstrumentDefinition` SHALL be an immutable cohesive product containing one `InstrumentId`, one possibly
non-currency `UnderlyingId`, stable `AssetId` values for base, quote, position, and settle roles, full `GridIdentity`
values for the position-lot and quote-per-base price grids, and exact rational base-per-position and
quote-per-position payoff coefficients.

The instrument identity and role set SHALL be stored once rather than repeated inside listing and payoff components.
The definition SHALL contain no trusted `Asset`, `DimensionHandle`, `GridHandle`, catalog lineage, snapshot, registry,
path-dependent dimension, effect, parser result, or venue-specific product-family flag. Text parsing and wire/database
schema concerns SHALL remain outside this in-memory definition.

#### Scenario: Describe an instrument before resolution
- **WHEN** an adapter has stable asset and grid identities plus exact payoff coefficients
- **THEN** it can construct one `InstrumentDefinition` without first obtaining trusted handles

#### Scenario: Exclude contradictory component owners
- **WHEN** listing and payoff data are placed in one definition
- **THEN** they necessarily refer to the single root identity and role-ID product rather than carrying independently
  contradictory role objects

#### Scenario: Keep parsing outside assembly
- **WHEN** source configuration contains malformed numeric text or a venue-specific product-family label
- **THEN** the adapter rejects or normalizes it before constructing the exact product-family-neutral definition

### Requirement: Assembly uses one explicit catalog snapshot
Instrument assembly SHALL be a pure operation of one `InstrumentDefinition` and one immutable `CatalogSnapshot`. It
SHALL resolve every asset and grid only through that snapshot and SHALL NOT capture another snapshot, consult a live
catalog, perform I/O, observe a clock, or fall back to current mutable state.

Every successfully assembled handle SHALL therefore be a member of the selected snapshot and share its catalog lineage.
A concurrent later catalog publication SHALL have no effect on the assembly result or diagnostics.

#### Scenario: Assemble against a fixed revision
- **WHEN** a definition is assembled from revision `10` while revision `11` is concurrently published
- **THEN** all resolution and validation observe revision `10` only

#### Scenario: Reject a later identity in an older snapshot
- **WHEN** a definition names a grid introduced after the supplied snapshot
- **THEN** assembly returns a contextual unknown-grid violation instead of consulting a newer catalog state

#### Scenario: Run assembly deterministically
- **WHEN** the same definition and snapshot are assembled repeatedly
- **THEN** they produce the same ordered errors or semantically identical `InstrumentSpec`

### Requirement: Independent resolution failures accumulate
Assembly SHALL attempt independent resolution of the four asset roles and two full grid identities and SHALL retain
every observable lookup failure in deterministic role/rule order. Each failure SHALL identify its instrument,
definition field or semantic role, requested stable identity, selected snapshot revision, and underlying typed catalog
lookup cause.

Checks that require a resolved handle SHALL not run for that failed branch. A missing asset SHALL not cause a fabricated
dimension or payoff-endpoint error, and a missing grid SHALL not cause a fabricated grid-dimension error. Resolution
errors SHALL be owned by assembly rather than leaked as uncontextualized catalog or economics failures.

#### Scenario: Accumulate missing identities
- **WHEN** base, settle, position-grid, and price-grid identities are independently absent
- **THEN** assembly returns all four contextual resolution violations in stable order

#### Scenario: Suppress dependent grid validation
- **WHEN** the price grid cannot be resolved
- **THEN** assembly reports that lookup failure and does not claim that the nonexistent handle has a wrong dimension

#### Scenario: Preserve catalog cause
- **WHEN** snapshot lookup rejects an identity
- **THEN** the assembly violation retains the typed lookup cause without converting it to a string

### Requirement: Structural and dependent economic checks are staged
Assembly SHALL detect independently observable structural violations, including equal base and quote asset IDs and an
economically empty two-leg payoff, whether or not unrelated catalog lookups succeed. After the required handles resolve,
it SHALL check that the position-lot grid dimension equals the position asset dimension and that the price-grid
dimension equals exact quote divided by base.

Only after role and dimension prerequisites succeed SHALL assembly construct endpoint-typed
`Rate[position.D, base.D]` and `Rate[position.D, quote.D]` payoff terms from the exact supplied coefficients. Checked
runtime evidence and any unavoidable narrowing SHALL remain inside the assembly boundary. No mismatch SHALL be repaired
by quantization, inferred asset identity, grid substitution, or an unchecked public cast.

#### Scenario: Reject equal base and quote
- **WHEN** one raw definition names the same `AssetId` for base and quote
- **THEN** assembly returns the structural violation even if another independent identity is also missing

#### Scenario: Reject an empty payoff
- **WHEN** both exact payoff coefficients are zero
- **THEN** assembly reports that the instrument has no economic value and returns no `InstrumentSpec`

#### Scenario: Reject a wrong position grid
- **WHEN** the resolved position-lot grid belongs to a dimension different from the resolved position asset
- **THEN** assembly returns a typed position-grid dimension violation without retagging the grid

#### Scenario: Reject a wrong price grid
- **WHEN** the resolved price grid dimension is not canonical quote divided by base
- **THEN** assembly returns a typed price-grid dimension violation preserving expected and supplied keys

#### Scenario: Construct typed payoff endpoints
- **WHEN** all handles and dimensions are coherent and at least one payoff coefficient is nonzero
- **THEN** assembly retains exact rates from position to base and position to quote without raw-coefficient relabeling

### Requirement: Assembly errors are non-empty and deterministic
The accumulating assembly API SHALL return `InstrumentAssemblyErrors` or an equivalent domain-owned non-empty ordered
collection whose elements form a closed `InstrumentAssemblyViolation` hierarchy. Public diagnostics SHALL not expose a
Cats validation collection, exception, `null`, boolean flag, raw tuple, or registry-specific error hierarchy.

The boundary SHALL also expose one explicitly named fail-fast projection derived from the same rules and stable order.
It SHALL return the first assembly violation and SHALL produce the same successful `InstrumentSpec` as the accumulating
entry point. No separate duplicated validation implementation is permitted.

#### Scenario: Order mixed violations deterministically
- **WHEN** one definition has several lookup, structural, and dependent violations
- **THEN** repeated assembly returns the same domain violations in the documented stage/rule order

#### Scenario: Project the first violation
- **WHEN** the accumulating entry point returns multiple violations
- **THEN** the fail-fast entry point returns exactly the first of that collection

#### Scenario: Agree on success
- **WHEN** a definition is coherent
- **THEN** accumulating and fail-fast entry points return semantically identical `InstrumentSpec` values

### Requirement: InstrumentSpec carries resolved proofs
Successful assembly SHALL return a sealed, constructor-private `InstrumentSpec` retaining the source instrument and
underlying identities, trusted base/quote/position/settle `Asset` handles, the position-lot
`GridHandle[position.D]`, the quote-per-base `GridHandle[Divide[quote.D, base.D]]`, and exact typed
base-per-position and quote-per-position rates with those same role endpoints.

The specification SHALL retain enough dependent structure that downstream instrument construction and economics do not
re-resolve IDs, compare issuer lineage, re-check grid dimensions, reconstruct rate endpoints from `Rational`, or cast
the handles again. Its public observers SHALL not expose a general retagging proof, lineage token, mutable snapshot, or
catalog lookup capability.

The trusted result SHALL be named `InstrumentSpec`. The API SHALL NOT require parallel `UnresolvedInstrument` and
`ResolvedInstrument` wrappers when `InstrumentDefinition` and the controlled assembly function already identify the
boundary.

#### Scenario: Retain exact role relationships
- **WHEN** assembly succeeds
- **THEN** the specification's grids and payoff rates are statically tied to its exact path-dependent asset roles

#### Scenario: Reject caller construction
- **WHEN** downstream Scala attempts to construct an `InstrumentSpec` from equal-looking handles and coefficients
- **THEN** no public constructor or implementation authority is available

#### Scenario: Avoid proof extraction
- **WHEN** a caller receives an `InstrumentSpec`
- **THEN** it can observe the trusted economic definition but cannot obtain an unrestricted cast, lineage token, or
  arbitrary `SameDimension` producer

### Requirement: Final Instrument construction is total
The final `Instrument` construction boundary SHALL consume only an `InstrumentSpec` and SHALL be total. It SHALL retain
the specification's identity, roles, listing grids, and payoff rates without catalog lookup, live capability access,
fallible issuer/dimension validation, hidden quantization, or repeated narrowing.

Raw `InstrumentDefinition`, `CatalogSnapshot`, and assembly errors SHALL not be accepted by ordinary `Instrument`
construction. The removed `ValidatedDefinition` SHALL not survive as a second proof-carrying representation or
compatibility alias.

#### Scenario: Construct from a specification
- **WHEN** a caller supplies an assembled `InstrumentSpec`
- **THEN** construction returns the final `Instrument` directly and preserves every trusted component

#### Scenario: Reject raw direct construction
- **WHEN** downstream code has only an `InstrumentDefinition`
- **THEN** it must assemble through a snapshot before it can obtain an `Instrument`

#### Scenario: Do not revalidate a specification
- **WHEN** an `Instrument` is constructed from a trusted specification
- **THEN** no catalog or definition error can arise and no successful proof is recomputed

### Requirement: Assembled meaning is stable across catalog updates
An `InstrumentSpec` and `Instrument` SHALL retain the immutable handles and exact payoff meaning selected by their
assembly snapshot. Later catalog revisions, newly registered grid versions, or availability changes SHALL not alter or
invalidate those retained values. Reassembling the same raw definition against a later snapshot MAY succeed with the
same semantic handles or fail/succeed differently only when the named stable identities have different membership in
that explicit snapshot.

#### Scenario: Add a new grid version after assembly
- **WHEN** an instrument was assembled with grid version `1` and version `2` is later registered
- **THEN** the existing specification continues to use version `1` with its original quantum

#### Scenario: Reassemble explicitly
- **WHEN** an application wants a definition to use a new grid version
- **THEN** it changes the raw stable `GridIdentity` and assembles a new specification against an explicit snapshot

#### Scenario: Calculate without catalog access
- **WHEN** pricing, lots, valuation, or PnL consumes an already constructed instrument
- **THEN** no snapshot, live catalog, registry, or lock is required to reaffirm its static definition

### Requirement: Instrument definitions and specifications are not persistence formats
`InstrumentDefinition` SHALL be an in-memory domain command whose durable representation is owned by an explicit
boundary codec. `InstrumentSpec` and `Instrument` SHALL be trusted in-memory values and SHALL fail Java object
serialization through the project-owned unsupported-serialization mechanism. Persistent data SHALL store stable IDs,
exact numeric fields, and an explicit schema version and SHALL reassemble through a selected snapshot after loading.

#### Scenario: Reject specification serialization
- **WHEN** a caller passes an `InstrumentSpec` to `ObjectOutputStream`
- **THEN** serialization fails without persisting path-dependent handles or catalog lineage

#### Scenario: Restore an instrument definition
- **WHEN** an explicit codec loads stable IDs and exact payoff data
- **THEN** it produces an `InstrumentDefinition` that must pass snapshot assembly before entering economics
