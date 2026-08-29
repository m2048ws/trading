## 1. Rebase the Historical Workflow Implementation

- [x] 1.1 Record the current HEAD, Git/OpenSpec state, and current architecture/functional-design instructions before
  transplanting any historical workflow hunk.
- [x] 1.2 Transplant the staged workflow implementation from `/Users/m/src/money4` or `/tmp/patch` as merge hunks while
  excluding this change's rebased planning artifacts and preserving every newer current-repository section.
- [x] 1.3 Treat all historical task marks, test results, apply/remediation reports, and four review reports as diagnostic
  inputs only; do not reuse them as current completion or approval evidence.
- [x] 1.4 Verify the resulting implementation scope contains only agent instructions, workflow helpers/tests,
  project-scoped agent configuration, documentation, and this active OpenSpec change, with no Scala or SBT edits.

## 2. Shared Role Policy and Backend Guards

- [x] 2.1 Make the shared role policy and executable selector refuse every native primary-worktree writer until protected
  immutable execution covers the complete decision closure and writer exclusion survives broker-process death.
- [x] 2.2 Make native preparation and the script launcher consume the shared policy rather than maintaining separate
  role mappings.
- [x] 2.3 Preserve one canonical prompt-rendering path and bind formal context `CHANGE_NAME` to the change assigned by
  both native and script launches.
- [x] 2.4 Apply the role's JSON schema, workflow-consistency rules, and explicit assigned-change binding through one
  backend-neutral report-validation entry point.
- [x] 2.5 Keep the active primary-worktree writer lease in the script parent, never inherit it into the delegated worker,
  and deny native preparation rather than relying on the broker-owned descriptor.
- [x] 2.6 Preserve content-sensitive launch identity for tracked-unstaged and non-ignored-untracked path, type, mode, and
  content, and resolve every relative Git index/object path against the target repository rather than the caller cwd.

## 3. Retained Native Broker Protocol

- [x] 3.1 Retain the ephemeral broker, private-channel, generation, and one-shot protocol as nonauthorizing diagnostic
  foundation code while production role selection keeps every native writer ineligible.
- [x] 3.2 Keep the dormant protocol's launch tuple, lease evidence, identifiers, and digests nonauthorizing and unavailable
  to any production workspace-writing subagent.
- [x] 3.3 Retain internal diagnostic coverage for collection, failure classification, exact generation, and one-shot
  consumption without presenting that live-helper closure as production transition authority.
- [x] 3.4 Preserve the complete parent-owned standalone script exclusion path and verify that unrelated broker death does
  not release its writer reservation.
- [x] 3.5 Retain fail-closed protocol diagnostics for lease/capability/generation/replay cases while production native
  writer preparation is denied before recovery authority could be required.

## 4. Context-Efficient Native Handoffs

- [x] 4.1 Render the complete canonical native worker prompt into a unique ignored shared file and return its path from
  preparation.
- [x] 4.2 Update steward instructions so a fresh native worker receives only a short assignment to read and execute the
  prepared prompt file, never a copy of the complete rendered prompt in the primary conversation.
- [x] 4.3 Keep the assigned structured report in a unique ignored file and preserve report correction by the same fresh
  worker without granting prose summaries transition authority.
- [x] 4.4 Persist the complete validated report, refreshed steward state, broker generation outcome, and detailed
  collection diagnostics in ignored artifacts without persisting the one-shot capability.
- [x] 4.5 Make broker-mediated collection stdout a compact transition-oriented result containing only
  integrity/validation outcome, role/change/launch/generation identity, concise repository/OpenSpec status, and
  paths/digests for detailed evidence.
- [x] 4.6 Remeasure rendered-prompt/context, spawn-assignment, report, broker preparation, full-state, and compact-result
  sizes and elapsed observations; keep context savings separate from worker model usage and validation time.

## 5. Native Control Plane and Script Fallback

- [x] 5.1 Keep bounded exploration in a read-only native profile and make the retained apply, remediation, and
  finalization profiles read-only dormant fallbacks that route formal work to the script backend.
- [x] 5.2 Make native writer eligibility require an isolated broker channel, protected state, protected immutable
  executable closure, and crash-surviving exclusion; select deterministic script fallback because the latter two guards
  are unavailable.
- [x] 5.3 Retain fresh native prepare, wait/steer/interrupt, broker-mediated collect, classify-failure, and recovery as a
  documented future protocol that current production selection cannot launch for a writer.
- [x] 5.4 Keep bounded parallel native work non-mutating and prevent it from authorizing formal workflow transitions.
- [x] 5.5 Keep `run-worker` fully supported for every formal role with shared role settings, prompts, report validation,
  parent-owned writer serialization, trace output, and commit-authorization behavior.
- [x] 5.6 Prevent possible native-writer mutation by denying launch; retain refreshed-state classification as a future
  fail-closed protocol rather than current fallback authority.

## 6. Independent Review and Workflow Documentation

- [x] 6.1 Preserve formal independent review on the detached staged-snapshot script backend.
- [x] 6.2 Reject review approval after tracked, staged, non-ignored untracked, committed-HEAD, or committed-tree mutation
  of the isolated review snapshot.
- [x] 6.3 Preserve primary staged-tree identity comparison before and after review so any concurrent index change makes
  approval stale.
- [x] 6.4 Update `AGENTS.md`, `.agent/workflow.md`, and `.agent/steward.md` with current native-writer ineligibility,
  script-only formal role routing, retained future broker protocol, and bounded native exploration.
- [x] 6.5 Reconcile the ACTIVE native-orchestration decision with script-backed formal workers and nonauthorizing broker
  diagnostics without treating it as settled before fresh approval and archive.
- [x] 6.6 Update the measurement document for broker-mediated handoffs while preserving the limits of elapsed-time and
  total-token conclusions.

## 7. Focused Workflow Regression Coverage

- [x] 7.1 Test shared role-policy equivalence, unconditional native-writer ineligibility, bounded exploration eligibility,
  profile configuration, prompt rendering, and deterministic script fallback.
- [x] 7.2 Test valid reports plus missing, malformed, schema-invalid, summary-only, contradictory, and cross-change or
  identity-injection reports for every formal backend path; roles whose schema intentionally omits a repeated change
  field must reject the attempted injection at the schema boundary.
- [x] 7.3 Retain internal broker-protocol diagnostics for lease retention, corrected collection, classification,
  self-release rejection, generation handling, and handoff integrity without exposing the test-only protocol override in
  production.
- [x] 7.4 Test tracked and untracked dirty-start fingerprints, same-path content/mode/type mutation, unchanged dirty state,
  ignored-output exclusion, and cross-cwd calls that must use only the target repository's HEAD, index, and objects.
- [x] 7.5 Test manifest tampering for role, change, report path, schema path, initial repository identity, launch identity,
  generation, and lease tuple; every case must deny transition and safe fallback.
- [x] 7.6 Test that claimed capabilities and live-helper tampering cannot enable a production native writer, that broker
  death cannot release a script parent's reservation, and that the dormant protocol retains its earlier capability,
  tamper, replay, and atomic-consumption diagnostics.
- [x] 7.7 Test clean detached review plus tracked, staged, non-ignored untracked, ignored-output, committed-HEAD/tree, and
  primary staged-tree freshness cases.
- [x] 7.8 Test that native spawn instructions reference rather than inline the rendered prompt and that compact
  broker-mediated collection omits complete report/state payloads and all one-shot capabilities while preserving
  inspectable detailed artifacts.
- [x] 7.9 Reproduce F1/F2 against the blocked tree, then verify production writer preparation remains denied after
  live-helper tampering and script writer exclusion remains held after broker-process death.

## 8. Proportional Validation and Independent Approval

- [x] 8.1 Run syntax checks for every changed shell and Python helper.
- [x] 8.2 Run every focused executable under `.agent/tests` and resolve any nondeterministic or environment-dependent
  result without invoking unrelated Scala/SBT validation.
- [x] 8.3 Run strict validation for `adopt-native-subagent-orchestration` and `openspec validate --all --strict`.
- [x] 8.4 Smoke-test all project-scoped profiles, public writer selection denial, and the internal ignored-artifact broker
  fixture; verify no capability or generated artifact enters the staged tree.
- [x] 8.5 Verify the staged path set contains no Scala source or SBT/build-definition change; while that remains true,
  do not run `sbt test`, Scala formatting, Scaladoc, or JMH during apply, remediation, review, or finalization.
- [x] 8.6 Run cached and unstaged Git diff checks, inspect untracked state, and stage exactly the intended workflow and
  active-change artifacts without committing.
- [x] 8.7 Obtain fresh independent approval of the exact staged tree through the pre-existing detached-snapshot script
  backend; any remediation must return to another fresh independent review.
