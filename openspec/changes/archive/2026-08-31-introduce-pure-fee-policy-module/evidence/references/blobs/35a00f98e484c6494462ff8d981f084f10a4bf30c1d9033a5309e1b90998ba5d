# Task Group 3 — Refined fee mathematics

## Public ownership and exact formulas

The fee-policy artifact now owns the public `trading.fee` mathematics:

| Concept | Representation and operation |
| --- | --- |
| Quoted policy rate | Opaque nominal `FeeRate` over exact `Rational`; positive means charge and negative means rebate. |
| Percentage contribution | `FeeCalculation.percentage(NonNegative[Quantity[D]], FeeRate): Quantity[D]`, implemented by scaling the typed basis by the negated exact rate. |
| Minimum adjustment | `FeeCalculation.minimumCharge(Quantity[D], NonNegative[Quantity[D]]): Quantity[D]`, total for refined input and dimension preserving. |

Percentage calculation does not project the basis coefficient, rediscover its sign, or reconstruct a typed quantity.
Minimum adjustment projects coefficients only to compare sign and magnitude. It returns the original typed contribution
for rebates, zero, equal/larger charges, and a zero minimum; only a smaller-magnitude charge returns the typed minimum
scaled by exact negative one.

`FeeRate` is opaque rather than a JVM-instantiable wrapper. Its checked public constructor rejects null, Scala callers
cannot substitute a raw `Rational`, and no generated `copy` or Java-visible value constructor can bypass that boundary.
An initial private-class representation was rejected during the API audit because `javap` showed Scala's private
constructor as JVM-public.

## Core quantization boundary

The provisional instrument-bound policy helper now accepts an existing `NonNegative[Quantity[D]]`, delegates to the
total percentage formula, and passes that exact typed contribution directly to core `Fee.create`. Core
`FeeDenomination` remains the sole owner of grid selection, quantization policy, signed grid amount, and residual
conservation. The provisional `InvalidFeeBasis` error and repeated raw-sign branches are removed.

A focused control uses two exact USD `-3/500` components on a one-cent toward-zero denomination. Each component crosses
the core boundary separately and retains amount zero plus residual `-3/500`; incorrectly aggregating first would
produce a `-1/100` grid amount. This demonstrates why policy components cannot be combined before core quantization.
Search and packaged-JAR audits confirm percentage/minimum policy remains absent from quantities, reference data,
instrument economics, order model, execution scenario, and risk. Lower-layer ownership guards now reject the full
`trading.fee` root instead of only the provisional `trading.fee.policy` subpackage.

## Tests and boundary evidence

| Check | Result |
| --- | --- |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck` | pass |
| `sbt -batch feePolicy/test` | 17 passed: 11 retained integration tests and 6 focused example/property tests |
| `sbt -batch adversarialBoundary/test` | 152 passed, including 39 completed economics/fee-policy compiler and JAR tests |
| Completed fee-policy positive client | compiles and executes using refined basis plus nominal rate |
| Refined-input negative client | raw `Quantity` basis and raw `Rational` rate are independently rejected by the completed-JAR Scala compiler boundary |
| Packaged ownership | fee-policy TASTy/classes are present only in the fee-policy JAR; lower-layer and risk JAR guards reject every `trading.fee` prefix |

Generated properties cover arbitrary exact signed rates, nonnegative bases, signed contributions, and nonnegative
minimums. Examples cover positive charge, negative rebate, zero rate, negative-basis refinement failure, every minimum
boundary, dimension preservation, and exact amount-plus-residual conservation. The only runtime warning is Scala's
upstream `sun.misc.Unsafe` terminal-deprecation warning on OpenJDK 26.0.2.

## Automated Task Group review

The first read-only review pass found one evidence-quality gap: the negative completed-JAR fixture produced two errors,
but its test explicitly identified only the `NonNegative` diagnostic. The test now also requires the nominal `FeeRate`
diagnostic, and the 39-test economics/compiler suite passes after that remediation.

The final automated review checked Task Group scope, formula/sign correctness, refinement and nominality, typed dimension
preservation, core quantization ownership, one-way packaged dependencies, retained client behavior, deterministic tests,
constant-time pure operations, security applicability, evidence claims, and checkpoint integrity with no findings. It
changed no file during the final pass, human-triaged no finding, and is neither canonical Verify nor Human Review.
