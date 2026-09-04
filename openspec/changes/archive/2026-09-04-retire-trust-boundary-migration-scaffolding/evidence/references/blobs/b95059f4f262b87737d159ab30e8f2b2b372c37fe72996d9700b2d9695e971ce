## MODIFIED Requirements

### Requirement: Static construction expresses in-process collaboration

Production owners SHALL use ordinary static Scala/JVM calls for construction and cross-owner observation. Production
and benchmark source MUST NOT use method handles, reflective private constructors, or reflective private-member access
to simulate an in-process security boundary. Direct constructors MAY be visible when their field types express every
local invariant; values requiring validation SHALL retain domain-named checked factories, and consumers that strengthen
authority SHALL establish their required semantic predicates.

The repository SHALL enforce the prohibited-reflection rule as a zero-tolerance invariant over all production and
benchmark Scala and Java source. The enforcement SHALL NOT contain an allowance baseline, grandfathered per-file
counts, or another exception ledger, and the normal CI workflow SHALL run it. A deterministic isolated regression
fixture SHALL prove that one prohibited token makes the enforcement fail without modifying tracked production source.

#### Scenario: Construct a field-valid closed alternative

- **WHEN** every field type makes a closed domain alternative valid by construction
- **THEN** the owner may expose ordinary construction without a reflective factory or hidden issuance token

#### Scenario: Strengthen supplied data into evidence

- **WHEN** an operation turns caller-supplied data into evidence, an assessment, or authoritative state
- **THEN** it checks the required predicate or consumes still-authoritative semantic evidence before returning success

#### Scenario: Inspect production reflection use

- **WHEN** the completed production and benchmark source sets are scanned locally or by the normal CI workflow
- **THEN** every method-handle or reflective private-member construction token is rejected with no allowance mechanism

#### Scenario: Prove zero-tolerance enforcement

- **WHEN** an isolated regression fixture adds one prohibited token to an otherwise clean production source tree
- **THEN** the source guard fails deterministically without changing any tracked repository source
