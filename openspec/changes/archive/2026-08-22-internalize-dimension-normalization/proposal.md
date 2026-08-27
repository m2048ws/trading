## Why

The remaining public `Normalize[D]` and `Normalize.Aux[D, O]` capability is used primarily to compute canonical result
types, but the representative application, generic-library, algebra, and runtime-discovery cases do not require that
public computation. They are better served by expression-preserving arithmetic, semantic endpoint operations,
`SameDimension`-checked alignment, and authoritative `DimRef` construction, leaving normalization as internal
validation and equivalence machinery rather than ordinary client vocabulary.

## What Changes

- **BREAKING** Remove `Normalize` and `Normalize.Aux` from the public API. Keep the closed dimension grammar and its
  compile-time normalization engine as library-private machinery for static validation and non-reflexive
  `SameDimension` derivation.
- **BREAKING** Make generic dimension-changing quantity and grid arithmetic preserve source expressions directly:
  multiplication returns `Times[A, B]`, dimensional division returns `Divide[A, B]`, and witness algebra returns the
  corresponding `Times`, `Inverse`, or `Divide` expression rather than a compiler-selected canonical output type.
- Preserve semantic endpoint typing without public proofs: rate construction, identity, application, reciprocal,
  composition, cross-rate calculation, and ratio calculation return their named endpoint types directly. Provide an
  authoritative endpoint constructor that works with statically declared and runtime-resolved `DimRef` values.
- Preserve explicit canonical-spelling transitions through `alignTo[E]` and `SameDimension[D, E]`. Generic downstream
  code may retain expression types or nominate its own output type with `SameDimension`; the compiler no longer infers
  an independently named canonical `O` through public associated-output evidence.
- **BREAKING** Require `DimRef[D]`, or a stronger matching witness such as `GridRef[D]`, when manufacturing a value from
  no existing trusted carrier. Replace normalization-based quantity, grid, refinement, and algebraic zero authority
  with runtime-inhabitation authority.
- Keep public atom-key restrictions and malformed-representation rejection inside authority-bearing constructors.
  Removing the public proof type must not widen `DimRef` atom construction, make reflexive `SameDimension` certify
  validity, or allow malformed `Canonical` representations to acquire values or arithmetic through supported APIs.
- Do not add instrument-domain types. Runtime-discovered instrument and conversion use cases remain motivating boundary
  fixtures for the quantities and runtime APIs; instruments, orders, positions, and venue adapters remain deferred.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `exact-quantity-arithmetic`: Remove public canonical-output evidence, preserve expression result types, move empty
  construction to `DimRef` authority, and keep endpoint-oriented rate APIs and explicit alignment ergonomic.
- `quantity-grid-projection`: Apply expression-preserving result typing to exact operations leaving a grid and replace
  normalization-based grid zero authority without weakening grid identity or projection rules.
- `runtime-quantity-identity`: Allow registry-resolved dimensions to construct and apply endpoint-oriented rates without
  static normalization evidence, while retaining registry provenance and checked equivalence recovery.

## Impact

The affected surface includes the static-dimension macros, `DimRef`, `Quantity`, `Rate`, `GridQuantity`, refinements,
optional algebra instances, runtime heterogeneous flows, documentation, and downstream compiler-boundary fixtures.
Removing a public typeclass and changing dimension-changing result types is source- and JVM-binary-incompatible; client
signatures that forward `Normalize` or expose `Normalize.Aux` outputs must migrate to expression types, explicit
`SameDimension` alignment, or authoritative endpoint APIs. Runtime `DimKey` encoding, exact coefficients, grid
coordinates and identity, registry ownership, packed records, and wire formats remain unchanged. This change follows
`freeze-static-dimension-authority`, `demote-same-dimension`, and `trust-existing-dimensional-values`, and supersedes
their remaining public-normalization requirements while preserving their authority and carrier-trust invariants.
