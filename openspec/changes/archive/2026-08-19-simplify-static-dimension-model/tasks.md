## 1. Establish the New Type-Inference Contract

- [x] 1.1 Add minimal downstream compile fixtures for singleton-string atoms, concrete multiplication, denominator
  cancellation to a named atom, identity reduction, and commuted `SameDimension` derivation.
- [x] 1.2 Add a generic compile fixture proving that one `Normalize.Aux[Expression, Out]` context can be selected,
  forwarded, and returned as `Quantity[Out]` without an `operation.Out` result type.
- [x] 1.3 Add compile fixtures for literal, object, generative, and runtime-witness singleton keys, plus negative fixtures
  for unresolved key equality and structures outside the closed grammar.

## 2. Introduce the Closed Static Dimension Model

- [x] 2.1 Replace the open dimension representation with sealed `Dimension`, `Dim[Entries]`, `Power[Key, Exponent <: Int]`,
  `Times`, and `Inverse`, and define `Atom`, `One`, and `Divide` as the specified aliases.
- [x] 2.2 Add the restricted `Normalize[D]` façade, its `Aux[D, Out]` refinement, transparent automatic derivation, and
  actionable missing-evidence diagnostics for generic code.
- [x] 2.3 Preserve private construction for `Normalize` and `SameDimension`, and add downstream compilation checks that
  supported callers cannot fabricate either capability.

## 3. Implement the Closed Quoted Normalizer

- [x] 3.1 Implement guarded transparent alias and annotation exposure for the closed `Dim`, `Times`, and `Inverse`
  grammar, with clean cycle and non-progress diagnostics.
- [x] 3.2 Parse canonical `Dim` tuples into ordered singleton-key and `BigInt` exponent entries, rejecting unresolved
  tails, non-`Power` entries, unstable or unresolved keys, nonliteral exponents, zero powers, and duplicate keys.
- [x] 3.3 Normalize nested product, quotient, and inverse expressions by combining type-equal keys in first-occurrence
  order and deleting exact zero results.
- [x] 3.4 Range-check every surviving exponent against `Int`, emit singleton `IntConstant` types, and diagnose
  `Int.MinValue` negation and addition beyond either bound without wrapping.
- [x] 3.5 Reparse and independently validate each emitted `Dim` before constructing final `Normalize` evidence.
- [x] 3.6 Reimplement static `SameDimension` using the same normalizer and key/exponent comparison modulo tuple order,
  while retaining separate reflexive identity semantics.
- [x] 3.7 Add focused macro tests for aliases, annotations, nested expressions, cancellation, tuple permutation,
  malformed canonical claims, abstract keys, and compiler-error hygiene.

## 4. Migrate Dimension Witnesses and Runtime Agreement

- [x] 4.1 Add authoritative constructors for statically named singleton-key atoms and expose generative atomic witnesses
  with a concrete `Atom[this.type]`-equivalent associated dimension.
- [x] 4.2 Represent `DimRef.fresh` runtime-only keys as opaque singleton-key atoms without exposing or guessing their
  runtime decomposition.
- [x] 4.3 Rewrite `DimRef.times`, `inverse`, and `divide` to return one transparent `Normalize` dependent output while
  continuing to compute values through `DimensionKey` multiplication and inversion.
- [x] 4.4 Update registry, asset, resolved-dimension, and runtime-evidence integrations for the new witness aliases while
  preserving ownership checks and scoped `SameDimension` recovery.
- [x] 4.5 Add static/runtime agreement tests for named atoms, generative atoms, products, inverses, cancellation, opaque
  runtime composites, and runtime `BigInt` exponents outside the static range.

## 5. Migrate Exact Quantity and Rate Arithmetic

- [x] 5.1 Rewrite quantity multiplication, checked division, and inversion consumers to use transparent dependent outputs
  with `Normalize`, while generic callers constrain the same evidence with `Aux` and return `Quantity[Out]`.
- [x] 5.2 Keep addition and subtraction on restricted `SameDimension` and retain explicit checked retagging only for
  intentionally equivalent tuple orders or runtime-recovered equality.
- [x] 5.3 Rewrite ratio construction so concrete equal-dimension division returns `Ratio` directly without quotient or
  alignment evidence at the call site.
- [x] 5.4 Rewrite rate construction, application, identity, composition, and cross-rate operations so their endpoint APIs
  return `Quantity[To]` or `Rate[From, To]` directly without caller-supplied alignment repair.
- [x] 5.5 Add caller-shape tests matching the proposed BTC/USD/EUR examples, including direct notional calculation,
  composed conversion, invalid addition, and incorrect-target compile failures.

## 6. Migrate Grids, Refinements, and Optional Algebra

- [x] 6.1 Update exact/grid mixed multiplication and division to share the new normalized exact result types without
  introducing grid-specific dimension evidence.
- [x] 6.2 Update grid construction, projection, and grid-evidence signatures that mention the removed static operation
  evidence while preserving grid identity and coordinate semantics.
- [x] 6.3 Update checked refinements and nonzero division paths for the new concrete and generic quantity result types.
- [x] 6.4 Update optional exact algebra instances and law fixtures to request or forward only `Normalize` and
  `SameDimension` where mathematically necessary.
- [x] 6.5 Re-run focused quantity, rate, grid, refinement, runtime, and algebra law suites after each consumer layer is
  migrated.

## 7. Remove the Legacy Static Algebra

- [x] 7.1 Migrate all repository atom declarations, explicit result annotations, examples, and test aliases from arbitrary
  `Dimension` subtypes and `Powers` to singleton-key `Atom`, `Dim`, and literal `Int` powers.
- [x] 7.2 Remove `Natural`, signed-natural exponent types, `Powers`, `NormalizedPowers`, `DimensionProduct`,
  `DimensionInverse`, `DimensionQuotient`, `DimensionAlignment`, and their companion APIs.
- [x] 7.3 Delete associated-output fixed-point exposure, arbitrary factor classification, recursive term-path traversal,
  and other macro machinery made unreachable by the closed singleton-key grammar.
- [x] 7.4 Replace obsolete adversarial fixtures with smaller boundary fixtures for malformed `Dim`, unforgeable evidence,
  unresolved generic keys, explicit overflow, and downstream warning-free derivation.
- [x] 7.5 Verify by source search and public API compilation that no legacy proof carrier, signed-natural encoding, or
  specialized operation evidence remains accessible or referenced.

## 8. Documentation and Final Validation

- [x] 8.1 Update module documentation and examples to explain singleton-key identity, literal integer powers, canonical
  tuple order semantics, generic `Normalize.Aux`, static range failures, and the runtime opaque-key boundary.
- [x] 8.2 Format all changed Scala, test, build, and documentation sources with the repository formatting tasks.
- [x] 8.3 Run the focused static-dimension suites and real downstream compiler fixtures with the repository warning policy.
- [x] 8.4 Run the complete multi-module test suite, including clean and downstream-JAR consumer paths, and resolve every
  regression without restoring a removed evidence family.
- [x] 8.5 Run strict OpenSpec validation for `simplify-static-dimension-model` and confirm every specified positive,
  negative, overflow, runtime-agreement, and caller-shape scenario has executable coverage.

## 9. Completed-Change Review Remediation

- [x] 9.1 Make nominal singleton keys own one fixed runtime atom identity so a caller cannot provide contradictory
  singleton-key and `AtomId` authority.
- [x] 9.2 Remove the package-visible opaque witness bridge and adopt safe generative witnesses only inside the registry's
  private implementation.
- [x] 9.3 Add direct endpoint-oriented cross-rate division while retaining canonical generic `divideBy` output.
- [x] 9.4 Require complete-expression `Normalize` evidence at endpoint result boundaries, including application,
  composition, ratio cancellation, and cross-rate division.
- [x] 9.5 Flatten complete expressions before globally combining powers so first-occurrence order is independent of
  `Times` association.
- [x] 9.6 Add standalone downstream/JAR fixtures for every reviewed positive, negative, authority, malformed-shape,
  overflow, generic-forwarding, legacy-removal, and opaque-runtime case.
- [x] 9.7 Run the complete required validation matrix and reconcile the Git index to the reviewed implementation.

## 10. Independent-Review Blocking Remediation

- [x] 10.1 Reproduce the widened literal-`ValueOf` and widened nominal-key authority failures against the packaged public
  API, then harden both `DimRef.atom` constructors with the sole public `Normalize` capability and dependent nominal
  key identity.
- [x] 10.2 Replace permissive singleton-key validation with an explicit concrete-key structural whitelist; reject
  `Singleton`, `Nothing`, `Null`, broad intersections, bounds, non-term references, and unresolved wrappers while
  preserving literal, nominal, stable local/module, generative atomic, fresh runtime, alias, and annotation keys.
- [x] 10.3 Require and forward `Normalize[D]` across all dimension-preserving `Quantity`, `GridQuantity`, allocation,
  quantization, projection/constraint/encoding, embedding, refinement, and optional arithmetic-algebra boundaries without
  changing `SameDimension` reflexivity or adding redundant operand evidence to complete-expression operations.
- [x] 10.4 Add registered downstream negative fixtures for widened keys, malformed canonical keys, malformed
  dimension-preserving arithmetic, refined arithmetic, and arithmetic instances, plus positive generic-forwarding and
  static/runtime authority fixtures compiled from the packaged JAR with warning and compiler-hygiene assertions.
- [x] 10.5 Update the active proposal, delta specification, and design with authoritative atom-key construction, the
  canonical singleton-key grammar, and arithmetic-boundary validation semantics; do not sync or archive the change.
- [x] 10.6 Run standalone packaged-JAR probes for both widening defects, malformed keys, and malformed arithmetic, plus
  the supported executable static/runtime authority example, and record their exact exit results.
- [x] 10.7 Run focused suites, full clean multi-module validation, documentation/dependency checks, formatter checks,
  strict change/all OpenSpec validation, legacy-surface searches, and final Git/index reconciliation.
- [x] 10.8 Obtain a fresh independent review of the fully staged remediation and its validation evidence; this task is
  intentionally left incomplete for the independent reviewer and must not be self-certified.
