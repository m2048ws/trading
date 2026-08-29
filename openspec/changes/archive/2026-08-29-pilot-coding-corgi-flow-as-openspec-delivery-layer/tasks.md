## 1. Qualification Baseline and Reversible Installation

- [x] 1.1 Refresh Git/OpenSpec state, inventory active changes, and record the project-owned workflow, configuration,
  knowledge, hook, and generated-path surfaces that the pilot must preserve.
- [x] 1.2 Select one exact Corgi release compatible with the installed OpenSpec version, verify its MIT license and
  package identity, and add a reproducible project-local pin without relying on an unpinned global install.
- [x] 1.3 Run Corgi bootstrap and update behavior against an isolated fixture first; capture every generated or modified
  path and reject ambiguous ownership or destructive replacement before targeting the repository.
- [ ] 1.4 Define and test complete rollback for packages, generated assets, hooks, configuration, worktrees, branches,
  issues, draft PRs, and derived Memory/Wiki content.
- [ ] 1.5 Select new feature and maintenance pilot changes without migrating or changing the lifecycle of any active
  OpenSpec change.

## 2. Pilot Authority and Worktree Integration

- [ ] 2.1 Configure OpenSpec as canonical artifact authority and Corgi as the admitted pilot change's lifecycle
  authority; reject vanilla lifecycle mutations that would bypass an active Corgi run.
- [x] 2.2 Add guarded admission for exactly one issue, exclusive claim, worktree, branch, and Corgi run per pilot change.
- [x] 2.3 Extend writer serialization to permit different claimed changes in separate worktrees while rejecting a second
  writer for the same change and serializing integration/merge.
- [x] 2.4 Encode standing authority for required local pilot commits, fast-forward WIP-branch pushes, and one draft PR,
  while keeping protected-branch push, force-push, ready/reviewer transition, merge, deletion, tag, publish, and release
  explicitly controlled.
- [x] 2.5 Use native GitHub issue dependencies for cross-change `blockedBy` state and keep dependency readiness separate
  from transient claims and discovered file/module overlap.

## 3. GitHub PR Wrapper

- [x] 3.1 Implement a focused project-local Python wrapper that consumes supported Corgi JSON and `gh` JSON without
  editing OpenSpec artifacts, Corgi lifecycle files, or a separate lifecycle database.
- [x] 3.2 Implement idempotent draft-PR open/reuse after the first successful Apply checkpoint with exact
  change/issue/worktree/branch/base/head identity, a versioned body marker, and safe evidence projection.
- [x] 3.3 Implement fast-forward-only WIP push and PR synchronization for Task Group, repair, Verify, review, QA, and
  Archive commits; reject remote divergence, unexpected branch prefixes, mismatched PRs, and stale SHA evidence.
- [x] 3.4 Implement explicitly invoked ready, merge, and finalize operations that revalidate current SHA, dependencies,
  checks, reviews, authority, GitHub outcome, and Corgi phase before each external transition.
- [x] 3.5 Prevent publication of Corgi nonces/CAS tokens, credentials, private paths, sensitive report bodies, and full
  diagnostics; exercise redaction and failure cases in focused tests.
- [ ] 3.6 Make every operation recoverable after process interruption, duplicate invocation, partial push, PR already
  existing, GitHub outage, merge refusal, or an already completed downstream transition.

## 4. Review and Evidence Integration

- [x] 4.1 Keep Corgi per-Task-Group automated review as structured local evidence and project bounded summaries/checks
  onto the draft PR without treating them as independent approval.
- [x] 4.2 Extend fresh independent review to select an exact committed pilot PR-head tree in an isolated detached
  worktree, reject any review-worktree mutation, and make approval stale when the selected PR tree changes.
- [x] 4.3 Publish actionable independent findings and a SHA-bound verdict on the PR while retaining the complete
  structured report locally; grant the reviewer no implementation, branch-push, or merge authority.
- [ ] 4.4 Route a rejected review through Corgi repair, rerun Verify, publish new commits, and require another fresh
  independent review before a human may record Corgi approval.
- [ ] 4.5 Bind the human Corgi Review decision to the reviewed PR SHA and preserve final GitHub CI and approval for the
  later archived PR head as a separate merge gate.

## 5. Archive, Merge, and Cleanup Integration

- [ ] 5.1 Run Corgi Archive begin/local only after passing Verify, fresh independent review, human Corgi Review, and
  Human QA; retain the durable Archive intent and worktree after local materialization.
- [ ] 5.2 Push and synchronize the Archive commit, require exact-SHA final CI and review, and stop before merge unless the
  user explicitly authorizes ready/reviewer and merge operations.
- [x] 5.3 After confirmed merge of the exact PR, resume tracker confirmation and Archive finish so issue closeout and
  worktree cleanup describe merged delivery and do not repeat local Archive work.
- [ ] 5.4 Exercise base drift, merge-queue or serialized-integration behavior, stale approval, failed merge, GitHub
  outage, already merged PR, tracker failure, and cleanup retry without silent rebase or evidence reuse.

## 6. Knowledge Ownership and Workflow Coexistence

- [ ] 6.1 Configure and test Corgi Memory/Wiki so delivery pages, evidence, summaries, and pitfalls remain derived from
  and linked to canonical OpenSpec/project sources.
- [x] 6.2 Inventory every Corgi knowledge write and fail qualification if it silently redefines architecture, decisions,
  requirements, or another project-owned source.
- [x] 6.3 Keep active and non-pilot changes on the existing workflow and make Corgi pilot admission explicit rather than
  inferred from installed assets.
- [ ] 6.4 Document which current prompts, guards, reports, and finalization steps are duplicated, reused, or candidates
  for later removal without changing their default authority during the pilot.

## 7. Pilot Scenarios and Adoption Decision

- [ ] 7.1 Complete one RFC-backed feature and one bounded maintenance-exemption flow through the admitted Corgi path.
- [ ] 7.2 Execute two independent pilot changes concurrently in distinct worktrees and prove same-change writer
  exclusion plus serialized integration.
- [ ] 7.3 Interrupt and resume Apply without duplicated Task Groups, commits, issues, PRs, or evidence.
- [ ] 7.4 Exercise independent-review rejection, Corgi repair, repeated Verify, fresh re-review, Human Review, Human QA,
  local Archive, explicit PR merge, tracker confirmation, and cleanup.
- [ ] 7.5 Measure elapsed time, primary-thread context, worker usage, CI executions, human gates, failure-recovery effort,
  generated-state volume, and operational friction against representative existing workflow observations.
- [ ] 7.6 Demonstrate complete rollback to plain OpenSpec with active non-pilot changes and canonical specs unchanged.
- [ ] 7.7 Record an evidence-backed `ADOPT`, `ADAPT`, or `REJECT` decision. Require a follow-up change before making Corgi
  default, removing existing machinery, or migrating unfinished work.

## 8. Focused Validation and Independent Approval

- [x] 8.1 Run syntax and unit tests for every changed workflow helper, wrapper, hook, configuration parser, and generated
  asset boundary.
- [ ] 8.2 Run focused workflow regressions for authority, worktree isolation, claims, exact-SHA review, safe publication,
  idempotency, recovery, Archive resumption, knowledge ownership, and rollback.
- [x] 8.3 Run strict validation for this change and repository-wide strict OpenSpec validation.
- [x] 8.4 Verify the changed path set remains workflow/configuration/documentation/OpenSpec-only; while true, do not run
  unrelated Scala/SBT tests, Scala formatting, Scaladoc, or JMH.
- [x] 8.5 Run cached and unstaged Git diff checks, inspect untracked state, and stage exactly the intended pilot change
  without committing unless the user separately authorizes this proposal commit.
- [ ] 8.6 Obtain fresh independent approval of the exact staged proposal/implementation tree through the existing
  detached-snapshot review path; any remediation requires another fresh independent review.
