## 1. Permanent Zero-Tolerance Reflection Guard

- [ ] 1.1 Confirm the effective RFC/source binding, exact `origin/main` planning baseline, isolated worktree identity,
  pilot authority, closed native dependencies, and clean pre-implementation source-guard result.
- [ ] 1.2 Simplify `tools/check-in-process-reflection.sh` to reject every established prohibited token with
  deterministic path-and-line diagnostics, remove allowance lookup and per-file migration counts, and delete
  `tools/in-process-reflection-baseline.tsv`.
- [ ] 1.3 Add a POSIX-shell regression fixture that executes the tracked guard against a trap-cleaned temporary source
  tree and proves one prohibited production token produces a deterministic non-zero result without touching tracked
  production or benchmark source.
- [ ] 1.4 Add the real repository guard and its regression fixture to the normal GitHub Actions build job before the SBT
  gate, preserving the existing pull-request, main-branch, and manual triggers.
- [ ] 1.5 Run the guard and regression fixture, inspect their stable pass/fail diagnostics and tracked-file cleanliness,
  and run strict OpenSpec/Corgi validation for the modified functional-design contract.

## 2. Runtime Build Cleanup and Integration Verification

- [ ] 2.1 Remove only the runtime project's obsolete Java-bridge comment, `JavaThenScala` compile-order override, and
  duplicate project-local JDK release option while retaining the authoritative `ThisBuild` JDK 25 settings.
- [ ] 2.2 Run a clean focused runtime compile and test, confirming no source-order dependency or project-local Java
  bridge remains and the runtime still inherits the repository toolchain contract.
- [ ] 2.3 Run formatting checks, the permanent guard and its fixture, strict OpenSpec/Corgi validation, and the clean
  aggregate test plus benchmark-compilation gate used by CI.
- [ ] 2.4 Inspect the completed diff and test evidence to confirm no production API, dependency, domain result, wire
  format, repository-level Java setting, or runtime behavior changed outside the accepted slice.
