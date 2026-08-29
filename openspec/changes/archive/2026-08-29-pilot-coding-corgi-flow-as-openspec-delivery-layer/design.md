## Context

OpenSpec is already the repository's requirements and change-artifact system. The current guard plane adds formal
script-backed apply, review, remediation, and finalization workers; serializes primary-worktree writers; validates
structured reports; and reviews an isolated staged snapshot. Coding Corgi Flow is attractive because it keeps OpenSpec's
artifact model while adding an RFC-first lifecycle, isolated delivery worktrees, sequential checked Task Groups,
recovery, GitHub issue tracking, whole-change gates, and archive evidence.

Corgi does not natively supply the required GitHub PR lifecycle. Its automated and human reviews are Corgi evidence, not
GitHub PR reviews, and its Archive transaction normally closes the tracker and removes the worktree after local
materialization. The pilot therefore needs an explicit PR adapter and must reconcile Corgi's commit-oriented lifecycle
with the repository's independent-review and publication boundaries.

The pilot qualifies Corgi under the existing guard plane. It does not treat Corgi installation as proof that the current
workflow can be removed, and it does not migrate an active OpenSpec change into a different lifecycle.

## Goals / Non-Goals

**Goals:**

- Preserve the familiar OpenSpec Explore, specification, Apply, and Archive semantics while evaluating Corgi's stronger
  delivery lifecycle.
- Prove safe parallel execution across isolated changes with exactly one writer and claim per change.
- Exercise Corgi's required local commits and recovery while limiting automatic publication to fast-forward WIP pushes
  and one draft PR.
- Put actionable independent-review findings and human discussion on the PR while retaining structured local evidence.
- Bind Verify, independent review, Human Review, QA, final PR approval, and merge to explicit Git tree identities.
- Integrate GitHub delivery with Corgi's resumable Archive phases without closing the issue before merge.
- Keep Corgi-generated knowledge derived from canonical OpenSpec and project documentation.
- Produce evidence sufficient for an explicit adopt, adapt, or reject decision and a complete rollback.

**Non-Goals:**

- Parallel Task Group execution within one change.
- Immediate replacement or deletion of the current formal-worker guard plane.
- Migration of an already active OpenSpec change.
- Automatic force-push, protected-branch push, ready-for-review transition, reviewer request, merge, remote branch
  deletion, tag, publish, or release.
- A GitHub App, hosted daemon, merge bot, or distributed claim database.
- Treating GitHub issue text, Corgi Memory/Wiki, or PR prose as a second canonical specification store.
- Changing Scala, SBT, production modules, domain behavior, or public APIs.

## Decisions

### 1. Qualify before permanent cutover

The change installs and exercises a reversible Corgi path but does not make it the default for all new work. Active
changes continue through their existing lifecycle. The pilot ends with a recorded `ADOPT`, `ADAPT`, or `REJECT`
decision. `ADOPT` still requires a follow-up change that names which overlapping helpers and instructions are removed,
retained, or migrated.

This avoids interpreting successful installation as proof of recovery, review, GitHub, and rollback behavior.

### 2. Keep one authority for each kind of state

Authority is divided as follows:

- OpenSpec owns canonical proposals, specifications, designs, tasks, and archived specification state.
- Corgi owns the pilot change's live lifecycle, Task Group checkpoints, Verify/Review/QA evidence, and resumable Archive
  transaction.
- The repository guard plane owns formal-worker launch isolation, independent-review freshness, and pilot admission.
- GitHub owns native cross-change dependencies, PR discussion, CI status, final PR approval, and merge state.
- The PR wrapper owns only validated projection and sequencing between Corgi and GitHub.

The wrapper never edits OpenSpec artifacts or Corgi lifecycle files directly. It consumes supported Corgi JSON and
GitHub APIs, and it never treats a PR label or body marker as authority to advance Corgi.

### 3. Install an exact project-local Corgi version

The pilot records the selected Corgi version and MIT license, uses a repository-reproducible installation, and captures
every bootstrap-generated path before accepting it. It does not depend on an unpinned global installation. Project
scope, hooks, schemas, and Memory/Wiki initialization are enabled only after their diffs are inspected and collision
tests show that project-owned configuration is preserved.

The exact version is selected during qualification against the then-current OpenSpec compatibility range rather than
being guessed in planning artifacts.

Qualification selected `corgispec@4.0.0-rc2`. Stable `3.0.1` lacks the durable Run Contract and staged Archive contract
required here. The v4 bootstrap refuses migration while any worktree contains an active OpenSpec change, including this
pilot itself. Consequently this change can prepare and review the dormant delivery boundary, but activation must be a
follow-up after all active changes are complete; no migration bypass is permitted.

### 4. Use one change as the unit of execution and claim

Each pilot change has exactly one Corgi run, GitHub issue, isolated delivery worktree, WIP branch, and draft PR. A central
coordinator grants the change-level claim before a writer starts. GitHub assignee/labels may mirror the claim but are not
an atomic synchronization mechanism.

Different claimed changes may execute concurrently in distinct worktrees. A second writer for the same change is
rejected. Task Groups inside a change remain ordered and sequential. Integration and merge remain serialized or use a
merge queue that does not rewrite the verified PR head.

Native GitHub `blockedBy` relationships express cross-change readiness. Runtime claim contention and discovered path or
module overlap remain separate transient concerns.

### 5. Grant narrowly scoped commit and WIP-publication authority

For an explicitly admitted pilot change, Corgi and its wrapper may:

- create local planning-baseline, Task Group, repair, and Archive commits required by the tested Corgi contract;
- create or reuse only the configured change branch and worktree;
- fast-forward push that WIP branch;
- create or update one draft PR and its safe status/evidence projection; and
- allow normal CI triggered by those pushes.

They may not force-push, push another branch, mark the PR ready, request reviewers, merge, close the PR, delete a remote
branch, tag, publish, or release without a separate explicit command or authorization. Remote divergence, unexpected
commits, an unrecognized branch prefix, or a mismatched PR head fails closed.

The draft PR is created after the first successful Apply checkpoint, never during planning-only Propose. Later checked
Task Group commits are pushed to provide remote recovery and visible progress; push cadence may be reduced when CI cost
is material, but evidence identity is never weakened.

### 6. Implement a thin project-local Python PR wrapper

Python matches the repository's existing workflow-helper surface and can consume Corgi and `gh` JSON without adding a
second application runtime beyond Corgi's own Node requirement. The wrapper exposes bounded operations equivalent to:

- `open`: validate the change and create or reuse its draft PR;
- `sync`: project phase, Task Group, Verify, review, QA, and Archive summaries;
- `ready`: validate the locally archived PR head and perform an explicitly requested ready/reviewer handoff;
- `merge`: revalidate exact SHA, checks, review, dependencies, and explicit merge authority before invoking GitHub; and
- `finalize`: after confirmed merge, resume tracker confirmation and Corgi cleanup.

Operations are idempotent. PR discovery uses the exact head branch plus a versioned hidden body marker. No nonce, CAS
token, credential, sensitive report content, private path, or full diagnostic log is published. The wrapper stores no
independent lifecycle database; durable authority remains in Corgi and GitHub.

### 7. Dual-record review without self-certification

Corgi's per-Task-Group automated reviews remain local structured evidence and may be projected as summaries or GitHub
checks. After whole-change Verify, a fresh independent reviewer evaluates an isolated worktree of the exact committed PR
head, writes the repository's structured report, and posts actionable findings and a SHA-bound verdict on the PR.

The implementer cannot provide that verdict. A failing review routes through Corgi's repair lifecycle, produces new
commits, reruns Verify, and requires another fresh independent review. A human reviews the PR and records the matching
Corgi Human Review decision; the pilot may assist with identity and SHA validation but never manufactures that choice.

Archive local materialization adds a final commit, so GitHub reruns CI and requires final approval of the archived PR
head. Corgi Human Review and final GitHub merge approval are related but distinct gates.

### 8. Pause Archive between local materialization and tracker closeout

After Verify, independent approval, Corgi Human Review, and Human QA, the finalization path runs Archive `--begin` and
`--local`. It then pushes the Archive commit, synchronizes the draft PR, and stops. The retained Archive intent and
worktree permit final GitHub CI, review, and an explicitly authorized merge.

Only after GitHub reports the exact PR merged does finalization run tracker confirmation and Archive finish. Issue
closeout and worktree removal therefore describe merged delivery rather than merely locally archived work. GitHub or
merge failure leaves the Corgi intent resumable and does not repeat local Archive materialization.

### 9. Preserve tree identity across base drift and review

The change branch is brought current before whole-change Verify. A rebase, merge-from-base, remediation commit, Archive
commit, or any other tree change invalidates prior review/CI evidence as appropriate. The wrapper never silently rebases
a verified branch. Final integration uses serialized merge or a merge queue that tests the combined result without
rewriting the approved PR head.

Legacy staged-snapshot review remains supported for non-Corgi changes. Corgi review selects the committed PR-head tree,
records its identity before review, verifies the detached review worktree remains unchanged, and rejects approval if the
selected PR tree changes.

### 10. Keep Corgi knowledge derived and bounded

Canonical architecture, design decisions, and behavioral requirements remain in their existing OpenSpec and project
documentation owners. Corgi Archive may create delivery pages, evidence indexes, change summaries, and discovered
pitfalls, but those artifacts must link to canonical sources and must not silently redefine them.

The pilot inventories every Memory/Wiki write and treats conflicting generated knowledge as drift. Inability to prevent
a second architectural authority is an adoption failure, not a reason to duplicate canonical text.

### 11. Make the adoption decision evidence-based

Qualification includes two concurrently active changes, one interrupted Apply/resume, one independent-review rejection
and repair, remote divergence, base drift, GitHub unavailability, Archive resumption, exact-SHA checks, and complete
rollback. Measurements distinguish elapsed time, primary-thread context, CI executions, worker usage, failure-recovery
effort, and human gate count.

No claim of lower total cost or improved safety is made without representative evidence. The decision record names
which behavior improved, regressed, remained unchanged, or could not be measured.

## Risks / Trade-offs

- [Risk] A release-candidate Corgi version or OpenSpec compatibility change invalidates the pilot. -> Mitigation: pin the
  exact qualified version, test bootstrap in isolation, and keep rollback complete.
- [Risk] Corgi bootstrap or Memory/Wiki generation overwrites or duplicates project-owned context. -> Mitigation:
  inventory generated paths, compare before/after content, and fail the adoption gate on ambiguous ownership.
- [Risk] Existing guard-plane and Corgi state machines create duplicate ceremony or contradictory status. ->
  Mitigation: assign one authority per state category and measure every duplicated gate during the pilot.
- [Risk] Frequent WIP pushes trigger expensive CI. -> Mitigation: keep PRs draft, make push cadence configurable, and
  require full CI only at named integration gates.
- [Risk] A PR or branch changes after Corgi evidence is recorded. -> Mitigation: exact-SHA validation, stale-evidence
  rejection, fast-forward-only pushes, and no silent rebases.
- [Risk] The independent reviewer cannot publish to GitHub without gaining mutation authority over code. -> Mitigation:
  grant only review/check publication capability and continue to verify the detached review tree is unchanged.
- [Risk] A GitHub outage occurs after local Archive. -> Mitigation: retain the Archive intent and worktree, retry
  idempotently, and do not close the issue or repeat local Archive.
- [Risk] Pilot artifacts become a de facto permanent workflow without approval. -> Mitigation: keep Corgi non-default
  and require a follow-up adoption change after the explicit decision.

## Migration Plan

1. Record a clean baseline, active-change inventory, existing workflow/configuration owners, and complete rollback map.
2. Select and pin a compatible Corgi release; verify its MIT license and bootstrap output in an isolated fixture.
3. Add a disabled project-local pilot configuration, guarded change-worktree admission, focused tests, and the PR
   wrapper without migrating active changes.
4. After this and all other active changes are complete, use a reviewed follow-up to rerun bootstrap, resolve the
   user-level Codex skill boundary, explicitly admit the two pilot changes, and enable the pilot.
5. Execute the feature, maintenance, concurrency, recovery, review/repair, GitHub, Archive, and rollback scenarios.
6. Publish the evidence and record `ADOPT`, `ADAPT`, or `REJECT`.
7. On `REJECT`, remove pilot runtime/configuration assets and verify plain OpenSpec behavior remains intact. On `ADAPT`,
   retain only separately justified mechanisms. On `ADOPT`, create a follow-up change for default routing, migration,
   and removal of overlapping machinery.

## Qualification Gate

No unresolved product choice blocks the design, but activation is mechanically gated. The exact release,
bootstrap-generated path set, Memory/Wiki ownership, OpenSpec compatibility defects, Codex skill target, active-change
migration refusal, and GitHub authentication state are recorded in `qualification.md`. The checked-in path remains
disabled until a follow-up demonstrates that every gate is clear.
