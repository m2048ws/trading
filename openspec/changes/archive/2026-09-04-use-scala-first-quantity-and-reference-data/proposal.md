## Why

Quantity and reference data are now consumed through a Scala 3 source boundary, but their implementation still retains three Java-owned identifier classes, raw factories maintained for the retired Java API, and hand-written product/equality machinery for derived catalog results. Removing that residue completes the post-trust-boundary simplification while preserving checked external reconstruction and catalog semantics.

## What Changes

- **BREAKING** Replace Java-owned `AssetId`, `GridId`, and `GridVersion` with Scala-owned checked values that preserve validation failures, value equality, hashing, display, null behavior, and Java-object-serialization rejection.
- **BREAKING** Remove the Java-oriented raw factories from `UniformGrid` and `GridDefinition`; accept an established `PositiveRational` directly without defensive revalidation, while raw external values continue through their owning refinement or reconstruction boundary.
- Replace hand-written `CatalogCommit` alternatives and `CatalogTransition` product machinery with exhaustive direct Scala sums/products while preserving publication, unchanged, revision, delta, lineage, and structural-equality behavior.
- Update completed-artifact Scala fixtures and active quantity/reference-data specifications to describe the Scala-only source contract and retain invariant, conflict, reconciliation, null-boundary, and serialization coverage.
- Remove the final production Java source files from the repository.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `quantity-grid-projection`: Make refined Scala construction the sole domain source entry for anonymous uniform grids and remove the Java-oriented raw factory.
- `runtime-quantity-identity`: Preserve checked runtime witness construction while making its refined Scala grid entry direct and retaining external reconstruction and serialization boundaries.
- `reference-data-identity`: Define Scala-owned checked stable identifiers and direct refined grid-definition construction without Java-owned or defensively revalidated paths.
- `reference-data-catalog`: Represent derived commit and transition observations as direct exhaustive Scala sums/products without changing catalog transition semantics.

## Impact

The change affects production and test sources in `quantities` and `reference-data`, completed-artifact Scala compiler fixtures in `adversarial-boundary`, downstream Scala call sites, and the four active specifications named above. Stable external representations, exact numeric behavior, generative grid identity, catalog authority and lineage, module dependencies, JDK 25, and Java-library integration remain unchanged.
