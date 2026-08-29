## 1. Portfolio and Baseline Gates

- [ ] 1.1 Confirm Proposals 0–9 are complete and approved for ordered application, Proposals 0–8 have landed, and the
  repository's Java 17 build/runtime baseline is effective before editing codec production code.
- [ ] 1.2 Refresh Git/OpenSpec/source/build state and record a clean baseline for formatting, compilation, catalog/
  assembly/order/scenario tests, external-artifact compilation, Java-serialization rejection, and full repository tests.
- [ ] 1.3 Inventory final stable IDs, catalog commands/outcomes/state/snapshot APIs, instrument definitions/assembly,
  economic market observers, order/scenario alternatives, and every removed packed-codec name; reconcile any portfolio
  drift before fixing V1.

## 2. Boundary-Codec Module and Dependency Direction

- [ ] 2.1 Add `boundaryCodecs` in `boundary-codecs/`, artifact `trading-boundary-codecs`, package `trading.codec`, with
  only direct quantities/reference-data/instrument-economics/order-model/execution-scenario, Cats Core, and Jackson Core
  3.x production dependencies; add Jackson-3-compatible NetworkNT JSON Schema Validator 3.x and an RFC-listed Java JCS
  reference implementation only to test scope.
- [ ] 2.2 Wire root aggregation, dependency-ordered tests, completed-JAR external-artifact compilation, adversarial/
  compiler classpaths, schema resources, golden test resources, and artifact documentation.
- [ ] 2.3 Add compiler guards proving every lower/pure artifact is independent of codecs and codecs cannot access fee
  policy, risk, application, runtime, Cats Effect, FS2, storage/network clients, clocks, transactions, tracing, or
  metrics.
- [ ] 2.4 Verify no public codec API exposes parser/JSON-library types, Jackson Databind/the Jackson Scala module, Circe,
  reflection-based generic decoding, `F[_]`, a live catalog, runtime resources, or a heterogeneous `Any` registry, and
  no test oracle/validator dependency appears on the production classpath.

## 3. Strict JSON, Paths, Limits, and Schema Algebra

- [ ] 3.1 Implement immutable structured field/index paths, syntax locations, stage ordinals, typed encode/decode/limit
  violations, and domain-owned non-empty deterministic aggregates.
- [ ] 3.2 Implement validated immutable `DecodeLimits` with the documented safe defaults for payload characters/bytes,
  depth, batch/object/array/string sizes, integer digits, dimension factors, commands, slices, and conversions; enforce
  exact input character/UTF-8-byte limits in project code before parsing.
- [ ] 3.3 Isolate the configured Jackson Core 3.x streaming parser behind one adapter that preserves raw number spelling/
  locations, enables strict duplicate detection, disables permissive non-standard JSON, configures stream constraints as
  defense in depth, catches expected parser failures, and returns an immutable package-private AST.
- [ ] 3.4 Implement RFC 8785-compatible canonical rendering for the restricted AST, including UTF-16 member ordering,
  Unicode validation, escaping, no whitespace, relevant canonicalization-vector tests, and comparison with the
  independent test-only JCS reference oracle.
- [ ] 3.5 Implement the package-private `WireSchema[A]`-shaped algebra for primitives, checked refinements, fields,
  products, tagged coproducts, vectors/traversal, limits, paths, and deterministic accumulating decode.
- [ ] 3.6 Implement interpretation of the same schema shape to local-reference JSON Schema Draft 2020-12 documents with
  stable URN IDs and `additionalProperties: false`/closed-case behavior; configure the test-only validator for this
  dialect and local resources only.
- [ ] 3.7 Add law/property tests for total invariant mapping identity/composition, product association under the public
  record projection, tagged-sum round trip, vector order/path preservation, schema/codec agreement, and no validation-
  library leakage.

## 4. Exact Primitives, Envelope, and Version Dispatch

- [ ] 4.1 Implement canonical signed/positive decimal-string validation with digit-limit checks before `BigInt`
  construction; test zero/sign/leading-zero/huge/invalid forms and platform-range independence.
- [ ] 4.2 Implement canonical reduced rational object encoding/decoding with positive denominator and zero-as-`0/1`;
  reject non-reduced/sign/zero-denominator/floating forms and property-test exact round trips.
- [ ] 4.3 Implement well-formed-Unicode validation and exact stable identifier construction for asset/grid/instrument/
  underlying/atom IDs without trimming, normalization, case folding, or unpaired-surrogate acceptance.
- [ ] 4.4 Implement canonical dimension-factor and full `GridIdentity` codecs, verifying nonzero powers, uniqueness,
  UTF-16 lexical order, exact normalization, positive grid version, and no `toString` parsing.
- [ ] 4.5 Implement validated record-type/schema-version values and the exact `{payload, recordType, schemaVersion}`
  envelope with family-specific dispatch, current writer selection, and typed unknown/missing/mismatched version errors.
- [ ] 4.6 Add shared fixtures for reordered/whitespace input, duplicate/unknown/missing/null members, cross-alternative
  fields, malformed Unicode, unknown types/versions, canonical re-encoding, and schema-versus-grid-version distinction.

## 5. Grid-Coordinate Record Families

- [ ] 5.1 Implement immutable Java-serialization-rejecting V1 general and asset grid-coordinate records containing only
  full stable grid identity, exact coordinate, and asset ID where applicable.
- [ ] 5.2 Implement packing from exact `GridHandle`/`GridQuantity` relationships without lookup, quantization,
  projection, copied quantum, or arbitrary-quantity overloads.
- [ ] 5.3 Implement constructor-private `DecodedGridQuantity` and `DecodedAssetGridQuantity` dependent packages without
  restoring old `Packed*`/`Resolved*`/registry compatibility names.
- [ ] 5.4 Implement general dimension-first then full-grid snapshot reconstruction and asset-first/dimension-check then
  full-grid reconstruction, with direct lookup and typed stage-specific failures.
- [ ] 5.5 Implement all-valid-or-indexed-errors batch reconstruction over one explicit snapshot and preserve input order
  without per-record live coordination.
- [ ] 5.6 Add exact/property/compiler/adversarial tests for compound dimensions, signed/huge coordinates, historical
  versions, unknown identities, asset-dimension drift, no grid scans, dependent type coherence, and inability to encode
  off-grid quantities implicitly.

## 6. Catalog Journal and Pure Replay

- [ ] 6.1 Implement frozen V1 tagged representations/codecs for dimension, asset, and grid catalog commands and ordered
  non-empty batches with exact definitions/quanta.
- [ ] 6.2 Implement Java-serialization-rejecting catalog journal entries containing positive successor revision and batch,
  plus construction from submitted batch and `Published` outcome only.
- [ ] 6.3 Implement entry and history structural decoding that accumulates all independent indexed wire violations before
  any state transition and preserves command/input order.
- [ ] 6.4 Implement replay validation for fresh empty revision-zero state and sequential next-revision checking through
  the normative pure `CatalogModel.commit`.
- [ ] 6.5 Implement final replay result plus typed revision-gap/repeat, catalog-violation, unexpected-unchanged, and
  non-fresh-start failures retaining entry/last-successful revision context but no successful partial state.
- [ ] 6.6 Add direct-model equivalence/property tests for empty history, published multi-command batches, valid duplicate
  commands, gaps/repeats, conflicts, no-op entries, prefixes, historical grid versions, command permutations, huge exact
  values, and new-lineage separation.
- [ ] 6.7 Add negative/API tests proving no journal path encodes or restores root/lineage tokens, handles, snapshots,
  mutable state, activation/delisting, timestamps, checkpoints, or live effects.

## 7. Instrument-Definition Codec and Assembly Composition

- [ ] 7.1 Implement frozen V1 instrument-definition products for one identity/underlying, role asset IDs, full listing
  grid identities, and canonical exact payoff coefficients with no trusted/revision/venue/market fields.
- [ ] 7.2 Implement structural JSON-to-`InstrumentDefinition` decoding that accumulates syntax/identifier/exact/local
  product failures without catalog lookup.
- [ ] 7.3 Implement the separate pure decode/assemble operation using exactly one snapshot, canonical
  `InstrumentAssembler`, and total `Instrument.fromSpec`, retaining codec versus assembly errors as typed stages.
- [ ] 7.4 Implement family batch reconstruction over one snapshot with stable indexed diagnostics and no partial success
  vector.
- [ ] 7.5 Add golden/property/model tests for exact linear/inverse/quanto definitions, missing/conflicting role/grid
  identities, empty payoff/equal roles, historical grids, cross-lineage reassembly, and absence of serialized authority
  or revision claims.

## 8. Immutable Order Codec

- [ ] 8.1 Implement frozen V1 products/coproducts for root instrument ID, side, lot coordinate, position effect, all
  activation cases, market/priced execution, limit/peg pricing, duration, liquidity constraint, and visibility.
- [ ] 8.2 Encode each valid order alternative with only case-local fields and omit derived position change, component
  instrument IDs, scenarios, venue lifecycle, fills, account state, and reported fees.
- [ ] 8.3 Implement explicit-instrument decoding that checks root identity, constructs lots/prices/refinements and exact
  alternatives, derives intent position change, and delegates to canonical accumulating `Order.create`.
- [ ] 8.4 Preserve structural, refinement, and order-validation failures as distinct deterministic typed stages and add
  all-valid batch decoding where useful.
- [ ] 8.5 Add exhaustive golden/round-trip/compiler tests for every activation/execution/pricing/visibility combination,
  foreign instruments, invalid coordinates/offsets/durations/icebergs, cross-case fields, derived signed positions, and
  unsupported `Any`/kind-plus-option decoding.

## 9. Hypothetical Scenario and Round-Trip Codecs

- [ ] 9.1 Implement frozen V1 scenario records containing the order once, activation/pricing observations, ordered
  non-empty slices, liquidity role, price/lots coordinates, base/quote settle rates, and ordered additional conversion
  source/rate pairs.
- [ ] 9.2 Implement scenario encoding from retained validated assumptions/markets while preserving slice/additional-
  conversion order and omitting duplicated instrument/target, actual-execution, fee-policy, fee/PnL, and lifecycle data.
- [ ] 9.3 Implement branch-sensitive decoding through the reconstructed order's associated evidence/resolution
  constructors without object identity, `Any`, or unchecked public casts.
- [ ] 9.4 Resolve additional conversion assets through the one supplied snapshot, construct typed source-to-settle/base/
  quote rates immediately, and delegate market/lots/slice/`MatchedSlices` construction to owning smart boundaries.
- [ ] 9.5 Invoke canonical accumulating `OrderScenario.evaluate` only after prerequisites and retain codec/catalog/
  evidence/market/scenario failures in stable typed paths/order.
- [ ] 9.6 Implement V1 round-trip as entry × exit only, decode eligible legs independently, and delegate held-position/
  exact-flat/identity proof to `RoundTripScenario.create` without cached PnL or fees.
- [ ] 9.7 Add golden/property/model tests for all evidence shapes, same-shape semantic replay, multi-slice order,
  long/short round trips, third-asset conversions, incoherent anchors, empty/mismatched slices, peg/trigger/limit/
  liquidity failures, cross-lineage assets, and hypothetical-not-executed semantics.

## 10. Limits, Batch Semantics, and Adversarial Robustness

- [ ] 10.1 Verify every family checks payload/input limits before expensive allocation where practical and integer digit
  limits before arbitrary-precision parsing, with no truncation, numeric overflow, expected exception, or stack overflow.
- [ ] 10.2 Verify oversized batches fail once before record work, within-limit independent records accumulate in stable
  zero-based order, and any failure suppresses the batch success vector.
- [ ] 10.3 Add controlled larger-limit profile tests proving operational policies do not change exact/schema semantics
  and that canonical writer output can report an explicit reader-profile rejection.
- [ ] 10.4 Add malformed/mutation/property fuzzing for syntax, deep nesting, duplicate/unknown/null fields, Unicode,
  exact strings, arrays, sums, and mixed independent errors; assert total typed outcomes and bounded completion.
- [ ] 10.5 Extend the shared non-published `benchmarks` JMH project with focused codec parsing/rendering/batch-
  reconstruction measurements separating JSON work, one live snapshot capture outside codecs, and pure per-record
  snapshot lookup; record JVM, arguments, batch size, payload shape/size, and thread count, and verify no monitor/atomic/
  live lookup occurs inside the batch loop.

## 11. Schemas, Compatibility, Documentation, and Scope Audit

- [ ] 11.1 Generate and check in one stable-URN local-reference JSON Schema Draft 2020-12 document per record family V1;
  test regeneration byte equality, validate every schema against the Draft 2020-12 meta-schema, and independently check
  JSON-valid schema-level valid/invalid fixtures with NetworkNT 3.x without network resolution.
- [ ] 11.2 Check in canonical JCS golden records/histories for every alternative, compare applicable renderer output
  with the independent test-only JCS oracle, and test accepted noncanonical JSON re-encodes canonically while
  semantically invalid/malleable forms remain rejected.
- [ ] 11.3 Add semantic round-trip properties within adequate limits and coherent snapshot/instrument contexts, plus
  frozen-version tests proving unknown versions fail and V1 never changes implicitly.
- [ ] 11.4 Extend project-owned Java-serialization rejection to every codec-owned record/dependent package, verify the
  lower-layer policies specified by their owning capabilities, and test that only explicit JSON text/UTF-8 bytes are
  supported durable forms at this boundary.
- [ ] 11.5 Document schema/version policy, exact encodings, limits, record-family APIs, snapshot capture/batch use,
  catalog-prefix replay, new-lineage semantics, hypothetical-scenario status, and absence of storage durability.
- [ ] 11.6 Audit production code/API/resources for removed packed aliases, serialized handles/snapshots/specs, generic
  object codecs, live/effect calls, hidden current snapshots, partial batch success, fee/risk/live-execution schemas,
  checkpoint authority, or claims of atomic journal persistence.

## 12. Verification and Steward Handoff

- [ ] 12.1 Format all affected Scala/SBT/JSON/Markdown sources and run clean compilation in dependency order.
- [ ] 12.2 Run primitive/schema/golden/unit/property/model/fuzz suites, all domain reconstruction dependents, negative
  compilation, completed-JAR external boundaries, Java-serialization checks, adversarial tests, focused shared-JMH
  smoke runs, and the full repository validation matrix.
- [ ] 12.3 Inspect packaged APIs and production/test dependency reports for leaked parser/Cats validation internals,
  Jackson Databind/the Jackson Scala module/Circe, test validator/oracle leakage, effect/runtime dependencies, unstable
  schema derivation, unchecked casts, raw-number precision loss, per-record coordination, and accidental persistence of
  authority.
- [ ] 12.4 Run strict validation for this and all active OpenSpec changes, perform the final portfolio-wide requirement/
  dependency/name/order cross-check, and reconcile every discovered artifact conflict before implementation begins.
- [ ] 12.5 Prepare the fully validated proposal/implementation worktree for fresh independent review without self-
  certifying, archiving, committing, or releasing outside steward authorization.
