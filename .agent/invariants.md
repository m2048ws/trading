# Trading Project Invariants

These invariants describe settled project constraints.

They are more authoritative than an implementation agent's local design choice.

An active OpenSpec change may intentionally propose changing an invariant, but an apply or remediation agent must not silently weaken one to make implementation or tests easier.

If satisfying an active change appears to require changing an invariant that the change does not explicitly address, stop the implementation/remediation loop and escalate to OpenSpec exploration/design review.

Review findings should cite invariant IDs where applicable.

---

# Exact Quantities

## INV-Q1 — Exact rational semantics

`Quantity[D]` represents an arbitrary exact rational coefficient in dimension `D`.

It is not inherently fixed-point and does not imply grid membership.

## INV-Q2 — Exact arithmetic stays exact

Ordinary `Quantity` arithmetic must not introduce rounding, truncation, floating-point approximation, or hidden quantization.

Any loss of exactness must occur only through an explicitly named boundary operation.

## INV-Q3 — Dimension and coefficient are independent concerns

Dimensional correctness must not depend on inspecting or encoding meaning into the rational coefficient.

The coefficient is mathematical magnitude; `D` carries dimensional meaning.

---

# Grids

## INV-G1 — Grid membership is contextual

A dimension or asset does not imply one universal grid.

Grid constraints belong to explicit contexts such as settlement, listing, protocol, storage, venue, or account representation.

## INV-G2 — Grid quantities are coordinates

`GridQuantity[D,G]` represents an integer coordinate in explicit grid `G`.

The grid phantom is part of the value's meaning and must not be silently discarded or substituted.

## INV-G3 — Grid-to-exact embedding is exact

Embedding a valid `GridQuantity[D,G]` into `Quantity[D]` must be exact.

## INV-G4 — Exact-to-grid is explicit

Converting an arbitrary exact `Quantity[D]` into a grid requires either:

- exact membership/narrowing; or
- explicit quantization with the required residual/reporting semantics.

Hidden rounding is forbidden.

## INV-G5 — Grid provenance remains independent of dimensions

Dimension equivalence must not silently imply grid identity, grid ownership, or registry provenance.

---

# Static Dimensions

## INV-S1 — Closed static grammar

Only dimensions accepted by the current documented static dimension grammar may receive canonical static authority.

Scala upper bounds alone are not sufficient proof of semantic validity.

## INV-S2 — Canonicalization is coherent

Definitionally or semantically equivalent valid dimension expressions must not receive contradictory canonical interpretations.

Canonicalization must be deterministic for supported inputs.

## INV-S3 — Static and runtime arithmetic agree

For valid dimension expressions, static dimensional arithmetic and runtime `DimKey` arithmetic must denote the same dimension.

## INV-S4 — Static interpretation is library-private

Closed-expression validation and canonical mathematical interpretation belong
to library-private compiler machinery.

The machinery may support non-reflexive `SameDimension` derivation and internal
coherence checks, but it must not expose public `Normalize`, associated-output
evidence, recursive rules, or caller-constructible proof tokens.

It is not runtime `DimRef` inhabitation or value-construction authority.

## INV-S5 — Private interpretation does not imply DimRef existence

Private acceptance of `Atom[K]` does not imply that a public
`DimRef[Atom[K]]` must exist.

Private static interpretation and runtime witness inhabitation remain distinct
concepts.

## INV-S6 — SameDimension is equivalence, not validity

`SameDimension[A,B]` represents dimensional equivalence.

In particular:

```scala
SameDimension[D,D]
```

may be reflexively available without certifying that `D` is a valid canonical dimension expression.

Code must not use reflexive `SameDimension` as a substitute for static-dimension validity.

## INV-S7 — Generic specialization must remain sound

Private interpretation or derived equivalence evidence must not commit to
equality, inequality, atomicity, reducibility, or opacity while those facts can
still change through later generic specialization.

Local aliases, refinements, dependent selections, or stable local rebinding must not launder unresolved generic structure into trusted static authority.

## INV-S8 — Public proof authority is minimal

Compiler/macro implementation machinery must not be exposed as composable
public evidence merely to implement private static interpretation.

Callers must not be able to choose arbitrary canonical outputs or construct trusted evidence for malformed representations through public or package-private helper authority.

---

# Static / Runtime Identity

## INV-I1 — Public DimRef authority is unique

For any publicly inhabitable static atom type `Atom[K]`, public construction must not permit two `DimRef[Atom[K]]` values that denote contradictory runtime atom identities.

Conceptually:

```text
same publicly accepted static atom identity
    => same authoritative runtime DimKey
```

## INV-I2 — Runtime authority is not required to be total

The project requires uniqueness for publicly inhabitable `DimRef` atom types.

It does not require every atom key accepted by private static interpretation to
have a public runtime `DimRef` constructor.

## INV-I3 — Runtime equivalence is checked

When equivalence is recovered from runtime values rather than already known statically, it must be based on checked authoritative runtime identity such as `DimKey` equality.

## INV-I4 — Generative identities remain distinct

Fresh/generative runtime dimension witnesses must retain distinct static identities unless an explicit checked equivalence proves otherwise.

A generic type widening must not collapse distinct runtime identities into one authoritative static atom type.

---

# Arithmetic Boundaries

## INV-A1 — Malformed dimensions gain no arithmetic authority

A malformed static dimension claim must not acquire arithmetic capability merely because it satisfies a Scala upper bound or reflexive type equality.

## INV-A2 — Dimension-changing arithmetic preserves the complete expression

Generic dimension-changing arithmetic returns the complete public `Times`,
`Inverse`, or `Divide` expression rather than selecting a public canonical
output type. Canonical comparison occurs only at an explicit equivalence
boundary.

## INV-A3 — Direct and algebra APIs agree

Optional algebra/typeclass instances must not permit arithmetic that the direct public API rejects.

Likewise, direct APIs must not silently have weaker dimensional invariants than corresponding algebra instances.

## INV-A4 — Runtime-backed arithmetic preserves provenance

Heterogeneous/runtime arithmetic must preserve required registry ownership, provenance, and checked identity semantics.

Static simplification must not weaken runtime provenance rules.

## INV-A5 — Existing dimensional carriers are trusted

For supported callers, every normally returned dimensional carrier has already
passed an authoritative construction or checked reconstruction boundary.

Operations that preserve its existing dimension index require no repeated
static-dimension or equivalence capability. Manufacturing a carrier without an
existing trusted value still requires `DimRef`, `GridRef`, or a documented
stronger matching witness.

---

# Refinements

## INV-R1 — Refinement predicates are orthogonal to dimensions

Numeric refinements such as positive, nonnegative, and nonzero describe coefficient/coordinate predicates, not dimension validity.

## INV-R2 — Closed refinement operations preserve proven predicates structurally

If an operation is mathematically closed over a refinement, implementations should preserve the refinement without unnecessary predicate revalidation.

## INV-R3 — Refinements must not create hidden arithmetic authority

A refined wrapper must not allow malformed dimensions, invalid grids, or unchecked runtime identities to bypass the corresponding underlying arithmetic boundary.

---

# API and Architecture

## INV-P1 — Foundation remains domain-neutral

The quantities foundation does not contain speculative instrument, order, execution, position, P&L, account, ledger, or wallet domain models.

Those belong in later modules.

## INV-P2 — Prefer small public concepts

Do not introduce a new public proof family, abstraction, package, or generic layer solely to make internal implementation convenient.

A new public concept requires an explicit user-facing semantic role.

## INV-P3 — Do not resurrect superseded APIs accidentally

Historical OpenSpec artifacts, archived designs, and negative fixtures are not justification for restoring removed production APIs.

Any restoration requires an explicit new design decision.

## INV-P4 — Concrete ergonomics matter

Normal concrete client code should derive required static evidence automatically where the design permits.

Fixes that make sound concrete use materially more cumbersome require explicit ergonomic review.

## INV-P5 — Generic programming remains intentional

Generic clients should be able to forward the project's intended public capabilities without depending on private macro machinery, casts, or implementation-specific types.

---

# Build and Test Boundaries

## INV-T1 — Real public API behavior is tested downstream

Important compiler/type-system guarantees must include tests that compile real downstream Scala source against the public/package artifact, not only same-package unit tests.

## INV-T2 — Negative compiler fixtures prove the intended failure

A negative compiler fixture must:

- have a valid independently compilable prelude;
- fail at the intended expression;
- check relevant diagnostic behavior;
- avoid succeeding merely because of syntax, import, stale-name, or unrelated errors.

## INV-T3 — Compiler diagnostics remain clean

Expected static rejection must not leak:

- compiler stack traces;
- macro implementation exceptions;
- cyclic-reference implementation failures;
- private internal authority names unnecessarily.

## INV-T4 — Configured checks are release gates

A failing configured check such as:

- tests;
- formatting;
- strict OpenSpec validation;
- packaged downstream compilation;
- Git diff checks;

is a real blocker, not a stylistic suggestion.

## INV-T5 — Build correctness may not depend on retries

Sleep, retry, silent rerun, or nondeterministic filesystem timing must not be used to disguise repository build failures.

---

# OpenSpec and Workflow

## INV-W1 — Active change scope is authoritative

Implementation and remediation work must stay within the semantic scope of the active OpenSpec change.

If a sound fix requires a materially different API or invariant, stop and escalate to exploration/proposal rather than silently redesigning the project.

## INV-W2 — Implementation cannot self-review

The agent that applies or remediates a change must not complete or certify the independent-review gate.

A fresh review context is required.

## INV-W3 — Every remediation returns to review

A blocked independent review transitions to remediation and then back to a fresh independent review.

A remediation report never transitions directly to archival.

## INV-W4 — Review tries to falsify

Independent review should attempt to construct supported public counterexamples to the claimed invariants rather than merely confirm implementation intent.

## INV-W5 — Archive only after independent approval

An OpenSpec change may be archived only after:

- implementation/remediation tasks are supported by evidence;
- independent review returns an approval verdict;
- independent review is the sole remaining process gate;
- required configured validation passes.

## INV-W6 — Archive before final commit for completed pre-release changes

For the current pre-release workflow, a completed OpenSpec change should normally be:

```text
independently approved
→ review task completed
→ archived
→ post-archive validated
→ committed
```

unless an explicit workflow decision says otherwise.

## INV-W7 — Pre-release history stays clean

Do not add compatibility, migration, or deprecation machinery for APIs that were never released unless explicitly requested.

Active specs and implementation should describe the intended final design directly.

## INV-W8 — Archived history is historical

Archived OpenSpec changes should not be rewritten casually to match later redesigns.

New semantics belong in the current active change unless a narrowly scoped historical correction is explicitly justified.

---

# Escalation Rule

If an implementation or remediation agent concludes:

> "The smallest sound fix requires changing one of these invariants."

it must not make that change autonomously.

The steward should classify the situation as an architectural/design issue and route it to OpenSpec exploration/proposal or human review.

Tests passing is not sufficient justification for weakening an invariant.
