## Context

See `proposal.md` for motivation. RFC-0005 Slice 2 has already made command transitions total, replaced encoded replay
keys with typed orderings, and made observation ledger derivation single-pass. The remaining execution-derived outputs
are nevertheless modeled by sealed abstract bases plus one final class per case, owner-side forwarding constructors,
and repeated equality/hash implementations. `LifecycleAccepted` additionally pairs a broad accepted class with a
`LifecycleTransitionKind` tag.

The execution package also contains intentionally guarded values. Commands, source facts, lifecycle and identity
values, non-empty diagnostic/conflict wrappers, and immutable transition states establish predicates at construction;
their restricted constructors and checked factories remain authority rather than representation boilerplate.

## Goals / Non-Goals

**Goals:**

- Make every converted alternative exhaustive by construction and remove parallel classification tags.
- Use compiler-generated structural equality and product support for derived values whose owners already establish all
  semantic meaning.
- Preserve dimensions and path-dependent grid types through each sum case and structural product.
- Keep fail-closed Java object serialization on all revised values.
- Keep completed-artifact Scala use domain-readable and exhaustively matchable.

**Non-Goals:**

- Do not convert commands, source facts, execution lifecycle/state, identities, refinements, or non-empty wrappers into
  unchecked products.
- Do not change replay, exposure, uncertainty, conflict, anomaly, validation, ordering, or transition semantics.
- Do not promise Java source or JVM binary compatibility and do not alter durable codecs or schemas.
- Do not convert quantity/reference-data or order/scenario models assigned to later RFC-0005 slices.

## Decisions

### Use sealed Scala sums with generated case products

Represent command transitions, source-fact transitions, lifecycle transitions, submission knowledge, cancellation
knowledge, and effective-fill classification as sealed traits plus final case classes. Each case directly owns exactly
its valid payload and inherits `JavaSerializationUnsupported` from the sum root.

This is preferred over keeping abstract classes with hand-written subclasses because the compiler supplies the product
shape, extractor, equality, and hash implementation and checks exhaustiveness. It is preferred over forcing all
families into Scala `enum` syntax because these sums carry three path-dependent dimension parameters and existing
top-level case names are clearer to downstream callers. Sealed traits plus case classes are the equivalent direct
Scala sum permitted by the RFC.

`LifecycleTransitionKind` is removed. Accepted lifecycle classifications become distinct applied, idempotent, and
conflicting cases; rejection remains a separate case carrying its typed rejection. This keeps classification in the
sum shape instead of a tag field.

### Convert only owner-derived field products

Use final case classes for submission and cancellation evidence, command/source conflicts, effective-fill ledgers,
post-cancellation and aggregate anomalies, lifecycle observations, and replay results. Preserve any useful computed
accessors such as conflict identities, anomaly fill identity, and `ExecutionAnomalies.isEmpty` in the case-class body.
Construction remains package-restricted where values are meaningful only as the output of lifecycle derivation.

State machines (`CommandState`, `SourceEvidenceState`, and `ExecutionState`) remain guarded classes even though they
have structural equality: their field relationships are transition invariants and unrestricted `copy` would create
states the transition owners did not derive. Non-empty wrappers (`CommandViolations`, `SourceFactViolations`,
`LifecycleDiagnostics`, `SubmissionConflicts`, `ModifierAmbiguity`, and source classifications) likewise retain their
checked construction.

### Preserve public meaning while allowing Scala source-shape migration

Retain the current top-level alternative names and public case fields where practical so ordinary matches remain
domain-readable. Construction sites switch from forwarding helpers and `new` calls to generated companion
construction. Downstream fixtures use exhaustive matches over each revised sum rather than reflection or generated JVM
layout assertions.

Scala source syntax may change where the new lifecycle transition alternatives replace `LifecycleAccepted.kind`; this
is the source-shape break explicitly admitted by the RFC. No compatibility adapter or deprecated tag is retained.

### Verify serialization and guarded authority independently

Focused tests serialize representative instances of every converted family through `ObjectOutputStream` and require
the inherited fail-closed hook to throw. Completed-artifact compiler fixtures exhaustively consume the new sums and
their fields. Existing positive/negative suites remain the evidence that commands, source facts, lifecycle identity,
non-empty wrappers, dimension/grid association, and external reconstruction still use checked factories and typed
failures.

## Risks / Trade-offs

- [Generated `apply` or `copy` exposes an invariant-bearing value] → Convert only derived cases/products, retain
  restricted construction where ownership matters, and leave state and non-empty wrappers as guarded classes.
- [Generic sealed cases lose path-dependent precision] → Keep all `D`, `B`, and `Q` parameters on roots and cases and
  compile completed-artifact matches against real instrument/grid values.
- [Case-class serialization support bypasses the rejection hook] → Keep `JavaSerializationUnsupported` on every sum
  root or product and exercise representative converted instances with `ObjectOutputStream`.
- [Removing the lifecycle kind tag changes semantics at call sites] → Map each old accepted kind one-for-one to a
  dedicated case and retain transition state/work equality assertions.
- [A mechanical conversion accidentally weakens checked input construction] → Bound edits to enumerated derived
  families and rerun focused factory/null/negative compiler suites plus the clean aggregate matrix.

## Migration Plan

1. Convert source, command, and lifecycle transition alternatives and update their exhaustive consumers.
2. Convert submission, cancellation, and effective-fill derived sums while retaining dimension precision.
3. Convert the remaining field-valid derived products, remove forwarding/equality boilerplate, and strengthen
   completed-artifact, serialization, and guarded-boundary evidence.

Every step remains independently buildable and is committed as its own Task Group. A rollback reverts the relevant
Task Group commit; there is no data migration or runtime rollout.
