## ADDED Requirements

### Requirement: Execution authority is semantic rather than constructor-secret

Execution identities, commands, source facts, ordering evidence, lifecycle states, observations, and replay results
SHALL be created and transformed through statically callable owner-defined operations. Hidden constructors or dynamic
private access SHALL NOT be treated as their authority. Each strengthening transition SHALL establish the required
identity scope, command/fact shape, source ordering, duplicate/conflict classification, reference resolution,
completeness, cancellation, lineage, and exact-exposure predicates before returning authoritative state.

#### Scenario: Record a source fact

- **WHEN** a source fact is supplied to a lifecycle transition
- **THEN** its qualified identity, target scope, payload, ordering evidence, duplicates, conflicts, and references are
  classified before state is updated

#### Scenario: Replay equivalent evidence

- **WHEN** the same authoritative sequenced facts are delivered in different network orders
- **THEN** replay produces equivalent lifecycle authority and exposure without constructor provenance contributing
  semantics

#### Scenario: Preserve incomplete knowledge

- **WHEN** ordering, acknowledgement, referenced facts, or completeness authority is absent or conflicting
- **THEN** the lifecycle returns the corresponding typed uncertainty or diagnostic rather than manufacturing stronger
  evidence

