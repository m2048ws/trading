# Trading Project Decisions

This file records significant architectural decisions and their current status.

It exists so future steward, implementation, review, and remediation agents do
not have to reconstruct project history from old diffs, archived OpenSpec
changes, or conversation transcripts.

This file is not a substitute for OpenSpec.

OpenSpec defines active change semantics. This file records the project's
higher-level decision state and rationale.

---

# Status Vocabulary

Every decision must have one of these statuses.

## SETTLED

The decision is part of the accepted project baseline.

Agents should preserve it unless an explicit new OpenSpec change proposes to
alter it.

## ACTIVE

The decision is currently being implemented or reviewed by an active OpenSpec
change.

Agents must read that active change for authoritative details.

An ACTIVE decision must not be treated as SETTLED until the change is
independently approved, archived, and incorporated into the accepted baseline.

## PROPOSED

The idea is considered a likely future direction but has not yet been accepted.

Do not implement it as part of unrelated work.

It should go through OpenSpec exploration/proposal before implementation.

## EXPLORING

The idea is deliberately unresolved.

Agents may analyze alternatives but must not assume an outcome.

## SUPERSEDED

The project deliberately moved away from this design.

Do not restore it merely because historical code or archived specifications
mention it.

A new explicit design decision would be required to bring it back.

---

# Settled Decisions

## DEC-011 — Hybrid native worker orchestration

**Status:** SETTLED

**OpenSpec change:**

```text
adopt-native-subagent-orchestration
```

Codex-native subagents are the preferred interactive control plane only for
worker roles whose required guards can be satisfied. Repository scripts remain
the guard plane and complete portable fallback. The current repository/client
boundary satisfies that rule only for bounded read-only exploration: native
primary-worktree writers are mechanically ineligible because the complete
executable decision closure is worker-writable and the broker-owned reservation
does not survive broker-process death.

The archived change preserves all existing workflow invariants while introducing:

- one shared machine-readable role policy for both backends;
- retained profiles and a diagnostic broker protocol for possible future
  native apply, remediation, and finalization workers, without current launch
  or transition authority;
- bounded native read-only exploration;
- script-only formal independent review against an isolated staged snapshot;
- schema and logical validation before any worker report informs a transition;
- a steward-owned broker whose private control channel and protected memory
  retain the full launch tuple, monotonic generation, and random one-shot
  capability outside worker-owned handoffs;
- a parent-held root-inode writer reservation for script-backed formal writers,
  never inherited by delegated workers;
- compact collection output with complete evidence retained by ignored path;
- content-sensitive dirty-state refresh and classification instead of blind
  fallback after possible native mutation;
- volatile backend, worker-identity, artifact-path, and staged-tree trace data.

Enabling native primary-worktree writers later requires protected immutable
execution for their complete transition/release/fallback decision closure and
an independent controller that retains exclusion across broker death. Fresh
independent approval and archive established this conservative boundary as part
of the accepted baseline. The change itself was bootstrapped through the
pre-existing script workflow.

---

## DEC-001 — Exact rational interior

**Status:** SETTLED

The mathematical interior of the quantities system uses arbitrary exact rational
arithmetic.

`Quantity[D]` is not universally fixed-point.

Rationale:

Financial and trading calculations can produce exact values that do not lie on
a settlement/storage grid.

For example, inverse contracts and similar financial formulas may naturally
produce exact rational quantities before a discrete settlement boundary is
applied.

Discrete representation belongs at explicit boundaries rather than defining the
entire mathematical model.

---

## DEC-002 — Grids are contextual

**Status:** SETTLED

An asset or dimension does not inherently define one universal grid.

Grids belong to contexts such as:

- protocol;
- venue;
- listing;
- settlement;
- storage;
- account representation.

Therefore exact quantities and grid-constrained quantities remain separate
concepts.

---

## DEC-003 — Separate Quantity and GridQuantity

**Status:** SETTLED

The foundation distinguishes conceptually:

```scala
Quantity[D]
```

from:

```scala
GridQuantity[D, G]
```

`Quantity[D]` represents an arbitrary exact rational quantity.

`GridQuantity[D,G]` represents an integer coordinate in explicit grid `G`.

Grid-to-exact conversion is exact.

Exact-to-grid conversion requires exact narrowing or explicit quantization.

---

## DEC-004 — Domain models stay out of the quantities foundation

**Status:** SETTLED

The quantities foundation does not own:

- instruments;
- listings;
- orders;
- executions;
- positions;
- P&L;
- accounts;
- ledgers;
- wallets.

Those belong in later domain modules.

The quantities foundation should provide the exact arithmetic, grid, refinement,
runtime identity, and algebra mechanisms those later modules need.

---

## DEC-005 — Runtime dimension identity is separate from static grammar

**Status:** SETTLED

Static dimension validity/canonicalization and runtime dimension identity are
different responsibilities.

Acceptance by library-private static interpretation does not imply existence of
a runtime dimension witness.

A runtime `DimRef[D]` carries authority about the runtime `DimKey`
represented by `D`.

---

## DEC-006 — Runtime authority is unique, not total

**Status:** SETTLED

Runtime authority is required to be unique over publicly inhabitable `DimRef`
atom types.

Conceptually:

```text
for any publicly constructible:

x: DimRef[Atom[K]]
y: DimRef[Atom[K]]

x and y must not denote contradictory runtime atom identities
```

The project does **not** require runtime authority to be total over every key
that may be accepted by library-private static interpretation.

In particular:

```text
private interpretation accepts Atom[K]
```

must not be interpreted as implying:

```text
a public DimRef[Atom[K]] exists
```

Rationale:

Static grammar validation and runtime witness inhabitation should remain
separate authorities.

---

## DEC-007 — SameDimension reflexivity is not validity

**Status:** SETTLED

`SameDimension[D,D]` represents reflexive Scala/static identity.

It does not prove that `D` is a valid canonical dimension expression.

Therefore malformed `D` may still have reflexive `SameDimension[D,D]` without
gaining canonical or arithmetic authority.

Validity and equivalence remain separate concepts.

---

## DEC-008 — Runtime equivalence recovery is checked

**Status:** SETTLED

When equivalence is not already statically known and must be recovered from
runtime witnesses, it must be backed by checked authoritative runtime identity,
such as `DimKey` equality.

Runtime evidence recovery must not assume equivalence solely from generic type
shape.

---

## DEC-009 — Pre-release changes optimize for the intended final API

**Status:** SETTLED

Before the first public release, the project does not preserve compatibility,
migration, or deprecation scaffolding for APIs that never shipped.

When a design is removed before release, active specifications, documentation,
and implementation should generally be rewritten as though the removed API
never existed.

Archived OpenSpec changes remain historical records.

---

## DEC-010 — Independent review is a separate authority

**Status:** SETTLED

Implementation and remediation agents do not certify their own independent
review.

Every remediation returns to a fresh independent reviewer.

Archival requires independent approval.

---

# Settled Architecture Decisions

## DEC-C01 — Architecture and functional design charter

**Status:** SETTLED

**OpenSpec change:**

```text
establish-architecture-and-functional-design-charter
```

The archived change establishes cohesive one-way responsibility ownership, algebra-first modeling, semantic type
preservation, evidence-producing validation, a pure domain core with effect-polymorphic application ports and concrete
runtime interpreters, contained dependency admission, logical-before-physical modules, control-plane/data-plane
separation, domain-readable advanced Scala, and claim-proportional verification.

It records JDK 17 as the current minimum build/runtime baseline and requires independent version coordinates for
independently released libraries. Proposal 1 owns the actual Cats/Algebra coordinate split; this charter makes no build
change.

The target reference-data, instrument-economics, order, scenario, fee, risk, application, codec, and runtime graph is a
governance decision in this change. Its physical modules/APIs remain proposed until their owning dependent changes are
implemented. Current transitional exceptions are recorded in `docs/architecture-charter-audit.md`.

Fresh independent approval and archive established this decision as part of the accepted baseline. The proposed
physical modules and APIs remain governed by their owning dependent changes.

---

# Settled Simplification Decisions

## DEC-A01 — Simplified static dimension model

**Status:** SETTLED

**OpenSpec change:**

```text
simplify-static-dimension-model
```

The archived change replaced the earlier richer static dimension evidence
architecture. Subsequent approved changes internalized its temporary public
normalization capability. The current public model is centered conceptually on:

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

The exact API must be read from the current canonical OpenSpec specs and source;
the archived changes explain how that state was reached.

Do not treat this section as a substitute for those artifacts.

The accepted change lineage established:

- library-private closed-expression interpretation rather than a public
  normalization or associated-output capability;
- concrete singleton atom keys;
- static/runtime atom authority through `DimRef`;
- `SameDimension` remaining equivalence rather than validity;
- expression-preserving dimension-changing results such as `Times[A, B]` and
  `Divide[A, B]`;
- explicit `alignTo` when a caller nominates an equivalent spelling.

The relevant OpenSpec changes are independently approved and archived.

---

## DEC-A02 — Current arithmetic validation policy

**Status:** SETTLED

**OpenSpec changes:**

```text
simplify-static-dimension-model
demote-same-dimension
trust-existing-dimensional-values
internalize-dimension-normalization
```

The current arithmetic policy is:

- homogeneous addition and subtraction require the exact same Scala dimension
  type and no contextual dimension evidence;
- index-preserving operations trust existing normally returned carriers;
- dimension-changing arithmetic returns its public `Times`, `Inverse`, or
  `Divide` expression;
- endpoint-oriented rate operations return their named endpoint types;
- different-but-equivalent spellings cross only through explicit, non-null
  `SameDimension` and `alignTo`;
- manufacturing a carrier without an existing trusted value requires `DimRef`,
  `GridRef`, or a documented stronger matching witness.

Malformed indices are rejected at supported construction and checked
reconstruction roots. An otherwise uncallable method body over a hypothetical
malformed carrier may type-check for index-preserving operations; that does not
create public construction authority.

---

# Settled Follow-up Decisions

The `P` and `E` identifiers below retain their historical names, but their
directions were explicitly selected, implemented, independently reviewed, and
archived.

## DEC-P01 — Demote SameDimension from homogeneous arithmetic

**Status:** SETTLED

**OpenSpec change:**

```text
demote-same-dimension
```

Ordinary homogeneous arithmetic operates on exactly the same static dimension
type.

Conceptually, prefer:

```scala
def +(that: Quantity[D]): Quantity[D]
```

over:

```scala
def +[E <: Dim](
  that: Quantity[E]
)(using SameDimension[D,E]): Quantity[D]
```

`SameDimension` remains for:

- explicit alignment between different-but-equivalent static expressions;
- checked runtime equivalence recovery;
- advanced generic client APIs that intentionally cross static spellings.

Illustrative client code:

```scala
def total[D <: Dim](
  left: Quantity[D],
  right: Quantity[D]
): Quantity[D] =
  left + right
```

Different static spellings require an explicit transition such as:

```scala
right.alignTo[D]
```

The archived change established this behavior for exact quantities, grid
quantities, refinements, algebra instances, and downstream compiler boundaries.

---

## DEC-P02 — Trust existing dimensional values for preserving operations

**Status:** SETTLED

**OpenSpec change:**

```text
trust-existing-dimensional-values
```

Accepted invariant:

> If a `Quantity[D]` or `GridQuantity[D,G]` already exists through supported
> public construction, its dimension validity was established at the
> construction boundary.

Operations that preserve `D` require no repeated static-dimension or
equivalence evidence.

Example target ergonomics:

```scala
def twice[D <: Dim](
  value: Quantity[D]
): Quantity[D] =
  value + value
```

and:

```scala
def scale[D <: Dim](
  value: Quantity[D],
  factor: Rational
): Quantity[D] =
  value * factor
```

Authority remains necessary where a value is manufactured without an existing
trusted carrier, for example:

```scala
Quantity.zero[D]
```

which requires an authoritative `DimRef[D]`. `GridQuantity.zero[D, G]`
likewise requires `DimRef[D]` unless a matching grid witness owns the
construction.

Possessing a carrier does not reveal its dimension witness, grid witness,
runtime key, or registry provenance.

---

## DEC-P03 — Freeze static dimension authority after current remediation

**Status:** SETTLED

**OpenSpec change:**

```text
freeze-static-dimension-authority
```

The archived freeze established explicit authority boundaries. Later approved
changes refined its static side by internalizing normalization; the current
settled responsibilities are:

```text
closed-expression interpretation
    -> library-private compiler machinery

runtime inhabitation/identity
    -> DimRef

dimension equivalence
    -> SameDimension

exact value
    -> Quantity

grid coordinate
    -> GridQuantity

grid construction and exact embedding
    -> GridRef
```

These capabilities remain independent: private static acceptance does not
create runtime inhabitation, equivalence does not certify reflexive validity,
and value possession does not reveal authority or provenance.

---

# Settled Exploration Outcomes

## DEC-E01 — Internalize dimension normalization

**Status:** SETTLED

**OpenSpec change:**

```text
internalize-dimension-normalization
```

The public capabilities:

```scala
Normalize[D]
Normalize.Aux[D, O]
```

were removed without replacing them with another public associated-output
capability. The private interpreter remains solely for closed-expression
validation, non-reflexive equivalence derivation, and internal coherence.

Generic dimension-changing arithmetic preserves expression types directly:

```scala
Quantity[A] * Quantity[B]
```

could return:

```scala
Quantity[Times[A,B]]
```

rather than requiring public canonical-output evidence. Generic callers may
nominate an equivalent output explicitly:

```scala
SameDimension[Times[A, B], O]
```

and use `alignTo[O]`. Endpoint-oriented rate APIs retain their named result
types. No compatibility alias or replacement associated-output evidence is
part of the public API.

---

## DEC-E02 — Replace type-only zero construction with witness-based construction

**Status:** SETTLED

**OpenSpec change:**

```text
internalize-dimension-normalization
```

Manufacturing a quantity from only:

```scala
D <: Dim
```

requires an authority source. The accepted API is:

```scala
Quantity.zero[D](using DimRef[D])
```

`GridQuantity.zero[D, G]` likewise requires `DimRef[D]` unless a matching grid
witness owns the construction. Coefficient-bearing quantity construction,
runtime rate construction, refinements that manufacture values, and
identity-bearing algebra follow the same authority rule. Existing carriers may
still undergo index-preserving transformations without repeated authority.

---

# Superseded Decisions

## DEC-X01 — Universal fixed-point quantity representation

**Status:** SUPERSEDED

The project does not model every financial quantity as an integer number of
smallest units.

Exact rational quantities are the mathematical interior.

Discrete grids are contextual boundaries.

Do not restore universal fixed-point representation.

---

## DEC-X02 — Representation-polymorphic Quantity

**Status:** SUPERSEDED

Earlier concepts equivalent to:

```scala
Quantity[D, R]
RationalQuantity
Rate[A, B, R]
Ratio[R]
Quanta
```

are no longer part of the intended design.

Do not restore representation polymorphism without a new explicit design
decision.

---

## DEC-X03 — Public operation-specific static evidence families

**Status:** SUPERSEDED

Earlier public concepts included operation-specific evidence such as:

```text
NormalizedPowers
DimensionProduct
DimensionQuotient
DimensionInverse
DimensionAlignment
```

The archived simplified static-dimension change replaced this architecture
with a smaller model. Its public normalization capability was subsequently
internalized; current clients use expression-preserving results and explicit
`SameDimension` alignment.

Historical references do not justify restoring those APIs.

---

## DEC-X04 — Generalized CoordinateGrid abstraction

**Status:** SUPERSEDED

A generalized `CoordinateGrid[V,Z]` abstraction was considered but removed as
premature.

The current design uses direct `GridQuantity` / grid reference operations and
only introduces generic algebra where it has a concrete use.

Do not restore `CoordinateGrid` without a new use case and design proposal.

---

## DEC-X05 — Money/domain package inside quantities foundation

**Status:** SUPERSEDED

Premature domain concepts such as account, market, fee, tiered-pricing, and
approved-persistence models do not belong in the quantities foundation.

They should be designed later in appropriate domain modules.

---

# Steward Rules for This File

The steward must:

1. Read this file at startup.
2. Refresh ACTIVE decision details from the actual repository/OpenSpec state.
3. Never promote ACTIVE, PROPOSED, or EXPLORING decisions to SETTLED on its own.
4. Never implement PROPOSED or EXPLORING decisions as incidental remediation.
5. Flag implementation diffs that appear to implement a future decision outside
   the declared active change.
6. Propose updates to this file when an OpenSpec change is archived or a human
   explicitly settles/rejects a design direction.

When an OpenSpec change is archived successfully, the steward should recommend a
small decision-state update such as:

```text
DEC-A01:
  ACTIVE -> SETTLED
```

or, if the final archived semantics differ materially, update the decision text
to match the accepted result.

The steward must not rewrite decision history merely to make current code appear
consistent.
