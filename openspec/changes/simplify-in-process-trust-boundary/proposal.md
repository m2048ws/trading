## Why

The repository currently uses production `MethodHandle`/`privateLookupIn` machinery and hostile-caller tests to make
domain construction difficult for code that is already executing in the same JVM. That complexity does not match the
one-maintainer threat model: supported in-process code is cooperative, while wire, persistence, configuration, venue,
and replay data must remain explicitly checked before becoming trusted domain state.

## What Changes

- Define one repository-wide trust model: well-typed Scala and ordinary Java clients using documented APIs are
  supported; deliberate same-package, reflection, unsafe-bytecode, subclassing, or constructor-bypass attacks from
  code already inside the process are non-goals.
- Remove all production and benchmark `MethodHandle`, `MethodHandles.privateLookupIn`, and reflective private-member
  invocation. Replace them with owner-local construction, narrow statically callable operations, and checked smart
  constructors where supplied data must establish an invariant.
- Preserve exact arithmetic, dimension/grid evidence, catalog lineage, instrument and execution identity, associated
  order/scenario evidence, risk monotonicity, fee attribution, lifecycle authority/order/conflict/completeness, and
  deterministic non-empty errors through types or explicit semantic validation rather than constructor secrecy.
- Keep external records untrusted: retain bounded parsing, explicit V1 dispatch, canonical encoding, coherent snapshot
  resolution, typed reconstruction/replay failures, and Java-serialization rejection.
- Remove or rewrite tests and runtime guards whose only purpose is resisting deliberately hostile same-JVM code, while
  retaining compiler boundaries, closed matching, dependency isolation, ordinary misuse tests, and semantic rejection
  tests.
- **BREAKING (unsupported JVM internals):** private constructor modifiers, synthetic access details, exact-class guards,
  and reflective construction paths are not preserved as compatibility commitments. Published V1 representations and
  documented domain-named factories retain their observable successful and failing behavior.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `repository-architecture`: Distinguish cooperative in-process callers from untrusted external data and require static
  cross-owner collaboration instead of reflection-based access control.
- `scala-functional-design`: Treat types and checked predicates—not resistance to hostile same-JVM bypass—as domain
  evidence, and constrain unavoidable casts without requiring anti-forgery tests.
- `exact-quantity-arithmetic`: Scope carrier and dimension guarantees to supported typed construction while retaining
  exactness, dimension grammar, and checked reconstruction.
- `quantity-grid-projection`: Scope grid construction guarantees to supported callers while retaining grid provenance,
  exact projection, refinement, and checked packed reconstruction.
- `runtime-quantity-identity`: Preserve witness uniqueness and checked runtime reconstruction without claiming
  resistance to deliberate visibility or JVM bypass.
- `reference-data-identity`: Preserve catalog-issued handles and lineage as semantic capabilities while retiring
  JVM-private constructor behavior as a security boundary.
- `order-scenarios`: Make activation, pricing, scenario, and round-trip construction statically callable while requiring
  every associated-evidence, identity, non-empty, and flatness predicate to be checked before stronger results return.
- `position-risk-sizing`: Preserve constructive monotonicity and exact sizing while replacing reflective observation and
  hidden construction with owner-defined static operations.
- `actual-execution-lifecycle`: Preserve command/fact authority, replay, conflict, ordering, completeness, cancellation,
  lineage, and exposure invariants through checked transitions without reflective construction.
- `versioned-boundary-codecs`: Preserve strict bounded V1 encoding/decoding and checked reconstruction while replacing
  reflective creation of codec records, diagnostics, and replay results with static construction.
- `catalog-command-replay`: Preserve publication-derived journaling and deterministic checked replay without treating
  a hidden result constructor as authority.

## Impact

- Affects production construction and observation internals in `order-model`, `execution-scenario`, `fee-policy`,
  `risk`, `execution-lifecycle`, and `boundary-codecs`, plus the risk benchmark.
- Reconciles the architecture guide, affected canonical specifications, module documentation, Java/Scala client
  fixtures, adversarial-boundary coverage, and completed-artifact inspection rules.
- Removes dynamic invocation descriptors and erased casts used only after dynamic calls; it does not add dependencies,
  effects, persistence formats, authentication, sandboxing, or runtime services.
- Requires clean module and full-repository tests, packaged Scala/Java client checks, strict reflection-source scanning,
  serialization rejection, codec/replay matrices, and representative benchmark compilation or comparison.
