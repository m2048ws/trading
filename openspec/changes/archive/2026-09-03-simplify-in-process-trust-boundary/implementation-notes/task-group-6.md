# Task Group 6 — Execution identity, authority, ordering, and lineage

## Implementation

Execution identity values now use ordinary companion construction after their
existing null, blank, or non-negative representation checks. Qualified targets,
events, orders, fills, streams, and positions likewise construct directly only
after deterministic missing-value validation.

Ordering origins, continuations, authoritative positions, checkpoints, and
completeness evidence use narrow static constructors. The two constructors
owned by a different companion use `private[execution]`; owner-local
constructors remain `private`. Exact-runtime-class guards were removed from the
sealed continuation and ordering families while their exhaustive alternatives
and scope/continuation checks remain intact.

Lineage diagnostics use one package-visible non-empty construction operation,
and valid links are built directly after distinct order, lineage, instrument,
definition, confirmed-cancellation, and successor-submission checks. No
reflective recovery cast remains in these owners.

Hostile constructor and subclass assertions were removed only for the migrated
values. Positive checked Scala clients, representation validation, equality and
hash behavior, deterministic errors, exhaustive alternatives, and Java
serialization rejection remain covered. The repository guard now reports 363
reviewed migration tokens in later owner groups.

## Verification and automated review

| Check | Result |
| --- | --- |
| Focused execution identity, authority, and lineage tests | Pass: 21 tests. |
| Completed-artifact execution boundary suite | Pass: 9 checks, including ordinary checked clients and dependency/JAR boundaries. |
| `tools/check-in-process-reflection.sh` | Pass: 363 reviewed migration tokens remain; no regression. |
| Group source scan and `git diff --check` | Pass: no dynamic access, `private[this]`, invocation recovery, or exact-class guard remains in the four owners. |

The structured review covered invariant ownership, visibility, non-empty
diagnostics, identity and stream qualification, continuation rules, lineage
predicates, closed alternatives, equality/hash behavior, serialization policy,
and fixture replacement. No critical or important finding remains. This review
changed no file itself and is not canonical whole-change Verify or Human Review.
