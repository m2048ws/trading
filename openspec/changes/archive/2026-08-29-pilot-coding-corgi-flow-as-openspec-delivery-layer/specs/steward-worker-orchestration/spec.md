## MODIFIED Requirements

### Requirement: Shared mutable repository work is serialized
The steward MUST allow at most one apply, remediation, or finalization worker to mutate the primary worktree at a time.
During the admitted Corgi pilot, it MAY allow different explicitly claimed changes to mutate distinct isolated delivery
worktrees concurrently, but MUST allow at most one writer per change/worktree and MUST serialize or safely queue final
integration. Parallel native delegation SHALL remain limited to independent assignments that cannot create conflicting
repository state.

#### Scenario: A mutating worker is active
- **WHEN** an apply, remediation, or finalization worker is operating on the primary worktree
- **THEN** the steward does not launch another worker that may mutate that worktree

#### Scenario: Native writer report is rejected
- **WHEN** collection rejects a primary-worktree native worker's assigned report
- **THEN** the guard plane retains that worker's lease until report correction or refreshed-state classification completes
- **AND** no native or script writer can acquire the primary worktree before that guarded path completes

#### Scenario: Broker process dies while a script writer is active
- **WHEN** a script-backed apply, remediation, or finalization parent owns the primary-worktree reservation
- **AND** an unrelated native broker process terminates
- **THEN** the script parent's reservation remains held
- **AND** another native or script writer cannot acquire the primary worktree

#### Scenario: Independent read-heavy work is available
- **WHEN** multiple bounded investigations can run without changing shared repository state
- **THEN** the steward may delegate those investigations to native subagents in parallel

#### Scenario: Independent Corgi changes are claimed
- **WHEN** two admitted pilot changes have distinct claims, branches, and isolated delivery worktrees and no known integration conflict
- **THEN** one script-backed writer may operate in each change worktree concurrently
- **AND** neither writer receives authority over the other's worktree, branch, issue, PR, or Corgi run

#### Scenario: Another writer selects an active Corgi change
- **WHEN** a claim or writer already owns that pilot change or delivery worktree
- **THEN** the second writer is rejected before repository mutation or GitHub publication

#### Scenario: Corgi changes reach integration
- **WHEN** multiple completed pilot changes are ready for final delivery
- **THEN** their merge operations are serialized or safely queued with exact-SHA checks

### Requirement: Independent review evaluates an isolated selected snapshot
Every independent review SHALL evaluate the exact Git tree selected by the steward in an isolated detached review
worktree. A legacy path MAY select the current staged Git tree; an admitted Corgi path SHALL select the exact committed
PR-head tree. The review mechanism MUST detect tracked, non-ignored untracked, staged, or committed snapshot changes and
MUST reject a ready verdict when the review snapshot was modified.

#### Scenario: Legacy review begins
- **WHEN** the steward launches independent review for a staged non-Corgi change
- **THEN** the reviewer receives an isolated detached worktree representing the current staged Git tree

#### Scenario: Corgi review begins
- **WHEN** the steward launches independent review for an admitted Corgi change
- **THEN** the reviewer receives an isolated detached worktree representing the exact committed PR-head tree
- **AND** the selected commit and tree identities are recorded with the review

#### Scenario: Reviewer changes the snapshot
- **WHEN** the review worktree contains a tracked, staged, or non-ignored untracked change after review
- **THEN** the review result is rejected as a process failure even if the reviewer reported ready

#### Scenario: Reviewer commits a snapshot change
- **WHEN** the detached review `HEAD` or tree differs from the selected snapshot even though porcelain status is clean
- **THEN** the review result is rejected and cannot approve the implementation

#### Scenario: Native review cannot be isolated
- **WHEN** native delegation cannot bind a reviewer to the isolated selected snapshot with the required cleanliness checks
- **THEN** the steward uses the script-backed independent-review path

### Requirement: Review approval remains bound to selected tree identity
The steward MUST record the selected tree identity immediately before independent review and MUST compare it with the
corresponding delivery identity after review. A legacy staged review compares the staged tree; a Corgi review compares
the committed PR-head tree. A ready verdict SHALL be stale when those identities differ.

#### Scenario: Selected tree is unchanged
- **WHEN** an independent review returns ready and the applicable staged or committed PR-head tree identity is unchanged
- **THEN** the steward may accept the approval subject to the remaining finalization gates

#### Scenario: Selected tree changes during review
- **WHEN** the applicable staged or committed PR-head tree identity after review differs from the identity recorded before review
- **THEN** the steward rejects the approval as stale and requires fresh review of the new tree

#### Scenario: Corgi publishes another commit after review
- **WHEN** repair, base integration, or another implementation commit changes the reviewed PR-head tree
- **THEN** the prior independent approval is stale
- **AND** Verify and fresh independent review are required for the new implementation tree

#### Scenario: Local Archive adds its expected final commit
- **WHEN** Corgi local Archive materialization changes the implementation-approved PR head only by the expected archive transaction
- **THEN** final GitHub CI and approval are required for the archived head before merge
- **AND** the earlier Corgi Human Review is retained as implementation evidence rather than represented as final PR-head approval
