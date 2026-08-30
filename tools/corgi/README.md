# Coding Corgi Flow pilot

This directory pins the reversible Corgi delivery pilot. Corgi v4 bootstrap, the accepted Foundation/portfolio RFCs,
and semantic rehydration are complete. Admission is enabled for exactly the five names in `pilot.json`; all other
changes remain rejected. GitHub-native dependencies gate claims: Issues #7 and #8 are blocked by the external
instrument-economics tracker #11, while Issues #9 and #10 are blocked by #7.

## Reproduce the runtime and preflight

```bash
cd tools/corgi
npm ci --ignore-scripts --no-audit --no-fund
cd ../..
.agent/bin/corgi-qualify
```

The preflight is read-only. It verifies the exact npm version, integrity, and MIT package metadata; inventories active
changes; checks the Codex skill target; checks GitHub CLI authentication; and reports whether the project-owned
admission switch is enabled. In a sandbox that cannot read the macOS credential store, run GitHub-dependent checks in
the normal user shell.

## Activation evidence

Bootstrap cleared its zero-active-change migration gate, installed the RFC/Memory/Wiki foundation, preserved hook
opt-out, replaced the old project-local skills with 27 verified user-level Corgi skills, and restored GitHub CLI
authentication. The exact legacy source trees and dependency map are recorded in `migration-manifest.json`.

Admission was enabled only after:

1. the Foundation RFC was validated, explicitly accepted, merged, and effective;
2. the architecture-portfolio RFC with five delivery Slices was validated, explicitly accepted, merged, and effective;
3. Corgi Propose plus semantic reconciliation rehydrated all five changes from the preserved source commit;
4. every package passed strict planning readiness with complete RFC acceptance traceability;
5. Issues #6–#10 received finalized Task Group dashboards and the native dependency relationships were verified;
6. exactly those five names were added to `admittedChanges` while ready/reviewer/merge/release authority remained off.

Release-candidate evidence: `corgispec validate` searches project-local skill directories even though v4 installs Codex
skills user-level; use `corgispec doctor` to verify that installation. Bootstrap `verify` also rejects this valid
`spec-driven` installation because its managed project-file set is empty. Doctor, Memory/Wiki lint, RFC, and lifecycle
commands remain available; retain both observations for the final adoption decision.

Do not hand-edit Corgi Run Contract state under `.corgi/loop`.

## Delivery commands

The wrapper owns no lifecycle database. Corgi, Git, and GitHub are re-read before each operation.
Delivery worktrees reuse the exact-version runtime installed under the primary worktree when their own ignored
`node_modules` is absent; the wrapper verifies the binary version before use.

```bash
# From the primary worktree: create exactly one branch/worktree for an admitted change.
.agent/bin/corgi-pr admit <change>

# From the finalized planning worktree: require closed native blockedBy dependencies,
# take the Corgi owner/session claim, and let Corgi create its planning-baseline commit.
.agent/bin/corgi-pr claim <change> --owner <agent-id> --session <durable-session-id>

# From that delivery worktree, after the first acknowledged Task Group commit:
.agent/bin/corgi-pr open <change>
.agent/bin/corgi-pr sync <change>

# Publish an automated whole-change review only while both local and PR head equal the reviewed SHA:
.agent/bin/corgi-pr review <change> --report <local-report.json> --reviewed-sha <full-sha>
```

`open` and `sync` may only fast-forward push `corgi/<change>` and create/update its one draft PR. They reject a dirty
worktree, branch mismatch, remote divergence, ambiguous PR, stale head, marker mismatch, missing Corgi issue, or missing
Apply checkpoint.

`claim` writes only the current four-field CAS handoff under ignored `.corgi/adapter/`; it is not an independent
lifecycle database. Corgi's Run Contract remains authoritative. The token file is mode `0600` and is never projected to
GitHub.

`ready`, `merge`, and `finalize` exist but require both an explicit command flag and enabled authority in `pilot.json`.
The checked-in pilot grants neither ready nor merge authority. Merge rechecks the exact head, native issue dependencies,
CI, review decision, and merge state. Finalize accepts a local token file only under ignored `.corgi/` and resumes
tracker confirmation/Archive finish only after GitHub confirms the exact PR merged.

The optional PR review projection is separate from canonical Corgi Verify and Human Review. Its local JSON contract is:

```json
{
  "schemaVersion": 1,
  "verdict": "ready",
  "summary": "No actionable findings.",
  "findings": [],
  "repository_unchanged": true
}
```

A `blocked` verdict requires one or more findings with `id`, `severity`, `title`, `location`, `evidence`, and
`smallest_remediation`. Publication validates the report internally, rechecks the exact local and GitHub PR head, and
adds or updates a bounded PR comment. It does not make the human Corgi decision or transition Run Contract state.

## Rollback

The exact pre-migration source remains at the commit recorded in `migration-manifest.json`. The pilot now has Issues,
worktrees, and finalized planning state, so rollback must use the supported lifecycle and preserve tracker evidence.

After an activated pilot, rollback is intentionally explicit and recoverable:

- stop new admissions and set `enabled: false`;
- finish or withdraw each Corgi Run through its supported lifecycle rather than deleting `.corgi/loop`;
- retain remote WIP branches unless remote deletion is separately authorized;
- close draft PRs and issues only with explicit authority and preserve their URLs in the rollback record;
- remove clean registered worktrees with `git worktree remove`, then delete only the exact local pilot branches;
- use Corgi's bootstrap backup/manifest to restore overwritten or removed project assets;
- remove hooks, Memory/Wiki/RFC generated assets, package runtime, and ignored local state only after ownership checks;
- rerun strict OpenSpec validation and the existing workflow regressions.

Never force-push, delete a remote branch, merge, tag, publish, or release as part of implicit cleanup.
