## Context

See `proposal.md` for motivation. `CommandTransition` currently combines `CommandTransitionKind` with
`Option[CommandViolations]`; both lifecycle command paths inspect the kind and call `.get` for rejection. Replay and
observation order commands, dispatch evidence, source facts, modifiers, conflicts, continuations, anomalies, and
diagnostics through composite `String` keys assembled with `|` and `-`, although identifier values may contain those
characters. `ExecutionState.observe` derives an effective-fill ledger and `ExecutionAnomalies.derive` independently
derives it again.

The implementation stays in the pure `trading-execution-lifecycle` owner. Its existing identity, source-fact,
economic, ordering-authority, exact-exposure, and Java-serialization-rejection contracts remain authoritative.

## Goals / Non-Goals

**Goals:**

- Make every command transition state representable and exhaustively consumable without unchecked extraction.
- Define one reusable, owner-local lexicographic ordering vocabulary over typed execution components.
- Preserve delivery-order-independent replay, diagnostic stability, and exact lifecycle observations for all existing
  evidence.
- Derive effective fills once along the observation path and pass the result to anomaly derivation.

**Non-Goals:**

- Converting the broader submission, cancellation, effective-fill, evidence, anomaly, ledger, and observation models
  to direct Scala enums or case classes; RFC-0005 Slice 3 owns that representation cleanup.
- Changing command/source-fact factories, identity/refinement validation, non-empty wrappers, lifecycle authority,
  external formats, Java compatibility, dependencies, or runtime effects.
- Assigning semantic authority to network delivery order, timestamps, rendering, or incidental collection order.

## Decisions

### 1. Use an owner-sealed transition hierarchy with payload-correct alternatives

Replace the kind-plus-option product with a sealed `CommandTransition` hierarchy whose alternatives each carry the
resulting `CommandState`; only the rejected alternative additionally carries `CommandViolations`. Command and dispatch
recording construct these alternatives directly, and lifecycle transition handling uses exhaustive pattern matching.

This makes the invalid combinations unrepresentable now while deliberately leaving the broad direct-derived-model
conversion to Slice 3. Retaining a kind field or adding a helper that reconstructs an optional violation was rejected
because either would preserve the parallel representation that caused the unchecked extraction.

### 2. Centralize typed execution orderings under the lifecycle owner

Introduce one `private[execution]` ordering vocabulary for targets, qualified identifiers, stream positions,
continuations, source ordering, commands, dispatch evidence, source facts, fill modifiers, conflicts, diagnostics, and
post-cancellation anomalies. Each comparison uses an explicit alternative rank followed by lexicographic comparison of
the typed value's semantic components; null input used by checked replay rejection receives an explicit lowest rank.

Replay, `EffectiveFillLedger`, and `ExecutionAnomalies` reuse these orderings through typed `Ordering` values or
domain-named comparison operations. String rendering remains presentation only. Keeping separate string-key helpers
was rejected because escaping or changing delimiters still erases component boundaries and duplicates ordering logic.

### 3. Treat equality and ordering consistency as a boundary invariant

Every ordering key includes all fields that participate in the corresponding value's structural equality and uses no
receipt order, timestamp, hash code, or `toString` tie-breaker. Focused fixtures use distinct identifier component
sequences that the old concatenation mapped to the same string, plus permutations of the same evidence set, to prove
the new comparisons remain total and delivery-order independent.

This is preferable to accepting an arbitrary stable insertion order: conflicts and diagnostics are public derived
evidence, so deterministic order must follow semantic data rather than the order in which evidence arrived.

### 4. Pass the derived ledger into anomaly construction

`ExecutionState.observe` remains the orchestration owner: it derives `EffectiveFillLedger` once, supplies it to
`ExecutionAnomalies.derive`, and places both results in the observation. Anomaly derivation keeps ownership of
cancellation ordering and post-cancellation classification but no longer reconstructs effective fills.

Focused tests retain all existing correction, bust, unresolved-reference, overfill, and cancellation-race assertions
and additionally prove that a non-empty overfill value in the anomaly result is the value supplied by the observation
ledger. A cache or mutable memoization was rejected because the data flow is already pure and explicit parameter
passing states the dependency directly.

## Risks / Trade-offs

- **[Ordering omits a semantic field]** → Compare each ordering against the value's equality definition, cover every
  closed alternative exhaustively, and add delimiter-collision plus permutation regressions before replacing callers.
- **[Transition API changes downstream matches]** → Update the execution module and completed-artifact Scala fixture in
  the same Task Group, with exhaustive matching and preserved state/classification assertions.
- **[Slice 2 creates churn before Slice 3]** → Limit the temporary sealed hierarchy to command transitions required by
  AC-004; defer every other representation conversion to the next accepted Slice.
- **[Typed comparisons add hot-path overhead]** → Use allocation-light direct comparisons/tuple keys over already
  retained fields and keep the existing bounded replay complexity; run the aggregate test and benchmark compilation
  gates.

## Migration Plan

Implement and verify one buildable Task Group at a time: total transitions first, typed ordering second, and
single-pass observation derivation last. No stored data or runtime deployment migration exists. Rollback is the
ordinary reversal of the current Task Group before its Corgi acknowledgement; acknowledged groups remain preserved by
the Run Contract.
