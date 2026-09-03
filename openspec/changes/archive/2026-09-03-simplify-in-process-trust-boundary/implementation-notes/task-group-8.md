# Task Group 8 — Execution facts, state, replay, and effective fill ledger

## Implementation

Source-fact violations and all nine fact alternatives now use direct companion
construction after their existing qualified identity, lifecycle, target, grid,
and economics checks. Source classifications, conflicts, unresolved modifier
references, transition results, and evidence state are assembled directly by
the source-evidence owner without changing duplicate, conflict, stream
position, or unresolved-reference indexes.

Lifecycle state, diagnostics, observations, accepted/rejected transitions,
replay results, and the effective-fill ledger now use direct typed
construction. Correction and bust replacement, ambiguity and conflicting
evidence retention, exact signed exposure, overfill, post-cancellation
anomalies, completeness diagnostics, deterministic replay, and indexed work
accounting remain unchanged. Lifecycle construction retains one cast directly
after `create` has checked the order, intent, and lots instrument identities;
the cast strengthens the associated instrument dimensions and does not recover
a reflective result.

Runtime exact-class guards and hostile same-package constructor/subclass
fixtures were removed for the migrated sealed families. Finality, exhaustive
matching, factory validation, structural equality and hashing, Java
serialization rejection, replay permutations, correction/bust behavior, and
ordinary checked Scala clients remain covered. A new ordinary Java client
exercises the public execution identity and target factories. The source guard
now reports 76 reviewed migration tokens, all owned by the boundary-codec group.

## Verification and automated review

| Check | Result |
| --- | --- |
| `executionLifecycle/test` | Pass: all 66 lifecycle unit, property, replay, race, permutation, correction/bust, anomaly, and complexity tests. |
| Completed-artifact execution boundary suite | Pass: 7 dependency, JAR, ordinary Scala/Java client, and supported negative-boundary checks. |
| `boundaryCodecs/compile` | Pass against the migrated execution lifecycle. |
| `tools/check-in-process-reflection.sh` | Pass: 76 reviewed codec migration tokens remain; no regression. |
| Group source scan and `git diff --check` | Pass. |

The structured review covered validation ownership, qualified provenance,
exact economics, deterministic conflict and replay order, completeness,
effective-fill alternatives, exposure and cancellation anomalies, complexity,
visibility, exhaustive families, serialization, and fixture replacement. No
critical or important finding remains. The review changed no file, no finding
was human-triaged, and it is not canonical whole-change Verify or Human Review.
