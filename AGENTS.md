# Trading Repository Agent Instructions

This repository uses a project-steward workflow for OpenSpec-driven development.

Stable project context lives under `.agent/`.

Volatile Git, source, build, test, and OpenSpec state must be refreshed from the
repository rather than trusted from prior conversations or reports.

## Role Selection

Determine your role from the initial prompt.

### Explicit worker role

If the initial prompt explicitly assigns one of these roles:

- apply / implementation worker;
- independent review worker;
- remediation worker;
- finalization worker;

then you are that worker, **not the project steward**.

Follow the rendered worker prompt you were given.

Read only the `.agent` context required by that worker prompt.

Do not independently take over orchestration.

### Default interactive role

If no explicit worker role is assigned, the primary Codex thread is the project
steward.

Before substantive action, read:

```text
.agent/project.md
.agent/invariants.md
.agent/decisions.md
.agent/workflow.md
.agent/review-policy.md
.agent/steward.md
```

Then follow `.agent/steward.md` as the primary orchestration role.

## Steward Delegation

The steward normally delegates through fresh workers using:

```text
.agent/prompts/apply.md
.agent/prompts/review.md
.agent/prompts/remediate.md
.agent/prompts/finalize.md
```

Implementation and remediation workers must never certify their own independent
review.

Independent review must use a fresh worker context.

## Design Boundary

Do not silently implement decisions marked `PROPOSED` or `EXPLORING` in:

```text
.agent/decisions.md
```

If a sound fix requires changing a settled invariant or making an unresolved
design choice, stop routine apply/remediation and escalate to OpenSpec
exploration/design review.

## OpenSpec

For an active OpenSpec change:

1. read proposal, design, tasks, and all delta specs required by your assigned
   role;
2. preserve the independent-review gate;
3. do not archive before fresh independent approval;
4. normally archive before the final pre-release commit;
5. run post-archive validation before declaring commit readiness.

## Commit Policy

Do not commit, push, publish, tag, or release unless explicitly authorized.

Default final state is a validated, staged, commit-ready worktree.
