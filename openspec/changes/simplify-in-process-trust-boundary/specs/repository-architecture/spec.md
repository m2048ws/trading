## ADDED Requirements

### Requirement: Cooperative code and untrusted data have distinct boundaries

The repository SHALL treat well-typed Scala and ordinary Java application code using documented APIs as cooperative
in-process callers. It SHALL NOT claim that constructor visibility, exact-class checks, synthetic access details,
reflection avoidance, or difficulty instantiating a representation protects against deliberately hostile code already
executing in the same JVM. If mutually untrusted executable code is introduced, its isolation SHALL be designed at an
explicit process, module, class-loader, or sandbox boundary.

Data arriving from wire, persistence, configuration, venue, replay, or another external representation SHALL remain
untrusted until an owning checked boundary validates it. Construction difficulty SHALL NOT substitute for validating
the semantic predicate that distinguishes data from trusted domain evidence.

#### Scenario: Collaborate across in-process owners

- **WHEN** one production owner must construct or observe another owner's value to implement documented behavior
- **THEN** it uses a narrow statically callable operation without reflective private-member access or an issuance token
  whose only purpose is resisting same-JVM callers

#### Scenario: Reject malformed external data

- **WHEN** malformed or incoherent wire, persistence, configuration, venue, or replay data enters the process
- **THEN** the owning decoder, resolver, assembler, or transition returns its typed failure before trusted state is
  produced

#### Scenario: Introduce mutually untrusted executable code

- **WHEN** a future design requires plugins or another executable component not trusted by the application
- **THEN** a separate design establishes a real isolation and authentication boundary instead of relying on domain
  constructor secrecy
