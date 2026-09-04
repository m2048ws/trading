## MODIFIED Requirements

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

- **WHEN** supported Scala code or checked external reconstruction supplies a null payload, negative command index, or
  non-conflicting duplicate-definition collection for a catalog error
- **THEN** no malformed public error value is returned while checked expected-input factories retain their typed errors

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

- **WHEN** supported Scala code or checked external reconstruction supplies duplicate delta additions or combines a
  state, snapshot, revision, and publication delta that were not issued together by the catalog model
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
handles. Supported downstream Scala 3 callers SHALL NOT obtain usable retyping authority by invoking the evidence
constructor or by supplying a handle, visible identity, or observed lineage-related value as an issuance token.

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
