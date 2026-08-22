## MODIFIED Requirements

### Requirement: Compile-time dimension equivalence
`SameDimension[A, B]` SHALL be derivable at compile time when normalization of the statically visible closed dimension
expressions `A` and `B` produces canonical `Dim` entries with the same singleton keys and `Int` exponents modulo tuple
permutation. Static derivation SHALL require no runtime `DimRef`, `DimensionKey`, or total ordering over singleton keys.
The evidence SHALL remain a restricted capability whose construction is unavailable to supported downstream code; it
SHALL authorize controlled quantity- and grid-dimension coercion but SHALL NOT expose unrestricted Scala type equality or
a global implicit conversion between arbitrary quantity types.

Concrete arithmetic whose canonical result is statically determined SHALL expose that result directly. Callers SHALL
not need a second alignment evidence family or a routine `asDimension` repair merely to select the named atom, rate
endpoint, ratio, or canonical composite that the operation computes. Explicit `SameDimension`-checked retagging SHALL
remain available for checked runtime equality and for intentionally selecting between distinct but equivalent tuple
orders. Reflexive `SameDimension[D, D]` SHALL mean Scala type identity only; every non-reflexive proof and every use as a
canonical operation result SHALL validate the complete closed representation.

#### Scenario: Align commuted canonical dimensions
- **WHEN** two canonical `Dim` values contain the same singleton-key powers in different tuple orders
- **THEN** compile-time `SameDimension` evidence is derivable without assigning a total order to their keys

#### Scenario: Align commuted products
- **WHEN** two concrete products normalize to the same singleton-key powers in different tuple orders
- **THEN** compile-time `SameDimension` evidence is derivable without assigning a total order to their keys

#### Scenario: Use equivalence in addition
- **WHEN** two quantities have equivalent canonical dimensions in different tuple orders
- **THEN** addition and subtraction accept statically derived evidence and retain the left operand's dimension type

#### Scenario: Use evidence in addition
- **WHEN** two quantities have equivalent normalized `Dim` entries in different tuple orders
- **THEN** addition and subtraction accept statically derived `SameDimension` evidence and retain the left operand's
  dimension type

#### Scenario: Expose a concrete economic result directly
- **WHEN** concrete quantity or rate arithmetic cancels all intermediate source dimensions and leaves a named target
  dimension
- **THEN** the result is assignable to that target type without caller-supplied alignment or retagging

#### Scenario: Select an economic result type explicitly
- **WHEN** a caller intentionally selects an equivalent canonical composite whose tuple order differs from the computed
  result
- **THEN** explicit `SameDimension`-checked retagging exposes the selected type without runtime comparison or an
  unchecked cast

#### Scenario: Reject unequal static dimensions
- **WHEN** normalized dimensions differ by a singleton key or exponent
- **THEN** `SameDimension` is not derivable and evidence-requiring arithmetic does not compile

#### Scenario: Recover checked runtime equivalence
- **WHEN** two opaque runtime witnesses have equal authoritative `DimensionKey` values but distinct singleton-key types
- **THEN** successful runtime comparison may issue scoped `SameDimension` evidence for explicit controlled retagging

#### Scenario: Derive evidence from downstream code
- **WHEN** supported downstream Scala requests `SameDimension` for equivalent commuted canonical products with compiler
  warnings treated as errors
- **THEN** the evidence compiles without inaccessible-member diagnostics or access to implementation-only proof rules

#### Scenario: Keep reflexivity separate from canonical certification
- **WHEN** a malformed `Dim` representation requests `SameDimension[D, D]`
- **THEN** reflexive identity MAY be available, but normalization and arithmetic SHALL reject the malformed representation

### Requirement: Atomic and canonical static derivation
The public static dimension language SHALL be closed over canonical `Dim[Entries]`, source expressions `Times[A, B]` and
`Inverse[A]`, and `Divide[A, B]` as quotient syntax. A canonical entry SHALL be
`Power[Key, Exponent]`, where `Key <: Singleton` identifies an atom and `Exponent <: Int` is a singleton integer literal.
`Atom[Key]` SHALL denote `Dim[Power[Key, 1] *: EmptyTuple]`, and `One` SHALL denote `Dim[EmptyTuple]`. An arbitrary subtype
of `Dimension` SHALL NOT silently become a new atomic identity.

A canonical `Dim` SHALL contain each singleton key at most once and SHALL store only nonzero literal exponents. Tuple
order SHALL not carry mathematical meaning. A zero exponent, duplicate key, non-`Power` entry, abstract or nonliteral
exponent, unresolved tuple, or key that is not a stable singleton type SHALL make a claimed canonical representation
invalid. Floating-point, decimal, and rational exponent types SHALL not be part of the static dimension language.

`Key <: Singleton` SHALL be necessary but SHALL NOT by itself certify a canonical key. After annotations and transparent
aliases are exposed, automatic normalization SHALL accept only concrete literal `ConstantType` identities, concrete
stable term/module singleton references, and supported generative `ThisType` identities. It SHALL reject `Singleton`,
`Nothing`, `Null`, broad intersections and unions, refinements, bounds, unresolved match or lambda structures,
ordinary non-term `TypeRef` values, abstract/deferred/parameter keys, and unknown wrappers. Rejection of `Nothing` and
`Null` SHALL follow this structural whitelist rather than a permissive subtype test.

`Normalize[D]` SHALL be the sole public associated-output evidence for reducing a dimension expression to a canonical
`Dim`. Its automatic derivation SHALL parse only the closed grammar, expose definitionally transparent aliases and
annotations, combine equal keys, remove zero results, validate the complete output, and then issue final evidence as one
trusted operation. Public `NormalizedPowers`, `DimensionProduct`, `DimensionInverse`, `DimensionQuotient`,
`DimensionAlignment`, recursive merge rules, guards, and caller-constructible proof tokens SHALL NOT be available.

Automatic derivation SHALL reject unresolved generic dimensions, refinements, intersections, unions, unresolved match
types, and other structures outside the closed grammar with an actionable diagnostic rather than treating them as
atoms. Generic code SHALL accept and forward the required final `Normalize` evidence. A runtime-resolved opaque
dimension witness SHALL expose a concrete atom alias keyed by its own stable singleton identity; its hidden runtime
decomposition SHALL not be guessed statically.

A public literal atom constructor SHALL require `Normalize[Atom[K]]` in addition to `ValueOf[K]`, so a caller-created
`ValueOf[String & Singleton]` cannot select one broad static key for different literal values. A public nominal-object
atom constructor SHALL take one authority-bearing singleton key whose runtime `AtomId` is fixed by that key and SHALL
bind its result key to the supplied stable value's singleton type. It SHALL NOT accept a caller-selected key supertype or
the static singleton key and runtime identifier as independent arguments. At every public static atom construction
boundary, one accepted `Atom[K]` key type SHALL determine exactly one runtime atom identity. Opaque runtime witness
construction used by a registry SHALL be lexically owned or delegated through a safe generative witness;
package-qualified visibility alone SHALL NOT grant that authority to downstream code declaring the library package.

#### Scenario: Declare named atoms with singleton keys
- **WHEN** a caller declares `type USD = Atom["asset:USD"]` and obtains the corresponding authoritative `DimRef`
- **THEN** `USD` is the canonical one-power dimension for that singleton key

#### Scenario: Reject a widened literal constructor key
- **WHEN** a caller creates two legal `ValueOf[String & Singleton]` values containing different strings and explicitly
  requests `DimRef.atom[String & Singleton]`
- **THEN** `Normalize[Atom[String & Singleton]]` is not derivable and each invalid construction is rejected

#### Scenario: Bind nominal construction to the supplied stable identity
- **WHEN** two distinct `NominalAtom` objects are widened to a shared nominal singleton supertype
- **THEN** their constructor results retain distinct stable-value singleton key types and cannot both inhabit one
  caller-selected `DimRef[Atom[K]]` type

#### Scenario: Reject nonconcrete singleton keys
- **WHEN** normalization is requested for a canonical entry keyed by `Singleton`, `Nothing`, `Null`, a broad
  intersection, an abstract bound, or an ordinary non-term type reference
- **THEN** derivation fails with a concrete-stable-singleton diagnostic and no canonical evidence is issued

#### Scenario: Preserve supported concrete keys
- **WHEN** an atom key is a literal, nominal object, stable local/module value, generative atomic witness, fresh runtime
  witness, or transparent alias/annotation exposing to one of those identities
- **THEN** normalization succeeds without a caller manually constructing evidence

#### Scenario: Name an integer-powered canonical dimension
- **WHEN** a caller names `Dim[Power["length", 2] *: Power["time", -1] *: EmptyTuple]`
- **THEN** normalization accepts the nonzero literal `Int` powers and preserves their exact mathematical values

#### Scenario: Reject malformed canonical entries
- **WHEN** a claimed `Dim` contains a zero power, duplicate key, non-`Power` entry, abstract exponent, or unresolved tuple
- **THEN** normalization and every arithmetic boundary reject the complete representation

#### Scenario: Reject fractional exponents
- **WHEN** a caller attempts to use a floating, decimal, or rational type as a `Power` exponent
- **THEN** the type does not satisfy the static exponent contract and compilation fails

#### Scenario: Normalize the closed expression grammar
- **WHEN** a concrete expression combines `Dim`, `Times`, `Inverse`, `Divide`, `Atom`, `One`, transparent aliases, and
  transparent annotations
- **THEN** one `Normalize` derivation produces a validated, unannotated canonical `Dim`

#### Scenario: Require one contextual operation in generic code
- **WHEN** generic code operates on an abstract `D <: Dimension` whose entries are not statically visible
- **THEN** it accepts and forwards contextual `Normalize` evidence for its complete expression

#### Scenario: Reject the legacy proof surface
- **WHEN** supported downstream code names the removed signed-natural exponent types or specialized product, quotient,
  inverse, alignment, or normalized-powers evidence
- **THEN** those APIs are unavailable and the code must use literal `Int` powers and `Normalize`

#### Scenario: Preserve an opaque runtime dimension safely
- **WHEN** a runtime key is resolved without a statically visible decomposition
- **THEN** its witness carries a stable singleton-key atom type that participates safely in subsequent static algebra

#### Scenario: Prevent recursive carrier specialization
- **WHEN** downstream source names removed recursive merge, guard, token, or equivalent carrier APIs in an attempt to
  choose a normalized output
- **THEN** those APIs are unavailable and cannot authorize a malformed canonical result

#### Scenario: Reject malformed exponent magnitudes
- **WHEN** a stored `Power` uses an abstract, unresolved, or nonliteral `Int` exponent
- **THEN** normalization and every arithmetic evidence boundary reject the complete representation

#### Scenario: Reject disguised reducible factors
- **WHEN** a claimed canonical key is an intersection, refinement, bound, unresolved wrapper, or non-singleton dimension
  expression disguised behind an alias
- **THEN** automatic derivation rejects it rather than treating it as one concrete singleton identity

#### Scenario: Diagnose unresolved generic derivation cleanly
- **WHEN** a generic method attempts multiplication without contextual `Normalize[Times[A, B]]` evidence
- **THEN** compilation reports actionable contextual-evidence guidance without a macro exception or compiler stack trace

#### Scenario: Reject an alias-hidden generic endpoint
- **WHEN** generic normalization or arithmetic introduces `type X = A` for an unresolved dimension parameter
- **THEN** the alias is followed to `A` and automatic derivation requires contextual `Normalize` evidence

#### Scenario: Expose a concrete associated alias
- **WHEN** a fixed holder defines a transparent `holder.D = Times[A, B]` over concrete dimensions
- **THEN** normalization flattens `holder.D` exactly as `Times[A, B]`

#### Scenario: Preserve a stable abstract associated identity
- **WHEN** a runtime-issued witness exposes its own concrete stable singleton key while another key is rooted in an
  abstract parameter or refinable selection
- **THEN** the witness key remains one opaque atomic identity and the unresolved key is rejected

#### Scenario: Reject rebound generic operation evidence
- **WHEN** generic `Normalize` evidence is copied through stable local values or singleton ascriptions while its input or
  output remains unresolved
- **THEN** rebinding does not create automatic authority and generic code must forward the contextual evidence

#### Scenario: Reuse a concrete operation output
- **WHEN** a stable `Normalize` value exposes an exact canonical output over fully concrete inputs
- **THEN** the output can be reused and is accepted consistently with direct concrete derivation

#### Scenario: Expose concrete operation endpoints transitively
- **WHEN** transparent aliases successively expose a fully concrete normalized operation result
- **THEN** normalization reaches the same canonical `Dim` as the final concrete endpoint regardless of alias depth

#### Scenario: Reject nested powers after endpoint exposure
- **WHEN** a claimed canonical entry or alias exposes a nested `Dim`, `Times`, `Inverse`, or other dimension expression
  where a concrete singleton key is required
- **THEN** final validation rejects it instead of certifying the expression as an atomic key

#### Scenario: Reject recursive term paths conservatively
- **WHEN** recursive aliases or witness paths prevent a singleton key or dimension expression from reaching a stable
  semantic form
- **THEN** derivation fails with contextual-evidence guidance without a stack overflow, macro exception, or compiler
  assertion

#### Scenario: Reuse a completed shared term path
- **WHEN** two acyclic branches reference the same completed concrete stable singleton key or normalized expression
- **THEN** both branches derive consistently and produce one canonical identity for that key

#### Scenario: Revalidate exposed final factors
- **WHEN** a transparent alias in a computed or claimed canonical entry exposes an unresolved, nonconcrete, duplicate,
  zero-powered, or otherwise malformed structure
- **THEN** the final normalization boundary rejects it before constructing trusted evidence

#### Scenario: Canonicalize an annotated atom coherently
- **WHEN** a valid stable singleton key or `Atom[K]` is wrapped in a transparent annotation
- **THEN** normalization derives the same unannotated canonical `Dim` as for the underlying atom

#### Scenario: Normalize an annotated reducible expression
- **WHEN** `Times[A, B]`, `Inverse[A]`, `Divide[A, B]`, a canonical `Dim`, or a transparent alias is annotated
- **THEN** derivation exposes and reduces the underlying expression normally and stores no annotation wrapper

#### Scenario: Canonicalize annotated natural magnitudes
- **WHEN** a migrated exponent uses a valid annotated singleton `Int` literal in place of the removed natural encoding
- **THEN** normalization emits the corresponding ordinary unannotated singleton `Int` exponent

#### Scenario: Reject invalid annotated underlying structure
- **WHEN** an annotation wraps a nonliteral exponent, malformed `Dim`, nonconcrete key, unresolved generic expression, or
  structure outside the closed grammar
- **THEN** derivation rejects the exposed underlying structure by the same rule as the unannotated form

### Requirement: Arbitrary-precision exactness
Quantity coefficients, grid coordinates, rational numerators and denominators, and runtime `DimensionKey` exponents SHALL
retain arbitrary-precision semantics. Static `Power` exponents SHALL instead use the exact values representable by Scala
singleton `Int` literals. Static normalization SHALL perform exponent arithmetic without machine overflow and SHALL emit
a canonical result only when every surviving exponent fits in `Int`; otherwise it MUST fail compilation explicitly.
Static or runtime arithmetic MUST NOT silently wrap, truncate, saturate, or approximate a value.

#### Scenario: Denominator grows during arithmetic
- **WHEN** an exact calculation produces a denominator absent from either input
- **THEN** the normalized rational result is preserved exactly

#### Scenario: Static exponent fits the literal range
- **WHEN** static dimension arithmetic produces a surviving exponent within the `Int` range
- **THEN** the exact value is emitted as the corresponding `Int` singleton literal

#### Scenario: Static exponent exceeds the literal range
- **WHEN** combining or negating static powers would produce a surviving exponent outside the `Int` range
- **THEN** compilation fails with an explicit range diagnostic and emits no wrapped or otherwise incorrect dimension type

#### Scenario: Runtime exponent exceeds the static range
- **WHEN** runtime `DimensionKey` arithmetic produces an exponent outside the `Int` range
- **THEN** the runtime key preserves the exact `BigInt` exponent without approximation

#### Scenario: Dimension exponent exceeds machine range
- **WHEN** static or runtime dimension arithmetic produces an exponent outside the static singleton-`Int` range
- **THEN** static compilation fails explicitly without emitting an incorrect type, while runtime `DimensionKey`
  arithmetic preserves the exact mathematical exponent

### Requirement: Dimension-safe additive and multiplicative arithmetic
Addition and subtraction SHALL accept quantities with the same dimension type or trusted `SameDimension` evidence and
SHALL retain the left operand's dimension type. Multiplication by `Rational` SHALL preserve the quantity's dimension.
Multiplying `Quantity[A]` by `Quantity[B]` SHALL use the single normalization operation and return an exact quantity in a
canonical `Dim`: nested products SHALL be flattened, inverse powers negated, equal singleton keys combined, zero powers
removed, and every surviving key stored exactly once with a nonzero `Int` exponent. Entry order MAY follow operand order
and SHALL NOT affect dimension equivalence.

For fully concrete inputs, the inferred public result SHALL expose the complete canonical dimension without a
specialized product evidence type or caller-visible alignment step. Generic code SHALL state and forward one contextual
`Normalize` computation for the complete multiplication, inversion, or quotient expression when its inputs are
unresolved. Instantiating such generic code with concrete dimensions SHALL agree with normalizing the corresponding
concrete expression directly. Hidden decompositions of runtime-resolved opaque dimensions SHALL remain unavailable to
static cancellation until checked runtime equivalence is supplied.

Every public operation that performs arithmetic and preserves a dimension parameter `D` SHALL require `Normalize[D]`.
For heterogeneous addition or subtraction it SHALL require normalization of both participating dimensions in addition
to `SameDimension[D, E]`. This includes zero identities, exact scalar multiplication and division, grid closed
arithmetic, allocation and quantization, arithmetic grid conversion/projection, refinement-preserving wrappers, and
optional arithmetic algebra instances. The requirement SHALL be `Normalize[D]`, not `Normalize.Aux[D, D]`, because a
valid quantity dimension may be a noncanonical source expression such as `Divide[T, F]` in `Rate[F, T]`. Operations
already requiring `Normalize` of their complete dimension-changing expression SHALL not add redundant operand
normalization evidence. Equality, ordering, sign inspection, and authoritative witness-owned construction SHALL remain
separate from arithmetic validation.

#### Scenario: Add and subtract exact quantities
- **WHEN** two exact USD quantities are added or subtracted
- **THEN** both results are exact `Quantity[USD]` values

#### Scenario: Reject malformed dimension-preserving arithmetic
- **WHEN** a zero-power or otherwise malformed `Dim` is used with quantity or grid zero, addition, subtraction, scalar
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
- **THEN** the exact result's public dimension is their validated canonical `Dim` product

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
- **THEN** every use reaches the same canonical `Dim` and runtime `DimensionKey` as direct use of the concrete endpoint

#### Scenario: Canonicalize definitionally equal aliases coherently
- **WHEN** `holder.D` is a transparent alias for `Times[A, B]`
- **THEN** normalization produces the same canonical output for `holder.D` and `Times[A, B]`, including duplicate-key
  combination, cancellation, inversion, and runtime-key agreement

#### Scenario: Canonicalize definitionally equal annotated inputs coherently
- **WHEN** a stable atom, canonical `Dim`, reducible expression, or transparent alias differs from another input only by
  annotations
- **THEN** normalization produces the same unannotated canonical output and agrees with runtime `DimensionKey`
  multiplication and inversion

#### Scenario: Keep runtime-hidden structure opaque
- **WHEN** an opaque runtime dimension's key contains a factor that would cancel a separate static atom
- **THEN** automatic static normalization does not inspect the hidden key, and cancellation requires checked runtime
  equivalence

### Requirement: Exact rates and ratios
`Rate[From, To]` SHALL represent an exact coefficient oriented from `From` to `To`, and `Ratio` SHALL denote
`Quantity[One]`. Rate construction, application, composition, reciprocal arithmetic, and ordinary quantity
multiplication SHALL preserve that orientation. Applying `Rate[From, To]` to `Quantity[From]` SHALL expose
`Quantity[To]` directly. Composing `Rate[A, B]` with `Rate[B, C]` SHALL expose `Rate[A, C]` directly. Fully concrete
ordinary rate multiplication and division SHALL expose their canonical endpoint dimension without a caller-supplied
alignment step. Identity rates SHALL require an authoritative dimension witness; composition SHALL remain associative
and preserve both identities. Every endpoint-oriented helper that returns a declared target instead of its dependent
normalization output SHALL require `Normalize` for the complete source expression that justifies the cancellation, so a
malformed endpoint representation cannot cross that boundary.

#### Scenario: Apply a mathematical rate
- **WHEN** `0.1 BTC` is acted on by `60000.01 USD/BTC`
- **THEN** ordinary multiplication and rate application both produce exact `6000.001 USD` as `Quantity[USD]`

#### Scenario: Compose rates without alignment repair
- **WHEN** an `A`-to-`B` rate is followed by a `B`-to-`C` rate
- **THEN** rate composition produces `Rate[A, C]` directly, and fully concrete ordinary multiplication exposes the same
  canonical endpoint dimension

#### Scenario: Compose rates
- **WHEN** an `A`-to-`B` rate is followed by a `B`-to-`C` rate
- **THEN** rate composition produces the exact product directly as `Rate[A, C]`

#### Scenario: Derive a cross rate by division
- **WHEN** `USD/BTC` is divided by nonzero `USD/ETH`
- **THEN** an endpoint-oriented cross-rate operation returns `Rate[BTC, ETH]` directly without explicit retagging, while
  generic quantity division continues to return its canonical `Normalize` output

#### Scenario: Reject malformed endpoint cancellation
- **WHEN** rate application, composition, ratio construction, or cross-rate division mentions a malformed `Dim`
- **THEN** normalization of the complete arithmetic expression fails and no endpoint-shaped result is produced

### Requirement: Checked quantity division
Division by a quantity SHALL require `NonZero[Quantity[B]]`. Dividing `Quantity[A]` by that evidence SHALL return an exact
quantity whose dimension is the canonical normalization of `A / B`, using the same singleton-key and literal-`Int`
rules as multiplication and inversion. Fully concrete equal-dimension division SHALL expose `Ratio` directly, and fully
concrete quotient arithmetic SHALL require no specialized quotient or alignment evidence at the call site. A grid
divisor SHALL first use its canonical exact embedding and then the same generic `NonZero` check. No separate divisor
carrier or validator SHALL exist.

#### Scenario: Produce a ratio directly
- **WHEN** exact `10 USD` is divided by checked nonzero `3 USD`
- **THEN** the result is `Ratio` with coefficient `10/3` without explicit dimension alignment

#### Scenario: Produce a ratio
- **WHEN** exact `10 USD` is divided by checked nonzero `3 USD`
- **THEN** the result is `Ratio` with coefficient `10/3`

#### Scenario: Simplify a quotient of rates
- **WHEN** exact `USD/BTC` is divided by checked nonzero `USD/ETH`
- **THEN** equal USD powers cancel and the result exposes the canonical `ETH/BTC` dimension

#### Scenario: Reject zero divisor evidence
- **WHEN** `NonZero` receives a zero exact quantity or the exact embedding of a zero grid quantity
- **THEN** it fails and division remains unavailable
