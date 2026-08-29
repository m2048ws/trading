## 1. Private Static Interpretation and Atom Authority

- [x] 1.1 Replace normalization-output assertions in the static-dimension unit tests with `SameDimension` equivalence,
  malformed-input rejection, and runtime-key agreement assertions that describe the new private interpreter contract.
- [x] 1.2 Refactor `StaticDimension.scala` so one private engine validates the closed grammar, exposes aliases and
  annotations safely, combines powers as `BigInt`, and derives non-reflexive `SameDimension` without emitting a
  canonical `Canonical` output.
- [x] 1.3 Add static-equivalence tests for association, commutation, cancellation, canonical tuple permutation,
  transparent aliases and annotations, unresolved generic inputs, an independently compiled recursive or
  non-progressing path with controlled diagnostics, and unequal dimensions.
- [x] 1.4 Add tests proving private interpretation compares out-of-`Int` accumulated powers exactly and permits large
  intermediate powers to cancel without overflow or canonical-type emission.
- [x] 1.5 Move literal `DimRef.atom[K]` validation behind a lexically owned inline/macro gate that accepts only a concrete
  literal singleton and derives the runtime `AtomId` from that same literal.
- [x] 1.6 Keep nominal, generative, and fresh-runtime atom constructors bound to their exact stable/path-dependent types,
  and share the private concrete-key policy wherever static inspection is required.
- [x] 1.7 Extend separately compiled and same-package-spoof fixtures to reject widened `ValueOf`, `Singleton`, `Nothing`,
  `Null`, intersections, unresolved keys, nominal widening, private-helper access, and contradictory atom authority.

## 2. Expression-Preserving Dimension Witness API

- [x] 2.1 Update `Dim`, `Times`, `Inverse`, `Divide`, `Canonical`, `Rate`, and `Ratio` scaladoc so expressions are public
  result types and mathematical equality is explicitly separate from Scala type identity.
- [x] 2.2 Change `DimRef.times`, `DimRef.inverse`, and `DimRef.divide` to return `DimRef[Times[A, B]]`,
  `DimRef[Inverse[A]]`, and `DimRef[Divide[A, B]]` without contextual canonical-output evidence.
- [x] 2.3 Verify witness identity, product, inverse, and quotient keys against primitive `DimKey` operations,
  including cancellation, repeated construction, opaque runtime atoms, and powers beyond machine range.
- [x] 2.4 Add downstream compile fixtures showing generic witness algebra needs no output type parameter and that an
  explicitly nominated spelling is related with `SameDimension`, not inferred by witness algebra.

## 3. Quantity Arithmetic and Explicit Alignment

- [x] 3.1 Finalize `Quantity.alignTo` and `GridQuantity.alignTo` as the only public `SameDimension`-controlled
  value-level retagging operations; remove low-level evidence retagging methods, the former `asDimension` spelling,
  and evidence-driven heterogeneous addition/subtraction overloads.
- [x] 3.2 Make homogeneous `Quantity[D]` addition and subtraction accept exactly `Quantity[D]` and make scalar
  multiplication and exact whole-scalar division preserve existing carriers without dimension capabilities.
- [x] 3.3 Change quantity-by-quantity multiplication to return `Quantity[Times[A, B]]` with exact coefficient arithmetic
  and no normalization context or associated output.
- [x] 3.4 Change checked generic quantity division to return `Quantity[Divide[A, B]]`, retaining the existing
  `NonZero[Quantity[B]]` divisor boundary and exact quotient calculation.
- [x] 3.5 Migrate quantity arithmetic tests and law helpers from `Normalize` outputs to raw expression type assertions,
  proof-free homogeneous generic methods, and explicit `alignTo` where a named spelling is required.
- [x] 3.6 Add generic client compile fixtures for raw multiplication, raw division, homogeneous `total`, and a
  client-nominated `O` using `SameDimension[Times[A, B], O]`.

## 4. Semantic Rate and Ratio Endpoints

- [x] 4.1 Reimplement `Rate.apply(from, to, coefficient)` and `Rate.identity` using endpoint `DimRef` algebra so named
  and path-dependent runtime endpoints construct direct `Rate[From, To]` values without casts or static normalization.
- [x] 4.2 Remove normalization contexts from `applyRate`, `andThen`, `crossRate`, and `ratioTo`, keeping their direct
  `Quantity[To]`, `Rate[A, C]`, and `Ratio` result types through lexically owned coefficient helpers.
- [x] 4.3 Add a checked nonzero rate reciprocal operation with the direct result type `Rate[To, From]` and exact
  reciprocal coefficient.
- [x] 4.4 Make grid rate application and grid ratio calculation delegate through canonical embedding to the same direct
  endpoint operations without public proof parameters.
- [x] 4.5 Update rate and ratio unit/law suites for identity, associativity, application, reciprocal, cross rate, zero
  rejection, and agreement between endpoint coefficients and explicitly aligned raw-expression arithmetic.
- [x] 4.6 Add examples contrasting ordinary `price * size` raw typing with `applyRate`, `andThen`, `crossRate`,
  `reciprocalRate`, `ratioTo`, and explicit `alignTo` at a nominated economic boundary.

## 5. Grid, Projection, and Refinement Transformations

- [x] 5.1 Finalize exact-type same-grid and cross-grid addition/subtraction so existing values require neither
  normalization nor implicit dimensional alignment, and retain explicit `GridQuantity.alignTo` for cross-spelling use.
- [x] 5.2 Change grid-by-grid, grid-by-exact, exact-by-grid, and checked grid division results to `Times` or `Divide`
  expression types and remove grid-specific canonical-output plumbing.
- [x] 5.3 Remove dimension validity contexts from existing-carrier projection, exact narrowing, quantization,
  constrained encoding, quotient/remainder, and allocation paths while retaining their source/target grid witnesses.
- [x] 5.4 Remove redundant dimension validity contexts from checked `SameGrid`, `SameQuantum`, and `Embedding`
  conversions, preserving registry provenance and exact coordinate/quantum checks.
- [x] 5.5 Remove normalization contexts from refinement operations that transform existing quantity or grid values, and
  retain predicate strength only where the existing algebraic closure rules justify it.
- [x] 5.6 Migrate grid, projection, allocation, quantization, grid-evidence, and refinement unit tests to the trusted
  existing-carrier API and raw expression results.
- [x] 5.7 Migrate grid and refinement discipline/law sets so their generic constructors receive actual witnesses while
  their transformation laws require no validity capability.

## 6. Empty Construction and Algebraic Identities

- [x] 6.1 Change `Quantity.zero[D]` and `GridQuantity.zero[D, G]` to require contextual `DimRef[D]`, and let matching grid
  operations pass their owned dimension witness when manufacturing zero.
- [x] 6.2 Change refined quantity/grid zero constructors to require `DimRef[D]` or the stronger matching witness already
  owned by the caller, without adding proof storage to refined values.
- [x] 6.3 Change identity-bearing quantity vector-space, grid-module, and nonnegative monoid instances to require
  `DimRef[D]` or a stronger matching witness; keep combine-only positive semigroups and observations authority-free.
- [x] 6.4 Audit every `Quantity` and `GridQuantity` opaque construction call in constructors, grids, refinements,
  projection, registry adoption, heterogeneous recovery, decoding, persistence, and algebra, and classify it against
  the design's authority table.
- [x] 6.5 Add positive tests for generic zero and identity-bearing algebra with locally scoped `DimRef` values and
  negative tests for missing or ambiguous authority.
- [x] 6.6 Add adversarial fixtures proving malformed `Canonical` types cannot obtain quantity zero, grid zero, refined zero,
  an identity-bearing algebra instance, a coefficient-bearing constructor, or a non-reflexive alignment.

## 7. Runtime-Resolved Arithmetic and Endpoint Rates

- [x] 7.1 Refactor `ResolvedExactQuantity` into a dependent in-memory package that owns one `D <: Dim`,
  `DimRef[D]`, and `Quantity[D]`, so expression results need not be converted to a fresh registered atom.
- [x] 7.2 Migrate heterogeneous same-grid and exact-addition flows to checked retyping plus explicit `alignTo`, followed by
  exact-type proof-free arithmetic, while preserving registry ownership failures.
- [x] 7.3 Add heterogeneous exact multiplication that packages `Quantity[Times[A, B]]` with the matching product
  `DimRef`, and verify its runtime key and coefficient.
- [x] 7.4 Add runtime/path-dependent fixtures that resolve stable source and target dimensions, construct a rate from
  their `DimRef` values, and apply, compose, reciprocate, and cross that rate with direct endpoint types.
- [x] 7.5 Add a checked-recovery fixture for independently resolved equal endpoint keys with distinct path-dependent
  types, proving composition or homogeneous arithmetic becomes available only after explicit alignment.
- [x] 7.6 Add a BitMEX-shaped adapter-boundary fixture that supplies base, quote, position, and settlement endpoints
  explicitly and proves the quantity layer neither infers missing currency roles nor introduces instrument-domain types.
- [x] 7.7 Re-run registry provenance, heterogeneous arithmetic, packing, decoding, persistence, and Java serialization
  suites to verify keys, owners, coordinates, and logical records are unchanged.

## 8. Remove the Public Normalization Surface and Migrate Clients

- [x] 8.1 Delete the public `Normalize` trait, companion, `Aux` alias, derived given, canonical-output emitter, and every
  production signature or import that names them, leaving only private interpreter terminology.
- [x] 8.2 Replace positive adversarial fixtures that forwarded `Normalize` with expression-preserving generic methods,
  endpoint methods, or client-nominated `SameDimension`, and add a negative fixture proving `Normalize` is unavailable.
- [x] 8.3 Replace former exponent-overflow normalization failures with tests for exact raw-expression interpretation,
  while retaining rejection of malformed declared `Power` entries and private-key shapes.
- [x] 8.4 Update all unit-test helpers, discipline fixtures, examples, and compiler-boundary expected diagnostics so no
  supported code treats static validity, runtime inhabitation, equivalence, or value possession as the same capability.
- [x] 8.5 Update README/API documentation and migration examples with before/after signatures for multiply, cross rate,
  notional with nominated output, homogeneous total, algebraic zero, alignment, and runtime endpoint construction.
- [x] 8.6 Inspect the packaged artifact and downstream compiler classpath to verify neither `Normalize` nor another public
  associated canonical-output capability remains, and that private inline helpers compile from a separate project.
- [x] 8.7 Reconcile the active `freeze-static-dimension-authority`, `demote-same-dimension`, and
  `trust-existing-dimensional-values` deltas or their synchronization order so none of their final guidance requires
  public normalization after this change lands.

## 9. Final Verification

- [x] 9.1 Run `scalafmt` for quantities production/tests, adversarial-boundary tests and fixtures, and SBT build sources,
  then confirm formatting introduces no semantic changes.
- [x] 9.2 Run the targeted static-dimension, quantity, grid, refinement, algebra, runtime, and public-boundary test suites
  and resolve all expression-result or authority regressions.
- [x] 9.3 Run `adversarialBoundary / Test / test` against the packaged quantities JAR and verify every positive fixture
  compiles and every negative fixture fails for its intended reason.
- [x] 9.4 Run the full ordered SBT test command for the repository and verify no runtime, law, persistence, serialization,
  or compiler-boundary regression remains.
- [x] 9.5 Run strict OpenSpec validation for `internalize-dimension-normalization` and confirm proposal, specs, design, and
  task completion remain coherent before implementation handoff or archival.
