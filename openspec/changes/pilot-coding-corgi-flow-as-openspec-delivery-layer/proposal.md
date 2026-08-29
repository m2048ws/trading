## Why

The repository's current OpenSpec workflow provides strong worker isolation, independent review, and mechanical
handoffs, but its custom execution machinery is primary-worktree-oriented and does not provide a GitHub-native delivery
surface. Coding Corgi Flow preserves the OpenSpec proposal/spec/design/task model while adding change worktrees,
checkpoint commits, recovery, issue tracking, verification, human review, QA, and archive evidence. Because parallelism
across changes is sufficient for this project, Corgi's sequential in-change Task Groups are not a blocker.

A permanent cutover would nevertheless change commit semantics, review identity, knowledge promotion, and the point at
which GitHub considers work delivered. A reversible pilot is required to prove that Corgi can operate under the
repository's existing safety boundaries and support one issue, branch, worktree, and reviewable PR per change before it
becomes the default delivery layer.

## What Changes

- Introduce a reversible, project-local pilot of one exactly pinned MIT-licensed Coding Corgi Flow release while
  retaining OpenSpec as the canonical requirements and delta-spec authority.
- Exercise the Corgi Explore -> RFC -> Propose/Update/Ready -> Apply -> Verify -> Human Review -> Human QA -> Archive
  lifecycle without migrating active changes or making Corgi the default before an explicit adoption decision.
- Use one exclusive change claim, GitHub issue, isolated worktree, WIP branch, and draft delivery PR per Corgi-managed
  change; allow separate changes to execute concurrently while keeping Task Groups within a change sequential.
- Grant Corgi-managed pilot changes standing authority to create local Task Group and Archive commits, fast-forward push
  only their WIP branches, and create or update one draft PR. Continue to require explicit authority for force-push,
  protected-branch push, ready-for-review transition, reviewer requests, merge, remote deletion, tag, publish, or release.
- Add a thin project-local PR wrapper that validates exact change/issue/branch/worktree/PR/SHA identity, publishes only
  safe evidence, synchronizes Corgi phases with GitHub, and is idempotent across retries and partial GitHub outages.
- Dual-record review: retain machine-verifiable Corgi/local evidence while publishing whole-change independent findings,
  checks, and human discussion on the GitHub PR. Keep fresh independent review separate from implementation and bind
  every approval to the exact reviewed tree.
- Pause the resumable Archive transaction after local archive materialization, push and review the final archive commit,
  require explicit PR merge, and only then confirm tracker closeout and remove the worktree.
- Keep canonical OpenSpec specifications and project documentation authoritative. Limit Corgi Memory/Wiki output to
  derived delivery evidence, change summaries, and discovered pitfalls that link back to canonical sources.
- Run qualification scenarios for concurrent changes, interrupted Apply recovery, rejected review and repair, remote
  divergence, exact-SHA enforcement, archive/merge resumption, and complete rollback. End with an explicit
  `ADOPT`, `ADAPT`, or `REJECT` decision; permanent cutover requires a follow-up change.
- Fail closed in a disabled prepared state when the selected Corgi release cannot migrate alongside active changes;
  activation and live scenarios then move to a reviewed follow-up rather than withdrawing or rewriting those changes.

## Capabilities

### New Capabilities

- `corgi-delivery-workflow`: Qualify Coding Corgi Flow as a guarded OpenSpec delivery layer with isolated change
  execution, controlled GitHub publication, dual-record review, resumable archive/merge integration, and reversible
  adoption evidence.

### Modified Capabilities

- `steward-worker-orchestration`: Extend writer serialization and independent-review identity from the primary staged
  worktree to explicitly claimed Corgi change worktrees and exact committed PR-head trees during the pilot.

## Impact

The pilot may affect project-local agent configuration, workflow prompts and guards, focused workflow tests,
`openspec/config.yaml`, Corgi-managed project assets, and a new GitHub PR wrapper. It may create explicitly authorized
pilot branches, issues, commits, and draft PRs, but it does not authorize merge or make Corgi the default workflow. It
does not change Scala source, SBT definitions, production modules, published artifacts, domain semantics, or public
library APIs. An unsuccessful pilot must leave active non-pilot changes and canonical OpenSpec specifications intact.
