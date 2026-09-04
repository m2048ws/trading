## ADDED Requirements

### Requirement: Execution lifecycle exposes a Scala 3 source boundary

The supported source API for lifecycle identities, commands, facts, transitions, replay, and observations SHALL be
Scala 3. Completed-artifact Scala clients and negative Scala compiler fixtures SHALL verify public construction,
exhaustive consumption, checked authority, and dependency direction. The lifecycle SHALL NOT promise an ordinary-Java
domain construction API or retain a dedicated dynamic Java compiler/classloader fixture for that purpose.

This source boundary SHALL NOT weaken semantic transition tests, external reconstruction, JVM-library integration, or
the requirement that lifecycle values reject Java object serialization where their owning requirements specify it.

#### Scenario: Consume the completed lifecycle artifact

- **WHEN** a downstream Scala 3 client compiles against completed lifecycle artifacts
- **THEN** it can construct supported inputs and exhaustively observe public results without implementation classpath
  leakage

#### Scenario: Reject unsupported authority

- **WHEN** a negative Scala compiler fixture attempts to bypass lifecycle identity, command, fact, or dependency
  boundaries
- **THEN** compilation fails while the nearby supported client remains valid

#### Scenario: Preserve fail-closed serialization

- **WHEN** a caller attempts Java object serialization of a lifecycle value covered by the serialization policy
- **THEN** serialization still fails despite removal of the ordinary-Java API fixture
