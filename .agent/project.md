# Trading Project Overview

## Purpose

`trading` is a Scala 3 multi-module library for exact financial and trading quantities.

The foundation intentionally separates:

- exact mathematical quantities;
- discrete/grid-constrained quantities;
- static dimension expressions;
- runtime dimension identity;
- runtime grids and provenance.

The quantities foundation must remain useful independently of higher-level domain models such as instruments, orders,
executions, positions, P&L, accounts, ledgers, and wallets. A current aggregate economics module implements some of
those concerns, but they remain outside the quantities foundation and are being separated by active proposals.

## Modules

The root SBT project is `trading` and is an unpublished aggregator.

The current production modules are:

- project: `quantities`
- artifact: `trading-quantities`
- primary package: `trading.quantity`
- project: `referenceData`
- artifact: `trading-reference-data`
- primary package: `trading.reference`
- project: `application`
- artifact: `trading-application`
- primary package: `trading.application`
- project: `instrumentEconomics`
- artifact: `trading-instrument-economics`
- primary package: `trading.economics.instrument`
- project: `economics`
- artifact: `trading-economics`
- primary packages: `trading.order`, `trading.scenario`, `trading.fee.policy`, `trading.risk`

A separate downstream/boundary project exercises completed public artifacts from outside their production modules.

The current physical dependency graph is:

```text
quantities <- referenceData <- application
     ^             ^
     |             +-- instrumentEconomics <- economics
     +------------------------^

all completed production artifacts -> adversarialBoundary (test-only)
```

The root aggregates the five production modules plus the test-only adversarial boundary and is not published. Reference
data contains pure immutable catalog state/transitions and coherent snapshots. Application currently contains only the
minimal `LiveCatalog[F]` port. The benchmark-only project is non-published and outside root aggregation.

The quantities production package layout includes:

- `trading.quantity`
- `trading.quantity.grid`
- `trading.quantity.refinement`
- `trading.quantity.algebra`

The reference-data production package is `trading.reference`. It owns stable asset/grid identity, definitions, trusted
handles, opaque lineage, pure handle reconciliation, immutable catalog transitions, and snapshots. Quantity-owned
packed records are absent; Proposal 9 owns their durable replacement.

Avoid speculative package subdivision unless an actual body of code requires it.

Instrument economics owns assembled instruments, exact economic values, and pure valuation. The transitional economics
artifact owns only downstream order, hypothetical execution-scenario, fee-policy, and risk packages until Proposals
5 through 7 move them into their final artifacts.

## Exact Quantity Model

The central exact quantity type is conceptually:

```scala
opaque type Quantity[D <: Dim] = Rational
```

A `Quantity[D]` is an arbitrary exact rational quantity in dimension `D`.

It does not imply membership in a discrete grid.

Exact rational arithmetic is the mathematical interior of the system.

## Grid Quantity Model

Discrete values are represented separately, conceptually:

```scala
opaque type GridQuantity[D <: Dim, G] = BigInt
```

A `GridQuantity[D,G]` is an integer coordinate in explicit grid `G`.

Grid membership is contextual.

An asset or dimension does not inherently imply one universal grid.

Different contexts may impose different grids, for example:

- protocol;
- listing;
- settlement;
- storage;
- venue;
- account representation.

Grid-to-exact conversion is exact.

Exact-to-grid conversion requires either:

- exact narrowing; or
- explicit quantization, preserving/reporting residual information as required.

## Static Dimensions

The current simplified static model uses a small dimension expression language centered on concepts such as:

```scala
Dim
Canonical[...]
Power[K, Int]
Atom[K]
One
Times[A, B]
Inverse[A]
Divide[A, B]
SameDimension[A, B]
```

The exact public surface must always be refreshed from the repository before performing implementation or review work.

Do not resurrect older static evidence families merely because they appear in historical OpenSpec material or old compiler fixtures.

## Private Static Interpretation

The compiler machinery that validates and canonically interprets closed static
dimension expressions is library-private.

It supports non-reflexive `SameDimension` derivation and internal coherence
checks without exposing `Normalize`, `Normalize.Aux`, an associated canonical
output, or caller-constructible proof tokens.

Generic dimension-changing arithmetic preserves public expression types such as
`Times[A, B]`, `Inverse[A]`, and `Divide[A, B]`. A caller that wants another
equivalent spelling nominates it explicitly through `SameDimension` and
`alignTo`.

Private static interpretation is not runtime identity or value-construction
authority.

## SameDimension

`SameDimension[A,B]` represents dimensional equivalence.

Reflexive:

```scala
SameDimension[D,D]
```

is intentionally structural/type identity and must not be interpreted as proof that `D` is a valid canonical dimension expression.

Homogeneous arithmetic requires the exact same Scala dimension type and consumes
no `SameDimension` evidence. Different-but-equivalent spellings require an
explicit `alignTo` transition. Runtime recovery may issue the same restricted
capability only after authoritative `DimKey` equality.

## Trusted Carriers and Construction Authority

Every normally returned `Quantity[D]` and `GridQuantity[D, G]` has a dimension
index established by an authoritative construction or checked reconstruction
boundary.

Operations that preserve an existing carrier's dimension therefore require no
repeated static-dimension or equivalence capability. Manufacturing a value
without an existing trusted carrier requires `DimRef[D]`, `GridRef[D]`, or a
documented stronger matching witness.

Possessing a dimensional value does not reveal `DimRef`, `DimKey`,
`SameDimension`, grid identity, or registry provenance.

## Runtime Dimension Identity

`DimRef[D]` bridges static dimension identity to runtime `DimKey`.

For publicly inhabitable atom types, static/runtime authority must be unique:

> Two publicly constructible `DimRef[Atom[K]]` values with the same accepted static atom type must not denote contradictory runtime atom identities.

Runtime authority is unique for publicly inhabitable `DimRef` keys.

It is not required to be total over every key accepted by library-private
static interpretation.

In particular:

> Private acceptance of `Atom[K]` does not imply that a public
> `DimRef[Atom[K]]` exists.

This separation keeps static grammar validation and runtime witness authority distinct.

## Rates and Ratios

Rates are exact quantities with dimensional meaning derived from source/target dimensions.

Conceptually:

```scala
type Rate[From <: Dim, To <: Dim] =
  Quantity[Divide[To, From]]
```

A ratio is a dimensionless exact quantity.

Source dimension expressions may remain readable source expressions rather than forcing users to expose canonical internal `Canonical[...]` forms.

## Runtime Identity and Evidence

Runtime `DimKey` arithmetic must agree with valid static dimension semantics.

Runtime recovery of equivalence must be based on checked authoritative runtime identity.

Casts used internally are acceptable only when protected by clear phantom, provenance, or runtime equality invariants.

Do not introduce public proof-construction authority merely to make internal implementation easier.

## Refinements

Refinements such as:

- nonnegative;
- positive;
- nonzero;
- positive whole;
- nonzero whole;

express numeric predicates independently of dimensional semantics.

Closed refinement operations should preserve predicates structurally where possible rather than unnecessarily revalidating already-guaranteed properties.

## Algebra

Typelevel Algebra integration is optional convenience.

Algebra instances must reflect the same arithmetic and authority semantics as the direct public API.

Do not let an algebra instance create an arithmetic capability that the direct API intentionally rejects.

## Domain-Neutral Runtime Dimension Identity

`DimKey`, `DimRef`, and checked `SameDimension` recovery live directly in `trading.quantity`; there is no separate
`trading.quantity.runtime` production package or quantity-owned heterogeneous carrier. These mathematical runtime
capabilities establish dimension identity only and confer no stable asset, grid, or reference-data provenance.

Pure `CatalogModel.commit` and immutable `CatalogSnapshot` own reference-data construction and lookup. The application
artifact owns only `LiveCatalog[F]`; Proposal 8 owns concrete live coordination, and Proposal 9 owns future durable
decoding.

## Design Philosophy

Prefer:

- exact arithmetic;
- small public conceptual surfaces;
- explicit boundaries;
- static/runtime coherence;
- canonical internal representations;
- meaningful downstream compiler tests;
- simple generic client ergonomics;
- clean pre-release API design.

Avoid:

- compatibility machinery for APIs that never shipped;
- speculative generalization;
- public evidence families that exist only to support macro internals;
- silent authority expansion;
- fixes that solve one compiler fixture by weakening a global invariant.

## Architecture and Functional Design Charter

The canonical OpenSpec capabilities `repository-architecture` and `scala-functional-design` govern the architecture
charter. The detailed guide is `docs/design-principles.md`; the current/target and cross-proposal record is
`docs/architecture-charter-audit.md`.

Every production concept has one primary owning layer for its vocabulary, invariants, construction, and errors.
Dependencies remain acyclic and point toward the smallest lower-level meaning required. Lower mathematical/domain
layers do not depend on higher policy, workflows, codecs, or runtime implementations.

The proposed responsibility graph is:

| Proposed layer | Owns | May depend on |
| --- | --- | --- |
| quantities | exact arithmetic, dimensions, anonymous grids, refinements, mathematical algebra | mathematical foundations |
| reference data | stable identities/versions, trusted handles, immutable catalog transitions/snapshots/errors | quantities |
| instrument economics | assembled instrument meaning, lots, prices, valuation, economic fee values, P&L | quantities and reference data |
| order model | immutable order instructions | instrument economics |
| execution scenario | hypothetical execution evidence and checked outcomes | order model and instrument economics |
| fee policy | venue/account/tier policy and assessed attribution | instrument economics, orders, scenarios |
| risk | downside and sizing procedures | quantities and instrument economics |
| application | effect-polymorphic ports/workflows for genuine external capabilities | required reference/domain layers |
| boundary codecs | versioned boundary records and checked reconstruction | internal values encoded, never runtime |
| runtime | concrete effects, resources, coordination, streams, clients, telemetry, interpreters | application and required domain/codec layers |

Target modules and APIs remain proposed until their owning change is implemented; reference data and the initial
application port are now physical. Logical boundaries precede
physical modules; create a module only with a coherent code body or enforceable dependency, publication, or independent
verification boundary.

## Layer-Specific Functional Profile

- quantities are pure, exact, type-indexed, algebraic, and law-tested;
- reference data uses pure immutable state transitions and coherent snapshots; issued handles and reconciliation remain
  immutable;
- domain and economics values use closed ADTs, refinements, smart constructors, typed errors, and no infrastructure
  effects;
- application ports use effect polymorphism only for genuine environmental variation;
- codecs remain pure where possible and reconstruct through owning checked boundaries against an explicit snapshot;
- runtime contains concrete effects, mutation, concurrency, resources, transactions, streams, clients, and telemetry.

Design begins by identifying honest sums, products, refinements, non-empty structures, associative combinations,
identities, orderings, traversals, and pure state transitions. Use the weakest complete abstraction and keep common
public calls domain-readable.

Independent validation failures accumulate in deterministic order. Dependent checks sequence only from evidence
produced by a successful prerequisite. Successful results retain the strongest useful evidence, and errors remain
owned by the layer that can explain them.

Public mathematical and domain APIs represent expected absence, invalidity, conflict, and failure in their result
types. They do not use `null`, unchecked extraction, sentinel values, or ordinary exceptions as control flow.
Unavoidable partial operations, casts, and mutable mechanisms remain narrowly scoped, explicitly named or hidden,
protected by a stated invariant, and inaccessible as public construction authority.

## Dependency and Platform Baseline

Prefer mature maintained mechanisms for general infrastructure when they satisfy the actual contract, but contain a
dependency in the narrowest owning module/configuration. Project-owned domain meanings, trusted transitions, public
errors, and durable schema semantics do not become third-party representations accidentally. A second vocabulary for
the same concern needs an explicit distinct requirement.

The minimum build and runtime JDK is 17 while the repository uses Scala 3.8.x. Independently released libraries use
independently named version coordinates even when their current strings match. Proposal 1 owns splitting the current
shared Cats/Algebra coordinate; Proposal 0 does not edit `build.sbt`.

## Trust, Effects, and Hot Paths

Boundary primitives become trusted domain data once through parse, resolution against one coherent immutable view,
validation, and assembly. Trusted values carry their established identity/evidence into pure calculations and do not
repeatedly resolve through live state.

Control-plane identity/configuration/publication work stays out of data-plane arithmetic, valuation, decoding, event
processing, and replay. Prefer immutable snapshots, batching, resolved capabilities, and caching. Any unavoidable live
coordination in a hot path requires an explicit invariant and representative measurement.

Verification is proportional to the claim: laws receive property/discipline tests; type-level authority receives
packaged downstream positive/negative fixtures; multiple interpreters share contracts; concurrency receives coherence
tests; complexity receives deterministic bounds; and performance-sensitive hot paths receive representative JMH
evidence.

## Pre-release History Policy

Before the first public release, prefer rewriting active specs, documentation, and code to describe the intended final design directly.

Do not preserve deprecation, migration, or compatibility history for APIs that were never released.

Archived OpenSpec changes remain historical records and should not be rewritten casually by later changes.

## OpenSpec

OpenSpec is the design/change-control mechanism for nontrivial project changes.

Agents must distinguish:

- current canonical project/spec state;
- active proposed changes;
- archived historical changes;
- future exploratory ideas.

An active OpenSpec change is not complete merely because implementation tests pass.

Independent review is a separate process gate.

## Review Philosophy

Reviews should attempt to falsify the claimed invariants using supported public Scala and the actual packaged/public API.

Important recurring review areas include:

- static/runtime identity coherence;
- generic specialization and substitution stability;
- canonical static representations;
- public construction/proof authority;
- real downstream compiler behavior;
- runtime `DimKey` agreement;
- generic client ergonomics;
- OpenSpec semantic conformance;
- formatting and Git/index cleanliness;
- full clean multi-module builds.

Implementation and remediation agents must not certify their own independent review gate.
