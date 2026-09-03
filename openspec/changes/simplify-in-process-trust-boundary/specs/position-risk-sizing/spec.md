## ADDED Requirements

### Requirement: Monotone risk evidence is established without reflective authority

Risk construction, observation, composition, and sizing SHALL use statically callable owner-defined operations.
Successful construction SHALL establish the closed-curve or complete-table predicate that guarantees exact coherent
assessments and monotonicity; hidden constructors, reflective observers, marker identity, and caller assertions SHALL
NOT substitute for that predicate.

#### Scenario: Construct a monotone model

- **WHEN** a caller supplies a supported closed curve or complete finite table
- **THEN** checked construction establishes structure, coherence, coverage, and monotonicity before returning the model

#### Scenario: Compose risk models

- **WHEN** compatible monotone models are added, minimized, maximized, or quantized
- **THEN** the owner-defined static operation preserves instrument identity, settlement dimension, domain cap, exact
  assessment, and the applicable monotonicity law

#### Scenario: Size from a validated model

- **WHEN** maximum-affordable or exhaustive sizing evaluates a model
- **THEN** it observes exact assessments through a static domain operation and returns the same witness assessment used
  for the decision
