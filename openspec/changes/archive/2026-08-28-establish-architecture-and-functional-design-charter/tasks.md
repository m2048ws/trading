## 1. Proposal Portfolio Coherence Gate

- [x] 1.1 Verify that the nine dependent architectural proposals named in `design.md` exist with complete proposal, delta-specification, design, and task artifacts; stop the apply phase if any required proposal remains absent or materially unresolved.
- [x] 1.2 Perform and record a cross-proposal ownership audit proving every moved or introduced concept has one primary owner and the combined target dependency graph is acyclic.
- [x] 1.3 Perform and record a cross-proposal boundary audit proving names, trusted-value transitions, catalog responsibilities, validation stages, effect placement, codec ownership, and error ownership agree between adjacent proposals.
- [x] 1.4 Record the accepted implementation order and pre-release compatibility policy, including any required intermediate states, and resolve discovered conflicts in the proposal artifacts before changing project guidance.

## 2. Human-Facing Design Charter

- [x] 2.1 Create the detailed project design-principles guide covering responsibility ownership, algebra-first modeling,
  semantic type preservation, layer-specific functional programming, dependency admission/containment, validation,
  totality, advanced Scala, effect boundaries, testing, performance, and the review checklist.
- [x] 2.2 Update the root project documentation to describe the current modules accurately, link the charter, distinguish current implementation from target architecture, and avoid implying that proposed modules already exist.
- [x] 2.3 Update each current production module guide, or add a concise guide where missing, with its owned responsibilities plus allowed and forbidden dependency directions.
- [x] 2.4 Add a concise normative charter summary and source links to `AGENTS.md`, preserving the steward workflow while making the architecture and Scala/functional-design expectations visible to every role.
- [x] 2.5 Document known transitional exceptions in the current tree and map each exception to the dependent proposal that owns its eventual migration rather than claiming immediate conformance.

## 3. Stable Steward Context and Workflow

- [x] 3.1 Update `.agent/project.md` with the current module baseline, target responsibility graph, layer-specific
  functional profile, dependency-admission model, JDK 17 build/runtime baseline, trust-boundary model, and logical-
  before-physical module rule.
- [x] 3.2 Add stable `.agent/invariants.md` entries for cohesive dependency direction, mature-mechanism reuse with
  layer containment, independent release coordinates, algebra-first design, semantic type preservation, evidence-
  producing validation, pure/effect separation, runtime containment, hot-path separation, and domain-readable public
  ergonomics without weakening existing quantity or authority invariants.
- [x] 3.3 Record the architectural charter in `.agent/decisions.md` as ACTIVE for this OpenSpec change, leaving promotion to SETTLED to approved finalization after archive.
- [x] 3.4 Extend `.agent/review-policy.md` with the charter's architecture and dependency-admission questions plus the
  claim-proportional verification matrix, including law, downstream compiler, interpreter-contract, concurrency,
  deterministic complexity, and JMH performance evidence where applicable.
- [x] 3.5 Update the steward and worker prompt templates so apply, review, and remediation contexts identify relevant charter obligations and route any required exception to design escalation instead of incidental implementation.

## 4. Enforceable Boundary and Guidance Checks

- [x] 4.1 Audit the current SBT dependency graph against the documented present-state boundaries and verify Proposal 0 creates no speculative target modules or new production dependencies.
- [x] 4.2 Verify the detailed guide, concise agent instructions, stable invariants, decisions, review policy, module guides, and both new canonical capabilities use consistent ownership and effect-boundary terminology.
- [x] 4.3 Verify every target responsibility is clearly marked as current, transitional, or proposed and that no documentation presents an unimplemented API, interpreter, or module as available.
- [x] 4.4 Verify Proposal 0 changes only governance, specification, and documentation artifacts; move any production API, package, build-topology, or runtime implementation change into its owning dependent proposal.
- [x] 4.5 Inventory current dependency coordinates and record Proposal 1 as the owner of splitting the coincidentally
  shared Cats/Algebra version variable; verify this governance-only change itself neither edits `build.sbt` nor adds a
  production dependency.

## 5. Validation and Independent Review

- [x] 5.1 Build a requirement-to-artifact trace showing where every `repository-architecture` and `scala-functional-design` requirement is represented in the guide, stable invariants or decisions, and review workflow.
- [x] 5.2 Check all added documentation links and referenced paths, run Markdown or repository documentation checks if configured, and run `git diff --check`.
- [x] 5.3 Run strict OpenSpec validation for this change and for the complete proposal portfolio, resolving schema or semantic inconsistencies before staging.
- [x] 5.4 Run the configured Scala/SBT formatting checks and `sbt -batch clean test` to prove the governance-only change leaves the existing build and packaged downstream boundaries intact.
- [x] 5.5 Inspect scope and stage exactly the intended charter documentation, stable-context, workflow, and active-change artifacts with no unrelated source or build changes.
- [x] 5.6 Obtain a fresh independent review of the fully staged charter against both capability specs and the reconciled proposal portfolio; only approved finalization may complete this task, and any remediation SHALL return to another fresh independent review.
