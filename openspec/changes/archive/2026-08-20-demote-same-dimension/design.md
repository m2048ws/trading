## Context

See `proposal.md` for motivation. The current public `Quantity` `+` and `-` operations introduce a second dimension
parameter and require `Normalize` for both operands plus `SameDimension` between them. `GridQuantity` same-grid
arithmetic and all dimension-preserving refinement arithmetic are already exact-type operations, but cross-grid
`addExact` and `subtractExact` repeat the heterogeneous pattern. Checked runtime addition currently recovers
`SameDimension`, coerces one exact embedding, and then calls the heterogeneous public addition signature.

This change follows `freeze-static-dimension-authority`. Its design depends on that change's separation of static
validity (`Normalize`), runtime inhabitation (`DimRef`), equivalence (`SameDimension`), and exact values (`Quantity`). In
particular, neither possession of a quantity nor reflexive equivalence makes a malformed dimension valid for arithmetic.

## Goals / Non-Goals

**Goals:**

- Give every homogeneous additive API one static dimension parameter and one corresponding `Normalize` requirement.
- Make a distinct static spelling visible at an explicit `alignTo` boundary before arithmetic.
- Preserve one `SameDimension` proof family for compile-time equivalence and checked runtime recovery.
- Keep equivalence-aware grid comparison available without treating comparison as additive arithmetic.
- Make evidence direction and the migration of generic clients mechanically clear.

**Non-Goals:**

- Do not alter static normalization, proof derivation, expression-result typing, runtime keys, registries, or grid
  identity.
- Do not require `Normalize` merely to perform reflexive alignment, and do not turn `SameDimension` into validity
  evidence.
- Do not add implicit conversions, automatic operand alignment, a symmetric/reversed proof API, or a second alignment
  evidence family.
- Do not change the numerical representation or runtime result types of heterogeneous operations.

## Decisions

### 1. Homogeneous quantity arithmetic has one dimension parameter

The public shape becomes:

```scala
def +(that: Quantity[D])(using Normalize[D]): Quantity[D]
def -(that: Quantity[D])(using Normalize[D]): Quantity[D]
```

The private coefficient helpers follow the same shape. The right operand cannot introduce an `E`, so neither
`Normalize[E]` nor `SameDimension[D, E]` participates in overload resolution. This makes Scala type identity the
ordinary homogeneous boundary while retaining `Normalize[D]` as the independent validity check required by the frozen
authority model.

The alternative of retaining a generic `E` and conditionally preferring exact equality was rejected: it would preserve
the hidden alignment behavior and keep generic APIs dependent on equivalence vocabulary. Removing `Normalize` together
with `SameDimension` was also rejected because a `Quantity[D]` does not certify that `D` is a valid closed expression.

### 2. `alignTo` is the sole value-level name for intentional retagging

Both exact and grid quantities expose:

```scala
def alignTo[Target <: Dim](using SameDimension[D, Target]): Quantity[Target]
def alignTo[Target <: Dim](using SameDimension[D, Target]): GridQuantity[Target, G]
```

The implementation remains zero-allocation phantom retagging backed by the restricted proof. `asDimension` is removed
without an alias so the breaking migration converges on one boundary name. Existing controlled coercion primitives on
`SameDimension` may continue to back these extensions; this change does not redesign proof construction or recovery.

`alignTo` does not require `Normalize`. Non-reflexive statically derived evidence already validates its closed operands,
while reflexive evidence deliberately remains available for any identical Scala type. Requiring normalization here
would merge equivalence with validity; arithmetic after alignment still independently requires `Normalize` for the
selected type.

An implicit conversion was rejected because it would make the cross-spelling transition invisible again. Keeping
`asDimension` as an alias was rejected because the chosen clean break would leave two names for the same authority
boundary.

### 3. Evidence is oriented from the value being aligned to its target

`value.alignTo[Target]` requires `SameDimension[Source, Target]`. Generic helpers shall request evidence in that direction
rather than adding `SameDimension.reverse`. For example, a function returning the left operand's type requests
`SameDimension[B, A]` and evaluates `a + b.alignTo[A]`.

This orientation matches checked heterogeneous addition: request runtime evidence from the right witness to the left
witness, align the right embedded quantity to the left dimension, and return a value indexed by the left authoritative
witness. Adding reversal was rejected as unnecessary API growth; callers can request or recover evidence in the
direction of their chosen result type.

### 4. Cross-grid addition is homogeneous in dimension, not in grid

`GridQuantity.addExact` and `subtractExact` keep distinct grid parameters but remove the second dimension parameter:

```scala
def addExact[H](
  that: GridQuantity[D, H],
  leftGrid: Grid[D, G],
  rightGrid: Grid[D, H]
)(using Normalize[D]): Quantity[D]
```

`subtractExact` has the analogous shape. Same-grid `+` and `-`, refinement wrappers, and algebra instances already use
one exact `D`; they require verification and regression coverage rather than a new semantic design.

These signatures apply only when both values and both grid witnesses already share that exact `D`. `GridQuantity.alignTo`
retags the value's phantom dimension but deliberately does not turn its source `GridRef[E, H]` into `GridRef[D, H]`.
For naturally cross-spelled grids, callers therefore embed each value through its own original grid witness, align one
resulting exact `Quantity` to the selected result dimension, and use ordinary exact-type `Quantity` addition or
subtraction. `SameDimension` does not manufacture `DimRef` or `GridRef` authority, and value-only grid alignment is not
presented as a way to make `addExact` or `subtractExact` accept a source-typed grid witness.

`exactlyEquals` and `compareExact` deliberately retain their second dimension parameter and
`SameDimension[D, E]`. They are explicit advanced comparisons across exact embeddings, produce no quantity, and remain
outside arithmetic validity. Multiplication and division continue to use normalization of their complete result
expressions and are unchanged.

The alternative of forcing callers to align solely for comparison was rejected because comparison is already an
explicit equivalence-aware API and does not conceal a result-type transition inside homogeneous arithmetic.

### 5. Runtime recovery aligns before invoking ordinary arithmetic

Runtime heterogeneous addition retains its checked failure behavior and dependent result type. After recovering
`SameDimension[right.D, left.D]`, it embeds both grid values, invokes `rightExact.alignTo[left.D]` with the recovered
proof, and calls the exact-type `Quantity` addition operation. Runtime mismatch still returns the existing structured
error before any alignment or arithmetic.

This keeps runtime recovery as a trusted source of the same restricted proof while ensuring the arithmetic primitive
itself has no runtime/static special case. Direct proof coercion inside heterogeneous arithmetic was rejected as the
documented flow because `alignTo` should remain the visible transition even within generic implementation code.

### 6. Compiler-boundary tests define the breaking surface

Positive compile-time coverage will demonstrate exact-type `Quantity`, grid, refinement, and algebra arithmetic with
only `Normalize[D]`. Negative fixtures will demonstrate that equivalent but differently spelled operands cannot be
added directly. Companion positive fixtures will align exact quantities first; naturally cross-spelled grid fixtures
will embed through their respective original grid witnesses, align an exact embedding, and then use `Quantity`
arithmetic. They will not simulate support by constructing both grid witnesses in one dimension, retagging only a value
away from that dimension, and retagging it back. Existing runtime recovery tests will be rewritten to align the recovered
value explicitly. All `asDimension` examples and fixtures will migrate to `alignTo`; a boundary fixture will verify that
`asDimension` is absent.

Tests for malformed dimensions will continue to separate reflexive `SameDimension[D, D]` from unavailable
`Normalize[D]`, so the simpler signatures cannot accidentally grant arithmetic through type identity alone.

## Risks / Trade-offs

- [Source and JVM binary incompatibility] Existing compiled or source clients use removed signatures and the old
  alignment name → Mark the release as breaking, provide direct migration examples, and do not claim binary
  compatibility.
- [Evidence-direction mistakes] Generic and path-dependent runtime code may request `SameDimension` in the opposite
  direction → Express the selected result type first, recover `Source -> Target` evidence, and add compile-time and
  runtime tests for that orientation.
- [False confidence from simpler signatures] Exact type equality could appear to make normalization unnecessary → Keep
  `Normalize[D]` on every arithmetic boundary and retain malformed-dimension negative fixtures.
- [Accidental loss of advanced comparison] A broad search-and-remove of `SameDimension` could alter `exactlyEquals` or
  `compareExact` → Test those APIs explicitly with distinct but equivalent dimension spellings.
- [Prerequisite drift] Implementing this change before the authority freeze may reopen questions that this design treats
  as settled → Complete or reconcile `freeze-static-dimension-authority` first and validate both change contracts before
  implementation.

## Migration Plan

1. Complete or reconcile the `freeze-static-dimension-authority` prerequisite so arithmetic validity consistently
   requires `Normalize`.
2. Introduce `alignTo`, remove `asDimension`, and narrow `Quantity` and cross-grid additive signatures in one source
   change so no intermediate implicit-alignment surface remains.
3. Update runtime heterogeneous arithmetic, examples, laws, and compiler-boundary fixtures to align explicitly.
4. Run formatting, module tests, adversarial boundary tests, the full SBT test suite, and strict OpenSpec validation.

There is no persisted-data migration. Rollback is a source revert to the prior public signatures and name; artifacts
compiled against either API generation must be rebuilt against the selected version.
