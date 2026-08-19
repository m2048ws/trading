# Trading Agent Workflow

This file defines how the project steward orchestrates OpenSpec implementation, independent review, remediation, archival, and escalation.

The steward owns workflow transitions.

Worker agents do not decide workflow state for themselves.

---

# Core Principle

The normal lifecycle is:

```text
OpenSpec change
    ↓
apply
    ↓
independent review
    ↓
    ├── READY ───────────────→ finalize/archive
    │
    └── BLOCKED
            ↓
        classify findings
            ↓
        implementation defect?
            ↓ yes
        remediate
            ↓
        fresh independent review
```

A remediation agent never transitions directly to archival.

A reviewer never edits the repository.

An implementation/remediation agent never certifies its own independent review.

---

# Workflow States

The steward must classify the repository into exactly one primary workflow state.

## NO_ACTIVE_CHANGE

No active OpenSpec change is in progress.

Possible next actions:

* explore a design question;
* propose a new OpenSpec change;
* inspect repository health;
* do nothing.

The steward must not invent an implementation task without an active change unless the user explicitly requests direct work outside OpenSpec.

---

## EXPLORING

A design question is unresolved.

Examples:

```text
Should SameDimension be removed from homogeneous arithmetic?

Should existing Quantity[D] values be trusted without Normalize[D]?

Should Normalize become implementation-private?
```

Allowed actions:

* launch OpenSpec explore agents;
* compare alternatives;
* inspect client ergonomics;
* identify affected invariants;
* recommend whether to propose a change.

Not allowed:

* silently implement one alternative;
* modify settled invariants;
* treat exploration conclusions as accepted decisions.

Exit conditions:

```text
human/steward decides not to proceed
```

or:

```text
an OpenSpec proposal is created
```

---

## PROPOSED

An OpenSpec change exists but implementation has not completed.

The steward should read:

* proposal;
* design;
* tasks;
* delta specs;
* relevant settled decisions/invariants.

If the change is ready to implement, launch an apply agent.

Transition:

```text
PROPOSED
    ↓
APPLYING
```

---

## APPLYING

A dedicated implementation agent is applying the active OpenSpec change.

Preferred worker:

```text
$openspec-apply-change
```

or the project's equivalent apply workflow.

The apply agent may:

* edit implementation;
* edit tests;
* edit active OpenSpec artifacts where required by discovered implementation detail;
* run validation;
* stage intended changes.

The apply agent may not:

* complete the independent-review task;
* archive;
* commit;
* declare independent approval.

When implementation reports completion, the steward must independently inspect current repository state sufficiently to determine whether review can begin.

Transition:

```text
APPLYING
    ↓
AWAITING_REVIEW
```

---

## AWAITING_REVIEW

Implementation or remediation is complete and staged.

A fresh independent reviewer must be launched.

The reviewer should receive:

* `.agent/project.md`;
* `.agent/invariants.md`;
* `.agent/decisions.md`;
* this workflow;
* actual active OpenSpec artifacts;
* current repository/source state;
* relevant claimed implementation results.

The reviewer should not receive implementation reasoning as authoritative truth.

Implementation reports are claims to falsify.

Transition:

```text
AWAITING_REVIEW
    ↓
REVIEWING
```

---

## REVIEWING

A fresh reviewer performs read-only validation.

The reviewer's primary objective is:

> Attempt to falsify the active change's claimed invariants using supported public behavior.

The reviewer may:

* inspect code;
* inspect Git/OpenSpec state;
* run tests;
* compile downstream fixtures;
* create temporary read-only probes;
* inspect public API artifacts;
* assess ergonomics;
* compare static/runtime behavior.

The reviewer may not:

* edit source;
* stage files;
* complete tasks;
* archive;
* commit.

The reviewer returns:

```text
READY
```

or:

```text
BLOCKED
```

with concrete evidence.

If READY:

```text
REVIEWING
    ↓
READY_TO_FINALIZE
```

If BLOCKED:

```text
REVIEWING
    ↓
FINDINGS_CLASSIFICATION
```

---

# Findings Classification

Every blocking review finding must be classified by the steward before launching remediation.

The classifications are:

```text
IMPLEMENTATION_DEFECT
SPEC_MISMATCH
DESIGN_CONFLICT
PROCESS_FAILURE
INFRASTRUCTURE_FAILURE
```

---

## IMPLEMENTATION_DEFECT

Definition:

The active OpenSpec semantics are coherent, but implementation fails to satisfy them.

Examples:

```text
Normalize accepts Null despite the spec rejecting it.

A public constructor permits widened atom authority.

A dimension-preserving arithmetic method forgot its required validity check.

A compiler negative fixture misses an ordinary Scala alias path.
```

Action:

```text
launch remediation agent
```

Then:

```text
REMEDIATING
    ↓
AWAITING_REVIEW
```

---

## SPEC_MISMATCH

Definition:

Implementation and active OpenSpec artifacts disagree, but the intended design is already clear from the active change and settled decisions.

Examples:

```text
Implementation follows the intended design, but one delta spec still describes old behavior.

A task claims a validation step completed when the configured check currently fails.
```

Action:

If the mismatch can be corrected without changing the design:

```text
remediation agent may update active OpenSpec + implementation/tests as needed
```

Then return to independent review.

If correcting the spec would materially alter the intended API or invariant, reclassify as DESIGN_CONFLICT.

---

## DESIGN_CONFLICT

Definition:

A sound fix appears to require changing an invariant, API contract, or design choice that the active change did not authorize.

Examples:

```text
The only sound implementation requires SameDimension to mean validity.

Fixing arithmetic requires Quantity to store DimRef at runtime.

A blocker can only be solved by changing normalized result typing.

The remediation would implement a PROPOSED or EXPLORING decision.
```

Action:

**Stop the apply/remediation loop.**

Do not launch a normal remediation agent.

Transition:

```text
FINDINGS_CLASSIFICATION
    ↓
DESIGN_ESCALATION
```

The steward should:

1. identify the conflicting invariant/decision;
2. explain why the active change cannot soundly resolve it as written;
3. launch OpenSpec exploration if appropriate;
4. request human/design approval before changing direction.

---

## PROCESS_FAILURE

Definition:

The implementation may be correct, but required project process state is objectively incomplete.

Examples:

```text
formatting check fails;
unstaged source changes remain;
independent-review task was self-certified;
OpenSpec task claims are unsupported;
archive happened before review approval.
```

Action:

Use a narrowly scoped remediation/finalization correction.

Do not redesign implementation.

Return to review if the process failure could conceal code/spec changes.

---

## INFRASTRUCTURE_FAILURE

Definition:

Failure occurred before or outside the repository task being evaluated.

Examples:

```text
SBT launcher cannot obtain a user-level lock;
agent tool session terminates;
temporary environment lacks required permissions.
```

Action:

Record separately.

Do not convert it into a repository defect.

Retry is permitted only when:

```text
the repository task itself never began
```

A repository task that begins and fails must not be silently retried away.

---

# REMEDIATING

A remediation agent receives:

* active OpenSpec change;
* project invariants;
* settled decisions;
* exact blocking findings;
* relevant regression obligations;
* current Git state.

Its instruction is:

> Repair the findings with the smallest sound change while preserving all previously established invariants.

A remediation agent may:

* edit source;
* edit tests;
* edit active OpenSpec artifacts;
* add retained regression fixtures;
* stage intended changes;
* run validation.

It may not:

* archive;
* commit;
* complete independent review;
* silently implement a future proposed/exploratory decision.

Every remediation ends at:

```text
AWAITING_REVIEW
```

Never:

```text
READY_TO_FINALIZE
```

---

# Fresh Review Requirement

A review after remediation must use a fresh review context.

Do not ask the remediation agent:

```text
"Now independently review your fix."
```

The reviewer should not inherit the remediator's private reasoning or confidence.

It may receive the remediation completion report as a set of claims to verify.

---

# Review Severity Policy

Default classification:

## Critical

Always blocking.

Examples:

* trusted evidence can certify contradictory static/runtime semantics;
* malformed representations obtain core authority;
* public construction breaks foundational invariants.

## High

Always blocking.

Examples:

* public API soundness failure;
* caller can violate runtime/static identity;
* generic specialization produces incorrect trusted types.

## Medium

Blocking by default.

Examples:

* an arithmetic boundary violates an active spec;
* build nondeterminism affects supported repository validation;
* meaningful generic-client functionality contradicts intended API.

The steward may downgrade only with explicit rationale and human approval.

## Low

May or may not block archival.

However, any low finding that corresponds to a configured required check is blocking.

Example:

```text
scalafmtCheckAll fails
```

is blocking regardless of severity label.

---

# Review Loop Limits

Automation must not loop forever.

Track:

```text
review_cycle_count
finding_signature
affected_invariant
```

Default limits:

```text
maximum review/remediation cycles per active change: 5

same invariant failing after 3 remediation cycles:
    stop and escalate to design review
```

The steward may stop earlier if fixes repeatedly move authority boundaries.

A repeated sequence like:

```text
review finds authority hole
→ fix moves authority
→ review finds new authority hole
→ fix adds another proof family
```

is a signal for architectural exploration, not infinite patching.

---

# Regression Preservation

Every remediation review must verify:

1. the newly reported finding is closed;
2. previously fixed findings remain closed;
3. positive supported ergonomics remain intact.

The steward should maintain a compact list of active regression classes for the current change.

Example:

```text
current regression obligations:
- widened literal atom key rejected
- widened nominal atom key rejected
- broad canonical singleton keys rejected
- malformed arithmetic rejected
- concrete literal/nominal/generative keys remain supported
```

This list is change-specific and should be refreshed from active OpenSpec/tests rather than permanently added to project invariants.

---

# Scope Drift Detection

Before launching review, and again before finalization, the steward should inspect the staged diff.

If the active change concerns:

```text
SameDimension arithmetic ergonomics
```

but the diff also substantially changes:

```text
runtime registry semantics
grid provenance
build architecture
public dimension grammar
```

the steward must flag scope drift.

Possible outcomes:

```text
explained and necessary
separate into another OpenSpec change
revert unintended drift
escalate to design exploration
```

Do not accept unrelated architectural changes merely because tests pass.

---

# READY_TO_FINALIZE

This state is reached only after a fresh independent review returns READY.

Before finalization, verify:

```text
independent review is the sole remaining process gate
configured checks are green
repository state is understood
no unresolved review finding exists
```

Then launch a separate finalization agent or perform the documented finalization workflow.

---

# FINALIZING

The standard pre-release finalization order is:

```text
1. Complete the independent-review task.

2. Confirm all active OpenSpec tasks are complete.

3. Strict-validate the active OpenSpec change.

4. Archive the change using the normal OpenSpec workflow.

5. Inspect archive-generated changes.

6. Run post-archive validation.

7. Stage the archive-generated changes.

8. Inspect final Git/index state.

9. Hand off for commit or commit if explicitly authorized.
```

Archive normally occurs before the final commit.

---

# Post-Archive Validation

At minimum, run the repository's configured equivalents of:

```text
OpenSpec strict validation
full clean tests
formatting checks
Git diff checks
final status inspection
```

For this repository that commonly includes commands equivalent to:

```bash
openspec validate --all --strict

sbt -batch clean test
sbt -batch scalafmtCheckAll
sbt -batch scalafmtSbtCheck

git diff --check
git status --short
```

Use current repository configuration rather than treating these exact command spellings as immutable.

A post-archive failure blocks commit.

---

# COMMIT_READY

A change is commit-ready only when:

```text
OpenSpec change archived
post-archive validation green
all intended changes staged
no unexpected unstaged/untracked files
Git diff checks clean
```

Automatic committing should be separately configurable.

Default steward policy:

```text
do not commit unless explicitly authorized
```

---

# Current-State Refresh

At the beginning of every steward run, do not trust stored task/test/Git counts.

Refresh volatile state from the repository.

At minimum inspect:

```text
Git HEAD
staged state
unstaged state
untracked state
active OpenSpec changes
OpenSpec task status
active proposal/design/spec/tasks
build/module topology relevant to active work
```

Stored project overview and decisions provide context.

The repository provides current truth.

---

# Worker Report Trust Model

Worker reports are not authoritative repository state.

Treat statements such as:

```text
"all tests pass"
"52/53 tasks complete"
"no unstaged files"
"the bypass is closed"
```

as claims.

The steward or next independent reviewer should verify load-bearing claims directly where practical.

---

# Prompt Construction Rules

## Apply prompt

Include:

* active change identity;
* project overview;
* relevant invariants;
* relevant settled decisions;
* current Git/OpenSpec state;
* apply authority and prohibitions.

## Review prompt

Include:

* active change identity;
* relevant invariants;
* claimed implementation outcomes;
* required falsification targets;
* read-only prohibition;
* required verdict format.

Avoid prescribing the implementation fix before a finding is reproduced.

## Remediation prompt

Include:

* exact findings;
* affected invariants;
* concrete reproductions;
* smallest-remediation guidance from review;
* prior regression obligations;
* active-change scope.

Do not include unrelated future proposals.

## Finalization prompt

Include only:

* approved review result;
* task completion;
* archive;
* post-archive validation;
* Git/index reconciliation.

Finalization is not another design/implementation phase.

---

# Steward Decision Rule

At every transition ask:

> Is the next action implementing the accepted design, verifying it, repairing a concrete implementation defect, or changing the design itself?

Route accordingly:

```text
implement accepted design
    -> apply/remediate

verify
    -> fresh independent review

repair implementation defect
    -> remediate then review

change design
    -> stop and explore/propose
```

When uncertain, prefer stopping for design classification over silently widening the active change.
