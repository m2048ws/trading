## Context

See [proposal.md](proposal.md) for the motivation and the three capability deltas for normative behavior.

The current static engine has two coupled responsibilities:

1. validate and mathematically interpret the closed `Dimension` grammar; and
2. emit a canonical `Dim[...]` as the associated output of public `Normalize` evidence.

`Quantity`, `GridQuantity`, `DimRef`, rate helpers, zero construction, refinements, and algebra instances consequently
forward `Normalize` even when they need only one of those responsibilities. The three preceding changes separate
runtime authority, explicit equivalence, and trust in existing carriers, but their expected end state still leaves
`Normalize` at dimension-changing and empty-construction boundaries.

The implementation is Scala 3 macro code plus opaque carriers. It must remain sound for separately compiled downstream
code, not only for sources compiled in the library package. Runtime dimensions use stable path-dependent atom types and
arbitrary-precision `DimensionKey` powers. No runtime proof is stored in each `Quantity` or `GridQuantity`, and this
change must preserve that representation and all packed-record formats.

The runtime-instrument exploration exercises two requirements without moving instruments into this module: a venue
adapter must be able to construct rates from runtime-resolved endpoint witnesses, and a generic domain library must be
able to keep raw expression results or select a semantic output explicitly. Market prices and conversion rates are
time-varying data, not static evidence.

## Goals / Non-Goals

**Goals:**

- Separate private dimension interpretation from public result typing so that the former remains available to
  `SameDimension` and authority constructors after `Normalize` disappears.
- Make ordinary generic arithmetic signatures closed under `Dimension` expressions and independent of macros at their
  definition sites.
- Keep named rate and ratio operations ergonomic for both static and runtime-resolved endpoints.
- Preserve the carrier-construction invariant by replacing type-only validation with `DimRef` authority wherever an
  operation manufactures a value from nothing.
- Give downstream library authors a complete migration pattern for every use that previously depended on
  `Normalize.Aux` selecting an output type.

**Non-Goals:**

- Do not add a replacement public canonical-output typeclass under another name.
- Do not canonicalize or rewrite stored Scala expression types at runtime.
- Do not add proof fields to quantities, grid quantities, refinements, or rates.
- Do not infer currency roles, contract payoff formulas, conversion paths, or grids from venue metadata.
- Do not add instruments, positions, PnL models, orders, or a conversion-graph service.
- Do not change `DimensionKey`, rational, coordinate, registry, packed-record, or serialization representations.

## Decisions

### 1. Split the macro engine into private interpretation and authority gates

Remove the public `Normalize` trait, companion, `Aux` alias, and derived given. Retain one private macro implementation
with an internal representation equivalent to `List[(TypeRepr, BigInt)]`. The engine will expose only implementation
entry points needed by public operations:

- interpret a closed dimension expression and combine its powers;
- validate a declared canonical `Dim` and its singleton keys;
- compare two interpretations for non-reflexive `SameDimension` derivation; and
- validate the concrete literal key selected by the public literal `DimRef.atom` constructor.

The engine will no longer materialize `Dim[Entries]`, publish an associated `Out`, or issue reusable validity evidence.
Its key whitelist, alias/annotation exposure, cycle guards, and diagnostics remain centralized so that atom construction
and static equivalence cannot drift to different definitions of an accepted key.

`SameDimension.reflexive[D]` remains a non-macro identity instance. The non-reflexive derived instance invokes the
private interpreter on both complete expressions. A generic signature such as
`using SameDimension[Times[A, B], O]` therefore remains legal: the evidence is supplied at a call site where the types
are concrete or is forwarded from a higher-level abstraction. It is not automatically inferred from unresolved type
parameters at the generic definition.

The literal `DimRef.atom[K]` entry point remains compile-time checked even though `Normalize[Atom[K]]` vanishes from its
signature. Its inline/macro gate must reject a caller-manufactured `ValueOf[String & Singleton]` before delegating to
the lexically owned witness constructor. Nominal and fresh-runtime constructors already bind their exact path-dependent
types to constructor-owned keys and continue to do so. A separately compiled downstream fixture will verify that the
inline expansion does not leak an inaccessible helper or permit a package-name spoof.

Alternative considered: retain an advanced public `CanonicalResult[D] { type Out }`. This preserves the old generic
output computation but also preserves its authority-like appearance, macro-context forwarding, output refinement
surface, and malformed-representation attack surface. The explored client cases below all have a simpler expression,
endpoint, or `SameDimension` formulation, so the advanced capability is not retained.

Alternative considered: erase the macro engine entirely and compare only runtime `DimensionKey` values. That would make
ordinary static `SameDimension` alignment require runtime witnesses and would regress compile-time equivalence for named
dimensions, so private interpretation remains.

### 2. Preserve expressions in every generic dimension-changing primitive

Primitive signatures will state their result directly:

```scala
def *[B <: Dimension](that: Quantity[B]): Quantity[Times[A, B]]

def divideBy[B <: Dimension](
  that: NonZero[Quantity[B]]
): Quantity[Divide[A, B]]

def times[A <: Dimension, B <: Dimension](
  left: DimRef[A],
  right: DimRef[B]
): DimRef[Times[A, B]]
```

The corresponding grid/exact overloads follow the same types. `DimRef.inverse` and `DimRef.divide` return `Inverse[A]`
and `Divide[A, B]`. Opaque-owner helpers continue to construct coefficients and coordinates; the removed evidence was
compile-time-only and never participated in numerical calculation.

Homogeneous arithmetic uses one exact Scala type and no evidence. A differently spelled but equivalent value must be
converted with `alignTo`, as established by `demote-same-dimension`. Expression nesting is intentionally observable:
`Times[Times[A, B], C]` and `Times[A, Times[B, C]]` are different Scala types and are mathematically reconciled by
`SameDimension` when a common spelling is needed.

Alternative considered: make only generic methods expression-preserving while retaining canonical concrete overloads.
Scala overload selection would then make result typing depend on how much type information happens to be visible at a
call site, and separately compiled generic code could disagree with concrete code. One uniform primitive result rule is
more predictable.

### 3. Interpret powers with arbitrary precision without emitting a canonical type

Declared `Power[K, N]` entries remain restricted to nonzero singleton `Int` literals. The private interpreter converts
those literals to `BigInt` immediately and performs combination, negation, cancellation, and equality at arbitrary
precision. It no longer converts a final accumulated power back to an `Int` merely to emit an output type.

As a result, a raw expression can have a mathematically valid interpretation whose surviving power is outside the
singleton-`Int` range. `SameDimension` may compare that interpretation with another expression, and `DimRef` algebra
produces the matching arbitrary-precision runtime key. There need not be a canonical `Dim[Power[K, N]]` spelling for
that power.

Alternative considered: reject any primitive arithmetic whose private interpretation exceeds `Int`. That requires a
hidden contextual macro at every generic arithmetic call, recreates the usability problem under a private name, and
makes valid runtime-key algebra unavailable from abstract operands. The `Int` bound belongs to declared canonical
storage syntax, not to the mathematical expression group.

### 4. Make semantic endpoint operations deliberate typed primitives

`Rate.apply(from, to, coefficient)` will construct `DimRef.divide(to, from)` and then the exact rate directly. Since
witness division now returns `DimRef[Divide[To, From]]`, the rate alias matches without canonicalization or a cast.
The same implementation accepts registry-resolved and fresh path-dependent endpoint types.

The opaque quantity owner will provide coefficient-only internal helpers for these semantic operations:

- `applyRate`: `Quantity[From] × Rate[From, To] -> Quantity[To]`;
- `andThen`: `Rate[A, B] × Rate[B, C] -> Rate[A, C]`;
- checked `reciprocalRate`: `NonZero[Rate[A, B]] -> Rate[B, A]`;
- `crossRate`: rates with a common target -> the declared outer-endpoint rate; and
- `ratioTo`: same-type numerator and nonzero denominator -> `Ratio`.

These methods are not hidden canonicalization. Their endpoint equality is in their input types, their result spelling is
part of their semantic contract, and all inputs are already trusted carriers. Grid variants delegate through canonical
embedding and the exact endpoint helper. Ordinary `*` and `divideBy` remain expression-preserving even for the same
operands, so clients opt into semantic endpoint typing by choosing the semantic method.

Rate values remain explicit data or values returned by a domain-owned resolver. They will not become global givens:
several simultaneous market prices can share endpoints, and implicit selection would hide timestamp, venue, side, and
path choices. A future instrument library may carry endpoint witnesses and conversion values in its own generic or
runtime package; this module supplies only the typed arithmetic boundary.

Alternative considered: implement endpoint helpers by producing a raw expression and summoning `SameDimension` inside
the generic method. Non-reflexive static derivation cannot inspect unresolved endpoint parameters, so this merely moves
the old contextual requirement from `Normalize` to `SameDimension`. Direct trusted construction is both sounder and
simpler.

### 5. Use runtime inhabitation authority for empty construction

Construction behavior will follow this boundary table:

| Operation class | Authority used |
|---|---|
| Transform existing `Quantity[D]` or `GridQuantity[D, G]`, preserving indices | Existing carrier; no capability |
| Compute a new dimension from existing carriers | Raw `Times`, `Inverse`, or `Divide` expression |
| Manufacture `Quantity[D]` or grid zero with no existing carrier | `DimRef[D]` or a stronger matching witness |
| Attach or inspect a nonzero grid coordinate | Matching `GridRef[D]` |
| Cross between equivalent static spellings | Explicit `SameDimension` and `alignTo` |
| Reconstruct registry-owned data | Existing registry ownership and checked witness path |

`Quantity.zero[D]` and `GridQuantity.zero[D, G]` will take contextual `DimRef[D]`; a matching grid operation may pass
its `dimension` reference explicitly. Identity-bearing quantity vector spaces, grid modules, and nonnegative monoids
will require the same authority because their interfaces can manufacture zero. Combine-only semigroups, ordering,
sign observation, and transformations of existing refined values remain proof-free.

No carrier stores the witness. Possession of a value still does not allow recovery of `DimRef`, `GridRef`, registry
ownership, or a runtime key. This preserves the smaller authority boundary established by
`freeze-static-dimension-authority`: private interpretation may accept a valid singleton key for equivalence while no
public constructor inhabits `Atom[K]`.

Alternative considered: allow polymorphic zero for every `D` because its coefficient is harmless. That is incompatible
with the trusted-carrier invariant once dimension-preserving operations stop validating `D`; a type-only zero would be
the remaining supported route to `Quantity[Bad]` or `GridQuantity[Bad, G]`.

### 6. Carry expression witnesses through runtime heterogeneous results

Registry resolution remains generative and key-authoritative. Where heterogeneous arithmetic changes dimension, the
resolved result package will retain both the expression-typed value and the `DimRef` produced by matching witness
algebra. Where an operation has semantic endpoints, such as rate application, it retains the declared target witness.
Checked equality between independently resolved but equal endpoint types still produces scoped `SameDimension`; one
side is explicitly aligned before exact-type homogeneous arithmetic or composition.

Runtime adapter tests will build stable resolved endpoints, construct rates, and apply/compose them. A BitMEX-shaped
fixture may supply realistic identifiers, but the test will stop at the quantities boundary. It will not infer that an
`ETHUSD`-like symbol has a particular position currency, derive a payoff from multipliers, or add instrument domain
types. Those are explicit adapter/domain decisions.

Alternative considered: canonicalize every runtime-resolved static result to a fresh atom. That hides useful expression
relationships, makes identical computations generate unrelated static types, and forces runtime recovery for equality
that the private interpreter can already prove. Fresh opaque atoms remain appropriate only where the runtime
decomposition is intentionally hidden.

### 7. Downstream generic-programming evaluation

The explored cases do not justify retaining public canonical-output computation:

| Client case | Public formulation after this change |
|---|---|
| Generic multiply | Return `Quantity[Times[A, B]]` |
| Generic cross rate | Use `crossRate` and return `Rate[A, C]` |
| Generic notional with client-selected `O` | Accept `SameDimension[Times[P, S], O]` and call `alignTo[O]` |
| Homogeneous total | Accept one `D`; no capability |
| `Monoid` / `VectorSpace` with zero | Require `DimRef[D]` because the instance manufactures a value |
| Generic alignment | Accept `SameDimension[A, B]`; return `value.alignTo[B]` |
| Generic instrument PnL/conversion | Carry explicit endpoint rates; compose/apply semantic operations and retain raw expressions for truly generic products |

The only lost facility is compiler selection of an independently named output `O` for downstream code. In each
representative case the client either does not need `O`, already knows the semantic endpoint, or can name `O` and ask
for equivalence. We explicitly accept that trade-off in exchange for a smaller authority surface and predictable
expression result types.

### 8. Verify both source ergonomics and authority boundaries

Tests will be organized around public compiler boundaries rather than macro implementation details:

- positive compile fixtures for expression-preserving generic methods, nominated-output `SameDimension`, endpoint rate
  APIs, proof-free homogeneous/refined/grid operations, and identity-bearing instances with `DimRef`;
- negative compile fixtures proving `Normalize` and `Normalize.Aux` are unavailable, atom widening remains rejected,
  malformed `Dim` cannot acquire a witness or zero, and cross-spelling addition remains explicit;
- runtime laws showing every `DimRef` expression operation's `DimensionKey` matches primitive key arithmetic, including
  large accumulated powers and cancellation;
- registry/path-dependent fixtures showing runtime rate construction, application, composition, checked alignment, and
  retained result witnesses; and
- full downstream-package and same-package-spoof suites to catch leaked constructors or inaccessible inline symbols.

Documentation and examples will show both branches at every dimension-changing call: retain the raw expression, or
choose a semantic spelling with an endpoint helper or `alignTo`. No example will suggest that static equivalence alone
creates runtime authority.

## Risks / Trade-offs

- [Risk] Raw expression types grow with long arithmetic chains and make diagnostics verbose. → Prefer endpoint helpers
  for domain relationships, encourage local type aliases and intentional `alignTo` at API boundaries, and add a
  representative depth/diagnostic compiler fixture.
- [Risk] Removing `Normalize.Aux` breaks downstream signatures that expose compiler-selected `O`. → Document the two
  migrations (`Times`/`Divide` result or nominated `O` plus `SameDimension`) and include both as compiling fixtures.
- [Risk] Moving literal atom validation into an inline constructor accidentally weakens the concrete-key whitelist or
  leaks private implementation symbols after separate compilation. → Reuse one private key-policy implementation and
  retain adversarial downstream compilation for widened `ValueOf`, package spoofing, and nominal widening.
- [Risk] A missed public zero, refinement, decoder, or algebra path can manufacture a malformed carrier after
  operation-local validation is removed. → Audit every opaque construction call and require each call site to be
  classified by the authority table before deleting `Normalize`.
- [Risk] Direct endpoint helpers could become unchecked general-purpose retagging. → Keep their constructors and
  coefficient helpers lexically private, constrain source/target equality in method types, and expose arbitrary
  cross-spelling only through `SameDimension.alignTo`.
- [Risk] Private interpretation and runtime `DimensionKey` arithmetic diverge for aliases, annotations, cancellation,
  or large powers. → Run paired static-equivalence/runtime-key law fixtures over the same expression corpus.
- [Risk] Expression-preserving associativity is mathematical rather than Scala-definitional. → State this in scaladoc,
  test `SameDimension` across association and commutation, and keep semantic composition directly endpoint-typed.
- [Risk] Multiple contextual `DimRef[D]` values create ambiguous zero/algebra summons. → Keep witness provisioning local
  and explicit in examples; never make market rates or registry results global givens.
- [Risk] This source- and binary-incompatible change lands while the three prerequisite authority changes are active. →
  Implement against their combined expected end state and reconcile their remaining `Normalize` assertions before
  synchronization or archival.

## Migration Plan

1. Establish the expected end-state tests from `freeze-static-dimension-authority`, `demote-same-dimension`, and
   `trust-existing-dimensional-values`; if those changes have not landed, fold their relevant boundary behavior into
   the working branch before changing result types.
2. Refactor the macro engine behind existing APIs first. Add direct tests for private interpretation, arbitrary-precision
   equality, key validation, annotations, aliases, and recursion while behavior is still comparable.
3. Change `DimRef` algebra to expression result types and move literal-atom validation to the private constructor gate.
   Add runtime-key agreement tests before consumers migrate.
4. Change `Quantity` and `GridQuantity` primitive arithmetic to exact-type homogeneous operations and raw expression
   results. Add or finalize `alignTo` and migrate grid/refinement delegates.
5. Migrate `Rate`, `ratioTo`, composition, cross rate, checked reciprocal, and runtime endpoint construction to direct
   semantic results. Add static and path-dependent runtime client fixtures.
6. Change all empty construction and identity-bearing algebra instances from `Normalize[D]` to `DimRef[D]` or their
   stronger matching witness. Audit raw opaque construction, projection, decoding, registry adoption, and refinement
   closure against the authority table.
7. Remove the public `Normalize` definitions and all remaining production, test, documentation, and build references.
   Retain only private interpreter terminology. Run downstream positive and negative compiler-boundary suites, the full
   test suite, formatting, and API/documentation checks.
8. Reconcile and sync the three predecessor deltas so no archived requirement still directs clients to public
   normalization. Because runtime and wire representations are unchanged, release this as an explicitly breaking API
   version with source migration notes.

Rollback is a source-level revert of the change and its dependent client migrations. No data rollback, registry
migration, or packed-record rewrite is needed. Avoid publishing an intermediate artifact in which `Normalize` has been
removed before zeros and algebra identities have moved to `DimRef`, because that state would have either missing APIs or
an authority hole.
