## Why

PR #37 removed the final prohibited production and benchmark reflection sites, but the repository still carries the
allowance baseline and per-file counting machinery used during that migration. The runtime build likewise retains
compile-order and compiler-option overrides for a Java privacy bridge that no longer exists, while CI does not run the
source guard that is meant to keep the simplified boundary permanent.

## What Changes

- Replace the migration-aware reflection scan with a zero-tolerance production and benchmark source guard that has no
  allowance file or per-file exception accounting.
- Add a deterministic regression fixture that proves one prohibited token makes the guard fail without changing
  tracked production source, and run the permanent guard in the normal CI workflow.
- Remove the runtime project's obsolete `JavaThenScala` compile order, Java-bridge explanation, and duplicated local
  JDK release option so it inherits the repository's ordinary build settings.
- Preserve JDK 25, JVM execution, all domain and wire behavior, and Java compiler settings still owned at the
  repository level; this slice changes enforcement and obsolete build scaffolding only.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `scala-functional-design`: Make the existing no-reflective-construction rule a permanent zero-tolerance invariant
  enforced by a deterministic regression test and the normal CI workflow.
- `repository-architecture`: Require project-local build overrides to correspond to current source ownership and
  remove bridge-specific compilation settings after their owning bridge disappears.

## Impact

- Affects `tools/check-in-process-reflection.sh`, its migration baseline, a focused guard regression fixture, and
  `.github/workflows/ci.yml`.
- Affects only the runtime project settings in `build.sbt`; repository-wide JDK 25 settings remain authoritative.
- Adds no production dependency, module, API, effect, persistence change, or runtime behavior.
