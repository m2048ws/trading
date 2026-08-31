# Task Group 9 — Verification evidence and Corgi handoff

## Clean build and repository matrix

Both formatting checks pass. A clean compilation then completed explicitly in dependency order: quantities, reference
data, application, runtime, instrument economics, risk, order model, execution scenario, fee policy, and the
adversarial test boundary. Every production artifact compiled under the configured JDK 25 release target on OpenJDK
26.0.2, SBT 1.12.15, and Scala 3.8.4.

The full root matrix passes 913 tests:

| Project | Tests |
| --- | ---: |
| quantities | 601 |
| reference data | 13 |
| application | 9 |
| runtime | 18 |
| instrument economics | 13 |
| risk | 40 |
| order model | 7 |
| execution scenario | 16 |
| fee policy and downstream risk integration | 38 |
| completed-JAR/compiler/adversarial boundary | 158 |

This covers exact/refinement/unit/property/law suites, linear/inverse/quanto scenario normalization, fee-policy laws,
typed policy and attribution failures, different leg policies, selected-slice conversions, fee-inclusive PnL, pure-risk
dependents, negative Scala and Java compilation, JVM construction authority, completed-product classpaths, and the
repository aggregate. The non-published JMH project also compiles explicitly; Group 9 makes no new performance claim.

The only runtime warning is Scala's upstream `sun.misc.Unsafe` terminal-deprecation warning on OpenJDK 26.0.2.

## Packaged API and source audit

The completed fee-policy JAR contains 132 entries and 90 classes under its canonical `trading.fee` ownership. It has no
`trading/fee/policy/` path, retired `FeeSchedule`, `FeeLine`, `FeePolicyError`, or `FeeOrchestration`, and no risk,
application, or runtime class. Public `javap` output confirms that `FeeDirective` exposes only a core fee and refined
slice index, `FeePolicy` is an ordinary typed `Either` strategy, and assessment/PnL operations accept explicit pure
domain inputs. The completed-JAR suite separately verifies the exact dependency classpath and bytecode concerns.

Production import and source scans find no reverse risk/application/runtime dependency, `F[_]`, effect/stream library,
live catalog, arbitrary source market, object-identity reconciliation, global policy `Monoid`, execution report, audit
envelope, or old capability name. `Rational` is confined to the nominal `FeeRate` representation and exact typed
quantity scaling/sign operations; assessment and fee-inclusive PnL contain no raw-scalar kernel, coefficient fold, or
average-price shortcut.

## Planning and source integrity

`openspec validate introduce-pure-fee-policy-module --strict --no-interactive` passes. Deterministic
`corgispec ready introduce-pure-fee-policy-module --strict --json` reports `ready` at planning revision
`sha256:b2226bdaf8f713cf48bb49f599da6b5cd84462cb96d8f028a6263caed76c4158`. All eleven checks pass: artifact
completeness, strict OpenSpec validation, task configuration/uniqueness/structure, placeholder and open-question
absence, capability/spec parity, RFC contract binding, AC traceability, and current source provenance.

The read-only cross-artifact semantic review found no errors, warnings, or informational drift across the proposal,
design, both delta specs, and all nine Task Groups. Goals/non-goals, normative behavior, success/failure/boundary
scenarios, design decisions, migration, exclusions, task mapping, and execution order remain aligned. The readiness
skill changed no file.

## Handoff boundary

This Task Group prepares the final Apply checkpoint only. After its dedicated commit is acknowledged, Apply stops at
`awaiting_verify`. Canonical whole-change Verify, explicit Human Review, Human QA when applicable, and Archive remain
separate gates. This delivery does not begin the boundary-codec Slice or introduce any codec/application/runtime fee
capability.

## Automated Task Group review

The review loop covered exact Run/Group identity, AC-013 through AC-016 evidence, clean build/test/JMH results, package
and public-signature audits, source/dependency purity, strict deterministic and semantic readiness, architecture,
performance/security applicability, checkpoint integrity, and gate separation. It returned no findings, changed no
file during the review, required no human triage, and is neither canonical whole-change Verify nor Human Review.
