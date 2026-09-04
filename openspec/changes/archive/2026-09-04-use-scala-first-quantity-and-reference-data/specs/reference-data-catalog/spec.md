## MODIFIED Requirements

### Requirement: Revisions distinguish publication from no-op

Every catalog state and snapshot SHALL expose a nonnegative arbitrary-precision `CatalogRevision`. A fresh root SHALL
start at revision zero. A successful batch that adds at least one immutable key SHALL publish exactly one successor
revision equal to the previous revision plus one, regardless of the number of commands or new definitions in the batch.

A fully idempotent batch SHALL return an explicit unchanged outcome at the existing revision. A failed batch SHALL also
leave the revision unchanged but SHALL remain distinguishable from an idempotent success. A published outcome SHALL
include a non-empty immutable delta identifying every newly added asset binding, dimension, and grid identity; a delta
SHALL never contain removal or replacement.

The successful commit outcome SHALL be an exhaustive direct Scala sum whose alternatives are unchanged and published,
and the state/outcome transition SHALL be a direct structural Scala product. Supported Scala callers SHALL be able to
pattern-match those alternatives exhaustively and compare independently observed equal results structurally without
hand-written product, extractor, equality, hash, or rendering behavior becoming part of the contract. Construction of
model-issued observations SHALL remain owned by the reference-data model so arbitrary state/outcome combinations cannot
be presented as successful catalog transitions.

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

#### Scenario: Match and compare direct catalog observations

- **WHEN** supported Scala code receives two successful catalog observations containing the same model-issued state and
  equal outcome fields
- **THEN** exhaustive matching distinguishes unchanged from published outcomes and structural equality observes equal
  state, snapshot, and delta fields
