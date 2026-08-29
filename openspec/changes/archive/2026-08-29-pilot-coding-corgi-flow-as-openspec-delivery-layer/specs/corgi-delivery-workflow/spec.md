## ADDED Requirements

### Requirement: Corgi extends rather than replaces OpenSpec authority during qualification
The pilot SHALL retain OpenSpec as the canonical owner of proposals, specifications, designs, tasks, and archived
specification state. Corgi SHALL own only the admitted pilot change's live delivery lifecycle and evidence. Installing
Corgi MUST NOT make it the default for active or future changes without a later explicit adoption decision.

#### Scenario: Pilot change is admitted
- **WHEN** a new change is explicitly selected for the Corgi pilot
- **THEN** its canonical planning artifacts remain OpenSpec artifacts
- **AND** its live Task Group, Verify, Review, QA, and Archive state is advanced only through supported Corgi operations

#### Scenario: Non-pilot change is active
- **WHEN** Corgi assets are installed but a change was not explicitly admitted to the pilot
- **THEN** that change retains its existing workflow and is not migrated or claimed by Corgi

#### Scenario: Pilot completes successfully
- **WHEN** all qualification scenarios pass
- **THEN** Corgi still does not become the default until a follow-up adoption change is explicitly approved

### Requirement: Pilot installation is exact, reproducible, and reversible
The pilot SHALL select and record one exact MIT-licensed Corgi version compatible with the installed OpenSpec version,
use a project-reproducible installation, inventory every generated or modified path, and provide tested rollback. It
MUST fail qualification when bootstrap or update behavior ambiguously owns or destructively replaces project assets.

#### Scenario: Version is selected
- **WHEN** qualification chooses a Corgi release
- **THEN** the exact version, package identity, license, OpenSpec compatibility, and generated path set are recorded

#### Scenario: Bootstrap encounters project-owned content
- **WHEN** Corgi cannot preserve or unambiguously migrate an existing project-owned configuration, hook, instruction, or knowledge file
- **THEN** repository installation stops before accepting the generated state
- **AND** the ambiguity is an adoption blocker

#### Scenario: Pilot is rejected
- **WHEN** the decision is `REJECT`
- **THEN** runtime/configuration assets, hooks, worktrees, branches, issues, draft PRs, and derived knowledge are removed or closed according to the tested rollback
- **AND** canonical OpenSpec artifacts and non-pilot changes remain intact

### Requirement: Each Corgi change has one isolated execution identity
Every admitted pilot change SHALL have exactly one exclusive claim, Corgi run, GitHub issue, delivery worktree, WIP
branch, and draft PR. Different changes MAY execute concurrently only in distinct worktrees. Task Groups within one
change SHALL remain sequential, and integration SHALL be serialized or performed by a merge queue that preserves the
approved PR-head identity.

#### Scenario: Two independent changes are ready
- **WHEN** each change has no unresolved dependency and receives a distinct claim and worktree
- **THEN** their writers may execute concurrently without sharing a branch, worktree, issue, PR, or Corgi run

#### Scenario: A second writer selects the same change
- **WHEN** an active claim or run already owns that change
- **THEN** the second writer is rejected before mutation or publication

#### Scenario: Concurrent changes are ready to integrate
- **WHEN** two independently completed changes are ready for merge
- **THEN** their final integration is serialized or queued with exact-SHA validation

### Requirement: Cross-change dependencies and transient claims remain distinct
GitHub native issue dependencies SHALL be the projected operational authority for cross-change `blockedBy` readiness.
The central coordinator SHALL own transient change claims, and GitHub assignee or label state MAY mirror but MUST NOT
replace atomic admission. Discovered file/module overlap SHALL be treated as transient integration contention rather
than silently rewriting the planned dependency graph.

#### Scenario: Issue is blocked
- **WHEN** a pilot issue has an unresolved native `blockedBy` relationship
- **THEN** the coordinator does not grant its execution claim

#### Scenario: Issue is ready and unclaimed
- **WHEN** all dependencies are resolved and no active claim exists
- **THEN** the coordinator may grant one bounded change-level claim

#### Scenario: Independent work later overlaps
- **WHEN** two nominally independent changes touch a conflicting integration surface
- **THEN** integration is ordered or one change is repaired
- **AND** dependency history is not falsified to represent transient contention

### Requirement: Corgi publication authority is narrow and explicit
For an explicitly admitted pilot change, Corgi and the PR wrapper MAY create required local checkpoint and Archive
commits, fast-forward push only the configured WIP branch, create or update one draft PR, publish safe evidence, and
trigger normal CI. They MUST NOT force-push, push a protected or unrelated branch, mark ready, request reviewers, merge,
close the PR, delete a remote branch, tag, publish, or release without separate explicit authority.

#### Scenario: First Apply checkpoint succeeds
- **WHEN** the expected change branch contains a checked Corgi Task Group commit and remote identity is safe
- **THEN** the wrapper may fast-forward push the WIP branch and create or reuse its one draft PR

#### Scenario: Later checkpoint succeeds
- **WHEN** the local WIP branch is a fast-forward continuation of the expected remote branch
- **THEN** the wrapper may push it and synchronize bounded PR evidence

#### Scenario: Remote or target identity is unexpected
- **WHEN** the remote branch contains an unexpected commit, the branch prefix is not allowed, the PR mapping differs, or the push would rewrite history
- **THEN** publication fails closed without force-push or alternate-target fallback

#### Scenario: Merge is otherwise ready
- **WHEN** every Corgi and GitHub gate passes but merge has not been explicitly authorized
- **THEN** the wrapper stops without merging, closing, deleting, tagging, publishing, or releasing

### Requirement: The PR wrapper projects state without becoming lifecycle authority
The project-local PR wrapper SHALL consume supported Corgi and GitHub machine-readable output, validate the complete
change/issue/worktree/branch/PR/SHA product, and perform idempotent bounded operations. It MUST NOT edit OpenSpec
artifacts, Corgi lifecycle files, or an independent lifecycle database, and MUST NOT publish credentials, CAS material,
private paths, sensitive report content, or full diagnostics.

#### Scenario: Draft PR does not exist
- **WHEN** the first Apply checkpoint is valid and all identities match
- **THEN** `open` creates one draft PR with a versioned hidden marker and safe evidence summary

#### Scenario: Draft PR already exists
- **WHEN** `open` or `sync` is repeated for the same exact branch and change
- **THEN** the wrapper reuses and idempotently updates that PR rather than creating another

#### Scenario: Corgi and GitHub SHAs differ
- **WHEN** a requested ready, merge, or finalize transition observes a PR head different from the required Corgi revision
- **THEN** the wrapper rejects stale evidence and performs no downstream transition

#### Scenario: Invocation outcome is uncertain
- **WHEN** interruption or GitHub failure makes the prior operation outcome unknown
- **THEN** a retry discovers actual Corgi/GitHub state and completes or reports the same bounded transition without duplication

### Requirement: Review is visible on the PR and verifiable locally
Corgi automated review, fresh independent review, and human review SHALL remain distinct. Complete structured evidence
SHALL remain local and bound to an exact tree, while bounded summaries, checks, actionable findings, and human
discussion SHALL appear on the PR. An implementer or remediation worker MUST NOT certify its own independent review.

#### Scenario: Task Group automated review completes
- **WHEN** Corgi accepts the group's checks and automated review
- **THEN** the local evidence remains canonical for that checkpoint
- **AND** a safe summary or check may be projected to the draft PR without granting independent approval

#### Scenario: Whole-change Verify passes
- **WHEN** a fresh independent reviewer evaluates an isolated worktree of the exact committed PR head
- **THEN** the complete structured report is retained locally
- **AND** actionable findings and a SHA-bound verdict are published on the PR

#### Scenario: Independent review rejects the implementation
- **WHEN** the reviewer reports a blocking finding
- **THEN** Corgi routes the change through repair and repeated Verify
- **AND** another fresh independent review is required for the new tree

#### Scenario: Human accepts the implementation
- **WHEN** the human has reviewed the exact implementation PR head and chooses approval
- **THEN** the corresponding Corgi Human Review decision records that human identity and tree
- **AND** no agent manufactures or infers the decision merely from PR state

#### Scenario: Archive adds the final commit
- **WHEN** local Archive materialization changes the PR head
- **THEN** final GitHub CI and approval apply to the archived head as a distinct merge gate

### Requirement: Archive closeout follows confirmed PR merge
The pilot SHALL run Corgi Archive begin and local materialization only after passing Verify, fresh independent review,
Human Review, and Human QA. It SHALL retain the Archive intent and worktree while the final Archive commit is pushed,
checked, reviewed, and explicitly merged. Tracker confirmation, issue closeout, and worktree cleanup SHALL occur only
after GitHub confirms merge of the exact PR.

#### Scenario: Local Archive succeeds
- **WHEN** Archive materializes its evidence and final commit
- **THEN** the wrapper pushes and synchronizes that commit but leaves tracker confirmation and cleanup incomplete

#### Scenario: GitHub is unavailable or merge fails
- **WHEN** final delivery cannot be confirmed
- **THEN** the issue remains open and the Archive intent/worktree remain resumable
- **AND** local Archive is not repeated

#### Scenario: Exact PR merge is confirmed
- **WHEN** GitHub reports the required PR and head merged
- **THEN** finalization may confirm the tracker and finish Archive idempotently
- **AND** issue closeout and worktree removal describe merged delivery

### Requirement: Corgi knowledge remains derived from canonical owners
Corgi Memory/Wiki output SHALL be limited to derived delivery evidence, change summaries, and discovered pitfalls that
link to canonical OpenSpec specifications and project documentation. It MUST NOT silently redefine architecture,
decisions, requirements, or construction authority owned elsewhere.

#### Scenario: Archive promotes delivery knowledge
- **WHEN** Corgi creates a delivery page, evidence index, summary, or pitfall
- **THEN** the artifact identifies its canonical source and derived status

#### Scenario: Generated knowledge conflicts with a canonical source
- **WHEN** Memory/Wiki content would establish a competing architectural, decision, or behavioral authority
- **THEN** qualification fails until ownership is made unambiguous
- **AND** the generated text is not accepted as canonical through repetition

### Requirement: Adoption is evidence-based and separately authorized
The pilot SHALL exercise representative feature, maintenance, concurrency, recovery, review/repair, GitHub, Archive,
knowledge, and rollback scenarios and record an explicit `ADOPT`, `ADAPT`, or `REJECT` decision. It MUST distinguish
elapsed time, primary-thread context, worker usage, CI executions, human gates, recovery effort, and generated-state
volume, and MUST NOT make Corgi default or remove existing machinery without a follow-up change.

#### Scenario: Qualification evidence is complete
- **WHEN** all required scenarios and rollback pass
- **THEN** the decision record states which dimensions improved, regressed, remained unchanged, or were not measurable

#### Scenario: A required scenario fails
- **WHEN** safety, identity, recovery, review, knowledge ownership, or rollback cannot meet the specified contract
- **THEN** the decision is `ADAPT` or `REJECT` rather than silent adoption

#### Scenario: Decision is adopt
- **WHEN** the pilot recommends `ADOPT`
- **THEN** a follow-up change defines default routing, active-work migration, and removal or retention of overlapping machinery
