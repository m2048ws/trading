## ADDED Requirements

### Requirement: Effect-scoped concurrent live-catalog interpreter
The runtime artifact SHALL provide at least one effect-scoped concurrent in-memory interpreter of the application
`LiveCatalog` capability. Creating the interpreter SHALL establish exactly one fresh catalog lineage and evaluate any
explicit bootstrap batch through the pure catalog model before exposing a usable capability. Invalid bootstrap SHALL
return its typed catalog violations and no usable interpreter. Every snapshot and successful commit during that
interpreter lifetime SHALL belong to its one lineage.

Snapshot capture SHALL return one complete immutable published revision. Commit SHALL execute the normative pure catalog
transition atomically against the latest published state, publish the successor only for a valid non-empty change, and
preserve the specified total order for concurrent commits. No runtime lock, atomic reference, fiber, or mutable state
handle SHALL appear in the application-facing value.

#### Scenario: Create an in-memory catalog
- **WHEN** runtime creates the live-catalog interpreter from a valid bootstrap, then captures snapshots or commits
  batches
- **THEN** every observed snapshot belongs to its one lineage and no internal coordination primitive escapes

#### Scenario: Reject an invalid bootstrap
- **WHEN** the explicit bootstrap batch violates the pure catalog rules
- **THEN** creation returns the same ordered typed violations as the pure model and exposes no live capability

#### Scenario: Race successful commits
- **WHEN** several valid independent batches commit concurrently
- **THEN** their published revisions have one linear order and the final snapshot contains every accepted addition

#### Scenario: Race cancellation with commit
- **WHEN** a caller is cancelled while a batch transition may publish
- **THEN** the catalog remains at either the complete prior revision or the complete published successor and never a
  partial state

#### Scenario: Retry after an unobserved acknowledgement
- **WHEN** a commit publishes but cancellation or failure prevents its caller from observing the response
- **THEN** retrying the identical batch returns the idempotent unchanged outcome at the published state without another
  revision

### Requirement: Live-catalog interpreters share the pure conformance suite
The application/runtime foundation SHALL provide reusable live-catalog contract tests parameterized by interpreter
construction. Every live-catalog interpreter SHALL be checked against the pure model for bootstrap, lookup,
successful publication, idempotence, conflicts, accumulated error ordering, revision/delta behavior, snapshot
coherence, canonical handle relationships, lineage isolation, and concurrent commit ordering where concurrency is
supported.

Interpreter-specific suites SHALL cover construction, cancellation, runtime failure, implementation stress, and
finalization when the interpreter owns releasable resources. Performance-sensitive interpreters SHALL measure snapshot
and commit paths separately; snapshot lookup SHALL remain pure and require no capability call per decoded value.

#### Scenario: Verify the in-memory interpreter
- **WHEN** equivalent bootstrap definitions and batches are evaluated by the pure model and in-memory live interpreter
- **THEN** their typed commit outcomes, revisions, deltas, errors, lookups, and within-lineage handle relationships agree

#### Scenario: Isolate two interpreter instances
- **WHEN** two live-catalog interpreters bootstrap equal visible definitions independently
- **THEN** the conformance suite verifies distinct lineages and rejects cross-instance handle reconciliation

#### Scenario: Measure the data-plane path
- **WHEN** a high-volume decode or replay benchmark uses the live interpreter
- **THEN** it captures one snapshot and measures pure snapshot lookup separately from rare coordinated publication
