## 1. Build and package foundation

- [x] 1.1 Configure the non-published `trading` root aggregator, the `quantities/` SBT project and
  `trading-quantities` artifact, Scala 3, MUnit, ScalaCheck, Discipline, Typelevel Algebra and its laws, Cats Kernel
  laws, and formatting checks with test-only law dependencies.
- [x] 1.2 Establish only `trading.quantity`, `trading.quantity.grid`, `trading.quantity.refinement`,
  `trading.quantity.runtime`, and `trading.quantity.algebra` as production packages.
- [x] 1.3 Add exact generators and compile-time assertion helpers.

## 2. Exact scalars and dimensions

- [x] 2.1 Implement normalized arbitrary-precision `Rational` arithmetic and strict parsing.
- [x] 2.2 Implement generic sign refinements and scalar aliases for positive rational, whole, nonzero whole, and count values.
- [x] 2.3 Bound eager finite-decimal scale before power-of-ten materialization.
- [x] 2.4 Implement normalized free-abelian-group dimensions with `BigInt` exponents.
- [x] 2.5 Implement generative witnesses and checked canonical `SameDimension` evidence.

## 3. Exact and grid quantity carriers

- [x] 3.1 Implement rational-backed opaque `Quantity[D]` with no representation parameter.
- [x] 3.2 Implement independently opaque `GridQuantity[D, G]` backed by a `BigInt` coordinate.
- [x] 3.3 Add dimension-witnessed exact quantity constructors, zero, coefficient inspection, and lexically private raw attachment.
- [x] 3.4 Add witness-owned grid coordinate construction and inspection, zero, and exact `asQuantity` embedding.
- [x] 3.5 Exclude floating constructors, raw opaque reconstruction, and package-qualified result attachment from supported callers.
- [x] 3.6 Keep arithmetic result helpers lexically private and operand-derived.

## 4. Dimension-safe arithmetic

- [x] 4.1 Implement exact quantity addition, subtraction, rational scaling, multiplication, and checked division.
- [x] 4.2 Implement same-grid closed arithmetic and coordinate ordering.
- [x] 4.3 Implement cross-grid and mixed arithmetic with exact `Quantity` results.
- [x] 4.4 Implement `Rate[From, To]`, `Ratio`, rate application, and rate composition.
- [x] 4.5 Cover exact `0.1 BTC × 60000.01 USD/BTC = 6000.001 USD`.
- [x] 4.6 Prove unlike-dimension operations and implicit grid projection do not compile.

## 5. Division and allocation

- [x] 5.1 Keep exact whole division distinct, and publish witness-bearing Euclidean quotient/remainder and allocation
  extensions from `trading.quantity.grid` rather than core.
- [x] 5.2 Return unrestricted `Quantity[D]` from exact division of a grid quantity.
- [x] 5.3 Preserve Euclidean remainder and reconstruction laws for signed coordinates.
- [x] 5.4 Preserve ordered allocation conservation and one-quantum spread.

## 6. Projection and quantization

- [x] 6.1 Implement same-grid, same-quantum, and global embedding evidence without conflating identity.
- [x] 6.2 Implement exact narrowing from `Quantity[D]` and grid-to-grid narrowing through exact embedding.
- [x] 6.3 Implement explicit total rounding policies with negative tie behavior.
- [x] 6.4 Implement residual-bearing quantization and exact conservation.
- [x] 6.5 Cover cent, satoshi, `0.03`, `6000.001 USD`, and `2/100001 XBT` projection examples.

## 7. Refinements and algebra

- [x] 7.1 Implement generic checked `NonNegative[A]`, `NonZero[A]`, and `Positive[A]` construction.
- [x] 7.2 Implement one final, privately constructible sign capability with only library-owned supported instances.
- [x] 7.3 Implement positive-to-nonnegative/nonzero weakening and positive-`Int` widening without revalidation.
- [x] 7.4 Make scalar refinement names aliases and eliminate duplicated scalar validators and divisor carriers.
- [x] 7.5 Use `NonZero[Quantity[D]]` for division and canonical exact embedding for checked grid divisors.
- [x] 7.6 Construct mathematically closed refinement results directly in their opaque owners without checked
  reconstruction or proof-recovery `.toOption.get`, and weaken or check operations that are not closed.
- [x] 7.7 Make `ExactScalarField` refine the standard commutative ring and make `VectorSpace` refine `LeftModule` over the additive commutative group.
- [x] 7.8 Provide one strongest exact-quantity vector-space instance and one strongest grid integer-module instance without duplicate additive groups.
- [x] 7.9 Provide production dimension and nonzero-rational multiplicative groups, exact orders, and the deliberately closed refined additive structures.
- [x] 7.10 Keep primitive operations below instances, reuse inherited algebra operations internally, and verify initialization, cycle, and import coherence.
- [x] 7.11 Verify every supported production structure with standard laws and reusable exact-field, module, vector-space, graded, rate, canonicality, normalization, projection, quantization, division, allocation, and refinement laws using independent models.
- [x] 7.12 Exclude public field, universal numeric, unsupported ring, unsafe rate category, open sign, unchecked refinement, implicit conversion, and test-only algebra-instance loopholes.

## 8. Runtime evidence and registered grid packing

- [x] 8.1 Implement runtime asset, dimension, and registered-grid witnesses with concrete implementations nested privately in each registry.
- [x] 8.2 Preserve dimension-scoped `(DimKey, GridId, GridVersion)` identity and immutable definitions.
- [x] 8.3 Keep generic `SameGrid` recovery independent of registry provenance, and have
  `RuntimeEvidence.sameGrid` reject foreign registries before ordinary grid compatibility checks.
- [x] 8.4 Implement heterogeneous `ResolvedAssetGridQuantity`, `ResolvedGridQuantity`, and `ResolvedExactQuantity`.
- [x] 8.5 Implement `PackedAssetGridQuantity` and `PackedGridQuantity` only for registered grid coordinates.
- [x] 8.6 Verify expected dimension before grid resolution and preserve historical version checks.
- [x] 8.7 Leave arbitrary exact-quantity packing and wire-schema versioning explicitly deferred.
- [x] 8.8 Reject same-package registered-witness construction and counterfeit-quantum packing while preserving canonical coordinate decoding.

## 9. Documentation and supported boundary

- [x] 9.1 Document exact quantities in the mathematical interior and grid quantities at discrete boundaries.
- [x] 9.2 Document that dimensions and assets do not inherently impose a grid.
- [x] 9.3 Update examples, runtime naming, persistence inventory, and the complete resolved-carrier serialization policy.
- [x] 9.4 Test supported Scala construction boundaries and downstream core, grid, refinement, and runtime same-package
  spoofing.
- [x] 9.5 Keep invariant-bearing Java serialization fail-closed through the common explicit mechanism.
- [x] 9.6 Permanently compile-check the intended external public type shapes, extra arities, and absent alternative names with independently valid preludes and targeted diagnostics.
- [x] 9.7 Apply the common explicit unsupported Java-serialization mechanism to invariant-bearing quantization and allocation result records.
- [x] 9.8 Inventory each public invariant-bearing typed-error family with legitimate representatives and verify the project-owned `NotSerializableException` mechanism.

## 10. Validation and review gate

- [x] 10.1 Run formatting, clean compilation, focused standard and project law suites, unit properties, compile-time checks, and downstream boundary suites.
- [x] 10.2 Run `trading-quantities` packaging and generated documentation, dependency inspection, strict OpenSpec
  validation, and staged-diff checks.
- [x] 10.3 Inspect external compiler probes and public documentation for representation leakage.
- [ ] 10.4 Obtain a fresh independent review of the staged foundation before the initial commit.
