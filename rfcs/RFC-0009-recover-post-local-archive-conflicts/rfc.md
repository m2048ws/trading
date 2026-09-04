# RFC-0009-recover-post-local-archive-conflicts: Recover post-local Archive integration conflicts

## Goal

Allow a delivery that completed `corgispec archive --local` but cannot pass its required integration gate to return to
the ordinary Run Contract v3 repair workflow without rewriting Git history, hand-editing lifecycle state, bypassing
the guarded provider adapter, or losing canonical evidence.

The recovery shall be available only before tracker closeout and only while the exact local Archive closeout commit,
open Issue, unmerged pull request, and clean registered worktree still agree. It shall reverse the tracked closeout
projection through one auditable recovery commit, preserve the original archive attempt and evidence in Run Contract
history, restore the active Change, and require a repair successor to integrate the current base, resolve the actual
conflicts, and repeat Verify, Human Review, Human QA, and Archive.

## Non-goals

- Do not reopen an archived Run, closed Issue, merged pull request, deleted worktree, published release, or delivery
  whose tracker closeout has begun or completed.
- Do not reset, rebase, amend, force-push, delete, replace, or hide the verified final commit, Archive closeout commit,
  remote branch, pull request, Issue, Run events, evidence, or delivery history.
- Do not automatically choose application-code, specification, generated-knowledge, or conflict-marker resolutions.
  Recovery restores a normal repair boundary; the repair Task Group owns and verifies all semantic reconciliation.
- Do not weaken the existing clean deterministic base-update wrapper accepted by the integration adapter, or treat a
  manually resolved post-Archive merge as if it were the previously verified final revision.
- Do not make the Corgi CLI provider-aware. GitHub/GitLab state and mutations remain responsibilities of guarded
  project adapters; the provider-neutral CLI owns local Git, Change, evidence, and Run Contract transitions.
- Do not provide general undo, unarchive, Issue reopen, arbitrary commit revert, or lifecycle-state editing commands.
- Do not change trading-domain APIs, calculations, schemas, dependency direction, or the JDK 25 baseline.

## Boundary

The provider-neutral Corgi lifecycle shall add one explicit post-local Archive recovery operation. Its public command
shall require a non-empty reason and the current four-field CAS token. It is admissible only when the Run is
`archiving`, `archive.localCompleted` is true, `archive.trackerCompleted` is false, the closeout commit and archived
root are recorded, and the worktree is clean at that exact closeout commit. The closeout commit must remain a direct
child of the verified final revision and its archived Change, manifest, delivery page, RFC delivery sidecar, and
knowledge checkpoint must pass the existing sealed Archive integrity checks.

Recovery shall create exactly one ordinary commit whose parent is the Archive closeout commit and whose tree is
byte-for-byte equal to the verified final revision's tree. This commit therefore restores the active Change and its
pre-Archive delivery sidecar while removing only the tracked Archive projection created by the direct closeout child.
It shall not discard the closeout commit: both the original closeout and recovery commits remain in local and remote
history. Canonical evidence retained by the Run store remains immutable and available to the eventual archived
manifest; reproducible materialized evidence may be generated again by the successor Archive.

The recovery event shall bind the reason, original final revision, closeout commit, evidence-manifest hash, recovery
commit and tree, and restored Change/source/traceability identities. It shall transition the predecessor Run to
`repair_required` with kind `implementation` and failed phase `archive_integration`. The existing planning-only update
and `corgispec change repair` workflow then creates exactly one successor Run using the same Slice, Issue, branch,
worktree, and pull request. Prior completed Task Groups may remain immutable; the planning package appends a dedicated
repair Task Group for base integration, explicit conflict resolution, focused and aggregate checks, and automated
review. Verify and every human gate are invalidated and must be repeated against the successor final revision.

The local transaction shall be journaled and resumable. A retry with the same token after an unknown outcome shall
either complete the same recovery commit and one Run event or return the already completed result; it shall never
create a second recovery commit, duplicate event, or partially restore tracked files. A deterministic validation
failure before mutation shall leave Git, Change, evidence, and Run state unchanged. A failure after preparing tracked
files shall restore the exact closeout tree unless the journal proves that the recovery commit already exists.

The project GitHub adapter shall expose one guarded recovery command rather than asking operators to call provider
tools directly. Under the existing integration lock it shall require the admitted change, configured worktree and
branch, exact local/remote/PR closeout head, canonical Human Review approval, open bound Issue without the `done`
label, unmerged pull request, enabled repair authority, and the Corgi recovery token. It shall first return a ready PR
to draft and recheck all remote identities, preventing a concurrent merge before local recovery. It shall then invoke
the provider-neutral operation, fast-forward the existing remote branch to the exact recovery commit, refresh the
bounded PR body, and retain the Issue and PR. It shall never force-push, delete a branch, create a second Issue or pull
request, confirm tracker closeout, or resolve content conflicts.

Adapter recovery shall also be resumable across provider failures. If the local recovery succeeded but publication
did not, the ignored adapter handoff and authoritative Run state shall let the same command verify and publish the
existing recovery commit. If the PR was made draft but local recovery did not occur, the command shall report that
safe boundary and permit an ordinary re-ready operation or an exact retry. Every provider mutation shall use expected
head identity and be confirmed by a fresh read.

After recovery, the repair successor shall integrate the latest configured base only through the existing admitted
first-Task-Group merge topology. Conflicted files are then ordinary repair work: each owner keeps its semantics,
generated Memory/Wiki content is reconciled through Corgi-owned transitions, and the combined result receives focused
tests, the clean repository matrix, automated review, canonical Verify, explicit Human Review, Human QA, and a new
Archive transaction. The original failed integration attempt remains auditable in predecessor Run and Git history.

The pinned project-local Corgi build, checksum/integrity metadata, skills, `tools/corgi/README.md`, adapter help, and
qualification checks shall describe one consistent recovery protocol. Tests shall cover the pure state transition,
Git transaction, crash points, invalid-state matrix, provider-adapter orchestration, exact fast-forward publication,
and a realistic two-delivery integration conflict. Production and test fixtures shall use temporary repositories and
provider doubles; they shall never mutate a developer's live Run, Issue, or pull request.

## Slices

### S-01-recover-post-local-archive-conflicts: Return a local Archive integration conflict to formal repair

- AC-001 [evidence: automated]: The provider-neutral recovery command accepts only an exact clean
  `archiving` Run with local Archive complete, tracker closeout incomplete, a sealed direct-child closeout commit,
  matching archived Change/evidence/delivery/knowledge identities, a current four-field CAS token, and a non-empty
  reason; each stale, dirty, missing, already-tracked, already-archived, or structurally invalid case fails before
  mutation with a specific typed/structured diagnostic.
- AC-002 [evidence: automated]: One successful recovery creates a single commit parented by the Archive closeout whose
  tree exactly equals the verified final tree, restores the active Change and pre-Archive RFC delivery projection,
  retains the closeout commit and canonical evidence in history, and records an evidence-bound
  `implementation/archive_integration` repair requirement without hand-editing Run state or generated knowledge.
- AC-003 [evidence: automated]: Transaction and crash-point tests prove same-token retry idempotence, no duplicate
  commit or Run event, exact rollback before commit, recovery of an already-created commit after an unknown outcome,
  and rejection of any second or stale recovery attempt.
- AC-004 [evidence: automated]: The guarded GitHub adapter requires the exact admitted branch/worktree/Issue/PR/archive
  identities and enabled explicit authority, returns the unmerged PR to draft before local recovery, publishes only a
  fast-forward to the exact recovery commit, preserves the single open Issue and pull request, and resumes safely when
  either local execution or provider synchronization was interrupted.
- AC-005 [evidence: both]: A temporary-repository integration scenario demonstrates two independently verified
  deliveries that conflict only after one completes local Archive; an operator recovers the blocked delivery, creates
  one repair successor, explicitly resolves the combined semantics against current base, repeats every verification
  and human gate, and archives with both delivery histories and all original evidence intact.
- AC-006 [evidence: automated]: The pinned Corgi artifact, integrity metadata, project adapter, skills, help text,
  qualification checks, and repository documentation agree on the recovery protocol; the complete Corgi package
  suite, adapter tests, artifact/mirror validation, production dependency audit, and repository CI pass on the
  supported toolchain without weakening force-push, branch-deletion, tracker, merge, tag, publish, or release guards.

## Risks

- Reversing an Archive closeout could remove unrelated work. Mitigation: require a clean exact direct-child closeout,
  verify its sealed checkpoint, and create a recovery tree identical to the verified final tree rather than applying
  a path list or destructive reset.
- Git, Run Contract, and provider mutations cannot be one physical transaction. Mitigation: journal the local stages,
  use CAS and expected-head checks, make the PR draft before recovery, retain a mode-0600 adapter handoff, and make
  every boundary idempotently resumable.
- A PR could merge while recovery begins. Mitigation: hold the integration lock, require an unmerged exact head, move
  it to draft first, and re-read PR, remote branch, Issue, and base identities before local mutation.
- Restoring the Change could be mistaken for invalidating historical evidence. Mitigation: retain the final and
  closeout commits plus predecessor Run events and canonical evidence, while requiring the successor to create fresh
  Verify, Review, QA, and Archive evidence.
- Operators could use recovery to evade a legitimate failed gate. Mitigation: admit only post-local/pre-tracker
  integration failures, require a reason, force `repair_required`, and provide no path directly back to Archive.
- Conflict resolution could silently prefer one delivery. Mitigation: recovery performs no resolution; one named
  repair Task Group owns the merge, owner-by-owner reconciliation, completed-artifact checks, aggregate tests, and
  automated review before a new final revision is accepted.
- Updating the project-local package could drift from upstream Corgi. Mitigation: keep a self-contained patch against
  the exact upstream commit, use a unique local version and checksums, run the full package/mirror/audit matrix, and
  remove the recovery fork when an equivalent upstream release is adopted.
