## 1. Prove the Scala Type-Level Approach

- [x] 1.1 Add retained compile-only acceptance cases for concrete atoms, abstract generic dimensions, path-dependent
  dimensions, repeated powers, permutation equivalence, and unequal-dimension rejection; confirm the selected
  match-type/typeclass approach supports them before changing public arithmetic signatures.
- [x] 1.2 Implement the project-owned exact signed-natural exponent model, including addition, negation, canonical zero,
  and compile-time tests that prevent wrapped or contradictory exponent results.
- [x] 1.3 Implement left-biased normalized power-sequence operations for identity, visible atoms, statically opaque factors,
  multiplication, inversion, quotient, duplicate-factor combination, and zero-power removal.
- [x] 1.4 Add laws and examples showing that every statically computed power result agrees with the corresponding
  `DimKey` multiplication and inversion whenever both representations are visible.

## 2. Add Static and Runtime Dimension Evidence

- [x] 2.1 Implement permutation alignment for normalized powers and trusted compile-time derivation of
  `SameDimension[A, B]`, including reflexivity, private construction, focused missing-evidence diagnostics, and negative
  compile tests.
- [x] 2.2 Add explicit evidence-checked `asDimension[Target]` alignment for exact and grid quantities without introducing a
  global implicit quantity conversion.
- [x] 2.3 Generalize exact quantity addition and subtraction to accept contextual `SameDimension` evidence and retain the
  left operand's dimension type; verify unequal dimensions still fail to compile.
- [x] 2.4 Preserve `SameDimension.between` and `RuntimeEvidence.sameDimension` as checked runtime proof sources, and test that
  successful locally scoped runtime evidence is accepted by the same arithmetic while mismatches and foreign registries
  issue no proof.

## 3. Normalize Exact Arithmetic and Witnesses

- [x] 3.1 Migrate `DimRef` identity, atomic, product, inverse, and quotient construction to the static normalization
  machinery while preserving authoritative runtime keys and support for opaque fresh runtime dimensions.
- [x] 3.2 Change exact quantity multiplication and checked quantity division to return their simplified associated output
  dimensions while preserving exact coefficients, nonzero-divisor requirements, and lexical result-construction
  boundaries.
- [x] 3.3 Rebase `Rate`, ordinary rate multiplication, `applyRate`, `andThen`, identity rates, `ratioTo`, and cross-rate
  division on the generalized algebra, using evidence-checked alignment for declared endpoint types.

## 4. Propagate Normalization Through Grid Operations

- [x] 4.1 Update grid-by-grid and mixed grid/exact multiplication and grid quantity division to return the same simplified
  exact dimensions as canonical embedding followed by `Quantity` arithmetic.
- [x] 4.2 Rebase grid rate application and exact whole-scalar operations without changing same-grid coordinate closure,
  grid identity, projection, quantization, or registry provenance.
- [x] 4.3 Extend grid and heterogeneous runtime regression tests to confirm normalized exact results interoperate through
  static or checked runtime dimension evidence without weakening `SameGrid` requirements.

## 5. Migrate Callers and Defend the Boundary

- [x] 5.1 Migrate repository type annotations, examples, and law suites that currently assert raw `Times`/`Inverse`
  expression shapes to normalized results or explicit evidence-checked alignment.
- [x] 5.2 Add domain-shaped arithmetic examples without domain abstractions: spot or linear `P × S/P → S`, inverse
  `Q × B/Q → B`, composed cross rates, quotients of rates, dimensionless round trips, and multi-atom surviving powers.
- [x] 5.3 Extend external and same-package adversarial compilation tests so callers cannot forge exponents, normalized
  dimensions, static/runtime equality proofs, or operation results, and cannot add unequal quantities through implicit
  conversion.
- [x] 5.4 Verify optional algebra imports preserve the new primitive result types, coefficient laws, and evidence behavior
  without introducing competing instances or changing grid-coordinate algebra.

## 6. Verify and Validate

- [x] 6.1 Run the complete `quantities` and `adversarial-boundary` test suites, including compile-time negative tests and
  representative proof-search depth cases, and resolve all regressions.
- [x] 6.2 Run project formatting and static checks applicable to the changed Scala and test sources.
- [x] 6.3 Run strict OpenSpec validation for `add-static-dimension-normalization` and confirm all delta requirements have
  corresponding implementation and test coverage.

## 7. Capture Review Regressions

- [x] 7.1 Retain real-source term-path multiplication and quotient cases in which `x.D` and `y.D` would specialize to the
  same `d.D`; prove automatic derivation is rejected until contextual final evidence is supplied and direct `D²`/`One`
  derivation remains canonical.
- [x] 7.2 Retain term-parameter-rooted and stable arbitrary-holder refinable-member inversion cases that resolve to
  `Times[A, B]`; prove automatic derivation is rejected rather than freezing one opaque inverse factor, while contextual
  specialization matches direct inversion.
- [x] 7.3 Compile real positive and negative downstream fixtures with warnings as errors and future-source mode, including
  multiplication, quotient, inversion, `DimRef`, contextual generic operations, and commuted `SameDimension` derivation.
- [x] 7.4 Cover duplicate, zero, non-`Power`, malformed signed-natural, base-`Dim`, reducible, refined, intersected,
  and annotated `Powers` claims across the relevant final evidence boundaries.

## 8. Harden Static Derivation

- [x] 8.1 Implement conservative unresolved classification over complete type and term qualifier paths, refusing method
  type parameters, term-parameter-rooted selections, refinable members/projections, and unresolved magnitudes while
  preserving stable fixed witness paths and clean contextual-evidence diagnostics.
- [x] 8.2 Update generic library helpers and retained downstream examples to accept and forward product, inverse, quotient,
  and alignment evidence.
- [x] 8.3 Replace recursive implicit proof search with atomic final-operation macros and remove public `Guard`, `Token`,
  `InsertPower`, `MergePowers`, `RemovePower`, and `AlignPowers` authority; keep only final contextual evidence façades.
- [x] 8.4 Recursively validate exponent magnitudes, factor irreducibility, tuple shape, uniqueness, nonzero exponents, and
  minimal output representation, then independently revalidate every final automatic result before evidence construction.

## 9. Reintegrate and Verify

- [x] 9.1 Recheck `DimRef`, exact quantity, rate, grid, runtime-evidence, and optional-algebra behavior against revised
  outputs.
- [x] 9.2 Verify downstream compilation has no inaccessible-member diagnostics and audit the source-visible exponent
  compatibility surface.
- [x] 9.3 Run all tests, formatting/static checks, strict OpenSpec validation, and confirm every reopened requirement has
  coverage.

## 10. Remediate Independent Static-Derivation Review

- [x] 10.1 Reproduce the late-alias, refinable inversion, recursive carrier specialization, malformed natural, disguised
  factor, base `Dim`, and generic-diagnostic failures in real downstream source before remediation.
- [x] 10.2 Prove guards and recursive proof carriers cannot be transported or specialized into caller-selected final
  outputs, and prove final `derived` entry points recompute and validate their own canonical output.
- [x] 10.3 Adopt and document reflexive `SameDimension[D, D]` as structural identity without canonical-form certification;
  prove reflexivity cannot turn malformed structure into a canonical operation result.
- [x] 10.4 Retain independent prelude compilation and relevant/forbidden diagnostic assertions for every negative fixture,
  including the absence of macro exceptions, `CyclicReference`, and compiler stack traces.
- [x] 10.5 Complete focused and full validation, repeat the aggregate clean test when practical, audit every public
  derivation occurrence, and stage only the intended remediation without committing or archiving.
- [x] 10.6 Reproduce alias-hidden `x.D`/`y.D`, generic `type X = A`, and concrete `holder.D = Times[A, B]` bypasses in
  isolated downstream compilation before remediation.
- [x] 10.7 Add one lexical/private semantic type-exposure operation with transparent-alias traversal, deferred-member path
  preservation, relevant-wrapper traversal, and cycle/non-progress protection; use it for classification and parsing.
- [x] 10.8 Re-run unresolved classification and independently validate exposed stable irreducible factors at every final
  normalization, product, quotient, inverse, and alignment evidence boundary.
- [x] 10.9 Retain strict downstream alias regressions and definitionally-equal coherence/runtime laws for local aliases,
  generic aliases, concrete term aliases, alias chains, wrappers, stable paths, and contextual forwarding.
- [x] 10.10 Remove unjustified global compile serialization and replace the contextual inverse dummy cast with a supported
  `DimRef` operation; retain no replacement restriction unless a deterministic race is documented.
- [x] 10.11 Complete focused and repeated full validation for the alias, deterministic-product, and annotation
  remediation, then stage all intended changes (`clean test` ×5, separate-process module sequence ×5, and compiler
  fixture suite ×5, all without failure or retry).
- [x] 10.12 Obtain a new independent review of the remediated active change; the implementation agent MUST NOT self-certify
  or mark this task complete.

## 11. Remediate Remaining Independent-Review Findings

- [x] 11.1 Keep quantities' own tests on SBT's normal same-project products; give the adversarial project and standalone
  fixture compiler one explicit dependency on the same completed quantities `packageBin`; retain no exported-product
  override, root sequencing, fixture-fork wiring, serialization, retries, delays, publication, or hard-coded targets.
- [x] 11.2 Strip transparent `AnnotatedType` wrappers in the shared semantic exposure operation, preserve alias traversal
  and unresolved classification, structurally parse exposed natural magnitudes, and omit annotations from canonical
  output without generalizing transparency to refinements, intersections, unions, or match types.
- [x] 11.3 Add atomic, reducible-expression, alias, exponent, definitional-coherence, runtime-agreement,
  malformed-underlying, and strict real-downstream annotation regressions while retaining all prior alias/substitution
  fixtures.

## 12. Remediate Refined-Transport and Aggregate-Graph Review

- [x] 12.1 Reproduce definitional refinement, refinement rebinding, evidence rebinding, locally rebound parameter, and
  malformed transported-output cases; replace term-path-only stability with one private semantic qualifier analysis used
  by unresolved classification, atomic classification, associated-output handling, and final factor validation.
- [x] 12.2 Follow rebound initializer/ascription chains with recursion guards, reject retained generic dependencies, and
  retain strict downstream negative fixtures plus positive concrete operation, concrete rebound, direct stable witness,
  and contextual-forwarding coverage with static/runtime canonical agreement.
- [x] 12.3 Inspect the aggregate task graph, remove the exported-product/package/compile self-cycle, restore normal
  quantities Test products, and bind both external consumers to one completed immutable artifact task.
- [x] 12.4 Complete the final unretried matrices (`clean test` ×5, all six explicit module stages ×5, compiler fixture
  suite ×5), focused/full validation, strict OpenSpec validation, and final staging without committing or archiving.

## 13. Remediate Endpoint Depth and Recursive Term Review

- [x] 13.1 Reproduce concrete associated-endpoint depth laundering and legal typed-local term cycles against the completed
  quantities JAR before remediation, without casts, nulls, or caller-implemented sealed evidence.
- [x] 13.2 Expose exact concrete associated endpoints transitively to a guarded semantic fixed point, reparse each exposed
  endpoint, and use that same operation for derivation/classification and final factor validation while preserving stable
  opaque project witnesses.
- [x] 13.3 Separate active and completed type/term/selection traversal states, reject active recursive term re-entry, cache
  only completed results, and retain completed shared-DAG reuse plus noncyclic concrete rebinding.
- [x] 13.4 Retain strict downstream endpoint-depth, nested-`Powers`, two-/three-node cycle, shared-DAG, definitionally equal
  endpoint-coherence, rebound, static/runtime, and prior transport/alias/annotation/exponent/SameDimension regressions.
- [x] 13.5 Complete direct compiler probes, focused/full tests, formatting, strict OpenSpec validation, and final staging
  without changing the build graph, committing, archiving, or completing independent review.
