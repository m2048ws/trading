## MODIFIED Requirements

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

#### Scenario: Construct an assembled instrument
- **WHEN** a caller supplies an `InstrumentSpec` whose assembly already established roles, grids, and payoff endpoints
- **THEN** construction returns an `Instrument` retaining those exact trusted components without another fallible step

#### Scenario: Represent a non-currency underlying
- **WHEN** an assembled specification references an index or basket as its underlying
- **THEN** the instrument retains that identity without manufacturing a fifth currency role

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

## REMOVED Requirements

### Requirement: Proof-carrying instrument definition validation
**Reason**: Raw stable-ID resolution and proof-carrying validation now form the dedicated `instrument-assembly` capability; keeping `ValidatedDefinition` inside economics would duplicate the trusted boundary and continue catalog coupling.

**Migration**: Replace `Definition -> ValidatedDefinition -> Instrument` and `Instrument.create(Definition)` with `InstrumentDefinition -> InstrumentSpec -> Instrument`, using one explicit `CatalogSnapshot` at assembly.
