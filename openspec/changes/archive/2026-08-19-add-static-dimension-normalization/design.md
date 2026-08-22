## Context

See `proposal.md` for motivation and the delta specs for behavioral requirements.

The static algebra currently represents multiplication and inversion as unreduced `Times` and `Inverse` syntax, while
`DimensionKey` already implements the corresponding free abelian group at runtime as an arbitrary-precision atom-to-power
map. `Quantity` and `GridQuantity` arithmetic therefore expose expression history, and `applyRate`, `andThen`, `ratioTo`,
and runtime `SameDimension` recovery manually bridge selected algebraic equalities.

The implementation must continue to support nominal source dimensions, fresh path-dependent dimensions, dimensions
resolved from a registry, opaque exact quantity representation, exact grid embedding, and the existing lexical trust
boundaries. Scala match types can recursively compute concrete type structure but can remain unreduced for abstract inputs,
so generic and dependent cases cannot rely on definitionally reduced match types alone.

A post-implementation review exposed two additional compiler constraints. First, negative equality search performed while
compiling a generic method can commit independently abstract parameters as unequal; if a caller later instantiates those
parameters with the same dimension, the associated output can retain duplicate factors or treat a now-visible product as
one opaque factor. Second, a public companion derivation that recursively searches through package-private proof rules
compiles with inaccessible-member deprecation diagnostics in supported downstream source and is rejected when those
warnings are fatal. Independent downstream review further demonstrated that inspecting only a selected member symbol
misses term-parameter roots such as `x.D`, that public guards and recursive rule carriers can be transported and
specialized into malformed final evidence, and that shallow exponent/factor validation accepts noncanonical structures.
A follow-up independent review found that an outer transparent alias could hide the same unresolved generic or
parameter-rooted endpoint, while a concrete term-prefixed alias could be retained as an opaque factor even when its right
hand side was a visible product. The revised design addresses all of these constraints at the final derivation boundary.
A final focused review found that SBT consumers still crossed mutable main-product directories and that Scala annotation
wrappers, despite being definitionally transparent, were rejected for ordinary dimensions while some annotated natural
magnitudes slipped through equality checks. A subsequent review showed that making every consumer JAR-backed introduced
a `Compile/fullClasspath` → `exportedProducts` → `packageBin` → `Compile/compile` self-cycle and still raced in aggregate
builds. It also showed that a stable local term can carry generic/refined semantic evidence through rebinding. The final
remediation strips annotations in the shared semantic exposure operation, restores normal same-project test products,
uses one completed external-consumer JAR task, and classifies the semantic type carried by every selected-type qualifier.
A later independent review found two remaining depth/recursion gaps: exact concrete operation endpoints were exposed only
one selection at a time during final factor validation, and an active typed-local term recursion was treated like an
already completed visit. The final type-system remediation follows concrete associated endpoints to a guarded semantic
fixed point and records active and completed traversal states separately.

## Goals / Non-Goals

**Goals:**

- Give every statically reducible product, quotient, and inverse a compact powers representation with exact signed
  exponents and no zero or duplicate factors.
- Treat permutation as representational rather than semantic and derive trusted static equivalence evidence.
- Let generic code request algebraic computation or equality evidence without requiring concrete currency types in the
  method body.
- Make generic algebra substitution-stable so late specialization produces the same canonical output as a direct concrete
  operation.
- Keep recursive proof construction and validation authority inseparable from the exact final operation derivation.
- Recursively validate exponent magnitudes and conservatively reject reducible, refined, intersected, or unresolved
  factor structures before issuing trusted evidence.
- Follow transparent aliases to their semantic endpoints for both stability classification and canonical parsing, while
  preserving documented stable abstract associated identities as opaque atoms.
- Treat transparent Scala annotations as semantic no-ops, omit them from canonical output, and validate their exposed
  underlying structure by the same conservative rules as unannotated input.
- Give definitionally equal inputs one coherent canonical output and re-expose every final factor before trusted evidence
  construction.
- Keep automatic derivation usable from real downstream compilation units with the supported warning policy.
- Use one restricted `SameDimension` capability at arithmetic call sites regardless of whether its premise was proven
  statically or checked at runtime.
- Keep static normalization and runtime `DimensionKey` operations consistent without weakening runtime provenance.

**Non-Goals:**

- Assign a global ordering, ordinal, or semantic hierarchy to dimension atoms.
- Provide global implicit conversions between quantity types or unrestricted Scala `A =:= B` evidence.
- Add rational powers, roots, floating arithmetic, or approximate coefficients.
- Generate atom types from venue APIs or introduce instruments, product payoffs, fees, P&L, orders, or positions.
- Change grid coordinate identity, grid closure, registry provenance, or persistence record formats.
- Generalize transparency to refinements, intersections, unions, unresolved match types, or arbitrary Scala wrappers.

## Decisions

### 1. Normalize to a power sequence modulo permutation

The static normal form will conceptually be a tuple of `Power[Factor, Exponent]` entries. Normalization will flatten
visible products, negate inverse powers, merge equal factors, add signed exponents, remove zero results, and retain one
entry for every surviving factor. Multiplication will preserve the left operand's surviving order and append previously
unseen factors from the right. Inversion will preserve entry order while negating powers.

Tuple order is deliberately not part of mathematical identity. A project-owned alignment proof will establish that two
normalized tuples contain the same factor/exponent entries modulo permutation. This avoids requiring a total order over
nominal or path-dependent Scala types and keeps the atom universe open.

When a dimension is statically opaque, normalization will treat that exact type as an indivisible symbolic factor. The
same opaque type can still cancel against its inverse. The implementation will not guess that two independently abstract
types are equal or unequal; it will require derivation or checked evidence when their relationship is unavailable. In
particular, failure to find `A =:= B` while compiling a generic method is not stable proof that `A` and `B` remain unequal
after method instantiation.

Alternatives considered:

- A globally sorted tuple would provide definitional equality, but requires an artificial and compiler-visible total atom
  order that is unavailable for independently resolved dependent types.
- Keeping raw `Times`/`Inverse` syntax would preserve the current inability to compose realistic dimensional formulas.

### 2. Use a project-owned exact signed exponent model

Static exponents will use a project-owned signed-natural representation with canonical zero, positive, and negative
states. Zero may exist during computation but will never be stored in a normalized power tuple. The representation will
have unbounded mathematical semantics; compiler resource exhaustion must stop compilation rather than wrap an exponent.
The runtime counterpart remains `BigInt` in `DimensionKey`.

The initial magnitude encoding may be chosen for simple and predictable reduction because trading dimensions normally
have small powers. If that encoding remains visible in supported result annotations, it is part of the source-level static
algebra and replacing it, for example from unary to binary, is a breaking migration even though the mathematical exponent
semantics do not change. The implementation may recover an evolvable internal encoding only by placing a stable public
exponent surface in front of it and proving through downstream compilation tests that the concrete encoding no longer
leaks.

Alternatives considered:

- Singleton `Int` arithmetic is convenient but introduces a machine bound and potential mismatch with runtime `BigInt`.
- Rational exponents materially change the dimension group and have no current requirement.

### 3. Separate type computation from propositional evidence

Public operation typeclasses with associated output types represent multiplication, inversion, quotient, normalization,
and alignment where generic code must carry a computation or proof. Their automatic givens delegate to one atomic quoted
derivation for the complete requested operation. The macro inspects the complete input types, computes powers in
macro-local data, independently validates the final tuple and output dimension, and only then constructs the final
evidence. It does not request caller-supplied merge, insertion, removal, alignment-rule, guard, or token evidence.

One lexical/private semantic-exposure operation is shared by the unresolved classifier, algebraic parser, tuple parser,
exponent parser, and final factor validator. It discards definitionally transparent `AnnotatedType` wrappers, follows each
transparent alias one definition at a time, including applied and concrete term-prefixed aliases, and recursively visits
relevant nontransparent wrappers without treating them as transparent. An active alias-symbol set rejects cyclic,
recursive, mutually recursive, or non-progressing representations cleanly. Following one definition at a time is
important: `type X = holder.D` exposes `holder.D` for path classification instead of using an unrestricted recursive
dealias that could skip over an abstract/refinable associated-member boundary. Annotation removal and alias exposure
recur to a stable semantic endpoint, so annotating an alias cannot restore opacity.

Deferred selected members are not globally dealiased. Their qualifier path remains visible to one lexical/private
semantic-stability classifier shared by initial unresolved classification, atomic-factor classification, associated
output handling, and final factor validation. Stability of a dependent associated type requires stability of both the
term path and the semantic type carried by every qualifier. The classifier recursively examines relevant `RefinedType`,
`AppliedType`, `TypeRef`, `TermRef`, `AnnotatedType`, intersection, union, bounds, match-type, and type-lambda forms and
follows typed local-value initializers with visited type, term, and selection guards.

Method type parameters, term parameters, paths rooted in term parameters, arbitrary abstract/refinable members,
refinable projections, unresolved exponent magnitudes, and stable locals whose widened/ascribed types retain any such
dependency stop automatic derivation, even through aliases or repeated local rebinding. A singleton ascription or an
`Aux[A, B, O]` refinement does not make `A`, `B`, or `O` substitution-stable. Paths whose identity is fixed by a
project-issued `DimRef` or registry witness remain available as opaque atomic identities. A final-operation associated
output is reusable automatically only when the qualifier's semantic type exposes an exact concrete output and all of its
dependencies are substitution-stable; that endpoint is normalized rather than retained as an opaque operation atom. A
stable arbitrary holder or unresolved operation value does not make its abstract/refinable member canonical. A
non-deferred concrete alias such as `holder.D = Times[A, B]` is transparent and is flattened before any atomic-factor
decision. Generic methods state the corresponding final operation evidence contextually and forward that exact value,
deferring derivation until the caller has substitution-stable concrete inputs.

Concretely refined associated endpoints are exposed transitively to a guarded semantic fixed point. After every exact
endpoint exposure, the resulting representation is fed back through transparent alias/annotation exposure and ordinary
algebraic parsing; a selected `operation.Out` that ultimately denotes `Powers[...]`, `Times`, `Inverse`, `One`, or another
reducible form is therefore parsed as that form rather than retained as an atomic factor. Stable opaque identities issued
by `DimRef` or a registry witness are deliberately not exposed, and unresolved/refinable members remain unresolved. The
same fixed-point operation runs during input classification, parsing, and final factor validation so endpoint indirection
depth cannot change the canonical result.

Type, term, and selected-member traversal distinguish active analysis from completed analysis. Re-entry into an active
term or semantic selection is conservative unresolved evidence; only a fully traversed result is cached as completed.
Completed results may be reused from multiple branches, so a shared acyclic value graph remains valid while self,
two-node, and longer recursive paths cannot establish stability. Active markers are removed on every traversal exit, and
endpoint/alias non-progress has an independent guard.

No recursive proof carrier is public. `InsertPower`, `MergePowers`, `RemovePower`, `AlignPowers`, `Guard`, and `Token` are
absent from the source-visible derivation API. The quoted implementation is validation logic, not an unchecked factory or
a second algebraic representation, and generated public signatures contain only the public evidence façade and canonical
dimension types.

### 4. Make equality evidence automatic at operations but target selection explicit

Multiplication and checked division will return the associated simplified output dimension. Addition and subtraction will
accept a right operand with an equivalent dimension when `SameDimension` evidence is available and will retain the left
operand's dimension type.

An explicit evidence-checked `asDimension[Target]` operation will select an equivalent public result type. It will be a
zero-allocation phantom retagging backed by `SameDimension`, not a numerical conversion. There will be no global implicit
`Conversion[Quantity[A], Quantity[B]]`.

Existing rate conveniences will remain for readability and stable endpoint APIs, but their coefficients and dimensions
will agree with generalized multiplication, division, and explicit alignment. Exact operations that leave a grid will
embed through their witnesses and reuse the same exact arithmetic rather than implement a second normalization path.

### 5. Use one restricted `SameDimension` capability with two trusted sources

`SameDimension[A, B]` will remain privately constructible and will expose only controlled quantity/grid retagging plus
contextual use by approved arithmetic. Static derivation will issue it after proving normalized powers equal modulo
permutation. Runtime factories will issue it only after authoritative `DimensionKey` equality and existing registry-owner
checks. It will not be promoted to unrestricted Scala type equality.

Reflexive `SameDimension[D, D]` records Scala structural type identity and is intentionally available even when `D` is a
manually named malformed `Powers` structure. It does not certify canonical form and cannot authorize construction of a
canonical operation result. Alignment between distinct representations and every automatic normalization, product,
quotient, inverse, or alignment result independently validates canonical structure.

Runtime evidence will remain a value inside `Option` or `Either`. A caller may install successful evidence as a local
contextual value, but the library will not publish runtime-derived global givens. This prevents ambiguity with static
derivation and keeps the runtime success path explicit.

### 6. Keep static witnesses and runtime keys in lockstep

`DimRef` product, inverse, and quotient construction will compute the new static output type through the same algebraic
operation represented at runtime by `DimensionKey`. Source-visible factors can normalize statically; a fresh witness for
an arbitrary runtime key may remain a statically opaque factor. Algebra that depends on the hidden decomposition of such a
factor will require checked runtime `SameDimension` evidence, preserving the existing runtime/compile-time boundary.

Runtime keys, packed records, and registry identity remain unchanged, so this change has no data migration.

### 7. Validate normal-form claims and keep derivation downstream-safe

A caller being able to name a `Powers[P]` type is not itself proof that `P` is a canonical normal form. Stored exponents
must be exactly `PositiveExponent[M]` or `NegativeExponent[M]`; `M` must recursively consist only of
`NaturalSuccessor[...]` nodes terminating in `NaturalZero`. Bare `Natural`, abstract or refined magnitudes, zero
exponents, duplicate factors, non-`Power` entries, empty or nonminimal `Powers`, and unresolved tuple structure are
invalid.

Stored factors must be concrete and structurally irreducible under the supported algebra. Transparent annotations are
removed before this decision and never become stored factors. Their exposed underlying atom is accepted exactly when the
unannotated atom is accepted; exposed `One`, `Times`, `Inverse`, `Powers`, the base `Dimension` bound, and reducible or
unresolved structures remain rejected. Intersections, refinements, match types, bounds, and other unknown wrappers remain
conservative rather than gaining annotation-like transparency. Every automatic evidence macro re-parses its computed
tuple, re-exposes annotations, transparent aliases, and every exact concrete associated endpoint to a guarded fixed point,
re-runs substitution-stability classification, and requires every surviving factor to satisfy the documented stable
irreducible-identity predicate immediately before construction.
Malformed input or output cannot bypass this check through contextual intermediate evidence, annotations, or aliases
introduced after the initial input classification.

Public derivation entry points expose only compiler-accessible final evidence signatures. Actual downstream source files
in the `adversarialBoundary` project are compiled independently with warnings as errors and future-source mode. Each
negative fixture first compiles with its marked offending block removed, so missing imports, stale identifiers, syntax,
or unrelated access errors cannot create false passes. String-based compile assertions remain supplemental.

Alternatives considered:

- Trusting every named `Powers[P]` as already canonical keeps derivation small but permits duplicate, zero, and malformed
  entries to bypass the normal-form contract.
- Continuing to rely on inaccessible implicit rules works only through deprecated compiler behavior and is not a stable
  public API.

### 8. Separate same-project products from a completed external-consumer JAR

The quantities project's own `Test` configuration uses ordinary SBT defaults: `Compile/exportJars` is false,
`Test/internalDependencyClasspath` contains the completed same-project `Compile/classes` product, and `Test/compile`
retains its normal direct dependency on main compilation. No exported-product or internal-classpath override routes the
quantities main compiler back through packaging.

One statically keyed `quantitiesExternalArtifact` task depends on the SBT-managed `Compile/packageBin`. The adversarial
project's compile and test classpaths and the real-source compiler-fixture classpath all depend on that exact task result.
The external classpaths therefore observe one completed immutable main JAR; the fixture classpath adds only external
quantities dependencies and the Dotty compiler/runtime JARs and contains zero quantities classes or test-classes
directories. There is one authoritative package task for that physical artifact.

The removed configuration made every exported-product variant and quantities' own Test classpath depend on the packaged
JAR. SBT task inspection showed the resulting cycle `quantities/Compile/fullClasspath` →
`quantities/Compile/exportedProducts` → `quantitiesMainProduct` → `Compile/packageBin` → `Compile/products` →
`Compile/compile`, allowing logically re-entered producers and consumers to touch the same mutable classes/TASTy output.
Restoring defaults removes that cycle; the explicit external task dependency ensures packaging returns before either
external consumer starts.

With each producer/consumer edge expressed in the SBT graph, root `test` uses normal aggregation. The earlier manual root
test sequence and fixture-only fork/system-property handoff are removed. No global compile serialization, retry, delay,
publication, or hard-coded target path is introduced.

## Risks / Trade-offs

- **[Generic match types remain stuck]** → Make algebraic operations and alignment explicit evidence typeclasses with
  associated outputs; use atomic automatic macros only for substitution-stable inputs and contextual final evidence for
  unresolved generic code.
- **[Generic derivation commits unstable inequality]** → Reject eager automatic derivation for unresolved method type
  parameters and require generic methods to accept and forward contextual operation evidence.
- **[Downstream proof search reaches inaccessible rules]** → Put recursive rules behind a compiler-accessible derivation
  façade and compile actual external source with warnings treated as errors.
- **[Callers name malformed normal forms]** → Validate structural power entries before granting normalization or alignment
  evidence and never fall back to treating malformed structural dimensions as opaque atoms.
- **[Transparent aliases hide unstable or reducible structure]** → Follow aliases one definition at a time in one shared
  private operation, preserve deferred-member paths for classification, and repeat exposure and validation at the final
  evidence boundary.
- **[Annotations diverge from Scala definitional equality]** → Strip `AnnotatedType` centrally before classification and
  parsing, structurally parse exposed natural magnitudes, and assert annotated/unannotated coherence across all final
  operations and runtime keys.
- **[Concrete associated outputs retain depth-dependent opacity]** → Follow exact refinements transitively to a guarded
  semantic fixed point and reparse every exposed endpoint before final factor certification.
- **[Recursive typed locals masquerade as completed stable paths]** → Track active and completed term analyses separately;
  reject active re-entry while permitting cached reuse only after a traversal completes.
- **[Mutable SBT products are observed before complete visibility]** → Keep quantities' own tests on SBT's direct
  same-project product lifecycle; give adversarial and standalone fixture consumers one explicit dependency on the
  completed main `packageBin`; do not override exported-product variants; validate repeated clean and separate-process
  module sequences.
- **[Quoted derivation is compiler-version-sensitive]** → Keep the macro narrowly scoped, retain concrete and generic
  compile fixtures, and use minimal sealed public rule carriers only if the macro cannot preserve the lexical boundary.
- **[Implicit proof search becomes slow or divergent]** → Keep normalization left-biased, keep proof rules closed and
  non-overlapping, add depth-oriented compile tests, and avoid a general-purpose generic-programming dependency.
- **[Compiler errors force unresolved return types]** → Keep diagnostics symbol-safe and omit `${A}`/`${B}` rendering from
  missing-evidence annotations so ordinary generic rejection cannot trigger `CyclicReference` or macro stack traces.
- **[Static and runtime evidence derivations become ambiguous]** → Give static derivation deterministic priority and keep
  runtime evidence local to the successful comparison branch.
- **[Exact type-level exponents exhaust compiler resources]** → Treat exhaustion as a compilation failure and test
  realistic repeated-power depths; treat a source-visible magnitude encoding as a compatibility commitment until it is
  demonstrably encapsulated.
- **[Private proof construction is accidentally widened]** → Retain lexical construction boundaries and extend the
  adversarial downstream-package compilation tests before exposing evidence-aware arithmetic.
- **[Breaking inferred result types disrupt callers]** → Migrate repository annotations and compile-time examples together,
  retain endpoint conveniences, and document explicit `asDimension` as the migration path.

## Migration Plan

1. Retain real-source regressions that reproduce late term-path specialization, guard/carrier transport, malformed
   magnitudes and factors, and unresolved-generic diagnostic failures.
2. Replace guarded recursive implicit proof search with atomic final-operation macros and require contextual final
   operation evidence in unresolved generic code.
3. Remove public recursive proof carriers and guards, recursively validate every structural normal-form claim, and
   independently validate every computed final tuple and output.
4. Recheck `DimRef`, `Quantity`, rate, grid, optional algebra, and runtime-evidence callers against the revised associated
   outputs without changing coefficients, runtime keys, grid identity, or provenance.
5. Compile actual external source with the repository warning policy, run the complete multi-module test suite and static
   checks, and run strict OpenSpec validation before declaring the change complete again.
6. Revalidate definitionally transparent annotations and replace mutable inter-project main-product consumption with the
   completed SBT-managed JAR while restoring default same-project test products before the final independent rereview.
7. Retain endpoint-depth, nested-associated-`Powers`, recursive-term, and shared-DAG compiler regressions; re-run focused
   and full validation before requesting another independent review.

No runtime data rollback is necessary. Source rollback consists of reverting the static API change and its migrated
callers as one unit; persisted runtime keys and packed records remain compatible.
