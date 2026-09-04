## Why

The execution lifecycle's derived results still carry hand-written class, tag, extractor, constructor-forwarding, and
equality machinery even though their owners have already established every semantic predicate. Now that Slice 2 has
stabilized total transitions and deterministic observation behavior, those representation layers can be replaced with
direct Scala 3 sums and structural products without changing execution meaning.

## What Changes

- Replace submission knowledge, cancellation knowledge, effective-fill classification, and source/command transition
  alternatives with exhaustive Scala sums that cannot represent mismatched tags or optional payloads.
- Replace field-valid derived evidence, anomaly, ledger, replay, and observation values with direct structural products
  and generated structural equality.
- **BREAKING (Scala source shape only)**: downstream Scala matches and construction syntax for converted derived values
  may change; their alternatives, public field information, domain meaning, and structural equality remain stable.
- Retain checked factories and typed failures for commands, source facts, lifecycle identity, refinements, non-empty
  wrappers, and external reconstruction.
- Preserve fail-closed Java object-serialization behavior for every converted derived value.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `actual-execution-lifecycle`: Express field-valid derived execution alternatives and observations as direct Scala 3
  sums and products while retaining authoritative lifecycle semantics and guarded invariant-bearing inputs.

## Impact

The change is confined primarily to `execution-lifecycle` production and test sources, plus completed-artifact Scala
fixtures that exhaustively consume the revised derived results. It changes no wire format, schema, persistence,
economic calculation, runtime behavior, dependency direction, Java source compatibility commitment, or checked
construction authority.
