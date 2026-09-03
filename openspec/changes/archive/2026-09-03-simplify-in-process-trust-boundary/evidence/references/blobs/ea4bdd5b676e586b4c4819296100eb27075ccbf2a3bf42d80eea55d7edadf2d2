## MODIFIED Requirements

### Requirement: Public domain APIs are total for expected inputs

Public mathematical and domain APIs SHALL represent expected absence, invalidity, conflict, and failure in their result
types. They MUST NOT use `null`, unchecked extraction, sentinel values, or exceptions as ordinary control flow.
Unavoidable partial operations, casts, and mutable mechanisms SHALL be narrowly scoped, protected by a stated semantic
invariant, and unavailable as documented unchecked construction paths. Private visibility or resistance to deliberate
same-JVM access SHALL NOT itself be treated as proof of a domain invariant.

#### Scenario: Expected input is invalid

- **WHEN** a caller supplies an invalid definition, conversion, grid coordinate, or scenario
- **THEN** the API returns a precise typed failure without throwing an expected exception

#### Scenario: Internal type evidence requires a cast

- **WHEN** Scala cannot express a relationship already established by authoritative runtime equality or closed evidence
- **THEN** the cast is isolated immediately behind that checked predicate and tests begin from the least-trusted
  supported input to prove the predicate rejects mismatches

#### Scenario: Runtime state must mutate

- **WHEN** a live interpreter must coordinate changing state
- **THEN** mutation remains encapsulated by the runtime effect abstraction while the state-transition semantics remain
  testable as pure immutable logic where practical

## ADDED Requirements

### Requirement: Static construction expresses in-process collaboration

Production owners SHALL use ordinary static Scala/JVM calls for construction and cross-owner observation. Production
and benchmark source MUST NOT use method handles, reflective private constructors, or reflective private-member access
to simulate an in-process security boundary. Direct constructors MAY be visible when their field types express every
local invariant; values requiring validation SHALL retain domain-named checked factories, and consumers that strengthen
authority SHALL establish their required semantic predicates.

#### Scenario: Construct a field-valid closed alternative

- **WHEN** every field type makes a closed domain alternative valid by construction
- **THEN** the owner may expose ordinary construction without a reflective factory or hidden issuance token

#### Scenario: Strengthen supplied data into evidence

- **WHEN** an operation turns caller-supplied data into evidence, an assessment, or authoritative state
- **THEN** it checks the required predicate or consumes still-authoritative semantic evidence before returning success

#### Scenario: Inspect production reflection use

- **WHEN** the completed production and benchmark source sets are scanned
- **THEN** no method-handle or reflective private-member construction mechanism is present
