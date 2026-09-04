## Why

Instrument-specific risk calls currently repeat path-dependent position and settlement types even when every operation
uses the same already assembled instrument. An owner-local immutable scope can retain that relationship once and make
downside measurement, model construction, and exhaustive sizing concise without weakening the risk module's checked
identity, dimension, grid, monotonicity, coverage, or evaluation-failure semantics.

## What Changes

- Add one pure, immutable risk scope bound to an exact instrument, with local aliases for the instrument's position and
  settlement dimensions and the risk values derived from them.
- Expose scoped downside measurement; checked single-assessment, affine, piecewise, and complete-table model
  construction; and explicitly arbitrary exhaustive lot sizing through thin delegates to the existing canonical risk
  operations.
- Preserve the deliberately broad existential inputs and runtime validation of `single` and complete-table
  construction, including deterministic foreign-identity, dimension, grid, coordinate, coverage, and monotonicity
  failures.
- Keep model-to-model combinators and model-bound maximum-affordable selection on their existing owners rather than
  turning the scope into a general risk facade.
- Add focused equivalence, validation, reuse, complexity, dependency, and completed-artifact compiler coverage for the
  concise positive calls and incompatible callbacks and budgets.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `position-risk-sizing`: Add an instrument-bound pure convenience boundary while preserving the existing risk
  mathematics, typed failures, exhaustive traversal, constructive model guarantees, ownership, and complexity bounds.

## Impact

- Affects the public Scala API and tests of the `trading-risk` artifact and completed-artifact compiler fixtures in the
  adversarial boundary.
- Does not change risk mathematics, wire formats, execution behavior, dependencies, effects, or the JDK 25 baseline.
- Current direct risk entry points remain the canonical implementation surface or thin compatibility delegates, with
  no duplicated validation or calculation.
