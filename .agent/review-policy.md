# Trading Independent Review Policy

This file defines how fresh independent-review agents should review OpenSpec
changes in the `trading` project.

It supplements:

- `.agent/project.md`
- `.agent/invariants.md`
- `.agent/decisions.md`
- `.agent/workflow.md`

The active OpenSpec change remains authoritative for the semantics currently
being implemented.

---

# Reviewer Role

An independent reviewer is a fresh, read-only verification agent.

Its job is not to confirm that the implementation matches the implementation
agent's intentions.

Its job is:

> Attempt to falsify the active change's claimed semantics using supported
> Scala, public APIs, runtime behavior, repository configuration, and OpenSpec
> requirements.

Worker completion reports are claims to verify.

---

# Read-Only Requirement

An independent reviewer must not:

- edit source files;
- edit tests;
- edit OpenSpec artifacts;
- stage files;
- complete tasks;
- archive;
- commit.

Temporary compiler probes outside tracked source are allowed when needed.

The reviewer must leave repository/index state unchanged.

---

# Review Inputs

Before reviewing, read:

1. `.agent/project.md`
2. `.agent/invariants.md`
3. `.agent/decisions.md`
4. `.agent/workflow.md`
5. the complete active OpenSpec change:
   - proposal;
   - design;
   - tasks;
   - every delta spec.
6. the relevant staged implementation and tests.

Do not assume archived OpenSpec material describes the current API.

Do not assume a worker report describes the actual repository state.

---

# Establish Current State First

At the beginning of a review, inspect current volatile state.

Typical checks include:

```bash
git rev-parse HEAD
git status --short
git diff --cached --stat
git diff --cached --check
git diff --name-status
git diff --name-only
git ls-files --others --exclude-standard
```

And current OpenSpec state:

```bash
openspec status --change <change-name> --json
openspec validate <change-name> --strict
```

Use the current installed CLI syntax if it differs.

Record:

- HEAD;
- staged state;
- unstaged state;
- untracked state;
- active change;
- task progress;
- validation state.

Do not alter the index.

---

# Primary Review Principle

Review the invariant, not just the example.

If a reported fix closes:

```text
type X = A
```

do not stop after reproducing that exact spelling.

Ask:

> What semantic property made the old case invalid, and what nearby ordinary
> Scala forms test the same property?

Examples may include:

- transparent aliases;
- local type aliases;
- stable local values;
- typed rebindings;
- singleton ascriptions;
- refinements;
- dependent selections;
- concrete associated outputs;
- annotations;
- generic specialization.

The purpose is not exhaustive compiler exploration.

Use a small number of nearby probes that test the actual invariant.

---

# Public API First

When possible, reproduce behavior through the same public API available to a
downstream client.

Prefer:

```text
packaged public artifact
+ ordinary Scala source
+ documented imports
```

over:

```text
same-package access
private implementation helpers
test-only construction mechanisms
```

Same-package boundary tests remain useful for verifying visibility/authority,
but they do not replace downstream public API checks.

---

# Packaged Compiler Probes

Important Scala type-system guarantees should be tested against the actual
quantities artifact when practical.

Use the repository's generated/test fixture classpath rather than manually
guessing classpath contents.

Compiler probes should normally use the repository's strict downstream mode,
currently including:

```text
-Werror
-source:future
```

where configured.

A reviewer should verify that the compiler probe actually depends on the
packaged/public library behavior under review.

---

# Negative Probe Quality

A negative compiler result is meaningful only when the intended surrounding
program is valid.

For each important negative probe:

1. establish a valid prelude;
2. compile the prelude independently where practical;
3. add the expression expected to fail;
4. verify failure occurs at that expression;
5. inspect the relevant diagnostic;
6. reject unrelated compiler/macro internal failures.

A negative probe must not "pass" merely because of:

- missing imports;
- stale API names;
- syntax errors;
- wrong type arity;
- unrelated missing evidence;
- compiler crashes.

---

# Positive Probe Quality

Every important rejection rule should have nearby positive behavior proving the
implementation is not merely over-restrictive.

Examples:

```text
reject widened singleton key
    + accept concrete literal key

reject parameter-rooted dependent dimension
    + accept stable project-issued DimRef path

reject generic unresolved operation output
    + accept concrete operation output

reject recursive term path
    + accept shared noncyclic DAG reuse
```

Positive fixtures should use real supported APIs.

Avoid:

- `null`;
- `asInstanceOf`;
- locally implementing sealed evidence;
- dummy values that prove only a type can be named.

---

# Static / Runtime Agreement

When a static operation has runtime meaning, verify both sides agree.

Examples include:

- dimension multiplication;
- cancellation;
- inversion;
- rate composition;
- grid-to-exact embedding;
- runtime `DimKey` normalization;
- `DimRef` static/runtime identity.

A successful static result with a contradictory runtime key is blocking.

A correct runtime coefficient with a malformed trusted static result is also
blocking.

---

# Static / Runtime Atom Authority

For changes touching `DimRef`, atom keys, singleton keys, or private static
interpretation, review this invariant explicitly:

> One publicly inhabitable static atom identity must not denote contradictory
> runtime atom identities.

Probe caller-controlled widening when relevant.

For publicly supported constructors ask:

```text
What determines the static atom identity?

What determines the runtime AtomId?

Can the caller vary one without varying the other?
```

Runtime authority is required to be unique over publicly inhabitable `DimRef`
keys.

It is not required to be total over every key accepted by library-private
static interpretation.

---

# Canonical Static Grammar Review

For changes touching static dimensions, private interpretation, or equivalence
derivation, inspect both:

```text
accepted concrete forms
rejected non-concrete/malformed forms
```

Do not rely solely on Scala subtype bounds.

Review semantic categories relevant to the current model, such as:

- literal singleton keys;
- stable term/module singleton keys;
- supported generative identities;
- transparent aliases;
- annotations;
- unresolved generic keys;
- broad singleton types;
- bottom/null types;
- bounds;
- refinements/intersections;
- malformed exponents;
- duplicate/noncanonical entries.

The exact grammar comes from the active OpenSpec change and current source.

---

# Generic Specialization Review

For changes involving macros, dependent types, or private type-level
interpretation,
check whether generic evidence remains sound after later specialization.

Key question:

> Is the implementation committing to a semantic fact while some relevant type
> is still unresolved?

Nearby forms worth considering when relevant:

- method type parameters;
- method-parameter-rooted selections;
- aliases over those parameters;
- stable locals that rebound generic values;
- exact concrete refinements;
- abstract refinements;
- associated output types;
- nested associated output types.

Do not use casts to manufacture counterexamples.

---

# Arithmetic Boundary Review

When an active change affects arithmetic validity, classify APIs by behavior.

## Dimension-preserving arithmetic

Examples:

```text
x + y
x - y
x * scalar
exact scalar division
closed grid coordinate arithmetic
closed refinement arithmetic
```

Verify the active design's required dimension authority is consistently
enforced.

## Dimension-changing arithmetic

Examples:

```text
Quantity[A] * Quantity[B]
Quantity[A] / Quantity[B]
rate composition
```

Verify the complete result expression is validated/canonicalized according to
the active design.

Avoid requiring redundant evidence unless the specification requires it.

## Observation-only operations

Examples may include:

- equality;
- ordering;
- sign inspection;
- coefficient access.

Do not classify an evidence-free observer as an arithmetic authority defect
without showing it can manufacture or transform a malformed dimensional value.

---

# Direct API / Algebra Agreement

When algebra/typeclass instances exist, compare them with direct public
operations.

Ask:

```text
Can a typeclass instance perform arithmetic that direct APIs reject?

Does the direct API admit malformed arithmetic that the instance correctly
rejects?

Did a newly captured capability break serialization/laws?
```

Do not add requirements to `Eq`, `Order`, or similar observation-only instances
merely for symmetry.

---

# Grid Review

When a change touches grids, preserve the distinction:

```text
dimension identity
grid identity
grid provenance
registry ownership
```

Do not infer one from another.

Review, as applicable:

- coordinate arithmetic;
- exact embedding;
- projection;
- quantization;
- allocation / quotient-remainder;
- grid-to-grid conversion;
- residual handling;
- runtime ownership/provenance.

Grid arithmetic must remain exact except at explicitly named quantization
boundaries.

---

# Refinement Review

Refinements are numeric predicates, not dimension authority.

When reviewing refined operations:

- preserve sign/nonzero semantics;
- verify closed predicates remain closed;
- avoid unnecessary predicate revalidation;
- ensure wrappers do not bypass underlying dimension/grid authority.

Test at least one direct underlying operation and its refined wrapper when the
change affects both.

---

# Runtime / Heterogeneous Review

For runtime registries and heterogeneous values, check:

- authoritative `DimKey` equality;
- registry ownership;
- provenance;
- checked recovery of static evidence;
- consistency with static arithmetic.

Do not weaken runtime provenance to simplify static APIs.

---

# Public Authority Audit

For proof/evidence-heavy changes, inspect the public and package-qualified API.

Look for:

- public constructors;
- public `derived` methods;
- `Aux` aliases;
- caller-selected output parameters;
- package-private constructors;
- recursive implementation evidence;
- validation tokens;
- raw evidence factories.

Ask:

> Can an ordinary downstream caller select, transport, or construct authority
> the design intended the library/compiler to compute itself?

A public type alias is not automatically dangerous.

The issue is whether it creates authority.

---

# API Ergonomics Review

Soundness is necessary but not sufficient.

Check representative client code.

At minimum distinguish:

## Concrete client ergonomics

Concrete valid calls should derive intended evidence automatically where the
design says they should.

## Generic client ergonomics

A downstream generic helper should be able to forward the intended public
capabilities without referring to private macro types.

## Advanced extension ergonomics

When the active change affects canonical generic result typing, alignment, or
runtime recovery, assess whether downstream libraries can still express the
intended extension patterns.

Report material over-restriction as a usability finding rather than silently
assuming all conservative rejection is acceptable.

---

# OpenSpec Semantic Review

Strict schema validation is necessary but not sufficient.

Read the actual prose.

Verify:

```text
proposal intent
design decisions
normative delta requirements
task completion
implementation behavior
```

all describe the same semantics.

A checked task without current supporting evidence is a process finding.

A parser-valid specification that contradicts implementation is a semantic
finding.

---

# Scope Review

Inspect the staged diff against the active change.

Report meaningful unrelated changes.

Examples:

```text
static arithmetic change unexpectedly rewrites registry provenance

SameDimension ergonomics change alters grid identity model

static-interpretation fix introduces a new public evidence family

API-only change rewrites build architecture
```

Do not flag mechanically related tests/docs as scope drift.

---

# Build Validation

Use current repository configuration.

Typical validation includes:

```bash
sbt -batch clean test
sbt -batch scalafmtCheckAll
sbt -batch scalafmtSbtCheck
```

And relevant module/focused commands.

If the active change involves packaged compiler behavior, run the compiler
boundary suite directly.

If prior review found nondeterministic build behavior and the active change
claims to repair it, use the agreed repeated-run matrix.

Otherwise do not stress-test unrelated stable build behavior unnecessarily.

---

# Failure Classification

Distinguish:

## Repository failure

A repository/build/compiler task began and failed because of code, test,
classpath, task graph, or repository configuration.

This counts.

## Infrastructure failure

The requested repository task never began because of the surrounding tool
environment.

Examples:

- launcher lock unavailable;
- tool session killed before SBT began;
- external permission failure.

Report separately.

Do not count as a repository pass or failure.

Do not hide repository failures through retries.

---

# Severity

Use these defaults.

## Critical

Foundational authority is fundamentally compromised.

Examples:

- malformed trusted evidence can certify contradictory semantics;
- public APIs can construct values that invalidate foundational invariants.

Always blocking.

## High

Supported public code breaks a core correctness invariant.

Examples:

- static/runtime identity contradiction;
- unsound generic specialization;
- caller-selected trusted canonical output.

Always blocking.

## Medium

Substantive supported behavior contradicts the active design or required
validation.

Examples:

- malformed arithmetic escapes through a secondary public path;
- deterministic supported builds fail;
- meaningful generic ergonomics required by the design are broken.

Blocking by default.

## Low

Localized quality/process issue with limited semantic impact.

A low-severity configured-check failure is still blocking.

---

# Findings Format

Every substantive finding should include:

```text
ID
Severity
Affected invariant(s)
Location
Supported reproduction
Observed behavior
Expected behavior
Why it matters
Existing test coverage
Smallest sound remediation
```

Prefer invariant IDs from `.agent/invariants.md`.

Example:

```text
F1 — Caller-selected atom-key widening

Severity:
  High

Affected invariants:
  INV-I1
  INV-S1

Location:
  quantities/.../DimensionRef.scala:...

Supported reproduction:
  ...

Observed:
  two DimRef[Atom[K]] values share K but have distinct runtime keys

Expected:
  public construction cannot create contradictory authority

Existing coverage:
  current fixture does not test explicitly widened K

Smallest remediation:
  ...
```

---

# Review Verdict

Default final verdict vocabulary:

```text
READY
BLOCKED
```

For archive-focused reviews it may be:

```text
READY TO ARCHIVE
REQUEST CHANGES
```

The steward should normalize these internally to:

```text
approved
blocked
```

A review may return READY only when:

- no unresolved blocking finding exists;
- required validation passes;
- OpenSpec semantics match implementation;
- active-change task claims are supportable;
- repository state is compatible with finalization;
- independent review is the sole remaining process gate where applicable.

---

# Findings Are Evidence, Not Implementation Instructions

A reviewer may suggest the smallest sound remediation.

However, the reviewer must not silently redesign the active change.

If the smallest sound remediation appears to alter a settled invariant or
implement a PROPOSED/EXPLORING decision, say so explicitly.

That finding should be routed by the steward to DESIGN_CONFLICT rather than
ordinary remediation.

---

# Review Breadth Rule

Do enough independent work to establish confidence.

Do not mechanically generate dozens of variations after an invariant has been
well tested.

A good pattern is:

```text
tracked regression
+ one or two nearby independent forms
+ one positive counterpart
```

Increase breadth when:

- the implementation uses structural compiler classification;
- prior fixes have repeatedly missed nearby forms;
- the active change explicitly claims complete grammar coverage.

---

# Reviewer Completion Rule

Before returning READY, ask:

1. Did I independently exercise the load-bearing new behavior?
2. Did I test at least one nearby form not merely copied from the implementation
   report where compiler/type-system classification is involved?
3. Did I preserve positive supported ergonomics?
4. Did I inspect OpenSpec semantics, not merely run validation?
5. Did I leave repository state unchanged?
6. Is there any finding I am suppressing merely because the test suite is green?

If any answer creates a substantive unresolved concern, return BLOCKED.
