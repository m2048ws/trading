# RFC-0004-simplify-in-process-trust-boundary: Simplify the in-process trust boundary

## Goal

Make the repository easier for one maintainer to understand, refactor, and operate by aligning its implementation with
its actual threat model: supported code running inside the application is cooperative and trusted, while data arriving
from wire, persistence, configuration, venue, or other external boundaries is untrusted until checked.

The delivery removes production reflection and method-handle machinery used to hide constructors, methods, or fields
from deliberately hostile same-package Scala, Java, or handcrafted JVM callers. It retains protections that prevent
ordinary programming mistakes: exact and dimension-safe types, refinements, closed domain alternatives, smart
constructors, typed validation, coherent catalog lineage, checked state transitions, bounded decoding, and explicit
versioned persistence formats.

## Non-goals

- Do not make malformed or adversarial external data trusted. Parsing, decoding, replay, catalog resolution, and
  application/runtime ingress remain checked boundaries with bounded resources and typed failures.
- Do not weaken exact arithmetic, dimensions, grids, refinements, instrument identity, catalog lineage, source-qualified
  execution identity, authoritative ordering, or event-replay semantics.
- Do not add cryptographic authentication, authorization, signing, encryption, sandboxing, a plugin-security boundary,
  or protection against malicious code already executing in the application process.
- Do not promise resistance to casts, reflection, `Unsafe`, instrumentation, custom bytecode, deliberate same-package
  source, constructor-bypassing deserialization, or intentional misuse of internal APIs.
- Do not make every internal representation publicly constructible. Use the narrowest ordinary Scala visibility that
  keeps ownership readable, without dynamic access to private members.
- Do not change published JSON shapes, schema versions, canonical encodings, economic calculations, lifecycle results,
  or public domain-named factory behavior merely to perform the simplification.
- Do not prohibit reflection in tests that inspect completed artifacts or load fixtures. Test-only introspection is not
  part of the shipped application and may remain when it verifies a useful accidental-misuse or dependency boundary.

## Boundary

The supported caller is well-typed Scala or ordinary Java application code that uses the documented API without trying
to defeat its visibility. The repository does not treat another class in the same JVM as a hostile security principal.
If mutually untrusted code is introduced later, isolation must be designed at a process, module, class-loader, or
sandbox boundary in a separate RFC rather than simulated through domain-object constructor tricks.

Production code in order model, execution scenario, risk, fee policy, boundary codecs, execution lifecycle, and
benchmarks SHALL contain no `java.lang.invoke.MethodHandle`, `MethodHandles.privateLookupIn`, reflective constructor,
private-method, or private-field access. Cached constructor descriptors and the casts required only to recover erased
types after dynamic invocation SHALL be removed. Cross-owner behavior SHALL instead move to the owning value or be
exposed as a narrow, statically callable, checked operation.

JVM-private construction SHALL no longer be treated as a domain proof. Values with purely local invariants MAY use
ordinary constructors when their field types make invalid states unrepresentable. Values requiring validation SHALL
retain domain-named smart constructors and typed expected failures. Essential defensive checks MAY remain in ordinary
constructors when erased Java inputs or accidental internal misuse could otherwise create a malformed instance, but
exceptions SHALL not replace the supported typed-failure path.

Evidence, assessment, replay, and transition values SHALL NOT become authoritative merely because their representation
was difficult to instantiate. Any operation that turns supplied data into a stronger trusted result SHALL establish
the required predicate itself or consume evidence whose authority still follows from a retained semantic capability.
For example, trigger verification must establish both instruction/evidence agreement and satisfaction of the trigger;
catalog handles must still originate from and reconcile against coherent lineage; execution replay must still check
identity, ordering, conflict, and completeness rules.

Closed Scala ADTs, associated evidence types, path-dependent dimensions, refinements, non-empty structures, exact
identities, and deterministic validation remain the normal accidental-error defenses. Runtime exact-class guards and
completed-JAR tests whose only purpose is rejecting deliberate Java subclassing, same-package spoofing, or direct
constructor invocation SHALL be removed or rewritten around observable semantic failure. Negative compiler tests that
prove ordinary type errors, illegal dimension/grid combinations, impossible ADT compositions, or dependency leaks
remain in scope.

External records remain data rather than authority. Boundary codecs SHALL continue strict bounded parsing, explicit
schema-version dispatch, exact primitive decoding, snapshot-coherent resolution, and reconstruction through owning
domain checks. Java object serialization SHALL continue to fail closed where it could accidentally establish an
unstable persistence contract. Reference-data lineage and other generative identities that prevent accidental mixing
remain semantic capabilities even though this RFC removes hostile-caller claims.

This is a pre-release source and JVM compatibility change. Existing domain-named Scala entry points and observable
successful/failing semantics SHOULD remain stable where they do not encode the retired threat model. JVM constructor
modifiers, synthetic access details, and unsupported Java construction paths are not compatibility commitments.
Normative specifications, architecture guidance, examples, and tests SHALL be reconciled so they describe one
consistent trusted-code/untrusted-data boundary.

## Slices

### S-01-simplify-in-process-trust-boundary: Remove hostile-JVM construction machinery

- AC-001 [evidence: both]: Repository architecture and affected capability specifications define supported in-process
  callers as cooperative, external data as untrusted, and deliberate same-JVM forgery or visibility bypass as a
  non-goal; human review confirms that the resulting public APIs remain domain-readable and proportionate to a
  one-maintainer project.
- AC-002 [evidence: automated]: No production or benchmark source imports or invokes `MethodHandle`, `MethodHandles`,
  `privateLookupIn`, reflective constructors, or reflective private-member access; construction and cross-owner calls
  are statically expressed without new public unchecked helpers or erased casts that manufacture authority.
- AC-003 [evidence: automated]: Focused behavioral and property tests prove that every invariant formerly dependent on
  hidden construction is either expressed by field/refinement types, checked by its supported smart constructor, or
  re-established before a stronger evidence, assessment, scenario, lifecycle, replay, fee, risk, or codec result is
  returned.
- AC-004 [evidence: automated]: Quantity dimension/grid safety, reference-data lineage, instrument identity,
  order/scenario evidence matching, risk monotonicity, fee attribution, execution identity/order/conflict/completeness,
  strict bounded decoding, canonical versioned encoding, and Java-serialization rejection retain their existing
  observable semantics under the clean full-repository test and completed-artifact matrix.
- AC-005 [evidence: automated]: Runtime guards and adversarial fixtures aimed only at foreign JVM subclassing,
  same-package spoofing, bytecode-private constructor modifiers, or deliberate direct construction are removed or
  replaced by tests for supported type errors and semantic rejection; module dependencies and closed Scala matching
  remain unchanged.
- AC-006 [evidence: both]: Representative Scala and ordinary Java clients use the supported factories, transitions, and
  codecs without reflective setup, while an external-data walkthrough demonstrates that malformed records and
  incoherent identities still fail at explicit checked boundaries without changing any V1 wire representation.

## Risks

- Some values currently rely on constructor secrecy for a predicate that is not checked again when the value is
  consumed. Mitigation: inventory every dynamic construction site and move or repeat each invariant before removing its
  protection; add a regression test that starts from the least-trusted statically representable input.
- A mechanical visibility rewrite could expose unchecked helpers or make erased Java calls fail with casts or ordinary
  exceptions. Mitigation: prefer typed public factories, narrow static internals, defensive erased-input validation, and
  completed-artifact Scala/Java client tests.
- Removing hostile-JVM tests may accidentally remove valuable type, dependency, or external-data coverage with them.
  Mitigation: classify each test by threat, retain ordinary misuse and boundary tests, and replace constructor-modifier
  assertions with behavioral assertions before deletion.
- The change spans several delivered modules and can produce difficult intermediate merges. Mitigation: implement in
  dependency order with one buildable Task Group per owning module, preserving public behavior at each commit.
- Constructor and access simplification may change allocation or hot-path behavior. Mitigation: retain representative
  risk, replay, codec, and lifecycle benchmarks and compare operation/allocation behavior where the existing delivery
  made a performance claim.
- Future contributors may mistake trusted in-process code for trusted input data. Mitigation: state the distinction in
  the architecture guide and keep parsing, persistence, venue, configuration, and replay boundaries explicitly checked.
