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
