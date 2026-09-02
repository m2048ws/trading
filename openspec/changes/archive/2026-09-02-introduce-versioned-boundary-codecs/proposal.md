## Why

The merged quantity/reference-data boundary intentionally removes quantity-owned packed records because they are
unversioned logical values tied to a mutable registry, while the later domain boundaries correctly refuse to treat
in-memory proof-carrying values as wire
formats. The portfolio now has enough stable IDs, immutable snapshots, assembly boundaries, and algebraic order/scenario
models to define one real durable representation and checked reconstruction path.

## What Changes

- Add a pure `trading-boundary-codecs` artifact above the domain modules; lower mathematical/domain artifacts remain
  independent of codecs and no codec operation returns `F` or performs I/O.
- Define independently versioned canonical JSON envelopes whose schema version is distinct from reference-data
  `GridVersion`, with checked dispatch and frozen version-specific record shapes.
- Specify canonical exact encodings for arbitrary integers, rationals, dimensions, stable asset/grid identities, and
  closed alternatives without floating point, platform number limits, kind-plus-null records, or Java serialization.
- Restore general and asset-qualified grid-coordinate packing as boundary-owned V1 records. Decoding resolves dimension
  before grid through one explicit `CatalogSnapshot` and returns a dependent decoded value retaining the canonical
  handles and mathematical grid quantity.
- Add V1 records for catalog journal entries, `InstrumentDefinition`, immutable orders, complete hypothetical order
  scenarios, and checked round trips. Records contain stable IDs and primitive/exact data, never trusted handles,
  `InstrumentSpec`, `Instrument`, path-dependent evidence, catalog lineage, effects, clients, or object graphs.
- Reconstruct instrument definitions through `InstrumentAssembler`, orders through their smart constructors, associated
  activation/pricing evidence through the owning instruction alternatives, scenarios through `OrderScenario`, and round
  trips through their checked constructor rather than duplicating domain validation in codecs.
- Decode a batch against one caller-supplied snapshot, accumulate independent record/field violations with deterministic
  indexed paths, and sequence only stages whose prerequisites succeeded.
- Use Jackson Core 3.x only as a private strict streaming-syntax adapter. Do not introduce Jackson Databind, the
  Jackson Scala module, Circe, generic object mapping, or parser-library types in the public codec model.
- Define ordered catalog-journal replay into a caller-supplied fresh catalog root/state. Each entry must publish the next
  recorded revision; gaps, conflicts, malformed batches, and idempotent no-op journal entries fail explicitly.
- Add canonical golden fixtures, independently validated JSON schemas/canonicalization output, round-trip/model
  properties, malformed/adversarial payload coverage, and configurable decode limits for boundary resource protection.
- Do not add a database repository, filesystem/network access, stream, checkpoint serializer, live-catalog lookup,
  market-data source, trade/execution-event schema, fee-policy schema, or runtime interpreter in this proposal.

## Capabilities

### New Capabilities

- `versioned-boundary-codecs`: Stable JSON envelopes, exact primitive schemas, supported record families, staged pure
  parsing/reconstruction, snapshot-coherent batch decode, diagnostics, and compatibility rules.
- `catalog-command-replay`: Versioned published-batch journal entries and deterministic pure rebuild of an append-only
  catalog under a fresh in-memory lineage.

### Modified Capabilities

None.

## Impact

- New SBT project/directory: `boundaryCodecs` in `boundary-codecs/`, artifact `trading-boundary-codecs`, package root
  `trading.codec`.
- Direct production dependencies: quantities, reference data, instrument economics, order model, execution scenario,
  Cats Core for internal validation/traversal, Jackson Core 3.x for strict streaming parsing, and a project-owned
  canonical renderer; no Jackson Databind/Scala module, Circe, fee-policy, risk, application, runtime, or effect-system
  dependency.
- Test-only dependencies include a Jackson-3-compatible NetworkNT JSON Schema Validator 3.x for offline Draft 2020-12
  checks and an RFC-listed Java JCS implementation as an independent canonicalization oracle. Neither defines the wire
  model, renderer, or production API.
- The shared non-published JMH project gains codec-only benchmark dependencies and focused parsing/rendering/batch-
  reconstruction measurements; the codec production artifact does not depend on benchmarks.
- Root aggregation, completed-JAR external compilation, adversarial fixtures, schema resources, golden fixtures, and the
  repository validation matrix gain the codec artifact.
- The removed `PackedAssetGridQuantity`, `PackedGridQuantity`, registry decoders, and `Resolved*` runtime packages do not
  return as aliases; boundary records and genuinely decoded dependent packages receive codec-owned names.
- Historical data names exact stable grid versions and can be reconstructed against any explicitly selected snapshot
  containing those append-only identities; availability/delisting policy remains separate.
- Future application/runtime ingress can capture one snapshot and call these pure batch decoders, while future durable
  trade and live execution protocols require their own domain/event proposals before receiving codecs.
