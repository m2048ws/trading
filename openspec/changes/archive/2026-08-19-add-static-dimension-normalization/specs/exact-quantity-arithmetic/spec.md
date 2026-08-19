## ADDED Requirements

### Requirement: Compile-time dimension equivalence
`SameDimension[A, B]` SHALL be derivable at compile time when the statically visible power representations of `A` and
`B` contain the same atom/exponent entries modulo permutation. Static derivation SHALL require no runtime `DimRef`,
`DimensionKey`, or atom ordering. The evidence SHALL be a restricted capability whose construction remains unavailable to
supported downstream code; it SHALL authorize quantity- and grid-dimension coercion and MAY be supplied contextually to
dimension-safe operations, but SHALL NOT expose unrestricted Scala type equality. An explicit evidence-checked alignment
operation SHALL allow callers to select an equivalent target dimension. The library SHALL NOT provide a global implicit
conversion between arbitrary `Quantity` types. Static derivation SHALL remain available to supported downstream Scala
compilation, including when compiler warnings are treated as errors, and SHALL NOT rely on inaccessible implementation
details. Reflexive `SameDimension[D, D]` SHALL mean structural Scala type identity and SHALL remain available without
certifying that `D` is a canonical normalized representation. Equivalence between distinct static representations and
every use of a representation as a canonical operation result SHALL require complete canonical validation.

#### Scenario: Align commuted products
- **WHEN** one exact result has powers `A¹, B¹` and another has powers `B¹, A¹`
- **THEN** compile-time `SameDimension` evidence is derivable without assigning an ordering to `A` and `B`

#### Scenario: Use evidence in addition
- **WHEN** two quantities have equivalent normalized powers in different tuple orders
- **THEN** addition and subtraction accept statically derived evidence and retain the left operand's dimension type

#### Scenario: Select an economic result type explicitly
- **WHEN** a composed rate has powers equivalent to `Rate[A, C]` but in a different order
- **THEN** evidence-checked alignment can expose the result as `Rate[A, C]` without runtime comparison or an unchecked cast

#### Scenario: Reject unequal static dimensions
- **WHEN** the normalized powers of two static dimensions differ by an atom or exponent
- **THEN** `SameDimension` evidence is not derivable and evidence-requiring arithmetic does not compile

#### Scenario: Derive evidence from downstream code
- **WHEN** supported downstream Scala requests `SameDimension` for equivalent commuted products with compiler warnings
  treated as errors
- **THEN** the evidence compiles without inaccessible-member diagnostics or access to implementation-only proof rules

#### Scenario: Keep reflexivity separate from canonical certification
- **WHEN** a malformed power representation requests `SameDimension[D, D]`
- **THEN** reflexive identity MAY be available, but normalization, product, quotient, inverse, and alignment SHALL still
  reject that representation as a canonical result or operand

### Requirement: Atomic and canonical static derivation
Automatic derivation of `NormalizedPowers`, `DimensionProduct`, `DimensionQuotient`, `DimensionInverse`, and
`DimensionAlignment` SHALL be one atomic trusted operation for the complete requested evidence. It SHALL inspect complete
input types, expose transparent aliases, reject unresolved or refinable structure, compute powers internally,
independently expose and reclassify every computed factor, validate the computed tuple and final dimension, and only then
issue final evidence. Callers SHALL NOT compose or replay merge, insertion,
removal, alignment-rule, guard, token, or equivalent intermediate evidence to choose a trusted output type.

A transparent Scala type alias SHALL NOT define a new static dimension identity. Stability classification and canonical
parsing SHALL recursively follow its right-hand side, including concrete term-prefixed aliases and applied aliases, with
cycle protection. A deferred associated member selected from a documented stable project witness MAY remain an opaque
atomic identity. A deferred member rooted in a method parameter, arbitrary refinable holder, or other
substitution-unstable path SHALL remain unresolved, including when one or more transparent aliases hide that path.

Stability of a dependent associated type SHALL require stability of both its term path and the widened/ascribed semantic
type carried by every relevant qualifier. A stable local `val`, repeated local rebinding, singleton ascription, or a
refinement over method parameters SHALL NOT make unresolved inputs or associated outputs substitution-stable. Operation
associated outputs SHALL be reused automatically only when their semantic evidence type exposes an exact concrete output
whose dependencies are all substitution-stable; otherwise generic code SHALL accept and forward contextual final
operation evidence. Final validation SHALL repeat semantic qualifier stability analysis before accepting any associated
output or stored factor.

Concretely refined associated type endpoints SHALL be exposed transitively to a guarded semantic fixed point before
canonical factor validation. Each exposed endpoint SHALL be returned to transparent alias/annotation exposure and
ordinary semantic parsing. If a concrete `operation.Out` exposes through one or more other exact associated endpoints to
`Powers`, `Times`, `Inverse`, `One`, or another reducible structure, that structure SHALL be normalized or rejected by its
ordinary canonical rule and SHALL NOT survive as one atomic stored factor. Stable project-issued opaque dimension
identities SHALL remain opaque, and abstract/refinable endpoints SHALL remain unresolved.

Recursive semantic traversal SHALL distinguish active term analysis from completed term analysis. Re-entry into an
active term SHALL be unresolved and SHALL NOT be interpreted as an empty set of stability problems. A result MAY be
cached only after its traversal completes; later branches MAY reuse that completed result, so a shared acyclic graph is
not treated as a cycle. Fixed-point endpoint traversal SHALL likewise reject cycles or representational non-progress
cleanly without macro/compiler exceptions.

A transparent Scala `AnnotatedType` SHALL NOT define a new static dimension identity. Semantic exposure SHALL discard
annotation wrappers centrally before stability classification, algebraic parsing, exponent parsing, and final canonical
validation. Annotated atoms, aliases, `Times`, `Inverse`, `Divide`, and `Powers` expressions SHALL derive exactly as their
unannotated underlying types, and canonical outputs SHALL omit annotation wrappers. Annotated natural magnitudes SHALL be
parsed from their exposed finite `NaturalZero`/`NaturalSuccessor` structure and emitted in the ordinary unannotated
encoding. An annotation SHALL NOT make unresolved or malformed underlying structure stable or canonical. This
transparency SHALL NOT extend to refinements, intersections, unions, or unresolved match types.

A canonical stored power SHALL be `Power[Factor, PositiveExponent[M]]` or
`Power[Factor, NegativeExponent[M]]`, where `M` is exactly `NaturalZero` or a finite chain of exact
`NaturalSuccessor[...]` nodes ending in `NaturalZero`. Stored zero exponents, bare `Natural`, abstract/refined magnitudes,
duplicate factors, non-`Power` entries, empty or nonminimal `Powers`, and unresolved tuples SHALL be rejected. Stored
factors SHALL be concrete and irreducible: after transparent annotation and alias exposure, `One`, `Times`, `Inverse`,
`Powers`, base `Dimension`, and reducible structures hidden by intersections, refinements, bounds, match types, or unknown
wrappers SHALL NOT be treated as opaque atoms. Final validation SHALL expose annotations and aliases again, reject
unresolved or reducible factor endpoints, and require each surviving factor to be a stable irreducible dimension identity
independently of the preceding canonicalizer.

#### Scenario: Prevent recursive carrier specialization
- **WHEN** downstream source attempts to derive a recursive merge proof for independently abstract factors, specialize
  them to the same path, and select a duplicate-factor final product output
- **THEN** no public recursive authority can be obtained and final operation derivation cannot issue the malformed result

#### Scenario: Reject malformed exponent magnitudes
- **WHEN** a stored power uses `PositiveExponent[Natural]`, `NegativeExponent[Natural]`, or an unresolved magnitude member
- **THEN** normalization and every final operation evidence boundary reject the structure

#### Scenario: Reject disguised reducible factors
- **WHEN** a stored factor is base `Dimension`, `Times[A, B] & Tag`, or an annotated/refined reducible constructor
- **THEN** automatic derivation rejects it rather than treating it as one opaque atom

#### Scenario: Diagnose unresolved generic derivation cleanly
- **WHEN** an inferred generic method attempts multiplication without contextual `DimensionProduct[A, B]`
- **THEN** compilation reports actionable contextual-evidence guidance without `CyclicReference`, macro exceptions, or a
  compiler stack trace

#### Scenario: Reject an alias-hidden generic endpoint
- **WHEN** generic normalization, inversion, or alignment introduces `type X = A` before requesting automatic evidence
- **THEN** the alias is followed to `A` and automatic derivation requires contextual final-operation evidence

#### Scenario: Expose a concrete associated alias
- **WHEN** a fixed holder defines transparent `holder.D = Times[A, B]`
- **THEN** normalization, inverse, product, quotient, and alignment flatten `holder.D` exactly as `Times[A, B]`

#### Scenario: Preserve a stable abstract associated identity
- **WHEN** a project-issued stable witness exposes an intentionally abstract associated dimension member
- **THEN** that exact stable path MAY remain one irreducible atom, while a parameter-rooted or refinable selection is
  rejected even when hidden behind aliases

#### Scenario: Reject rebound generic operation evidence
- **WHEN** a generic operation value is copied through one or more stable local values or singleton ascriptions and its
  semantic type still contains method parameters, refinements over them, or an abstract associated output
- **THEN** its selected output remains unresolved for automatic derivation and contextual final-operation evidence is
  required without macro or compiler exception leakage

#### Scenario: Reuse a concrete operation output
- **WHEN** a stable operation value and any local rebound copies expose an exact canonical output over fully concrete
  substitution-stable inputs
- **THEN** its associated output is exposed, normalized, and accepted consistently with direct concrete derivation

#### Scenario: Expose concrete operation endpoints transitively
- **WHEN** one, two, or three exact concrete operation refinements successively name the preceding operation's associated
  output
- **THEN** normalization, inverse, product, quotient, and alignment reach the same canonical result as the final concrete
  endpoint regardless of indirection depth

#### Scenario: Reject nested powers after endpoint exposure
- **WHEN** a claimed stored factor is an associated output that transitively exposes to `Powers[...]`
- **THEN** final validation reparses the exposed endpoint and rejects nested `Powers` instead of certifying the selected
  path as an atomic factor

#### Scenario: Reject recursive term paths conservatively
- **WHEN** two or more typed lazy local dimension witnesses form an initializer cycle and derivation selects a dimension
  member from that cycle
- **THEN** active term re-entry remains unresolved and derivation fails with contextual-evidence guidance without a stack
  overflow, macro exception, or compiler assertion

#### Scenario: Reuse a completed shared term path
- **WHEN** two acyclic branches reference the same completed concrete local witness or operation value
- **THEN** the second branch reuses the completed semantic analysis and canonical derivation succeeds

#### Scenario: Revalidate exposed final factors
- **WHEN** a transparent alias in a computed or claimed final factor exposes unresolved, reducible, base, nested, or
  malformed structure
- **THEN** the final evidence boundary rejects it before constructing trusted evidence

#### Scenario: Canonicalize an annotated atom coherently
- **WHEN** Scala proves `A @ Marker =:= A` for a stable atomic dimension
- **THEN** normalization, inverse, product, quotient, and alignment derive the same canonical result for both inputs

#### Scenario: Normalize an annotated reducible expression
- **WHEN** `Times[A, B]`, `Inverse[A]`, `Divide[A, B]`, a canonical `Powers` expression, or a transparent alias is
  annotated
- **THEN** derivation exposes and reduces the underlying expression normally and stores no annotation wrapper

#### Scenario: Canonicalize annotated natural magnitudes
- **WHEN** a valid exponent contains annotations around `NaturalZero`, a finite `NaturalSuccessor`, or its tail
- **THEN** the exponent parses to and is emitted as the ordinary unannotated canonical natural encoding

#### Scenario: Reject invalid annotated underlying structure
- **WHEN** an annotation wraps bare or abstract `Natural`, base `Dimension`, nonminimal or malformed `Powers`, duplicate
  factors, zero exponents, or a parameter-rooted selection
- **THEN** derivation rejects the exposed underlying structure by the same rule as the unannotated form

## MODIFIED Requirements

### Requirement: Arbitrary-precision exactness
Quantity coefficients, grid coordinates, rational numerators and denominators, and normalized dimension exponents SHALL
use arbitrary-precision semantics. Static dimension exponents SHALL have exact signed-integer semantics consistent with
runtime `DimensionKey` exponents. Arithmetic MUST NOT silently overflow, wrap, truncate, saturate, or approximate; an
exact unary magnitude SHALL be validated recursively and SHALL NOT be replaced by this remediation. An implementation
resource limit encountered during static normalization MUST fail compilation rather than produce an incorrect dimension.

#### Scenario: Denominator grows during arithmetic
- **WHEN** an exact calculation produces a denominator absent from either input
- **THEN** the normalized rational result is preserved exactly

#### Scenario: Dimension exponent exceeds machine range
- **WHEN** static or runtime dimension operations produce an exponent outside a machine integer range
- **THEN** the canonical mathematical exponent remains correct or static compilation fails explicitly without emitting an
  incorrect type

### Requirement: Dimension-safe additive and multiplicative arithmetic
Addition and subtraction SHALL accept quantities with the same dimension type or with trusted `SameDimension` evidence;
the result SHALL retain the left operand's dimension type. Multiplication by `Rational` SHALL preserve the quantity's
dimension. Multiplying `Quantity[A]` by `Quantity[B]` SHALL return an exact quantity whose static dimension is the
simplified algebraic product of `A` and `B`: nested products SHALL be flattened, inverse powers SHALL be negated, equal
atoms SHALL have their signed powers combined, and zero powers SHALL be removed. Each surviving atom SHALL occur exactly
once with a nonzero exponent. The order of surviving power entries MAY depend on operand order and SHALL NOT affect
dimension equivalence. Static normalization SHALL be substitution-stable: expressing an operation in generic code and
later instantiating its dimension parameters SHALL produce the same normalized dimension as expressing that operation
directly with those concrete types. An unresolved relationship between generic dimension parameters SHALL NOT be
committed as equality or inequality before instantiation. Unlike-dimension addition and subtraction SHALL remain
unavailable without trusted evidence. A path-dependent selection rooted in a method term parameter or refinable prefix
SHALL be unresolved for automatic derivation even when the selected member symbol itself appears stable. Generic code
SHALL accept and forward contextual final operation evidence; stable fixed paths returned by `DimRef.atomic` and
`DimRef.fresh` SHALL remain eligible for direct automatic derivation. Definitionally equal dimension inputs SHALL
canonicalize coherently: replacing an input by a transparent alias or adding a transparent annotation SHALL not change
the canonical powers or output of normalization, product, quotient, inverse, or alignment.

A stable local term identity alone SHALL NOT establish associated-type stability. If its widened/ascribed semantic type,
initializer chain, or exact member refinement retains unresolved method parameters or refinable outputs, automatic
derivation SHALL reject the selected type. Rebinding preserves unresolved dependencies. Conversely, a concrete operation
output whose exact endpoint and dependencies are substitution-stable SHALL remain reusable and canonical. Exact concrete
associated endpoints SHALL be followed transitively until their semantic representation stops changing; active recursive
term paths SHALL remain unresolved, while reuse of fully completed noncyclic term analysis SHALL remain valid.

#### Scenario: Add and subtract exact quantities
- **WHEN** two exact USD quantities are added or subtracted
- **THEN** both results are exact `Quantity[USD]` values

#### Scenario: Multiply dimensions
- **WHEN** exact quantities in dimensions `A` and `B` are multiplied
- **THEN** the exact result's public dimension is the simplified algebraic product of `A` and `B`

#### Scenario: Cancel a price denominator
- **WHEN** a quantity in dimension `Position` is multiplied by a quantity in dimension `Settlement / Position`
- **THEN** the exact result simplifies to `Quantity[Settlement]` without using a special rate-application operation

#### Scenario: Retain a multi-atom product
- **WHEN** multiplication leaves nonzero powers for more than one atom
- **THEN** the result type contains one entry for each surviving atom and no reducible multiplication or inversion history

#### Scenario: Preserve commutative equivalence
- **WHEN** quantities in dimensions `A` and `B` are multiplied in opposite operand orders
- **THEN** the two result dimensions admit compile-time `SameDimension` evidence

#### Scenario: Specialize generic multiplication to one dimension
- **WHEN** a generic multiplication operation over dimensions `A` and `B` is instantiated with both parameters equal to
  dimension `D`
- **THEN** its result contains one `D²` power and has the same static dimension as direct multiplication in `D`

#### Scenario: Specialize generic inversion to a visible product
- **WHEN** a generic dimension-inversion operation is instantiated with a statically visible product `A × B`
- **THEN** its result flattens to powers `A⁻¹, B⁻¹` and has the same static dimension as direct inversion of `A × B`

#### Scenario: Reject late alias specialization without contextual evidence
- **WHEN** a method derives multiplication or quotient for `x.D` and `y.D` while `x` and `y` are term parameters and the
  caller later passes the same witness for both arguments
- **THEN** automatic derivation is rejected at the generic definition and the caller supplies contextual final operation
  evidence only after the relationship is stable

#### Scenario: Reject refinable-member inversion without contextual evidence
- **WHEN** inversion is requested for an abstract member rooted in a term parameter that can later refine to `Times[A, B]`
- **THEN** automatic derivation is rejected instead of freezing that member as one opaque inverse factor

#### Scenario: Reject local aliases over dependent parameters
- **WHEN** generic product or quotient code defines `type X = x.D` and `type Y = y.D` before requesting automatic evidence
- **THEN** the aliases do not hide the parameter roots, and contextual product or quotient evidence is required before
  later specialization can merge or cancel equal dimensions

#### Scenario: Reject stable local transport of unresolved evidence
- **WHEN** generic evidence or a parameter-dependent witness is rebound through stable locals, explicit singleton
  ascriptions, or `Aux` refinements before requesting another automatic operation
- **THEN** automatic derivation follows the qualifier semantics and initializer chain, refuses to treat the selected
  member as an opaque atom, and cannot emit duplicate, uncancelled, or nested-power representations

#### Scenario: Preserve endpoint-depth coherence
- **WHEN** a concrete operation output is rebound and reused as the exact endpoint of one or more further concrete
  operation refinements
- **THEN** every final operation derives the same static powers and runtime `DimensionKey` as direct use of the concrete
  endpoint, and no nested `Powers` factor survives

#### Scenario: Canonicalize definitionally equal aliases coherently
- **WHEN** `holder.D` is a transparent alias for `Times[A, B]`
- **THEN** every valid static operation derives the same canonical output for `holder.D` and `Times[A, B]`, including
  duplicate merging, cancellation, inverse flattening, and runtime `DimensionKey` agreement

#### Scenario: Canonicalize definitionally equal annotated inputs coherently
- **WHEN** a stable atom, reducible product, or transparent product alias differs from another input only by annotations
- **THEN** every valid static operation produces the same unannotated canonical output and agrees with runtime
  `DimensionKey` multiplication and inversion

### Requirement: Exact rates and ratios
`Rate[From, To]` SHALL denote an exact quantity in the simplified dimension `To / From`, and `Ratio` SHALL denote
`Quantity[One]`. Rate construction, application, composition, reciprocal dimensional arithmetic, and ordinary quantity
multiplication SHALL preserve this orientation. Applying `Rate[From, To]` to `Quantity[From]` through either ordinary
multiplication or the rate convenience SHALL produce a result statically equivalent to `Quantity[To]`. Composing
`Rate[A, B]` with `Rate[B, C]` through either ordinary multiplication plus alignment or the composition convenience SHALL
produce a result statically equivalent to `Rate[A, C]`. Identity rates SHALL require an authoritative `DimRef[D]`;
composition SHALL be associative and SHALL preserve left and right identity.

#### Scenario: Apply a mathematical rate
- **WHEN** `0.1 BTC` is acted on by `60000.01 USD/BTC`
- **THEN** ordinary multiplication and rate application both produce exact `6000.001 USD` as `Quantity[USD]`

#### Scenario: Compose rates
- **WHEN** an `A`-to-`B` rate is followed by a `B`-to-`C` rate
- **THEN** ordinary multiplication and rate composition both produce the exact product in a dimension equivalent to
  `Rate[A, C]`

#### Scenario: Derive a cross rate by division
- **WHEN** `USD/BTC` is divided by nonzero `USD/ETH`
- **THEN** the simplified result dimension is equivalent to `ETH/BTC`

### Requirement: Checked quantity division
Division by a quantity SHALL require `NonZero[Quantity[B]]`. Dividing `Quantity[A]` by that evidence SHALL return an exact
quantity whose static dimension is the simplified quotient of `A` and `B`, using the same power normalization as
multiplication and inversion. Equal-dimension division SHALL expose `Ratio`, and division of algebraically equivalent
dimensions SHALL be alignable to `Ratio` with `SameDimension` evidence. A grid divisor SHALL first use its canonical exact
embedding and then the same generic `NonZero` check. No separate divisor carrier or validator SHALL exist.

#### Scenario: Produce a ratio
- **WHEN** exact `10 USD` is divided by checked nonzero `3 USD`
- **THEN** the result is `Ratio` with coefficient `10/3`

#### Scenario: Simplify a quotient of rates
- **WHEN** exact `USD/BTC` is divided by checked nonzero `USD/ETH`
- **THEN** equal USD powers cancel and the result dimension is equivalent to `ETH/BTC`

#### Scenario: Reject zero divisor evidence
- **WHEN** `NonZero` receives a zero exact quantity or the exact embedding of a zero grid quantity
- **THEN** it fails and division remains unavailable
