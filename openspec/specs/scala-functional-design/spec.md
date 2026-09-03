# scala-functional-design Specification

## Purpose
Defines the expected Scala 3 and functional-programming standard for discovering domain algebra, preserving invariants, composing validation and effects, and presenting rigorous but readable public APIs.
## Requirements
### Requirement: Algebra is identified before control flow

Design work SHALL first examine whether domain states and operations form meaningful sums, products, refinements, non-empty structures, associative combinations, identities, orderings, traversals, composable transformations, or state transitions. When a lawful structure reflects the domain, the implementation SHALL encode and reuse the weakest abstraction that completely captures it instead of reproducing the behavior with flags, primitive containers, mutation, or ad hoc branching.

#### Scenario: Model mutually exclusive domain states

- **WHEN** a concept has alternatives that cannot validly coexist
- **THEN** the model uses an exhaustive sum type instead of independently set boolean flags or nullable fields

#### Scenario: Aggregate independent contributions

- **WHEN** values have an associative domain combination and a lawful identity
- **THEN** the design exposes that combination and verifies its laws rather than scattering special-case accumulation logic

#### Scenario: No useful law exists

- **WHEN** a proposed algebraic abstraction has no honest domain laws or no compositional consumer
- **THEN** the design uses a direct domain operation and does not introduce the abstraction merely for sophistication

### Requirement: Semantic information remains in types

Domain operations SHALL preserve dimension, grid, refinement, identity, provenance, validation, and endpoint information already established by their inputs. Implementations MUST NOT erase that information into raw numbers, strings, tuples, booleans, or untyped collections and reconstruct it later when a typed operation or evidence-bearing result can carry it forward.

#### Scenario: Compose quantity conversions

- **WHEN** typed rates convert a quantity through one or more endpoints
- **THEN** composition preserves the source and target types without reducing the calculation to raw coefficient arithmetic followed by unchecked retagging

#### Scenario: Preserve validated construction evidence

- **WHEN** validation establishes relationships required by later construction
- **THEN** the successful result packages those relationships so downstream code does not repeat the checks or manufacture replacement evidence

#### Scenario: Cross a representation boundary

- **WHEN** serialization or interoperability requires primitive data
- **THEN** semantic information is erased only in the explicit boundary representation and is restored solely through checked reconstruction

### Requirement: Validation distinguishes independent and dependent checks

Independent structural violations SHALL be evaluated applicatively and accumulated in deterministic order. A check whose meaning or safe execution depends on an earlier successful result SHALL be sequenced fail-fast from that result. Successful validation SHALL return the strongest useful trusted value or evidence, and each failure SHALL be represented by an error owned by the layer able to explain and remediate it.

#### Scenario: Validate independent definition fields

- **WHEN** several fields can each be checked without relying on another field's success
- **THEN** all of their violations are returned together in deterministic order

#### Scenario: Validate a dependent relationship

- **WHEN** a later check needs a witness or normalized value produced by an earlier check
- **THEN** the later check runs only after that prerequisite succeeds and does not invent secondary errors from missing evidence

#### Scenario: Reuse a validated result

- **WHEN** downstream construction receives a proof-carrying validated value
- **THEN** it consumes the established evidence directly rather than repeating the boundary validation

### Requirement: Public domain APIs are total for expected inputs

Public mathematical and domain APIs SHALL represent expected absence, invalidity, conflict, and failure in their result
types. They MUST NOT use `null`, unchecked extraction, sentinel values, or exceptions as ordinary control flow.
Unavoidable partial operations, casts, and mutable mechanisms SHALL be narrowly scoped, protected by a stated semantic
invariant, and unavailable as documented unchecked construction paths. Private visibility or resistance to deliberate
same-JVM access SHALL NOT itself be treated as proof of a domain invariant.

#### Scenario: Expected input is invalid

- **WHEN** a caller supplies an invalid definition, conversion, grid coordinate, or scenario
- **THEN** the API returns a precise typed failure without throwing an expected exception

#### Scenario: Internal type evidence requires a cast

- **WHEN** Scala cannot express a relationship already established by authoritative runtime equality or closed evidence
- **THEN** the cast is isolated immediately behind that checked predicate and tests begin from the least-trusted
  supported input to prove the predicate rejects mismatches

#### Scenario: Runtime state must mutate

- **WHEN** a live interpreter must coordinate changing state
- **THEN** mutation remains encapsulated by the runtime effect abstraction while the state-transition semantics remain
  testable as pure immutable logic where practical

### Requirement: Advanced Scala serves domain semantics

Opaque types, enums, refinements, phantom types, path-dependent types, match types, contextual abstractions, type classes, and higher-kinded interfaces SHALL be introduced only when they prevent a real invalid state, preserve information required downstream, encode a lawful reusable structure, or make an architectural boundary explicit. Their public use SHALL remain ergonomic, and non-obvious safety or inference behavior SHALL be documented and verified.

#### Scenario: Sophisticated internals support a common operation

- **WHEN** an invariant requires advanced type machinery internally
- **THEN** ordinary callers use a small domain-named operation without handling implementation evidence or deeply nested representation types

#### Scenario: A type-class instance is proposed

- **WHEN** generic code will consume a shared lawful behavior across types
- **THEN** the instance agrees with the direct API and is supported by explicit laws

#### Scenario: Only one pure implementation exists

- **WHEN** a pure operation has no environmental capability or meaningful alternative interpretation
- **THEN** it remains a direct function rather than receiving a higher-kinded service interface

### Requirement: Standard composition replaces ad hoc plumbing

Implementations SHALL use established Scala or functional composition for sequencing, traversal, accumulation, non-empty data, and effect composition when its semantics match the domain. A custom abstraction SHALL require a domain distinction or law not adequately represented by the available standard vocabulary.

#### Scenario: Validate every element of a collection

- **WHEN** the same effectful or validated operation must be applied to each element while preserving collection structure
- **THEN** the implementation uses traversal semantics rather than a mutable accumulator and hand-written error plumbing

#### Scenario: Emptiness is invalid

- **WHEN** a calculation or domain object requires at least one element
- **THEN** the input type represents non-emptiness instead of accepting an ordinary collection and repeatedly rejecting empty values

#### Scenario: Standard vocabulary obscures the domain

- **WHEN** exposing a general functional representation would make ordinary domain use harder to understand
- **THEN** a domain-named facade exposes the operation while the lawful representation remains internal

### Requirement: Effect polymorphism is confined to genuine capabilities

Effect-polymorphic interfaces SHALL describe application capabilities whose execution can vary, such as market data, persistence, time, transactions, telemetry, or external communication. Pure entities and calculations MUST NOT acquire an effect parameter merely for uniformity. Durable commands and events SHALL remain inspectable domain data even when effectful workflows produce or consume them.

#### Scenario: Provide multiple execution interpreters

- **WHEN** the same application workflow runs against live infrastructure, a backtest, and deterministic tests
- **THEN** the workflow depends on capability interfaces whose interpreters preserve a shared behavioral contract

#### Scenario: Calculate a deterministic economic value

- **WHEN** all inputs to a valuation are already available and trusted
- **THEN** valuation remains a pure typed function and does not depend on an effect runtime

#### Scenario: Persist a trade command

- **WHEN** a workflow records or transports a durable trading fact
- **THEN** the command or event remains an explicit algebraic data type rather than existing only as an opaque effectful method invocation

### Requirement: Algebraic and type-level claims are verified at their boundary

Every public algebraic abstraction SHALL have tests for its stated laws. Every load-bearing type-level rejection or authority boundary SHALL have downstream positive and negative compiler coverage when ordinary unit tests cannot establish it. Effect interpreters SHALL share contract tests where multiple implementations are promised, and performance-sensitive abstractions SHALL be measured when their cost could affect a hot path.

#### Scenario: Introduce a lawful composition

- **WHEN** a public operation claims associativity, identity, ordering, module, traversal, or another algebraic law
- **THEN** property or discipline tests exercise those laws and compare generic behavior with the direct API

#### Scenario: Reject an invalid program statically

- **WHEN** correctness depends on downstream callers being unable to construct or combine a value
- **THEN** packaged-boundary compiler fixtures include the intended rejection and a nearby valid counterpart

#### Scenario: Add another interpreter

- **WHEN** a capability gains a second production or test interpreter
- **THEN** both interpreters run against the same observable contract suite in addition to interpreter-specific tests

### Requirement: Rigor remains readable at call sites

The repository SHALL target expert, idiomatic Scala 3 internally while keeping common public use explicit, domain-named, and locally understandable. Mathematical terminology SHALL be used where it accurately communicates laws; implementation-level abstraction machinery MUST NOT dominate an API when a domain vocabulary can expose the same semantics honestly.

#### Scenario: Expose category-shaped rate composition

- **WHEN** rates compose associatively through matching endpoints
- **THEN** callers use a plainly named rate-composition operation while documentation and law tests record the deeper algebraic structure

#### Scenario: Review concise but opaque code

- **WHEN** a point-free or highly generic expression makes the domain operation materially harder to identify
- **THEN** review prefers named intermediate concepts or a domain facade without discarding the underlying lawful composition

#### Scenario: Review mathematically sophisticated code

- **WHEN** advanced algebra directly captures a real domain structure and presents an ergonomic supported API
- **THEN** review treats that sophistication as a design strength rather than simplifying it into less informative primitives

### Requirement: Static construction expresses in-process collaboration

Production owners SHALL use ordinary static Scala/JVM calls for construction and cross-owner observation. Production
and benchmark source MUST NOT use method handles, reflective private constructors, or reflective private-member access
to simulate an in-process security boundary. Direct constructors MAY be visible when their field types express every
local invariant; values requiring validation SHALL retain domain-named checked factories, and consumers that strengthen
authority SHALL establish their required semantic predicates.

#### Scenario: Construct a field-valid closed alternative

- **WHEN** every field type makes a closed domain alternative valid by construction
- **THEN** the owner may expose ordinary construction without a reflective factory or hidden issuance token

#### Scenario: Strengthen supplied data into evidence

- **WHEN** an operation turns caller-supplied data into evidence, an assessment, or authoritative state
- **THEN** it checks the required predicate or consumes still-authoritative semantic evidence before returning success

#### Scenario: Inspect production reflection use

- **WHEN** the completed production and benchmark source sets are scanned
- **THEN** no method-handle or reflective private-member construction mechanism is present

