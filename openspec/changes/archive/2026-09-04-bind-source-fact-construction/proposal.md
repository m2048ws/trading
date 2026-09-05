## Why

Source-fact construction currently makes callers repeat the same execution lifecycle and its dependent dimensions for
each authoritative event, even when a whole normalization path targets one immutable order. RFC-0008/S-03 requires a
lifecycle-bound entry point that makes these common calls concise without weakening checked construction, exact result
types, or source-authority semantics.

## What Changes

- Add one immutable execution-lifecycle-owned source-fact scope covering acceptance, rejection, fill, correction,
  bust, effective cancellation, reconciliation checkpoint, source-order completion, and source-order absence.
- Preserve each fact owner's current checked construction as the single validation implementation, including complete
  deterministic `SourceFactViolations`, precise fact result types, and runtime identity, target, grid, ordering,
  reference, checkpoint, and completeness checks.
- Keep generic/direct fact-owner construction available, and prove a bound scope cannot make a statically compatible
  foreign value trusted by association.
- Add focused behavioral, reuse/concurrency, completed-artifact positive and negative compiler, dependency, and full
  repository regression evidence for the bound API.
- Keep command, dispatch, state transition, replay, observation, effective-ledger, codec, application, and runtime
  responsibilities outside the scope.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `actual-execution-lifecycle`: Add lifecycle-bound construction for all nine source-fact forms while preserving
  authoritative execution semantics, validation, purity, and the existing one-way module boundary.

## Impact

- Production code: `trading-execution-lifecycle`, principally lifecycle and source-fact construction APIs.
- Verification: focused execution-lifecycle tests and completed-artifact Scala compiler fixtures in the adversarial
  boundary suite.
- Compatibility: existing direct fact-owner constructors, source-fact algebra, errors, serialization policy, and
  dependency coordinates remain unchanged; no new runtime, codec, effect, or external dependency is introduced.
