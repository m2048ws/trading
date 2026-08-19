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

A static normalization capability does not automatically imply existence of a
runtime dimension witness.

A runtime `DimRef[D]` carries authority about the runtime `DimensionKey`
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
that may be accepted by static normalization.

In particular:

```text
Normalize[Atom[K]]
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
such as `DimensionKey` equality.

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

# Settled Simplification Decisions

## DEC-A01 — Simplified static dimension model

**Status:** SETTLED

**OpenSpec change:**

```text
simplify-static-dimension-model
```

The archived change replaced the earlier richer static dimension
evidence architecture with a smaller model centered conceptually on:

```scala
Dimension
Dim[...]
Power[K, Int]
Atom[K]
One
Times[A, B]
Inverse[A]
Divide[A, B]
Normalize[D]
SameDimension[A, B]
```

The exact API must be read from the archived OpenSpec change and current source.

Do not treat this section as a substitute for those artifacts.

The archived change established:

- one normalization capability rather than separate product/quotient/inverse
  evidence families;
- concrete singleton atom keys;
- static/runtime atom authority through `DimRef`;
- `SameDimension` remaining equivalence rather than validity;
- readable source dimension expressions such as `Divide[A,B]`;
- canonicalization remaining hidden behind the relevant static capability.

Independent review approved this state, and the OpenSpec change is archived.

---

## DEC-A02 — Current arithmetic validation policy

**Status:** SETTLED

**OpenSpec change:**

```text
simplify-static-dimension-model
```

The current conservative model validates dimension-preserving arithmetic with
the accepted static validity capability.

The important distinction is:

```scala
Normalize[D]
```

means the dimension expression is valid,

while:

```scala
Normalize.Aux[D, D]
```

would additionally require the input spelling itself to already be canonical.

Valid source expressions such as rate dimensions must not be forced into
canonical spelling merely to perform preserving arithmetic.

This policy may be simplified by a later explicit change, but must not be
silently weakened during unrelated work.

---

# Proposed Future Decisions

## DEC-P01 — Demote SameDimension from homogeneous arithmetic

**Status:** PROPOSED

**Suggested future OpenSpec change:**

```text
demote-same-dimension
```

Proposed direction:

Ordinary homogeneous arithmetic should operate on exactly the same static
dimension type.

Conceptually, prefer:

```scala
def +(that: Quantity[D]): Quantity[D]
```

over:

```scala
def +[E <: Dimension](
  that: Quantity[E]
)(using SameDimension[D,E]): Quantity[D]
```

`SameDimension` would remain for:

- explicit alignment between different-but-equivalent static expressions;
- checked runtime equivalence recovery;
- advanced generic client APIs that intentionally cross static spellings.

Illustrative client code:

```scala
def total[D <: Dimension](
  left: Quantity[D],
  right: Quantity[D]
): Quantity[D] =
  left + right
```

Different static spellings would require an explicit transition such as:

```scala
right.alignTo[D]
```

This is not yet settled.

Do not implement it during unrelated work.

---

## DEC-P02 — Trust existing dimensional values for preserving operations

**Status:** PROPOSED

**Suggested future OpenSpec change:**

```text
trust-existing-dimensional-values
```

Proposed invariant:

> If a `Quantity[D]` or `GridQuantity[D,G]` already exists through supported
> public construction, its dimension validity was established at the
> construction boundary.

If this invariant can be proved across the whole public API, operations that
preserve `D` would no longer require repeated normalization evidence.

Example target ergonomics:

```scala
def twice[D <: Dimension](
  value: Quantity[D]
): Quantity[D] =
  value + value
```

and:

```scala
def scale[D <: Dimension](
  value: Quantity[D],
  factor: Rational
): Quantity[D] =
  value * factor
```

without:

```scala
using Normalize[D]
```

Validity authority would remain necessary where a value is manufactured without
an existing trusted value, for example potentially:

```scala
Quantity.zero[D]
```

and where a new dimension expression is computed.

This change requires a complete audit of all public value-construction paths.

It is not yet settled.

---

## DEC-P03 — Freeze static dimension authority after current remediation

**Status:** PROPOSED

**Suggested future OpenSpec change:**

```text
freeze-static-dimension-authority
```

After `simplify-static-dimension-model` is independently approved, consider a
small explicit baseline/freeze change if useful to document the accepted
authority boundaries.

The goal would be to make the following responsibilities explicit and stable:

```text
static expression validity/canonicalization
    -> Normalize or its accepted successor

runtime inhabitation/identity
    -> DimRef

dimension equivalence
    -> SameDimension

exact value
    -> Quantity

grid coordinate
    -> GridQuantity
```

This may prove unnecessary if the active simplification change already captures
the final authority model sufficiently.

Do not create this change merely for process symmetry.

---

# Exploratory Decisions

## DEC-E01 — Internalize dimension normalization

**Status:** EXPLORING

**Suggested exploration/change name:**

```text
internalize-dimension-normalization
```

Question:

Can public reliance on:

```scala
Normalize[D]
Normalize.Aux[D, O]
```

be substantially reduced or removed without harming useful generic downstream
programming?

Potential direction:

Dimension-changing arithmetic could preserve expression types directly:

```scala
Quantity[A] * Quantity[B]
```

could return:

```scala
Quantity[Times[A,B]]
```

rather than requiring public canonical-output evidence.

Canonical equivalence could be requested explicitly at boundaries.

However, public normalization currently provides an important advanced
capability:

```scala
Normalize.Aux[Expression, O]
```

allows downstream generic libraries to ask the compiler to compute a canonical
output type `O` and reuse `O` in their own APIs.

That extensibility cost must be evaluated before normalization is internalized.

No outcome is currently selected.

Do not implement this idea merely to reduce the number of public concepts.

---

## DEC-E02 — Replace type-only zero construction with witness-based construction

**Status:** EXPLORING

If trusted-value semantics are adopted, manufacturing a quantity from only:

```scala
D <: Dimension
```

still requires an authority source.

One possible future API is:

```scala
Quantity.zero(dimRef)
```

or:

```scala
dimRef.zero
```

rather than:

```scala
Quantity.zero[D](using Normalize[D])
```

This could move construction authority toward `DimRef`.

Tradeoffs include:

- ergonomics for generic empty folds;
- algebra identity instances;
- availability of runtime witnesses for composite dimensions;
- distinction between static validity and runtime inhabitation.

No decision has been made.

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
with a smaller normalization-centered model.

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
