# Design: exact quantities inside, grid quantities at boundaries

## Context

Exact financial arithmetic has two distinct jobs. Mathematical calculations must preserve arbitrary rational results,
while accepted or persisted boundary values often need an integer coordinate on a named uniform grid. Treating the
grid coordinate as the universal quantity representation would make valid intermediate values unrepresentable;
treating every grid value as a stored rational would discard useful membership and coordinate guarantees.

The foundation therefore separates unrestricted exact values from grid-proven values. The repository is the
multi-module `trading` SBT build: its non-published root aggregates the `quantities` project and the downstream
adversarial project. The `quantities/` project publishes artifact `trading-quantities`.

## 1. Public type model

```scala
opaque type Quantity[D <: Dimension] = Rational

opaque type GridQuantity[D <: Dimension, G] = BigInt

type Ratio = Quantity[One]

type Rate[From <: Dimension, To <: Dimension] =
  Quantity[Divide[To, From]]
```

`D` says what is measured. `Quantity[D]` is any exact rational coefficient in that dimension. `GridQuantity[D, G]`
stores an integer coordinate and additionally proves membership in grid `G`. Representation choice is not part of any
public quantity type parameter.

## 2. Construction and trust boundary

`Quantity` constructs values from an explicit dimension witness and exact `Rational`, `BigInt`, decimal text, or finite
Java decimal. The coefficient accessor returns `Rational`. No public constructor accepts floating values or raw opaque
backing. Raw coefficient attachment and all operand-derived result constructors are private members of the opaque
owner. They are not exposed through package-qualified visibility, tokens, anchors, or generic trusted functions, so
downstream source declaring `package trading.quantity` receives no additional construction authority. Polymorphic zero is
safe because it contains no caller-supplied nonzero coefficient.

Grid quantities are constructed and inspected through a matching grid witness:

```scala
grid.fromCoordinate(coordinate)
grid.coordinate(value)
```

The grid witness is lexically owned with the opaque coordinate implementation. Raw `BigInt` attachment and inspection
remain private, while the sealed witness exposes only the matching path-dependent `G`. Same-grid arithmetic,
projection, quantization, exact embedding, quotient/remainder, and allocation compute results from operands and
witnesses rather than accepting caller-computed result coordinates. Polymorphic grid zero is safe for the same reason
as exact zero.

Guarantees apply to supported well-typed Scala 3 callers without casts, reflection, unsafe JVM access, hand-written
bytecode, foreign-language ABI calls, or constructor-bypassing deserialization.

Refinement representations, lawful closure construction, and the sign-capability constructor are owned by one lexical
source scope. No package-qualified visibility is used as the refinement trust boundary, and no unchecked refinement
constructor is public. Separate downstream compilation tests exercise `trading.quantity`, `trading.quantity.grid`,
`trading.quantity.refinement`, and their nesting behavior.

## 3. Exact scalar and dimensions

`trading.quantity.Rational` is normalized and arbitrary precision. Strict parsing accepts complete integer, decimal, and
fraction forms. Eager Java decimal conversion checks its scale bound before materializing powers of ten. No
authoritative `Float` or `Double` path exists.

Dimensions form a normalized free abelian group with `BigInt` exponents:

```scala
One
Atom[K]
Times[A, B]
Inverse[A]
Divide[A, B]
```

Runtime keys normalize expression shape. Atomic and runtime witnesses are generative; canonical equality recovers only
the supported `SameDimension.coerceQuantity` and `coerceGrid` operations. Reflexive `SameDimension[D, D]` denotes
structural type identity only; later static-normalization work validates canonical operation representations separately.

## 4. Uniform grids

A zero-anchored uniform grid with quantum `Positive[Rational]` contains exactly:

```text
{ n × q | n ∈ Z }
```

The coordinate is an opaque `BigInt`. This gives exact same-grid addition, subtraction, negation, integer scaling,
ordering, quotient/remainder, and allocation. A `0.03` grid is as fundamental as a reciprocal grid.

Dimensions and assets do not own one inherent grid. Protocols, listings, storage formats, settlement operations,
ledgers, and other contexts may select different grids, including equal-quantum grids with distinct identity.

## 5. Canonical embedding and explicit return to a grid

Every grid quantity embeds exactly:

```scala
val exact: Quantity[D] = value.asQuantity(grid)
```

There is no implicit global conversion. The reverse direction is checked or explicitly lossy:

```scala
exact.narrowExactlyTo(grid)
exact.quantizeTo(grid, policy)
```

Narrowing never rounds. Quantization returns the selected grid value and exact residual, satisfying:

```text
source = selected.asQuantity(grid) + residual
```

Grid-to-grid narrowing and quantization reuse these general exact operations after canonical embedding.

## 6. Arithmetic result rules

Exact quantities are closed under same-dimension addition, subtraction, and rational scaling. Multiplication combines
dimensions, checked quantity division divides dimensions, equal-dimension division returns `Ratio`, and exact whole
division returns another `Quantity[D]`.

Same-grid addition, subtraction, and integer scaling retain `GridQuantity[D, G]`. Operations that generally leave a
grid return `Quantity`: cross-grid addition or subtraction, grid multiplication, mixed exact/grid multiplication, grid
division, rate application, and exact whole division.

The representative calculation is:

```text
0.1 BTC × 60000.01 USD/BTC = 6000.001 USD
```

The result is `Quantity[USD]`; cent narrowing fails and cent quantization is explicit.

## 7. Rates and ratios

A mathematical `Rate[From, To]` has dimension `To / From`. Applying it to `Quantity[From]` or an explicitly embedded
source grid quantity returns `Quantity[To]`. Composition multiplies exact coefficients while presenting the clean
`Rate[A, C]` endpoint type. `Ratio` is the dimensionless exact quantity.

## 8. Scalar division and allocation

Three names prevent semantic ambiguity:

- `exactDivideBy` performs exact rational field division and returns `Quantity[D]`;
- `quotRemBy` performs Euclidean coordinate division on a specific grid;
- `allocateEvenly` returns ordered grid parts whose coordinates sum to the source.

The latter two are `trading.quantity.grid` extensions. They accept the matching grid witness and use only its public
`coordinate` and `fromCoordinate` authority; core `trading.quantity` does not publish or depend on their result types.

For positive divisors, Euclidean remainders are nonnegative and smaller than the divisor, including for negative source
coordinates. Allocation distributes at most one extra quantum according to explicit `RemainderOrder`.

## 9. Refinements

The entire public sign-refinement vocabulary is:

```scala
NonNegative[A]
NonZero[A]
Positive[A]
```

One final, privately constructible `Sign[A]` observes the exact sign of the supported carriers: `Int`, `BigInt`,
`Rational`, `Quantity[D]`, and `GridQuantity[D, G]`. Only the library supplies instances, so downstream source cannot
provide a lying sign implementation. The generic `from` methods return `ExpectedNonNegative`, `ExpectedNonZero`, or
`ExpectedPositive`.

The refinements are separate zero-cost opaque views in order to avoid ambiguous extension selection between refined
and raw quantity operations. `Positive[A]` exposes explicit `asNonNegative` and `asNonZero` weakenings, and every
refinement exposes the same `unrefined` operation. These operations reuse the representation directly: they perform no
predicate check, allocation, `Either`, or implicit conversion.

Scalar terms are aliases, not independent carriers:

```scala
type PositiveWhole = Positive[BigInt]
type NonZeroWhole = NonZero[BigInt]
type PositiveInt = Positive[Int]
type PositiveRational = Positive[Rational]
```

Facade constructors delegate to the generic checks. `PositiveInt.toPositiveWhole` is the one operation-specific,
sign-preserving widening. Invalid rational syntax and a zero rational denominator remain distinct from a failed
positive predicate.

Quantity division consumes `NonZero[Quantity[D]]`. Grid divisors first use canonical exact embedding and then the same
generic check. Closure determines result refinements: positive exact division remains positive; positive grid quotient
and positive quantization may weaken to nonnegative; nonzero grid quotient may become zero; subtraction may become
unrestricted. Lexically trusted closed operations construct their results without revalidation or proof-recovery
`.toOption.get` calls.

Runtime integer values remain runtime values. This design explicitly rejects type-level naturals, singleton divisors,
and singleton allocation counts for runtime financial values.

## 10. Exact-only algebra

Typelevel Algebra remains the standard ring vocabulary. Its historical standalone repository being archived reflects
source development moving into the Cats monorepo; the published Algebra API and artifact remain distinct from
`cats-core`. The production build therefore retains Algebra directly, while law and test integration remains outside
the production classpath.

The project-owned hierarchy is:

```text
CommutativeRing[F]
        ↓
ExactScalarField[F]

AdditiveCommutativeGroup[V]
        ↓
 LeftModule[V, S]
        ↓
 VectorSpace[V, F]
```

`ExactScalarField[Rational]` is the one supported commutative-ring instance and adds total reciprocal after
`NonZero[Rational]` evidence. `VectorSpace[Quantity[D], Rational]` is the one strongest quantity instance and supplies
its left module and additive commutative group. `LeftModule[GridQuantity[D, G], BigInt]` is the one strongest grid
instance and uses Algebra's standard `Ring[BigInt]`; no parallel grid group implementation exists. The instances
delegate to primitive arithmetic, and the primitive operations never summon the instances that delegate to them.

`DimensionKey` exposes its runtime free-abelian-group structure as a multiplicative commutative group with canonical
equality. `NonZero[Rational]` exposes exact multiplicative identity, multiplication, and reciprocal through lexical
closure without revalidation. Cats Kernel exact orders for `Rational`, `Quantity[D]`, and `GridQuantity[D, G]` delegate
to primitive comparisons and never act as refinement authority. Nonnegative exact and grid quantities expose additive
commutative monoids, while positive exact and grid quantities expose additive commutative semigroups. Nonzero values
have no additive structure.

Rates have category-shaped composition laws, but no production Cats `Category[Rate]` is defined: Cats requires an
unconstrained `id[A]`, whereas a legitimate rate identity requires `A <: Dimension` and an authoritative `DimRef[A]`.
`Rate.identity(dimension)` preserves that construction boundary. There is no public Algebra `Field[Rational]`,
`Numeric[Quantity[D]]`, `Ring[Quantity[D]]`, or `Ring[GridQuantity[D, G]]`, and no algebra instance reconstructs grid
coordinates or changes direct operator semantics.

## 11. Runtime identity and evidence

`QuantityRegistry` owns `RegisteredDimensionRef` and `RegisteredGridRef`. Full registered grid identity is:

```text
(canonical DimensionKey, GridId, GridVersion)
```

Definitions are immutable per full identity. A dimension-local `GridKey` is not full identity, equal quantum does not
imply equal grid, and a plain generative grid has no registered provenance. Generic `SameGrid` evidence checks ordinary
canonical dimension, grid ID, version, and quantum compatibility, and may therefore be recovered for matching
generative grids without registry provenance. `RuntimeEvidence.sameGrid` is the registry-aware operation: it first
checks common registry ownership and rejects foreign witnesses, then delegates to the ordinary `SameGrid` checks.
Concrete registered dimension, grid, asset, and general-dimension implementations are private classes nested inside
each registry instance. Only successful registry operations create them; neither top-level privacy nor
package-qualified privacy supplies provenance. The registered witness preserves its owning registry, canonical
dimension key, ID, version, and immutable exact positive quantum.

Heterogeneous grid values use `ResolvedAssetGridQuantity` and `ResolvedGridQuantity`. Same-grid operations recover grid
evidence; cross-grid exact operations recover dimension evidence, embed explicitly, and return
`ResolvedExactQuantity`.

## 12. Logical persistence

`PackedAssetGridQuantity` stores asset ID, expected canonical dimension, grid ID, grid version, and coordinate.
`PackedGridQuantity` stores canonical dimension, grid ID, grid version, and coordinate. Decoding resolves and verifies
the dimension before the dimension-scoped grid, then returns `ResolvedAssetGridQuantity` or `ResolvedGridQuantity`.
Packing accepts only a registry-produced `RegisteredGridRef`. A plain grid, or an equal-looking grid that substitutes a
different quantum under the same dimension, ID, and version, cannot be promoted to registered provenance. In
particular, coordinate `42` on canonical quantum `1/100` decodes only as exact `21/50`; a `7/13` quantum cannot be
attached to that registered identity.

These are logical in-memory records, not a stable wire schema. A future wire schema needs separate record-version
dispatch; `GridVersion` continues to mean the immutable grid definition and coordinate interpretation.

Arbitrary `Quantity[D]` is not packed here. That requires explicit numerator, denominator, dimension identity, and
schema design. Exact results must first be narrowed or quantized to a registered grid before using the grid-packed
records.

Java serialization is unsupported and fails closed through the common project-owned mechanism for invariant-bearing
public result and error records, nominal and logical records, and all three dependent carriers:
`ResolvedAssetGridQuantity`, `ResolvedGridQuantity`, and `ResolvedExactQuantity`.

## 13. Package ownership

Production code remains limited to:

```text
trading.quantity
trading.quantity.grid
trading.quantity.refinement
trading.quantity.runtime
trading.quantity.algebra
```

The root project contains no production Scala source. All ordinary production and test sources belong to the
`quantities/` module; `adversarial-boundary` depends directly on that module. Business and policy abstractions are
deferred.

## 14. Validation strategy

The foundation is validated with standard laws for every supported production ring, group, monoid, semigroup, and
exact order, plus reusable project laws for refinement-aware reciprocal, modules, vector spaces, graded quantity
multiplication, rate composition, rational canonicality, dimension normalization, embedding, narrowing, quantization,
Euclidean division, allocation, and refinements. Nontrivial expected results use independent rational, `BigInt`, map,
conservation, or coordinate-neighbor models rather than calling the operation under test again.

Focused unit and property regressions remain for invalid syntax, extreme bounds, negative ties, non-reciprocal grids,
construction boundaries, provenance, restart remapping, persistence, and serialization. Compile-time checks, a
downstream same-package adversarial project covering core, grid, refinement, and runtime package declarations,
counterfeit-quantum regression coverage, external compiler probes, generated API documentation inspection, strict
OpenSpec validation, formatting, dependency and classpath inspection, and staged-diff checks complete automated
validation. A fresh independent reviewer is still required before the initial commit.

## Risks and trade-offs

- Path-dependent runtime types require dependent reconstruction packages and checked evidence.
- Exact rational arithmetic allocates more than bounded integer arithmetic; correctness takes priority over speculative
  optimization.
- Explicit grid witnesses make cross-grid operations more verbose, but prevent hidden projection and identity loss.
- Scala opaque types secure the supported source boundary, not hostile foreign JVM access.
- Logical packed records could be mistaken for wire formats, so schema deferral is explicit.
