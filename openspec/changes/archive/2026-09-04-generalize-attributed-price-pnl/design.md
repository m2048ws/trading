## Context

See `proposal.md` for the motivation. Instrument economics already owns exact position, price, PnL, and settled-contribution concepts, while `execution-scenario` currently performs the multi-slice price fold privately as part of round-trip valuation. The new calculation must move that economic responsibility downward without reversing dependencies or introducing campaign/runtime concerns. Reference-coherent scenario behavior remains stable; rejecting a same-ID market from a foreign reference-data lineage is the explicit RFC-0007 compatibility exception.

The input is finite and may be empty. All quantities, prices, and results must retain identity, dimension, grid, provenance, and exactness through checked transitions. Public invalidity remains typed, independent failures accumulate deterministically, and dependent checks consume validated evidence.

## Goals / Non-Goals

**Goals:**

- Give instrument economics one domain-readable, pure entry point for exact single-instrument price PnL across arbitrary finite attributed changes.
- Preserve attribution and input order independently from the commutative aggregate economics.
- Make flat versus marked termination explicit and impossible to confuse in a successful result.
- Replace the scenario-local price fold with an adapter to the shared operation while preserving reference-coherent public compatibility and truthfully locating the foreign-lineage rejection.
- Keep validation, traversal complexity, laws, and downstream dependency boundaries explicit enough to verify.

**Non-Goals:**

- Defining campaigns, campaign membership, lifecycle transitions, books, or multi-instrument aggregation.
- Calculating execution fees, financing, tax, realized/unrealized reporting partitions, or risk.
- Introducing effects, live market lookup, codecs, persistence, concurrency, telemetry, or new dependencies.
- Generalizing over unrelated economic calculations before a second concrete use requires it.

## Decisions

### 1. Instrument economics owns an attributed finite-change algebra

Add an instrument-economics input product parameterized by an opaque attribution value. Each element pairs that attribution with one signed, priced position change. The calculation accepts an immutable finite sequence, including the empty sequence, and does not interpret the attribution.

The successful product contains:

- the exact ending position;
- an immutable sequence of settled price contributions in input order, each retaining its attribution;
- aggregate `PricePnl`; and
- a validated endpoint sum distinguishing flat completion from an open position marked at an exact price.

The endpoint request is also an explicit sum: flat, or marked with a price. A successful flat result carries proven zero exposure; a successful marked result carries proven non-zero exposure and its mark. The aggregate is derived only from settled contributions plus, for the marked case, the ending-position value at the mark.

This is preferred to an optional mark or `isFlat` flag because the alternatives admit contradictory states and discard why an endpoint is valid. A generic attribution parameter is preferred to scenario or campaign identifiers because economics needs only stable association, not knowledge of the upstream owner.

### 2. Validation is staged by dependency

Validate independently knowable facts applicatively and report them through an instrument-economics error ADT in deterministic input order: instrument identity, retained-reference coherence, quantity/price dimensions, and grid compatibility. Derive ending position before checking the endpoint relation, then validate flat/non-flat consistency and a marked price against that derived evidence. For compatible checked finite `BigInt`/`Rational` values, arithmetic is total under the current exact representation; invalid values remain failures of their owning constructors rather than a synthetic calculation failure.

The public operation returns a non-empty deterministic collection of errors on failure and never throws for expected domain invalidity. Any unavoidable internal partiality stays private behind checked non-empty invariants.

This staged model is preferred to fail-fast folding because independent defects should remain visible together, while an endpoint mismatch cannot honestly be diagnosed if no valid ending position exists.

### 3. Aggregate once with exact lawful combinations

After validation, map each input to one exact settled contribution and fold ending position and aggregate contribution with existing lawful quantity/PnL combinations. Apply the marked terminal value exactly once when required. The traversal remains linear in the number of changes and preserves the ordered contribution collection; no sorting or lossy numeric conversion is permitted.

The aggregate ending position and PnL are permutation-invariant for the same compatible multiset and endpoint, while contribution presentation follows input order. Keeping these two observations separate avoids making the output sequence falsely commutative.

This is preferred to incremental mutable state or a scenario-specific matching algorithm because price PnL is a signed cash-flow fold plus an optional terminal mark; matching lots is not needed for the accepted aggregate contract.

### 4. Round-trip scenarios adapt downward and fees remain downstream

`execution-scenario` converts each already-matched round-trip slice into an attributed priced change in its existing deterministic slice order and invokes the instrument-economics operation with a flat endpoint. It consumes the returned `PricePnl` and maps characterized validation failures so reference-coherent public scenario success and failure semantics remain stable. For a same-ID market from a foreign reference-data lineage, it maps the retained `ReferenceDataError` to `ScenarioValuationError.SliceValue` at the original leg and slice using the new `ValuationReferenceDataMismatch(context, cause)`. This truthful nested `ValuationError` expansion is the accepted compatibility exception; the adapter neither synthesizes a false instrument mismatch nor silently accepts incoherent references. The old independent scenario price fold is removed rather than retained as a fallback or comparison path.

Fee contribution and total-PnL combination remain in their existing downstream flow and occur once after shared price PnL succeeds. Instrument economics gains no dependency on scenario, fee, campaign, application, or runtime code; the dependency continues one way from scenario to economics.

This adapter is preferred to sharing a helper in `execution-scenario` because subsequent campaign slices need the economic operation without importing hypothetical execution concepts.

### 5. Verification separates algebra, compatibility, and boundaries

Instrument-economics examples cover empty/flat, non-empty/flat, and open/marked outcomes plus accumulated and dependent validation failures. Property tests cover at least three changes, scale-in/scale-out paths, attribution/order preservation, permutation-invariant aggregates, and linear settled cost using exact generators that respect refinements.

Scenario regressions cover reference-coherent single- and multi-slice long and short valuation, including fee-inclusive results and existing invalid cases, plus the same-ID foreign-lineage rejection at its original location. Completed-artifact compilation covers exhaustive matching of the expanded nested `ValuationError`; dependency checks demonstrate that the public operation is usable directly from instrument economics and that no forbidden reverse or runtime dependency was introduced.

## Risks / Trade-offs

- [Risk] The new public products duplicate or weaken existing exact primitives. → Reuse existing position, price, `PricePnl`, contribution, refinement, and error vocabulary wherever their semantics match; introduce only the attributed wrapper, endpoint sum, aggregate result, and genuinely owning errors.
- [Risk] Adapting scenarios changes contribution order, error accumulation, or fee totals. → Preserve existing slice order, add exact before/after regression fixtures, and remove the old fold only after the adapter covers all paths.
- [Risk] Exhaustive consumers of `ValuationError` must handle the added retained-reference case. → Make the accepted compatibility change explicit and prove exhaustive matching from completed artifacts.
- [Risk] Permutation laws are mistaken for permission to reorder attributed output. → Test aggregate invariance and ordered attribution as separate properties.
- [Risk] A future campaign requirement pressures this slice into premature multi-instrument or realized-PnL design. → Keep the operation explicitly single-instrument and aggregate-price-PnL only; later slices compose it rather than expanding this contract.
- [Risk] Validation requires extra traversals. → Prefer semantic clarity and complete errors; retain linear complexity and verify hot-path behavior in proportion to this small finite fold.

## Migration Plan

This is an additive in-process API followed by an internal scenario delegation, so no persisted-data or deployment migration is required. Implement and verify the instrument-economics operation first, adapt scenario valuation second, then remove the redundant scenario fold. Rollback consists of restoring the scenario delegation point and removing the additive economics surface within the same unreleased change.
