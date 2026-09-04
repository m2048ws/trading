## Context

See `proposal.md` for motivation. PR #37 left the reflection guard in its migration form: it reads
`tools/in-process-reflection-baseline.tsv`, counts matches per source file, and compares each count with an allowance.
The baseline now has no entries. The guard is documented but is not invoked by `.github/workflows/ci.yml`.

The runtime project has no remaining Java privacy bridge, yet its `build.sbt` settings still force
`CompileOrder.JavaThenScala`, repeat the repository-level `--release 25` Java option, and explain the removed bridge.
The repository-wide Java and Scala release settings remain the toolchain authority.

## Goals / Non-Goals

**Goals:**

- Turn the completed reflection migration into a simple permanent invariant with deterministic diagnostics.
- Prove the invariant can detect a regression without temporarily editing tracked production source.
- Make the normal CI workflow enforce the invariant.
- Return the runtime project to ordinary repository compilation settings and prove its behavior is unchanged.

**Non-Goals:**

- Change domain APIs, runtime code, wire formats, dependencies, or the repository's JDK 25 baseline.
- Broaden the prohibited-token policy beyond the already accepted RFC rule.
- Remove repository-level Java compiler configuration or Java-library integration; later RFC slices own any such
  source-API cleanup.

## Decisions

### 1. Replace allowance accounting with one zero-tolerance result

`tools/check-in-process-reflection.sh` remains the single owner of the production and benchmark source policy. It will
retain the established source-set selection and prohibited-token vocabulary, scan paths in deterministic order, print
matching path-and-line diagnostics, and return failure if any match exists. It will not read a baseline, count allowed
matches, or distinguish old sites from new ones. The now-empty baseline file will be deleted.

Keeping an empty baseline was rejected because its format advertises an exception mechanism that the accepted RFC no
longer permits. Replacing the textual policy with compiler plugins or bytecode analysis was rejected because this slice
needs no new mechanism or dependency and the existing conservative source rule is already the accepted contract.

### 2. Test the real guard in an isolated temporary repository shape

A focused POSIX-shell regression script will create a temporary directory, copy the real guard into the same relative
location it has in the repository, create an otherwise clean `src/main` fixture containing exactly one prohibited
token, and assert that the guard exits non-zero with a stable diagnostic. Cleanup will be trap-owned. The fixture will
never write into a tracked production or benchmark tree.

A test-only bypass flag or injectable match result was rejected because it would test a second execution path. Editing
and reverting a tracked source file was rejected because interruption could dirty the worktree and because the RFC
explicitly requires an isolated fixture.

### 3. Make CI run both the policy and its regression proof

The normal GitHub Actions build job will run the zero-tolerance guard and focused regression script before the SBT
format/build/test step. This keeps the source-policy failure small and immediate while leaving the existing clean SBT
gate unchanged.

Relying on developer convention or only running the regression fixture was rejected because neither proves that the
actual repository source set is clean on every pull request and main-branch update.

### 4. Delete only runtime-owned bridge overrides

The runtime settings will lose the bridge comment, `Compile / compileOrder := CompileOrder.JavaThenScala`, and its
project-local `Compile / javacOptions`. The top-level `ThisBuild` Java and Scala release settings remain unchanged, so
runtime inherits the same JDK 25 contract as every other project. Focused runtime tests precede the clean aggregate gate
so a latent source-order dependency is localized before integration verification.

Removing all Java compiler settings was rejected because this slice concerns one obsolete runtime bridge; Scala-only
source ownership across the repository is delivered by later RFC slices.

### 5. Keep architecture and effect boundaries unchanged

The source guard owns policy detection and diagnostic exit status; SBT owns compiler and test failures. The guard reads
source text and never constructs trusted domain data. Its filesystem effects are confined to developer tooling and CI,
while the regression script owns only a temporary directory. No production module dependency changes, domain error
types, algebraic laws, concurrency behavior, or data-plane hot paths are involved.

## Risks / Trade-offs

- **A conservative token match flags a legitimate future use.** → Treat that as an explicit policy/design decision;
  change the normative rule rather than introducing a hidden allowance.
- **The regression fixture accidentally tests a copy differently from the repository guard.** → Copy and execute the
  exact tracked guard and assert both its non-zero result and diagnostic, without reimplementing matching logic.
- **Removing compile ordering exposes an undeclared runtime source dependency.** → Run a clean focused runtime build
  and tests before the complete repository gate.
- **CI and local behavior drift.** → Make CI call the same two checked-in scripts developers run locally.

## Migration Plan

1. Simplify the guard, delete the baseline, add the isolated regression fixture, and wire both checks into CI.
2. Remove the three runtime-only bridge remnants and run focused runtime verification.
3. Run strict OpenSpec/Corgi validation, formatting, the permanent guard and fixture, and the clean aggregate SBT gate
   including benchmark compilation.

Rollback is a normal Task Group commit revert. No stored data, public API, deployment ordering, or state conversion is
involved.
