## Why

The steward currently launches formal workers through nested `codex exec` processes. That preserves important workflow
guards, but it gives up Codex-native lifecycle visibility and steering while repeatedly moving rendered prompts, worker
logs, reports, and full repository-state payloads through the primary thread. With seven architecture proposals still
to implement, adopting a guarded native control plane now can reduce orchestration latency and context pollution across
the remaining portfolio.

## What Changes

- Prefer Codex-native subagents for bounded read-only exploration while retaining the complete script backend for every
  formal role. Keep apply, remediation, and finalization mechanically native-ineligible until the client/runtime can
  protect the complete executable decision closure and retain writer exclusion across broker-process death.
- Keep formal independent review on the existing detached staged-snapshot script backend until native execution can
  provide equivalent worktree binding and cleanliness guarantees.
- Preserve one canonical family of role prompts, report schemas, model/effort settings, logical validation, state
  refresh, writer serialization, review freshness, archive gates, and explicit commit authorization across backends.
- Retain ignored prompt/report handoffs in the dormant broker protocol so their payload and integrity behavior remains
  measurable without granting current native-writer authority.
- Retain the one-shot broker protocol and native-writer profiles as diagnostic foundations only; worker-readable
  manifests and digests remain non-authorizing, and current policy refuses every primary-worktree native launch rather
  than claiming authority that this repository/runtime cannot isolate or keep alive across broker death.
- Retain compact collection and complete ignored diagnostic artifacts in the internal broker fixture; production formal
  transitions use the script report path.
- Add deterministic workflow regressions for backend selection, native-writer denial despite claimed capabilities or
  live-helper tampering, crash-independent script writer exclusion, handoff integrity, report validation, dormant broker
  protocol behavior, review isolation, and preservation of all existing approval gates.
- Validate the workflow and configuration surfaces directly; do not run the Scala/SBT test, formatting, Scaladoc, or
  benchmark suites unless implementation unexpectedly changes a Scala or build surface that those suites can exercise.
- Measure representative dormant-protocol payload sizes and elapsed time without enabling native writers for later
  portfolio changes; do not claim lower total model-token consumption merely from replacing one worker transport with
  another.

## Capabilities

### New Capabilities

- `steward-worker-orchestration`: Select and operate native or script-backed OpenSpec workers while preserving role
  independence, trusted launch authority, mechanical guards, compact handoffs, structured reports, and deterministic
  fallback behavior.

### Modified Capabilities

None.

## Impact

The change affects repository workflow configuration and automation under `AGENTS.md`, `.agent/`, and `.codex/`, plus
their workflow regression tests and documentation. It introduces no Scala production API, published artifact, quantity
semantics, or domain-capability change. Because it governs how later production changes are implemented and approved,
the change itself remains subject to the pre-existing script-backed independent-review and finalization workflow.
