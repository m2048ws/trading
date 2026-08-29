# Qualification Evidence

Date: 2026-08-29

Outcome: **prepared but not activatable**. This is not the pilot's final `ADOPT`, `ADAPT`, or `REJECT` decision because
the required live feature, maintenance, concurrency, GitHub, review/repair, Archive, and rollback scenarios cannot run
until the upstream migration gate clears.

## Release identity

- Selected: `corgispec@4.0.0-rc2`.
- npm integrity: `sha512-J+i7s4BthgIDvnMFTq/o/moRmnELa9EZ7So/a+3x+0CcUvzgASt/mESHX+/aTd3KuETk5onEkXZqZUhV/RX+ZQ==`.
- npm metadata and the lockfile report `MIT`; Node requirement is `>=20.19.0`.
- Local qualification used Node `26.5.1`, npm `11.17.0`, OpenSpec `1.7.0`, and Corgi `4.0.0-rc2`.
- Stable `corgispec@3.0.1` was rejected: its Apply command is a read-only next-group query and Archive only emits
  instructions. It lacks v4's durable Run Contract v3, CAS evidence, repair successors, and resumable Archive phases.

The exact dependency and transitive graph are pinned in `tools/corgi/package-lock.json`; `npm ci` installs only the
ignored project-local runtime.

## Isolated bootstrap inventory

Bootstrap was run first against a disposable clone. It:

- changed `.gitignore`, `AGENTS.md`, and `openspec/config.yaml`;
- created `memory/`, `wiki/`, `rfcs/RFC-0001-project-foundation/`, `openspec/.corgi-install.json`, and an install report;
- created timestamped backups under `openspec/.corgi-backups/`;
- removed the six existing project-local `.codex/skills/openspec-*` directories after backing them up;
- initialized the RFC-first contract and mandatory Session Bridge/Memory/Wiki structure; and
- left hooks disabled by default.

The bundled Codex installer targets the user-level `~/.codex/skills` directory. Bootstrap did not install replacement
Corgi skills project-locally. Current `doctor` reports that user-level target unwritable in this execution sandbox. This
must be resolved explicitly; silently deleting the current skills is not acceptable.

Two release-candidate defects were observed in the fixture:

1. An OpenSpec `skip_specs: true` change produces a `skipped` artifact status that Corgi rejects as outside its
   OpenSpec 1.6 status contract, despite OpenSpec 1.7 accepting the change.
2. Maintenance Propose created a `README.md` that its own finalize cleanliness check classified as unrelated until the
   generated file was removed.

The repository's pilot has concrete delta specs, so the first defect does not affect this planning package, but both are
adoption risks to retest against the next Corgi release.

## Lifecycle fixture

After supplying a concrete spec-bearing maintenance package, the disposable clone completed:

1. maintenance Propose and strict readiness;
2. finalized planning handoff;
3. Corgi-owned planning-baseline commit;
4. Run Contract v3 Apply start with one owner/session and four-field CAS token;
5. one dedicated Task Group commit with bounded automated evidence;
6. rejection of a path outside the classified maintenance scope;
7. rejection of a stale Session Bridge checkpoint;
8. successful exact workspace-fingerprint acknowledgement; and
9. transition to `awaiting_verify` with canonical commit/tree/evidence hashes.

This established the supported JSON used by the PR wrapper without copying any nonce or CAS token into projected PR
state.

## Activation blockers

The actual repository dry-run stops before mutation:

> v4 migration requires every repository worktree to have no active Change or nonterminal Run Contract.

It names seven active changes: this pilot plus the six pre-existing changes. Corgi checks every registered worktree, so
a sibling worktree or second clone does not provide a safe in-repository bypass. Migrating now would require deleting,
withdrawing, or changing the lifecycle of active work, which the proposal expressly forbids.

Additional external blockers are invalid GitHub CLI authentication and the unresolved user-level Codex skill target.
The checked-in `pilot.json` therefore has `enabled: false` and no admitted changes. `corgi-qualify` must report ready
before a follow-up change may flip that switch.

## Implemented dormant boundary

- Exact package pin, lockfile, integrity check, and read-only activation preflight.
- Explicit worktree/branch admission with no lifecycle database.
- Per-worktree writer locking and repository-common integration locking.
- Fast-forward-only WIP publication; no force option exists in the wrapper.
- One marker-bound draft PR per Corgi issue/change/base/head identity.
- Bounded phase/Task Group/SHA projection with private-path and credential rejection.
- SHA-bound independent-review comment upsert; the complete structured report stays local.
- Exact committed PR-head detached review support while preserving legacy staged reviews.
- Native GitHub `blockedBy`, exact-head CI/review/merge gates, and explicit ready/merge flags.
- Post-merge tracker confirmation and Archive finish from a local ignored token file.
- Idempotency and failure regression coverage for dormant admission, duplicate sync, divergence, marker mismatch,
  redaction, writer exclusion, exact review target, and merge checks.

No repository commit, Corgi bootstrap mutation, hook, issue, worktree, pilot branch, push, PR, review comment, merge, tag,
publication, or release was created during actual-repository qualification.

## Rollback boundary

The current dormant state is removed by reverting the tracked pilot files; ignored `tools/corgi/node_modules` can then be
deleted without touching canonical specs or active changes. Activated rollback additionally requires exact issue/PR/
branch/worktree inventory, supported Corgi Run completion or withdrawal, backup-manifest restoration, explicit authority
for any remote closure/deletion, and strict validation. The executable checklist is in `tools/corgi/README.md`.
