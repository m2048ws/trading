# repository-architecture Specification

## Purpose
Defines the repository-wide ownership, dependency, trust-boundary, effect-boundary, and review rules that keep mathematical, domain, application, and runtime concerns independently coherent as the trading system grows.
## Requirements
### Requirement: Cohesive ownership and directed dependencies

The repository SHALL assign every production concept one primary owning layer. A layer SHALL own the concept's vocabulary, invariants, construction rules, and errors, and lower layers MUST NOT depend on higher-level policy, workflow, codecs, or runtime implementations. Dependencies SHALL remain acyclic and SHALL point from consumers toward the smallest lower-level concepts they require rather than through broad convenience facades.

#### Scenario: A higher-level concern needs instrument values

- **WHEN** an order, execution, fee-policy, or risk feature needs an instrument value
- **THEN** that feature depends on the instrument-economic abstraction while the instrument-economic layer remains independent of that higher-level feature

#### Scenario: A proposed convenience creates a dependency cycle

- **WHEN** a proposal would make two responsibility layers depend on each other
- **THEN** the proposal is rejected or redesigned around a smaller shared lower-level concept before implementation

#### Scenario: One type contains unrelated policies

- **WHEN** a public type begins owning construction or behavior from multiple higher-level concerns
- **THEN** review requires those concerns to become consumers of the lower-level value instead of expanding the value into a service locator

### Requirement: Target responsibility layers

The target architecture SHALL distinguish the following responsibilities even while some begin as packages rather than independently published artifacts:

- quantities own exact scalar arithmetic, dimensions, anonymous mathematical grids, refinements, and lawful mathematical structures;
- reference data own assets, stable catalog identities and versions, trusted handles, pure catalog transitions, and immutable catalog snapshots;
- instrument economics own assembled instrument meaning, listing economics, lots, prices, payoffs, valuation, economic fee values, and P&L;
- the order model owns order instructions, duration, triggers, pegs, visibility, liquidity constraints, and position-effect intent;
- execution scenarios own execution evidence, matched slices, trigger and peg validation, and validated scenario construction;
- fee policy owns venue, account, tier, maker/taker, rebate, and schedule-selection policy;
- risk owns downside measures, sizing policy, candidate search, and future portfolio constraints;
- application code owns effect-polymorphic workflows and ports;
- boundary codecs own wire, database, packed, and replay representations plus checked reconstruction;
- runtime code owns concrete effects, resources, concurrent state, streams, external clients, telemetry, and interpreters.

#### Scenario: Stable grid identity is introduced

- **WHEN** a grid needs a stable external identifier or catalog version
- **THEN** quantities retain only the mathematical grid while reference data owns the stable identity-bearing handle

#### Scenario: Order validation uses instrument prices

- **WHEN** an order rule validates a typed price or lot count
- **THEN** the order layer consumes instrument-economic values without moving order policy into instrument economics

#### Scenario: A live external service is added

- **WHEN** the system adds market data, persistence, time, telemetry, transactions, or another external service
- **THEN** the application layer defines the required capability and the runtime layer supplies concrete interpreters without adding the effect to mathematical or domain values

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

### Requirement: Logical boundaries precede physical modules

Responsibility and dependency boundaries SHALL be established before or alongside code that needs them. The repository MUST NOT create empty or speculative artifacts solely to mirror a desired diagram; a responsibility SHALL become a physical SBT module when a real body of code, dependency boundary, publication boundary, or independent verification need makes that module enforceable and useful.

#### Scenario: A responsibility has no implementation yet

- **WHEN** the target architecture names a future responsibility but no production code currently implements it
- **THEN** the responsibility is documented without creating an empty module

#### Scenario: A responsibility acquires independent code and dependencies

- **WHEN** a responsibility has a coherent production surface and a dependency boundary worth enforcing
- **THEN** its proposal assigns it an explicit package or SBT module and prevents dependencies that violate the target direction

### Requirement: Boundary data becomes trusted domain data once

External identifiers, configuration, packed values, database rows, and wire values SHALL remain boundary representations until an owning resolver, parser, or assembler validates them and issues trusted domain values. A trusted value SHALL carry forward the semantic identity or evidence established at that boundary and MUST NOT require routine re-resolution through a live registry during pure domain calculations.

#### Scenario: Decode persisted quantity data

- **WHEN** persisted identifiers and coordinates enter the process
- **THEN** a boundary decoder uses a coherent catalog view to construct a trusted typed value before passing it to instrument economics

#### Scenario: Calculate P&L from resolved inputs

- **WHEN** P&L receives a validated instrument, positions, market values, and fee contributions
- **THEN** it performs a deterministic pure calculation without a live registry, lock, codec, or external identifier lookup

#### Scenario: Catalog state changes concurrently

- **WHEN** catalog definitions change while a batch or request is being processed
- **THEN** that operation continues against one coherent immutable catalog view rather than observing unrelated state versions between lookups

### Requirement: Pure core and explicit effect shell

Mathematical, reference-data transition, domain, and economic semantics SHALL be expressible as pure transformations over immutable values. Application workflows SHALL describe genuine external capabilities explicitly, and concrete state mutation, resource acquisition, concurrency, cancellation, streaming, transactions, and I/O SHALL remain in runtime interpreters.

#### Scenario: A pure calculation reports a domain failure

- **WHEN** a quantity, catalog transition, validation, or economic calculation can fail without performing I/O
- **THEN** it returns a typed pure result rather than acquiring an effect parameter or throwing an expected exception

#### Scenario: A workflow needs several execution environments

- **WHEN** a workflow must run live, in simulation, and in deterministic tests
- **THEN** it depends on explicit application capabilities that each environment can interpret

#### Scenario: A workflow has no external effect

- **WHEN** a computation is fully determined by its arguments
- **THEN** it remains an ordinary pure function rather than becoming polymorphic in an effect type

### Requirement: Control-plane coordination stays out of data-plane hot paths

Designs SHALL distinguish rare identity, configuration, registration, and publication work from high-volume arithmetic, valuation, event processing, decoding, and replay. Shared coordination MUST NOT be placed in a per-value hot path when immutable snapshots, resolved capabilities, batching, or caching can preserve the same semantics outside that path.

#### Scenario: Decode a high-volume batch

- **WHEN** many records are decoded against the same reference-data generation
- **THEN** the batch captures or receives one coherent immutable view instead of acquiring a shared registry lock for every resolution step

#### Scenario: Poll operational catalog metrics

- **WHEN** metrics inspect catalog size or registration state
- **THEN** their collection does not serialize unrelated high-volume domain calculations

#### Scenario: A coordinated read is unavoidable

- **WHEN** correctness genuinely requires live coordinated state in a hot path
- **THEN** the proposal documents the invariant, expected access pattern, measured cost, and rejected snapshot or caching alternatives

### Requirement: Architecture obligations are reviewable

Every nontrivial proposal SHALL identify its owning responsibility, allowed and forbidden dependencies, boundary and trusted representations, validation stages, error owner, algebraic model, effect placement, law obligations, and hot-path implications. A deliberate exception to this charter MUST be stated as an explicit design change rather than introduced as incidental implementation cleanup.

#### Scenario: Review a new proposal

- **WHEN** a proposal adds or moves a domain capability
- **THEN** its design makes each architecture obligation explicit enough for review to accept, reject, or mark not applicable with rationale

#### Scenario: Implementation discovers a conflicting boundary

- **WHEN** implementing an accepted change appears to require violating the charter or a settled layer boundary
- **THEN** work stops for design escalation instead of silently broadening the implementation

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

### Requirement: Build configuration follows active source ownership

Project-local compiler ordering and compiler-option overrides SHALL exist only when the current sources in that project
require them. When an owning bridge or source-language constraint is removed, its project-local build override and
explanatory scaffolding SHALL also be removed so the project inherits the repository's ordinary build configuration.
Removing an obsolete project override MUST NOT silently change the repository's declared JDK baseline or the settings
still required by other source owners, the JVM platform, or external libraries.

#### Scenario: Retire a removed construction bridge

- **WHEN** a project no longer contains the Java-owned construction bridge that required Java-before-Scala compilation
- **THEN** the project has no bridge-specific compile order, duplicate JDK release option, or stale bridge explanation

#### Scenario: Preserve the repository toolchain contract

- **WHEN** an obsolete project-local compiler override is removed
- **THEN** the project still builds and tests under the repository's authoritative JDK and compiler settings

