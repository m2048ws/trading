## 1. Portfolio and Baseline Gate

- [ ] 1.1 Confirm Proposal 0 and all dependent architectural proposals are complete and have passed the portfolio
  coherence review; do not start production edits before that gate is satisfied.
- [ ] 1.2 Refresh Git, OpenSpec, source, and build state and run a clean baseline compile/test so out-of-band changes are
  not mistaken for this migration.
- [ ] 1.3 Reconcile the accepted implementation order with the catalog and boundary-codec proposals, recording that this
  change's relocated synchronized registry is an unreleased bridge and its removed logical packing is intentionally
  restored only by the codec change.

## 2. Artifact and Dependency Boundary

- [ ] 2.1 Add the `referenceData` SBT project in `reference-data/`, publish it as `trading-reference-data`, make it depend
  only on `quantities`, and add it to the root aggregate.
- [ ] 2.2 Give reference data a completed immutable external-artifact task and update economics and adversarial-boundary
  dependencies/classpaths so public-source fixtures consume packaged quantity, reference-data, and economics JARs.
- [ ] 2.3 Add module-boundary compiler checks proving `trading-quantities` compiles without reference data and its
  packaged API exposes no asset, stable-grid, registry, or packed-record type.
- [ ] 2.4 Replace the generic `typelevelVersion` build variable with independent `catsVersion` and `algebraVersion`
  coordinates, map Cats Core/Kernel/Laws and Algebra/Algebra Laws to their owning coordinates, and verify the resolved
  dependency versions are unchanged by this structural migration.

## 3. Stable Reference-Data Identity and Handles

- [ ] 3.1 Move `AssetId`, `GridId`, `GridVersion`, and dimension-local `GridKey` to `trading.reference`, add the explicit
  full `GridIdentity` product, and preserve guarded empty/null/version construction behavior.
- [ ] 3.2 Move asset and grid definitions plus reference-data conflict/error vocabulary to the new artifact while keeping
  `AtomId`, `DimKey`, `DimRef`, and all mathematical numeric types in quantities.
- [ ] 3.3 Implement sealed, non-forgeable `DimensionHandle[D]`, path-dependent `Asset`, and `GridHandle[D]` capabilities
  with an implementation-private immutable lineage token and no public arbitrary-attachment constructor.
- [ ] 3.4 Make `GridHandle` retain one exact `GridRef.Grid[D, G]` and delegate coordinate construction, coordinate
  observation, exact embedding, dimension, and quantum behavior without duplicating mathematical state.
- [ ] 3.5 Extend the project-owned fail-closed serialization boundary to authority-bearing reference-data handles and add
  focused identifier, handle, null-input, and Java-serialization tests.
- [ ] 3.6 Add immutable-JAR adversarial fixtures proving downstream and same-package source cannot implement trusted
  handles, choose lineage, or attach a stable identity to an arbitrary dimension or grid.

## 4. Anonymous Mathematical Grids

- [ ] 4.1 Remove stable ID, version, and key members from `GridRef`; change `UniformGrid.create` to accept only
  `DimRef[D]` and `PositiveRational` while preserving a fresh generative `G` per successful construction.
- [ ] 4.2 Refactor `SameGrid` to compare private anonymous grid identity and coherent mathematical definition, retain
  `SameQuantum` and `Embedding` as dimension/quantum relationships, and remove stable-definition conflicts from the
  quantity `GridError` hierarchy.
- [ ] 4.3 Make `NotOnGrid` and related projection/constraint diagnostics identity-neutral, and remove
  `GridCoordinateEncoding` and `ConstrainedGridEncoding` from the quantity artifact.
- [ ] 4.4 Migrate exact embedding, projection, quantization, allocation, refinement, and algebra implementations to the
  anonymous witness without changing coefficients, residuals, coordinates, error ordering, or laws.
- [ ] 4.5 Add law/property and compiler tests proving fresh equal-definition grids are not `SameGrid`, may be
  `SameQuantum`, preserve exact embedding/quantization laws, and expose no stable-ID factory overload.

## 5. Reference-Data Evidence and Transitional Construction

- [ ] 5.1 Move the current synchronized registry implementation into the reference-data artifact as the explicit
  unreleased construction bridge, adapting its interned values to issue `Asset`, `DimensionHandle`, and `GridHandle`
  without otherwise redesigning registration or lookup semantics.
- [ ] 5.2 Replace mutable-registry-object provenance with the dedicated immutable lineage token while preserving canonical
  issuance, foreign-lineage rejection, and reference-data definition-conflict behavior.
- [ ] 5.3 Implement pure checked handle reconciliation for same dimension and same stable grid, delegating only after
  lineage/full-identity checks to the restricted quantity evidence required for safe retyping.
- [ ] 5.4 Add tests distinguishing foreign lineage, stable identity mismatch, dimension mismatch, immutable-definition
  conflict, same quantum, anonymous same-grid identity, and successful canonical-handle reconciliation.
- [ ] 5.5 Verify ordinary handle reads and evidence checks require no live registry lookup and expose no registration,
  mutation, synchronization, or effect capability through the handles themselves.

## 6. Runtime Quantity Cleanup and Downstream Migration

- [ ] 6.1 Remove asset, stable-grid, registry, and registered heterogeneous-value ownership from
  `trading.quantity.runtime` while retaining `DimKey`/`DimRef`, checked `SameDimension`, authoritative runtime rates, and
  carrier-construction trust.
- [ ] 6.2 Change economics build/imports and definition components from `AssetRef`/`RegisteredDimensionRef`/
  `RegisteredGridRef` to `Asset`/`DimensionHandle`/`GridHandle` without changing instrument identity or exact economic
  formulas.
- [ ] 6.3 Adapt validated instrument construction, market conversions, and fee denominations to pure handle evidence and
  foreign-lineage errors while preserving current deterministic validation staging and successful results.
- [ ] 6.4 Update economics unit/property tests, examples, fixtures, and compiler probes for the new handle vocabulary,
  including wrong-lineage and wrong-dimension failures and unchanged typed-rate/PnL results.
- [ ] 6.5 Search production and test sources to ensure economics receives no registry/construction mechanism and no
  quantity source imports `trading.reference`.

## 7. Remove Premature Packing and Reconstruction APIs

- [ ] 7.1 Remove `PackedAssetGridQuantity`, `PackedGridQuantity`, registry-backed decoding, and their dependent decoded
  registered-grid packages from the quantity artifact without adding aliases or a replacement wire promise.
- [ ] 7.2 Remove or relocate tests and examples that assert the obsolete logical record shape, retaining only quantity
  construction-boundary tests that belong below the future codec.
- [ ] 7.3 Verify no production module treats `GridVersion` as a schema version and record every required restored
  persistence/replay behavior against the accepted boundary-codec proposal.

## 8. Documentation and Canonical Ownership

- [ ] 8.1 Update quantity and reference-data READMEs, Scaladoc, examples, and package documentation to explain anonymous
  grids, stable handles, mathematical versus stable evidence, and the one-way artifact dependency.
- [ ] 8.2 Update the canonical purposes for `runtime-quantity-identity` and `quantity-grid-projection` so they no longer
  claim asset, registered-grid, or packed-codec ownership after delta-spec synchronization.
- [ ] 8.3 Update stable `.agent` architecture/invariant references only where the approved change supersedes current
  ownership wording; retain the explicit Proposal 2 and Proposal 9 follow-on obligations.

## 9. Verification and Independent Review

- [ ] 9.1 Run Scala and SBT formatting for every changed source and verify `scalafmtCheckAll`, `scalafmtSbtCheck`, and
  `git diff --check` pass.
- [ ] 9.2 Run focused quantity laws, reference-data tests, economics tests, and immutable-JAR adversarial/compiler suites,
  including every new positive and negative authority fixture.
- [ ] 9.3 Run `sbt -batch clean test` and confirm every aggregate project passes from a clean build with the new artifact
  graph.
- [ ] 9.4 Run strict validation for this change and `openspec validate --all --strict`, then inspect the complete diff for
  accidental catalog, instrument-assembly, economics-split, or codec design beyond this proposal.
- [ ] 9.5 Stage exactly the intended implementation, tests, build, documentation, and active-change artifacts in a
  validated commit-ready worktree without committing unless separately authorized.
- [ ] 9.6 Obtain fresh independent review of the fully staged implementation and validation evidence; remediation must
  return to another fresh independent review before finalization, and completion must record the unreleased dependency
  on the catalog and boundary-codec follow-on changes.
