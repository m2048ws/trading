## Why

The simplified static-dimension model is implemented, but its authority guarantees are spread across normalization,
runtime-witness, equivalence, and quantity contracts. Before further ergonomic simplification, the project needs one
stable boundary that prevents static validity from being mistaken for runtime inhabitation or identity authority.

## What Changes

- Define `Normalize[D]` solely as evidence that `D` is a valid closed static expression with a canonical output; deriving
  it does not imply that any `DimRef[D]` exists.
- Define `DimRef[D]` as the authority for runtime inhabitation and `DimensionKey` identity. For every atom type inhabitable
  through supported public `DimRef` APIs, all publicly obtained witnesses for that atom type must carry the same runtime
  identity.
- Preserve the separation of `SameDimension[A, B]` as controlled equivalence evidence: reflexive equality does not
  certify validity, and equivalence alone does not establish runtime inhabitation.
- Define `Quantity[D]` as an exact coefficient indexed by `D`, not as an identity witness. Preserve dimension-polymorphic
  zero for any normalized `D`, while caller-supplied coefficients continue to require `DimRef[D]`.
- Freeze the public static grammar and proof surface around `Dimension`, `Dim`, `Power`, `Atom`, `Times`, `Inverse`,
  `Divide`, `Normalize`, `SameDimension`, and `DimRef`; add focused contract coverage and documentation for the separated
  authority model.
- Do not add a new proof family, require runtime inhabitability for every key accepted by `Normalize`, or redesign
  expression-result typing, grids, registries, runtime keys, or quantity representation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `exact-quantity-arithmetic`: Clarify the independent contracts of static normalization, dimension equivalence, and
  exact values, including the explicit non-implication from `Normalize[Atom[K]]` to `DimRef[Atom[K]]` existence.
- `runtime-quantity-identity`: Define unique runtime authority for publicly inhabitable `DimRef` atom types and require
  public witness algebra to preserve the static/runtime association.

## Impact

The affected surface is the `trading.quantity` static-dimension and witness APIs, their Scaladoc and README guidance, and
downstream compiler-boundary fixtures. No runtime data, `DimensionKey` encoding, registry provenance, grid identity,
quantity representation, dependency, or wire-format migration is intended.
