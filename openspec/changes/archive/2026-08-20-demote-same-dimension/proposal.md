## Why

Ordinary homogeneous arithmetic currently consumes `SameDimension`, even though both operands can and should already
share one exact Scala dimension type. With the static authority boundaries frozen separately, dimensional equivalence
can now be reserved for explicit transitions between different static spellings instead of remaining hidden inside
routine addition and subtraction.

## What Changes

- **BREAKING** Restrict homogeneous `Quantity` addition and subtraction to operands with the exact same dimension type,
  while continuing to require `Normalize[D]` so malformed dimensions cannot acquire arithmetic.
- **BREAKING** Restrict cross-grid `GridQuantity.addExact` and `subtractExact` to one shared dimension type while still
  allowing different grid types.
- **BREAKING** Replace the controlled retagging name `asDimension` with the explicit boundary operation `alignTo`, with
  no compatibility alias. `alignTo[E]` requires `SameDimension[D, E]` and changes only the phantom dimension spelling.
- Keep `SameDimension` for intentional cross-spelling alignment, checked runtime equivalence recovery, advanced generic
  APIs, and equivalence-aware exact comparison; do not make alignment implicit or introduce a second proof family.
- Keep dimension validity independent: homogeneous arithmetic uses the applicable `Normalize` requirements, while
  `alignTo` remains governed by `SameDimension` and reflexive `SameDimension[D, D]` remains type identity rather than
  validity certification.
- Preserve runtime heterogeneous arithmetic by recovering checked evidence, explicitly aligning to a chosen result
  dimension, and only then invoking exact-type homogeneous arithmetic.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `exact-quantity-arithmetic`: Require exact static dimension types for homogeneous addition and subtraction, and make
  `alignTo` the explicit `SameDimension`-checked transition between equivalent spellings.
- `quantity-grid-projection`: Require one exact dimension type for same-grid and cross-grid additive arithmetic while
  retaining equivalence-aware comparison as an explicit advanced operation.
- `runtime-quantity-identity`: Route recovered runtime equivalence through explicit alignment before homogeneous
  arithmetic rather than allowing arithmetic itself to consume `SameDimension`.

## Impact

The affected public surface is `Quantity`, `GridQuantity`, dimension-preserving refinement wrappers, runtime
heterogeneous arithmetic, optional algebra integration, examples, documentation, and compile-time boundary tests.
Removing generic dimension parameters and contextual `SameDimension` arguments from arithmetic, and renaming
`asDimension`, are source- and JVM-binary-incompatible API changes. Static normalization, expression-result typing,
runtime dimension keys, registries, grid identity, and quantity representation are unchanged. This change is intended to
follow `freeze-static-dimension-authority` and relies on its separation of `Normalize`, `DimRef`, `SameDimension`, and
`Quantity` responsibilities.
