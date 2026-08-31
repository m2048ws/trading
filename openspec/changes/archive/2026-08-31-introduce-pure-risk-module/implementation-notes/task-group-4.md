# Task Group 4 — Lot-risk assessment and validated model boundary

## Checked assessment

`LotRiskAssessment[D, S]` retains one positive instrument-bound `Lots[D]`, exact
`NonNegative[Quantity[S]]` downside, and the retained position/settlement dimension witnesses. Its only public creation
path is `LotRiskAssessment.fromPnl(instrument)(lots, pnl)`:

- it validates the lots' ordinary runtime instrument identity;
- it invokes `Risk.downside`, so PnL identity is checked before net-PnL inspection;
- it derives the downside rather than accepting a separate risk assertion; and
- it reports the closed `Lots` or `Pnl` input location in `AssessmentInstrumentMismatch`.

The representation has explicit value equality/hashing, is final, rejects Java serialization, and has a JVM-private
constructor reached only through a cached private method handle owned by its companion.

## Validated monotone capability

`MonotoneLotRisk[D, S]` captures one `InstrumentId`, position and settlement dimension witnesses, a refined positive
cap, and a private total evaluator over its certified `1..cap` domain. Its evaluator and constructor are unavailable to
public callers. The initial `single` constructor admits only an already checked assessment at coordinate one, yielding
a non-empty domain whose totality and monotonicity are trivial. Group 5 extends the same private companion-owned
construction channel with closed exact curve constructors; no arbitrary function channel exists.

The model is final, rejects Java serialization, and has a JVM-private constructor. A cast from an arbitrary function
fails at runtime and no public boolean, marker subtype, type-class instance, proof token, or callback is accepted as a
monotonicity certificate.

## Domain-owned non-empty validation

`ModelViolation[S]` has focused variants for instrument identity, position/settlement dimension, empty domain,
out-of-domain, duplicate and missing coordinates, breakpoint order, negative marginal loss, downward boundaries, and
incompatible composition. Quantity-bearing violations retain their typed settlement quantity.

`ModelViolations[S]` is a final non-empty public observation with a JVM-private constructor. Constructors accumulate
independent checks internally with Cats `ValidatedNec` in explicit source order, convert the `NonEmptyChain` at the
public boundary, and publish either all failures or one complete model—never a partially validated capability.

The single-coordinate negative fixture independently violates identity, settlement dimension, missing coordinate one,
and out-of-domain coordinate two. Two runs return the same four violations in that stable order. Position dimension is
coherent in the fixture and correctly contributes no spurious error.

## Construction-authority evidence

- Same-package Scala cannot call assessment/model/non-empty-error constructors, copy an assessment, or instantiate a
  model subclass.
- Runtime reflection confirms `LotRiskAssessment`, `ModelViolations`, and `MonotoneLotRisk` are final and every JVM
  constructor is private.
- A risk-only completed-JAR client still compiles and runs; the artifact remains free of downstream/effect references.
- Focused gate: 9 risk tests and 7 completed-JAR/compiler tests pass; the transitional downstream project compiles.

The first compiler pass exposed that ordinary Scala `private` constructors were JVM-public. The implementation adopted
the repository's proven `private[this]` plus cached private-method-handle bridge and retained the strong bytecode test.
