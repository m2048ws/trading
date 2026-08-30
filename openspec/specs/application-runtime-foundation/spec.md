# application-runtime-foundation Specification

## Purpose
Defines how pure trading capabilities are composed into effectful application workflows and interpreted by
resource-safe runtimes without allowing infrastructure concerns to leak into the domain.
## Requirements
### Requirement: Application and runtime are distinct dependency boundaries
The system SHALL deliver effect-polymorphic application capabilities and workflows in an application artifact above the
pure domain artifacts. Concrete effects, resources, concurrent state, external clients, stream execution, and
capability interpreters SHALL be delivered in a runtime artifact above application.

The application artifact SHALL depend only on the smallest lower artifacts required by its actual capabilities. Pure
quantity, reference-data, instrument-economic, order, scenario, fee-policy, and risk artifacts SHALL NOT depend on
application or runtime. Application APIs SHALL NOT expose a concrete runtime effect, client, fiber, queue, lock,
transaction handle, tracer, or metrics implementation.

#### Scenario: Compile a pure downstream consumer
- **WHEN** downstream Scala uses any pure domain artifact without application or runtime
- **THEN** its production classpath and public API contain no effect-runtime or infrastructure type

#### Scenario: Implement an application capability
- **WHEN** runtime code supplies a live interpreter for an application port
- **THEN** dependency direction is runtime to application to the required lower domain artifacts

#### Scenario: Add a narrow workflow
- **WHEN** one workflow requires only the live catalog capability
- **THEN** its public construction does not require unrelated market-data, persistence, execution, telemetry, or time
  capabilities

### Requirement: Only genuine external variation becomes an effect-polymorphic port
An application port SHALL represent an operation whose execution depends on an external environment, coordinated live
state, time, persistence, communication, or another interpreter-varying effect. A calculation fully determined by
immutable inputs SHALL remain a pure domain function. The application layer SHALL NOT wrap pure entities or every
domain operation in `F`, define one universal application algebra, or add a port solely to make APIs structurally
uniform.

Before a new market-data, persistence, time, execution, or other port is admitted, its proposal SHALL define its domain-
named operations, typed inputs and expected outcomes, consistency/idempotency contract, and interpreter-observable
semantics. The repository SHALL NOT add placeholder ports whose protocols are only unconstrained type parameters,
strings, maps, callbacks, or generic effect transformations.

#### Scenario: Lift a pure valuation
- **WHEN** a workflow already has an instrument, immutable markets, and assessed fees
- **THEN** it calls the pure valuation function and does not acquire a valuation service in `F`

#### Scenario: Add market-data acquisition later
- **WHEN** a proposal defines which market observation is requested and what freshness, ordering, and absence mean
- **THEN** it may add a market-data application port with interpreters that preserve those semantics

#### Scenario: Reject a speculative service
- **WHEN** no trade record, persistence outcome, or durability contract has been defined
- **THEN** the foundation contains no generic trade repository with semantically empty CRUD methods

### Requirement: Commands, queries, events, and expected outcomes remain explicit data
Durable or externally communicated intent and facts SHALL be represented by closed, inspectable algebraic data owned by
the appropriate domain, application, or boundary layer. Effect-polymorphic ports SHALL consume and produce those values
rather than hiding business meaning inside closures, opaque runtime messages, untyped payloads, or side effects alone.

Expected domain absence, rejection, conflict, or idempotent replay SHALL remain in typed result data. Unexpected
infrastructure failure and cancellation MAY use the effect's failure semantics, but SHALL NOT erase a typed expected
outcome into an exception or universal string error.

#### Scenario: Submit a durable execution command later
- **WHEN** a future execution workflow sends an order command to either a live venue or simulator
- **THEN** both interpreters receive the same explicit command and return the same family of typed observable outcomes

#### Scenario: Retry an idempotent command
- **WHEN** a port's specified idempotency key has already been accepted
- **THEN** an interpreter returns the contract's typed replay outcome rather than inventing a second business fact

#### Scenario: Encounter infrastructure failure
- **WHEN** an external connection fails before a typed business outcome exists
- **THEN** the interpreter fails according to its documented effect contract without manufacturing a domain rejection

### Requirement: Workflows depend on minimal explicit capability products
Application workflows SHALL receive only the capabilities needed for that use case, either as explicit parameters or a
small workflow-specific product. The system SHALL NOT expose a repository-wide service locator, mutable global runtime,
ambient interpreter registry, or capability bag whose fields cover unrelated concerns.

Capability products SHALL be ordinary dependency products; they SHALL NOT own business state already represented by
domain values. Workflow composition SHALL sequence effects while delegating mathematical, validation, and state-
transition semantics to pure functions.

#### Scenario: Test one workflow deterministically
- **WHEN** a workflow depends on two external capabilities
- **THEN** a test supplies exactly two deterministic interpreters and ordinary immutable domain inputs

#### Scenario: Add an unrelated capability
- **WHEN** another workflow later needs a clock or trade store
- **THEN** existing workflow signatures and test environments do not gain that dependency

#### Scenario: Reuse pure semantics
- **WHEN** a workflow commits a catalog batch or computes PnL
- **THEN** it invokes the canonical pure catalog or economics operation instead of reimplementing its rules in `F`

### Requirement: Resource lifetime and cancellation are runtime responsibilities
Every interpreter owning a client, thread, fiber, queue, subscription, file, connection, or other lifecycle-bound value
SHALL expose resource-scoped construction and deterministic release. Application capabilities SHALL remain valid only
within that scope and SHALL not leak raw owned resources to callers.

Cancellation semantics SHALL be documented at each effectful boundary that can publish an external fact. Cancellation
MUST NOT expose a partially applied atomic operation. When publication may complete before its acknowledgement is
observed, the port SHALL define idempotent retry or another reconciliation mechanism rather than claiming impossible
exactly-once delivery.

#### Scenario: Release a runtime
- **WHEN** application scope ends normally, fails, or is cancelled
- **THEN** every acquired interpreter resource is finalized according to its runtime contract

#### Scenario: Cancel around atomic publication
- **WHEN** cancellation races with an operation that publishes one atomic business outcome
- **THEN** observers see either no publication or the complete publication, never a partial result

#### Scenario: Lose an acknowledgement
- **WHEN** publication succeeds but the waiting caller is cancelled before observing the response
- **THEN** the documented idempotency/reconciliation path can determine the existing outcome without duplicating it

### Requirement: Concurrency and streaming compose around semantic operations
Fibers, schedulers, queues, topics, backpressure, merge policy, parallelism, and stream-library values SHALL remain in
runtime wiring by default. Runtime loops SHALL capture or decode inputs at an explicit boundary, invoke an application
workflow or pure batch operation, and publish explicit outputs without moving concurrency primitives into domain values.

If ordering, replay, backpressure, or subscription lifetime is itself observable application behavior, a later proposal
MUST specify it as a dedicated capability contract. Merely needing to process many values SHALL NOT justify a generic
streaming algebra or per-value shared coordination.

#### Scenario: Process an ingress stream
- **WHEN** runtime receives a stream of boundary records
- **THEN** it batches and supervises the loop outside the pure domain and invokes the same semantic operation available
  to non-streaming callers

#### Scenario: Run independent work concurrently
- **WHEN** runtime evaluates independent pure candidates or requests in parallel
- **THEN** it normalizes any contractually ordered result before returning it and does not change domain semantics

#### Scenario: Require ordered delivery later
- **WHEN** a future market-data or execution feed promises a particular ordering and replay model
- **THEN** that promise is added to the feed capability specification and tested across its interpreters

### Requirement: Time distinguishes business observation from runtime scheduling
A workflow SHALL obtain wall-clock time through an explicit clock capability only when a timestamp affects a business
command, event, validity rule, or observable result. Runtime timeout, delay, retry, and scheduling mechanics SHALL use
the runtime's monotonic/temporal facilities and SHALL NOT be represented as domain wall-clock facts.

The system SHALL NOT read a global system clock from pure domain code. A future clock port MUST distinguish wall-clock
instants from monotonic elapsed time and support deterministic interpretation.

#### Scenario: Timestamp a future durable event
- **WHEN** an event's recorded occurrence time is part of its specified meaning
- **THEN** its workflow obtains one explicit wall-clock instant and places it in the event data

#### Scenario: Enforce a timeout
- **WHEN** runtime cancels an external request after a duration
- **THEN** the timeout uses runtime temporal mechanics and does not add a clock dependency to the underlying domain
  operation

#### Scenario: Replay deterministically
- **WHEN** a test or backtest repeats a time-sensitive workflow with the same clock interpreter and inputs
- **THEN** it observes the same business timestamps and domain outcomes

### Requirement: Transaction boundaries express business atomicity
Atomicity SHALL be specified on a domain-named capability operation whenever one business command has one indivisible
durable effect. A generic transaction abstraction SHALL NOT accept an arbitrary already-constructed `F[A]`, because
that form cannot establish that its effects use one transaction or prevent resources from escaping.

If a future workflow must compose several operations in one transaction, its proposal SHALL provide a scoped
transaction program whose repositories/interpreters are bound to the same session and whose result cannot leak that
session. It MUST specify commit, rollback, retry, idempotency, cancellation, isolation, and which external effects are
excluded or coordinated, such as through an outbox.

#### Scenario: Persist one atomic aggregate later
- **WHEN** recording a trade and its durable outbox event is one business commit
- **THEN** the application capability exposes that atomic intent and the runtime interpreter commits both or neither

#### Scenario: Reject a context-free transaction wrapper
- **WHEN** caller code has independently obtained effects that may use unrelated connections
- **THEN** it cannot claim atomicity merely by passing their combined `F` value to a generic wrapper

#### Scenario: Retry after an unknown commit response
- **WHEN** a transactional commit may have succeeded before transport failure
- **THEN** its explicit idempotency/reconciliation contract prevents a duplicate business outcome

### Requirement: Operational telemetry is separate from guaranteed business recording
Tracing and metrics SHALL decorate runtime interpreters and workflow boundaries by default. Their identifiers and labels
SHALL derive from bounded structured observations, SHALL NOT become untyped domain fields, and telemetry backend failure
SHALL NOT change a successful business result unless the capability explicitly specifies otherwise.

An audit record whose persistence is required for business correctness is not telemetry. It SHALL be modeled as
explicit event data and an application persistence/transaction capability with stated durability and failure behavior.

#### Scenario: Add tracing to an interpreter
- **WHEN** runtime decorates a capability with spans and metrics
- **THEN** callers retain the same capability contract and pure domain artifacts gain no telemetry dependency

#### Scenario: Lose best-effort metrics
- **WHEN** a metrics backend is unavailable after a business operation succeeds
- **THEN** the operation's typed business result remains unchanged under the documented best-effort policy

#### Scenario: Require an audit fact
- **WHEN** regulation or business logic requires a record to commit with a trade
- **THEN** the record participates in the specified persistence transaction rather than relying on a trace or metric

### Requirement: Multiple interpreters share observable contract tests
Every application capability SHALL define the observable laws and scenarios that all its interpreters must preserve.
Reusable contract suites SHALL exercise those semantics against live, simulation, backtest, deterministic, in-memory,
or database interpreters as each is added. Interpreter-specific tests SHALL additionally cover resource, failure,
concurrency, and integration behavior not visible in the abstract port.

An interpreter MAY provide stronger operational guarantees only when callers opt into a separately specified capability
or concrete configuration; code polymorphic in the base port SHALL rely solely on the shared contract.

#### Scenario: Add a second execution interpreter later
- **WHEN** a simulator and live venue implement the same execution capability
- **THEN** both pass the same command/outcome/idempotency contract suite in addition to environment-specific tests

#### Scenario: Compare a live state interpreter with the pure model
- **WHEN** both receive the same initial state and explicit commands
- **THEN** their observable domain results agree while runtime-only scheduling details remain outside the comparison

#### Scenario: Offer a stronger durable interpreter
- **WHEN** a database interpreter guarantees durable restart recovery beyond an in-memory interpreter
- **THEN** generic callers assume only the shared semantics and durability-aware callers use the explicitly stronger
  capability or configuration contract
