# Task Group 7 — Commands, submission knowledge, and cancellation

## Implementation

Command violations, submit/cancel commands, dispatch evidence, command
conflicts, transitions, and state now use direct typed construction. Companion-
owned representations keep private constructors; values assembled by the
command-state owner use `private[execution]`. Command recording still preserves
body idempotency, conflicting reuse, lifecycle/order/lineage/target scope,
original-submit references, and dispatch-evidence consistency.

Submission evidence and its eight closed knowledge outcomes are constructed
directly by the derivation owner. The derivation still distinguishes pending,
accepted, rejected, proven-not-dispatched, indeterminate, execution-proven,
authoritatively absent, and conflicting evidence, with deterministic non-empty
conflict kinds.

Cancellation evidence, its three knowledge outcomes, and post-cancellation
anomalies now use direct static construction. Source-supported confirmation,
command/source conflict retention, partial-fill and cancellation/fill ordering,
and exact exposure remain unchanged. The one retained cast converts an exact
coordinate through the lifecycle's already checked instrument and position grid;
it is unrelated to reflective result recovery.

Runtime exact-class guards and hostile construction/subclass assertions were
removed for these sealed families. Finality, exhaustive matching, ordinary
factory behavior, deterministic failure, equality/hash behavior, serialization
rejection, and absent native-amend/cancel-replace APIs remain tested. The source
guard now reports 261 reviewed migration tokens in later owner groups.

## Verification and automated review

| Check | Result |
| --- | --- |
| `executionLifecycle/test` | Pass: all 66 execution lifecycle tests, including command, submission, cancellation, lineage, race, replay, permutation, and effective-fill coverage. |
| Completed-artifact execution boundary suite | Pass: 9 dependency, JAR, client, negative-boundary, and remaining closed-family checks. |
| `executionLifecycle/compile` after final visibility tightening | Pass. |
| `tools/check-in-process-reflection.sh` | Pass: 261 reviewed migration tokens remain; no regression. |
| Group source scan and `git diff --check` | Pass. |

The structured review covered command idempotency and recovery, conflict
ordering, evidence provenance, authoritative absence, cancellation semantics,
post-cancellation exposure, retained-cast justification, visibility, exhaustive
families, and fixture replacement. No critical or important finding remains.
This review changed no file itself and is not canonical whole-change Verify or
Human Review.
