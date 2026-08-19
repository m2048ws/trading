## 1. Prerequisite and Public Arithmetic Surface

- [ ] 1.1 Complete or reconcile `freeze-static-dimension-authority`, and verify that all dimension-preserving arithmetic
  boundaries independently require `Normalize[D]` before narrowing their signatures.
- [ ] 1.2 Replace value-level `Quantity.asDimension` and `GridQuantity.asDimension` with `alignTo`, preserve coefficient,
  grid type, and coordinate exactly, and expose no `asDimension` compatibility alias.
- [ ] 1.3 Narrow the private and public `Quantity` addition and subtraction signatures to one dimension parameter and one
  `Normalize[D]` context, removing arithmetic consumption of `SameDimension` and `Normalize[E]`.
- [ ] 1.4 Narrow `GridQuantity.addExact` and `subtractExact` to one dimension parameter while retaining distinct grid
  parameters and `Normalize[D]`.
- [ ] 1.5 Audit same-grid, refinement-preserving, and optional algebra addition and subtraction; keep or adjust each to the
  exact-type homogeneous shape without adding equivalence evidence.

## 2. Explicit Equivalence Boundaries

- [ ] 2.1 Preserve `GridQuantity.exactlyEquals` and `compareExact` as explicit cross-spelling operations using
  `SameDimension[D, E]`, and add coverage for equivalent and rejected non-equivalent dimensions.
- [ ] 2.2 Update checked heterogeneous exact addition to recover evidence from the right runtime dimension to the left,
  call `alignTo` on the right exact embedding, and invoke exact-type homogeneous addition.
- [ ] 2.3 Migrate generic helpers, path-dependent runtime tests, and arithmetic laws that currently install
  `SameDimension` for addition so they explicitly align to their selected result type first.
- [ ] 2.4 Verify production uses of `SameDimension` are limited to derivation/recovery, explicit alignment, controlled
  coercion internals, and intentional equivalence-aware comparison rather than homogeneous arithmetic signatures.

## 3. Compiler and Behavioral Contracts

- [ ] 3.1 Add positive compile-time coverage showing generic `Quantity[D]` and `GridQuantity[D, G]` homogeneous arithmetic
  requires `Normalize[D]` but no `SameDimension` vocabulary.
- [ ] 3.2 Add negative compiler fixtures showing direct addition, subtraction, `addExact`, and `subtractExact` reject
  distinct-but-equivalent static dimension types, plus positive counterparts that use correctly oriented `alignTo`.
- [ ] 3.3 Extend malformed-dimension fixtures so reflexive `SameDimension[Bad, Bad]` and reflexive `alignTo[Bad]` do not
  make quantity, grid, refinement, or algebra arithmetic compile without `Normalize[Bad]`.
- [ ] 3.4 Add boundary coverage for `alignTo` preserving exact values and grid coordinates, rejecting missing or unequal
  evidence, and for the removal of the value-level `asDimension` operation.
- [ ] 3.5 Preserve runtime mismatch and registry-provenance tests, and verify successful runtime recovery returns the same
  dependent result types and exact coefficients after explicit alignment.

## 4. Documentation and Migration

- [ ] 4.1 Update Scaladoc and user guidance to present `Normalize` as arithmetic validity, `SameDimension` as explicit
  equivalence, and `alignTo` as the visible cross-spelling transition.
- [ ] 4.2 Migrate every example and downstream fixture from value-level `asDimension` to `alignTo`, without renaming the
  unrelated runtime witness accessor `asDimensionRef`.
- [ ] 4.3 Document the breaking source and JVM binary migration, including exact-type `total` and explicitly aligned
  `totalEquivalent` examples with source-to-target evidence orientation.

## 5. Verification

- [ ] 5.1 Run Scala and SBT formatting checks for all touched production, test, build, and documentation sources.
- [ ] 5.2 Run targeted quantity, grid, refinement, algebra, runtime, example, and compiler-boundary test suites.
- [ ] 5.3 Run the adversarial boundary tests and confirm downstream code cannot regain hidden alignment or forge proof
  capabilities.
- [ ] 5.4 Run the full SBT test suite and resolve all source-compatibility failures caused by the intentional API break.
- [ ] 5.5 Run strict OpenSpec validation for `freeze-static-dimension-authority` and `demote-same-dimension`, and confirm
  both authority separation and explicit-equivalence contracts remain coherent.
