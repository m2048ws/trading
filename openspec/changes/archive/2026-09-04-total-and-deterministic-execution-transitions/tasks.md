## 1. Total Command Transition Results

- [ ] 1.1 Confirm the effective RFC/source binding, exact `origin/main` planning baseline, isolated worktree identity,
  pilot authority, closed native dependencies, and clean focused execution-lifecycle baseline.
- [ ] 1.2 Extend `CommandStateSuite` and lifecycle transition coverage to characterize applied, idempotent,
  command-conflict, dispatch-conflict, null rejection, scope rejection, and reference rejection results, including their
  exact retained state and violation semantics.
- [ ] 1.3 Replace `CommandTransitionKind` plus optional violations with an owner-sealed total transition hierarchy in
  which every alternative carries its resulting command state and only rejection carries `CommandViolations`.
- [ ] 1.4 Update command recording, dispatch observation, and lifecycle transition handling to construct and
  exhaustively match the new alternatives; remove every production unchecked violation extraction without changing
  work accounting or conflict retention.
- [ ] 1.5 Update completed-artifact Scala compiler fixtures, API-shape checks, equality/serialization rejection
  assertions, and focused execution-lifecycle tests for the revised transition boundary.

## 2. Typed Deterministic Execution Ordering

- [ ] 2.1 Introduce an owner-local typed ordering vocabulary with explicit alternative ranks and lexicographic semantic
  component comparison for execution targets, qualified identifiers, positions, continuations, commands, dispatch
  evidence, source facts, modifiers, conflicts, diagnostics, and post-cancellation anomalies.
- [ ] 2.2 Replace execution replay, continuation, unsequenced-event, and diagnostic composite string keys with the typed
  orderings while preserving checked null rejection, authoritative source ordering, and existing stable results.
- [ ] 2.3 Replace effective-fill modifier/conflict and cancellation-anomaly composite string keys with the shared typed
  orderings, covering every closed fact and modifier alternative without `toString`, hash, timestamp, or delivery-order
  tie-breakers.
- [ ] 2.4 Add collision fixtures whose identifier components contain `|` and `-` in arrangements that formerly encoded
  to the same key, and prove replay, ledger, anomaly, continuation, and diagnostic equality across input permutations.
- [ ] 2.5 Run focused execution replay, effective-fill, cancellation, submission, and compiler-boundary tests and verify
  production ordering no longer concatenates composite string keys.

## 3. Single-Pass Observation Derivation and Integration

- [ ] 3.1 Change anomaly derivation to consume the effective-fill ledger supplied by `ExecutionState.observe`, leaving
  cancellation ordering and post-cancellation classification in their existing pure owner.
- [ ] 3.2 Add focused evidence that observation overfill/anomaly construction reuses the supplied ledger value and
  preserves exact correction, bust, unresolved-reference, conflict, cancellation-race, buy/sell exposure, and overfill
  results under existing focused and property tests.
- [ ] 3.3 Run the complete execution-lifecycle and adversarial compiler-boundary suites, the zero-reflection guard and
  regression fixture, formatting checks, strict OpenSpec/Corgi validation, and the clean aggregate test plus benchmark
  compilation gate used by CI.
- [ ] 3.4 Inspect the completed diff and automated AC evidence to confirm no broad Slice 3 representation conversion,
  checked-factory weakening, identity/source-authority change, Java-compatibility removal, wire change, dependency, or
  runtime effect entered this slice.
