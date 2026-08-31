## Purpose

Defines a durable append-only catalog-command journal representation and deterministic pure replay that rebuilds trusted
catalog state under a fresh process-local lineage without serializing snapshots or handles.

## ADDED Requirements

### Requirement: Journal entries represent successful catalog publications
A catalog-journal-entry V1 record SHALL contain one positive recorded `CatalogRevision` and one ordered non-empty
`CatalogBatch` encoded as its closed dimension, asset, and grid registration command alternatives. Commands SHALL retain
the complete stable definitions and canonical exact grid quanta needed by the pure catalog transition. An entry SHALL
contain no catalog root/lineage token, snapshot, trusted handle, object reference, mutable state, activation/delisting
policy, timestamp, database offset, or live interpreter.

The supported journal-entry construction path SHALL pair the submitted batch with a successful published catalog
outcome and record its successor revision. Failed commits and idempotent unchanged outcomes SHALL not produce journal
entries. A decoded external entry remains untrusted data until replay proves that its batch really publishes the
recorded next revision.

#### Scenario: Record one published batch
- **WHEN** a non-empty catalog batch publishes revision `n`
- **THEN** journal construction emits V1 data containing exactly revision `n` and the submitted ordered commands

#### Scenario: Omit an idempotent retry
- **WHEN** a catalog commit returns unchanged because every definition already exists identically
- **THEN** the supported journal constructor produces no publication entry

#### Scenario: Keep lineage out of the journal
- **WHEN** equal journal entries are replayed in two processes
- **THEN** they contain the same stable definitions/revisions but confer no cross-process handle authority

### Requirement: Journal JSON uses the shared exact versioned codec contract
Catalog journal entries SHALL use the canonical envelope, strict member handling, exact integer/rational/dimension
representations, structured diagnostics, and configurable limits of the versioned boundary-codec capability. The
catalog command alternatives and their V1 payload fields SHALL be frozen independently of future catalog commands or
record families.

Entry decoding SHALL first parse every independent envelope/field in an input sequence and accumulate indexed wire
violations in deterministic input/path order. Catalog transition replay SHALL begin only when every required entry has
decoded structurally; malformed entries SHALL not be skipped or replaced with empty commands.

#### Scenario: Accumulate malformed entry syntax
- **WHEN** independent journal payloads contain invalid revisions, grid quanta, and command tags
- **THEN** structural decoding returns all applicable indexed codec violations and performs no replay

#### Scenario: Reject a future command in V1
- **WHEN** a V1 entry contains an unknown registration-command alternative
- **THEN** decoding fails explicitly rather than ignoring the command or guessing its meaning

#### Scenario: Preserve command order
- **WHEN** a published batch contains several commands
- **THEN** canonical encoding/decoding preserves their exact order even where pure catalog semantics are order-
  independent

### Requirement: Replay rebuilds a catalog sequentially from a fresh root
Pure replay SHALL consume an explicitly supplied fresh empty catalog root/state at revision zero plus an ordered
immutable sequence of decoded journal entries. It SHALL process entries in supplied order. At position `i`, the
recorded revision MUST equal the current revision plus one, and applying the entry's batch through the normative pure
catalog model MUST return `Published` at exactly that revision.

On success, replay SHALL return the final immutable `CatalogState` and snapshot, with canonical handles issued by the
new root and every catalog transition law preserved. An empty journal SHALL return the unchanged revision-zero state.
Replay SHALL not use a live catalog, effect, mutable registry, hidden current snapshot, clock, file, database, or
network.

#### Scenario: Rebuild a valid history
- **WHEN** entries record consecutive valid published batches from revision one through `n`
- **THEN** replay returns revision `n` with the same stable definitions, lookup results, deltas per step, and within-
  lineage handle relationships as direct pure application

#### Scenario: Replay an empty journal
- **WHEN** no entries are supplied with a fresh empty state
- **THEN** replay succeeds with that state and its revision-zero snapshot

#### Scenario: Rebuild under a new lineage
- **WHEN** the same journal is replayed from two independently created roots
- **THEN** visible definitions and revision sequence agree while handles from the two results do not reconcile

### Requirement: Replay rejects gaps, no-ops, conflicts, and out-of-order history
If an entry's recorded revision is repeated, skipped, decreasing, or otherwise not exactly next, replay SHALL return a
typed revision-sequence failure retaining entry index, expected revision, and supplied revision. If the pure catalog
transition returns validation failures, replay SHALL retain the complete ordered `CatalogViolations` at that entry. If
the batch is valid but unchanged, replay SHALL return a typed unexpected-no-op journal failure.

State-dependent replay failures SHALL stop at the first failing entry because every later transition depends on the
missing successor. No failed replay SHALL return a partially rebuilt catalog as a successful result, skip an entry,
renumber revisions, overwrite an immutable key, or reinterpret availability as deletion. Diagnostics MAY retain the
last successful revision value but SHALL not expose a partial trusted state as completion.

#### Scenario: Detect a revision gap
- **WHEN** the current revision is `4` and the next entry records revision `6`
- **THEN** replay stops at that entry with expected `5`, supplied `6`, and no final catalog result

#### Scenario: Detect a conflicting correction
- **WHEN** an entry attempts to replace an existing immutable grid definition under the same full key
- **THEN** replay preserves the catalog's typed conflict collection and does not apply later entries

#### Scenario: Reject a no-op journal entry
- **WHEN** an entry's batch is entirely identical to definitions already replayed
- **THEN** replay reports unexpected unchanged publication instead of accepting its claimed next revision

#### Scenario: Avoid partial recovery claims
- **WHEN** entry `k` fails after earlier entries were valid
- **THEN** the result identifies `k` and the last successful revision but does not present the prefix state as a
  completed rebuild

### Requirement: Historical reconstruction uses stable IDs, not serialized authority
Replaying a journal prefix SHALL reproduce the identity membership and immutable definitions of that historical
revision under the selected fresh lineage. Grid corrections SHALL appear as new `GridVersion` keys, asset-dimension
corrections as new `AssetId` keys, and old versions SHALL remain resolvable. Activation, delisting, and effective dating
SHALL not be inferred from missing/later journal commands.

`CatalogSnapshot`, `CatalogState` implementation internals, catalog root/lineage tokens, `Asset`, `DimensionHandle`, and
`GridHandle` SHALL not have a supported durable encoding in this capability. A snapshot checkpoint cannot be restored as
authority merely by decoding bytes; future durable checkpoints/database interpreters require their own trusted rebuild
and integrity design.

#### Scenario: Replay through a historical revision
- **WHEN** a caller supplies the valid journal prefix ending at the revision that introduced grid version `1`
- **THEN** the resulting snapshot contains that version and excludes identities introduced by later omitted entries

#### Scenario: Retain corrected versions
- **WHEN** later journal entries add grid version `2` as a correction
- **THEN** full replay resolves both versions and never mutates version `1`

#### Scenario: Refuse a serialized snapshot shortcut
- **WHEN** a caller has bytes produced by Java serialization or an ad hoc snapshot dump
- **THEN** this capability offers no decoder that turns them into trusted catalog authority

### Requirement: Replay compatibility is deterministic and independently verified
Catalog-journal V1 SHALL have a checked-in JSON schema, canonical golden entry/history fixtures, malformed fixtures, and
model tests comparing replay with direct calls to the pure catalog transition. Tests SHALL cover empty history,
multi-command batches, idempotent duplicate commands within a publishing batch, independent additions, conflicts,
revision gaps/repeats, exact huge numbers within selected limits, historical prefixes, command-order permutations, and
fresh-lineage separation.

For any schema-valid journal within limits, repeated replay from equivalently fresh roots SHALL produce the same
ordered error shape or structurally equivalent definitions/revisions, while intentionally different lineage authority
remains observable only through failed cross-root reconciliation. A future journal version SHALL use an explicit pure
migration/replay path and SHALL not change V1 meaning.

#### Scenario: Compare direct transition and replay
- **WHEN** the same valid batches are directly committed and encoded/replayed in the same order
- **THEN** their revisions, definitions, lookup behavior, and per-publication delta content are structurally equivalent

#### Scenario: Normalize deterministic diagnostics
- **WHEN** replay implementation details traverse decoded collections in different orders
- **THEN** the public failure index, revision context, and nested catalog violation order remain identical

#### Scenario: Freeze journal V1
- **WHEN** a future catalog command or metadata field is introduced
- **THEN** V1 golden histories retain their behavior and the new meaning uses a separately supported schema version
