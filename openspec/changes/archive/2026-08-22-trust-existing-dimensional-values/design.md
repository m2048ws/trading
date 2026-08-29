## Context

See `proposal.md` for motivation. `Quantity[D]` and `GridQuantity[D, G]` are opaque carriers with lexically private raw
construction. Coefficient-bearing quantities are created from `DimRef[D]`; nonzero grid coordinates are created through
`GridRef[D]`; polymorphic zeros use `Normalize[D]`; checked alignment and grid evidence select validated target types;
and runtime decoding reconstructs coordinates only after registry resolution. Refinements wrap an existing carrier.

The current implementation nevertheless requests `Normalize[D]` again from most operations that preserve `D`.
`freeze-static-dimension-authority` deliberately separates static evidence from runtime identity, while
`demote-same-dimension` makes homogeneous arithmetic exact in its Scala dimension type. This change is the next stage:
it relies on both boundaries, but supersedes their temporary requirement that homogeneous arithmetic revalidate `D` at
each call.

The supported Scala trust boundary already excludes casts, reflection, unsafe JVM access, hand-written bytecode, and
constructor-bypassing deserialization. Downstream compilation tests also establish that declaring the library package
does not expose opaque construction. A direct compiler probe confirms that cast-free `null` cannot inhabit either opaque
dimensional carrier. Reference-valued construction authority must also be dereferenced at its public root so literal
`null` cannot produce a witness capable of attaching a coefficient or coordinate.

## Goals / Non-Goals

**Goals:**

- Make valid dimension indexing an inductive invariant of every normally returned dimensional carrier.
- Remove only those normalization contexts whose sole purpose is to revalidate an unchanged index.
- Classify target-witness transitions, computed-result operations, type-only manufacture, and observations distinctly.
- Preserve the existing static/runtime non-implications and grid-provenance requirements.
- Turn the constructor audit into a durable downstream compilation and checked-decoding contract.

**Non-Goals:**

- Do not make a dimensional value a source of `Normalize`, `DimRef`, runtime-key, grid, or registry evidence.
- Do not store a proof or witness inside every value, change opaque representations, or add a validity evidence family.
- Do not remove complete-expression normalization from multiplication, dimensional division, rate operations, ratios,
  or witness algebra.
- Do not add weaker algebra instances that compete with the existing strongest coherent hierarchy.
- Do not broaden the supported trust boundary to unsafe casts, reflection, bytecode, or unchecked deserialization.

## Decisions

### 1. Treat carrier validity as an inhabitation invariant, not an evidence implication

The public model distinguishes evidence search from the semantic origin of a value:

```text
Quantity[D] or GridQuantity[D, G] returned normally
                         implies
                 D was validated at a root

value[D]  does not imply  summon[Normalize[D]]
value[D]  does not imply  DimRef[D] or DimKey
gridValue[D, G] does not imply GridRef[D, G] or registry identity
```

An index-preserving implementation can construct a result from legitimate operands because it never needs to interpret
or recompute `D`. Generic code still cannot normalize a new expression, create zero from the type, obtain runtime
identity, or interpret a grid without the corresponding capability.

Alternative considered: derive or cache `Normalize[D]` from every value. This would add an evidence implication and
likely runtime representation, contrary to the frozen authority separation. The implementation instead relies on the
same sealed-construction discipline already used by opaque refinements.

### 2. Establish the invariant inductively over every public root and transition

The audit obligation is:

| Boundary | Why the result index is valid |
| --- | --- |
| `DimKey` and `DimRef` roots | Reject null atom/key identity before returning canonical runtime authority |
| `Quantity.zero[D]`, `GridQuantity.zero[D, G]`, refined zero | Requires non-null `Normalize[D]` |
| Coefficient-bearing `Quantity[D]` | Requires sealed authoritative `DimRef[D]` and a non-null exact coefficient |
| Nonzero `GridQuantity[D, G]` | Constructed and inspected only by its sealed `GridRef[D]` from a non-null coordinate |
| `DimRef` and `GridRef` algebra | Root authority plus non-null complete-expression normalization |
| Dimension-changing quantity arithmetic | Complete result expression is normalized through non-null evidence |
| `alignTo` and `SameDimension` coercion | Static derivation validates both sides, or runtime recovery compares authoritative witnesses |
| `SameGrid`, `SameQuantum`, `Embedding` | Checked source and target grid witnesses own both selected types |
| Refinement construction | Wraps an already trusted carrier after a closed observation |
| Projection, quantization, allocation | Consumes a trusted value and reconstructs only through a target or source grid witness |
| Registry adoption and logical decoding | Resolves dimension before grid and reconstructs through a registry-owned grid witness |
| Heterogeneous arithmetic | Recovers checked evidence, aligns or retypes, then combines trusted values |

No public raw coefficient or coordinate helper may escape the opaque owner, and those central helpers reject null
numeric payloads before returning a carrier, including coordinates reconstructed by checked decoding. Sealed `DimRef`,
`GridRef`, registered witness, proof, refinement, and result constructors remain part of the audit. Packed records remain
untrusted data until checked decoding completes. A public root that accepts reference-valued authority or payload must
inspect that value before it returns a carrier or another witness that can construct one. This includes retained
`Normalize` contexts: literal-null evidence must be rejected before returning a zero, computed carrier, witness, rate,
or identity-bearing algebra capability.

Alternative considered: keep operation-local normalization as defense in depth. It catches hypothetical malformed
parameter types but adds no protection for values obtainable through supported APIs; keeping it would defeat the generic
ergonomics this change targets.

### 3. Classify operations by result authority rather than by the word “arithmetic”

Every public operation follows one of five rules:

| Operation class | Required authority |
| --- | --- |
| Manufactures a dimensional value from the type alone | `Normalize[D]` |
| Transforms existing trusted values at the same `D` | No dimension evidence |
| Selects a target already owned by equivalence or a witness | The relevant proof or witness, no extra `Normalize` |
| Computes a new type-level dimension | `Normalize[CompleteExpression]` |
| Observes without constructing a dimensional value | No dimension evidence |

“Changes dimension” is intentionally not one undifferentiated category. `alignTo`, same-grid retyping, same-quantum
conversion, embedding, and registry recovery select already validated targets; multiplication and quotient arithmetic
compute new types and retain complete-expression normalization.

Endpoint-shaped rate and ratio helpers remain in the computed-result category for this change even where their trusted
operands could support a stronger future argument. This keeps result-typing policy unchanged and bounds the migration.

### 4. Remove contexts from index-preserving primitive operations

After `demote-same-dimension`, the exact public shapes become:

```scala
def +(that: Quantity[D]): Quantity[D]
def -(that: Quantity[D]): Quantity[D]
def *(factor: Rational): Quantity[D]
def exactDivideBy(divisor: NonZeroWhole): Quantity[D]
```

Grid addition, subtraction, integer scaling, negation, and exact whole-scalar division follow the same rule. Cross-grid
`addExact` and `subtractExact` retain different grid parameters but one exact `D`, and remove their remaining
`Normalize[D]`. Private coefficient and coordinate helpers lose the same unused contexts so implementation structure
matches the public contract.

Quantity/grid multiplication, dimensional division, rate application and composition, ratios, and `DimRef` product,
inverse, and quotient retain normalization of their complete expressions. Zeros retain `Normalize[D]` because they have
no trusted operand or target witness.

Alternative considered: remove normalization from all operations whose operands are trusted. This would also change
generic result computation and endpoint policy, exceeding the requested dimension-preserving simplification.

### 5. Propagate the rule through grids and refinements

Narrowing, constraint validation, quantization, constrained encoding, Euclidean quotient/remainder, and allocation all
start from a trusted carrier and reconstruct coordinates through a supplied `GridRef`; their `Normalize[D]` contexts are
removed. Refined variants delegate to those proof-free primitives and keep only their sign/refinement logic.

Checked grid relationships retain the evidence that proves source/target compatibility. `Embedding.widenTo` removes its
`Normalize[A]` and `Normalize[B]` contexts because the private evidence already retains validated source and target grid
witnesses. The same reasoning already applies to `SameGrid.retype` and `SameQuantum.convert`.

`GridQuantity.zero[D, G]` remains available from `Normalize[D]` without a grid witness because every supported grid is
zero-anchored. This establishes only dimension validity, not that arbitrary `G` has a grid definition. Exact embedding,
coordinate inspection, and packing therefore continue to require the matching witness.

### 6. Gate algebra instances by manufacture, not by their element type

The quantity vector space, grid module, and nonnegative quantity/grid monoids retain `Normalize[D]` because their
interfaces can manufacture zero before receiving any value. Their primitive `plus`, `negate`, and scalar actions become
proof-free internally, but the strongest instance remains construction-gated as a whole.

Positive quantity and grid additive semigroups remove `Normalize[D]` because they expose only combination of existing
positive values. Orders and equality remain proof-free. No separate `AdditiveSemigroup[Quantity[D]]` or weaker grid
instance is introduced: it would compete with the strongest vector-space/module hierarchy when normalization is also
available and would violate existing coherence policy.

Alternative considered: split identity manufacture from every unital structure so individual operations can be
summoned proof-free. That requires a broader algebra redesign and new coherence choices; primitive arithmetic already
provides the desired common ergonomics.

### 7. Keep runtime and persistence reconstruction as trusted roots

Packed records remain freely constructible untrusted boundary data. Decoders must still resolve the canonical dimension,
verify the dimension-scoped grid, and attach coordinates only through the returned registered grid witness.
`ResolvedAssetGridQuantity`, `ResolvedGridQuantity`, and `ResolvedExactQuantity` then contain trusted carriers.

Heterogeneous same-grid arithmetic recovers checked grid evidence, retypes to the selected grid, and calls proof-free
same-grid arithmetic. Exact heterogeneous arithmetic recovers right-to-left `SameDimension`, embeds, calls `alignTo`,
and performs proof-free homogeneous arithmetic. Error ordering, registry provenance, dependent result types, and packed
formats do not change.

### 8. Test construction rejection separately from transformation acceptance

The former negative fixture “a method over `Quantity[Bad]` cannot add” is intentionally inverted. Positive compiler
fixtures demonstrate that index-preserving method bodies over abstract or even hypothetical indices require no evidence.
Separate negative fixtures prove that supported callers cannot normally construct the malformed arguments through raw
opaque values, zeros, `DimRef`, grids, alignment, algebraic identities, registry adoption, decoding, or literal `null`.

This division is essential: testing only proof-free operations would miss a constructor escape, while continuing to test
operation-local rejection would silently preserve the old model.

## Risks / Trade-offs

- [A construction escape makes proof-free operations reachable for malformed `D`] → Audit every root and checked
  reconstruction path first, then lock it with immutable-JAR downstream fixtures before removing contexts.
- [The earlier changes encode superseded negative tests] → Implement and archive the three changes in dependency order,
  then deliberately replace operation-local malformed-arithmetic fixtures in this change.
- [A broad mechanical removal reaches dimension-changing operations] → Classify every `Normalize` occurrence and retain
  complete-expression evidence before applying edits.
- [Algebra instances become ambiguous] → Remove contexts only from combine-only existing instances; add no competing
  weaker instances for quantities or grids.
- [Source and binary compatibility surprise] → Mark the release as breaking, document explicit-`using` migration, and
  require downstream recompilation.
- [Users mistake carrier trust for runtime identity] → Keep `DimRef`, grid, registry, and packing APIs witness-based and
  add negative tests showing no capability can be recovered from a value.

## Migration Plan

1. Complete or reconcile `freeze-static-dimension-authority` and `demote-same-dimension`, preserving their constructor
   and explicit-alignment boundaries.
2. Add construction-root and checked-reconstruction fixtures, including malformed types, package spoofing, logical
   decoding, and cast-free null rejection, before changing operation signatures.
3. Remove `Normalize` from primitive quantity/grid transformations, then grid services and refinements, checking each
   layer against the classification table.
4. Retain normalization on zeros, complete-expression operations, and identity-bearing algebra; remove it from positive
   semigroups and verify instance coherence.
5. Update runtime heterogeneous flows, examples, documentation, and the superseded compiler fixtures.
6. Run formatting, focused suites, adversarial boundaries, the full SBT suite, binary/source-surface checks, and strict
   OpenSpec validation for all three staged changes.

There is no runtime-data or persistence migration. Rollback is a coordinated source revert of the signature and test
changes; downstream artifacts must be recompiled against whichever API generation is selected.
