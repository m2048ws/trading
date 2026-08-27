## 1. Capability Separation Fixtures

- [x] 1.1 Add a positive downstream compiler fixture showing that a concrete stable singleton key can normalize and support
  `Quantity.zero[D]` without any public `DimRef[D]` authority source.
- [x] 1.2 Add a negative downstream compiler fixture showing that successful `Normalize[Atom[K]]` cannot be used to
  construct, infer, or summon `DimRef[Atom[K]]` for an otherwise unsupported stable key.
- [x] 1.3 Add paired generic compiler fixtures proving that `DimRef[D]` alone does not satisfy dimension-preserving
  arithmetic and that explicitly forwarding `Normalize[D]` does.
- [x] 1.4 Extend malformed-dimension coverage so reflexive `SameDimension[Bad, Bad]` is available in the fixture prelude
  while zero, addition, scaling, and other arithmetic still fail for lack of `Normalize[Bad]`.
- [x] 1.5 Add static-equivalence coverage showing that `SameDimension[A, B]` permits only controlled retagging and exposes
  no `DimRef` or `DimKey` authority for either side.

## 2. DimRef Authority Boundary

- [x] 2.1 Audit every public `DimRef` root (`one`, literal atom, nominal atom, atomic, and fresh) and verify that its static
  type and runtime key cannot be selected independently through supported Scala source.
- [x] 2.2 Extend repeated-construction tests for literal, nominal, atomic, and fresh witnesses so the same publicly
  inhabitable atom type is observed with exactly one runtime key.
- [x] 2.3 Retain and strengthen downstream rejection fixtures for widened literal keys, widened nominal keys, direct
  `DimRef` implementation, and any constructor shape that accepts an independently chosen static type and runtime key.
- [x] 2.4 Extend witness-algebra tests across identity, product, inverse, quotient, cancellation, and association to compare
  each normalized static output with the corresponding exact `DimKey` operation result.
- [x] 2.5 Close any authority bypass found by the audit at the narrow constructor boundary, without reducing unrelated
  `Normalize` key support or introducing another public evidence family.

## 3. Public Contract Documentation

- [x] 3.1 Update Scaladoc for `Normalize`, `SameDimension`, `DimRef`, and `Quantity` to state their independent roles and
  explicit non-implications.
- [x] 3.2 Update the quantities README with the four-capability model, the partial-domain authority rule, and examples of
  both publicly inhabited atoms and normalized-but-uninhabited stable keys.
- [x] 3.3 Audit public examples and authority terminology so no documentation describes `Normalize` as producing,
  resolving, or guaranteeing a runtime witness.

## 4. Verification

- [x] 4.1 Run Scala formatting for all changed production and test sources and verify the repository formatting checks.
- [x] 4.2 Run the focused static-normalization, authority, and immutable-JAR downstream compiler suites with warnings
  treated as errors.
- [x] 4.3 Run the complete `quantities` and `adversarialBoundary` test suites and verify that no registry, grid, runtime-key,
  or expression-result behavior regresses.
- [x] 4.4 Run strict OpenSpec validation for `freeze-static-dimension-authority` and resolve every reported artifact or
  scenario issue.
- [x] 4.5 Obtain a fresh independent review of the fully staged change and its validation evidence; this task is
  completed only during finalization after fresh approval and must not be self-certified.
