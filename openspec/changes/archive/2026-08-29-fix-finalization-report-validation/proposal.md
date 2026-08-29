## Why

A successfully committed finalization currently produces an honest clean-index report with `git.staged: false`, but the logical validator requires `git.staged: true` for both commit-ready and committed outcomes. This caused the first dogfooded finalization report to be rejected after its authorized commit had already succeeded, so the status-dependent report contract must be made coherent before it is relied on again.

## What Changes

- Distinguish the Git and commit-state invariants for `commit_ready` and `committed` finalization reports.
- Require commit-ready reports to describe intended staged changes and no created commit.
- Require committed reports to describe a clean index, an explicitly authorized created commit, and equality between the reported Git HEAD and commit hash.
- Add validator and launcher-boundary regressions for valid and contradictory forms of both outcomes.
- Keep the separate post-archive remediation lifecycle question out of scope.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `steward-worker-orchestration`: Refine mechanically validated finalization-report semantics for commit-ready versus committed outcomes.

## Impact

The change affects only the agent logical report validator, focused workflow tests, and the steward-worker-orchestration specification. It changes no Scala source, SBT/build definition, production module, dependency, public library API, or native-writer eligibility boundary.
