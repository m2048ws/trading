## Why

The quantity artifact currently combines mathematical grids and runtime dimensions with asset master data, stable grid
identity, mutable registration, and packed boundary records. Separating those responsibilities now gives the pure
quantity kernel a durable dependency boundary and gives the later catalog, instrument-assembly, and codec proposals a
clean foundation.

## What Changes

- Add a `trading-reference-data` artifact that depends on `trading-quantities`; the quantity artifact remains independent
  of assets, stable catalog keys, registration, persistence, and instrument concepts.
- Split the current coincidentally shared Cats/Algebra build version into independent `catsVersion` and
  `algebraVersion` coordinates without changing either selected release as part of this migration.
- **BREAKING** Move `AssetId`, asset definitions, stable `GridId`/`GridVersion`/`GridKey` identity, and identity-bearing
  asset, dimension, and grid capabilities out of `trading.quantity` into reference data. Prefer the trusted domain names
  `Asset`, `DimensionHandle`, and `GridHandle` rather than a blanket `Resolved*` prefix.
- **BREAKING** Make `GridRef[D]` an anonymous mathematical grid witness containing only its authoritative dimension,
  positive exact quantum, and generative coordinate namespace. `UniformGrid.create` no longer accepts or exposes a
  stable grid ID or version.
- Introduce `GridHandle[D]` as the composition of stable dimension-scoped grid identity, opaque catalog provenance, and
  one underlying mathematical `GridRef[D]`; it delegates coordinate construction and exact interpretation without
  making stable identity part of quantity arithmetic.
- Keep `SameQuantum`, exact `Embedding`, projection, quantization, and anonymous-grid relationships in quantities. Move
  stable-grid reconciliation, conflicting immutable-definition errors, and issuer-provenance checks to reference data.
- **BREAKING** Remove stable `GridKey` context from pure projection errors and remove stable-key coordinate encoding and
  packed registry records from the quantity artifact. The later boundary-codec proposal owns their replacement and wire
  schema; this proposal does not bless their current location as a new permanent API.
- Relocate the current synchronized registry only as a temporary reference-data construction mechanism so the repository
  remains buildable while the next proposal replaces it with pure catalog transitions, immutable snapshots, and an
  effectful live capability. No new final live-registry contract is established here.
- **BREAKING** Update the current economics surface to consume immutable reference-data handles instead of
  quantity-registry-owned types while retaining its existing validation behavior. Instrument assembly and removal of
  provenance checks from P&L remain separate later proposals.
- Remove obsolete imports, aliases, and compatibility paths rather than preserving the unreleased package layout.

## Capabilities

### New Capabilities

- `reference-data-identity`: Stable asset and grid identity, immutable trusted handles, opaque issuer provenance, and
  the one-way dependency from reference data to anonymous quantity mathematics.

### Modified Capabilities

- `exact-quantity-arithmetic`: Keep carrier construction authority and serialization obligations mathematical and
  remove stable asset, grid-key, registry, and packed-boundary ownership from the quantity artifact.
- `quantity-grid-projection`: Redefine `GridRef` and grid evidence around anonymous mathematical grids, make projection
  errors identity-neutral, and remove stable-key encoding from the quantity layer.
- `runtime-quantity-identity`: Retain generic `DimKey`/`DimRef` authority and runtime endpoint behavior while moving
  assets, registered-grid provenance, registry reconciliation, heterogeneous registered values, and logical packing to
  their owning layers.
- `instrument-economics`: Replace quantity-registry vocabulary with immutable reference-data handles without yet
  changing instrument-assembly or valuation responsibility.
- `fee-inclusive-pnl`: Express fee denominations with reference-data asset and grid handles while preserving exact
  quantization and fee behavior.

## Impact

- Affected build structure: `build.sbt`, the root aggregate, external-artifact test wiring, and a new
  `reference-data/` project whose only production dependency is `quantities`.
- Build dependency hygiene: Cats Core/Kernel/Laws and Algebra/Algebra Laws follow their own named release coordinates;
  this proposal performs no incidental dependency upgrade.
- Affected quantity code: identifiers, anonymous grid construction and evidence, projection errors, runtime identity,
  registry-owned runtime packages, constrained encoding, and packed records.
- Affected downstream code: economics definitions, market conversions, fees, tests, examples, and adversarial compiler
  fixtures must import and retain reference-data handles instead of `AssetRef` and `RegisteredGridRef`.
- API impact is intentionally source-breaking: stable identity types change artifact/package, mathematical grid factory
  signatures shrink, registered witness names are replaced, and quantity-owned packing APIs disappear.
- Follow-on dependencies: the pure/live catalog proposal defines final creation and publication semantics; instrument
  assembly consumes these handles; boundary codecs define versioned persistence and replay. None is implemented by this
  change.
