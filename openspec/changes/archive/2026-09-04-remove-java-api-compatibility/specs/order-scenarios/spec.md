## ADDED Requirements

### Requirement: Associated Scala evidence constructs scenario assumptions directly

The supported order and scenario source API SHALL be Scala 3. When an order's associated activation evidence and
pricing resolution types are satisfied and matched slices are already represented by non-empty `MatchedSlices`,
assumption construction SHALL return the constructed assumptions directly without erased `Any` acceptance hooks or a
typed-failure wrapper that cannot fail. One-slice and head-plus-tail convenience construction SHALL provide the same
direct result. Vector reconstruction SHALL retain a typed result because emptiness remains expected invalid input.

Semantic agreement between same-shaped activation evidence or pricing resolution and a particular order SHALL remain
checked during scenario evaluation, where mismatches SHALL return their existing typed activation or pricing failures.

#### Scenario: Construct assumptions from established evidence

- **WHEN** supported Scala supplies an order's associated activation evidence, pricing resolution, and non-empty
  matched slices
- **THEN** assumption construction returns the assumptions directly without runtime shape acceptance checks

#### Scenario: Construct one or many slices directly

- **WHEN** supported Scala supplies either one slice or a head and zero or more tail slices with associated evidence
- **THEN** the corresponding convenience operation returns assumptions directly with non-empty slices

#### Scenario: Reject an empty reconstructed vector

- **WHEN** an external adapter reconstructs assumptions from an empty vector of matched slices
- **THEN** vector construction returns the typed empty-slices violation and no assumptions

#### Scenario: Reject statically mismatched evidence

- **WHEN** supported Scala attempts a fixed-versus-trailing or direct-versus-pegged evidence pairing
- **THEN** the assumption construction call does not type-check

#### Scenario: Reject a semantic replay mismatch

- **WHEN** correctly shaped evidence or resolution created for one instruction is evaluated against a different
  same-shaped instruction
- **THEN** scenario evaluation returns the existing typed activation or pricing mismatch
