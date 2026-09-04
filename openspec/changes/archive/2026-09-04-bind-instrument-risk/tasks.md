## 1. Bind the instrument-specific risk surface

- [ ] 1.1 Add the final field-only `Risk.InstrumentScope`, exact local aliases, six one-to-one delegates, and singleton-
  preserving `Risk.forInstrument` factory; verify `risk/Test/compile` succeeds and direct entry points, existential
  `single`/table inputs, model combinators, and primary sizing remain unchanged.
- [ ] 1.2 Add focused scope characterization covering every delegate's exact success, complete deterministic failures,
  runtime foreign identity/dimension/grid/coordinate/coverage/monotonicity rejection, construction costs, exhaustive
  coordinates and located causes, independent allocation, interleaving, and concurrent reuse; verify the focused risk
  suites pass.
- [ ] 1.3 Add completed-JAR positive and independently valid negative Scala fixtures for concise aliases, exact models,
  `instrument.Lots => Either[E, instrument.Pnl]`, decisions, and incompatible PnL/loss/segment/budget/evaluator inputs;
  verify `RiskCompilerBoundarySuite` compiles/runs the positive client, rejects only the marked negative calls, and
  retains its pure dependency/JAR boundary assertions.
- [ ] 1.4 Verify scoped models still compose and use model-bound maximum-affordable selection with unchanged cost and
  logarithmic bounds, exhaustive sizing retains explicit `O(cap)` observations, production risk gains no forbidden
  imports or dependencies, formatting is clean, and the clean JDK-25 aggregate test plus benchmark compilation matrix
  passes.
- [ ] 1.5 Run the automated Task Group review against the complete staged diff, remediate every actionable finding,
  rerun affected checks, and retain a ready verdict for the exact reviewed tree before the group's dedicated commit.
