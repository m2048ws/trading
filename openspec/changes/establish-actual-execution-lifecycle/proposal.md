## Why

The repository has immutable order intent and hypothetical execution scenarios, but no domain boundary for facts from
actual execution. Without that boundary, acknowledgements, fills, uncertain submission, source ordering, cancellation,
and replay would be forced into application/runtime mechanisms or collapsed into mutable status, losing authority,
identity, provenance, and exact exposure.

## What Changes

- Add a non-empty pure `trading-execution-lifecycle` artifact under `trading.execution`, depending only on the immutable
  order model and instrument economics required by actual fills.
- Define closed, typed command and source-fact vocabularies with stable application, logical-order, lineage, source,
  account, event, source-order, and fill identities whose authority and uniqueness scopes are explicit.
- Add an immutable lifecycle state and total checked transition that deterministically replays sequenced and explicitly
  unsequenced evidence, retaining duplicates, conflicts, gaps, rewinds, unresolved references, corrections, busts, and
  anomalies without overstating completeness.
- Model submission knowledge as accepted, rejected, proven-not-dispatched, or indeterminate, with same-identity retry and
  explicit reconciliation semantics that do not manufacture missing acknowledgement facts.
- Keep cancel request distinct from effective cancellation, retain partial fills and cancel/fill races as exact exposure,
  and represent confirmed cancel-then-submit only through mechanism-neutral lineage.
- Extend repository boundary requirements and completed-artifact checks so lower and sibling pure modules cannot depend
  on actual execution, and actual execution cannot acquire application, runtime, codec, effects, clients, persistence,
  streams, or telemetry.
- Explicitly exclude native atomic amend/cancel-replace, live venue integration, application capabilities, runtime
  interpreters, durable codecs, multi-order campaigns, accounting, PnL, and risk from this Slice.

## Capabilities

### New Capabilities

- `actual-execution-lifecycle`: Authoritative execution commands and source facts, stable qualified identities, exact
  exposure, deterministic replay/reconciliation, explicit epistemic outcomes, cancellation races, corrections, busts,
  lineage, completeness, conflicts, and anomalies for one immutable order and execution account.

### Modified Capabilities

- `repository-architecture`: Admit the real execution-lifecycle artifact and enforce its one-way dependency boundary
  between immutable orders/instrument economics and later application/runtime/codec consumers.

## Impact

- Adds a new SBT subproject and published artifact, `trading-execution-lifecycle`, with package root
  `trading.execution`.
- Updates root aggregation, module documentation, dependency audits, packaged-JAR/compiler fixtures, and architecture
  documentation for the new boundary.
- Adds pure Scala domain types, checked constructors, transition/replay logic, exact/property/example tests, and
  representative operation-count or benchmark evidence where replay cost is material.
- Does not add a concrete effect runtime, external dependency mechanism, transport, persistence, codec, or public
  application service in this Slice.
