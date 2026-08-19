# Trading Project Overview

## Purpose

`trading` is a Scala 3 multi-module library for exact financial and trading quantities.

The foundation intentionally separates:

- exact mathematical quantities;
- discrete/grid-constrained quantities;
- static dimension expressions;
- runtime dimension identity;
- runtime grids and provenance.

The quantities foundation must remain useful independently of later domain models such as instruments, orders, executions, positions, P&L, accounts, ledgers, and wallets.

Those higher-level concepts belong in later modules rather than in the quantities foundation.

## Modules

The root SBT project is `trading` and is an unpublished aggregator.

The primary foundation module is:

- project: `quantities`
- artifact: `trading-quantities`
- primary package: `trading.quantity`

A separate downstream/boundary project exercises the published/public API from outside the quantities module.

The quantities production package layout includes:

- `trading.quantity`
- `trading.quantity.grid`
- `trading.quantity.refinement`
- `trading.quantity.runtime`
- `trading.quantity.algebra`

Avoid speculative package subdivision unless an actual body of code requires it.

## Exact Quantity Model

The central exact quantity type is conceptually:

```scala
opaque type Quantity[D <: Dimension] = Rational
```

A `Quantity[D]` is an arbitrary exact rational quantity in dimension `D`.

It does not imply membership in a discrete grid.

Exact rational arithmetic is the mathematical interior of the system.

## Grid Quantity Model

Discrete values are represented separately, conceptually:

```scala
opaque type GridQuantity[D <: Dimension, G] = BigInt
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

The exact public surface must always be refreshed from the repository before performing implementation or review work.

Do not resurrect older static evidence families merely because they appear in historical OpenSpec material or old compiler fixtures.

## Normalize

`Normalize[D]` is concerned with the static dimension language:

- validating a closed dimension expression;
- computing its canonical interpretation/output.

It is not runtime identity authority.

Current and future changes may reduce how visible `Normalize` is to ordinary client code, but such simplification must be proposed explicitly rather than silently introduced during unrelated remediation.

## SameDimension

`SameDimension[A,B]` represents dimensional equivalence.

Reflexive:

```scala
SameDimension[D,D]
```

is intentionally structural/type identity and must not be interpreted as proof that `D` is a valid canonical dimension expression.

Its public role may be reduced over time, especially for homogeneous arithmetic, but this must be handled as an explicit API change.

## Runtime Dimension Identity

`DimRef[D]` bridges static dimension identity to runtime `DimensionKey`.

For publicly inhabitable atom types, static/runtime authority must be unique:

> Two publicly constructible `DimRef[Atom[K]]` values with the same accepted static atom type must not denote contradictory runtime atom identities.

Runtime authority is unique for publicly inhabitable `DimRef` keys.

It is not required to be total over every statically normalizable key.

In particular:

> `Normalize[Atom[K]]` does not by itself imply that a public `DimRef[Atom[K]]` must exist.

This separation keeps static grammar validation and runtime witness authority distinct.

## Rates and Ratios

Rates are exact quantities with dimensional meaning derived from source/target dimensions.

Conceptually:

```scala
type Rate[From <: Dimension, To <: Dimension] =
  Quantity[Divide[To, From]]
```

A ratio is a dimensionless exact quantity.

Source dimension expressions may remain readable source expressions rather than forcing users to expose canonical internal `Dim[...]` forms.

## Runtime Identity and Evidence

Runtime `DimensionKey` arithmetic must agree with valid static dimension semantics.

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

## Runtime / Heterogeneous Values

Runtime registries and heterogeneous quantities exist to recover checked relationships when static types are not already known to agree.

Registry ownership and provenance are separate concerns from generic static dimension equivalence.

Do not weaken provenance checks in order to simplify static arithmetic.

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
- runtime `DimensionKey` agreement;
- generic client ergonomics;
- OpenSpec semantic conformance;
- formatting and Git/index cleanliness;
- full clean multi-module builds.

Implementation and remediation agents must not certify their own independent review gate.
