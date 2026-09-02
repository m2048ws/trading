# trading-boundary-codecs

This artifact is the pure boundary representation owner under `trading.codec`. It sits above quantities, reference
data, instrument economics, the order model, and execution scenarios; none of those lower artifacts depends back on
codecs.

The production classpath is intentionally limited to those five domain artifacts, Cats Core for internal pure
validation/traversal, and Jackson Core 3.x for the package-private strict streaming adapter. It contains no fee
policy, risk, application, runtime, effect, stream, persistence, network-client, clock, transaction, tracing, metrics,
Jackson Databind/Scala module, Circe, schema-validator, or JCS-oracle dependency.

NetworkNT JSON Schema Validator 3.x and the RFC 8785-listed Java JSON Canonicalization implementation are test-only
independent checks. The seven checked-in schemas live under `src/main/resources/trading/codec/schema`; canonical golden
records and histories live under `src/test/resources/trading/codec/golden`. The compatibility suite regenerates these
resources byte-for-byte, validates every schema against the Draft 2020-12 meta-schema, and validates JSON-valid fixtures
with remote resource fetching disabled.

The public foundation owns structured paths, syntax locations, stage/limit vocabulary, typed violations, non-empty
aggregates, validated immutable `DecodeLimits`, and checked `RecordType`/`SchemaVersion` values. Its package-private
kernel owns the immutable JSON AST, strict Jackson adapter, restricted RFC 8785-compatible renderer, invariant
wire-schema algebra, Draft 2020-12 interpreter, reusable exact integer/rational/stable-ID/dimension/grid-identity
schemas, and family-specific envelope dispatch. Operational limits are intentionally absent from generated
mathematical schemas.

Exact integers use canonical decimal JSON strings, rationals use reduced numerator/positive-denominator string records,
dimensions use unique nonzero factors in authoritative UTF-16 order, and full grid identity retains dimension, grid ID,
and positive grid version. Every record is wrapped as exactly `{payload, recordType, schemaVersion}`. V1 readers are
frozen and explicit: an unknown version is a typed failure, writers always emit V1, and operational `DecodeLimits` do
not alter generated mathematical schemas. A future version requires a separate frozen schema and explicit pure
migration; it is never selected from runtime configuration.

## Record families and reconstruction

| V1 family | Public owner | Stable input and reconstruction context |
| --- | --- | --- |
| General grid coordinate | `GeneralGridCoordinateRecord` | Full `GridIdentity` plus exact coordinate; reconstruct with one `CatalogSnapshot` |
| Asset grid coordinate | `AssetGridCoordinateRecord` | Asset ID, full grid identity, exact coordinate; reconstruct with one snapshot |
| Catalog journal entry | `CatalogJournalEntry` / `CatalogReplay` | Published ordered `CatalogBatch` plus successor revision; replay from explicit fresh state |
| Instrument definition | `InstrumentDefinitionRecord` | Stable IDs, listing grids, exact payoff; assemble with one snapshot |
| Immutable order | `OrderRecord` | Stable instrument ID and closed instruction alternatives; reconstruct with one instrument |
| Hypothetical order scenario | `OrderScenarioRecord` | One immutable order, observations, and ordered market slices; reconstruct with one instrument and snapshot |
| Hypothetical round trip | `RoundTripScenarioRecord` | Entry and exit scenarios only; reconstruct with one instrument and snapshot |

Each family provides canonical text encoding, typed parsing, and a stable-URN local-reference schema. Snapshot-dependent
batch APIs receive the snapshot explicitly, reject oversized batches before record work, evaluate every independent
within-limit record, and return either all values in input order or all indexed failures with no partial success vector.
The caller captures a live snapshot once outside the codec; codecs never choose a current snapshot or coordinate with a
live catalog.

Catalog journal decoding is structural and accumulating before replay. Replay then checks each claimed successor
revision sequentially through `CatalogModel.commit`; a stateful failure has no partial-state success channel. Replaying
a prefix is the supported historical reconstruction mechanism. Every replay starts from fresh generative lineage, so
equal stable definitions and revisions do not imply interchangeable handles across runs.

Scenario records are complete hypothetical assumptions, not venue fills, trades, or live lifecycle events. They omit
actual execution IDs, venue/account data, fee-policy selection, assessed fees, PnL, risk decisions, catalog revision,
lineage, and snapshots. Round-trip records retain only entry and exit; held position and exact-flat proof are rebuilt by
the scenario domain.

## Limits and durable representation

`DecodeLimits.default` bounds payload characters (1,000,000), UTF-8 bytes (4,000,000), nesting depth (32), batch
records (10,000), object members (128), generic array entries (10,000), strings/identifiers (4,096), integer digits
(4,096), dimension factors (256), catalog commands (10,000), scenario slices (10,000), and market conversions (1,024).
Trusted offline replay may supply a larger validated profile without changing V1 schemas or exact semantics.

Codec-owned records and decoded dependent packages reject Java object serialization. Lower-layer handles, instruments,
orders, and scenarios retain the policy of their owning artifacts and are not advertised as durable graphs. The only
supported durable boundary form here is explicit versioned JSON text, or its UTF-8 bytes.

This artifact provides no database/filesystem/network operation, repository, transaction, stream, checkpoint,
signature, checksum, encryption, access control, or atomic publication-plus-journal guarantee. Applications and runtime
interpreters own framing, transport, storage, recovery, and one-time live snapshot capture.

Build the focused boundary with:

```text
sbt boundaryCodecs/test adversarialBoundary/test
```

Regenerate the checked-in compatibility resources deliberately with:

```text
sbt 'boundaryCodecs/Test/runMain trading.codec.generateBoundaryCodecCompatibilityResources <repository-root>'
```
