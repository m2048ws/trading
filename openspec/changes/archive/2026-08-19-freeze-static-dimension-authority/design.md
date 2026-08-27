## Context

See `proposal.md` for motivation. The current Scala 3 model already has a closed static grammar and a single quoted
`Normalize` operation, sealed `SameDimension` evidence, sealed `DimRef` witnesses, and runtime `DimKey` algebra.
The relevant guarantees are nevertheless described as if they formed one linear trust chain in some places, even though
the APIs deliberately support statically valid keys for which no public runtime witness exists.

The implementation also permits `Quantity.zero[D]` from `Normalize[D]` while requiring `DimRef[D]` for caller-supplied
coefficients. That is intentional: `Quantity[D]` is a phantom-indexed exact value, not a carrier of runtime identity.

## Goals / Non-Goals

**Goals:**

- Establish one capability model that keeps static validity, runtime inhabitation, equivalence, and exact values distinct.
- State runtime authority as a unique mapping over publicly inhabitable atom types, not as a total mapping over every key
  accepted by normalization.
- Make the authority argument explicit at each public atom root and preserve it inductively through `DimRef` algebra.
- Protect the boundary with downstream compiler fixtures and public documentation suitable for future ergonomic work.

**Non-Goals:**

- Narrow the current set of concrete singleton keys accepted by `Normalize` merely because some have no public `DimRef`
  constructor.
- Add an `AtomAuthority`, implicit conversion, proof token, registry lookup, or other evidence family connecting
  normalization to runtime inhabitation.
- Make `DimRef[D]` automatically provide `Normalize[D]`, or make `Quantity[D]` retain a witness or runtime key.
- Change result typing, tuple-order equivalence, `DimKey` representation, registry provenance, grids, or persistence.

## Decisions

### 1. Treat the model as four independent capabilities

The public contracts are:

| Capability | Meaning | Does not establish |
| --- | --- | --- |
| `Normalize[D]` | `D` is in the closed static grammar and has canonical `Out` | A runtime witness or key |
| `DimRef[D]` | `D` is inhabited by this authoritative runtime key | Contextual static normalization evidence in generic code |
| `SameDimension[A, B]` | Controlled equivalence of two dimension indices | Validity or runtime inhabitation by itself |
| `Quantity[D]` | An exact coefficient indexed by `D` | A `DimRef[D]` or `DimKey` |

No new automatic implication will be introduced between these capabilities. In particular,
`Normalize[Atom[K]]` will never be a constructor, resolver, or existence proof for `DimRef[Atom[K]]`.

Alternative considered: describe the model as `Normalize -> DimRef -> DimKey`. This is rejected because
normalization has no runtime atom mapping and intentionally accepts concrete stable singleton identities that are not
publicly inhabitable.

### 2. Define atom authority as a partial-domain function

Let `N` be the singleton keys accepted by `Normalize[Atom[K]]`, and let `I` be atom types inhabitable through supported
public `DimRef` APIs. The intended relationship is:

```text
I ⊆ N
authority: I -> DimKey
```

`authority` is total and single-valued on `I`; it is not defined for every member of `N`. Therefore, for publicly
obtained `r1` and `r2` of the exact type `DimRef[Atom[K]]`, `r1.key == r2.key` must hold. Unsupported casts, reflection,
or other violations of Scala's type-safety boundary are outside this contract.

Alternative considered: shrink `N` until it equals `I`. This would reject currently supported stable module, local-value,
non-string literal, or unrestricted `ThisType` normalization shapes solely because no runtime constructor is defined for
them, conflating static canonicalization with runtime authority.

### 3. Bind authority at roots and preserve it inductively

The supported roots each prevent the static type and runtime key from being selected independently:

- Literal atoms derive `AtomId` from the exact accepted string singleton.
- Nominal atoms return the supplied stable value's exact singleton type and obtain the identifier from that same object's
  constructor-owned final value.
- Atomic and fresh witnesses use their own path-dependent singleton type and retain the identifier or complete key
  captured by that witness.
- The identity witness always denotes `DimKey.one`.

`DimRef` construction remains sealed behind these roots. Product, inverse, and quotient take authoritative input
witnesses, use one complete `Normalize` computation for the static output, and apply the matching `DimKey` operation
to the input keys. Atom uniqueness therefore extends to constructed dimensions by induction without a second proof
system.

Alternative considered: expose a constructor accepting both `K` and `DimKey` plus validation evidence. No static
validation can prove that an arbitrary caller-selected key is the unique runtime interpretation of `K`, so that shape
would recreate the authority hole.

### 4. Keep equivalence independent from certification

Reflexive `SameDimension[D, D]` remains ordinary Scala type identity and may exist even for a malformed representation.
Non-reflexive static derivation continues to validate both closed expressions, while runtime recovery continues to require
authoritative witness equality. The resulting capability authorizes only controlled retagging; it does not bundle
`Normalize`, `DimRef`, registry provenance, or unrestricted type equality.

Arithmetic boundaries remain responsible for requesting `Normalize` wherever validity is required. Retagging and
comparison operations that do not create arithmetic results may continue to consume only their documented equivalence or
witness authority.

### 5. Keep quantities witness-free after construction

Public coefficient-bearing construction continues to require `DimRef[D]`, which prevents arbitrary coefficients from
being attached to an uninhabited dimension. Dimension-polymorphic zero continues to require only `Normalize[D]`, because
static validity is sufficient for the additive identity. A `Quantity[D]` stores no runtime witness and offers no route
back to one.

Alternative considered: store `DimRef[D]` inside every quantity. This would change representation, equality and allocation
costs, erase the intentional zero distinction, and duplicate registry/runtime concerns in static arithmetic.

### 6. Verify the boundary as a downstream compilation contract

The existing immutable-JAR compiler fixture remains the primary adversarial boundary. Coverage will explicitly include:

- a normalized stable singleton key with no matching public `DimRef` construction;
- separate `DimRef[D]` and `Normalize[D]` requirements in generic code;
- repeated literal, nominal, atomic, and fresh witness authority;
- rejection of widened literal and nominal keys and downstream `DimRef` construction;
- reflexive `SameDimension` on a malformed `Canonical` followed by rejected arithmetic; and
- agreement between normalized static outputs and runtime keys for witness algebra.

Scaladoc and the quantities README will use the same four-capability vocabulary and avoid describing normalization as
producing or guaranteeing a runtime witness.

## Risks / Trade-offs

- **[The freeze remains documentation-only and later code drifts]** → Encode every non-implication and authority root in
  downstream positive or negative compiler fixtures, not only prose.
- **[Negative tests become coupled to compiler wording]** → Assert public diagnostic fragments and rejected API shapes
  rather than full compiler messages.
- **[Partial runtime inhabitation surprises users]** → Document representative normalized-but-uninhabited keys and
  state explicitly that callers needing runtime identity must start from a supported `DimRef` authority source.
- **[A future ergonomic API accidentally merges capabilities]** → Treat these requirements as the prerequisite contract
  for later changes and require any new convenience to preserve the non-implications.
- **[A discovered implementation gap requires a source restriction]** → Prefer sealing or binding the affected public
  constructor; do not narrow unrelated normalization inputs or introduce a new evidence family.

## Migration Plan

1. Add focused downstream fixtures for the capability separation and authority uniqueness statements.
2. Audit public `DimRef` roots and algebra against those fixtures, making only boundary-tightening changes required by a
   failing contract.
3. Align Scaladoc and README terminology with the four independent capabilities.
4. Run formatting, focused compiler fixtures, the full quantities and adversarial-boundary test suites, and strict
   OpenSpec validation.

There is no runtime or persisted-data migration. Rollback consists of reverting the source, tests, and documentation from
this change together; `DimKey`, registry, grid, and packed-record formats are unchanged.
