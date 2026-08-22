## Why

Static quantity arithmetic currently preserves the syntax of dimension expressions even when the corresponding runtime
`DimensionKey` is already normalized. This prevents ordinary multiplication and division from expressing reusable P&L,
rate, ratio, and other multi-atom calculations with compact result types, and forces special endpoint methods or checked
runtime coercion to recover algebraic equality that is knowable at compile time.

## What Changes

- **BREAKING** Replace raw multiplicative result types such as `Quantity[Times[A, B]]` with statically simplified power
  results that flatten products, combine equal atoms, negate inverse powers, and remove zero powers.
- Represent dimension equivalence modulo power-entry permutation and derive compile-time `SameDimension[A, B]` evidence
  for algebraically equal static dimensions without requiring a global atom ordering.
- Allow exact addition, subtraction, and explicit result alignment to consume trusted dimension-equality
  evidence while avoiding global implicit conversions between quantity types.
- Preserve checked runtime `DimensionKey` comparison as a second trusted source of the same scoped `SameDimension`
  capability for independently resolved witnesses.
- Apply the generalized result typing to exact values produced from grid multiplication, division, and rate application
  while leaving coordinate-level grid identity and closure unchanged.
- Keep rate and ratio conveniences, but require them to agree with the generalized dimensional arithmetic rather than
  being the only way to obtain simplified endpoint types.
- Limit automatic static derivation to complete, substitution-stable inputs. Generic type parameters, term-parameter
  dependent selections, and refinable abstract members forward contextual operation evidence instead of committing
  early equality, inequality, atomicity, or reducibility decisions.
- Treat transparent type aliases as definitionally equal to their right-hand sides for both substitution-stability
  classification and canonical algebra. Alias names never create new static dimension identities and cannot hide generic,
  parameter-rooted, reducible, or malformed structure.
- Treat transparent Scala type annotations as definitionally transparent for static derivation. Derivation follows the
  annotated underlying type through aliases and reducible expressions, canonical outputs omit annotation wrappers, and
  invalid or unresolved underlying structure remains invalid or unresolved.
- Preserve intentionally abstract associated dimension identities only for documented stable witness paths; deferred
  members rooted in parameters or refinable holders remain unresolved even through alias chains.
- Require both a stable term path and a substitution-stable semantic qualifier type before reusing a selected associated
  type automatically. Local rebinding, singleton ascription, or a generic refinement does not make method parameters or
  refinable associated outputs concrete; concrete project-issued identities and concretely exposed operation outputs
  remain supported.
- Make each public operation evidence derivation one atomic compiler operation. Recursive merge, insertion, removal,
  alignment, guard, and token machinery is not caller-composable authority, and every final result is independently
  re-exposed, reclassified, and checked as a canonical power structure.
- Validate signed exponent magnitudes recursively down to `NaturalZero`, conservatively reject reducible/refined factor
  wrappers and the base `Dimension` universe, and issue clean contextual-evidence diagnostics for unresolved generics.
- Define reflexive `SameDimension[D, D]` as structural identity rather than canonical-form certification; distinct static
  equivalence and every canonical operation result remain subject to complete structural validation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `exact-quantity-arithmetic`: Normalize static multiplicative dimensions and support compile-time dimensional-equivalence
  evidence throughout exact arithmetic.
- `quantity-grid-projection`: Return normalized exact dimensions from grid operations that leave a grid.
- `runtime-quantity-identity`: Make checked runtime comparison and static algebraic derivation trusted sources of the same
  restricted dimension-equality capability.

## Impact

The change affects the public static dimension algebra, exact multiplication and division result types, evidence-aware
additive operations, rate conveniences, exact grid-operation result types, `DimRef` construction, and compile-time and
adversarial boundary tests in the `quantities` module. Explicit annotations that depend on raw `Times`/`Inverse` expression
shape will require migration. Runtime coefficients, `DimensionKey` semantics, grid coordinates and provenance, persistence
records, and deferred trading-domain modules remain unchanged. No new external library dependency is required by the
capability contract.
The remediation does not alter runtime coefficients, `DimensionKey`, registry provenance, grid identity, persistence,
refinement, or optional algebra semantics. The active change remains open for independent rereview.
The quantities project's own tests use SBT's ordinary same-project `Compile/classes` relationship. Build-only compiler
fixtures and the adversarial inter-project consumer instead depend on one explicitly keyed, completed
`trading-quantities` main JAR and never consume mutable quantities output directories. The ordinary aggregate task graph
remains in control.
