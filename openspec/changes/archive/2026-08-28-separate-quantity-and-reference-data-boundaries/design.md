## Context

See `proposal.md` for motivation and the six delta specifications for normative behavior. This proposal is the first
dependent change under `establish-architecture-and-functional-design-charter`; it must remain coherent with that
charter and with the later catalog, instrument-assembly, economics, and codec proposals before any implementation
begins.

Today `trading-quantities` owns four different kinds of concept:

- mathematical identity and arithmetic: `AtomId`, `DimKey`, `DimRef`, `Quantity`, `Rate`, `GridQuantity`, and grid
  projection;
- stable business/reference identity: `AssetId`, `GridId`, `GridVersion`, and `GridKey`;
- trusted catalog capabilities and coordination: `AssetRef`, `RegisteredDimensionRef`, `RegisteredGridRef`, and the
  synchronized `QuantityRegistry`;
- boundary reconstruction: constrained grid-key encoding, logical packed records, and registry-backed decoding.

The fusion is visible in `GridRef`: every mathematical grid must carry a stable ID and version even though exact
embedding and quantization need only a dimension, quantum, and coordinate namespace. It is also visible downstream:
instrument definitions and fee denominations import registry-owned types from the quantity artifact, and economics
performs issuer-provenance checks in operations that should eventually consume already assembled values.

This design preserves the settled quantity invariants: exact rational arithmetic, contextual grids, separate exact and
grid carriers, authoritative construction roots, minimal proof authority, static/runtime dimension coherence, and a
domain-neutral quantity foundation. APIs are unreleased, so the migration follows `DEC-009` and introduces no
compatibility aliases.

## Goals / Non-Goals

**Goals:**

- Give quantity mathematics and stable reference identity a one-way physical dependency boundary.
- Make anonymous mathematical grids useful without inventing stable catalog identities for them.
- Preserve the generative grid type and every existing exact arithmetic/quantization law.
- Define neutral immutable handles that later catalog snapshots can issue and instrument assembly can consume.
- Separate mathematical compatibility evidence from stable identity and catalog-lineage evidence.
- Keep the repository buildable through the ordered proposal portfolio without treating transitional registry code as
  the final live architecture.
- Remove quantity-owned packed APIs that would otherwise force a reverse dependency on stable reference data.

**Non-Goals:**

- Do not define immutable catalog state transitions, revisioning, snapshots, atomic batches, or the live catalog port;
  the next catalog proposal owns those decisions and incorporates the accepted append-only, immutable-by-key policy.
- Do not define activation, delisting, effective dating, correction workflow, or market/reference-data distribution.
- Do not introduce a wire/database schema, schema version, decoder dispatch, or replay policy; the boundary-codec
  proposal owns them.
- Do not introduce `InstrumentSpec`, move catalog resolution out of instrument construction, or remove all provenance
  checks from valuation; the assembly and pure-economics proposals own those boundaries.
- Do not split orders, execution scenarios, fee policy, or risk in this change.
- Do not add an effect type, tagless-final catalog algebra, Cats Effect, FS2, STM, actors, or a concurrency abstraction.
- Do not generalize every mathematical grid over an arbitrary stable identity parameter.

## Decisions

### 1. Create a real reference-data artifact now

Add an SBT project with:

```text
SBT ID:       referenceData
directory:    reference-data
artifact:     trading-reference-data
package root: trading.reference
depends on:   trading-quantities
```

`economics` will depend explicitly on both `quantities` and `referenceData`, because it directly consumes public types
from both. The root aggregates the new project, and the adversarial-boundary project receives completed immutable JARs
for all public artifacts it probes. The reference-data artifact gets its own external-artifact task so compiler tests do
not accidentally validate source-directory or same-build privilege.

This first production build migration also replaces the current generic `typelevelVersion` variable with independent
`catsVersion` and `algebraVersion` coordinates. Cats Core, Cats Kernel, and Cats Laws use the former; Algebra and
Algebra Laws use the latter. Their current strings happen to match, but they are independent release trains and must be
upgradeable and reviewable independently. This is a coordinate-only cleanup: neither dependency is upgraded here.

This is not speculative subdivision. The new artifact immediately owns stable identities, definitions, trusted handles,
provenance errors/evidence, and the temporarily relocated construction mechanism. Its dependency edge enforces a rule
that packages alone cannot: mathematical quantity code cannot import assets or catalog keys.

Alternative considered: create packages in `trading-quantities` and postpone the SBT split. Rejected because the
critical property is absence from the quantity artifact and classpath, not merely a different namespace.

Alternative considered: create separate asset-identity, grid-identity, and catalog artifacts immediately. Rejected
because those concepts form one coherent reference-data surface today and separate publication boundaries would be
speculative.

### 2. Keep only mathematical identifiers in quantities

`AtomId` and `DimKey` remain in `trading.quantity`. They describe the runtime form of the free abelian dimension group
and are required by `DimRef`, static/runtime coherence, and rate endpoints.

The following move to `trading.reference`:

```text
AssetId
GridId
GridVersion
GridKey                 // GridId × GridVersion, local to a dimension
GridIdentity            // DimKey × GridKey, globally qualified within a lineage
AssetDefinition
GridDefinition
```

`GridIdentity` gives the already settled full identity a named product rather than repeatedly transporting a tuple.
`GridDefinition` binds that full identity to one positive rational quantum. It is a non-product validated value: Scala
construction from `PositiveRational` rechecks the erased representation, while raw/JVM input uses a typed checked
factory. Its constructor and generated product operations cannot attach zero or negative quanta. This proposal
specifies immutable values; the next proposal specifies append-only registration and conflict behavior.

Alternative considered: leave IDs in quantities because they are small value classes. Rejected because dependency
ownership follows meaning, not implementation size. A quantity calculation has no use for an asset master ID or a grid
catalog version.

### 3. Make `GridRef` anonymous while retaining generative identity

The mathematical shape becomes conceptually:

```scala
sealed trait GridRef[D <: Dim]:
  type G
  def dimension: DimRef[D]
  def quantum: PositiveRational

object UniformGrid:
  def create[D <: Dim](
    dimension: DimRef[D],
    quantum: PositiveRational
  ): GridRef[D]
```

Because the refinement erases to `Rational` on the JVM, the factory also exposes a typed checked raw/JVM entry and
defensively revalidates the erased refined entry before returning a grid. Zero or negative quanta never return a
`GridRef`, while ordinary positive Scala construction retains the concise `create` call.

`G` remains essential. It is the generative coordinate namespace that prevents coordinates on different grids from
mixing accidentally. Removing stable `GridId` and `GridVersion` does not make grids structural by quantum.

Each created grid has a private anonymous identity token. The implementation may use the stable grid object itself as
that token or a separate private token; reference equality is not a public semantic promise. `SameGrid` succeeds only
when two retained references denote the same anonymous identity and their authoritative dimension/quantum definition is
coherent. Separately created equal-quantum grids are related by `SameQuantum`, not `SameGrid`.

`NotOnGrid` retains the exact source and target quantum but loses `GridKey`. `ConstrainedGridEncoding` is removed from
the quantity artifact because emitting a stable key requires a trusted identity-bearing handle.

Alternative considered: parameterize `GridRef[D, I]` by an arbitrary stable identity type. Rejected because it adds a
catalog concern and another public type parameter to all grid arithmetic, permits callers to choose meaningless
identity types, and still does not establish trusted catalog issuance. Composition through a wrapper is narrower.

Alternative considered: identify mathematical grids structurally by dimension and quantum. Rejected because distinct
contexts can deliberately use equal quanta while retaining different coordinate namespaces and stable meanings.

### 4. Compose stable identity around the mathematical grid

The public capability shape is conceptual rather than a commitment to exact field layout:

```scala
final class DimensionHandle[D <: Dim]:
  def key: DimKey
  def ref: DimRef[D]

final class Asset:
  type D <: Dim
  def id: AssetId
  def dimension: DimensionHandle[D]

final class GridHandle[D <: Dim]:
  type G
  def identity: GridIdentity
  def dimension: DimensionHandle[D]
  def grid: GridRef.Grid[D, G]
```

`GridHandle` delegates `fromCoordinate`, `coordinate`, and `asQuantity` to `grid`, so downstream ergonomics remain
direct while the quantity API stays anonymous. It does not duplicate quantum or coordinate semantics; any convenience
observer projects from the underlying grid.

All three handles carry an implementation-private immutable lineage token. Public code cannot select the token, attach
a stable key to an arbitrary grid, or manufacture a trusted handle by declaring the package. The completed JVM artifact
enforces this boundary with final public value classes, Scala-private constructors, and a registry-private issuance
permit checked before lineage or domain fields are retained. A JVM caller can name the public value and may see the
Scala-private constructor in bytecode, but cannot subclass it or make that constructor return a value without the
private permit. Scala/TASTy privacy alone is not treated as JVM access control. The token and permit do not become public
type parameters. The next proposal ensures every immutable catalog state and snapshot in one live catalog lineage
retains the same token and canonical handles.

The public names are `Asset`, `DimensionHandle`, and `GridHandle`. A `Resolved` prefix is reserved for a result where
resolution itself is the relevant state distinction, such as a future codec result paired with its source record. The
trusted handle need not have an `UnresolvedAsset` twin: `AssetId` and `AssetDefinition` already name boundary forms.

Alternative considered: retain `AssetRef` and `RegisteredGridRef` in the new artifact. Rejected because those names
expose the current mutable registry mechanism rather than the enduring domain meaning.

Alternative considered: expose the lineage token as a phantom owner parameter everywhere. Rejected because it would
make ordinary instrument and economics signatures substantially harder to use without eliminating the need for runtime
checks at existential boundaries.

### 5. Separate three different grid relationships

The design makes the following distinctions explicit:

| Relationship | Owning layer | Checks | Authority granted |
| --- | --- | --- | --- |
| Same anonymous grid | quantities | private generative identity plus coherent definition | coordinate retyping for one mathematical grid |
| Same quantum / embedding | quantities | canonical dimension and exact quantum relation | documented numerical coordinate conversion |
| Same stable grid handle | reference data | lineage, full stable identity, dimension, and immutable definition | use of independently retained canonical handles as one stable grid |

Reference-data evidence may return or internally use the ordinary `SameDimension` and `SameGrid` capabilities after its
additional checks. It must not create parallel unrestricted casts. Failure is a closed reference-data ADT that
distinguishes foreign lineage, stable identity mismatch, dimension mismatch, and definition conflict.

Stable handle checks are pure comparisons of immutable values. They do not consult a registry, snapshot, or live
service. `SameQuantum` never implies stable identity, and equal visible stable fields from different lineages never imply
shared authority.

Alternative considered: use only stable ID equality after resolution. Rejected because separately constructed catalogs
or processes can contain equal-looking IDs without sharing the canonical path-dependent witnesses that make retyping
safe.

### 6. Keep the current registry only as an unreleased portfolio bridge

The handle constructors need an issuer until the catalog proposal is implemented. For this change, the current
synchronized registry implementation and its definition/conflict errors move mechanically into the reference-data
artifact and issue the new handle vocabulary. Its registration and lookup behavior is not redesigned or promoted to a
new long-lived interface here.

This is an explicit portfolio transition exception:

- the mutable registry is absent from `trading-quantities`;
- no economics calculation receives it;
- no new tagless-final or snapshot API is inferred from it;
- the following catalog proposal replaces its state/coordination model;
- Proposal 1 must not be released as the final architecture while that replacement remains unapplied.

Where practical, implementation and review of Proposals 1 and 2 should occur consecutively in the same unreleased
migration window. If they are committed separately for review clarity, the intermediate commit is buildable but not a
supported architectural endpoint.

Alternative considered: design the final catalog in this proposal. Rejected because catalog state transitions,
revision semantics, snapshot coherence, batching, and live publication form a substantial independent capability and
already have explicit decisions to encode in Proposal 2.

Alternative considered: make handle constructors public temporarily. Rejected because even a short-lived construction
hole would invalidate the provenance boundary and could leak into downstream code.

### 7. Remove quantity-owned packing instead of relocating it incorrectly

`GridCoordinateEncoding`, `PackedAssetGridQuantity`, `PackedGridQuantity`, their decoders, and dependent decoded grid
packages are removed from `trading-quantities`. They are not moved into reference data as a permanent convenience: a
stable record is a codec concern, and its decoder must later consume an explicit immutable catalog snapshot.

The boundary-codec proposal will decide:

- wire/database schema versions independent of `GridVersion`;
- exact field types and validation order;
- batch reconstruction against one snapshot;
- dependent decoded result names and ownership;
- replay and historical-version behavior.

Removing the current logical records creates a deliberate temporary feature gap if Proposal 1 is applied before the
codec proposal. That gap is preferable to preserving an explicitly non-production format as architecture. No durable
payload exists and no compatibility migration is required.

Alternative considered: mechanically move packed records into reference data. Rejected because it would make the new
module immediately own a concern the charter assigns to boundary codecs and encourage catalog APIs to grow around an
unstable record shape.

### 8. Limit the economics edit to dependency migration

Current economics types change mechanically and semantically from registry vocabulary to stable handle vocabulary:

```text
AssetRef                  -> Asset
RegisteredDimensionRef    -> DimensionHandle
RegisteredGridRef         -> GridHandle
foreign registry          -> foreign reference-data lineage
```

Instrument definition validation, market conversion construction, and fee denomination construction continue to check
the same relationships at this stage, using pure handle evidence instead of `sharesRegistryWith`. The economics module
does not receive the mutable construction mechanism, and arithmetic continues to retain handles already stored in the
instrument or market state.

This proposal does not claim that those checks are in their final layer. Proposal 3 introduces an explicit instrument
assembly boundary, and Proposal 4 makes ordinary valuation and P&L consume only the resulting trusted specification.
Keeping this edit narrow prevents Proposal 1 from silently absorbing those design decisions.

Alternative considered: remove every provenance check from economics in this change. Rejected because doing so without
the assembly boundary would either weaken validation or duplicate the later proposal.

### 9. Use source-breaking migration and real artifact-boundary tests

All source imports and type names move directly. There are no forwarding aliases in `trading.quantity`, deprecated
registry witnesses, dual `UniformGrid.create` overloads, or compatibility packed types.

Verification must cover both laws and architecture:

- existing exact, grid, refinement, and algebra laws still pass against anonymous grids;
- separately generated equal-quantum grids are not `SameGrid` but may be `SameQuantum`;
- a plain grid cannot satisfy `GridHandle` and a foreign-lineage handle cannot recover stable evidence;
- the packaged quantities JAR exposes no asset, stable-grid, registry, or packing types;
- reference data depends on the packaged quantity surface and cannot forge quantity internals;
- economics consumes packaged quantity and reference-data artifacts;
- same-package adversarial source cannot construct handle implementations or lineage tokens;
- downstream Java cannot invoke stable-identity or concrete handle implementation constructors, and registry-issued
  Java clients remain supported;
- clean compilation and the full test suite pass.

Alternative considered: test only through the combined SBT source graph. Rejected because same-build visibility and
classpath mistakes can hide a reverse dependency or accidentally exposed constructor.

## Risks / Trade-offs

- [Path-dependent `Asset` and `GridHandle` members can make Scala inference awkward] → Keep the dependent indices local,
  provide `GridHandle.Grid[D, G]`-style refinements where a method must name them, and add real downstream compiler
  fixtures before settling signatures.
- [Anonymous `SameGrid` could accidentally become structural equality] → Give each mathematical grid private
  generative identity and test that separate equal-definition grids fail same-grid recovery.
- [A lineage check could accidentally depend on mutable registry object identity] → Put lineage in a dedicated immutable
  private token carried by handles; Proposal 2 preserves it across state revisions and snapshots.
- [The temporary synchronized registry could be mistaken for the target catalog] → Mark it as an unreleased portfolio
  bridge, add no new contract around it, and require the catalog proposal before release/final architectural approval.
- [Removing logical packing temporarily reduces functionality] → Record the gap explicitly, preserve no unstable format,
  and make the boundary-codec proposal responsible for restoring the capability with a real schema and snapshot input.
- [Moving types across artifacts can weaken adversarial tests if classpaths are wrong] → package each public artifact and
  compile downstream fixtures only against those completed immutable JARs.
- [Broad import churn could conceal semantic changes in economics] → Limit this proposal to handle substitution and
  existing validation semantics; review instrument assembly and pure valuation separately.

## Migration Plan

1. Split the Cats and Algebra version coordinates without upgrading them, then add the `referenceData` SBT project,
   root aggregation, external-artifact task, and explicit economics/adversarial dependencies without moving behavior.
2. Move stable identifiers and definitions, introduce `GridIdentity`, and establish reference-data constructor and
   serialization boundaries.
3. Remove stable fields from `GridRef`, make `UniformGrid.create` anonymous, and update mathematical grid evidence,
   projection errors, laws, and compiler fixtures.
4. Add `Asset`, `DimensionHandle`, `GridHandle`, and checked pure handle evidence; mechanically adapt the current registry
   to issue them from the reference-data artifact.
5. Migrate economics definitions, market conversions, fee denominations, fixtures, and error vocabulary to immutable
   handles without changing their economic formulas or validation ordering.
6. Remove quantity-owned constrained encoding, packed records, registry decode paths, and their obsolete tests; retain
   boundary-codec requirements in the later proposal rather than adding a temporary replacement.
7. Update canonical spec purposes and repository documentation so they describe runtime dimension identity separately
   from reference data and codecs.
8. Run formatting, module-focused law/property tests, immutable-JAR compiler/adversarial tests, `sbt -batch clean test`,
   strict OpenSpec validation, and staged-diff checks.
9. Obtain fresh independent review under the steward workflow. Continue directly into the catalog proposal before any
   release or claim that the reference-data runtime architecture is complete.

Rollback is a source-level revert of the whole unreleased change. Because no compatibility aliases, schema, persisted
payload, or external release is introduced, rollback requires no data migration.
