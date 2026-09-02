## 1. Contract, Module, Identity, and Baseline Gates

- [ ] 1.1 Confirm the effective RFC/source/traceability bindings, accepted revision, closed immutable-order and instrument-economic prerequisites, isolated branch/worktree, current `origin/main` baseline, pilot authority, and absence of implementation edits outside this Change.
- [ ] 1.2 Inventory build aggregation, module/test classpaths, order/instrument dependent types, existing JVM construction guards, architecture/readme surfaces, and repository test/benchmark gates; record a clean formatting, dependency-ordered compile/test, packaged-boundary, and JMH-compilation baseline.
- [ ] 1.3 Add the non-empty `executionLifecycle` SBT project in `execution-lifecycle/`, published as `trading-execution-lifecycle`, with package root `trading.execution`, only the admitted order-model/instrument-economic/pure dependencies, root aggregation, and initial checked nominal identity production body.
- [ ] 1.4 Add execution-lifecycle completed-JAR/compiler classpath generation plus positive and negative boundary fixtures proving upstream/sibling modules cannot import it and it cannot import scenario, fee, risk, application, runtime, codec, effect, stream, client, persistence, telemetry, or venue-SDK mechanisms.
- [ ] 1.5 Add module documentation and identity/value/JVM tests covering stable application command, logical execution-order, lineage, source, account, native event/order/fill, stream, and sequence identities without generation, payload-hash, timestamp, or receipt-order authority.

## 2. Qualified Authority and Typed Lifecycle Construction

- [ ] 2.1 Implement source/account-qualified event, source-order, fill, stream-position, and execution-target products with nominal structural equality, hashing, documented authority/uniqueness scope, native provenance, and checked representation boundaries.
- [ ] 2.2 Implement closed explicitly-unsequenced and authoritatively-sequenced evidence, including qualified positions and explicit continuation/checkpoint/completeness data sufficient to report gaps and rewinds without using delivery order or time.
- [ ] 2.3 Implement lifecycle creation for one retained trusted instrument, compatible immutable typed order, logical execution-order identity, lineage, source, and account while preserving position/base/quote dimensions and the instrument position grid.
- [ ] 2.4 Define lifecycle-owned scope/construction violations and deterministic non-empty accumulation so independent identity/instrument mismatches accumulate and dependent values are built only from prior evidence.
- [ ] 2.5 Add Scala/Java completed-JAR, `javap -p`, serialization-rejection, equality/hash, and nearby-positive tests proving callers cannot forge qualified references, ordering evidence, execution targets, or lifecycle construction through constructors, copies, subclassing, same-package code, or erased inputs.

## 3. Commands, Dispatch Evidence, and Idempotent Command State

- [ ] 3.1 Implement closed immutable submit and cancel command bodies with stable application command identity, logical execution-order target, execution target, immutable order/lineage association, and no native amend or atomic cancel-replace alternative.
- [ ] 3.2 Implement the command identity index and typed transition classification so same-identity/same-body redelivery is idempotent, same-identity/different-body reuse is a retained conflict, and transport attempts or receipt identifiers never become business commands.
- [ ] 3.3 Implement separate proven-not-dispatched and indeterminate dispatch evidence tied to the original submit command, with typed rejection for foreign or incompatible command references.
- [ ] 3.4 Add deterministic command/dispatch validation and observation APIs that keep issued commands, dispatch knowledge, command conflicts, and cancellation requests as independent immutable evidence rather than one mutable status.
- [ ] 3.5 Add example, property, packaged-API, and adversarial tests for command retry/conflict, distinct logical orders, original-identity recovery, transport-attempt neutrality, expected typed failures, exhaustive alternatives, and absence of native-amend placeholders.

## 4. Authoritative Source-Fact Vocabulary and Provenance

- [ ] 4.1 Implement closed source facts for acceptance, rejection, fill, correction, bust, effective cancellation, and reconciliation/completeness evidence, each retaining qualified source-event identity, execution target, logical/source-order references, and ordering evidence.
- [ ] 4.2 Implement checked exact fill construction from instrument-bound positive lots and grid price, qualified fill/source-order identity, and retained native provenance without untyped maps, decimal conversion, hypothetical slices, or caller-forged instrument association.
- [ ] 4.3 Implement explicit correction and bust facts referencing qualified fill identity, plus unresolved-reference representations that preserve a modifying fact arriving before its target without inventing economics.
- [ ] 4.4 Implement source-event/fill identity indexes and classifications for same-identity duplicates, conflicting content, economically identical distinct fills, foreign-scope facts, and multiple claimants at one qualified stream position.
- [ ] 4.5 Add closed-location errors, deterministic source diagnostic ordering, JVM construction/serialization guards, and focused tests for every source-fact alternative, qualified deduplication scope, retained provenance, conflict retention, and hypothetical-scenario separation.

## 5. Deterministic Transition, Ordering, and Reconciliation

- [ ] 5.1 Implement the immutable lifecycle state and total checked transition result, retaining canonical command, event, fill, stream-position, unresolved-reference, completeness, conflict, and anomaly indexes with applied, duplicate, conflict, and rejected-input classifications.
- [ ] 5.2 Implement authoritative stream evaluation for exact missing ranges, duplicate/conflicting positions, explicit checkpoint/continuation rewinds, late gap fills, and explicitly unsequenced facts without interpreting network arrival or timestamps as business order.
- [ ] 5.3 Implement reference reconciliation and source-authoritative completeness boundaries so later targets resolve earlier correction/bust/cancellation/acknowledgement references and non-authoritative absence remains unknown.
- [ ] 5.4 Derive stable public observations of retained evidence, known state, incomplete scopes, non-empty diagnostics, and unresolved authority using closed location ordering rather than map iteration or arrival order.
- [ ] 5.5 Add replay/idempotence/permutation properties, sequenced and unsequenced examples, gap/conflict/rewind/reference recovery cases, deterministic structural equality, and operation instrumentation proving ordinary indexed transition work avoids a full-history scan.

## 6. Submission Knowledge and Indeterminate Recovery

- [ ] 6.1 Implement the closed derived submission-knowledge alternatives for issued/pending, accepted, rejected, proven-not-dispatched, indeterminate, execution-proven-without-acceptance, authoritative absence, and conflicting evidence while retaining their supporting facts.
- [ ] 6.2 Enforce that indeterminate submission blocks a fresh submit for the same logical order while allowing exact same-command recovery, explicit reconciliation, source-supported lookup, and safe defensive cancellation.
- [ ] 6.3 Reconcile later acceptance, rejection, or fill evidence against prior dispatch knowledge without fabricating missing acknowledgement facts or collapsing externally reported contradictions.
- [ ] 6.4 Admit non-acceptance from absence only when explicit source/account/order completeness evidence covers the lookup; retain non-authoritative absence as incomplete knowledge.
- [ ] 6.5 Add checked examples, generated transition laws, deterministic diagnostics, and completed-JAR tests for all four RFC submission outcomes, same-identity recovery, fresh-duplicate prevention, later fact refinement, and fill-without-acceptance behavior.

## 7. Effective Fill Ledger, Corrections, and Exact Exposure

- [ ] 7.1 Implement the effective-fill ledger so distinct qualified fill identities contribute separately, same-identity duplicates do not double count, conflicting variants remain diagnosed, and unresolved fills or modifiers contribute no invented value.
- [ ] 7.2 Implement authoritatively ordered correction and bust resolution that retains original/modifying facts, replaces only the target's effective exact economics, makes a busted fill's effective contribution zero, and reports ambiguous modifier chains.
- [ ] 7.3 Derive exact signed cumulative `PositionLots` from the retained instrument grid and immutable order side across active fills without floating-point, decimal, quantization, or dimension erasure.
- [ ] 7.4 Retain authoritative overfill exposure and return exact typed excess anomalies while rejecting foreign instrument/order/source/account economics before contribution.
- [ ] 7.5 Add partial-fill, duplicate, distinct-equal, correction, bust, modifier-before-fill, conflicting-chain, long/short, zero/equal/overfill, dimension/compiler-boundary, equality/hash, and delivery-permutation tests for the complete effective ledger.

## 8. Cancellation Races, Anomalies, and Lineage

- [ ] 8.1 Implement derived cancellation knowledge that keeps a cancel request distinct from authoritative effective cancellation and retains all command/source evidence.
- [ ] 8.2 Reconcile partial fills and cancel/fill races from authoritative source positions so delivery after a cancel message is not itself contradictory and explicitly unsequenced facts make no unsupported before/after claim.
- [ ] 8.3 Retain fills provably effective after cancellation as exact exposure with typed post-cancellation anomalies, compose them deterministically with overfill/source conflicts, and preserve later correction/bust effects.
- [ ] 8.4 Implement checked mechanism-neutral lineage linking distinct predecessor/successor logical execution orders only after confirmed predecessor cancellation, without mutation, native amendment, atomicity, priority, or cutover claims.
- [ ] 8.5 Add race-order permutations, late delivery, post-cancel, partial-fill, overfill, correction/bust, predecessor/successor, invalid-lineage, packaged API/import, Java/Scala construction, and explicit no-native-amend tests.

## 9. Architecture Reconciliation and Final Evidence

- [ ] 9.1 Update repository/module architecture documentation, dependency diagrams/audits, execution-scenario distinctions, and examples so actual execution ownership, authority, exactness, incompleteness, later application/runtime/codec direction, and Slice non-goals are explicit.
- [ ] 9.2 Format all affected Scala/SBT sources and run clean compilation and tests in dependency order for quantities, reference data, instrument economics, order model, execution lifecycle, unchanged sibling/downstream modules, root aggregate, and completed-JAR/adversarial boundaries.
- [ ] 9.3 Run the complete unit/example/property/replay/race/correction/completeness suites plus explicit JMH compilation and representative transition/replay measurements or operation-count evidence proportional to the implemented complexity claims.
- [ ] 9.4 Inspect packaged APIs, bytecode privacy, dependency classpaths, source imports, public signatures, Java serialization, error/result exhaustiveness, and forbidden native-amend/downstream-mechanism names for boundary drift.
- [ ] 9.5 Confirm every RFC AC against retained automated/human-readable evidence, validate OpenSpec and Corgi source/traceability strictly at the final planning revision, reconcile any permitted planning-only drift, and prepare the last acknowledged Task Group commit for separate Verify and human gates.
