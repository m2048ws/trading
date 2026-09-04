## ADDED Requirements

### Requirement: Build configuration follows active source ownership

Project-local compiler ordering and compiler-option overrides SHALL exist only when the current sources in that project
require them. When an owning bridge or source-language constraint is removed, its project-local build override and
explanatory scaffolding SHALL also be removed so the project inherits the repository's ordinary build configuration.
Removing an obsolete project override MUST NOT silently change the repository's declared JDK baseline or the settings
still required by other source owners, the JVM platform, or external libraries.

#### Scenario: Retire a removed construction bridge

- **WHEN** a project no longer contains the Java-owned construction bridge that required Java-before-Scala compilation
- **THEN** the project has no bridge-specific compile order, duplicate JDK release option, or stale bridge explanation

#### Scenario: Preserve the repository toolchain contract

- **WHEN** an obsolete project-local compiler override is removed
- **THEN** the project still builds and tests under the repository's authoritative JDK and compiler settings
