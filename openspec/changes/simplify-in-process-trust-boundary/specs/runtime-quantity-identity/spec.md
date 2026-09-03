## ADDED Requirements

### Requirement: Runtime witness authority is checked at supported roots

Runtime dimension witnesses SHALL preserve the one-to-one static/runtime association established by supported literal,
nominal, generative, fresh, algebraic, or checked-recovery roots. Raw keys and caller-selected type arguments SHALL NOT
form a documented unchecked witness-construction path. This semantic guarantee SHALL NOT claim to defend against
deliberate reflection, unsafe bytecode, casts, or constructor bypass by code already running in the JVM.

#### Scenario: Recover a runtime dimension

- **WHEN** a checked boundary resolves a runtime key through an authoritative witness root
- **THEN** the dependent result retains the matching static type and exact runtime key

#### Scenario: Supply inconsistent external identity

- **WHEN** decoded data cannot establish agreement among its runtime key, dimension, grid, and catalog context
- **THEN** checked reconstruction returns a typed failure and no trusted carrier

#### Scenario: Use only supported in-process APIs

- **WHEN** ordinary Scala or Java code uses the documented runtime-witness operations
- **THEN** it cannot independently select contradictory static and runtime identities

