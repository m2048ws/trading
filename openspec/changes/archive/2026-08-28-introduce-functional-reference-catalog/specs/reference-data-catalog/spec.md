## Purpose

Defines the pure append-only reference-data catalog, its immutable state transitions, canonical handle issuance,
revisioned snapshots, transactional registration semantics, and typed lookup and definition failures.

## ADDED Requirements

### Requirement: Catalog evolution is an immutable state transition
The reference-data artifact SHALL model catalog evolution as a pure transformation from one immutable catalog state and
an explicit registration batch to either a non-empty domain failure or a new immutable state plus an observable commit
outcome. The input state SHALL remain usable and unchanged after every success or failure. Transition semantics SHALL
require no lock, mutable collection, effect type, clock, thread, database, actor, or live service.

Every state derived from one catalog root SHALL retain the same opaque issuer lineage established at that root. Creating
a distinct root SHALL establish a distinct lineage; state transitions SHALL never replace or reconstruct it from public
identifiers. Fresh-root creation SHALL be a controlled generative authority boundary outside the pure transition; once
a root state is supplied, transition evaluation SHALL not mint or replace catalog lineage.

#### Scenario: Apply a pure transition
- **WHEN** the same valid registration batch is evaluated against the same catalog state
- **THEN** it produces the same definitions, revision outcome, delta, and semantically canonical handles without
  mutating the input

#### Scenario: Retain the old state
- **WHEN** a batch publishes a successor state
- **THEN** callers retaining the prior state can still obtain its unchanged snapshot and lookup results

#### Scenario: Separate catalog roots
- **WHEN** equal definitions are built under two independently created catalog roots
- **THEN** the resulting handles have different opaque issuer lineage even though their visible definitions agree

#### Scenario: Re-evaluate within one root
- **WHEN** a caller evaluates the same batch against the same supplied root state more than once
- **THEN** each result is observationally equal under public catalog semantics and no evaluation creates a new lineage

### Requirement: Registration commands are explicit non-empty data
Catalog write intent SHALL be represented by a closed immutable command ADT covering asset, standalone dimension, and
stable grid registration. A transaction SHALL contain a non-empty ordered batch of those commands. It SHALL NOT encode
arbitrary callbacks, I/O, activation, delisting, deletion, or in-place update.

A grid command MAY refer to a dimension supplied anywhere in the same batch; its validity SHALL NOT depend on command
execution order. Public command and batch values SHALL retain the definitions needed for inspection, testing, logging,
and future explicit encoding without containing trusted handles or private lineage authority.

Every public catalog command, outcome, lookup error, violation, and guarded collection SHALL reject null payloads and
malformed nested evidence such as negative or repeated command indices, duplicate definitions whose keys differ from
the named key, equal conflict sides, or duplicate-proposal values that do not contain genuinely distinct conflicting
definitions. Expected negative revision input SHALL continue to be represented by `CatalogRevision.from` as a typed
domain result; defensive constructor rejection SHALL not replace that expected-input path with exception control flow.

#### Scenario: Inspect registration intent
- **WHEN** application code constructs an asset, dimension, or grid registration batch
- **THEN** the requested definitions are ordinary immutable data and can be inspected before transition evaluation

#### Scenario: Reference a dimension later in the batch
- **WHEN** a grid command appears before the command that introduces its canonical dimension in the same valid batch
- **THEN** the whole batch succeeds exactly as it would under the reverse command order

#### Scenario: Reject an empty transaction
- **WHEN** a caller attempts to construct a registration batch with no commands
- **THEN** no batch value is returned and no meaningless publication can be requested

#### Scenario: Reject malformed public error evidence
- **WHEN** a Scala or Java caller attempts to construct a catalog error with a null payload, negative command index, or
  non-conflicting duplicate-definition collection
- **THEN** no malformed public error value is returned while checked expected-input factories retain their typed errors

### Requirement: Identity definitions are append-only and immutable by key
The catalog SHALL add definitions and SHALL expose no command that deletes, overwrites, or mutates an existing identity
binding. Registering a key with its identical immutable definition SHALL be idempotent and SHALL return the already
canonical handle. Registering the same key with a different immutable definition SHALL return a typed conflict and
SHALL leave the complete state and revision unchanged.

An `AssetId` SHALL be permanently bound to one atomic dimension key within a lineage. One atomic asset dimension SHALL
be bound to at most one `AssetId`; a standalone canonical dimension MAY be introduced before its asset binding and then
reused by that asset. Reassigning either side of an existing asset binding SHALL fail. A semantic correction that would
change the asset's dimension SHALL use a new `AssetId`.

#### Scenario: Repeat an identical asset definition
- **WHEN** an already registered asset definition is registered again unchanged
- **THEN** the transition is idempotent and returns the existing canonical asset without publishing a change

#### Scenario: Reject asset reassignment
- **WHEN** an existing `AssetId` is supplied with a different atomic dimension
- **THEN** the transition returns a typed immutable-definition conflict and preserves the old binding

#### Scenario: Reject a second asset alias for one dimension
- **WHEN** a different `AssetId` attempts to claim an atomic dimension already bound to an asset
- **THEN** the transition fails rather than making two distinct asset identities directly fungible by dimension

#### Scenario: Bind an existing standalone dimension
- **WHEN** an atomic dimension was registered independently and a later asset definition claims it while no other asset
  does
- **THEN** the asset reuses that dimension's canonical handle without replacing it

### Requirement: Grid corrections create new immutable versions
Full stable grid identity SHALL remain the product of canonical `DimKey`, `GridId`, and positive `GridVersion`.
Registering a grid SHALL require that its dimension exists in the current state or the same batch. The full identity
SHALL bind permanently to one positive exact quantum and one canonical mathematical grid and handle.

Repeating the same full identity and quantum SHALL be idempotent. Supplying another quantum under the same full identity
SHALL fail without reinterpretation. A corrected or otherwise changed grid definition SHALL use a new `GridVersion`;
old and new versions SHALL coexist and remain independently resolvable.

#### Scenario: Repeat an identical grid definition
- **WHEN** the same dimension, grid ID, version, and quantum are registered again
- **THEN** the transition returns the original grid handle and publishes no change

#### Scenario: Reject reinterpretation at one version
- **WHEN** an existing full grid identity is supplied with a different quantum
- **THEN** the transition returns a typed conflict and the original coordinate interpretation remains unchanged

#### Scenario: Add a corrected grid version
- **WHEN** a new positive version is registered for an existing dimension-local grid ID with a changed quantum
- **THEN** both versions are retained as distinct immutable grid handles

#### Scenario: Reject an unknown grid dimension
- **WHEN** a batch registers a grid whose dimension exists neither in the state nor anywhere in that batch
- **THEN** the batch reports a typed missing-dimension violation and issues no grid handle

### Requirement: Batch validation is accumulating and atomic
The catalog SHALL detect all independently observable invalid commands and definition conflicts in a batch and return
them in deterministic command-index and rule order through a public non-empty domain collection. A check that requires
a valid prerequisite definition SHALL run only when that prerequisite is present and coherent; the catalog SHALL NOT
fabricate dependent grid or handle failures after the required dimension failed.

Validation SHALL occur against the complete proposed batch plus the input state. If any violation exists, no command in
the batch SHALL be committed, no handle from the failed proposal SHALL escape, the input state SHALL be returned
unchanged, and the revision SHALL not advance. If validation succeeds, every new definition SHALL appear together in
one successor state.

#### Scenario: Accumulate independent conflicts
- **WHEN** one batch independently conflicts with an existing asset and an existing grid and also names an unknown
  dimension for another grid
- **THEN** it returns all observable violations in stable order and publishes none of the otherwise valid commands

#### Scenario: Classify a canonical retry and one new alias once
- **WHEN** a batch repeats one canonical asset definition and then proposes one new asset for that already-bound
  dimension
- **THEN** the new alias command produces exactly one indexed binding violation rather than duplicate evidence from
  current-state and within-batch stages

#### Scenario: Suppress a dependent grid failure
- **WHEN** a proposed dimension definition is itself incoherent and a grid command depends on it
- **THEN** the catalog reports the prerequisite violation without constructing or validating a handle for that grid

#### Scenario: Publish a valid mixed batch atomically
- **WHEN** one batch adds dimensions, assets, and grids whose cross-references are coherent
- **THEN** one successor state contains all additions and no snapshot can observe a partial prefix

### Requirement: Revisions distinguish publication from no-op
Every catalog state and snapshot SHALL expose a nonnegative arbitrary-precision `CatalogRevision`. A fresh root SHALL
start at revision zero. A successful batch that adds at least one immutable key SHALL publish exactly one successor
revision equal to the previous revision plus one, regardless of the number of commands or new definitions in the batch.

A fully idempotent batch SHALL return an explicit unchanged outcome at the existing revision. A failed batch SHALL also
leave the revision unchanged but SHALL remain distinguishable from an idempotent success. A published outcome SHALL
include a non-empty immutable delta identifying every newly added asset binding, dimension, and grid identity; a delta
SHALL never contain removal or replacement.

#### Scenario: Publish several definitions once
- **WHEN** one valid batch adds ten definitions to revision `7`
- **THEN** its successor snapshot has revision `8` and one delta containing all ten additions

#### Scenario: Commit an idempotent batch
- **WHEN** every command repeats an identical existing definition
- **THEN** the outcome is successful and unchanged at the current revision with no fabricated publication delta

#### Scenario: Preserve revision on failure
- **WHEN** batch validation returns one or more violations
- **THEN** no successor revision or delta is produced

#### Scenario: Reject incoherent publication evidence
- **WHEN** a Scala or Java caller attempts to retain duplicate delta additions or pair a state, snapshot, revision, and
  publication delta that were not issued together by the catalog model
- **THEN** the checked delta factory returns typed rejection evidence and no malformed delta, published outcome, or
  transition is returned, while model-issued values remain publicly inspectable and pattern-matchable

### Requirement: Handles are canonical across a catalog lineage
Within one lineage, each canonical dimension key, asset binding, and full grid identity SHALL have one semantic trusted
handle and one immutable definition. Successor states and snapshots SHALL retain and structurally share existing
handles; they SHALL NOT reconstruct old path-dependent dimensions or grid coordinate namespaces after each commit.

Repeated lookup in one or later snapshots SHALL return handles accepted by checked same-handle reconciliation. JVM
reference equality MAY be used privately but SHALL NOT be the public contract. A handle remains valid after later
catalog revisions and carries no mutable pointer to a current state.

Only successful checked stable-grid reconciliation SHALL issue evidence capable of retyping coordinates between two
handles. Ordinary downstream Scala and Java callers SHALL NOT obtain usable retyping authority by invoking the
evidence constructor or by supplying a handle, visible identity, or observed lineage-related value as an issuance
token.

#### Scenario: Resolve an existing asset after a later commit
- **WHEN** an asset is resolved at revision `3` and again after unrelated additions publish revision `9`
- **THEN** both results reconcile as the same trusted asset and preserve the same path-dependent dimension

#### Scenario: Retain a historical grid namespace
- **WHEN** a later grid version is added
- **THEN** the earlier grid handle keeps its original mathematical grid, coordinate type, and quantum

#### Scenario: Avoid reference-equality dependence
- **WHEN** an implementation changes its immutable-map or snapshot representation without changing catalog semantics
- **THEN** callers continue to rely on checked handle evidence rather than `eq`

#### Scenario: Retype only after checked grid reconciliation
- **WHEN** a caller reconciles two stable grid handles and then retypes a coordinate
- **THEN** successful checked reconciliation supplies the required evidence, while direct downstream evidence
  construction cannot yield usable authority

### Requirement: Catalog snapshots are coherent pure read views
A `CatalogSnapshot` SHALL be an immutable coherent view of exactly one lineage and revision. It SHALL provide pure
direct lookup for asset IDs, canonical dimension keys, and full stable grid identities and SHALL return typed unknown
identity failures. It SHALL expose immutable counts or equivalent observations without requiring traversal under a
shared monitor.

Snapshot lookup SHALL not acquire a lock, observe a later publication, call a live capability, scan unrelated dimension
maps to diagnose an unknown full grid identity, or mutate lookup caches. Capturing a snapshot once and using it for a
batch SHALL make every lookup in that batch observe the same catalog revision.

#### Scenario: Resolve from one coherent revision
- **WHEN** a batch captures revision `12` and a live catalog concurrently publishes revision `13`
- **THEN** every lookup through the captured snapshot continues to observe only revision `12`

#### Scenario: Fail direct unknown-grid lookup
- **WHEN** a full grid identity is absent from a snapshot
- **THEN** lookup returns a typed unknown-grid failure without scanning grids owned by other dimensions

#### Scenario: Read without coordination
- **WHEN** many threads resolve values through the same immutable snapshot
- **THEN** the snapshot requires no synchronized read/read exclusion and returns the same results to every thread

### Requirement: Historical snapshots and definitions remain valid
Publishing a successor SHALL NOT invalidate, mutate, or silently redirect a prior snapshot. A prior snapshot SHALL
continue to resolve every identity it contained with its original handle and SHALL return unknown for identities added
later. The catalog SHALL impose no automatic retention duration or revocation on handles or snapshots held by callers.

Reconstructing equal definitions after a process restart SHALL create a new lineage and new trusted handles. Stable
external records SHALL therefore resolve against the selected snapshot in the new process instead of serializing or
reusing an old in-memory handle.

#### Scenario: Query a pre-update snapshot
- **WHEN** a grid is added at revision `5` and a caller still holds revision `4`
- **THEN** revision `4` reports that grid as unknown while preserving all of its earlier results

#### Scenario: Retain an assembled instrument meaning
- **WHEN** an instrument keeps handles obtained from an older snapshot and the catalog later adds another grid version
- **THEN** the retained handles continue to mean exactly what they meant when the instrument was assembled

#### Scenario: Restart from stable definitions
- **WHEN** a new process rebuilds a catalog from equal asset and grid definitions
- **THEN** stable IDs resolve to new-lineage handles and no old in-memory authority is deserialized

### Requirement: Availability policy is not identity mutation
The identity catalog SHALL NOT model activation, deactivation, delisting, venue availability, account eligibility,
effective time, or replacement of an immutable definition. Those concerns SHALL use separate policy or temporal data
keyed by stable identities. Marking an identity unavailable SHALL NOT delete it from historical catalog snapshots or
change the definition stored at its key.

#### Scenario: Delist a grid without deleting identity
- **WHEN** a venue no longer permits new use of one grid version
- **THEN** availability policy may reject new activity while historical catalog resolution still recovers the immutable
  grid definition

#### Scenario: Keep time out of catalog transitions
- **WHEN** a catalog batch is evaluated
- **THEN** its success and resulting definitions do not depend on the current clock or an implicit effective date
