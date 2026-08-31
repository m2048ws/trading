# Task Group 4 — Pure policy strategy and composition

## Public policy algebra

The `trading.fee` root now owns the open, pure strategy boundary:

```scala
trait FeePolicy[+E, PosD <: Dim, B <: Dim, Q <: Dim, S <: Dim]:
  def instrumentId: InstrumentId
  def evaluate(
    scenario: OrderScenario[PosD, B, Q, MarketState[B, Q, S]]
  ): Either[PolicyErrors[E], Vector[FeeDirective]]
```

`E` is covariant and remains owned by each policy implementation. `mapError` converts a policy-specific cause into a
caller-owned sum ADT, while `widen` performs the lawful covariant widening. The public result uses only `Either`, the
domain-owned `PolicyErrors`, and immutable `Vector`; no validation-library collection, `F[_]`, interpreter, exception,
or string conversion appears in the strategy contract.

`PolicyErrors[E]` is a final non-empty head/tail value with ordered `toVector`, `map`, and `concat` operations plus a
typed empty reconstruction failure. It has structural equality and hashing. `SliceIndex` is a final nominal
nonnegative coordinate with typed checked construction, and `FeeDirective` existentially retains its calculated core
fee dimension plus only that requested coordinate. It has no market, slice, scenario, token, or attribution-path field.

The first implementation used opaque representations for `PolicyErrors` and `SliceIndex`. Packaged API inspection
showed that those erased to `Tuple2` and `int` in Java signatures. Both are now honest JVM values whose constructor
bodies enforce the complete non-empty/nonnegative invariants even if Java bypasses the Scala-private factory. A
directive implementation also rejects null fee/index inputs internally, so unsupported JVM construction cannot retain
an invalid value.

## Contextual composition

`FeePolicy.noFees(instrument)` is the total empty policy for that exact instrument context.
`FeePolicy.combine(instrument)(components)`:

1. inspects every component identity and accumulates all `ForeignPolicyInstrument` failures in component order;
2. removes no-fee components and flattens nested checked composites into one component vector;
3. evaluates every admitted component, accumulating every policy error in component/error order; and
4. returns successful directives only when no component failed, concatenated in component/directive order.

The implementation uses local vector builders so evaluation is linear in component errors plus directives. The
observable operation is associative and has no-fee as both identities only inside the checked fixed-instrument
context. No unconditional Cats `Monoid[FeePolicy]` is published.

Tests map two distinct policy error types into a caller-owned sum and retain both exact causes. A generated contextual
law covers every success/failure combination for three policies under left/right regrouping and both no-fee identities.
Additional examples cover empty composition, stable directive order, multi-error accumulation, and multiple foreign
components.

## Transitional migration and packaged boundary

The old `trading.fee.policy.FeeSchedule` and instrument-bound `FeePolicy` class are absent from source and the completed
JAR. The still-provisional integration service is explicitly named `FeeOrchestration`; it now consumes the new policy
directives and selects the actual scenario market internally for existing PnL/risk regression paths. Group 5 replaces
that bridge with the canonical generic assessment types and removes `FeeLine` plus the universal error surface.

The completed-JAR positive client implements and evaluates the new strategy. A negative client independently rejects:

- the removed `FeeSchedule` path;
- an unconditional `Monoid[FeePolicy]` summon;
- a higher-kinded effect argument in the typed-error slot;
- assignment of custom errors to `Throwable` or `String` results; and
- a nonexistent directive `sourceMarket` field.

Packaged class inspection requires the public `PolicyErrors`, `SliceIndex`, `FeeDirective`, and `FeePolicy` classes,
rejects the removed schedule/service class names, and verifies the public policy classes contain no Cats data/monoid,
effect, stream, or clock references.

## Verification

| Check | Result |
| --- | --- |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck` | pass |
| `sbt -batch feePolicy/test` | 24 passed: 11 integration, 6 fee mathematics, and 7 strategy/composition tests |
| `sbt -batch adversarialBoundary/test` | 153 passed, including 40 economics/fee-policy completed-JAR tests |
| Public API/package audit | root policy types present; old schedule/service types absent; no effect/validation/monoid leakage |

The only runtime warning is Scala's upstream `sun.misc.Unsafe` terminal-deprecation warning on OpenJDK 26.0.2.

## Automated Task Group review

The review loop covered Task Group 4 scope, RFC acceptance alignment, type/refinement ownership, composition laws,
error provenance, completed-JAR boundaries, forbidden dependencies, JVM representation, complexity, evidence, and
checkpoint integrity. Its first pass found two implementation-hardening issues:

- Scala-private implementation constructors were JVM-visible without independently rechecking every retained
  no-fee/composite/mapped invariant; and
- mapping a checked composite's error type introduced an avoidable wrapper instead of retaining the normalized flat
  composite structure.

The constructors now reject nulls and malformed composite state at their bodies, including size, identity, and member
checks, and `mapError` maps normalized components while preserving a flat composite. The full verification table above
passed again after those changes. A final source, bytecode, boundary, and evidence pass found no remaining findings;
there was no human triage or filesystem change during that final pass. This review is neither canonical Verify nor
Human Review.
