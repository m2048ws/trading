## Context

See [proposal.md](proposal.md) for motivation. The present implementation models atoms as open `Dim` subtypes,
stores exponents in a recursive signed-natural encoding, and exposes separate evidence types for normalization, product,
inverse, quotient, and alignment. Because an arbitrary dimension subtype or associated type might later reveal reducible
structure, the macro performs extensive alias, refinement, selected-member, and term-path stability analysis before it
can decide whether a type is an atom.

The runtime model has a different and useful property: `DimKey` is already a canonical free-abelian-group value
using `BigInt` exponents. Runtime registries also need generative/path-dependent types because an identifier may be known
only after lookup. The redesign must simplify the static language without weakening that runtime authority or changing
persisted keys.

## Goals / Non-Goals

**Goals:**

- Make every statically representable dimension an instance of one sealed, closed grammar.
- Make ordinary source annotations use Scala singleton keys and literal `Int` exponents.
- Reduce all static algebra to one normalization computation and one restricted equality capability.
- Preserve precise concrete result types while giving generic code one explicit evidence pattern.
- Keep static and runtime dimension operations mathematically coherent within the static exponent range.
- Delete the semantic machinery needed only to classify arbitrary dimension subtypes and rebound associated outputs.

**Non-Goals:**

- Fractional or rational dimension exponents, dimension-valued roots, or non-integer powers.
- A total order over all atom keys or definitional equality for permuted canonical tuples.
- Static inspection of the hidden decomposition of a runtime-resolved opaque key.
- Changes to rational coefficients, grids, registry ownership, persistence records, or `DimKey` serialization.
- Source or binary compatibility with the current static dimension API.

## Decisions

### 1. Seal the dimension language and move identity into singleton keys

The source-visible grammar will have this shape:

```scala
sealed trait Dim
sealed trait Canonical[Entries <: Tuple] extends Dim
sealed trait Power[Key <: Singleton, Exponent <: Int]

type Atom[Key <: Singleton] =
  Canonical[Power[Key, 1] *: EmptyTuple]

type One = Canonical[EmptyTuple]

sealed trait Times[A <: Dim, B <: Dim] extends Dim
sealed trait Inverse[A <: Dim] extends Dim
type Divide[A <: Dim, B <: Dim] = Times[A, Inverse[B]]
```

`Dim` and its constructors are sealed in the quantities module. Downstream code creates identity by choosing a
singleton key, not by extending `Dim`. A literal key such as `"asset:USD"` gives concise stable domain aliases;
a stable object singleton can give nominal source identity; and a runtime witness can use its own `this.type` as a fresh
key. In all three cases the normalizer sees the same kind of leaf and never needs to ask whether an apparent atom is
secretly `Times`, `Inverse`, or an associated dimension output.

`Canonical[Entries]` is the only canonical result constructor. `Times` and `Inverse` remain expression syntax so callers can
write algebraic aliases without manually constructing tuples. `One` and `Atom` are aliases, ensuring that identities and
ordinary atoms are canonical by construction rather than separate cases that the macro must translate.

Alternatives considered:

- Keeping arbitrary `Dim` subtypes preserves the current open atom universe, but also preserves the hardest
  classification and substitution-stability problems.
- Restricting keys to literal strings would permit sorting known atoms, but cannot represent identities discovered at
  runtime without a second static model. `Singleton` supports both named and generative identities.
- Using only expression trees and never exposing a canonical `Canonical` would simplify validation but would retain expression
  history in result types and make equivalence harder to inspect.

### 2. Represent stored exponents with singleton `Int` literals

A stored entry is `Power[K, E]`, where `E` must expose to an `IntConstant`. Zero is permitted only in macro-local
arithmetic and is never emitted. A canonical `Canonical` rejects duplicate keys, zero powers, unresolved exponent types,
non-`Power` entries, and unresolved tuple tails.

The quoted implementation converts every decoded `Int` immediately to `BigInt`. Negation, addition, cancellation, and
validation use `BigInt`; no `scala.compiletime.ops.int` arithmetic is used. After the complete requested expression is
normalized and zeros are removed, each surviving value is range-checked and emitted with
`ConstantType(IntConstant(value.toInt))`. This specifically prevents `-Int.MinValue` and `Int.MaxValue + 1` from
wrapping. Cancellation inside one complete normalization may bring a macro-local value back into range before emission;
an earlier separately typed operation that itself needs an out-of-range result still fails.

Runtime `DimKey` continues to store `BigInt`. Static-to-runtime operations widen literal `Int` powers exactly.
Runtime-only keys may exceed the static range, but no static macro may claim a corresponding `Power` unless its exponent
fits. Diagnostics distinguish malformed/nonliteral exponents from mathematically valid but out-of-range results.

Alternatives considered:

- Decimal or floating singleton exponents are inexact and cannot represent values such as one third.
- Exact rational exponents would change the algebra from an integer lattice to a rational vector space and complicate
  roots, canonical reduction, and runtime compatibility without a current use case.
- Decimal-string exponents retain arbitrary precision but add parsing ceremony to every public type.
- `Long` changes only the bound, not the model; recursive or binary naturals retain unbounded semantics at much greater
  API and compiler complexity.

### 3. Replace specialized operation evidence with one `Normalize`

The only associated-output computation exposed by the static algebra will be:

```scala
sealed trait Normalize[D <: Dim]:
  type Out <: Dim

object Normalize:
  type Aux[D <: Dim, O <: Dim] = Normalize[D] { type Out = O }
  transparent inline given derived[D <: Dim]: Normalize[D] =
    ${ StaticDimension.normalize[D] }
```

Multiplication, inversion, quotient, and `DimRef` algebra all ask for `Normalize` of the corresponding complete
expression. Operation methods accept `Normalize[Expression]` and return its dependent `operation.Out`. Because the
derived given is transparent, a concrete call exposes the exact canonical `Quantity` or `DimRef` result rather than an
abstract path. Generic code that must name the result constrains the same context with `Normalize.Aux[..., O]`, forwards
it, and returns `O`; it no longer chooses among product, inverse, quotient, alignment, and normalized-powers evidence
families. This placement is necessary because implicit search does not infer a fresh method output parameter from a
transparent given's refinement before selecting the given.

The derived given is transparent so a concrete summon retains its exact output refinement. When an input dimension or
key is genuinely unresolved, derivation stops with a diagnostic instructing the generic definition to accept the final
`Normalize` evidence. It does not commit two abstract singleton keys as unequal merely because the compiler cannot yet
prove them equal.

`SameDimension[A, B]` remains a separate proposition, not a computation. Its static derivation invokes the same private
normalization engine for both sides and compares the resulting key/exponent maps modulo tuple order. Runtime recovery
continues to construct the same restricted capability only after authoritative key equality. `DimensionAlignment` is
removed; explicit checked retagging consumes `SameDimension` directly.

Alternatives considered:

- Pure match types are attractive for a closed grammar, but tuple merging, singleton-key equality, unresolved generic
  keys, useful diagnostics, and checked overflow are substantially clearer in one quoted derivation.
- Separate operation typeclasses merely give names to the expression being normalized and multiply the public and test
  surface without adding authority.
- A global implicit `Conversion` between equivalent quantities would hide meaningful boundary decisions and allow proof
  search to affect overload selection.

### 4. Use one small quoted normalizer over the closed grammar

The macro flattens the complete dimension into one ordered sequence of signed `(TypeRepr key, BigInt exponent)` entries.
It recursively handles only `Canonical`, `Times`, and `Inverse`; `Atom`, `One`, and `Divide` arrive through transparent alias
exposure. Only after the complete expression has been flattened are equal keys combined once in global first-occurrence
order and zero totals removed. Consequently, intermediate cancellation cannot erase a key's original position and
reintroduce it later in an association-dependent position. The final sequence is independently revalidated and emitted
as `Canonical[Tuple]`.

Semantic exposure is deliberately narrow:

- remove `AnnotatedType` wrappers and follow transparent aliases with a cycle/non-progress guard;
- accept only literal `ConstantType`, concrete stable `TermRef`/module or stable-value singleton, and supported generative
  `ThisType` endpoints;
- reject `Singleton`, `Nothing`, `Null`, `TypeBounds`, `AndType`, `OrType`, `Refinement`, unresolved `MatchType`,
  `TypeLambda`, ordinary non-term `TypeRef`, abstract/deferred/parameter keys, and unknown wrappers;
- reject abstract keys whose equality could change after generic instantiation, without relying on `key <: Singleton`
  or broad subtype-lattice tests as proof of concreteness;
- reject refinements, intersections, unions, unresolved match types, arbitrary `Dim` bounds, and unknown wrappers
  rather than interpreting them as atoms.

The implementation does not traverse typed local initializer graphs, search arbitrary holder refinements for associated
dimension endpoints, or retain active/completed term-analysis caches. Those mechanisms existed to determine whether an
open `Dim` subtype or an operation-associated output might later expose structure. Singleton keys are leaves by
definition, and the single `Normalize` output is either concretely refined by derivation or explicitly forwarded by
generic code.

Macro construction remains lexically private. Downstream code can name `Canonical` and `Power`, but naming a tuple is not proof
that it is canonical; all trusted operation and non-reflexive equality boundaries parse and validate the complete claim.

### 5. Make concrete caller results direct and generic results explicit

For concrete operands, transparent `Normalize` derivation preserves its exact dependent output:

```scala
val amount: Quantity[BTC] = Quantity(btc, Rational(1, 10))
val price: Rate[BTC, USD] = Rate(btc, usd, Rational(60000))
val notional: Quantity[USD] = amount * price
```

Quantity multiplication and division return the canonical `Canonical` produced by normalization. Single surviving
one-powers and the empty tuple are definitionally `Atom[K]` and `One`, so common cancellation exposes named atoms and
ratios directly. Addition keeps the left type and uses `SameDimension` only when its right type differs.

`Rate` remains an endpoint-oriented convenience over exact quantity semantics. `applyRate`, `andThen`, identity, and
`crossRate` return their declared endpoint types directly while requiring `Normalize` for the complete expression whose
cancellation justifies that result. Callers do not repair those results with separate alignment evidence. General
concrete rate multiplication and `divideBy` still use the canonical normalizer, and `SameDimension` remains available
when a caller intentionally chooses an equivalent composite alias or tuple order.

Generic operations use one output parameter:

```scala
def multiply[A <: Dim, B <: Dim, O <: Dim](
  left: Quantity[A],
  right: Quantity[B]
)(using Normalize.Aux[Times[A, B], O]): Quantity[O] =
  left * right
```

Endpoint-specific helpers such as applying `Rate[F, T]` can return `Quantity[T]` without exposing a normalization output
because the method signature already carries the cancellation law. This is a deliberate small convenience layer, not a
second public dimension algebra.

### 6. Give runtime witnesses concrete singleton-key aliases

Literal static dimensions use `DimRef.atom[K]`, which derives the runtime `AtomId` from the singleton string itself and
requires `Normalize[Atom[K]]`. The final normalization capability rejects an explicitly widened
`K = String & Singleton` even when a caller manually supplies legal `ValueOf[K]` values containing different strings.
Generic literal construction may forward `Normalize[Atom[K]]`; no second key-proof family is introduced.

Nominal object keys extend `DimRef.NominalAtom`, whose constructor owns one final authoritative `AtomId`. The public
constructor accepts only that key object and returns `DimRef[Atom[key.type]]`, with `Normalize[Atom[key.type]]` selected
automatically for concrete stable arguments. It therefore cannot pair an independently selected static supertype with
the runtime identifier, and two widened nominal values retain distinct stable-value result types. Generative atomic and
arbitrary-key witnesses expose a concrete alias equivalent to `Atom[this.type]` rather than declaring a fresh hidden
subtype of `Dim`. Their `DimRef` still carries the actual `DimKey` used at runtime.

`DimRef.times`, `inverse`, and `divide` calculate their output type through `Normalize` and their value through existing
`DimKey` operations. Tests compare both paths for every supported concrete shape. If an arbitrary runtime key
contains structure that is opaque statically, its singleton atom behaves as one indivisible generator. A later runtime
comparison may recover `SameDimension` against a statically decomposed dimension; the macro never guesses that equality.

Registry-only opaque witnesses are built inside the registry's private implementation by adopting a public generative
`DimRef.fresh` witness into the registry witness's own fresh `this.type`. No package-visible opaque constructor remains,
so declaring `package trading.quantity` downstream grants no construction authority. This keeps registry provenance and
persisted identities unchanged while removing the need for project-issued arbitrary `Dim` subtypes as a special
macro exception.

### 7. Preserve tuple permutation semantics instead of imposing key ordering

Normalization preserves first-occurrence order. `SameDimension` compares canonical entries as a mathematical map, so
`A * B` and `B * A` remain equivalent even when their tuple types differ. Literal strings could be sorted, but runtime
and object singleton keys have no stable source-level total order. A mixed ordering scheme would make inferred types
depend on whether a key happened to be compile-time-readable and would not eliminate equivalence evidence in the general
case.

The direct-result guarantee therefore applies to the computed canonical tuple and to results such as one surviving atom
or `One`. Selecting a differently ordered composite alias remains an explicit `SameDimension` operation.

### 8. Validate every dimension-preserving arithmetic boundary

`SameDimension[D, D]` deliberately remains Scala type identity, so it cannot also certify that a caller-written `Canonical`
is canonical. Every arithmetic operation that returns its input dimension therefore requests `Normalize[D]`; addition
and subtraction across `D` and `E` request both normalizations plus `SameDimension[D, E]`. The evidence is not refined to
`Normalize.Aux[D, D]`: `D` may intentionally be a valid source expression such as the `Divide[T, F]` spelling carried by
`Rate[F, T]`, whose normalization output is canonical but whose input spelling need not be.

The implementation audit classifies the public surface as follows:

| Boundary class | Validation decision |
| --- | --- |
| Dimension-changing quantity/grid product, quotient, inverse, rate endpoint operation | Retain the existing one `Normalize` for the complete expression; do not add redundant operand evidence |
| Dimension-preserving quantity, grid, allocation, quantization, projection/encoding, embedding, and refinement arithmetic | Require and forward `Normalize[D]`; require both `Normalize[D]` and `Normalize[E]` when `SameDimension[D, E]` participates |
| Arithmetic algebra instances | Require `Normalize[D]` when constructing vector-space, grid-module, or refined additive structures |
| `Eq`, `Order`, `Ordering`, and `Sign` | Retain without normalization because they only inspect opaque coefficients or coordinates and produce no arithmetic result |
| `DimRef`/`GridRef`-backed construction and coordinate inspection | Retain witness authority without redundant evidence; the witness fixes the static/runtime identity |
| Registry and heterogeneous arithmetic | Keep public registry provenance unchanged; path-dependent authoritative witnesses expose supported singleton keys, so lower arithmetic signatures derive valid normalization internally |

Grid projection, constraints, constrained encoding, quantization, allocation, and `Embedding.widenTo` create dimensional
numeric results and are included. `SameGrid.retype`, `SameQuantum.convert`, equality, comparison, persistence packing,
and registry-owned reconstruction are evidence recovery or witness-owned representation operations rather than public
arithmetic over an unchecked dimension parameter, so they do not acquire mechanical normalization requirements.

Arithmetic instances retain their required `Normalize[D]` context. Because the existing algebra contracts test Java
serialization of instances, the stateless sealed `Normalize` capability is serializable; this preserves instance
serialization without adding construction authority, arithmetic state, or a second proof family.

## Risks / Trade-offs

- **[A legitimate future formula needs fractional dimensions]** → Keep roots out of this change; if required later,
  introduce normalized rational numerator/denominator exponents as an intentional algebra change, never floating types.
- **[A static calculation exceeds `Int`]** → Calculate with `BigInt`, fail at the final emission boundary with the exact
  offending exponent, and preserve arbitrary precision in runtime-only keys.
- **[Transparent implicit refinement does not preserve the desired caller type in every Scala shape]** → Use dependent
  outputs on concrete operation methods and reserve `Aux` for generic code that already names `O`; retain downstream
  compile fixtures for concrete multiplication, cancellation, rate application, and generic evidence forwarding.
- **[Abstract singleton keys recreate late-specialization bugs]** → Derive automatically only for equality-stable concrete
  keys and require final contextual `Normalize` evidence for unresolved generic keys.
- **[Namespaced literal keys collide by convention]** → Treat runtime `AtomId`/registry authority as canonical and
  recommend namespaced literals; use object or generative singleton keys when nominal separation is required.
- **[Canonical tuples remain order-sensitive Scala types]** → Keep order mathematically irrelevant through
  `SameDimension`; do not promise definitional equality that cannot cover runtime singleton keys.
- **[The quoted API changes across Scala releases]** → Keep reflection localized to one parser/emitter and retain real
  downstream compiler fixtures with warnings treated as errors.
- **[A broad source migration obscures regressions]** → Land the representation and normalizer first with focused
  fixtures, then migrate each consumer layer and delete legacy machinery only after replacement coverage passes.

## Migration Plan

1. Add focused compile fixtures for the proposed caller surface, canonical literal powers, malformed tuples, overflow,
   generic `Normalize.Aux`, and runtime singleton witnesses.
2. Introduce the sealed grammar and single normalization evidence, then implement the closed quoted parser/emitter beside
   the old machinery long enough to compare canonical outputs and runtime keys in tests.
3. Move `DimRef`, exact quantity multiplication/division, rates, grids, refinements, and optional algebra to `Normalize`.
4. Migrate all static atoms, annotations, examples, and test fixtures to singleton keys, `Canonical`, and literal `Int` powers.
5. Remove signed naturals, `Powers`, specialized operation/alignment evidence, associated-output exposure machinery, and
   obsolete adversarial fixtures; retain boundary tests that still express real trust and generic-substitution risks.
6. Run formatting, focused compiler fixtures, the full multi-module test suite, downstream JAR compilation, and strict
   OpenSpec validation.

There is no runtime data migration. Source rollback must revert the static grammar, macro, and migrated callers as one
unit; runtime `DimKey`, registry, and packed data remain readable by either implementation.
