## ADDED Requirements

### Requirement: Public DimRef atom authority is unique
`DimRef[D]` SHALL be the authoritative public association between an inhabited static dimension type `D` and its runtime
`DimensionKey`. For every singleton key `K` whose `Atom[K]` is inhabitable through supported public `DimRef` APIs, any two
publicly obtained values of type `DimRef[Atom[K]]` SHALL have equal `DimensionKey` values. This uniqueness requirement
SHALL apply only to publicly inhabitable atom types; the set of keys accepted by `Normalize[Atom[K]]` MAY be larger, and
normalization alone SHALL NOT make an atom type runtime-inhabitable.

Public atom construction SHALL bind static and runtime identity at one authority-bearing boundary. Literal construction
SHALL derive the runtime atom identifier from the accepted literal singleton. Nominal construction SHALL bind the result
to the supplied stable object's exact singleton type and to the runtime identifier owned by that object. Generative and
fresh runtime witnesses SHALL bind their path-dependent atom type to the identity captured by that same witness. No
supported public constructor SHALL accept a caller-selected static atom type independently from a caller-selected runtime
identity.

The public `DimRef` identity witness SHALL bind `One` to `DimensionKey.one`. Product, inverse, and quotient operations
SHALL preserve the static/runtime association inductively: each SHALL return the canonical static output established by
the complete `Normalize` operation and the corresponding runtime key produced from its authoritative input keys.
Supported downstream code SHALL NOT directly construct or implement `DimRef` to bypass these roots and operations.

#### Scenario: Repeat literal construction
- **WHEN** supported callers construct `DimRef.atom["BTC"]` more than once
- **THEN** every result has type `DimRef[Atom["BTC"]]` and the same runtime dimension key derived from `"BTC"`

#### Scenario: Repeat nominal construction
- **WHEN** a stable `NominalAtom` object is supplied to `DimRef.atom` more than once
- **THEN** every result retains that object's exact singleton atom type and the same object-owned runtime identity

#### Scenario: Reject caller-selected literal widening
- **WHEN** a caller supplies different `ValueOf[String & Singleton]` values and requests the same widened
  `DimRef[Atom[String & Singleton]]` type
- **THEN** construction is rejected because the widened key is not accepted by `Normalize[Atom[K]]`

#### Scenario: Reject caller-selected nominal widening
- **WHEN** distinct nominal objects are widened to a shared nominal singleton supertype before construction
- **THEN** the results cannot both inhabit one caller-selected `DimRef[Atom[K]]` type and retain their distinct runtime
  identities

#### Scenario: Preserve generative authority
- **WHEN** a generative or fresh runtime witness exposes its dimension repeatedly
- **THEN** that witness's exact path-dependent atom type always denotes the runtime identity captured by that witness,
  while a different witness has a distinct path-dependent atom type

#### Scenario: Do not totalize normalized keys
- **WHEN** `Normalize[Atom[K]]` succeeds for a concrete stable key outside the supported public `DimRef` authority sources
- **THEN** no `DimRef[Atom[K]]` constructor or runtime key is inferred from normalization alone

#### Scenario: Preserve authority through witness algebra
- **WHEN** public `DimRef` product, inverse, or quotient combines authoritative input witnesses
- **THEN** the returned `DimRef` has the canonical normalized output type and the exactly corresponding runtime
  `DimensionKey` operation result

#### Scenario: Reject downstream witness forgery
- **WHEN** supported downstream source attempts to implement `DimRef[D]` or invoke an unbound static/runtime constructor
- **THEN** construction is unavailable and no contradictory runtime identity can inhabit the chosen static type
