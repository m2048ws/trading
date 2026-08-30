# Trading Repository Agent Instructions

This repository uses CorgiSpec v4 with RFC-first Run Contract v3 delivery. Do not substitute the retired project-steward, `run-worker`, or OpenSpec worker-role workflow.

## Startup and authority

Start with `memory/session-bridge.md`, then `memory/MEMORY.md` and `wiki/hot.md`. Read additional Wiki pages only when the task needs them. Refresh volatile Git, source, build, Corgi, and tracker state from the repository and provider rather than trusting an earlier transcript.

## Session Memory Protocol

At session startup, read `memory/session-bridge.md`, then `memory/MEMORY.md`, then `wiki/hot.md`. Read `wiki/index.md` only on demand. Treat `.corgi/loop` as lifecycle authority and the bridge as a durable checkpoint, not live state. During Apply, queue discoveries for later promotion; only `corgispec archive --local` writes archive-derived delivery knowledge.

The authoritative delivery surfaces are:

- accepted RFCs and their delivery sidecars under `rfcs/`;
- Corgi planning packages in the CLI-reported change root;
- Corgi Run Contract state, accessed only through the CLI;
- the single tracker Issue bound by `corgi/source.yaml`;
- `tools/corgi/pilot.json` and the guarded adapter documented in `tools/corgi/README.md`.

When worktree isolation is enabled, discover and work inside the registered delivery worktree. Do not infer an active change from the primary checkout.

## Workflow selection

Use the applicable `/corgi-*` command or `corgispec-*` skill for RFC, Propose, Apply, Verify, Human Review, Human QA, and Archive. If a requested Corgi command or skill is unavailable, report that gap; never fall back to a different orchestration framework.

For Apply:

1. require a strict-ready finalized proposal and closed native `blockedBy` dependencies;
2. claim through `.agent/bin/corgi-pr claim` so the adapter checks admission and creates the local CAS handoff;
3. implement exactly one current Task Group at a time;
4. run checks and the automated Task Group review loop before its dedicated commit;
5. acknowledge that exact commit with the Corgi CLI and retained four-field token;
6. after the first acknowledged Task Group, use `.agent/bin/corgi-pr open`, then `sync` for later groups;
7. stop Apply at `awaiting_verify`.

Never hand-edit `.corgi/loop/**`, canonical evidence, Issue dashboards, or planning task checkboxes. Run Contract state is execution authority. Corgi Verify, explicit human whole-change Review, Human QA when applicable, and Archive are separate gates.

## GitHub and Git authority

The checked-in pilot may permit local Corgi commits, fast-forward WIP pushes, and draft PR creation or synchronization. It does not imply authority to mark ready, assign reviewers, merge, delete remote branches, tag, publish, or release. Re-read `tools/corgi/pilot.json` before acting.

Do not commit, push, create a PR, or mutate an Issue unless the user has authorized that action or the active Corgi command and pilot authority explicitly own it. Never force-push. Use one branch, one worktree, one Issue, and one draft PR per admitted change.

## Architecture and functional design

For every nontrivial proposal or implementation:

- assign each concept and error one primary owner and preserve acyclic, one-way dependencies;
- look for honest sums, products, refinements, non-empty structures, lawful combinations, traversals, and pure state transitions before using flags, primitives, mutation, or ad hoc control flow;
- preserve dimension, grid, identity, provenance, validation, and endpoint information in types across trusted transitions;
- accumulate independent validation failures deterministically and sequence dependent checks from prior evidence;
- represent expected absence, invalidity, conflict, and failure in public mathematical/domain result types; quarantine unavoidable partiality behind checked invariants;
- keep pure mathematical, domain, and economic code free of live catalogs, codecs, concrete effects, and runtime state;
- use effect-polymorphic application ports only for genuine external variation and confine concrete effects, concurrency, resources, streams, clients, and telemetry to runtime interpreters;
- admit dependencies for a named mechanism in the narrowest owning layer, keep independently released version coordinates separate, and preserve the JDK 25 minimum unless an explicit compatibility change says otherwise;
- require advanced Scala and functional abstractions to protect semantics while keeping common public calls domain-readable;
- verify laws, downstream compiler boundaries, interpreter contracts, concurrency, complexity, and hot-path performance in proportion to the claim.

Logical responsibility boundaries precede physical modules. Do not create an empty target module or present a proposed module/API as implemented. Normative architecture and Scala design behavior lives in `openspec/specs/repository-architecture/`, `openspec/specs/scala-functional-design/`, and `docs/design-principles.md`; current delivery boundaries live in accepted RFCs.

## Worktree safety

Preserve unrelated user changes. Use `rg` for discovery and `apply_patch` for focused edits. Avoid destructive Git operations. Do not archive, remove a worktree, delete a branch, or discard recovery state until its Corgi lifecycle and tracker state make that action safe.
