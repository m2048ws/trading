# Task Group 1 — Contract, dependency, and baseline gates

## Authority and prerequisite reconciliation

- Delivery: `RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs`
- Change: `introduce-versioned-boundary-codecs`
- Issue: `#10`
- Run: `run-2b01f872-e634-4491-acd3-b1454c94b59e`
- Planning revision: `sha256:e5cbb7f10509095ee6311d1369cef5fd883b8a196713a4bf817a3d2289b26da6`
- Source digest: `sha256:625809aa552c0c3f4efd1a8e2d9982c76414f82ce405674ce6f91b475807cceb`
- Traceability digest: `sha256:c7545d03bf6039543fee8aab7947aa9c10c46923f8e0ee9cc716a12ab66ac44f`
- Planning baseline commit: `f356ba4bd02cf16bb4f1d1b1e03d9f3395f1286c`
- Integrated main parent: `4e0034e7ca16b0f4cc67b045beadf6a3a9b0613c`

The accepted RFC source binds exactly AC-017 through AC-020. `S-02-order-execution-scenarios` is archived in the
portfolio delivery sidecar at commit `7ca2e135632a275562d46a0e6c5b08b3a3771639`, and the subsequently delivered
S-03 and S-04 modules are also present on the integration parent. The repository requires JDK 25 or later through
`.java-version`, the build settings, and CI; the baseline ran on OpenJDK 26.0.2.

The planning package was reconciled against the delivered reference-data, instrument-economics, order-model, and
execution-scenario boundaries before production work. Strict Corgi readiness passes at the planning revision above,
and Run Contract v3 selects Group 1 as the sole current Task Group. The Group 1 commit records the planning baseline as
its first parent and exact configured `origin/main` as its second parent. The previously resolved merge tree was
preserved as recovery commit `2cf25761f36c6a01a026855aacea7b9f3adad995`; after refreshing the second parent, the
tree differs from that preservation point only by the nine Corgi process-fix paths introduced by `4e0034e`.

## Behavioral baseline

The reconciled production tree ran with SBT 1.12.15 and Scala 3.8.4 on OpenJDK 26.0.2.

| Check | Result | Evidence |
| --- | --- | --- |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck` | pass | Production, test, adversarial, benchmark, and SBT sources are formatted. |
| `sbt -batch clean test` | pass | 913 tests passed and none failed after clean dependency-order compilation. |
| Quantity/reference/application/runtime | pass | 601 quantity, 13 reference-data, 9 application, and 18 runtime tests passed. |
| Pure instrument/risk/order/scenario/fee policy | pass | 13 instrument-economics, 40 risk, 7 order-model, 16 execution-scenario, and 38 fee-policy tests passed. |
| Completed-JAR and adversarial boundaries | pass | 158 compiler, Java, package-spoof, provenance, serialization, effect, and runtime-boundary tests passed. |
| `sbt -batch "benchmarks/Jmh/compile"` | pass | The benchmark sources and generated JMH harness compiled. |
| `corgispec ready introduce-versioned-boundary-codecs --strict --json` | pass | All deterministic planning/source/traceability checks passed. |

The only build warning was Scala 3.8.4's upstream use of the terminally deprecated `sun.misc.Unsafe` API on JDK 26; it
did not affect compilation or tests.

## Stable identity and catalog inventory

| Concept | Delivered API and invariant | Codec implication |
| --- | --- | --- |
| Mathematical identity | `AtomId` and canonical `DimKey` own atomic and compound dimension identity; `Rational`, `Quantity`, and `GridQuantity` retain exact arithmetic. | Encode the exact identifiers, normalized dimension factors, integers, and reduced rationals; do not encode type witnesses or floating-point approximations. |
| Reference identity | `AssetId`, `GridId`, positive `GridVersion`, `GridKey`, and full `GridIdentity(DimKey, GridKey)` are stable ordinary values. | Grid records name the complete stable identity; reconstruction never infers a version or scans for a compatible grid. |
| Catalog intent | `CatalogCommand` is the closed sum `RegisterAsset(AssetDefinition)`, `RegisterDimension(DimKey)`, or `RegisterGrid(GridDefinition)`; `CatalogBatch` is ordered and non-empty. | Journal V1 mirrors this exact sum and order. It does not add an open command registry or accept empty batches. |
| Catalog result | `CatalogModel.commit` returns either ordered non-empty `CatalogViolations` or a `CatalogTransition(state, outcome)`. `CatalogCommit` is exactly `Unchanged(snapshot)` or `Published(snapshot, nonEmptyDelta)`. | A journal entry can be built only from `Published`; unchanged attempts and failures cannot become revisions. |
| Catalog authority | A fresh `CatalogRoot` owns immutable `CatalogState`; `CatalogRevision` is nonnegative and publication increments it. `CatalogSnapshot` is an immutable direct-lookup view with asset, dimension, and full-grid indexes. | Replay starts from a caller-supplied fresh revision-zero empty state. Records never contain root/lineage tokens, states, snapshots, or handles. |
| Snapshot lookup | `resolveAsset(AssetId)`, `resolveDimension(DimKey)`, `resolveGrid(GridIdentity)`, and the lineage-checked dependent grid lookup are the only reconstruction paths. | Each batch receives exactly one snapshot and returns all values or stable indexed errors; there is no live-catalog read or fallback scan. |

## Instrument and market inventory

`InstrumentId`, `UnderlyingId`, and `InstrumentIdentity` are the final stable instrument identifiers.
`InstrumentDefinition` is the stable-ID-only product of `AssetRoleIds` (base, quote, position, settle),
`ListingDefinition` (full position-lot and price-grid identities), and exact `PayoffDefinition` coefficients.
`InstrumentAssembler.assemble(definition, snapshot)` is the sole pure resolution boundary: it accumulates contextual
asset/grid/structural violations and produces the proof-carrying `InstrumentSpec`; `Instrument.fromSpec` then produces
the trusted `Instrument`. Codecs must compose those operations and must not encode or construct `InstrumentSpec`,
handles, lineage, or an `Instrument` object graph.

`MarketState` retains the instrument ID, settlement asset, checked price, base-to-settle and quote-to-settle endpoint
rates, and an ordered vector of additional `SettlementConversion`s. Its stable observable input surface is price ticks,
the two exact rational coefficients, and `conversionSources` paired with each exact source-to-settle coefficient.
Reconstruction must resolve every additional source asset through the supplied snapshot and invoke the canonical
market constructors; a generic asset/rational map is not an alternative API.

## Order and scenario alternative inventory

The immutable order algebra is closed and branch-sensitive:

- identity and intent: root `InstrumentId`, `Side` (`Buy`/`Sell`), exact positive lots, and `PositionEffect`
  (`Unrestricted`/`ReduceOnly`);
- activation: `ImmediateActivation`, `FixedActivation(reference, comparison, triggerPrice)`, or
  `TrailingActivation(reference, comparison, positiveOffsetTicks)`, where references are `Last`, `Mark`, or `Index`
  and comparisons are `AtOrAbove` or `AtOrBelow`;
- execution: `MarketExecution` with non-resting `ImmediateOrCancel` or `FillOrKill`, or `PricedExecution` with
  `LimitPricing` or `PeggedPricing`, full `TimeInForce`, `LiquidityConstraint`, and `Displayed`, `Hidden`, or
  `Iceberg(displayedLots)` visibility; and
- associated proof: immediate/fixed/trailing activation evidence and direct/peg pricing resolution are created only
  through the selected instruction alternative, then `Order.create` performs canonical accumulating validation.

The scenario boundary is likewise final: `LiquiditySlice` retains typed lots, one `MarketState`, and `Maker`/`Taker`;
`MatchedSlices` is non-empty; `ScenarioAssumptions` binds one exact order to its associated activation evidence,
pricing resolution, and slices; `OrderScenario.evaluate` produces the checked activation, effective pricing, and
derived position change. `RoundTripScenario.create` accepts only entry and exit scenarios for one instrument and derives
the held position. Codec records therefore store assumptions and observations, not cached domain results, fee/PnL
derivations, venue fills, or execution lifecycle data.

## Retired vocabulary and drift decision

The former quantity-runtime persistence surface is absent from production and must not return as an alias. The removed
names are `GridCoordinateEncoding`, `ConstrainedGridEncoding`, `PackedAssetGridQuantity`, `PackedGridQuantity`,
`ResolvedAssetGridQuantity`, `ResolvedGridQuantity`, `QuantityRegistry`, `RegisteredDimensionRef`, `RegisteredGridRef`,
`DimensionWitness`, `AssetRef`, `RegistryError` and its packed/registry-specific variants, `RuntimeEvidence`,
`ResolvedExactQuantity`, and `HeterogeneousQuantity`. The accepted design replaces only the real reconstruction phase
with codec-owned `DecodedGridQuantity` and `DecodedAssetGridQuantity`; ordinary trusted domain values keep their
existing names.

No portfolio drift requires an RFC or planning amendment. The delivered owners and alternatives match the proposed V1
families, so Group 2 may establish the one-way `trading-boundary-codecs` module without changing the frozen planning
artifacts.
