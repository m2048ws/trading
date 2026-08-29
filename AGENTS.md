# Trading Repository Agent Instructions

This repository uses a project-steward workflow for OpenSpec-driven development.

Stable project context lives under `.agent/`.

Normative architecture and Scala/functional-design behavior is defined by the canonical OpenSpec capabilities
`repository-architecture` and `scala-functional-design` after this active charter is archived. The human guide is
`docs/design-principles.md`; `docs/architecture-charter-audit.md` distinguishes the current tree, transitional
exceptions, and proposed target.

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

The steward uses Codex-native subagents as the preferred control plane only for
roles whose required guards are satisfied. Repository helpers under
`.agent/bin/` remain the guard plane for role policy, prompt rendering, report
validation, steward-retained launch identity, writer serialization, volatile
trace output, state refresh, and review isolation. The current repository and
client boundary cannot protect the complete executable decision closure from a
workspace-writing subagent or retain writer exclusion after broker-process
death. Native primary-worktree writers are therefore mechanically ineligible;
bounded read-only native exploration remains available.

The role matrix is:

| Role | Preferred backend | Mutation class |
| --- | --- | --- |
| bounded exploration | native | read-only |
| apply | script only | one primary-worktree writer |
| independent review | script only | isolated staged-snapshot reviewer |
| remediation | script only | one primary-worktree writer |
| finalization | script only | one primary-worktree writer |

Formal workers use the canonical rendered prompts under:

```text
.agent/prompts/apply.md
.agent/prompts/review.md
.agent/prompts/remediate.md
.agent/prompts/finalize.md
```

and the shared role settings in:

```text
.agent/worker-roles.json
```

The retained native broker protocol and writer profiles are diagnostic
foundations, not launch authority. `.agent/bin/native-worker select` and broker
preparation must select the script backend for apply, remediation, and
finalization even when a capabilities file claims every historical broker
guard. Enabling native writers requires a later reviewed change that supplies
both protected immutable execution for the complete transition/release/fallback
decision closure and exclusion owned independently of broker-process lifetime.

Use the complete `.agent/bin/run-worker` path for every formal role. It remains
the deterministic writer fallback and the detached staged-snapshot review
backend.

Implementation and remediation workers must never certify their own independent
review.

Independent review must use a fresh worker context through the detached
staged-snapshot script backend.

At most one apply, remediation, or finalization worker may be active against the
primary worktree. Parallel native delegation is limited to bounded,
non-mutating investigations.

## Design Boundary

Do not silently implement decisions marked `PROPOSED` or `EXPLORING` in:

```text
.agent/decisions.md
```

If a sound fix requires changing a settled invariant or making an unresolved
design choice, stop routine apply/remediation and escalate to OpenSpec
exploration/design review.

## Architecture and Functional Design

For every nontrivial proposal or implementation:

- assign each concept and error one primary owner and preserve acyclic, one-way dependencies;
- look for honest sums, products, refinements, non-empty structures, lawful combinations, traversals, and pure state
  transitions before using flags, primitives, mutation, or ad hoc control flow;
- preserve dimension, grid, identity, provenance, validation, and endpoint information in types across trusted
  transitions;
- accumulate independent validation failures deterministically and sequence dependent checks from prior evidence;
- represent expected absence, invalidity, conflict, and failure in public mathematical/domain result types; do not use
  `null`, unchecked extraction, sentinel values, or ordinary exceptions as control flow, and quarantine unavoidable
  partiality or unsafe mechanisms behind stated checked invariants without exposing construction authority;
- keep pure mathematical/domain/economic code free of live catalogs, codecs, concrete effects, and runtime state;
- use effect-polymorphic application ports only for genuine external variation and confine concrete effects,
  concurrency, resources, streams, clients, and telemetry to runtime interpreters;
- admit mature dependencies for a named mechanism in the narrowest owning layer, keep independently released version
  coordinates separate, and preserve the JDK 17 minimum unless an explicit compatibility change says otherwise;
- require advanced Scala and functional abstractions to protect semantics while keeping common public calls
  domain-readable;
- verify laws, downstream compiler boundaries, interpreter contracts, concurrency, complexity, and hot-path
  performance in proportion to the claim.

Logical responsibility boundaries precede physical modules. Do not create an empty target module or present a proposed
module/API as implemented. A required charter exception is a design change: stop apply/remediation and escalate rather
than introducing it as incidental cleanup.

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

## Session Memory Protocol

### Startup (every session)
Read in order, max 3 files:
1. `memory/session-bridge.md` — durable delivery checkpoint
2. `memory/MEMORY.md` — permanent source-backed constraints
3. `wiki/hot.md` — current project context (~500 words, hard cap 600)
Then read the active RFC, Slice, `source.yaml`, `traceability.yaml`, and Change artifacts named by the bridge. Read `wiki/index.md` only on demand.

### Retrieval Budget
- Startup: max 3 files (session-bridge + MEMORY + hot), then active delivery artifacts
- Per-question: max 2 wiki pages before answering
- If >5 pages needed: say "this needs a deep session"

### File Size Limits (hard caps)
| File | Target | Hard Cap | Overflow Action |
|------|--------|----------|-----------------|
| wiki/hot.md | 500 words | 600 words | Trim oldest entries |
| wiki/index.md | 40 lines | 80 lines | Archive completed entries |
| memory/pitfalls.md | 10 active | 20 active | Rotate oldest 10 |
| memory/session-bridge.md | 30 lines | 50 lines | Archive old Done items |

### Durable Checkpoints
`memory/session-bridge.md` is not a live state machine. Apply updates it only with a planning-baseline commit and immediately before each Task Group commit. `corgispec archive --local` alone writes the archive closeout checkpoint; skills must not repeat that write after the closeout commit is sealed. SessionStart/PostCompact hooks synthesize the live phase and report drift from `.corgi/loop`.

### Knowledge Promotion
- During Apply, keep discoveries in the bridge Promotion Queue; do not promote unverified claims.
- At Archive, `corgispec archive --local` creates `wiki/deliveries/<RFC-ID>-<Slice-ID>.md` and is the sole writer of archive-derived hot, architecture, patterns, pitfalls, MEMORY, and bridge provenance. Skills only prepare or verify this work read-only.
- Do not create new `wiki/sessions/` pages or append to `wiki/log.md`; migrated legacy data stays read-only in place.

### Compaction Triggers (agent self-maintains)
- Every archive: clear the completed delivery pointer and compact the bridge
- pitfalls > 20 entries: rotate oldest 10 to Archive section
- hot.md > 550 words: trim oldest entries
- Every 10 Corgi sessions: suggest running `/corgi-lint`; lint is read-only unless `--report` is explicit
