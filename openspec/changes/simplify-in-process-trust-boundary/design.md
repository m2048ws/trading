## Context

See `proposal.md` for motivation. The current tree contains method-handle construction or observation in six production
artifacts plus benchmarks: order model, execution scenario, fee policy, risk, actual execution, and boundary codecs.
Most sites compensate for `private[this]` constructors or methods that their own companions and collaborating owners
cannot call statically. Actual execution contains the largest cluster because nearly every identity, fact, transition,
diagnostic, and derived result was made dynamically constructible while remaining JVM-private.

The repository already has the semantic machinery that must survive: refined fields, path-dependent dimensions,
stable identities and lineage, associated order evidence, non-empty collections, exact risk formulae, immutable
execution transitions, explicit replay classifications, bounded versioned decoding, and serialization rejection. The
design changes how trusted in-process owners collaborate; it does not change external data into trusted input or alter
the V1 representations.

## Goals / Non-Goals

**Goals:**

- Make every production construction and observation path statically visible to the compiler and ordinary code review.
- Give each invariant one clear owner: field/refinement type, checked smart constructor, or strengthening consumer.
- Remove runtime exact-class guards and hostile-JVM fixtures that protect no supported boundary.
- Preserve module direction, closed Scala alternatives, typed failures, deterministic validation, exact semantics,
  replay behavior, and representative performance.
- Leave common Scala and ordinary Java use domain-named and free of reflection setup.

**Non-Goals:**

- Redesign the domain models, publish new wire/schema versions, or add persistence/runtime infrastructure.
- Make all internal constructors public or add generic unchecked reconstruction helpers.
- Eliminate casts that are justified by a checked runtime equality or an existential/path-dependent relationship that
  Scala cannot otherwise express.
- Defend against malicious code already executing with same-process reflection, bytecode, instrumentation, or unsafe
  access.

## Decisions

### 1. Classify every dynamic access site by invariant owner before rewriting it

Each method-handle site will be assigned exactly one of these forms:

1. **Field-valid product:** all invalid states are excluded by closed/refined field types. Use ordinary direct
   construction, normally through its companion for naming and Java ergonomics.
2. **Checked value:** raw or erased inputs require validation. Keep a domain-named `Either`/validated factory and make
   direct construction owner-local; defensively check a JVM-callable path when accidental erased misuse could otherwise
   return a malformed value.
3. **Derived evidence or state:** authority follows from a predicate over supplied values. Place construction behind the
   operation that establishes that predicate, or re-establish the predicate at the consumer before returning a stronger
   result.
4. **Observation of an already valid value:** expose the narrow owner-defined static/member observer needed by the
   algorithm. Do not dynamically recover erased types; retain only casts justified by an immediately preceding checked
   identity/equality relation.

This inventory is recorded in implementation notes as each module is changed. A mechanical `private[this]` to public
rewrite was rejected because it would obscure which values still need checked construction. Keeping method handles but
documenting them was rejected because the dynamic invocation and erased recovery casts are the complexity being
removed.

### 2. Use companion access and narrow package collaboration, not reflection

When a companion is the semantic owner, a non-`private[this]` constructor lets it invoke `new` directly while the
documented factory remains unchanged. When another type in the same responsibility owns the strengthening operation,
construction moves to the result companion as a narrow owning-package operation such as `private[trading]`, or to a member method on
the source value. Cross-module construction remains through documented public factories.

No new `Any`, raw `Object`, varargs, generic reflection replacement, or public `unsafe`/`unchecked` helper is allowed.
The final source scan covers production and benchmark imports and invocations; a focused API review covers newly
visible methods and erased casts.

Alternative: duplicate private constructors in each collaborator. Rejected because it splits ownership and validation.
Alternative: introduce a universal internal access token. Rejected because it retains issuance ceremony without a real
security principal.

### 3. Keep semantic strengthening checks at the return boundary

Order trigger evidence continues to prove both instruction/evidence agreement and trigger satisfaction; peg resolution
continues to prove instruction agreement. Scenario construction validates associated shapes, non-empty slices,
instrument/order identity, prices, and round-trip flatness. Risk model factories establish closed-form or complete-table
monotonicity, and composition checks instrument/dimension/cap compatibility. Fee construction and attribution retain
instrument, denomination, leg, and market-state checks.

Actual-execution construction becomes direct only inside its owning operations. Command/fact ingestion and replay still
qualify source/account identities, classify duplicates and conflicts, enforce authoritative ordering, retain unresolved
references and uncertainty, derive completeness honestly, and calculate exact exposure. Codec and catalog replay
records remain data; decoding/replay invokes the same identifier, refinement, snapshot, aggregate, revision, and
publication checks before returning trusted values.

Alternative: trust every internally constructed evidence value because in-process callers are cooperative. Rejected
because accidental mismatches and malformed external data remain in scope, and consumers already have enough semantic
information to check them.

### 4. Remove hostile-only runtime guards while preserving closed models

Exact-runtime-class checks whose only effect is rejecting foreign Java subclasses or handcrafted JVM alternatives are
removed. Scala enums, sealed ADTs, final cases, associated types, and exhaustive matches remain the domain model. A
runtime check remains only if it validates ordinary erased input or represents a real domain predicate independent of
who constructed the object.

Alternative: keep exact-class guards as cheap defensive programming. Rejected because they conflate implementation
class identity with domain validity, complicate extension-independent reasoning, and target deliberate misuse that the
accepted RFC excludes.

### 5. Separate obsolete adversarial tests from retained boundary tests

Every affected fixture is classified:

- remove tests that assert private bytecode constructors, exact implementation classes, same-package spoof resistance,
  deliberate direct construction failure, or hostile subclass rejection;
- rewrite tests around observable semantic rejection when the fixture also covers a real invariant;
- retain downstream compiler failures for dimensions, grids, refinements, associated evidence, closed combinations,
  and dependency leaks;
- retain checked raw/Java inputs, malformed external records, coherent-snapshot reconstruction, serialization failure,
  schema/golden/canonical laws, and completed-JAR Scala/Java positive clients.

A repository scan is added so production and benchmark method-handle/private-reflection use cannot return. Test-only
introspection may remain only where it inspects a completed artifact or fixture for a retained type, dependency, or
serialization contract.

### 6. Migrate in dependency order with one buildable owner checkpoint

The implementation order is documentation/guard baseline, order model, execution scenario, fee policy, risk and its
benchmark, actual execution, boundary codecs/catalog replay, then adversarial/integration reconciliation. Each owner
checkpoint compiles and runs its focused dependents before its dedicated Corgi Task Group commit. Actual execution is
kept separate because of its size and semantic density; boundary codecs follow the domain owners whose factories they
consume.

No new library or effect is introduced. Direct calls should remove lookup and invocation overhead; risk and replay
benchmarks or operation-count evidence will confirm that no claimed hot path regresses materially.

## Risks / Trade-offs

- **A constructor was secretly carrying an unchecked predicate.** → Classify every site before editing and add a test
  starting from the least-trusted statically representable input at the strengthening boundary.
- **Broader visibility becomes an accidental supported API.** → Prefer companion-private or narrow package visibility,
  document only domain-named factories, inspect packaged Scala/Java calls, and treat internal JVM details as explicitly
  unsupported.
- **Removing a hostile fixture also removes useful coverage.** → Classify fixtures individually and establish equivalent
  semantic, compiler, dependency, or external-data coverage before deletion.
- **Actual-execution changes are too broad for review.** → Keep one owner Task Group but split work and evidence by
  identity/commands, facts/transitions, and derived replay/exposure sections in its implementation note and test matrix.
- **A retained cast becomes detached from its proof.** → Require each retained authority-affecting cast to sit behind
  the checked equality or closed existential relation that justifies it; reject casts introduced only to emulate dynamic
  invocation.
- **Direct access changes allocation or hot-path behavior.** → Compile JMH, compare representative risk and replay paths,
  and retain existing operation-count bounds; direct invocation is expected to be neutral or cheaper.
- **Future maintainers confuse trusted code with trusted data.** → Reconcile the architecture guide and capability specs
  and keep explicit checked ingress examples for wire, persistence, venue, configuration, and replay.

## Migration Plan

1. Record the dynamic-access and hostile-test inventory, add the repository source guard, and update normative trust
   documentation without changing runtime behavior.
2. Replace dynamic construction owner by owner in dependency order, preserving each documented factory and running
   focused module/dependent tests at every checkpoint.
3. Rework actual execution and then boundary codecs/catalog replay against the now-static upstream APIs; retain all
   transition, replay, canonicalization, bounds, and error-order matrices.
4. Reconcile adversarial and completed-artifact fixtures, run clean full-repository checks and representative
   benchmarks, and walk through ordinary Scala/Java construction plus malformed external V1 reconstruction.

Rollback is by reverting the current Task Group commit before it is acknowledged. Because each earlier group remains
buildable and no wire/data migration occurs, rollback requires no state conversion. A semantic failure that cannot be
fixed within the accepted trust boundary requires an RFC Amendment rather than restoring hostile-JVM claims silently.
