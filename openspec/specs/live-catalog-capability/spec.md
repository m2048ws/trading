# live-catalog-capability Specification

## Purpose
Defines the effect-polymorphic application contract for observing and atomically advancing a live reference catalog
without coupling domain code to locks, mutable references, actors, databases, or a concrete effect runtime.
## Requirements
### Requirement: Live catalog is an application capability
The effectful live catalog contract SHALL be delivered above reference data by the application artifact as a
`LiveCatalog[F[_]]`-shaped capability. It SHALL expose only operations whose execution genuinely varies by interpreter:
capturing the current immutable `CatalogSnapshot` and atomically committing an explicit `CatalogBatch`.

The capability SHALL NOT be required by quantities, immutable reference-data handles, instrument economics, orders,
scenarios, fee values, PnL, or risk calculations. It SHALL NOT expose concrete mutable state, synchronization
primitives, thread or fiber types, database clients, clocks, telemetry, or a runtime-specific effect.

#### Scenario: Use the port from an application workflow
- **WHEN** an application workflow is polymorphic in `F` and must onboard reference definitions
- **THEN** it can request a snapshot or commit a batch through the abstract live catalog capability

#### Scenario: Calculate without the port
- **WHEN** domain code receives already trusted handles, an assembled instrument, or a captured snapshot
- **THEN** it performs its pure work without receiving `LiveCatalog[F]` or another effect parameter

#### Scenario: Supply different interpreters
- **WHEN** live, backtest, and deterministic test applications require different catalog coordination
- **THEN** they may implement the same capability without changing reference-data or domain semantics

### Requirement: Snapshot capture is the only effectful read
The live capability SHALL return one immutable `CatalogSnapshot` representing a fully published revision. Per-asset,
per-dimension, and per-grid resolution SHALL be pure behavior of that snapshot and SHALL NOT be duplicated as
effectful methods on the live port.

A workflow that requires one coherent catalog view SHALL capture a snapshot once for that boundary, such as a request,
input batch, replay segment, or instrument assembly. Reusing that snapshot SHALL require no further capability call and
SHALL remain valid while concurrent commits publish later revisions.

#### Scenario: Decode a batch from one capture
- **WHEN** an ingress workflow must reconstruct many records coherently
- **THEN** it captures one snapshot through `F` and performs every record lookup purely against that value

#### Scenario: Avoid effectful point lookup
- **WHEN** a caller has an `AssetId` or full grid identity
- **THEN** `LiveCatalog` offers no effectful resolve method and the caller resolves it through an explicit snapshot

#### Scenario: Observe a complete publication
- **WHEN** snapshot capture overlaps a successful catalog commit
- **THEN** the returned snapshot represents either the complete previous revision or the complete successor, never a
  partially applied batch

### Requirement: Batch commit is atomic and linearizable
Committing a batch through the live capability SHALL evaluate the pure catalog transition against one latest published
state and atomically publish its successor only when validation succeeds with a non-empty delta. A returned published
outcome SHALL identify the exact successor revision and snapshot that became visible. A failed or idempotent batch SHALL
not publish a new revision.

Concurrent commits SHALL have a total order consistent with their successful publication results. Each commit SHALL be
validated against the state produced by every earlier commit in that order, so updates are neither lost nor partially
merged. Snapshot capture SHALL observe one state in the same publication order.

#### Scenario: Commit two independent batches concurrently
- **WHEN** two valid batches race against one live catalog
- **THEN** both may publish in a defined order at consecutive revisions and the later state contains both deltas

#### Scenario: Race conflicting definitions
- **WHEN** two concurrent batches propose different definitions for one previously absent immutable key
- **THEN** at most one publishes and the other is validated against that successor and returns a typed conflict

#### Scenario: Commit an idempotent retry
- **WHEN** a caller retries an already published identical batch
- **THEN** the capability returns successful unchanged at the existing revision without publishing another revision

### Requirement: The port delegates semantics to the pure catalog model
Every interpreter SHALL use the normative pure catalog transition semantics for validation, canonical handle issuance,
revisioning, deltas, and errors. An interpreter MAY choose in-memory atomic references, STM, single-owner fibers,
database transactions, remote coordination, or another mechanism, but SHALL NOT change append-only behavior, error
ordering, idempotence, snapshot coherence, or handle lineage.

The reusable interpreter-conformance contract SHALL compare both typed failures and successful outcomes with the pure
model after consecutive commits, require idempotent retries to remain unchanged, require a concurrent losing conflict
to return the pure typed conflict, and exercise overlapping snapshot/commit capture so a revision never exposes a
partial or unrelated generation.

The application capability SHALL not require Cats Effect, FS2, ZIO, Akka, or another concrete runtime dependency in its
public artifact. Runtime-specific constraints, resources, cancellation behavior, retries, telemetry, and durability
guarantees SHALL be specified by concrete interpreter proposals rather than inferred from `F[_]` alone.

#### Scenario: Compare pure and live execution
- **WHEN** the same starting definitions and batch are run through the pure model and a conforming live interpreter
- **THEN** they produce the same domain outcome, revision, delta, lookup results, and handle relationships

#### Scenario: Reject an interpreter that hides a conflict
- **WHEN** an interpreter maps a pure `CatalogViolations` result to successful unchanged
- **THEN** the reusable contract fails because the live typed result differs from revalidation against current pure
  state

#### Scenario: Change coordination mechanism
- **WHEN** an application replaces a single-process interpreter with a transactional database interpreter
- **THEN** callers retain the same port and catalog semantics while runtime lifecycle and durability configuration may
  change

#### Scenario: Avoid false effect guarantees
- **WHEN** a generic workflow knows only `LiveCatalog[F]`
- **THEN** it cannot assume a particular thread model, cancellation guarantee, retry policy, persistence level, or
  telemetry implementation

### Requirement: One live instance owns one catalog lineage
A live catalog instance SHALL be initialized with exactly one catalog root and SHALL publish only states and snapshots
from that lineage for its lifetime. It SHALL expose no reset operation that silently replaces the lineage or causes
existing handles to acquire a new meaning. A separately bootstrapped or restarted instance SHALL have a distinct
lineage even when it loads equal stable definitions.

Replacing a live instance, loading persisted commands, and distributing snapshots are runtime lifecycle concerns.
Stable records SHALL cross that boundary through IDs and checked resolution, not by serializing live state or trusted
handles.

#### Scenario: Retain lineage across commits
- **WHEN** one live instance publishes many revisions
- **THEN** handles from all of its snapshots reconcile within the same lineage according to immutable identity

#### Scenario: Reject an in-place reset
- **WHEN** an operator needs to replace the entire live catalog source
- **THEN** the minimal capability provides no operation that mutates the existing instance into a new lineage

#### Scenario: Restart with equal definitions
- **WHEN** a new live instance loads the same stable definitions after process restart
- **THEN** it issues new-lineage handles and boundary data must resolve through a snapshot from that instance

### Requirement: Streaming and observation are separate capabilities
The minimal live catalog SHALL NOT expose change streams, subscriptions, polling intervals, metrics, tracing spans,
notifications, or callbacks. Applications that need to distribute `CatalogDelta` events or observe catalog health SHALL
use separately specified streaming and telemetry capabilities fed by successful commit outcomes or interpreter-level
instrumentation.

#### Scenario: Publish without a subscriber
- **WHEN** a valid batch is committed and no streaming capability is installed
- **THEN** catalog publication still succeeds with its complete domain outcome

#### Scenario: Add change distribution later
- **WHEN** an application must broadcast catalog revisions
- **THEN** it composes a separate event/stream capability around successful commits without changing snapshot lookup or
  the pure transition model
