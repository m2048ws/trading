## MODIFIED Requirements

### Requirement: Compile-time dimension equivalence
`SameDimension[A, B]` SHALL be derivable at compile time when normalization of the statically visible closed dimension
expressions `A` and `B` produces canonical `Canonical` entries with the same singleton keys and `Int` exponents modulo tuple
permutation. Static derivation SHALL require no runtime `DimRef`, `DimKey`, or total ordering over singleton keys.
The evidence SHALL remain a restricted capability whose construction is unavailable to supported downstream code; it
SHALL authorize controlled explicit quantity- and grid-dimension alignment and equivalence-aware comparison but SHALL
NOT expose unrestricted Scala type equality, a global implicit conversion between arbitrary quantity types, or implicit
alignment inside homogeneous arithmetic.

The public value-level alignment operation SHALL be named `alignTo`. Given `SameDimension[D, E]`, it SHALL retag
`Quantity[D]` as `Quantity[E]` and `GridQuantity[D, G]` as `GridQuantity[E, G]` without changing the exact coefficient,
grid identity, or coordinate. The former `asDimension` operation SHALL no longer be exposed. Concrete arithmetic whose
canonical result is statically determined SHALL expose that result directly; callers SHALL not need a second alignment
evidence family or a routine `alignTo` repair merely to select the named atom, rate endpoint, ratio, or canonical
composite that the operation computes. Explicit `SameDimension`-checked alignment SHALL remain available for checked
runtime equality and for intentionally selecting between distinct but equivalent tuple orders. Reflexive
`SameDimension[D, D]` SHALL mean Scala type identity only; every non-reflexive proof and every use as a canonical
operation result SHALL validate the complete closed representation.

#### Scenario: Align commuted canonical dimensions
- **WHEN** two canonical `Canonical` values contain the same singleton-key powers in different tuple orders
- **THEN** compile-time `SameDimension` evidence is derivable without assigning a total order to their keys

#### Scenario: Align commuted products
- **WHEN** two concrete products normalize to the same singleton-key powers in different tuple orders
- **THEN** compile-time `SameDimension` evidence is derivable without assigning a total order to their keys

#### Scenario: Use equivalence in addition
- **WHEN** two quantities have equivalent canonical dimensions in different tuple orders but different Scala dimension
  types
- **THEN** direct addition and subtraction do not compile; the right operand must first use
  `SameDimension[Right, Left]` to `alignTo[Left]`, after which exact-type arithmetic returns `Quantity[Left]`

#### Scenario: Use evidence in addition
- **WHEN** a generic caller selects the left dimension as the result type for equivalent `Quantity[Left]` and
  `Quantity[Right]` operands
- **THEN** it forwards `SameDimension[Right, Left]` to `right.alignTo[Left]` before exact-type addition or subtraction,
  and the arithmetic operation itself does not consume `SameDimension`

#### Scenario: Require explicit alignment before addition
- **WHEN** two quantities have equivalent canonical dimensions represented by different Scala dimension types
- **THEN** direct addition and subtraction do not compile until one operand is explicitly aligned to the other operand's
  dimension type

#### Scenario: Align an exact quantity
- **WHEN** `SameDimension[D, E]` is available and a caller invokes `alignTo[E]` on `Quantity[D]`
- **THEN** the result is `Quantity[E]` with exactly the original coefficient

#### Scenario: Align a grid quantity
- **WHEN** `SameDimension[D, E]` is available and a caller invokes `alignTo[E]` on `GridQuantity[D, G]`
- **THEN** the result is `GridQuantity[E, G]` with the original grid type and coordinate

#### Scenario: Expose a concrete economic result directly
- **WHEN** concrete quantity or rate arithmetic cancels all intermediate source dimensions and leaves a named target
  dimension
- **THEN** the result is assignable to that target type without caller-supplied alignment or retagging

#### Scenario: Select an economic result type explicitly
- **WHEN** a caller intentionally selects an equivalent canonical composite whose tuple order differs from the computed
  result
- **THEN** `alignTo` exposes the selected type using `SameDimension` without runtime comparison or an unchecked public
  cast

#### Scenario: Reject unequal static dimensions
- **WHEN** normalized dimensions differ by a singleton key or exponent
- **THEN** `SameDimension` is not derivable and `alignTo` cannot cross between the dimension types

#### Scenario: Recover checked runtime equivalence
- **WHEN** two opaque runtime witnesses have equal authoritative `DimKey` values but distinct singleton-key types
- **THEN** successful runtime comparison may issue scoped `SameDimension` evidence for explicit `alignTo`

#### Scenario: Derive evidence from downstream code
- **WHEN** supported downstream Scala requests `SameDimension` for equivalent commuted canonical products with compiler
  warnings treated as errors
- **THEN** the evidence compiles without inaccessible-member diagnostics or access to implementation-only proof rules

#### Scenario: Keep reflexivity separate from canonical certification
- **WHEN** a malformed `Canonical` representation requests `SameDimension[D, D]`
- **THEN** reflexive identity and `alignTo[D]` MAY be available, but normalization and arithmetic SHALL reject the
  malformed representation

### Requirement: Dimension-safe additive and multiplicative arithmetic
Addition and subtraction SHALL accept only quantities with the exact same Scala dimension type `D`, SHALL require
`Normalize[D]`, and SHALL return `Quantity[D]`. They SHALL NOT consume `SameDimension` to align a right operand whose
static dimension type differs from the left operand's type. Multiplication by `Rational` SHALL preserve the quantity's
dimension. Multiplying `Quantity[A]` by `Quantity[B]` SHALL use the single normalization operation and return an exact
quantity in a canonical `Canonical`: nested products SHALL be flattened, inverse powers negated, equal singleton keys
combined, zero powers removed, and every surviving key stored exactly once with a nonzero `Int` exponent. Entry order
MAY follow operand order and SHALL NOT affect dimension equivalence.

For fully concrete inputs, the inferred public result SHALL expose the complete canonical dimension without a
specialized product evidence type or caller-visible alignment step. Generic code SHALL state and forward one contextual
`Normalize` computation for the complete multiplication, inversion, or quotient expression when its inputs are
unresolved. Instantiating such generic code with concrete dimensions SHALL agree with normalizing the corresponding
concrete expression directly. Hidden decompositions of runtime-resolved opaque dimensions SHALL remain unavailable to
static cancellation until checked runtime equivalence is supplied.

Every public operation that performs arithmetic and preserves a dimension parameter `D` SHALL require `Normalize[D]`.
This includes zero identities, homogeneous exact addition and subtraction, exact scalar multiplication and division,
grid closed arithmetic, allocation and quantization, arithmetic grid conversion and projection,
refinement-preserving wrappers, and optional arithmetic algebra instances. The requirement SHALL be `Normalize[D]`,
not `Normalize.Aux[D, D]`, because a valid quantity dimension may be a noncanonical source expression such as
`Divide[T, F]` in `Rate[F, T]`. Operations already requiring `Normalize` of their complete dimension-changing expression
SHALL not add redundant operand normalization evidence. Explicit alignment, equivalence-aware comparison, equality,
ordering, sign inspection, and authoritative witness-owned construction SHALL remain separate from arithmetic
validation.

#### Scenario: Add and subtract exact quantities
- **WHEN** two exact USD quantities are added or subtracted
- **THEN** both results are exact `Quantity[USD]` values without requiring `SameDimension` evidence

#### Scenario: Write generic homogeneous arithmetic
- **WHEN** a generic operation accepts two `Quantity[D]` operands and forwards `Normalize[D]`
- **THEN** it can add or subtract them without declaring dimensional-equivalence vocabulary

#### Scenario: Reject implicit cross-spelling arithmetic
- **WHEN** `Quantity[A]` and `Quantity[B]` have different static dimension types even though `SameDimension[A, B]` is
  available
- **THEN** direct addition and subtraction do not compile, and the caller must explicitly align one operand to the
  chosen result type

#### Scenario: Reject malformed dimension-preserving arithmetic
- **WHEN** a zero-power or otherwise malformed `Canonical` is used with quantity or grid zero, addition, subtraction, scalar
  arithmetic, exact scalar division, allocation, quantization, refined arithmetic, or an arithmetic algebra instance
- **THEN** the boundary cannot obtain `Normalize[D]` and compilation fails even though reflexive `SameDimension[D, D]`
  remains identity-only

#### Scenario: Preserve source-expression dimensions
- **WHEN** a valid dimension-preserving operation is applied to `Quantity[Divide[T, F]]` or generic `Quantity[D]` code
  that forwards `Normalize[D]`
- **THEN** the operation compiles and retains the original dimension spelling without requiring `Normalize.Aux[D, D]`

#### Scenario: Multiply concrete dimensions
- **WHEN** exact quantities in concrete dimensions `A` and `B` are multiplied
- **THEN** the exact result exposes their validated canonical product directly

#### Scenario: Multiply dimensions
- **WHEN** exact quantities in concrete dimensions `A` and `B` are multiplied
- **THEN** the exact result's public dimension is their validated canonical `Canonical` product

#### Scenario: Cancel a price denominator
- **WHEN** `Quantity[Position]` is multiplied by a quantity in `Settlement / Position`
- **THEN** the result is directly assignable to `Quantity[Settlement]` without special rate application or alignment

#### Scenario: Retain a multi-key product
- **WHEN** multiplication leaves nonzero powers for more than one singleton key
- **THEN** the result contains one canonical entry for each surviving key and no reducible expression history

#### Scenario: Retain a multi-atom product
- **WHEN** multiplication leaves nonzero powers for more than one singleton-key atom
- **THEN** the result type contains one canonical entry for each surviving atom and no reducible multiplication or
  inversion history

#### Scenario: Preserve commutative equivalence
- **WHEN** quantities in dimensions `A` and `B` are multiplied in opposite operand orders
- **THEN** their result dimensions admit compile-time `SameDimension` evidence

#### Scenario: Use one generic normalization context
- **WHEN** a generic function multiplies, divides, or inverts quantities whose dimensions are abstract
- **THEN** its signature requires only the corresponding complete `Normalize` evidence and forwards that evidence to the
  operation

#### Scenario: Specialize generic multiplication to one dimension
- **WHEN** a generic multiplication operation with contextual `Normalize` evidence is instantiated with both dimension
  parameters equal to `D`
- **THEN** its result contains one `D` key with exponent `2` and agrees with direct concrete multiplication in `D`

#### Scenario: Specialize generic inversion to a visible product
- **WHEN** a generic inversion operation with contextual `Normalize` evidence is instantiated with a concrete product
  `Times[A, B]`
- **THEN** its result flattens to canonical powers `A` to `-1` and `B` to `-1`, matching direct inversion

#### Scenario: Reject late alias specialization without contextual evidence
- **WHEN** a generic method requests normalization for aliases of unresolved dimensions before a caller later supplies
  equal concrete arguments
- **THEN** automatic derivation is rejected at the generic definition and the method must accept the complete contextual
  `Normalize` evidence

#### Scenario: Reject refinable-member inversion without contextual evidence
- **WHEN** inversion is requested for an abstract member rooted in a parameter or refinable prefix
- **THEN** automatic derivation rejects the unresolved structure instead of freezing it as one singleton key, and generic
  code must accept contextual `Normalize` evidence

#### Scenario: Reject local aliases over dependent parameters
- **WHEN** generic arithmetic defines aliases over parameter-dependent dimension members before requesting normalization
- **THEN** the aliases do not hide the unresolved roots and contextual `Normalize` evidence remains required

#### Scenario: Reject stable local transport of unresolved evidence
- **WHEN** generic normalization evidence or a parameter-dependent witness is rebound through stable locals, singleton
  ascriptions, or `Normalize.Aux` refinements while dependencies remain unresolved
- **THEN** rebinding does not permit automatic derivation or an invalid duplicate, uncancelled, or nested representation

#### Scenario: Preserve endpoint-depth coherence
- **WHEN** transparent aliases or exact `Normalize.Aux` refinements successively expose a fully concrete operation output
- **THEN** every use reaches the same canonical `Canonical` and runtime `DimKey` as direct use of the concrete endpoint

#### Scenario: Canonicalize definitionally equal aliases coherently
- **WHEN** `holder.D` is a transparent alias for `Times[A, B]`
- **THEN** normalization produces the same canonical output for `holder.D` and `Times[A, B]`, including duplicate-key
  combination, cancellation, inversion, and runtime-key agreement

#### Scenario: Canonicalize definitionally equal annotated inputs coherently
- **WHEN** a stable atom, canonical `Canonical`, reducible expression, or transparent alias differs from another input only by
  annotations
- **THEN** normalization produces the same unannotated canonical output and agrees with runtime `DimKey`
  multiplication and inversion

#### Scenario: Keep runtime-hidden structure opaque
- **WHEN** an opaque runtime dimension's key contains a factor that would cancel a separate static atom
- **THEN** automatic static normalization does not inspect the hidden key, and cancellation requires checked runtime
  equivalence
