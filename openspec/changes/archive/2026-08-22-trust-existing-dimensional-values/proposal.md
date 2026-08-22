## Why

Generic code that only transforms an already-existing `Quantity[D]` or `GridQuantity[D, G]` still has to forward
`Normalize[D]`, even though supported public construction has already established that the carried dimension is valid.
After freezing construction authority and making cross-spelling alignment explicit, the value boundary can carry this
invariant so ordinary dimension-preserving code no longer repeats type-validity evidence.

## What Changes

- Establish a supported-caller invariant: every normally returned public `Quantity[D]` and `GridQuantity[D, G]` has a
  valid closed dimension index `D`, because all raw construction, witness, registry, and decoding paths are sealed or
  checked.
- Preserve the capability separation: possessing a value does not materialize `Normalize[D]`, `DimRef[D]`,
  `DimensionKey`, `GridRef[D, G]`, or registered identity; operations may only rely on the construction invariant when
  they preserve an existing dimension index.
- **BREAKING** Remove contextual `Normalize[D]` from dimension-preserving exact and grid arithmetic, whole-scalar
  division, refinement operations, grid projection and quantization, allocation, constrained encoding, and other
  transformations rooted in existing trusted values or an authoritative target witness.
- Keep `Normalize[D]` when a value is manufactured from the type alone, including quantity, grid, and refined zeros.
  Keep normalization of the complete expression whenever arithmetic computes a new type-level result dimension.
- Treat explicit alignment or selection of a target already validated by `SameDimension`, `GridRef`, checked grid
  evidence, or runtime registry evidence as an authority-bearing transition rather than a fresh normalization request.
- Audit algebra by capability: structures that manufacture an identity retain normalization, while structures that only
  combine existing trusted values do not. Do not introduce competing weaker instances merely to avoid an identity
  requirement.
- Replace operation-local malformed-dimension rejection with construction-boundary rejection. A hypothetical method over
  `Quantity[Bad]` may type-check, but supported public code cannot obtain a normally returned `Quantity[Bad]` value.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `exact-quantity-arithmetic`: Make valid dimension indexing an invariant of publicly constructed carriers, remove
  redundant normalization from transformations that preserve an existing index, and classify refinement and algebra
  requirements by whether they manufacture a value.
- `quantity-grid-projection`: Trust existing grid values and witness-owned reconstruction across same-grid arithmetic,
  projection, quantization, allocation, encoding, and checked grid relationships without weakening grid provenance.
- `runtime-quantity-identity`: Require registry adoption, checked decoding, and heterogeneous recovery to remain trusted
  construction roots whose returned values can undergo dimension-preserving transformations without static evidence.

## Impact

The affected API surface spans `Quantity`, `GridQuantity`, refinements, grid projection and quantization, allocation,
checked grid evidence, runtime heterogeneous operations, optional algebra instances, examples, and compiler-boundary
tests. Removing contextual parameters changes JVM signatures and breaks source that passes `Normalize` explicitly, even
though ordinary call sites become simpler. Static result typing, `Normalize` derivation, `DimRef`, runtime keys,
registries, carrier representations, packed records, and wire behavior are unchanged. This change is downstream of
`freeze-static-dimension-authority` and `demote-same-dimension` and intentionally supersedes their operation-local
normalization requirements while retaining their construction and equivalence boundaries.
