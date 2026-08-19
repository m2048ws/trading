## 1. Prerequisites and Construction Trust Boundary

- [ ] 1.1 Complete or reconcile `freeze-static-dimension-authority` and `demote-same-dimension`, preserving sealed
  witness construction and exact-type homogeneous arithmetic before simplifying normalization contexts.
- [ ] 1.2 Classify every production `Normalize` context as type-only manufacture, same-index transformation,
  witness-owned target selection, complete-expression computation, or observation, and record the intended keep/remove
  disposition in the implementation review.
- [ ] 1.3 Audit `Quantity`, `GridQuantity`, refinement, `DimRef`, `GridRef`, proof, and resolved-result constructors so no
  public raw coefficient, coordinate, proof, or opaque-carrier constructor can manufacture a caller-selected malformed
  dimension.
- [ ] 1.4 Extend immutable-JAR downstream fixtures for package spoofing, inaccessible opaque constructors, literal
  `null`, and the inability to recover `Normalize`, `DimRef`, `DimensionKey`, `GridRef`, or registry provenance from a
  dimensional value.
- [ ] 1.5 Add malformed-index construction fixtures covering type-only zero, coefficient and coordinate construction,
  alignment, grid evidence, refinements, and identity-bearing algebra, while retaining the supported exclusions for
  casts, reflection, unsafe bytecode, and constructor-bypassing deserialization.
- [ ] 1.6 Audit registry adoption, logical decoding, and heterogeneous result constructors, and add checked-boundary
  coverage proving unknown, mismatched, foreign, and conflicting packed identities fail before a typed carrier is
  returned.

## 2. Primitive Quantity and Grid Transformations

- [ ] 2.1 Remove `Normalize[D]` from private and public exact-type `Quantity[D]` addition and subtraction after the
  explicit-equivalence change, preserving `Quantity[D]` result typing and coefficient semantics.
- [ ] 2.2 Remove `Normalize[D]` from rational scaling and exact nonzero-whole division of an existing `Quantity[D]`,
  including their private helpers and all delegating call sites.
- [ ] 2.3 Remove `Normalize[D]` from same-grid addition, subtraction, integer scaling, and negation of existing
  `GridQuantity[D, G]` values, including private coordinate helpers used by those operations.
- [ ] 2.4 Remove `Normalize[D]` from exact cross-grid addition and subtraction with one shared dimension type and from
  exact nonzero-whole division after witness-backed grid embedding.
- [ ] 2.5 Retain `Normalize[D]` on `Quantity.zero` and `GridQuantity.zero`, and retain complete-expression normalization
  on multiplication, dimensional division, ratios, rates, and dimension/grid witness algebra; add signature assertions
  that distinguish these retained boundaries from the removed contexts.

## 3. Grid Services and Refinement Closure

- [ ] 3.1 Remove redundant `Normalize[D]` contexts from exact grid narrowing, grid constraint validation, and arithmetic
  projection methods that consume a trusted value and reconstruct only through the supplied target `GridRef[D]`.
- [ ] 3.2 Remove redundant normalization from exact-to-grid and grid-to-grid quantization plus constrained encoding,
  preserving target-witness ownership, residual values, policies, and failure behavior.
- [ ] 3.3 Remove redundant normalization from Euclidean quotient/remainder and even-allocation operations, including
  refined quotient/remainder wrappers, while preserving source-grid coordinates and allocation invariants.
- [ ] 3.4 Remove `Normalize[A]` and `Normalize[B]` from checked `Embedding.widenTo`, relying on its retained source and
  target grid witnesses and preserving the selected target dimension and grid types.
- [ ] 3.5 Remove normalization from dimension-preserving `NonNegative`, `Positive`, and `NonZero` quantity/grid
  operations and refined quantization wrappers, while retaining `Normalize[D]` on refined zero manufacture.

## 4. Algebra and Checked Runtime Flows

- [ ] 4.1 Keep `Normalize[D]` on the quantity vector space, grid module, and nonnegative quantity/grid monoids because
  they manufacture zero, while updating their combination and scalar-action implementations to use proof-free
  primitives.
- [ ] 4.2 Remove `Normalize[D]` from positive quantity and grid additive semigroups, and do not introduce competing
  weaker quantity or grid instances to obtain proof-free combination.
- [ ] 4.3 Extend algebra compiler and behavioral tests to show identity-bearing structures remain construction-gated,
  positive semigroups summon for abstract `D`, and strongest-instance coherence is unchanged when normalization is
  available.
- [ ] 4.4 Update checked heterogeneous same-grid and exact cross-grid arithmetic to retype or explicitly `alignTo` the
  selected witness-owned target, then invoke exact-type arithmetic without static normalization evidence.
- [ ] 4.5 Add runtime and persistence tests showing decoded and heterogeneous dependent carriers support same-index
  transformations without `Normalize`, while their values alone reveal no registry owner, runtime key, or grid witness.

## 5. Compiler Contracts, Examples, and Migration

- [ ] 5.1 Replace generic compiler examples with proof-free `Quantity[D]` and `GridQuantity[D, G]` transformations,
  including collection reduction, scaling, movement, projection, quantization, allocation, and refinement-preserving
  operations.
- [ ] 5.2 Add positive compiler coverage showing an index-preserving method body over a hypothetical `Quantity[Bad]` or
  `GridQuantity[Bad, G]` may type-check, paired with negative fixtures proving supported code cannot construct an
  argument for it.
- [ ] 5.3 Replace the superseded malformed-arithmetic and explicit-normalization fixtures from the prerequisite changes,
  and migrate laws, examples, and client call sites away from removed explicit `using Normalize[D]` arguments.
- [ ] 5.4 Update Scaladoc and user documentation with the operation classification, the inductive carrier-inhabitation
  invariant, and the explicit rule that a value supplies neither normalization evidence nor runtime/grid authority.
- [ ] 5.5 Document the breaking source and JVM binary migration, including downstream recompilation, removal of explicit
  normalization parameters for preserving operations, retained zero/result-expression contexts, and explicit alignment
  for cross-spelling equivalence.

## 6. Verification

- [ ] 6.1 Run Scala and SBT formatting for every changed production, test, build, example, and documentation source.
- [ ] 6.2 Run focused quantity, grid projection, quantization, allocation, refinement, algebra, runtime, persistence, and
  compiler-fixture suites, including their law checks.
- [ ] 6.3 Run the immutable-JAR adversarial boundary suite and verify malformed dimensions remain uninhabitable despite
  proof-free index-preserving method bodies.
- [ ] 6.4 Run the full SBT test suite and resolve every intentional signature-migration failure without weakening
  complete-expression normalization or witness authority.
- [ ] 6.5 Re-audit all remaining production `Normalize` contexts against the five operation classes, then run strict
  OpenSpec validation for `freeze-static-dimension-authority`, `demote-same-dimension`, and
  `trust-existing-dimensional-values`.
