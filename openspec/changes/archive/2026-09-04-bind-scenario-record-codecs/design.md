## Context

See `proposal.md` for motivation. `OrderScenarioRecord` and `RoundTripScenarioRecord` already own V1 record
projection, canonical envelope encoding, parsing, schema generation, domain reconstruction, decode-and-reconstruct,
and atomic ordered batch reconstruction. Encoding currently repeats an `Instrument`; reconstruction repeats that
instrument and one immutable `CatalogSnapshot`. Both families already preserve associated evidence, dependent market
state, exact errors and paths, explicit `DecodeLimits`, and stable record indices.

The instrument is stable shared context for encoding. The instrument-snapshot product is the smallest coherent context
for reconstruction because catalog-backed conversions must use one snapshot generation. Parsing, record-only writing,
and schemas require neither value and must remain independently usable.

## Goals / Non-Goals

**Goals:**

- Bind scenario projection and canonical scenario encoding to one exact instrument across both record families.
- Bind all scenario reconstruction modes to that instrument and one immutable snapshot across both record families.
- Preserve exact dependent result types, canonical bytes, typed failures, diagnostic order and location, explicit
  limits, batch atomicity/order, and current null behavior.
- Keep the two record companions as the only implementation of record, wire, reconstruction, and batch semantics.
- Make both immutable contexts safely reusable in sequential, interleaved, and concurrent pure calls.
- Prove concise positive and incompatible negative use against the completed boundary-codec artifact.

**Non-Goals:**

- Move or duplicate parsing, record-only encoding, schemas, record models, decode limits, or indexed diagnostics into a
  bound context.
- Remove, narrow, or deprecate current `OrderScenarioRecord` or `RoundTripScenarioRecord` entry points.
- Change envelopes, schema versions, canonical JSON, associated-evidence replay, catalog resolution, market-state or
  scenario validation, error types, batch semantics, or Java-serialization rejection.
- Capture a live catalog, mutable catalog state, decode limits, record index, input batch, parser, cache, resource,
  effect, application/runtime interpreter, persistence client, or scenario valuation.
- Add a repository-wide facade, type class, macro, code generation, dependency coordinate, or JDK compatibility
  change.

## Decisions

### Add one codec-owned facade with two distinct contexts

`ScenarioRecord.encoder(instrument)` will return a final immutable
`ScenarioRecord.Encoder[instrument.type]`. Its public methods will project and encode both scenario families:
`order`, `encodeOrder`, `roundTrip`, and `encodeRoundTrip`.

`ScenarioRecord.decoder(instrument, snapshot)` will return a final immutable
`ScenarioRecord.Decoder[instrument.type]`. Its public methods will cover each existing snapshot-dependent mode for
both families: `order` and `roundTrip` for supplied records, `decodeOrder` and `decodeRoundTrip` for supplied
wire inputs, and `orderBatch` and `roundTripBatch` for ordered wire vectors.

One facade is preferred over adding separate contexts to both record companions because each RFC context must be
reused across both scenario families. Separate encoder and decoder classes prevent an encoder from acquiring reference
data and prevent a decoder from hiding parsing policy or record location. An instrument method, extension, ambient
given, or service interface was rejected because the codec artifact owns record translation.

Each class retains the exact instrument singleton. Public method parameters and results use
`instrument.PositionD`, `instrument.BaseD`, `instrument.QuoteD`, and `instrument.MarketState`, so ordinary
calls infer the dependent relationships without type arguments or local role projections.

### Delegate each operation to the matching record companion

Every context method will be a typed thin delegate:

- encoder order methods call the matching `fromScenario` operation;
- encoder wire methods call the matching `encodeScenario` operation;
- decoder record methods call the matching `reconstruct` operation;
- decoder wire methods call the matching `decodeAndReconstruct` operation; and
- decoder batch methods call the matching `reconstructBatch` operation.

Current companion methods remain available and remain the single implementation of projection, encoding, parsing,
validation, reconstruction, and batching. The facade will not reverse those entry points through itself, precompute a
record, widen a return type, extract an `Either`, catch an expected failure, or use an unchecked cast.

`DecodeLimits` and `recordIndex` remain method parameters with their current defaults. The factories will not move
canonical operation-time validation into context creation, so null scenarios, records, input, limits, and captured
values continue to reach the same direct boundary in the same delegation path.

### Keep snapshot coherence structural and contexts field-only

`Decoder` stores exactly the supplied immutable `CatalogSnapshot`; every delegate passes that same object to the
current reconstruction operation. A batch therefore retains the existing single-snapshot behavior without a lookup,
callback, live port, or per-record observation. Constructing a later snapshot cannot change an existing decoder.

`Encoder` stores only its instrument. Neither class stores operation inputs or results, memoizes wire values, mutates
state, synchronizes, or uses thread-local/ambient context. Repeated and concurrent calls remain pure independent
delegations, with no new nested traversal or per-element coordination.

### Verify behavior and the completed-artifact boundary

A focused `ScenarioRecordScopeSuite` will compare all ten bound methods with their direct counterparts for exact
result ascriptions, records, canonical golden bytes, successful reconstructions, malformed/version/limit failures,
domain and catalog failures, deterministic multi-error ordering, record paths and indices, and batch atomicity/order.
It will retain context-free parser, record writer, and schema calls, compare null-boundary behavior, exercise distinct
immutable snapshots, and reuse both contexts sequentially, interleaved, and concurrently.

A positive packaged Scala fixture will reuse one encoder and one decoder across order and round-trip scenarios without
explicit dimension type arguments or local `D`/`B`/`Q` aliases at the calls. An independently valid negative
fixture will pass scenarios built for incompatible instrument dimensions to both encoder families; only the marked
calls must fail with ordinary type mismatches. Existing completed-artifact checks will continue to prove that records,
parsing, schemas, and reconstruction remain in `trading-boundary-codecs` and that its dependency cone gains no live
catalog, application, runtime, effect, persistence, stream, telemetry, or venue responsibility.

## Risks / Trade-offs

- [The facade becomes a second codec implementation] → Keep every method a one-to-one typed delegate and compare all
  successes and complete failures with its corresponding record companion.
- [One context quietly absorbs unrelated policy] → Store only the exact instrument or instrument-snapshot product and
  leave limits, indices, records, wire inputs, parsing, record writing, and schemas explicit or context-free.
- [A captured snapshot is confused with live reference data] → Accept only `CatalogSnapshot`, retain the exact object
  for every call, test distinct generations, and preserve the codec artifact's dependency exclusions.
- [Dependent types widen through the umbrella facade] → Return singleton-parameterized contexts, ascribe exact results
  in focused and packaged-client tests, and compile incompatible scenario fixtures negatively.
- [Defaults or delegation change diagnostics] → Forward limits and record indices explicitly and compare canonical
  bytes, failure alternatives, full ordering, paths, indices, versions, and null behavior.
- [Reusable syntax introduces cached or order-dependent behavior] → Use final field-only classes and verify independent
  sequential, interleaved, and concurrent calls.
- [Method names obscure the two record families] → Keep paired `order`/`roundTrip` names and exercise every method in
  one operation table and the packaged fixture.

## Migration Plan

1. Add the codec-owned encoder and decoder contexts with ten direct typed delegates while retaining every current
   record-family entry point.
2. Add focused equivalence, compatibility, coherent-snapshot, invalidity, null, reuse, ordering, and concurrency tests.
3. Add completed-artifact positive and negative compiler fixtures and retain dependency-boundary assertions.
4. Run reflection guards, formatting, focused checks, the clean JDK-25 repository test/JMH-compile matrix, and automated
   Task Group review. Roll back by reverting the dedicated Task Group commit; no data, wire, schema, dependency, or
   runtime migration is required.
