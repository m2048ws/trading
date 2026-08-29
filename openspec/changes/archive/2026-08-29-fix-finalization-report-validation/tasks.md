## 1. Logical Validation

- [x] 1.1 Refactor successful finalization validation so archive, validation, diff, unstaged, and untracked gates remain common while staged/index expectations are status-specific.
- [x] 1.2 Require `commit_ready` reports to retain intended staged changes and report no authorization, no created commit, and an empty commit hash.
- [x] 1.3 Require `committed` reports to describe no staged changes, explicit authorization, a created non-empty commit hash, and equality between that hash and reported Git HEAD.

## 2. Regression Coverage

- [x] 2.1 Preserve the accepted `commit_ready` fixture and add an accepted clean-index `committed` fixture with matching Git HEAD and commit hash.
- [x] 2.2 Reject `commit_ready` reports with a clean unstaged index, authorization, creation, or a non-empty hash.
- [x] 2.3 Reject `committed` reports with staged changes, missing authorization/creation/hash, or a Git HEAD different from the commit hash.
- [x] 2.4 Exercise the valid and contradictory status products through both direct worker-report validation and the formal `run-worker` boundary without creating a real commit.

## 3. Validation and Review

- [x] 3.1 Run syntax/configuration checks and every executable under `.agent/tests`.
- [x] 3.2 Run strict validation for `fix-finalization-report-validation` and repository-wide strict OpenSpec validation.
- [x] 3.3 Verify the changed path set remains limited to workflow validation, focused workflow tests, and the active OpenSpec change; while true, do not run Scala, SBT, Scalafmt, Scaladoc, or JMH.
- [x] 3.4 Run cached and unstaged Git diff checks, inspect untracked state, and stage exactly the intended change without committing.
- [x] 3.5 Obtain fresh independent approval of the exact staged tree through the detached-snapshot script backend; any remediation returns to another fresh independent review.
