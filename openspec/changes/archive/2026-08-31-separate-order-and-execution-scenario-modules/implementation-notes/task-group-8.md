# Task Group 8 — Verification Evidence and Corgi Handoff

## Run authority

- Run: `run-a0f11aa6-d868-45bc-bfcd-872a4a5b6e49`
- Current group: `8` (`Verification Evidence and Corgi Handoff`)
- Starting state revision: `8`
- Previous acknowledged commit: `79d099b117706a31e44c076fdc28c68a945de30f`
- Planning revision: `sha256:b1c31669d8693dd3b954fd0234b2198ba3e892d9e5330e54d24f421de996fe83`

## Verification boundary

- Clean compilation ran explicitly in dependency order: quantities, reference data, application, instrument economics,
  order model, execution scenario, transitional economics, then the adversarial compiler-test boundary. All modules
  compile with the configured warning policy on OpenJDK 26.0.2.
- Module-local property laws now verify that every positive order-lot count normalizes once to its exact side-directed
  retained position and that equal opposite retained positions close exactly while unequal pairs retain both signed
  coordinates. These supplement the example, negative-compilation, and completed-JAR suites.
- Packaged-JAR inspection now requires the final liquidity, assumptions, checked scenario, closed diagnostic, and
  round-trip classes. It continues to reject the removed `Orders`/`Scenarios` services, universal errors, and any
  order/scenario production package in the transitional economics JAR.

## Baseline fixture comparison

- Valid instruction alternatives, local refinements, fixed/trailing evidence, direct/pegged resolution, signed
  position intent, reduce-only retention, slice conservation, activation/pricing behavior, liquidity-role rules,
  limit quality, long/short round trips, exact fee conversion, charge/rebate signs, minimum charges, no-fee and
  fee-inclusive PnL, and schedule composition retain their original mathematical outcomes.
- Downstream comparison explicitly preserves missing-conversion leg/slice attribution, foreign fee-line rejection,
  exhaustive candidate visitation, non-monotone greatest-candidate selection, scenario-failure propagation, and flat-
  fee sizing outcomes.
- Intentional public differences are limited to the accepted Slice: instrument-owned service views are replaced by
  explicit smart constructors; the duplicated scenario target and its reference reconciliation are removed; universal
  order/scenario wrappers become locally owned closed violations; diagnostics follow stable stage/rule/index ordering;
  and held position is the retained signed `PositionLots` rather than an unsigned count projection.

## API and dependency audit

- `trading-order-model` imports only quantities and instrument economics and owns no market state, scenario, fee, risk,
  application, runtime, codec, lifecycle, or effect capability. `trading-execution-scenario` depends one way on order
  model plus instrument economics and owns no upstream mutation or downstream policy/runtime code.
- Production order/scenario sources contain no casts, mutation, throws, effect kinds, live catalogs, lifecycle state,
  untyped evidence maps, object-reference reconciliation, compatibility aliases, or old capability names. Signed
  side/count multiplication occurs only in the owning `OrderIntent` normalization; scenario/round-trip code consumes
  retained positions and checked instrument algebra.
- The transitional fee policy still contains its pre-S-02 per-slice valuation arithmetic and source-market reference
  guard. S-02's task 7.3 and baseline inventory require fee behavior to remain unchanged; RFC S-04 AC-015 explicitly
  owns removal of caller-forged attribution and reference-equality reconciliation. Neither transitional detail leaks
  into the new order or scenario artifacts, and this delivery does not begin S-04.

## Planning and source integrity

- `openspec validate separate-order-and-execution-scenario-modules --strict --no-interactive` — pass.
- `corgispec ready separate-order-and-execution-scenario-modules --strict --json` — ready with all checks passing.
- Planning revision remains `sha256:b1c31669d8693dd3b954fd0234b2198ba3e892d9e5330e54d24f421de996fe83`.
- Source digest remains `sha256:5d573cd0d12bbcffacb9b047475ee4beb81069dedabd420018b5d3f32f7acdd0`.
- Traceability digest remains `sha256:a265f9092baad966ec7ef5f731ab1a889dc3542f8bd9a5e893b1f8338af6de5f`.
- AC-005 through AC-008 remain mapped exactly once to their planning anchors and completed Task Groups.

## Checks and automated review

- `sbt -batch scalafmtAll` — pass.
- Clean dependency-order compilation through adversarial `Test/compile` — pass.
- Focused order property suite — pass, 1 property.
- Focused scenario property suite — pass, 1 property.
- Focused downstream equivalence suite — pass, 10 tests.
- Focused economics compiler/JAR boundary suite — pass, 29 tests.
- Final `sbt -batch scalafmtCheckAll clean test` — pass: 601 quantity, 13 reference-data, 7 application, 13 instrument-
  economics, 7 order-model, 8 execution-scenario, 10 downstream economics, and 126 adversarial tests (785 total).
- `git diff --check` — pass.
- The first automated review found missing order/scenario property suites. After adding the two laws and rerunning the
  full matrix, the second review returned no findings across all applicable axes. No finding was human-triaged; this
  Task Group review is not canonical whole-change Verify or Human Review.

After acknowledgement, Apply must stop at `awaiting_verify`. Canonical Verify, explicit human whole-change Review,
Human QA if applicable, and Archive remain separate gates.

The only runtime warning is the existing Scala `sun.misc.Unsafe` terminal-deprecation notice under JDK 26.0.2.
