## ADDED Requirements

### Requirement: Boundary codecs expose a Scala 3 source boundary

The supported source API for record construction, versioned encoding, decoding, and checked domain reconstruction
SHALL be Scala 3. Completed-artifact Scala clients, negative compiler fixtures, schemas, golden vectors, semantic laws,
and external wire and null-boundary tests SHALL remain the compatibility evidence. The codec artifact SHALL NOT
promise an ordinary-Java domain API or retain a dedicated dynamic Java compiler/classloader fixture for that purpose.

Java-library use inside the codec implementation, exact decimal conversion, canonical external representations,
schema validation, and Java-object-serialization rejection SHALL remain governed by their existing requirements.

#### Scenario: Use codecs from a completed Scala artifact

- **WHEN** a downstream Scala 3 client compiles against completed boundary-codec artifacts
- **THEN** it can construct supported records and encode or decode versioned representations without implementation
  classpath leakage

#### Scenario: Preserve external compatibility evidence

- **WHEN** the codec compatibility suite runs after ordinary-Java API fixture removal
- **THEN** schemas, goldens, independent validation, semantic round trips, wire/null failures, and dependency checks
  continue to cover the published boundary

#### Scenario: Preserve Java serialization rejection

- **WHEN** a caller attempts Java object serialization of a codec-owned record or decoded dependent package
- **THEN** serialization still fails and the supported durable path remains explicit versioned encoding
