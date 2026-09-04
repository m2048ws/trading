## 1. Exact attributed price economics

- [ ] 1.1 Add the instrument-economics attributed-change, endpoint, successful-result, and owning error domain types by reusing existing exact primitives; verify public construction and pattern matching compile in focused instrument-economics tests.
- [ ] 1.2 Implement the pure finite-change calculation with deterministic accumulated independent validation and dependent endpoint checks; verify focused examples cover empty flat, multi-change flat, open marked, endpoint mismatch, multiple incompatibilities, and exact-arithmetic failures.
- [ ] 1.3 Add exact properties for three-or-more-change scale-in/scale-out paths, attribution and order preservation, permutation-invariant aggregates, and linear settled cost; verify `instrumentEconomics / test` passes.

## 2. Round-trip scenario reuse

- [ ] 2.1 Adapt round-trip scenario slices into attributed changes, delegate to the shared flat-endpoint calculation, map failures compatibly, and remove the redundant scenario price fold; verify `executionScenario / compile` succeeds with the dependency remaining one-way toward instrument economics.
- [ ] 2.2 Extend exact scenario regressions across single-slice and multi-slice long and short cases, including settled fees and invalid inputs; verify `executionScenario / test` proves unchanged price PnL, contribution order and attribution, fee contribution, total PnL, and public failure semantics.

## 3. Public boundary and integration evidence

- [ ] 3.1 Add completed-artifact boundary coverage that consumes the public calculation from instrument economics and rejects forbidden scenario, campaign, fee, application, or runtime ownership leakage; verify `adversarialBoundary / test` passes.
- [ ] 3.2 Run repository formatting checks and the full SBT test suite, resolving only regressions caused by this change; verify all configured checks pass on JDK 25 with no new dependency or module-cycle findings.

## 4. Canonical Verify remediation

- [ ] 4.1 Separate position identity/grid validation from market evidence, derive ending position through `PositionLots.flat` and `PositionLots.combine` after position reconciliation, and accumulate endpoint mismatches beside independently knowable market/reference failures in stable order; verify a real market-only-invalid flat request also reports its non-flat endpoint mismatch.
- [ ] 4.2 Restore the characterized four-case `ScenarioValuationError` surface, project every delegated failure reachable from public round-trip valuation to scenario-native leg/slice or construction locations, and add end-to-end invalid-round-trip plus completed-artifact exhaustive-match regressions.
- [ ] 4.3 Demonstrate that compatible checked inputs remain total under the current unbounded exact representation, adding direct open-short and cross-zero/reversal examples and signed generative paths without inventing a numeric ceiling or approximation.
- [ ] 4.4 Make the instrument-economics completed-artifact compiler classpath hermetic through an explicit compiler-support allowlist and assertions rejecting effect, codec, and other non-owning dependencies.
- [ ] 4.5 Run focused instrument-economics, execution-scenario, fee-policy, and adversarial-boundary checks, both reflection guards, formatting, the complete clean JDK-25 test/build matrix, and the automated repair-group review before its dedicated commit.

## 5. RFC-0007 amendment remediation

- [ ] 5.1 Add `ValuationReferenceDataMismatch(context, cause)` to the instrument-economics `ValuationError` algebra and project same-ID foreign-lineage failures through the existing `ScenarioValuationError.SliceValue` case at the original leg and slice, preserving the exact `ReferenceDataError`; verify the public round-trip path never fabricates an instrument mismatch.
- [ ] 5.2 Add completed-artifact exhaustive-match coverage for the expanded nested `ValuationError`, plus end-to-end scenario and fee-policy regressions proving reference-coherent behavior remains unchanged.
- [ ] 5.3 Reconcile RFC-0007's exact-totality contract—explicitly superseding Task 1.2's unreachable arithmetic-failure evidence request without changing the immutable completed group—and run focused suites, boundary checks, reflection guards, formatting, the clean JDK-25 build matrix, and the automated amendment-group review before its dedicated commit.
