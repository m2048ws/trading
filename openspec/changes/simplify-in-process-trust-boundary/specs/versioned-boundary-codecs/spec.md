## ADDED Requirements

### Requirement: Codec records remain data until static checked reconstruction succeeds

Codec records, envelopes, paths, violation collections, decode limits, and replay results SHALL use statically callable
construction and observation without reflective access to private members. Direct construction of a structurally valid
record SHALL confer no domain or catalog authority. Decoding and replay SHALL continue to apply bounded parsing,
explicit version dispatch, exact primitive validation, coherent snapshot resolution, and owning domain checks before
returning trusted results.

#### Scenario: Construct an internal record representation

- **WHEN** a codec combines already validated primitive fields into a closed record or diagnostic value
- **THEN** it uses ordinary static construction and the record remains non-authoritative data

#### Scenario: Decode malformed V1 data

- **WHEN** V1 input violates syntax, limits, exact primitives, refinements, identity, associated shape, or domain
  relationships
- **THEN** decoding returns the same deterministic typed failure without constructing a trusted result

#### Scenario: Round-trip valid V1 data

- **WHEN** a valid record is encoded, decoded against its required coherent snapshot, and re-encoded
- **THEN** its canonical bytes and reconstructed semantic value remain unchanged by the construction simplification

