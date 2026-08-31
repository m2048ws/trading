# Task Group 5 — Validated scenario fee assessment

## Canonical staged assessment

`FeeAssessment.evaluate(instrument)(scenario, policy)` is the only supported final-attribution boundary. Its result is:

```scala
Either[
  FeeAssessmentErrors[E],
  ScenarioFees[PosD, B, Q, S]
]
```

The boundary evaluates three ordered stages:

1. compare the explicit instrument with the scenario and policy runtime identities, accumulating both input failures;
2. only when that stage succeeds, evaluate the policy and retain every typed cause with its stable policy-error
   ordinal; and
3. only on policy success, validate each directive's fee identity, denomination identity, and requested index before
   selecting the scenario slice.

Foreign inputs suppress policy execution. Output validation evaluates all three independent properties for every
directive and accumulates failures in directive order, then component order: fee, denomination, slice. A single output
failure suppresses construction of the entire successful result.

Locations are closed domain values rather than strings. Input identity, directive identity, index-range, and policy
failure are separate violation products, so an index failure cannot be paired with a fee-identity component and an
input failure cannot claim a directive ordinal. `FeeAssessmentErrors[E]` is a public non-empty ordered head/tail value;
`mapPolicyCause` changes only the policy-owned cause and preserves every domain location and assessment failure.

## Scenario-owned attribution

`AssessedFee` existentially retains the calculated fee dimension together with:

- the original refined `SliceIndex`; and
- the exact `LiquiditySlice` selected from the supplied scenario at that index.

The selected slice already owns its immutable market state. There is no separate market field, scenario token,
caller-supplied attribution path, or later reference-equality reconciliation. Equal-looking duplicate slices remain
unambiguous because indices zero and one retain the corresponding original slice references.

`ScenarioFees` stores one scenario and an immutable assessed-fee vector. `instrumentId` is projected from that scenario
instead of duplicated in constructor state. Both `ScenarioFees` and the concrete assessed-fee representation have
JVM-private constructors; `FeeAssessment` invokes them through cached private method handles only after all validation
succeeds. Completed-JAR Scala and reflection guards prevent supported or Java-level caller construction of final
attribution.

## Transitional integration cleanup

The provisional `FeeOrchestration` bridge now calls canonical assessment for both round-trip legs and converts each fee
through `assessed.sourceSlice.market`. `FeeLine`, raw `Int` attribution, caller-selected `sourceMarket`, and the old
closed `FeePolicyError` hierarchy are absent. The remaining provisional integration errors are covariant in the policy
cause and retain `FeeAssessmentErrors[E]` without exception or string erasure. Later Task Groups replace the price/PnL
bridge itself.

## Verification

| Check | Result |
| --- | --- |
| `sbt -batch scalafmtCheckAll scalafmtSbtCheck` | pass |
| `sbt -batch feePolicy/test` | 31 passed, including 7 focused assessment tests |
| `sbt -batch adversarialBoundary/test` | 155 passed, including 42 economics/fee-policy completed-JAR tests |
| Public API/package audit | assessment types present; final constructors JVM-private; old line/universal errors absent |
| Source attribution audit | no production `sourceMarket` or `.eq` reconciliation; conversion projects the selected slice market |

The only runtime warning is Scala's upstream `sun.misc.Unsafe` terminal-deprecation warning on OpenJDK 26.0.2.

## Automated Task Group review

The review loop covered Task Group 5 scope, RFC AC-015 alignment, staged identity/evaluation/output suppression,
independent stable accumulation, typed-cause provenance, honest location/violation products, scenario ownership,
JVM construction authority, migration completeness, dependency purity, linear complexity, security applicability,
evidence, and checkpoint integrity. Source, bytecode, completed-JAR, compiler-negative, and test evidence agree with the
implementation, and the final pass found no findings. It changed no file during that pass, required no human triage,
and is neither canonical Verify nor Human Review.
