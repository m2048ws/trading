## Context

See `proposal.md` for motivation and `specs/steward-worker-orchestration/spec.md` for the behavioral contract.

The current script path is intentionally strong: `run-worker` renders a version-controlled role prompt, invokes an
ephemeral Codex process with a role schema, applies logical report validation, and gives independent review a detached
worktree built from the staged Git tree. Native Codex subagents add first-class threads, visibility, follow-up steering,
interruption, and bounded contexts, but native agents in one interactive repository session share the primary worktree
and do not themselves provide detached staged-snapshot review or schema-bound final responses.

An earlier staged implementation in `/Users/m/src/money4` supplies mature design and regression material. Four fresh
reviews found defects in rejected-report lease retention, dirty-path fingerprinting, assigned-change binding, committed
review mutation, and finally worker-writable launch authority. The current proposal treats those reports as design input,
not as current validation or approval. It also corrects two efficiency problems identified against the present steward:
the earlier design routed the complete rendered prompt through the primary conversation and returned the complete report
plus a roughly 55 KB repository-state payload from every collection.

The current repository has advanced several generations beyond the source repository. The historical staged diff still
applies cleanly, but implementation must transplant it as a patch and preserve newer architecture/functional-design
instructions rather than copying old files wholesale.

## Goals / Non-Goals

**Goals:**

- Separate native agent lifecycle management from mechanical repository workflow enforcement.
- Prefer native threads for eligible roles without weakening existing workflow invariants.
- Keep bulk prompts, reports, and refreshed-state payloads out of the primary conversational context.
- Keep launch and fallback authority outside worker-writable handoffs.
- Preserve a deterministic, independently usable script backend.
- Make backend selection, writer ownership, handoff integrity, and transition authority explicit and testable.
- Validate only the workflow surface changed by this proposal.

**Non-Goals:**

- Running multiple primary-worktree writers concurrently.
- Making formal independent review native before equivalent staged-snapshot isolation exists.
- Replacing OpenSpec workflow states, report schemas, or the independent-review authority.
- Eliminating the physical model/tool work performed by delegated agents.
- Claiming lower total token consumption without representative measurements.
- Changing Scala sources, SBT configuration, published artifacts, or domain semantics.
- Removing `run-worker`.

## Decisions

### Decision 1: Use native exploration plus a script formal-worker control plane

The interactive steward selects and manages bounded read-only exploration through native subagent lifecycle controls.
Every formal role uses the script backend in the current implementation. An ephemeral authority-broker protocol is
retained as a diagnostic foundation, but it is not current transition or writer-release authority: fresh review proved
that its live executable decision closure remains worker-writable and its root-inode reservation ends on broker-process
death. Versioned repository helpers remain responsible for role policy, prompt rendering, report validation,
working-tree identity, review isolation, failure classification, and trace persistence.

```text
interactive steward
    |
    +-- bounded read-only exploration --> fresh native thread
    |
    +-- every formal role -------------> run-worker

both paths
    -> validate role result
    -> refresh repository/OpenSpec state
    -> classify the next transition
```

Native exploration output is informative only and cannot authorize a workflow transition. Formal transitions use the
validated script report and a steward refresh of load-bearing state.

Alternative considered: replace all scripts with native delegation. Rejected because native lifecycle controls do not
mechanically reproduce detached staged-snapshot review, structured report enforcement, or writer leases.

Alternative considered: retain only nested `codex exec`. Rejected because it keeps worker lifecycle opaque and carries
avoidable logs and handoffs through the main thread.

### Decision 2: Centralize role policy and select a backend per launch

One machine-readable role policy records each role's model, reasoning effort, mutation class, prompt, report schema,
native eligibility, and required guards. Native preparation and `run-worker` consume the same policy.

Initial policy:

| Role | Preferred backend | Boundary |
| --- | --- | --- |
| bounded exploration | native | non-mutating assignment; read-only profile when selectable |
| apply | script | fresh serialized primary-worktree writer |
| independent review | script | detached staged snapshot and cleanliness enforcement |
| remediation | script | fresh serialized primary-worktree writer |
| finalization | script | fresh serialized writer plus approval/archive/commit gates |

Native availability alone is insufficient. Selection requires every role guard. Current native-writer ineligibility is
unconditional because this repository/runtime lacks two guards that a capability file cannot self-assert: protected
immutable execution for the complete transitive decision closure and a writer reservation owned independently of
broker-process lifetime. The production selector closes native eligibility over an immutable in-process role-name
allowlist containing only exploration, and broker preparation independently denies every formal role name before
consulting mutable classification or creating handoff artifacts. The shared policy provides the corresponding normal
classification, but changing that workspace-controlled policy or helper cannot expand the loaded broker's production
scope.

Alternative considered: a repository-wide native mode. Rejected because eligibility differs by role and client.

### Decision 3: Keep canonical prompts and schemas, but pass prompt references

The existing `.agent/prompts/` and `.agent/schemas/` files remain the semantic source of formal roles. Preparation
renders the canonical prompt plus context into a unique ignored file. The steward spawns a fresh native worker with a
short message instructing it to read and execute that exact file; the steward does not read and resend the rendered
payload through the primary conversation.

Project-scoped custom-agent profiles retain role-specific model/effort settings. Exploration is read-only and usable;
apply, remediation, and finalization profiles are also read-only but dormant, instructing the client to route formal
work through the script backend. A later writer-enabling change would have to restore workspace-write only after every
authority guard is implemented and reviewed.

Initial formal settings remain aligned with the existing launcher:

- apply: `gpt-5.6-sol`, high;
- review: `gpt-5.6-sol`, max;
- remediation: `gpt-5.6-sol`, xhigh;
- finalization: `gpt-5.6-sol`, medium.

Alternative considered: embed the complete role in custom-agent files. Rejected because native and script semantics
would then have separate sources.

Alternative considered: send the rendered prompt text in the spawn call. Rejected because it adds thousands of repeated
tokens to the steward transcript without improving worker authority.

### Decision 4: Use assigned ignored files for worker-owned handoffs

Preparation allocates unique ignored paths for the rendered prompt, structured report, detailed collection result, and
launch diagnostics. The worker reads the prompt and writes only its assigned report. Its human-readable final response
may summarize progress but cannot replace the report.

Collection applies the existing JSON schema, workflow-consistency rules, and assigned-change binding. Invalid or missing
reports grant no authority. A rejected writer retains its lease until the same fresh worker corrects the assigned report
or failure classification refreshes and classifies repository state.

Alternative considered: parse the worker's final message. Rejected because prose is neither schema-bound nor a stable
machine transition boundary.

### Decision 5: Keep writer authority in a steward-owned one-shot broker

This section remains the required future protocol, not a claim of current native-writer eligibility. The current
implementation cannot protect the complete transitive executable closure used by collection/classification/recovery and
cannot retain the broker-owned lock after hard broker death. Consequently broker preparation refuses apply,
remediation, and finalization before any workspace-writing worker is spawned. A later change must close both gaps before
enabling this protocol.

Worker-writable manifests cannot be trusted as the source of role, change, report path, schema, initial repository
state, lease identity, release authority, or fallback authority. Before a native writer starts, the steward opens an
ephemeral broker through a client-owned control channel that is not inherited by or addressable from the delegated
worker. The broker retains the complete canonical launch tuple, owns the active writer lease, assigns a monotonically
increasing launch generation, and creates a cryptographically random one-shot capability. Neither the capability nor a
command that can exercise it is written to the workspace, worker prompt, manifest, trace, environment, or process
arguments visible to the worker.

The broker-retained tuple includes at least:

- role and mutation class;
- assigned OpenSpec change;
- launch identifier and monotonic generation;
- canonical prompt, report, schema, and detailed-result paths;
- HEAD, staged-tree identity, and content-sensitive working-tree fingerprint;
- the complete expected cross-backend writer lease;
- the one-shot capability state and whether it has been consumed.

The worker-readable manifest may repeat non-authorizing identifiers and digests so later validation can detect handoff
tampering, but those values cannot invoke the broker or replace its retained tuple. After the worker finishes or is
interrupted, the steward requests collection or failure classification over the private channel. The broker verifies
the exact active generation, manifest and prompt integrity, report destination and schema, complete writer lease, and
refreshed repository identity. It then atomically consumes the one-shot capability before releasing the writer or
returning transition or fallback authority. A second request for the same generation, a replayed lease, or any request
outside that channel is rejected.

Missing broker state, control-channel loss, capability mismatch, generation mismatch, or missing/altered manifest or
lease state fails closed. It cannot release the writer, admit another writer, authorize a transition, or classify blind
fallback as safe. Recovery is steward-controlled: refresh actual Git/OpenSpec state, preserve diagnostics, and either
reconnect to the still-authoritative broker or explicitly abandon the native launch through a protected recovery path.
The delegated worker cannot invoke recovery.

Any future native writer path is eligible only when the client can provide this non-worker-accessible control channel,
protected live state, a protected immutable executable decision closure, and writer exclusion that survives broker
death. Current script execution retains a parent-owned guard whose release capability is never inherited by its nested
worker.

Alternative considered: retain a SHA-256 identity while placing the authentic tuple and digest in worker-readable
files. Rejected because authenticity does not prove steward invocation; the worker can use the authentic values to
release and later replay its lease.

Alternative considered: give the steward a secret token without protected monotonic state. Rejected because a token
alone does not record consumption or prevent stale-generation replay and cannot safely coordinate another backend.

Alternative considered: store authority in another workspace file or volatile trace. Rejected because the worker can
modify those artifacts under the shared workspace permission model.

Alternative considered: trust a cooperative worker not to call guarded helpers. Rejected because the active contract
requires mechanical enforcement even when a worker acts unexpectedly.

Alternative considered: introduce a repository-global long-lived signing service. Rejected because an ephemeral
steward-owned broker supplies the required isolation without adding a persistent service or durable secret store.

### Decision 6: Serialize every script-backed primary-worktree writer

Exactly one script-backed apply, remediation, or finalization worker may hold the primary-worktree writer lease. The
standalone script parent owns the descriptor and never inherits it into the delegated worker. Because native writers
cannot start, broker-process death cannot release an active formal writer. The steward waits, validates the report, and
refreshes state before launching another writer.

Native parallelism is limited to bounded non-mutating investigations. A read-heavy agent that discovers needed edits
returns that conclusion for normal routing rather than changing roles.

Alternative considered: one worktree per writer followed by merges. Rejected because the portfolio operates on one
staged implementation state and merging would introduce conflict and stale-approval semantics outside this proposal.

### Decision 7: Preserve script-backed detached independent review

Formal review continues through `run-worker review`. It constructs a commit from the staged tree, checks out a detached
worktree, permits ignored build outputs required by the relevant review, and rejects tracked, staged, non-ignored
untracked, detached-HEAD, or detached-tree mutation. The steward compares the primary staged-tree identity before and
after review so a ready verdict becomes stale if the index changes concurrently.

Alternative considered: native review in the primary worktree. Rejected because read-only instruction alone does not
bind the reviewer to the exact staged snapshot and can prevent legitimate ignored test output.

### Decision 8: Return compact collection results and retain full evidence by path

Collection persists the complete validated report, refreshed `steward-state`, and diagnostics in ignored artifacts. Its
stdout contains only transition authority, role/change/launch identity, report status, HEAD and staged-tree identity,
concise worktree/OpenSpec status, integrity result, and paths/digests for the full evidence.

The steward can inspect a precise field or artifact when needed. It does not ingest the complete report plus complete
repository portfolio on every successful collection.

Alternative considered: retain the historical full JSON result. Rejected because current `steward-state` output is
roughly 55 KB and recreates the primary-context pollution the native path is intended to reduce.

### Decision 9: Keep script fallback complete and classify native failure before switching

`run-worker` remains supported and regression-tested for all formal roles. Script fallback is automatically safe only
when the trusted launch identity and refreshed identity are equal. Repository identity includes a content-sensitive
fingerprint of tracked-unstaged and non-ignored-untracked path, type, mode, and content so changes to already-dirty paths
cannot masquerade as unchanged state. The identity helper resolves relative Git index and object paths against the
target repository root, so its HEAD, staged tree, and dirty fingerprint cannot be mixed with a caller's current
directory.

No native writer may change repository state under current policy. The retained classification protocol remains a
future fail-closed contract and its internal regressions remain diagnostic; it grants no current transition or fallback
authority.

### Decision 10: Validate the changed workflow surface, not the Scala product

This change is complete when helper syntax, all `.agent/tests`, agent-profile configuration, strict active/all OpenSpec
validation, ignored-artifact behavior, Git diff checks, and isolated orchestration probes pass. Apply, review,
remediation, and post-archive validation do not invoke `sbt test`, Scala formatting, Scaladoc, or JMH while the change
remains limited to workflow/configuration/OpenSpec files.

If implementation unexpectedly touches Scala or SBT configuration, that is a scope expansion. The steward must first
classify it and then add tests proportional to that new surface; the no-SBT rule does not excuse testing actual build or
Scala changes.

Alternative considered: run the repository's generic full clean suite as ceremony. Rejected because it cannot exercise
these orchestration scripts and previously consumed substantial time while producing unrelated compiler failures.

### Decision 11: Measure context-path efficiency separately from worker cost

The implementation records representative sizes for rendered prompt/context, native spawn assignment, detailed report,
full refreshed state, and compact collection output. It also records directional elapsed-time observations for native
and script orchestration where practical.

The result distinguishes:

- primary-thread context volume;
- worker model/tool work;
- local orchestration overhead;
- validation/build time.

Native adoption may substantially reduce the first and modestly reduce the third while leaving worker work and
validation time largely unchanged. No total-token or monetary-cost claim is accepted without corresponding evidence.

## Risks / Trade-offs

- [Native behavior and profile selection vary across clients] -> Check eligibility per launch and retain complete script
  fallback.
- [A worker tampers with ignored launch or lease files] -> Treat them as non-authorizing evidence and require the
  steward-private broker generation and one-shot capability before release, transition, or fallback.
- [The authority broker, executable closure, or private control channel is lost] -> Native writers remain ineligible;
  formal work uses the independently owned script reservation.
- [A stale launch is replayed after another writer] -> Consume capabilities atomically and reject every request whose
  monotonic generation is not the one active in the broker.
- [A factually false but schema-valid report passes] -> Treat reports as claims, refresh repository state, and preserve
  fresh independent review.
- [Two writers conflict] -> Enforce one parent-owned script primary-worktree lease; native writer preparation is denied.
- [Prompt or role policy drifts across backends] -> Render one canonical prompt family and test role-policy equivalence.
- [Compact output hides needed evidence] -> Persist full ignored artifacts and return their paths/digests for targeted
  inspection.
- [Native delegation uses more model tokens] -> Measure context reduction separately and avoid an unsupported total-cost
  claim.
- [Hybrid orchestration adds local code] -> Centralize backend policy and guards, and cover them with deterministic
  focused tests.

## Migration Plan

1. Transplant the historical staged diff as hunks, preserving all newer current-repository instructions and artifacts.
2. Reopen all implementation and validation tasks against the current baseline; historical checked boxes and reports
   are evidence for test design only.
3. Retain the ephemeral broker protocol, monotonic launch generation, and one-shot capability as diagnostic foundations,
   but deny native primary-worktree preparation until its full closure and crash lifetime are protected.
4. Add trusted launch-identity verification and regressions for manifest, role, change, schema, report, snapshot, lease
   tampering, worker self-release, intervening-writer admission, stale-generation replay, broker loss, and caller-cwd
   repository identity.
5. Change native spawning to a short prompt-file assignment and collection to compact stdout plus detailed ignored
   artifacts.
6. Run focused workflow validation only; do not run unrelated SBT/Scala suites while scope remains no-code.
7. Exercise this enabling change through the pre-existing script apply/review/remediation/finalization workflow.
8. After fresh independent approval, archive and create the separately authorized unsigned commit.
9. Keep Proposal 3 and later formal workers script-backed; use native subagents only for bounded read-only exploration.

Rollback consists of disabling native preference, terminating any broker only through protected steward recovery, and
using the retained `run-worker` path for every formal role. No production-code or data migration is involved.
