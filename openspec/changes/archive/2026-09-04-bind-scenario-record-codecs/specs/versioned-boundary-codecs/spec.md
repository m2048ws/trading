## ADDED Requirements

### Requirement: Scenario records expose separate bound encoding and reconstruction contexts
The boundary-codec artifact SHALL expose one immutable scenario encoder bound only to one exact instrument and one
immutable scenario decoder bound to that exact instrument plus one immutable `CatalogSnapshot`. The encoder SHALL
construct records and produce canonical wire encodings for both order scenarios and round-trip scenarios. The decoder
SHALL reconstruct supplied records, decode and reconstruct supplied wire input, and reconstruct ordered batches for
both scenario families.

Each encoder operation SHALL accept scenarios whose position, base, quote, and market-state relationships match the
captured instrument. Each decoder operation SHALL return the same exact instrument-dependent scenario type as direct
reconstruction with the captured instrument and snapshot. Calls SHALL NOT require explicit dimension arguments or
local aliases for the instrument's role dimensions.

#### Scenario: Reuse one encoder across both scenario families
- **WHEN** a caller binds one exact instrument and supplies compatible order and round-trip scenarios
- **THEN** the same encoder constructs each corresponding V1 record and canonical wire value with the same result as
  direct scenario-record encoding

#### Scenario: Reuse one decoder across every reconstruction mode
- **WHEN** a caller binds one exact instrument and immutable snapshot and supplies order and round-trip records, wire
  inputs, and ordered batches
- **THEN** the same decoder reconstructs each family with the exact captured instrument-dependent result types and the
  same values or typed failures as direct reconstruction

#### Scenario: Reject an incompatible scenario shape
- **WHEN** downstream Scala attempts to pass an order or round-trip scenario with incompatible position, base, quote,
  or market-state types to a bound encoder
- **THEN** compilation fails without a cast, widening, runtime lookup, structural refinement, or replacement dimension
  evidence

#### Scenario: Use both contexts from the completed artifact
- **WHEN** a completed-artifact Scala client reuses one encoder and one decoder for both scenario families
- **THEN** record construction, canonical encoding, and reconstruction compile without explicit dimension arguments or
  nested instrument-role projections at the calls

### Requirement: Bound scenario contexts preserve wire compatibility, snapshot coherence, and codec ownership
Bound scenario contexts SHALL delegate to the existing record-family operations as the single implementation of record
projection, canonical writing, parsing, domain reconstruction, error accumulation, and batching. They SHALL preserve
the current V1 envelopes and schemas, byte-identical canonical output, exact reconstructed values, error alternatives
and deterministic order, canonicalization and version handling, null rejection, atomic batch behavior, input order,
and stable diagnostic locations.

Record parsing, record-only encoding, and schema access SHALL remain context-free operations on each record family.
`DecodeLimits` and single-record indices SHALL remain explicit operation inputs rather than captured context, and
batch diagnostics SHALL continue to identify their original input indices.

The decoder SHALL use only its one captured immutable snapshot throughout each operation and batch. Both contexts
MUST NOT acquire or observe a live catalog, another snapshot, mutable or ambient state, a resource, an effect, persistence,
application/runtime behavior, or downstream scenario valuation. Reusing either context sequentially or concurrently
SHALL NOT change any result.

#### Scenario: Retain context-free record operations
- **WHEN** a caller parses scenario wire input, encodes an already constructed record, or requests either scenario
  schema
- **THEN** those operations remain usable without an instrument, snapshot, encoder, or decoder

#### Scenario: Keep limits and locations explicit
- **WHEN** a caller decodes one record with explicit limits and record index or reconstructs a batch with explicit
  limits containing malformed and domain-invalid entries
- **THEN** the same limit failures, record indices, field paths, independent errors, and deterministic ordering are
  returned as by the corresponding direct operation

#### Scenario: Preserve one coherent snapshot
- **WHEN** a decoder reconstructs multiple records or a batch while other immutable catalog snapshots also exist
- **THEN** every lookup and reconstruction uses only the decoder's captured snapshot and cannot observe a live catalog
  or another generation

#### Scenario: Preserve exact supported compatibility
- **WHEN** characterization, golden, malformed-input, null-boundary, version, canonicalization, and batch tests compare
  bound and direct scenario-record entry points
- **THEN** their records, canonical bytes, successful scenarios, failures, ordering, and locations are exactly equal

#### Scenario: Keep binding inside the codec owner
- **WHEN** the completed boundary-codec artifact and dependency graph are inspected
- **THEN** the contexts and their record, parser, schema, and reconstruction behavior remain codec-owned and introduce
  no live-catalog, application, runtime, persistence, stream, client, telemetry, or venue-SDK dependency
