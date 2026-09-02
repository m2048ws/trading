## ADDED Requirements

### Requirement: Actual execution is a one-way pure lifecycle layer

Actual execution SHALL have one primary owner in the non-empty `trading-execution-lifecycle` artifact under
`trading.execution`. The artifact SHALL depend only on the immutable order model, instrument economics, quantities
available through those boundaries, and admitted pure support. Quantities, reference data, instrument economics, order
model, hypothetical execution scenarios, fee policy, and risk MUST remain independent of actual execution. Later
application capabilities, runtime interpreters, and boundary codecs MAY consume the lifecycle in that direction, but
their effects and mechanisms MUST NOT move into its pure model.

#### Scenario: Add the first actual execution production body

- **WHEN** authoritative commands, source facts, replay, reconciliation, exposure, and anomalies acquire a coherent implementation and independent verification boundary
- **THEN** they are implemented in `trading-execution-lifecycle` rather than in the root aggregate, order model, hypothetical scenario, application, or runtime modules

#### Scenario: Consume immutable order and instrument values

- **WHEN** actual execution binds a logical execution order or validates exact fill lots and prices
- **THEN** it consumes the established immutable order and instrument-economic values without moving execution state into either upstream owner

#### Scenario: Keep sibling domains independent

- **WHEN** hypothetical scenarios, fee policy, or risk are built and used without actual execution
- **THEN** their production classpaths and APIs do not require `trading-execution-lifecycle`

#### Scenario: Add a later effectful execution workflow

- **WHEN** an application capability or runtime interpreter submits, cancels, or reconciles against an external execution source
- **THEN** it consumes pure execution commands and transitions while concrete effects, clients, concurrency, buffering, and resources remain in application/runtime ownership

#### Scenario: Add later durable execution records

- **WHEN** execution commands and facts receive canonical durable representations
- **THEN** boundary codecs depend on lifecycle-owned reconstruction while the lifecycle artifact remains independent of parsers, schemas, envelopes, and storage
