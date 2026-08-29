## Context

See `proposal.md` for motivation and the delta specifications for normative behavior. This proposal depends on the
accepted `Asset`/`GridHandle` boundary and immutable `CatalogSnapshot` model defined by Proposals 1 and 2.

The current raw `Definition` is not actually raw: it contains already issued `AssetRef` and `RegisteredGridRef` values,
path-dependent payoff rates, and three component objects that each retain or point at roles. `ValidatedDefinition`
checks registry lineage and dimension keys, then casts grids and payoff rates into the selected role paths. Final
`Instrument.create` combines resolution-adjacent failures with economic failures, while later market/fee code still
knows registry vocabulary.

The intended trust sequence is:

```text
external syntax / stable IDs
             |
             v
     InstrumentDefinition
             |
     CatalogSnapshot + validation
             v
       InstrumentSpec
             |
       total construction
             v
          Instrument
```

The design preserves exact rational arithmetic, typed rate endpoints, contextual grids, proof-carrying validation,
deterministic accumulated diagnostics, and the pre-release no-compatibility policy. It does not yet narrow the broad
instrument facade or move orders, scenarios, fee policy, valuation, or sizing; those are later proposals.

## Goals / Non-Goals

**Goals:**

- Give untrusted stable instrument identity one explicit entry point into the typed domain.
- Resolve every reference against one caller-selected immutable snapshot.
- Remove invalid component-role combinations from the raw data model.
- Preserve every successful identity, dimension, grid, and endpoint proof in `InstrumentSpec`.
- Make final instrument construction total and catalog-independent.
- Give catalog versus assembly versus economics errors clear ownership.
- Keep ordinary adapters ergonomic without exposing casts or lineage tokens.

**Non-Goals:**

- Do not parse strings, venue payloads, database rows, or a stable instrument wire schema.
- Do not select which catalog snapshot is current or call `LiveCatalog[F]`.
- Do not model instrument activation, venue availability, account eligibility, or listing lifecycle.
- Do not generalize the two-leg payoff into an arbitrary leg collection.
- Do not change lot, price, market-state, fee, PnL, order, scenario, or risk formulas.
- Do not create a separate instrument-assembly SBT artifact before an independent dependency/publication boundary is
  justified.
- Do not retain a catalog revision as part of economic identity or semantic equality.
- Do not add compatibility aliases for `Definition` or `ValidatedDefinition`.

## Decisions

### 1. Make the raw definition an ID-only cohesive product

The conceptual public shape is:

```scala
final case class AssetRoleIds(
  base: AssetId,
  quote: AssetId,
  position: AssetId,
  settle: AssetId
)

final case class ListingDefinition(
  positionLotGrid: GridIdentity,
  priceGrid: GridIdentity
)

final case class PayoffDefinition(
  basePerPosition: Rational,
  quotePerPosition: Rational
)

final case class InstrumentDefinition(
  identity: InstrumentIdentity,
  roles: AssetRoleIds,
  listing: ListingDefinition,
  payoff: PayoffDefinition
)
```

`InstrumentId` and `UnderlyingId` remain ordinary stable domain values. The underlying is intentionally not an `Asset`
role: an index or basket need not be a currency or settlement dimension.

Identity and roles occur once. Listing and payoff values cannot point at a second `Roles` object, so
`ListingRolesDiffer` and `PayoffRolesDiffer` disappear as unrepresentable states rather than moving into a new error
hierarchy. Payoff inputs are exact coefficients because their endpoints are not known until asset resolution; assembly
turns them into typed `Rate` values once those endpoints exist.

Alternative considered: keep already resolved handles in `InstrumentDefinition`. Rejected because it leaves catalog
membership and snapshot coherence outside the assembly boundary and makes persisted/config definitions process-local.

Alternative considered: put raw strings and venue product-family fields in the definition. Rejected because syntax and
venue normalization are adapter/codec concerns; the domain command starts with validated IDs and exact numbers.

Alternative considered: put `CatalogRevision` in every definition. Rejected because revisions are lineage-local
publication coordinates, not stable economic identity. An application audit envelope may record the selected revision.

### 2. Require the caller to supply exactly one snapshot

The boundary is an ordinary pure API, conceptually:

```scala
object InstrumentAssembler:
  def assemble(
    definition: InstrumentDefinition,
    snapshot: CatalogSnapshot
  ): Either[InstrumentAssemblyErrors, InstrumentSpec]

  def assembleFirst(
    definition: InstrumentDefinition,
    snapshot: CatalogSnapshot
  ): Either[InstrumentAssemblyViolation, InstrumentSpec]
```

The parameter makes the reference-data view visible and testable. Assembly never captures a newer snapshot or receives
`LiveCatalog[F]`; the application workflow chooses a coherence boundary before calling it. This permits batch
instrument loading to reuse one snapshot and ensures diagnostics are reproducible.

Alternative considered: let `InstrumentAssembler[F]` call the live catalog. Rejected because snapshot selection is an
application effect, while resolution and validation are pure. It would also make unit construction and P&L transitively
effectful.

Alternative considered: pass six independently resolved handles. Rejected because callers would have to reproduce
snapshot membership and common-lineage validation and could mix revisions accidentally.

### 3. Stage structural validation, lookup, and dependent proof recovery

Assembly uses one ordered rule set with these stages:

1. Validate raw structural facts available without lookup: base differs from quote and the two payoff coefficients are
   not both zero.
2. Resolve assets in role order: base, quote, position, settle.
3. Resolve grids in listing order: position-lot, price.
4. Accumulate position-grid and price-grid dimension checks when each branch's prerequisites exist.
5. Construct typed payoff rates only after their asset endpoints exist and all required assembly checks succeed.
6. Build the sealed `InstrumentSpec`.

Independent checks within and across eligible branches use applicative accumulation. Missing prerequisites suppress
only dependent checks. In particular:

- missing position suppresses position-grid comparison and both position-origin payoff rates;
- missing base or quote suppresses the price-dimension relationship;
- equal base/quote suppresses the dependent price relationship to avoid a misleading secondary unit-dimension error;
- a missing grid suppresses only that grid's dimension check;
- unrelated resolution failures and the empty-payoff violation still accumulate.

Stable ordering is stage, rule ordinal, then collection index where relevant. Both public entry points derive from the
same result; fail-fast is `errors.head`, not a second `Either` implementation.

Alternative considered: resolve and validate fail-fast in field order. Rejected because independent configuration
errors are valuable together and recent validation work already established the staged accumulating pattern.

Alternative considered: run every possible check after a missing role. Rejected because invented expected endpoints and
grid errors would be diagnostically false.

### 4. Contextualize catalog lookup errors at assembly

Reference data owns generic failures such as unknown `AssetId` or `GridIdentity`. Assembly wraps them with the semantic
field that requested the lookup:

```scala
enum InstrumentAssemblyViolation:
  case AssetResolution(
    instrumentId: InstrumentId,
    role: AssetRole,
    requested: AssetId,
    revision: CatalogRevision,
    cause: CatalogLookupError
  )
  case GridResolution(...)
  case EqualBaseAndQuote(...)
  case EmptyPayoff(...)
  case GridDimension(...)
```

`AssetRole` and `ListingGridRole` are closed semantic enums rather than free-form role strings. The public non-empty
aggregate uses head plus immutable tail (or another domain-owned non-empty type). It retains typed causes and exact
keys; formatting is a separate concern.

The same snapshot makes foreign-lineage errors impossible during ordinary ID lookup. If a typed overload later accepts
a handle for convenience, it must still prove snapshot membership and map failure into the same assembly vocabulary;
the ID-based definition remains canonical.

Alternative considered: return `CatalogLookupError` directly. Rejected because `UnknownGrid` alone cannot tell a caller
whether the missing key was the lot or price rule or which instrument definition needs repair.

Alternative considered: fold everything into `EconomicsError`. Rejected because economics cannot remediate unknown
catalog identities and should never see a partial instrument.

### 5. Make InstrumentSpec the only proof-carrying definition value

The conceptual dependent shape is:

```scala
sealed abstract class InstrumentSpec private:
  val identity: InstrumentIdentity
  val roles: InstrumentRoles

  val positionLotGrid:
    GridHandle[roles.position.D]

  val priceGrid:
    GridHandle[Divide[roles.quote.D, roles.base.D]]

  val basePerPosition:
    Rate[roles.position.D, roles.base.D]

  val quotePerPosition:
    Rate[roles.position.D, roles.quote.D]
```

`InstrumentRoles` contains the exact trusted `Asset` values resolved from the snapshot. The implementation may use a
private final dependent class and narrow existential lookup results after `SameDimension`/handle evidence succeeds. Any
casts remain in one lexical constructor and are justified immediately by checked evidence.

The specification may expose ordinary observations of its source IDs and exact components. It does not expose its
lineage token, snapshot, a catalog resolver, or reusable arbitrary retagging evidence. It does not need a public
`ValidatedDefinition` wrapper around another raw object; it is itself the trusted static instrument meaning.

Alternative considered: make `InstrumentSpec` a case class with public existential fields. Rejected because callers
could assemble unrelated handles and coefficients without the boundary and the endpoint relationships would be lost.

Alternative considered: parameterize the spec publicly by base/quote/position/settle dimension and catalog-owner types.
Rejected because path-dependent assets retain the useful indices locally and avoid infecting every downstream signature.

### 6. Construct Instrument totally and retain the spec

`Instrument.fromSpec(spec)` (or an equally direct constructor) performs no `Either` work. The instrument may retain the
spec as one field and project roles/listing/payoff, or copy those immutable references; it never re-resolves or recasts
them.

The source-breaking migration removes:

```text
Definition
ValidatedDefinition
Instrument.validate(Definition)
Instrument.fromValidated(...)
Instrument.create(Definition)
DefinitionViolation / InvalidDefinition
ForeignRegistry as a definition-construction error
```

The precise economics error cleanup beyond definition construction remains Proposal 4. Runtime market conversion and
fee denomination paths may still validate additional trusted asset relationships until their own inputs are redesigned.

Alternative considered: keep `Instrument.create(definition, snapshot)` as a convenience. Rejected because it makes the
final value own two responsibilities again and obscures the reusable `InstrumentSpec` boundary. An application-facing
helper may compose `assemble` and `fromSpec` under a distinct name.

Alternative considered: retain `ValidatedDefinition` as an alias to `InstrumentSpec`. Rejected under the unreleased API
policy; one enduring concept should have one name.

### 7. Keep `InstrumentDefinition` and `InstrumentSpec` out of codec ownership

The raw definition is stable-ID domain data, but it is not automatically a wire schema. Proposal 9 defines schema
versioning, numeric representation, parsing, and decode errors, then produces `InstrumentDefinition`. On loading, the
definition always assembles through a selected snapshot.

`InstrumentSpec` contains in-memory path-dependent handles and fails Java serialization. Process restart rebuilds it by
decoding the stable definition and assembling against the new lineage. No snapshot or handle is serialized as authority.

Alternative considered: make `InstrumentDefinition` the database schema because its fields look serializable. Rejected
because field evolution, exact-number representation, and schema version are separate compatibility decisions.

### 8. Keep catalog revision as diagnostic context, not instrument semantics

Assembly errors include the snapshot revision used for failed resolution. Successful `InstrumentSpec` does not make that
revision part of equality or economic identity. Its retained stable IDs and immutable handles completely determine the
meaning needed for calculation.

Applications needing reproducible audit may store an assembly envelope containing the source definition and catalog
revision/lineage deployment identity outside the instrument core. Because a local revision cannot identify a catalog
after restart by itself, exposing it on every instrument would suggest a stronger persistence guarantee than exists.

Alternative considered: retain the entire `CatalogSnapshot` in the spec. Rejected because it increases retention,
exposes unrelated reference data, and tempts later calculations to perform opportunistic lookup.

### 9. Use a package boundary before the final artifact split

Implementation initially introduces focused definition/assembly/spec packages beside the current broad economics code.
It does not create `trading-instrument-assembly` as a separate artifact because the target graph assigns the resolved
specification and instrument meaning to `trading-instrument-economics`.

Proposal 4 narrows/renames the current economics artifact around the instrument and moves higher concerns outward.
During this proposal, compile/API checks enforce direction inside the artifact:

```text
definition -> assembly -> InstrumentSpec -> Instrument
                         -> no reverse catalog dependency from calculations
```

Alternative considered: postpone the assembly package until the whole economics split. Rejected because the trusted
boundary is independently useful and later proposals need its exact input/output contract.

Alternative considered: create a permanent assembly service artifact. Rejected because it would be a thin split around
one pure constructor with the same foundational dependencies and no separate publication requirement.

## Risks / Trade-offs

- [Dependent lookup results can make Scala inference difficult] → Prototype the complete snapshot-to-spec path in real
  downstream fixtures, keep indices local, and provide focused role/listing helper products without public casts.
- [Accumulation can produce cascading noise] → Stage dependencies explicitly and suppress price/position/payoff checks
  when required resolved evidence or base/quote distinction is missing.
- [Removing repeated role objects changes many fixtures] → Migrate directly to one raw cohesive product and add no
  compatibility constructors; test that old contradictory states no longer compile or construct.
- [Assembly and economics remain in one artifact temporarily] → Enforce package/import direction and complete the
  physical narrowing in Proposal 4 before release.
- [A successful spec could accidentally retain a whole snapshot] → Store only the six resolved handles and typed
  economic components; add retention/API review and no-resolver compiler checks.
- [Catalog revision omission may concern audit users] → Keep revision in assembly diagnostics and let application/codec
  envelopes record it explicitly without making a local revision part of economic meaning.
- [Moving failures out of `EconomicsError` may break callers] → APIs are unreleased; update callers directly and keep
  typed causes more informative at their owning boundary.

## Migration Plan

1. Satisfy the portfolio gate and apply the reference-data identity and functional-catalog prerequisites.
2. Add raw stable-ID definition products and closed semantic role enums, migrating fixtures away from already resolved
   `Roles`/`ListingRules`/`ContractPayoff` input objects.
3. Add assembly violation/aggregate values, staged lookup/validation rules, and deterministic first-error projection.
4. Implement sealed `InstrumentSpec`, localize checked dependent narrowing, and add total `Instrument.fromSpec`.
5. Remove `ValidatedDefinition`, definition-specific `EconomicsError` cases/mapping, and raw `Instrument.create` paths
   without aliases.
6. Migrate existing economics tests/examples by building a pure catalog snapshot, assembling definitions, and then
   constructing instruments; preserve all exact downstream expected results.
7. Add compiler/adversarial tests for raw-direct construction rejection, spec forgery, reversed payoff endpoints,
   missing snapshot membership, and absence of public retagging authority.
8. Update docs to state the one-way trust boundary and link the later pure economics and codec proposals.
9. Run formatting, focused assembly/economics tests, immutable-JAR boundary tests, `sbt -batch clean test`, strict
   OpenSpec validation, staged-scope checks, and fresh independent review.

Rollback is a source-level revert of the unreleased API migration. No wire schema, persisted handles, or live side
effect is introduced, so rollback requires no external data conversion.
