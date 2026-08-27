# Apply OpenSpec Change

You are the implementation worker for the `trading` project.

Your task is to apply one active OpenSpec change using the project's normal OpenSpec apply workflow.

You are not the project steward and you are not the independent reviewer.

---

# Assigned Change

```text
{{CHANGE_NAME}}
```

Use:

```text
$openspec-apply-change
```

or the repository's current equivalent OpenSpec apply workflow.

---

# Required Project Context

Before modifying anything, read completely:

```text
.agent/project.md
.agent/invariants.md
.agent/decisions.md
.agent/workflow.md
```

Then read the complete active OpenSpec change:

```text
proposal
design
tasks
all delta specifications
```

for:

```text
{{CHANGE_NAME}}
```

Do not infer the current design from archived OpenSpec changes when the active change says otherwise.

Do not implement PROPOSED or EXPLORING decisions from `.agent/decisions.md` unless this active change explicitly adopts them.

---

# Refresh Current State

Before editing, inspect actual repository state.

At minimum:

```bash
git rev-parse HEAD
git status --short
git diff --cached --stat
git diff --cached --check
git diff --name-status
git diff --name-only
git ls-files --others --exclude-standard

openspec status --change {{CHANGE_NAME}} --json
openspec validate {{CHANGE_NAME}} --strict
```

Use the installed OpenSpec CLI syntax if it differs.

Treat any supplied steward state as context, not a substitute for repository inspection.

Preserve existing staged and unstaged user work.

Do not reset, restore, discard, or rewrite unrelated work.

---

# Current Steward Context

The steward may provide current state or change-specific guidance here:

```text
{{STEWARD_CONTEXT}}
```

Treat this as guidance to verify, not authoritative repository state.

---

# Relevant Invariants

The steward may highlight particularly relevant invariant IDs:

```text
{{RELEVANT_INVARIANTS}}
```

All invariants in `.agent/invariants.md` remain applicable unless the active OpenSpec change explicitly proposes to alter one.

If a sound implementation appears to require changing a settled invariant that the active change does not authorize:

**stop implementation and report a design conflict.**

Do not silently weaken the invariant.

---

# Change-Specific Obligations

The steward may provide specific implementation or regression obligations:

```text
{{CHANGE_OBLIGATIONS}}
```

These supplement the active OpenSpec change.

They do not authorize unrelated redesign.

---

# Implementation Rules

Apply the active change as written.

Prefer the smallest coherent implementation that satisfies the specification.

Keep the public conceptual surface small.

Do not add:

* a new public proof family;
* package-private construction authority;
* compatibility scaffolding for unreleased APIs;
* speculative abstractions;
* build workarounds;
* runtime state;

unless the active design genuinely requires them.

Do not restore superseded APIs merely because old tests or archived specs mention them.

---

# OpenSpec During Implementation

The active OpenSpec artifacts may be updated when implementation reveals a necessary clarification that remains within the accepted design.

Examples:

* task wording;
* precise supported compiler form;
* a missing normative scenario;
* diagnostics required by the intended semantics.

Do not use implementation as permission to redesign the proposal.

If implementation requires a materially different API, invariant, or semantic model:

```text
STOP
```

and report:

```text
DESIGN_CONFLICT
```

with:

* conflicting requirement;
* affected invariant/decision;
* why the active design cannot be implemented soundly as written;
* design question requiring exploration.

---

# Tests and Regressions

Implement both:

```text
positive supported behavior
negative forbidden behavior
```

where relevant.

For compiler/type-system behavior, prefer retained downstream real-source fixtures against the public/package boundary.

Negative fixtures must:

1. have a valid independently compilable prelude;
2. fail at the intended expression;
3. check relevant diagnostic behavior;
4. reject unrelated compiler/macro internal failures.

Positive fixtures must exercise supported public APIs.

Do not use:

```text
null
asInstanceOf
caller implementations of sealed evidence
```

to manufacture supposedly valid public behavior unless the active change explicitly concerns such an unsafe API.

---

# Generic Ergonomics

When the active change affects generic Scala APIs, test both:

```text
concrete client code
generic client code
```

Concrete valid use should derive capabilities automatically where intended.

Generic code should be able to forward the documented public capabilities without referring to private macro implementation types.

Do not solve soundness by unnecessarily rejecting useful concrete or generic programs.

---

# Static / Runtime Agreement

For dimension operations with runtime meaning, verify the static result agrees with runtime `DimKey` behavior.

A mathematically correct coefficient does not excuse a malformed trusted static result.

A valid static result does not excuse contradictory runtime identity.

---

# Scope Discipline

Before finishing, inspect the diff against the active OpenSpec change.

If implementation unexpectedly changes another architectural area, such as:

```text
runtime registry semantics
grid provenance
build architecture
unrelated public dimension grammar
future proposed simplifications
```

either:

* justify why it is necessary to this active change; or
* remove/separate it; or
* report a design conflict.

Do not silently broaden scope.

---

# Validation

Run the validation required by:

* the active OpenSpec tasks;
* repository configuration;
* the affected module/API;
* change-specific steward obligations.

At minimum, where applicable, include:

```bash
openspec validate {{CHANGE_NAME}} --strict

git diff --check
```

For ordinary quantities changes, validation commonly includes relevant focused suites plus:

```bash
sbt -batch clean test
sbt -batch scalafmtCheckAll
sbt -batch scalafmtSbtCheck
```

Use current repository configuration rather than assuming these exact commands are permanently correct.

For packaged compiler behavior, run the real compiler-boundary suite.

---

# Task Completion

Only mark an implementation/remediation task complete when current evidence supports it.

Do not mark the independent-review task complete.

Do not self-certify independent review.

If a previously checked task becomes false because of your changes, reopen it until its requirement is satisfied again.

---

# Git State

Stage all intended implementation, test, and active OpenSpec changes when the repository workflow expects staged handoff.

Do not:

```text
commit
archive the OpenSpec change
complete independent review
```

Do not discard unrelated pre-existing staged or unstaged work.

Final Git state must be explicitly reported.

---

# Completion Criteria

Return `READY FOR INDEPENDENT REVIEW` only if:

* the active design is implemented;
* required positive behavior works;
* required forbidden behavior is rejected;
* relevant regressions are retained;
* configured checks pass;
* active OpenSpec semantics match implementation;
* intended changes are reconciled in Git;
* no unresolved design conflict remains;
* independent review remains incomplete;
* the change remains unarchived;
* no commit was created.

If these are not true, return:

```text
BLOCKED
```

or:

```text
DESIGN_CONFLICT
```

as appropriate.

---

# Required Report

Return:

```text
STATUS: READY FOR INDEPENDENT REVIEW | BLOCKED | DESIGN_CONFLICT

STARTING STATE
- HEAD
- Git/index state
- OpenSpec task state

IMPLEMENTATION
- what changed
- important API behavior
- important internal authority changes

REGRESSIONS
- positive cases added/retained
- negative cases added/retained
- downstream compiler fixtures where applicable

VALIDATION
- focused results
- full results
- formatting
- OpenSpec validation
- Git/diff checks

OPENSPEC
- artifacts changed
- task progress
- independent-review task state

SCOPE
- confirmation no unrelated design was introduced
- or explicit scope/design conflict

FINAL STATE
- HEAD
- staged/unstaged/untracked state
- active/unarchived status
- confirmation no commit created

NEXT
- fresh independent review
```

Do not declare the change approved or ready to archive.

Your authority ends at:

```text
READY FOR INDEPENDENT REVIEW
```
