## ADDED Requirements

### Requirement: Quantity authority protects supported construction, not hostile same-JVM access

Exact quantity and dimension guarantees SHALL apply to values produced by documented, well-typed construction and
checked reconstruction paths. Those paths SHALL preserve exact coefficients, valid dimension grammar, witness
identity, refinement predicates, and endpoint types. The capability SHALL NOT claim resistance to deliberate casts,
reflection, unsafe bytecode, same-package source, instrumentation, or constructor-bypassing deserialization by code
already executing in the process.

#### Scenario: Use a supported quantity factory

- **WHEN** a well-typed caller constructs or derives a quantity through its documented witness-bearing API
- **THEN** the result preserves exact coefficient and dimension evidence without an unchecked public retagging helper

#### Scenario: Reconstruct external quantity data

- **WHEN** primitive external data is reconstructed as a quantity
- **THEN** a checked codec and owning witness boundary validate its dimension, refinement, and grid relationships before
  returning a typed carrier

#### Scenario: Attempt a deliberate JVM bypass

- **WHEN** code already executing in the JVM deliberately bypasses the supported construction surface
- **THEN** resistance to that action is outside the quantity capability's contract

