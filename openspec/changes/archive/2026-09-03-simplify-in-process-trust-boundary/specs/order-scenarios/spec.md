## ADDED Requirements

### Requirement: Scenario authority follows from checked semantics

Order and scenario construction SHALL use statically callable domain operations. Constructor secrecy, reflective
issuance, and JVM object identity SHALL NOT establish activation evidence, peg resolution, matched-slice non-emptiness,
scenario validity, or round-trip validity. Before returning a stronger result, the owning operation SHALL establish the
associated evidence shape and values, instrument and order identity, price/grid relationships, non-empty structure,
and exact flatness predicates applicable to that result.

#### Scenario: Replay trigger evidence

- **WHEN** fixed or trailing evidence is replayed against an activation
- **THEN** verification checks both instruction/evidence agreement and trigger satisfaction before a scenario returns

#### Scenario: Rebuild a complete scenario

- **WHEN** supplied assumptions and slices are reconstructed from external data
- **THEN** checked construction rejects associated-shape, identity, grid, pricing, or empty-slice failures without
  relying on the difficulty of instantiating an intermediate representation

#### Scenario: Build a round trip

- **WHEN** entry and exit scenarios are combined
- **THEN** checked construction verifies shared instrument identity and exact flat signed position before returning the
  round trip
