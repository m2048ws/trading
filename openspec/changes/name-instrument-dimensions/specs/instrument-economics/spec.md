## ADDED Requirements

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
