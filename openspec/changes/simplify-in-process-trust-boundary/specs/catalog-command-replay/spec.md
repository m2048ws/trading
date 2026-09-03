## ADDED Requirements

### Requirement: Replay authority follows from publication checks

Journal entries and replay results SHALL use statically callable construction. Neither record constructor visibility nor
hidden result construction SHALL confer catalog authority. An entry decoded from external data SHALL remain
non-authoritative until sequential replay proves the expected revision, successful publication, command validity, and
catalog transition; failures SHALL retain their exact index, revision context, and nested violations.

#### Scenario: Replay a valid journal

- **WHEN** contiguous V1 entries describe exactly the batches successfully published from a fresh catalog root
- **THEN** replay returns structurally equivalent definitions, revisions, lookup behavior, and delta content in a fresh
  lineage

#### Scenario: Replay an invalid journal

- **WHEN** an entry contains a revision gap, no-op, conflict, invalid command, or out-of-order history
- **THEN** replay returns the same typed failure and exposes no partial catalog as successful completion

#### Scenario: Construct a journal-shaped value directly

- **WHEN** cooperative in-process code constructs structurally valid journal data without a publication transition
- **THEN** the value carries no assertion that the batch was published and checked replay still establishes authority

