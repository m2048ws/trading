## Context

See `proposal.md` for motivation. The remaining production Java is limited to `AssetId`, `GridId`, and `GridVersion`; each duplicates validation-adjacent value behavior that Scala can derive while its companion retains the checked factory. `UniformGrid.create` and `GridDefinition.apply` currently re-run `PositiveRational` validation and each owner also exposes a raw-rational factory whose only live callers are null-boundary tests. `CatalogCommit` and `CatalogTransition` manually emulate Scala products so downstream Scala code can inspect and pattern-match model-issued results.

The conversion must preserve path-dependent grid authority, reference-data lineage, checked wire reconstruction, null rejection at supported roots, exact diagnostics, and Java-object-serialization failure. No generated JVM layout is a compatibility promise after S-05 established Scala 3 as the supported domain source API.

## Goals / Non-Goals

**Goals:**

- Make the final reference-data identifiers Scala-owned without opening unchecked construction.
- Trust an already established `PositiveRational` exactly once and keep raw invalidity at the refinement or external reconstruction boundary.
- Use compiler-derived Scala sum/product behavior for catalog observations while retaining model-owned issuance.
- Preserve downstream source behavior that is part of the Scala contract and characterize any generated representation assumptions before conversion.

**Non-Goals:**

- Changing stable identifier syntax, normalization, wire formats, catalog commands, revision rules, lineage, or handle authority.
- Replacing invariant-bearing catalog state, snapshots, deltas, commands, non-empty wrappers, or checked factories with unrestricted products.
- Introducing explicit-nulls, Java source compatibility adapters, reflection, serialization support, or a new external reconstruction API.
- Changing quantity arithmetic, dimension normalization, generative grid identity, or module ownership.

## Decisions

### Use private-constructor Scala case classes for checked identifiers

Move the three identifiers into the Scala reference-data owner as final case classes with private constructors and checked companion `from` methods. The generated field access, value equality, hashing, and domain-readable product rendering match the existing observations, while constructor and `copy` accessibility follow the checked construction boundary. Each value continues to mix in `JavaSerializationUnsupported`, and `from` explicitly rejects null before classifying expected invalidity.

This is preferred to opaque aliases because their runtime rendering would expose only the underlying scalar unless extra wrapping machinery were reintroduced. It is preferred to hand-written Scala classes because those would merely translate the Java equality/hash/rendering boilerplate rather than simplify it.

### Remove raw grid factories and trust the established refinement

`UniformGrid.create` and `GridDefinition.apply` will null-check their owning inputs, force any required dimension-key observation, and store the supplied `PositiveRational` directly. `UniformGrid.from` and `GridDefinition.from` will be removed. Raw reconstruction already has access to `PositiveRational`; an owning codec or parser must refine first and map the refinement failure into its own diagnostic before requesting grid authority.

This is preferred to retaining deprecated forwarders because S-05 deliberately removed the Java API obligation they served. The reference-data error type for nonpositive external definitions may remain available to owning reconstruction code; removing an error alternative is not needed to remove the factory.

### Use a sealed direct sum and private direct products for catalog observations

Represent `CatalogCommit` as a sealed Scala sum with `Unchanged` and `Published` case-class alternatives, and represent `CatalogTransition` as a case class. Their constructors remain `private[reference]`, their bodies retain null checks, and catalog evaluation remains the only supported issuer. Public extractors, field observations, structural equality, hashing, and rendering are compiler-derived.

A sealed sum is preferred to a public enum constructor surface because issued snapshots and publication deltas must remain coherent. A direct case-class transition is safe with package-private construction because the product carries already validated model-issued values and its equality continues to compare the same two fields.

### Adapt Scala consumers to source semantics, not JVM accessor spelling

Downstream Scala code will use parameterless Scala value accessors and exhaustive pattern matching. Completed-artifact positive and negative fixtures will verify smart-constructor-only identity creation, catalog observation matching, and inaccessible issuance. Tests will preserve precise invalid input, null, equality/hash/display, lineage/reconciliation, and serialization behavior without asserting generated bytecode names or Java-call syntax.

## Risks / Trade-offs

- [Generated case-class methods expose an unchecked identifier path] → Keep constructors private, retain the negative direct/apply/copy fixture, and inspect the completed artifact from downstream Scala.
- [Scala-derived rendering or hashing differs from the Java values] → Characterize exact equality, hash, and display before conversion and keep those observations in reference-data and completed-artifact tests.
- [Removing raw factories moves invalidity too late] → Require raw values to pass through `PositiveRational` at the owning external boundary and test that no grid definition, witness, or handle is returned on failure.
- [Direct catalog products expose incoherent issuance] → Restrict constructors to the reference-data owner, retain null checks, and keep external negative construction plus model-issued exhaustive matching coverage.
- [Source conversion accidentally changes codec bytes] → Run the complete boundary-codec compatibility/property suite against unchanged stable representations.

## Migration Plan

1. Convert checked identifiers and adapt Scala accessor call sites while preserving characterization and completed-artifact tests.
2. Remove raw grid factories and defensive refinement revalidation, then move raw invalidity assertions to the owning refinement/reconstruction tests.
3. Convert catalog observations to direct sums/products and update model-internal null tests and downstream exhaustive matches.
4. Run clean aggregate tests, benchmark compilation, formatting, the zero-reflection guard, production-source inventory, and strict Corgi readiness.

Rollback is one Task Group commit at a time before archive. No data migration or wire-version change is required.
