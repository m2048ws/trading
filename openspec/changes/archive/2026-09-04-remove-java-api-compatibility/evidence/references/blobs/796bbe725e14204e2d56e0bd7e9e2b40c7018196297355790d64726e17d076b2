## MODIFIED Requirements

### Requirement: Mature mechanisms are reused and contained by responsibility

Every proposal that adds a third-party dependency SHALL identify the mechanism it supplies, its owning module and
configuration, its public-type exposure, and its compatibility with the repository platform baseline. A maintained
library that satisfies a general infrastructure need SHALL be evaluated before implementing a bespoke equivalent, but
the project SHALL retain ownership of domain meanings, invariants, trusted-value transitions, public domain errors, and
durable schema semantics. A second library vocabulary for the same concern SHALL require an explicit distinct need.

Independently released dependencies SHALL use independently named version coordinates even when their current version
strings happen to match. The repository minimum build and runtime JDK SHALL remain 25 while it uses Scala 3.8.x;
changing that floor or selecting a dependency major that requires a higher floor SHALL be an explicit compatibility
decision.

#### Scenario: Select a concrete runtime mechanism

- **WHEN** a runtime interpreter needs safe concurrent state, cancellation, or managed resources
- **THEN** it may use the selected mature effect runtime inside the runtime layer while the application contract and
  pure state-transition semantics remain independently owned

#### Scenario: Parse a durable boundary record

- **WHEN** a standards parser supplies strict tokenization, locations, duplicates, or resource constraints
- **THEN** the codec layer confines the parser behind immutable project-owned records and errors rather than exposing
  parser objects or allowing generic object mapping to define the wire contract

#### Scenario: Equal version strings belong to separate release trains

- **WHEN** Cats and Algebra or any other independently released libraries currently publish the same version string
- **THEN** the build retains separate coordinates so upgrading one does not silently assert compatibility or ownership
  over the other

#### Scenario: A competing stack is proposed for convenience

- **WHEN** an existing selected effect, JSON, refinement, derivation, or testing vocabulary already satisfies the need
- **THEN** another stack is rejected unless the proposal demonstrates a distinct integration or semantic requirement

### Requirement: Cooperative code and untrusted data have distinct boundaries

The repository SHALL treat well-typed Scala 3 application code using documented domain APIs as cooperative in-process
callers. Ordinary Java source SHALL NOT be a supported domain API contract. Java libraries and JVM platform services
MAY still be integrated behind their owning Scala boundary, and this source-policy distinction SHALL NOT be presented
as an isolation or security boundary. The repository SHALL NOT claim that constructor visibility, exact-class checks,
synthetic access details, reflection avoidance, or difficulty instantiating a representation protects against
deliberately hostile code already executing in the same JVM. If mutually untrusted executable code is introduced, its
isolation SHALL be designed at an explicit process, module, class-loader, or sandbox boundary.

Data arriving from wire, persistence, configuration, venue, replay, Java-library interoperation, or another external
representation SHALL remain untrusted until an owning checked boundary validates it. Construction difficulty SHALL
NOT substitute for validating the semantic predicate that distinguishes data from trusted domain evidence.

#### Scenario: Collaborate across in-process owners

- **WHEN** one production owner must construct or observe another owner's value to implement documented behavior
- **THEN** it uses a narrow statically callable Scala operation without reflective private-member access or an issuance
  token whose only purpose is resisting same-JVM callers

#### Scenario: Integrate a Java library

- **WHEN** a Scala owner consumes a Java library or JVM platform service
- **THEN** that owner validates external values as required and exposes the repository's supported Scala domain surface
  without promising an ordinary-Java domain API

#### Scenario: Reject malformed external data

- **WHEN** malformed or incoherent wire, persistence, configuration, venue, replay, or library data enters the process
- **THEN** the owning decoder, resolver, assembler, or transition returns its typed failure before trusted state is
  produced

#### Scenario: Introduce mutually untrusted executable code

- **WHEN** a future design requires plugins or another executable component not trusted by the application
- **THEN** a separate design establishes a real isolation and authentication boundary instead of relying on domain
  constructor secrecy
