## Why

The current static dimension API exposes a recursive signed-natural exponent encoding and several overlapping
associated-output evidence types, forcing callers and the macro implementation to carry complexity that is unnecessary
for the project's greenfield API. A closed canonical representation with ordinary Scala literal exponents can make the
common API direct while retaining exact, dimension-safe arithmetic and checked runtime identity.

## What Changes

- **BREAKING** Replace source-visible natural-number exponent types with `Int` singleton literals in canonical
  `Power[Key, Exponent]` entries.
- **BREAKING** Define atomic dimensions and the multiplicative identity as aliases of one canonical `Dim[Entries]`
  representation, with atom identity carried by a stable singleton key rather than by an arbitrary `Dimension` subtype.
- **BREAKING** Replace `NormalizedPowers`, `DimensionProduct`, `DimensionInverse`, `DimensionQuotient`, and
  `DimensionAlignment` with one closed `Normalize[Expression]` operation and retain `SameDimension` only as the
  restricted equivalence capability.
- Make concrete quantity, dimension-reference, rate, and grid arithmetic expose canonical result dimensions directly,
  without caller-visible `operation.Out`, alignment evidence, or routine `asDimension` repair.
- Perform exponent arithmetic as macro-local `BigInt`, then either emit a checked `Int` singleton exponent or fail
  compilation explicitly when the canonical result is outside the static range. Runtime `DimensionKey` exponents remain
  arbitrary-precision `BigInt` values.
- Keep runtime-resolved dimensions type-safe by assigning each opaque runtime witness a concrete singleton-key atom type;
  hidden runtime decompositions continue to require checked runtime equivalence before retagging.
- Make every accepted static atom key authoritative: literal construction validates its key through `Normalize`, nominal
  construction binds its result to the supplied stable value's singleton type, and one accepted static key therefore
  cannot carry two different runtime `AtomId` values.
- Treat `Key <: Singleton` as a necessary bound rather than canonical certification. Normalization accepts only literal
  constants and concrete stable term/module or supported generative `this` identities after transparent exposure, and
  rejects `Singleton`, `Nothing`, `Null`, broad intersections, bounds, non-term references, and unresolved wrappers.
- Require `Normalize[D]` at every dimension-preserving arithmetic boundary, including quantity, grid, allocation,
  quantization, refinement, and optional arithmetic-instance APIs, while retaining identity-only
  `SameDimension[D, D]` and valid source expressions such as `Rate[F, T]`.
- Deliberately keep the dimension algebra integer-powered. Fractional roots and rational exponents remain outside this
  change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `exact-quantity-arithmetic`: Replace the static dimension representation, exponent contract, operation evidence, and
  concrete arithmetic result-shape requirements while preserving exact coefficients and dimension safety.

## Impact

- Replaces the public static dimension model in `trading.quantity` and substantially rewrites
  `StaticDimension.scala`, `Dimension.scala`, and `DimensionRef.scala`.
- Updates quantity, rate, grid, optional algebra, and runtime-identity integrations to use the single normalization
  boundary.
- Requires repository callers, examples, compile fixtures, and tests to migrate from signed naturals, `Powers`, and the
  specialized operation evidence types to singleton keys, `Dim`, literal `Int` powers, and `Normalize`.
- Does not change rational coefficient semantics, persisted `DimensionKey` data, asset/grid identifiers, or wire formats.
- Introduces no new runtime dependency.
