## Purpose

Defines how the project steward delegates OpenSpec worker roles through native Codex subagents or a portable script
backend without weakening workflow gates, trusted launch authority, review independence, or mechanically validated and
context-efficient handoffs.

## ADDED Requirements

### Requirement: Native orchestration is preferred only when eligible
In an interactive Codex environment with native subagent support, the steward SHALL prefer native delegation for a
worker role only when that execution path can satisfy every guard required for the role. The steward MUST use the
script-backed path when native delegation is unavailable or cannot provide an equivalent required guard.

#### Scenario: Eligible native worker role
- **WHEN** the steward is ready to launch a role and native delegation satisfies the role's context, repository, report, and transition requirements
- **THEN** the steward launches a fresh native subagent for that role

#### Scenario: Native execution lacks a required guard
- **WHEN** native delegation cannot satisfy a required guard such as independent-review snapshot isolation or validated structured reporting
- **THEN** the steward launches the role through the script-backed worker path instead

#### Scenario: Steward-private writer authority is unavailable
- **WHEN** the client cannot provide broker state, a control channel, a protected immutable executable decision closure,
  and crash-surviving writer exclusion that are inaccessible to the delegated worker
- **THEN** apply, remediation, and finalization are ineligible for native delegation
- **AND** the steward launches the role through the script-backed worker path instead

#### Scenario: Current native writer implementation is selected
- **WHEN** backend selection or broker preparation is requested for apply, remediation, or finalization in the current repository/runtime
- **THEN** the request selects the script backend even if the supplied capability file claims every historical broker guard
- **AND** no native writer launch, reservation, report destination, transition authority, or fallback authority is created
- **AND** bounded non-mutating exploration remains eligible for native delegation

#### Scenario: Mutable policy reclassifies a formal role
- **WHEN** workspace-controlled role policy or its live classifier is changed after broker start to describe apply,
  review, remediation, or finalization as read-only and native-eligible
- **THEN** an immutable in-process production role-name boundary still selects the script backend
- **AND** broker preparation independently denies the formal role before creating handoff artifacts, a reservation,
  active authority, transition authority, or fallback authority

#### Scenario: Native delegation is unavailable
- **WHEN** the current environment does not expose native subagent delegation
- **THEN** the complete steward workflow remains operable through the script-backed path

### Requirement: Worker roles retain fresh and separate authority
Every delegated apply, review, remediation, and finalization worker SHALL start in a fresh agent context containing only
the role-relevant assignment and accessible repository state. A worker that implemented or remediated a change MUST NOT
serve as an independent reviewer for that repository state.

#### Scenario: Apply completes
- **WHEN** an apply worker reports that implementation is ready for review
- **THEN** the steward requires a distinct fresh review worker before approving finalization

#### Scenario: Remediation completes
- **WHEN** a remediation worker reports that all assigned findings are closed
- **THEN** the steward requires another distinct fresh review worker before approving finalization

#### Scenario: Review context is prepared
- **WHEN** the steward delegates an independent review
- **THEN** the reviewer receives implementation or remediation reports only as claims to falsify and does not inherit the prior worker's private reasoning

### Requirement: Shared mutable repository work is serialized
The steward MUST allow at most one apply, remediation, or finalization worker to mutate the primary worktree at a time.
Parallel native delegation SHALL be limited to independent assignments that cannot create conflicting repository state.

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

### Requirement: Worker reports are mechanically validated
A worker result MUST satisfy the role's structured report schema, assigned-change binding, and workflow-consistency
rules before the steward uses it to transition workflow state. A human-readable native summary alone SHALL NOT
authorize a transition.

#### Scenario: Valid native worker result
- **WHEN** a native worker's assigned result passes schema, assigned-change, and workflow-consistency validation
- **THEN** the steward may use the validated report as an input to workflow classification

#### Scenario: Invalid native worker result
- **WHEN** a native result is absent, malformed, schema-invalid, or contradictory to its role transition rules
- **THEN** the steward does not transition and instead requests correction or classifies the failed launch

#### Scenario: Launch context names another change
- **WHEN** a formal native or script worker context names a change different from the change assigned by its launch
- **THEN** preparation rejects the context before launching the worker or acquiring transition authority

#### Scenario: Structured report names another change
- **WHEN** a schema-valid worker report explicitly names an OpenSpec change different from the assigned change
- **THEN** collection rejects transition authority
- **AND** a native writer retains its lease until report correction or refreshed-state classification

#### Scenario: Worker claims successful repository state
- **WHEN** any worker reports successful validation, clean Git state, or completed OpenSpec work
- **THEN** the steward refreshes the load-bearing repository and OpenSpec state before the next transition

### Requirement: Native writer authority is steward-owned and one-shot
Native transition, writer-release, and fallback authority MUST remain bound to live state and a control channel owned by
the steward and inaccessible to the delegated worker. For every native writer launch, that state MUST retain the full
launch tuple, the active monotonic generation, and an unconsumed one-shot capability. No manifest, digest, report, trace,
prompt, lease file, environment value, or command available to the worker SHALL authorize a transition, release a
writer, select another change, classify fallback as safe, or recover failed authority.

The current implementation MUST NOT claim that this conditional authority exists. Until the complete transitive
executable decision closure is protected and writer exclusion survives broker-process death, every native
primary-worktree writer role MUST remain ineligible before launch.

#### Scenario: Worker changes its assigned change or role
- **WHEN** a native worker modifies a workspace artifact to name a different role or OpenSpec change
- **THEN** collection rejects the launch as integrity-invalid and grants no transition authority

#### Scenario: Worker changes its report or schema destination
- **WHEN** a native worker modifies a workspace artifact to select another report file or validation schema
- **THEN** collection continues to require the steward-retained assigned destination and schema and rejects the mismatch

#### Scenario: Worker changes its launch snapshot
- **WHEN** a native worker modifies a workspace artifact to replace the recorded initial repository identity
- **THEN** failure classification uses the steward-retained launch identity and cannot classify changed state as fallback-safe

#### Scenario: Worker changes or removes lease state
- **WHEN** active writer-lease data differs from the complete steward-retained launch tuple or is absent
- **THEN** collection and failure classification reject automatic transition and fallback authority

#### Scenario: Worker attempts self-release with authentic artifacts
- **WHEN** a native worker uses every authentic value readable from its prompt, manifest, report, trace, or lease to invoke release, collection, or failure classification
- **THEN** the request is rejected because it did not arrive through the steward-owned control channel with the active one-shot capability
- **AND** the writer remains active
- **AND** another native or script writer cannot acquire the primary worktree

#### Scenario: Stale launch generation is replayed
- **WHEN** a prior manifest or lease is restored after its generation was consumed or superseded
- **THEN** release, collection, failure classification, and fallback are rejected
- **AND** the stale generation cannot affect the active writer or authorize a transition

#### Scenario: Broker state or control channel is lost
- **WHEN** the broker state, private control channel, active generation, or one-shot capability is unavailable or inconsistent
- **THEN** automatic release, transition, recovery, and fallback are denied
- **AND** the steward refreshes actual repository state before using a protected recovery path

#### Scenario: Live guard-plane helper is alterable
- **WHEN** a workspace-writing subagent could alter any executable or data dependency used to validate, refresh,
  classify, release, recover, or admit a competing writer
- **THEN** native primary-worktree writer preparation is denied before that subagent starts
- **AND** altered live helpers cannot create transition or fallback authority for a nonexistent native launch

#### Scenario: Steward collects an intact launch
- **WHEN** the steward requests collection through its private control channel for the exact active generation
- **AND** the launch tuple, handoff integrity, lease, report validation, and refreshed repository state all succeed
- **THEN** the broker atomically consumes the one-shot capability before releasing the writer
- **AND** the guarded workflow may return transition authority for that exact launch

### Requirement: Repository identity is bound to one target root
Every repository identity SHALL derive HEAD, staged-tree identity, and the content-sensitive dirty-state fingerprint from
the same target repository root. Its result MUST be independent of the caller's current working directory, including
when Git returns relative index or object paths.

#### Scenario: Identity helper is called from another repository
- **WHEN** the identity helper located in repository B is invoked while the caller's current directory is repository A
- **THEN** the reported HEAD, staged tree, and dirty-state fingerprint all describe repository B
- **AND** no index or object state from repository A contributes to the result

#### Scenario: Identity helper is called from its target repository
- **WHEN** the same repository state is measured from inside and outside the target repository
- **THEN** both calls return the same HEAD, staged tree, and dirty-state fingerprint

### Requirement: Native handoffs keep bulk payloads out of the steward context
Any future eligible formal native path SHALL use shared ignored files for the canonical rendered prompt, structured
worker report, full refreshed repository state, and detailed diagnostics. The retained internal broker fixture SHALL
verify that spawn and collection exchanges contain only the bounded assignment reference, protocol-critical summary,
and artifact locations. This requirement does not grant current native-writer authority.

#### Scenario: Native worker is spawned
- **WHEN** native preparation has rendered the canonical role prompt to an ignored shared file
- **THEN** the steward delegates a short assignment instructing the fresh worker to read and execute that file
- **AND** the steward does not copy the complete rendered prompt into the primary conversation

#### Scenario: Native report is collected
- **WHEN** report validation and repository refresh complete
- **THEN** collection returns a compact transition-oriented result and paths or digests for detailed artifacts
- **AND** it does not inline the complete report plus complete refreshed repository state into the steward-facing result

#### Scenario: Detailed evidence is needed
- **WHEN** the steward or a reviewer needs facts omitted from the compact result
- **THEN** the complete ignored report, refreshed-state, and trace artifacts remain available for targeted inspection

### Requirement: Independent review evaluates an isolated staged snapshot
Every independent review SHALL evaluate the exact staged Git tree selected by the steward in an isolated review
worktree. The review mechanism MUST detect tracked, non-ignored untracked, staged, or committed snapshot changes and
MUST reject a ready verdict when the review snapshot was modified.

#### Scenario: Review begins
- **WHEN** the steward launches an independent review
- **THEN** the reviewer receives an isolated detached worktree representing the current staged Git tree

#### Scenario: Reviewer changes the snapshot
- **WHEN** the review worktree contains a tracked, staged, or non-ignored untracked change after review
- **THEN** the review result is rejected as a process failure even if the reviewer reported ready

#### Scenario: Reviewer commits a snapshot change
- **WHEN** the detached review `HEAD` or tree differs from the selected snapshot even though porcelain status is clean
- **THEN** the review result is rejected and cannot approve the staged implementation

#### Scenario: Native review cannot be isolated
- **WHEN** native delegation cannot bind a reviewer to the isolated staged snapshot with the required cleanliness checks
- **THEN** the steward uses the script-backed independent-review path

### Requirement: Review approval remains bound to tree identity
The steward MUST record the staged tree identity immediately before independent review and MUST compare it with the
staged tree identity after review. A ready verdict SHALL be stale when those identities differ.

#### Scenario: Staged tree is unchanged
- **WHEN** an independent review returns ready and the staged tree identity is unchanged
- **THEN** the steward may accept the approval subject to the remaining finalization gates

#### Scenario: Staged tree changed during review
- **WHEN** the staged tree identity after review differs from the identity recorded before review
- **THEN** the steward rejects the approval as stale and requires fresh review of the new staged state

### Requirement: Native and script backends preserve one workflow contract
Native and script-backed workers SHALL use the same version-controlled role prompts, role boundaries, report semantics,
workflow transitions, commit-authorization rules, and validation obligations. Backend selection MUST NOT change the
meaning of apply, review, remediation, or finalization.

#### Scenario: Same role uses a different backend
- **WHEN** the steward executes the same role through native delegation in one environment and the script path in another
- **THEN** both workers receive semantically equivalent role instructions and reports governed by the same validation contract

#### Scenario: Commit is not authorized
- **WHEN** any backend reaches a state that is otherwise commit-ready without explicit user authorization
- **THEN** the workflow stops at a validated staged commit-ready worktree without creating a commit

#### Scenario: Native execution fails before repository work begins
- **WHEN** native delegation fails or is interrupted and steward-retained repository identity remains unchanged
- **THEN** the steward may classify infrastructure failure and safely use the script fallback

#### Scenario: Native execution fails after repository state may have changed
- **WHEN** a native worker fails and refreshed repository identity differs from the steward-retained launch identity
- **THEN** the steward forbids blind script fallback and classifies actual Git and OpenSpec state

#### Scenario: An already-dirty path changes after launch
- **WHEN** an existing tracked-unstaged or non-ignored-untracked path changes content, mode, or type after launch
- **THEN** refreshed repository identity differs and blind fallback is forbidden

#### Scenario: Dirty launch state remains unchanged
- **WHEN** dirty state is content-identical to launch and only ignored diagnostics or build outputs changed
- **THEN** failure classification may treat repository state as unchanged and permit safe fallback

### Requirement: Validation is proportional to the changed workflow surface
Apply, remediation, review, and finalization SHALL validate the workflow helpers, agent configuration, OpenSpec contract,
and Git behavior exercised by this change. They MUST NOT run unrelated Scala/SBT tests, Scala formatting, Scaladoc, or
JMH merely as a generic ceremony when no Scala source or build definition changed.

#### Scenario: Implementation remains workflow-only
- **WHEN** the staged change is limited to agent instructions, workflow helpers, agent configuration, workflow tests, and OpenSpec artifacts
- **THEN** validation runs focused helper syntax, workflow regressions, configuration smoke tests, strict OpenSpec checks, and Git checks
- **AND** it does not invoke the unrelated Scala/SBT validation suites

#### Scenario: Implementation unexpectedly changes Scala or build behavior
- **WHEN** implementation expands into Scala sources or SBT configuration
- **THEN** the steward treats that as a scope change and adds validation proportionate to the newly changed surface

### Requirement: Efficiency claims are evidence-based
The project SHALL distinguish primary-thread context reduction, worker model usage, and elapsed validation time when
evaluating native orchestration. It MUST NOT claim lower total model-token consumption without representative measured
evidence.

#### Scenario: Native preference is evaluated
- **WHEN** the change is ready to enable native orchestration for later proposals
- **THEN** validation records representative handoff sizes and elapsed orchestration observations for the old and new paths
- **AND** documents which cost dimensions improved, remained unchanged, or were not measurable
