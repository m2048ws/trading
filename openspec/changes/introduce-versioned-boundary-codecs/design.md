## Context

See `proposal.md` for motivation and the two delta specifications for normative behavior. This is Proposal 9, the final
change in the initial architecture portfolio. It assumes Proposals 0–8 have established final pure artifacts, immutable
append-only catalog state/snapshots, a proof-carrying instrument assembly boundary, algebraic order/scenario models, and
an application/runtime split.

Proposal 1 removes `PackedGridQuantity`, `PackedAssetGridQuantity`, and registry-backed decoding rather than blessing an
unversioned logical record. Proposal 2 states that stable catalog commands may be replayed but handles/snapshots are
never authority-bearing persistence. Proposal 3 makes `InstrumentDefinition` stable-ID domain data but explicitly not a
wire schema. Proposals 4–7 likewise keep economic, order, scenario, fee, and risk values independent of Java
serialization and external I/O.

The repository has no released durable payloads, database schema, trade aggregate, venue execution events, market-data
protocol, or stream integration. We can therefore define V1 without a compatibility shim, but once V1 is accepted its
bytes and meaning become a long-lived contract. The hot-path concern is reconstruction: a high-volume batch must use one
immutable snapshot and ordinary map lookups, not one coordinated live-catalog read per field or record.

## Goals / Non-Goals

**Goals:**

- Create one enforceable codec artifact whose dependency direction is only upward from pure domain modules.
- Make exact numeric and algebraic domain meaning portable without serializing Scala/JVM authority.
- Give each record family an explicit frozen version and one canonical textual representation.
- Reuse domain smart constructors and staged validation instead of building a parallel trust model.
- Preserve path-dependent quantity/grid relationships in genuinely dependent decoded result packages.
- Make malformed-input behavior total, bounded, accumulated where independent, and deterministically ordered.
- Make catalog restart/replay semantics explicit and comparable with the pure transition model.
- Generate compatibility evidence from independently checked schemas/canonicalization, golden fixtures, properties,
  compiler tests, and adversarial inputs.

**Non-Goals:**

- Do not implement storage, transport, transactions, a catalog journal repository, or atomic journal/publication.
- Do not serialize catalog lineage, handles, snapshots, `InstrumentSpec`, or other in-memory proof authority.
- Do not define executed trades, order lifecycle events, account state, fee-policy configuration/results, PnL/risk audit
  envelopes, live market feeds, or operational telemetry schemas.
- Do not add effects, Cats Effect, FS2, a live-catalog dependency, or automatic “current snapshot” selection.
- Do not support arbitrary JSON-to-object mapping, generic Java serialization, reflection-based polymorphic typing, or
  Scala case-class binary compatibility.
- Do not establish Circe, Jackson Databind, the Jackson Scala module, generic derivation, or a JSON-library AST as a
  second domain or public-codec vocabulary.
- Do not add compression, encryption, signing, checksums, transport framing, database indexes, or retention policy.

## Decisions

### 1. Add one pure boundary-codec artifact above current encodable domains

Add:

```text
SBT ID:       boundaryCodecs
directory:    boundary-codecs
artifact:     trading-boundary-codecs
package root: trading.codec
depends on:   trading-quantities,
              trading-reference-data,
              trading-instrument-economics,
              trading-order-model,
              trading-execution-scenario
```

It may depend on Cats Core for pure `Validated`/traversal and on Jackson Core 3.x for strict streaming parsing. The
repository's Java 17 build/runtime baseline supports Jackson 3.x. Jackson is a syntax mechanism behind a package-private
adapter, not the codec algebra or domain model: the production artifact does not add Jackson Databind, the Jackson
Scala module, Circe, or generic object mapping. It does not depend on fee policy or risk because this change defines no
stable representation for those derived/policy values. It does not depend on application or runtime: callers capture a
snapshot elsewhere and pass it as an ordinary value.

The artifact is intentionally one module now. Its record families share exact primitives, envelope dispatch, path/error
types, limits, schema generation, and compatibility tests. If future trade, ledger, or venue schemas introduce heavy
independent dependencies/publication cycles, they may form narrower codec artifacts then.

Alternative considered: put each codec beside the domain type it encodes. Rejected because lower modules would own
wire versions/parser dependencies and could begin treating in-memory representation as compatibility.

Alternative considered: split catalog, quantity, instrument, order, and scenario codecs immediately. Rejected because
the common infrastructure is the larger body of code and there is not yet an independent release/dependency consumer.

### 2. Use explicit V1 record families and codec-owned decoded names

The supported families are conceptually:

```text
CatalogJournalEntry.V1
GeneralGridCoordinateRecord.V1
AssetGridCoordinateRecord.V1
InstrumentDefinitionRecord.V1
OrderRecord.V1
OrderScenarioRecord.V1
RoundTripScenarioRecord.V1
```

Version-specific records are immutable validated boundary data and reject Java serialization. Each family exposes
domain-named `encode`, `parse`, and (where relevant) `reconstruct` operations; there is no public reflection-based
`Codec[Any]` registry. A common private envelope dispatcher may inspect type/version and then call a known family.

Grid reconstruction returns:

```scala
final class DecodedGridQuantity(
  val dimension: DimensionHandle[?]
)(
  val grid: GridHandle[dimension.D]
)(
  val value: GridQuantity[dimension.D, grid.G]
)

final class DecodedAssetGridQuantity(
  val asset: Asset
)(
  val grid: GridHandle[asset.D]
)(
  val value: GridQuantity[asset.D, grid.G]
)
```

Exact Scala spelling may use an abstract type member for the first package, but the dependencies cannot be weakened.
`Decoded` is appropriate here because record-versus-reconstructed phase is a real state distinction; it is not a
blanket prefix for ordinary trusted domain values. The old `Resolved*`, `Packed*`, and registry names do not return.

Alternative considered: make all inputs and outputs one `BoundaryRecord` sum. Rejected because heterogeneous dynamic
dispatch is useful only at framing routers, while normal callers deserve family-specific types, required context, and
errors.

Alternative considered: call every trusted result `Resolved*`. Rejected because `Asset`, `GridHandle`, `Instrument`,
and `OrderScenario` already communicate their own authority; only existential record reconstruction needs a phase name.

### 3. Use a small inspectable internal wire-schema algebra

The repeated structure is real: primitives, refined primitives, products, vectors, tagged coproducts, path tracking,
encoding, accumulating decoding, and JSON Schema generation. Implement one package-private `WireSchema[A]`-shaped
algebra rather than hand-writing unrelated encoders/decoders/schema files for every family.

Conceptually each schema carries or can interpret:

```scala
final case class WireSchema[A] private[codec] (
  shape: SchemaShape,
  encodeValue: A => ValidatedNec[WireEncodeViolation, JsonValue],
  decodeValue: DecodeCursor => ValidatedNec[WireDecodeViolation, A]
)
```

It supports:

- invariant mapping for true total isomorphisms;
- checked refinement mapping for identifiers, positive values, rationals, and domain constructors;
- associative product composition with deterministic left-to-right field order;
- tagged coproduct composition for closed alternatives;
- vector traversal with indexed paths and explicit collection limits;
- interpretation to a JSON Schema Draft 2020-12 document.

The actual implementation may separate shape, encoder, and decoder types if Scala inference is clearer. The algebra
remains internal and law-tested; public callers see record/domain operations and domain-owned error aggregates. This is
an honest use of algebraic abstraction because there are many consumers and multiple interpretations. It is not a
tagless-final application capability and has no `F[_]`.

Schema documents under `src/main/resources` are generated from the same shape and checked into source control for human
review. A test regenerates each document and requires byte equality, so source and documentation cannot drift.

Alternative considered: generic case-class derivation. Rejected because Scala member names/defaults/sealed hierarchy
changes must not silently become wire compatibility, and strict unknown/case-local field behavior needs deliberate
control.

Alternative considered: manually maintain JSON Schema separately from codecs. Rejected because two definitions of the
same products/coproducts will drift; generated documents plus golden fixtures are stronger.

Alternative considered: publish the schema algebra for downstream user-defined records. Rejected because no extension
contract or compatibility need exists yet; keeping it internal permits simplification without creating a framework.

### 4. Isolate an imperative strict JSON parser behind an immutable AST

Use a narrowly scoped, explicitly configured Jackson Core 3.x streaming parser to build a package-private immutable
`JsonValue` AST while preserving raw number spelling and source locations. Enable strict duplicate detection and keep
permissive non-standard JSON features disabled. Exact public character/UTF-8-byte limits are checked by project code at
the input boundary before parsing; Jackson stream-read constraints additionally enforce depth/string/name/number/
document bounds as defense in depth. Library resource constraints do not silently redefine the documented
`DecodeLimits` contract. Parser mutation and expected parse exceptions remain lexical to one adapter and are converted
immediately to `WireDecodeViolation`; neither parser objects nor library exceptions appear publicly.

The project owns a renderer for its restricted AST that follows RFC 8785 JCS rules: well-formed Unicode, UTF-16 code-
unit object-member sorting, required escaping, and no insignificant whitespace. Exact domain numbers are JSON strings,
so JCS floating-number restrictions apply only to the small literal schema-version field. Renderer behavior is checked
against relevant JCS vectors, project goldens, and a test-only RFC-listed Java JCS implementation used solely as an
independent oracle. The oracle's types and rendering choices do not enter production code or define the contract.

The envelope is exactly the product:

```json
{
  "payload": { },
  "recordType": "trading.example",
  "schemaVersion": 1
}
```

JCS determines actual member order. Record-type strings are fixed ASCII constants and version `1` is a JSON integer.
The public durable output is immutable `String`; callers explicitly choose UTF-8 bytes/framing in their adapter.

Alternative considered: expose a parser library's JSON AST. Rejected because it leaks a dependency and may not retain
duplicate members/raw numeric spelling needed for strict validation.

Alternative considered: derive JSON directly with reflection/macros. Rejected because it couples V1 to Scala layout
and often defaults to skipping unknown fields or normalizing numbers.

Alternative considered: use Circe as the general Scala JSON/codec vocabulary. Rejected for this boundary because its
AST and derivation-oriented codec layer would duplicate the deliberately small wire-schema/strict-token model without
defining the durable schema contract. This is a scoped decision, not a judgment against Circe for unrelated application
JSON where its vocabulary is the actual desired abstraction.

Alternative considered: invent a binary format. Rejected because current needs favor inspectable configuration/journal
records and golden review; a future binary transport can wrap the same domain boundary under its own version.

### 5. Make exact primitive codecs strict and reusable

The primitive layer includes:

```text
CanonicalIntegerString
PositiveIntegerString
CanonicalRationalRecord
CanonicalDimensionRecord
AssetId / GridId / GridVersion / GridIdentity
InstrumentId / UnderlyingId
closed enum tags used by order/scenario
```

Integer syntax is checked for digit length before `BigInt` allocation. Rational decoding constructs numerator and
denominator, verifies positive denominator/gcd/zero form, then calls the exact `Rational` constructor and confirms its
canonical projections match the record. It does not accept-and-normalize wire aliases.

Dimension encoding maps `DimKey.powers` to an ordered array of `{ "atom": ..., "power": ... }`. Decoding validates all
entries independently where possible, then verifies nonzero, unique, and ascending atom order before calling `DimKey`;
it compares the result's public powers with the supplied vector to guard implementation drift. It never parses
`DimKey.toString`.

All stable/domain identifier constructors are invoked rather than bypassed. Because JVM `String` can contain unpaired
surrogates even when domain identifier invariants otherwise hold, encoding also returns typed path-located malformed-
Unicode failures. Valid Unicode identifiers are preserved exactly; no trimming, case folding, or normalization is
introduced at the codec boundary.

Alternative considered: encode integers as JSON numbers. Rejected because cross-language readers commonly truncate
beyond 53/64 bits and decimal/exponent aliases undermine canonical equality.

Alternative considered: normalize noncanonical rationals/dimensions on read. Rejected because multiple byte strings for
one signed/audited value invite malleability and hide upstream producer defects.

### 6. Thread explicit decode context and convert validation containers at boundaries

`DecodeContext` contains the immutable limits, zero-based record index where applicable, and structured path. Paths are
products of field-name and zero-based array-index segments; syntax locations additionally retain character/byte offset,
line, and column when the parser supplies them. Field strings are appropriate here because they name external schema
syntax, not internal domain semantics.

The initial safe `DecodeLimits.default` is:

```text
payload characters       1,000,000
UTF-8 payload bytes       4,000,000
nesting depth                    32
batch records                10,000
object members                  128
generic array entries        10,000
identifier/string chars       4,096
integer digits                4,096
dimension factors               256
catalog commands             10,000
scenario slices              10,000
market conversions            1,024
```

Exact named constants are part of documentation/tests but not the JSON schema; tuning defaults later is an operational
compatibility decision, not a record-version change. `DecodeLimits.create` accumulates invalid nonpositive/relationship
configuration. There is no unsafe global mutable limit and no special “unbounded” sentinel; controlled replay supplies
explicit sufficiently large positive limits.

Structural decoding uses `ValidatedNec` (or equivalent) internally. It is converted to domain-owned non-empty ordered
errors at every public boundary. Independent fields/records traverse applicatively; dependent phases use `Either` or
validated-and-then sequencing. Errors sort by record index, stage ordinal, path, and nested domain order. Syntax can
report only what is observable before an AST exists; later field errors are not fabricated.

Alternative considered: fail at the first field everywhere. Rejected because configuration/record fields are commonly
independent and the charter requires useful deterministic accumulation.

Alternative considered: continue after missing prerequisites. Rejected because it manufactures unknown IDs, evidence,
or scenario errors from values that do not exist.

Alternative considered: expose Cats `ValidatedNec` publicly. Rejected because callers need codec/domain vocabulary and
the validation container is an implementation choice.

### 7. Restore grid coordinate persistence at the handle/snapshot boundary

V1 payloads are:

```text
general: GridIdentity × coordinate
asset:   AssetId × GridIdentity × coordinate
```

The dimension inside `GridIdentity` is also the asset record's expected dimension, avoiding a second duplicate field.
Packing is total after Unicode encoding checks because the function receives `GridHandle[D]` and
`GridQuantity[D, grid.G]`; it reads the handle identity and exact coordinate. Arbitrary `Quantity[D]` has no packing
overload, so quantization remains explicit elsewhere.

General decode performs:

```text
record
  -> snapshot.resolveDimension(identity.dimension)
  -> snapshot.resolveGrid(identity)
  -> grid.fromCoordinate(record.coordinate)
  -> DecodedGridQuantity
```

Asset decode performs:

```text
record
  -> snapshot.resolveAsset(assetId)
  -> compare asset.dimension.key with identity.dimension
  -> snapshot.resolveGrid(identity)
  -> grid.fromCoordinate(record.coordinate)
  -> DecodedAssetGridQuantity
```

The snapshot has direct full-identity indexes, so no fallback scan exists. Construction stays within the dependent
scope where Scala knows the resolved handle's type members. Any unavoidable existential narrowing is inside the same
checked key/lineage boundary and receives packaged-JAR adversarial coverage.

Alternative considered: persist the grid quantum with each value and reconstruct an anonymous grid. Rejected because
it could silently reinterpret a stable key under a conflicting definition and would not recover catalog authority.

Alternative considered: omit dimension from asset records. Rejected because a stable record would then fail to detect
asset-ID semantic drift when replayed against independently bootstrapped definitions.

### 8. Separate instrument syntax decoding from trusted assembly

`InstrumentDefinitionRecord.V1` structurally mirrors Proposal 3's stable-ID definition product. The record codec first
returns an `InstrumentDefinition`; this is useful for configuration inspection and does not imply catalog membership.

A second operation composes:

```text
JSON -> InstrumentDefinition
     -> InstrumentAssembler.assemble(definition, snapshot)
     -> Instrument.fromSpec
```

Its public error is a two-stage sum retaining either codec failures or `InstrumentAssemblyErrors`. It does not copy
assembly cases. Success returns `Instrument`; callers needing the intermediate definition use the first operation, and
assembly already exposes `InstrumentSpec` if they need to inspect the trust boundary directly.

The payload has no catalog revision. Reproducible applications persist/select the catalog journal prefix separately and
pass its rebuilt snapshot explicitly. Revision is lineage-local diagnostic context, not instrument identity.

Alternative considered: serialize `InstrumentSpec` because it has all required data. Rejected because it contains
path-dependent handles/lineage and would bypass reconstruction after restart.

Alternative considered: make codec decode directly to `Instrument` with duplicated validation. Rejected because it
would erase the useful raw definition phase and create a second assembler.

### 9. Mirror the order algebra and derive its proofs during decode

`OrderRecord.V1` stores:

```text
instrumentId
side
lots coordinate
position effect
activation = immediate
           | fixed(reference, comparison, trigger-price coordinate)
           | trailing(reference, comparison, positive tick offset)
execution  = market(non-resting duration)
           | priced(
               pricing = limit(price coordinate)
                       | pegged(reference, signed tick offset),
               duration,
               liquidity constraint,
               visibility = displayed | hidden | iceberg(displayed-lots coordinate)
             )
```

Exact enum/tag vocabularies are listed in V1 schema resources and frozen. No optional field is used to flatten the
alternatives. `positionChange` is absent because `OrderIntent.create` derives it from side/lots; instrument/grid identity
inside component values is absent because the root `instrumentId` plus explicit supplied `Instrument` owns it.

Decode checks the root identity before using coordinates, constructs `Lots`/`Price` through instrument smart
constructors, constructs the chosen local alternatives/refinements, creates intent, and calls canonical accumulating
`Order.create`. Codec/refinement/order failures retain separate stages. Pattern matching on the record coproduct gives
the exact Scala constructors; no `Any` evidence or kind-to-cast table is used.

Alternative considered: persist every derived order field for audit convenience. Rejected because duplicated signed
position/instrument claims can disagree; audit can recompute and retain the stable input record.

Alternative considered: persist a generic map of venue order attributes. Rejected because this schema is the portable
pure order model, not a venue adapter payload.

### 10. Encode scenarios as reproducible assumptions, not execution facts

`OrderScenarioRecord.V1` embeds its order record exactly once, then case-associated observations and one non-empty
ordered slice vector. Its market payload is:

```text
price coordinate
base-to-settle rational
quote-to-settle rational
additional conversions: Vector[(source AssetId, source-to-settle rational)]
```

The instrument determines settlement target; repeating target ID per conversion is unnecessary. Additional conversion
order is retained exactly, as required for deterministic diagnostics/fee attribution; it is not sorted as an unordered
map. Base/quote rates are separate because market coherence is an explicit invariant and the domain exposes both.

Decode proceeds branch-sensitively:

1. Decode the embedded order against the explicit instrument.
2. Pattern-match its activation/execution alternatives and decode only the associated observation shape.
3. Build evidence through those exact instruction values so same-shape semantics are rechecked.
4. Resolve every additional source `AssetId` through the one supplied snapshot and construct endpoint-typed
   source-to-settle rates immediately from decoded rationals.
5. Construct base/quote endpoint rates, additional `SettlementConversion`s, `MarketState`, `Lots`, and each slice through
   instrument-economic smart constructors.
6. Traverse into `MatchedSlices` and call canonical `OrderScenario.evaluate`.

The scalar rational exists only in the record representation while endpoints are external IDs; it is converted to
typed `Rate` at the reconstruction boundary and never becomes the calculation kernel.

`RoundTripScenarioRecord.V1` is exactly entry × exit. It stores neither held position nor cached price/fee/net PnL.
Decode runs both scenario branches independently where possible, accumulates their reconstruction failures in leg
order, then invokes `RoundTripScenario.create` only if both exist.

Alternative considered: encode a successful scenario object graph. Rejected because associated evidence and handles
must be reconstructed, and object identity is explicitly not its semantics.

Alternative considered: treat scenario records as venue fills. Rejected because they represent complete hypothetical
assumptions; operational execution needs separately specified IDs, lifecycle, ordering, and provenance.

### 11. Make batch reconstruction all-valid-or-errors over one snapshot

Family-specific batch APIs accept `Vector[String]`, one explicit snapshot/instrument context as required, and limits.
They first reject a batch exceeding `maxBatchRecords`; otherwise they traverse every record with a zero-based index.
All successes are returned in input order only when there are no failures. If any fail, the API returns the normalized
non-empty indexed errors and no partial vector.

This is result atomicity, not an external transaction: no state changes. Callers needing per-record quarantine use the
single-record operation explicitly and own that policy. Mixed-family routing may parse the envelope type first, but
heterogeneous result orchestration is not hidden in one `Vector[Any]` API.

Snapshot-dependent batches receive one `CatalogSnapshot` argument. They never accept `LiveCatalog[F]`; an application
workflow does the one effectful capture before invoking them. Concurrent catalog publication cannot alter an in-flight
decode.

Alternative considered: return both successes and failures in one report by default. Rejected because callers may
mistake partial reconstruction for an atomically valid ingestion batch; the explicit single-record path makes policy
visible.

Alternative considered: capture a snapshot inside each record decoder. Rejected because it adds effects, permits mixed
revisions, and recreates the registry hot path.

### 12. Journal only published batches and replay their claimed revisions

`CatalogJournalEntry.V1` contains successor revision plus ordered `CatalogBatch`. `CatalogJournalEntry.fromPublished`
accepts the submitted batch and `CatalogCommit.Published` outcome; it has no constructor from failure/unchanged. This
prevents normal live code from journaling retries as revisions, while decoded external data is still revalidated.

The pure API is conceptually:

```scala
object CatalogReplay:
  def rebuild(
    fresh: CatalogState,
    entries: Vector[CatalogJournalEntry.V1]
  ): Either[CatalogReplayFailure, CatalogReplayResult]
```

It first verifies revision zero and empty indexes. `CatalogReplayResult` retains final `CatalogState` and projects its
snapshot/revision; it need not store duplicate snapshot data. For each zero-based entry index, expected revision is
`current.revision.next`; mismatch fails before transition. `CatalogModel.commit` must return `Published` with that
revision. `Left` becomes a contextual replay failure preserving all catalog violations; `Unchanged` becomes
`UnexpectedUnchanged`. Later entries are not evaluated because their starting state is unavailable.

`decodeAndRebuild` first structurally decodes all entry strings applicatively. Replay begins only on full decode
success. On stateful failure it reports entry index, recorded/expected/last-successful revision, and typed cause, but not
a successful partial state.

Each replay root is fresh and generative. Same journal means equal stable definitions and revision sequence, not equal
lineage. Replaying a prefix is the historical snapshot mechanism. There is no encoded checkpoint because safely
materializing/reloading handle authority requires a future database/runtime design.

Alternative considered: store serialized snapshots periodically. Rejected because a blob cannot safely restore private
generative lineage/path-dependent handles and bypasses catalog validation.

Alternative considered: sort journal entries by recorded revision. Rejected because order/gaps are integrity evidence;
reordering would hide a corrupt or incompletely delivered history.

Alternative considered: permit unchanged entries as heartbeat revisions. Rejected because Proposal 2 defines revisions
as actual non-empty publications; operational heartbeats need a separate event stream.

### 13. Keep codec compatibility separate from storage durability

Every record V1 has:

- a generated checked-in JSON Schema Draft 2020-12 document with stable URN `$id` and no remote references;
- canonical JCS golden strings;
- valid/noncanonical/malformed/unknown/limit fixtures;
- structural and semantic round-trip properties;
- mutation/fuzz tests asserting typed failure rather than expected exceptions;
- packaged-JAR compiler and Java-serialization boundary tests.

The checked-in schema documents are also validated by a test-only NetworkNT JSON Schema Validator 3.x interpreter,
configured for Draft 2020-12 and local resources only. Tests validate each generated document against its meta-schema
and check JSON-valid schema-level valid/invalid fixtures without remote reference resolution. This is independent
compatibility evidence, not a second schema definition and not a substitute for codec/domain semantic tests: JSON
Schema cannot express every exact canonicality, catalog, refinement, or path-dependent reconstruction invariant.

The validator and JCS oracle are test mechanisms. Their dependencies remain off the production/runtime classpath, and
their Jackson 3.x compatibility is pinned deliberately rather than inherited accidentally.

Readers accept only enumerated versions. A future V2 gets a separate V2 record schema plus a pure `V1 -> current input`
and `V2 -> current input` migration; V1 record parsing/goldens never change. Writers emit one current version explicitly,
not a runtime-negotiated guess. Removing read support requires a later migration proposal.

Canonical JSON makes stable signing/hashing possible later but this proposal provides neither. Similarly,
`CatalogJournalEntry.fromPublished` followed by an external write is not atomic with in-memory publication. A future
durable catalog interpreter must define transaction/outbox/recovery behavior rather than assuming the codec provides
durability.

Alternative considered: call case-class equality and unit tests sufficient. Rejected because compatibility includes
text bytes, schemas, unknown fields, exact numbers, limits, and cross-lineage reconstruction, not merely current JVM
values.

## Risks / Trade-offs

- [V1 covers several nested domain families and is a large compatibility surface] → Keep families independently
  versioned, generate schemas from one internal algebra, freeze goldens, and omit derived/live concepts without a clear
  persistence consumer.
- [A custom JCS renderer can be subtly wrong] → Restrict the JSON AST, follow RFC 8785 precisely, test official/relevant
  canonicalization vectors and Unicode edge cases, compare it with an independent test-only reference implementation,
  and replace it behind the same contract if a vetted production library is preferable during implementation.
- [An imperative parser may throw or leak mutable state] → Isolate it in one adapter, configure strict limits/duplicates,
  catch expected parse failures immediately, fuzz it, and expose only immutable AST/errors.
- [Accumulating structural errors can consume memory on hostile batches] → Bound payloads, fields, arrays, digits, and
  batch count before/while parsing; return no partial success vector.
- [Finite decode limits mean a writer can emit a mathematically valid value rejected by a default reader] → Make limits
  explicit operational policy, document defaults, and test larger controlled profiles without weakening exact schemas.
- [Scenario decoding must recover associated path-dependent evidence] → Mirror coproducts, pattern-match before
  dependent construction, keep any unavoidable narrowing lexical to checked sealed cases, and add compiler/adversarial
  fixtures for each alternative.
- [Scenario market records can drift from MarketState observers] → Require Proposal 4's public base/quote/additional
  conversion observers, encode only those stable projections, and reconstruct through the canonical market boundary.
- [A journal record may be persisted after in-memory publication fails to be durably recorded] → State explicitly that
  codecs are pure representation only; future durable interpreters own atomic publication/journaling.
- [JSON schemas can be mistaken for venue/trade protocols] → Use record-family names documenting hypothetical/domain
  status and exclude execution/trade/event meanings normatively.
- [Schema-validator/JCS-oracle dependencies can leak into production or drift onto incompatible Jackson lines] → Scope
  them to tests, pin Jackson-3-compatible versions independently, inspect production dependency reports/public APIs,
  and keep project codecs/goldens authoritative.

## Migration Plan

1. Apply Proposals 0–8 and verify final module names, reference-data identities/snapshots, instrument definition/
   assembly, economic observers, and order/scenario alternatives match the complete accepted portfolio.
2. Add the `boundaryCodecs` project, one-way dependencies, Cats Core and Jackson Core 3.x production dependencies,
   NetworkNT/JCS-oracle test dependencies, resources, external-artifact wiring, and compiler guards against effect/
   reverse dependencies or JSON-library leakage.
3. Implement immutable JSON AST/parser isolation, JCS rendering, structured paths/errors, `DecodeLimits`, and the
   internal wire-schema algebra with law/generation tests.
4. Implement/test exact integer, rational, Unicode, dimension, stable-ID, grid-identity, envelope, and version dispatch
   codecs plus generated schemas/goldens.
5. Implement general/asset grid-coordinate V1 packing, snapshot reconstruction, dependent packages, batch traversal,
   historical-version tests, and removal-name negative fixtures.
6. Implement catalog command/journal V1, published-outcome construction, structural batch decode, sequential pure replay,
   prefix/failure/model tests, and fresh-lineage checks.
7. Implement instrument-definition V1 structural decode and assembly/total-construction composition with stage-preserving
   errors and coherent-snapshot batch tests.
8. Implement order V1 products/coproducts and reconstruction through the instrument/order smart constructors, including
   exhaustive alternatives and derived-position negative tests.
9. Implement scenario/round-trip V1 observations, market conversions, slices, evidence reconstruction, canonical
   scenario evaluation, round-trip construction, and semantic/golden/property tests.
10. Generate/check in all JSON Schema and golden resources, validate schemas offline against Draft 2020-12 and fixtures,
    compare canonical output with the independent JCS oracle, add malformed/unknown/duplicate/null/Unicode/limit/fuzz/
    Java-serialization tests, and document supported record families and replay workflow.
11. Extend the shared non-published JMH project with codec parsing/rendering/batch-reconstruction benchmarks while
    retaining deterministic correctness/limit assertions in ordinary tests.
12. Run formatting, clean dependency-ordered compilation, all focused/property/compiler/adversarial/external-artifact
    tests, focused JMH smoke runs, the full repository matrix, strict OpenSpec validation, and portfolio-wide consistency
    review.
13. Prepare the staged commit-ready worktree for fresh independent review; do not add storage/runtime integrations,
    archive, commit, or release outside steward authorization.

Rollback before release is a source-level revert of the codec artifact/resources/build wiring. The old unversioned
packed records remain removed and no external storage has been written. After V1 is released or external payloads are
created, rollback must retain V1 read support or provide an explicit data migration rather than deleting the schema.
