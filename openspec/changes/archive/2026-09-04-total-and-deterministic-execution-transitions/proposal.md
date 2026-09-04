## Why

Execution transitions currently encode mutually exclusive outcomes with a kind flag plus optional violations, then
recover the expected payload with unchecked extraction. Replay and observation also derive several orderings from
delimiter-concatenated strings and recompute the effective-fill ledger, leaving avoidable invalid states, collision
risk, and duplicated work inside an otherwise pure deterministic lifecycle.

## What Changes

- Replace command-recording and dispatch-observation results with closed total transition alternatives whose rejected
  case necessarily carries non-empty violations.
- Preserve applied, idempotent, command-conflict, dispatch-conflict, and rejection semantics with focused transition
  characterization.
- Replace composite string sort keys with owner-local typed lexicographic orderings across replay, effective fills,
  anomalies, continuation, and diagnostics, including identifiers containing the former delimiters.
- Derive the effective-fill ledger once per lifecycle observation and supply it to anomaly derivation while preserving
  exact exposure and all existing reconciliation results.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `actual-execution-lifecycle`: Strengthen total transition results, deterministic typed ordering, and single-pass
  observation derivation without changing authoritative execution semantics.

## Impact

The change is confined to the pure `execution-lifecycle` production and test sources plus downstream Scala compiler
fixtures that observe its public transition results. It adds no effects, runtime behavior, persistence, codec format,
dependency, or Java-compatibility commitment; broad derived-model conversion remains in RFC-0005 Slice 3.
