## 1. Scala-Owned Checked Stable Identifiers

- [ ] 1.1 Characterize `AssetId`, `GridId`, and `GridVersion` valid construction, exact empty/nonpositive failures, null rejection, value equality, hashing, domain-readable display, accessor behavior, and Java-object-serialization rejection.
- [ ] 1.2 Replace the three production Java identifier classes with private-constructor Scala-owned checked values whose companion `from` factories preserve the characterized contract without public `apply`, `copy`, or other unchecked construction.
- [ ] 1.3 Adapt boundary codecs and all supported Scala consumers to parameterless Scala value accessors without changing stable wire representations or exact numeric conversion.
- [ ] 1.4 Update completed-artifact positive and negative Scala fixtures to prove smart-constructor-only creation and retained field observation without asserting generated JVM layout.
- [ ] 1.5 Run focused reference-data, boundary-codec, completed-artifact compiler, serialization, formatting, and production-source inventory checks; confirm no production Java source remains.

## 2. Direct Refined Grid Construction

- [ ] 2.1 Characterize `UniformGrid` and `GridDefinition` refined construction, generative identity, null roots, raw nonpositive rejection, and the absence of authority on failure.
- [ ] 2.2 Remove `UniformGrid.from`, `GridDefinition.from`, and defensive reconstruction of an already established `PositiveRational`; retain direct null checks and required dimension/identity observation.
- [ ] 2.3 Move raw invalid-quantum tests to `PositiveRational` or the owning external reconstruction boundary and retain precise `ExpectedPositive` or `NonPositiveGridQuantum` mapping before grid construction.
- [ ] 2.4 Run focused quantity, reference-data, codec reconstruction, compiler-boundary, serialization, and formatting checks for the direct refined path.

## 3. Direct Catalog Results and Full Integration

- [ ] 3.1 Characterize unchanged and published commit alternatives, transition fields, exhaustive matching, equality, hashing, rendering, null rejection, revision/delta semantics, and model-owned construction.
- [ ] 3.2 Replace hand-written `CatalogCommit` product/extractor/equality machinery with a sealed direct Scala sum of private-constructor structural alternatives while retaining model-issued snapshots and deltas.
- [ ] 3.3 Replace hand-written `CatalogTransition` product/extractor/equality machinery with a private-constructor structural Scala product and preserve internal null rejection and external construction restrictions.
- [ ] 3.4 Update reference-data documentation, downstream runtime/application/codec consumers, and completed-artifact Scala fixtures for exhaustive direct results and the Scala-only quantity/reference-data source boundary.
- [ ] 3.5 Run the clean aggregate test matrix, benchmark compilation, formatting, zero-reflection guard and regression, production Java/source scans, strict Corgi readiness, and final Task Group review.
