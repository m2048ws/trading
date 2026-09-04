# versioned-boundary-codecs Specification

## Purpose
Defines stable exact external representations and pure checked reconstruction for current catalog, quantity,
instrument, order, and hypothetical-scenario data without treating trusted in-memory values as serialized authority.

## Requirements

### Requirement: Boundary codecs form a pure one-way artifact
Stable wire/database records and their codecs SHALL be delivered by a `trading-boundary-codecs` artifact above the pure
quantity, reference-data, instrument-economic, order-model, and execution-scenario artifacts. Those lower artifacts
SHALL NOT depend on codecs. The codec artifact SHALL NOT depend on fee policy, risk, application, runtime, a concrete
effect system, persistence clients, network clients, clocks, streams, transactions, tracing, or metrics.

Encoding, parsing, validation, snapshot reconstruction, and batch reconstruction SHALL be deterministic pure operations
over immutable inputs. A codec operation SHALL NOT capture a live catalog, read a file/database/network, acquire a
resource, return an abstract effect, or make an external write.

#### Scenario: Encode without runtime
- **WHEN** downstream Scala supplies one supported immutable domain value to its boundary encoder
- **THEN** it obtains canonical JSON without a live catalog, effect runtime, or external service

#### Scenario: Decode against an explicit snapshot
- **WHEN** a record requires reference-data reconstruction
- **THEN** the caller supplies exactly one immutable `CatalogSnapshot` and no live capability is consulted

#### Scenario: Preserve lower-module independence
- **WHEN** the codec artifact is absent from a lower artifact's compile classpath
- **THEN** all quantity, catalog, economics, order, scenario, fee, and risk semantics remain available

### Requirement: Every record uses an independently versioned envelope
Every supported JSON record SHALL contain exactly one stable record-type identifier, one positive schema-version field,
and one payload whose shape is selected by that pair. Schema versions SHALL be scoped to their record type and SHALL be
semantically distinct from `GridVersion`, `CatalogRevision`, venue versions, and application release versions.

Writers SHALL emit one declared current version for each record family. Readers SHALL dispatch only to explicitly
supported type/version pairs and return typed envelope failures for missing, invalid, unknown, or mismatched pairs. A
reader SHALL NOT guess a record family from payload shape, interpret an unknown version as the current version, or
silently fall back to another decoder.

#### Scenario: Decode a known type and version
- **WHEN** an envelope declares a supported record family and that family's V1 schema
- **THEN** dispatch validates the V1 payload under only that record contract

#### Scenario: Reject an unknown future version
- **WHEN** a known record family declares a schema version the reader does not support
- **THEN** decoding returns a typed unsupported-version failure retaining the family and supplied version

#### Scenario: Keep grid and schema versions distinct
- **WHEN** a V1 grid-quantity record names grid version `7`
- **THEN** the envelope schema remains version `1` while the payload retains reference-data `GridVersion(7)`

### Requirement: Canonical JSON has one writer representation
For each supported record version, the writer SHALL emit RFC 8785 JSON Canonicalization Scheme compatible text with the
exact declared member names, UTF-16 code-unit member ordering, no insignificant whitespace, canonical string escaping,
and canonical tagged alternatives. The reader SHALL accept semantically equivalent member ordering and JSON whitespace
but SHALL reject duplicate members, unknown members, missing required members, explicit `null` in required fields, and
fields belonging to another algebraic alternative. Record strings SHALL be well-formed Unicode without unpaired
surrogates; otherwise encoding or decoding SHALL return a typed path-located text violation.

Adding, removing, renaming, or changing the meaning/type of a field or closed alternative SHALL require a new schema
version unless the existing version explicitly declared that field optional with a fixed default. V1 schemas SHALL not
gain implicit defaults after publication. Re-encoding any successfully decoded V1 record SHALL produce the one
canonical V1 representation.

#### Scenario: Canonicalize accepted JSON variation
- **WHEN** a valid V1 object arrives with permitted whitespace and a different member order
- **THEN** it decodes successfully and re-encodes to the canonical golden representation

#### Scenario: Reject ambiguous members
- **WHEN** a JSON object repeats a member or supplies an unknown misspelled member
- **THEN** structural decoding reports the exact member path rather than choosing one value or ignoring it

#### Scenario: Preserve a frozen V1 shape
- **WHEN** a future change needs another required field or closed alternative
- **THEN** it introduces a new version and keeps existing V1 fixture behavior unchanged

### Requirement: Exact primitives have canonical non-floating encodings
All arbitrary-precision integers used as coordinates, tick offsets, powers, revisions, or exact coefficients SHALL be
encoded as canonical decimal JSON strings matching zero or an optional minus sign followed by a nonzero digit and
remaining digits. Leading plus signs, leading zeroes, surrounding whitespace, decimal points, exponents, hexadecimal,
`NaN`, and infinities SHALL be rejected. Positive values SHALL additionally reject zero and a minus sign.

An exact rational SHALL be an object containing canonical numerator and strictly positive denominator decimal strings.
The pair SHALL be reduced by greatest common divisor; zero SHALL have denominator one. Non-reduced, negative-
denominator, and zero-denominator forms SHALL fail rather than being silently normalized as alternate wire spellings.
No exact domain number SHALL be represented as a JSON floating-point number or constrained to JavaScript/64-bit numeric
precision merely by the wire format.

A canonical dimension key SHALL be an array of unique atom/power entries with nonzero powers in the same ascending
UTF-16 code-unit lexicographic atom-ID order used by the authoritative runtime key; dimensionless one is the empty
array. Stable IDs SHALL preserve their exact validated string value. Full grid identity SHALL encode canonical
dimension plus grid ID and positive grid version explicitly.

#### Scenario: Round-trip values beyond 64-bit range
- **WHEN** a grid coordinate, dimension exponent, or rational numerator exceeds signed 64-bit range
- **THEN** canonical string encoding and decoding preserve the exact integer subject only to the selected operational
  decode limits

#### Scenario: Reject rational malleability
- **WHEN** a payload spells one half as `2/4`, `-1/-2`, or with a noncanonical integer string
- **THEN** decoding reports a typed exact-number violation rather than accepting an alternate encoding

#### Scenario: Reconstruct a compound dimension
- **WHEN** a canonical dimension array contains several positive and negative atom powers
- **THEN** decoding reconstructs the exactly equal normalized `DimKey` without parsing an implementation `toString`

#### Scenario: Reject a noncanonical dimension array
- **WHEN** dimension entries are unsorted, duplicated, or contain a zero power
- **THEN** decoding reports their indexed structural violations and does not normalize the record silently

### Requirement: Record schemas preserve sums, products, refinements, and derived fields
Each V1 record SHALL mirror the domain's meaningful sums and products with tagged alternatives and required case-local
fields. It SHALL NOT encode closed alternatives as a kind plus nullable/not-applicable fields, store refinement success
as a boolean, serialize path-dependent evidence, or duplicate a value that the domain deterministically derives from
other encoded fields.

Decoding SHALL invoke the owning identifier/refinement/smart-construction boundary. Expected constructor failures SHALL
remain typed and located at the record field or alternative that supplied the invalid value. The codec SHALL not
reimplement a parallel validation rule, clamp or quantize an invalid value, infer omitted required meaning, or use an
unchecked cast to make a record fit.

#### Scenario: Encode an order activation
- **WHEN** an order has immediate, fixed, or trailing activation
- **THEN** its record contains exactly that tagged payload and none of the fields exclusive to another activation case

#### Scenario: Reconstruct a derived position change
- **WHEN** an order record contains side and positive lot coordinate
- **THEN** order-intent construction derives signed `PositionLots`; the record does not carry a separately mutable sign
  or position-change claim

#### Scenario: Reject a failed refinement
- **WHEN** a record supplies zero for a positive lots, price, version, or tick-offset field
- **THEN** decoding retains the owning typed refinement/domain error and produces no trusted value

### Requirement: Grid-coordinate records restore dependent values through snapshots
V1 SHALL support both a general grid-coordinate record and an asset-qualified grid-coordinate record. A general record
SHALL contain one full `GridIdentity` and exact signed coordinate. An asset-qualified record SHALL additionally contain
one `AssetId`; the dimension in its full grid identity is the recorded expected asset dimension. Neither record SHALL
contain a grid quantum, trusted handle, path-dependent type, catalog lineage, or registry object.

Packing SHALL consume a trusted `GridHandle` and a value on that exact underlying grid, project stable identity plus
coordinate, and perform no lookup, quantization, projection, or scalar reconstruction. General reconstruction SHALL
resolve the recorded dimension before resolving the full grid identity. Asset reconstruction SHALL resolve the asset,
verify its dimension equals the recorded full-grid dimension, and only then resolve the grid. Both SHALL construct the
coordinate through the resolved handle and return a codec-owned dependent package retaining the canonical handle(s)
and typed `GridQuantity`.

Failures SHALL distinguish wire/field problems, unknown dimension, unknown asset, asset-dimension mismatch, unknown
full grid identity, and snapshot/lineage inconsistency. Lookup SHALL be direct through the supplied snapshot and SHALL
not scan other dimensions or recapture live state.

#### Scenario: Pack and restore a general coordinate
- **WHEN** a trusted compound-dimension grid value is encoded and decoded against a snapshot containing its full grid
  identity
- **THEN** the decoded package's dimension, grid handle, coordinate type, coordinate, and exact embedded quantity agree

#### Scenario: Detect changed asset meaning
- **WHEN** an asset-qualified record's stable asset resolves to a dimension different from its recorded grid dimension
- **THEN** decoding returns an asset-dimension mismatch before attempting grid resolution

#### Scenario: Restore a historical grid version
- **WHEN** a record names grid version `1` and the snapshot also contains later versions
- **THEN** decoding selects exactly version `1` and its original immutable mathematical grid

#### Scenario: Avoid hidden quantization
- **WHEN** a caller has an arbitrary exact quantity that is not already a coordinate on the selected grid
- **THEN** the grid-coordinate encoder offers no operation that rounds or silently invents a coordinate for it

### Requirement: Instrument-definition records reconstruct through assembly
An instrument-definition V1 payload SHALL contain exactly the stable `InstrumentId`, `UnderlyingId`, base/quote/
position/settle `AssetId` values, full position-lot and quote-per-base price `GridIdentity` values, and canonical exact
base-per-position and quote-per-position coefficients required by `InstrumentDefinition`. It SHALL contain no trusted
roles, handles, catalog revision/lineage, `InstrumentSpec`, `Instrument`, typed rate witness, venue product-family flag,
or current market data.

Structural decoding SHALL produce the in-memory `InstrumentDefinition` after only syntax, identifier, exact-number, and
local product validation. A separate pure reconstruction operation SHALL compose that result with
`InstrumentAssembler` against one explicit snapshot and total `Instrument` construction. Codec-stage and assembly-
stage failures SHALL remain distinct typed branches; successful construction SHALL carry only proofs issued by the
assembler.

#### Scenario: Decode a raw definition without a catalog
- **WHEN** a V1 instrument payload has valid stable IDs and canonical coefficients
- **THEN** structural decoding produces `InstrumentDefinition` without pretending its identities have resolved

#### Scenario: Assemble a decoded definition
- **WHEN** the caller supplies a snapshot containing coherent roles and grids
- **THEN** reconstruction delegates to assembly and returns an `Instrument` with typed payoff endpoints

#### Scenario: Preserve assembly diagnostics
- **WHEN** several decoded role/grid identities are absent or economically inconsistent
- **THEN** reconstruction retains the ordered `InstrumentAssemblyErrors` rather than converting them to codec strings

#### Scenario: Keep revision out of stable identity
- **WHEN** equal instrument records are assembled against explicitly selected snapshots from different process lineages
- **THEN** the record itself claims no cross-lineage revision authority and each assembly establishes its own handles

### Requirement: Order records reconstruct one immutable instruction algebra
An order V1 payload SHALL contain one `InstrumentId`, side, positive lot coordinate, position effect, one tagged
activation, and one tagged execution alternative. Fixed/trailing activation, market/priced execution, limit/peg
pricing, duration, liquidity constraint, and visibility SHALL encode only their case-valid primitive fields. The record
SHALL omit derived signed position change and every scenario, lifecycle, venue-order, fill, reported-fee, and account
field.

Decoding SHALL require the explicit assembled `Instrument` named by the record. It SHALL construct lots/prices through
that instrument's stable grids, invoke local activation/execution refinements, derive intent position change, and
delegate final combinations to the canonical accumulating order constructor. Structural, refinement, and aggregate
order failures SHALL remain typed and stage-located.

#### Scenario: Round-trip each order alternative
- **WHEN** every valid activation/execution/visibility combination is encoded and decoded with its instrument
- **THEN** the reconstructed order is semantically equal and retains the same exact coordinates and instructions

#### Scenario: Reject a foreign instrument
- **WHEN** a record names one `InstrumentId` but decoding is supplied another instrument
- **THEN** reconstruction fails before attaching record coordinates to the foreign grids

#### Scenario: Reject cross-case fields
- **WHEN** a market-order payload also contains maker-only or iceberg members
- **THEN** structural decoding rejects the extraneous fields instead of ignoring them

#### Scenario: Preserve aggregate order violations
- **WHEN** independently invalid order fields reach canonical order construction
- **THEN** reconstruction retains its deterministic non-empty typed violation collection

### Requirement: Scenario records rebuild associated evidence and validated outcomes
An order-scenario V1 payload SHALL contain its order record exactly once, one activation-observation payload associated
with that order's activation case, one pricing-observation payload associated with its execution/pricing case, and one
ordered non-empty matched-slice array. Every slice SHALL contain a positive lot coordinate, assumed liquidity role, and
a market payload containing price coordinate, canonical exact base-to-settle and quote-to-settle rates, and an ordered
vector of explicitly supplied additional source-asset-to-settle conversions. The enclosing order/instrument supplies
meaning that would otherwise be duplicate.

Decoding SHALL require the explicit assembled instrument and one catalog snapshot for heterogeneous conversion assets.
It SHALL decode the order, build evidence through the reconstructed activation/pricing values, resolve conversion
assets through the snapshot, construct market states/lots/slices, construct `MatchedSlices`, and delegate to canonical
scenario evaluation. Associated-shape mismatch SHALL fail structurally or through the owning semantic boundary without
an untyped evidence cast or JVM object-identity check.

A round-trip-scenario V1 payload SHALL contain exactly one entry and one exit scenario payload. It SHALL omit a separate
held-position or PnL claim. Reconstruction SHALL decode both complete scenarios and invoke the checked round-trip
constructor for identity and exact-flat validation.

#### Scenario: Preserve ordered multi-slice assumptions
- **WHEN** a complete scenario with several prices, roles, and conversions is encoded and decoded
- **THEN** slice order, exact coordinates, conversion endpoints/rates, and validated scenario meaning are preserved

#### Scenario: Reject evidence for another case
- **WHEN** a fixed-activation order record is paired with trailing evidence fields or a direct limit with peg evidence
- **THEN** reconstruction returns a typed structural/associated-shape failure without constructing assumptions

#### Scenario: Resolve a third-asset conversion
- **WHEN** a slice market contains a fee-source asset distinct from instrument roles and the snapshot contains it
- **THEN** decoding resolves the asset, constructs its source-to-settlement typed rate, and retains it in the market

#### Scenario: Reject an empty slice array
- **WHEN** a scenario payload contains no matched slices
- **THEN** `MatchedSlices` reconstruction returns its typed empty-boundary error and no scenario is evaluated

#### Scenario: Recheck hypothetical semantics
- **WHEN** encoded observations, peg resolution, limits, lot total, or liquidity roles violate the reconstructed order
- **THEN** canonical scenario evaluation returns its complete ordered violations rather than trusting prior encoding

#### Scenario: Reconstruct a round trip without duplicate PnL
- **WHEN** entry and exit records decode to one exact-flat round trip
- **THEN** reconstruction retains both scenarios and derives held position while the record carries no cached PnL or
  fee-policy result

### Requirement: Staged diagnostics accumulate only meaningful independent failures
Syntax failure for one payload SHALL prevent structural interpretation of that payload. Once an envelope/payload is
available, independently checkable fields and independent records SHALL accumulate violations. Identifier/refinement
construction SHALL run only for structurally valid fields; catalog lookup SHALL run only for valid stable identities;
and domain assembly/evaluation SHALL run only when its prerequisite values exist.

Wire diagnostics SHALL use a domain-owned non-empty ordered collection and structured paths composed of field and array-
index segments, with syntax offsets where applicable. Family reconstruction errors SHALL wrap wire, catalog,
instrument-assembly, order, evidence, market, scenario, and round-trip causes in distinct typed branches rather than one
universal string error. Ordering SHALL be input record index, stage ordinal, structured path, then owning-domain order.

#### Scenario: Accumulate malformed independent fields
- **WHEN** one structurally parseable payload has several independently invalid IDs and exact-number fields
- **THEN** decoding returns all applicable path-indexed violations in stable schema order

#### Scenario: Suppress dependent lookup
- **WHEN** a full-grid dimension field is malformed
- **THEN** decoding does not fabricate unknown-dimension or unknown-grid errors for a value it could not construct

#### Scenario: Preserve domain causes
- **WHEN** syntax succeeds but assembly, order, or scenario validation fails
- **THEN** the family error retains the original typed domain violations and their established ordering

### Requirement: Batch reconstruction uses one coherent snapshot and is atomic as a result
Every snapshot-dependent family SHALL expose a pure batch reconstruction boundary that receives an indexed immutable
payload collection, one explicit `CatalogSnapshot`, and explicit decode limits. It SHALL use that same snapshot for all
records and every lookup in the batch even if a newer live revision is concurrently published elsewhere.

Independent records SHALL all be evaluated when within limits, and their failures SHALL be accumulated in input order.
If any record fails, the atomic batch result SHALL return the non-empty indexed failures and no vector of partially
trusted successes. If all records succeed, output order SHALL equal input order. A separate single-record decoder MAY
support quarantine workflows, but no batch API SHALL silently drop, reorder, or partially accept records.

#### Scenario: Decode a coherent batch
- **WHEN** every record is valid in snapshot revision `10` while revision `11` is published concurrently
- **THEN** every output is resolved from revision `10` and remains unaffected by revision `11`

#### Scenario: Accumulate failures across records
- **WHEN** records at indices `1` and `4` independently fail
- **THEN** the batch returns both indexed failures in order and no partial success vector

#### Scenario: Preserve output order
- **WHEN** an implementation validates independent records in a different traversal order
- **THEN** the successful output or normalized failures have exactly the specified input-index order

#### Scenario: Avoid per-record live coordination
- **WHEN** a high-volume caller already holds the batch snapshot
- **THEN** reconstruction performs pure snapshot lookups and makes no live-catalog or atomic-reference call per record

### Requirement: Decode limits protect parsing without changing mathematical schemas
The codec boundary SHALL define an immutable `DecodeLimits`-shaped policy covering at least payload character/byte
length, nesting depth, batch size, collection sizes, identifier length, dimension-factor count, exact-integer digit
count, and scenario-slice/conversion counts. A safe documented default SHALL exist, and callers with controlled data MAY
select a different explicit policy.

Limits SHALL be checked before allocating or parsing the bounded structure where practical, especially before
constructing arbitrary-precision integers. Exceeding a limit SHALL return a typed path/indexed limit violation rather
than truncate input, overflow a platform number, throw an expected exception, hang, or overflow the call stack.
Operational limits SHALL not redefine the mathematical domain or stable schema: a canonical writer MAY emit an exact
value larger than a chosen reader profile, and the reader SHALL report that explicit profile limit.

#### Scenario: Reject an oversized integer before conversion
- **WHEN** a canonical decimal string exceeds the configured digit limit
- **THEN** decoding reports a digit-limit violation without constructing the `BigInt`

#### Scenario: Reject an oversized batch deterministically
- **WHEN** input count exceeds the configured batch limit
- **THEN** the batch boundary returns one batch-limit failure and does not begin partial record reconstruction

#### Scenario: Use a controlled larger profile
- **WHEN** an offline trusted replay explicitly raises a limit while preserving the same schema
- **THEN** values within that profile decode with identical exact/domain semantics

### Requirement: Compatibility is verified by schemas, golden vectors, and semantic laws
Each published record type/version SHALL have a checked-in human-readable JSON schema, canonical golden examples, valid
and invalid fixtures, and tests that prevent accidental field/tag/encoding drift. For every supported writer/reader
pair and values within selected decode limits, canonical decode-after-encode SHALL preserve the complete stable record;
domain encode/decode SHALL preserve observable domain meaning when supplied the required same-semantic snapshot or
instrument.

Every generated schema SHALL validate against the JSON Schema Draft 2020-12 meta-schema. JSON-valid schema-level valid
and invalid fixtures SHALL also be checked by an independent Draft 2020-12 validator configured for offline local
resource resolution. This independent check is compatibility evidence, not a second schema definition and not a
replacement for codec/domain semantic tests that cover canonical exactness, refinements, catalogs, and dependent
reconstruction. Canonical renderer output SHALL be checked against applicable official vectors and an independent JCS
oracle in addition to project-owned goldens.

Future readers supporting several versions SHALL parse each frozen version separately and apply explicit pure
migrations to the current boundary/domain input. Writers SHALL not silently emit an older/newer version based on
runtime configuration. Removal of read support for a published version requires an explicit compatibility proposal and
migration path.

Codec-owned boundary record objects and decoded dependent packages SHALL reject Java object serialization. Existing
lower-layer values retain the serialization policy specified by their owning capability; none of those object graphs is
advertised as a durable format. The supported durable value at this boundary is the versioned JSON text/bytes produced
by these codecs.

#### Scenario: Detect accidental writer drift
- **WHEN** a schema field name, tag, canonical ordering/escaping, or exact-number spelling changes without a version
  change
- **THEN** golden/schema compatibility tests fail

#### Scenario: Independently reject a schema-incompatible fixture
- **WHEN** a JSON-valid fixture violates a generated V1 schema's required fields, closed alternatives, or exact
  structural types
- **THEN** both the project codec tests and an offline independent Draft 2020-12 validation check reject it without
  remote schema resolution

#### Scenario: Establish semantic round trip
- **WHEN** a domain value is encoded and decoded using coherent catalog/instrument inputs
- **THEN** the result has the same stable IDs, exact coordinates/rates, alternatives, order, and validated meaning

#### Scenario: Reject Java object serialization as a shortcut
- **WHEN** a caller passes a codec-owned record wrapper or decoded dependent package to `ObjectOutputStream`
- **THEN** serialization fails and the supported path remains explicit versioned encoding plus checked reconstruction

#### Scenario: Preserve lower-layer ownership
- **WHEN** a caller considers serializing a handle, `InstrumentSpec`, `Instrument`, order, or scenario object graph
- **THEN** the codec API makes no persistence promise for it and preserves the fail-closed or unsupported policy defined
  by that value's owning capability

### Requirement: Boundary-codec scope excludes unspecified live and derived records
This capability SHALL encode only the specified catalog, grid-coordinate, instrument-definition, immutable-order,
hypothetical-scenario, and round-trip record families. It SHALL NOT claim that those scenario records are venue fills or
trades, and SHALL NOT invent schemas for live execution lifecycle, account state, fee-policy selection, fee assessment,
PnL audit, risk decisions, market-data feeds, tracing, or metrics without their own semantic proposals.

The codec artifact SHALL provide no database repository, filesystem/network operation, stream processor, transaction,
live-catalog interpreter, availability/delisting decision, or automatic selection of a “current” snapshot. Framing,
transport, storage durability, encryption, signatures, checksums, and access control remain responsibilities of later
boundary/application/runtime integrations unless separately specified.

#### Scenario: Encode a hypothetical scenario
- **WHEN** a complete order scenario is persisted with this codec
- **THEN** its envelope identifies hypothetical scenario data and makes no claim of actual venue execution

#### Scenario: Request trade persistence later
- **WHEN** the system introduces a durable executed-trade fact and repository
- **THEN** a separate proposal defines its identity, event/transaction semantics, and codec rather than repurposing the
  scenario V1 schema

#### Scenario: Keep storage effects absent
- **WHEN** a caller receives canonical JSON from the codec
- **THEN** choosing where/how atomically to store or transmit it remains an explicit application/runtime operation

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

### Requirement: Boundary codecs expose a Scala 3 source boundary

The supported source API for record construction, versioned encoding, decoding, and checked domain reconstruction
SHALL be Scala 3. Completed-artifact Scala clients, negative compiler fixtures, schemas, golden vectors, semantic laws,
and external wire and null-boundary tests SHALL remain the compatibility evidence. The codec artifact SHALL NOT
promise an ordinary-Java domain API or retain a dedicated dynamic Java compiler/classloader fixture for that purpose.

Java-library use inside the codec implementation, exact decimal conversion, canonical external representations,
schema validation, and Java-object-serialization rejection SHALL remain governed by their existing requirements.

#### Scenario: Use codecs from a completed Scala artifact

- **WHEN** a downstream Scala 3 client compiles against completed boundary-codec artifacts
- **THEN** it can construct supported records and encode or decode versioned representations without implementation
  classpath leakage

#### Scenario: Preserve external compatibility evidence

- **WHEN** the codec compatibility suite runs after ordinary-Java API fixture removal
- **THEN** schemas, goldens, independent validation, semantic round trips, wire/null failures, and dependency checks
  continue to cover the published boundary

#### Scenario: Preserve Java serialization rejection

- **WHEN** a caller attempts Java object serialization of a codec-owned record or decoded dependent package
- **THEN** serialization still fails and the supported durable path remains explicit versioned encoding

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
