## MODIFIED Requirements

### Requirement: Supported Scala trust and serialization boundary
Construction guarantees SHALL apply to well-typed supported Scala 3 callers without casts, reflection, unsafe JVM
access, hand-written bytecode, or constructor-bypassing deserialization. Java object serialization SHALL fail closed
through the common project-owned `NotSerializableException` mechanism for invariant-bearing quantity result and error
records and dependent runtime-dimension carriers. Stable asset and grid identities, authority-bearing reference-data
handles, and durable boundary records SHALL be owned and specified by downstream artifacts rather than by the quantity
serialization boundary.

#### Scenario: Reject Java serialization
- **WHEN** an invariant-bearing quantity result, dimension-identity record, or quantity error record is serialized
- **THEN** Java serialization fails instead of creating an unchecked reconstruction path

#### Scenario: Keep durable reconstruction downstream
- **WHEN** a wire or database record must reconstruct a typed quantity together with stable asset or grid identity
- **THEN** an explicit downstream codec and trusted reference-data boundary perform the reconstruction rather than a
  quantity-owned packed decoder

### Requirement: Existing dimensional carriers have validated indices
For supported, well-typed Scala callers, every normally returned `Quantity[D]` and `GridQuantity[D, G]` SHALL have a
valid closed dimension index `D`. The invariant SHALL be established at public construction roots: coefficient-bearing
quantities and zero manufacture without an existing carrier require an authoritative `DimRef[D]`; nonzero grid
coordinates require a matching anonymous `GridRef[D]`; grid zero requires `DimRef[D]` or a stronger matching grid
witness; non-reflexive alignment requires a non-null `SameDimension`; and checked runtime reconstruction must delegate
to those same roots after obtaining the required trusted witness. Dimension-changing results derived from existing
trusted carriers SHALL preserve their complete `Times`, `Inverse`, or `Divide` expression rather than require or expose
a caller-selected canonical output.

Possessing a dimensional value SHALL NOT materialize or permit recovery of `DimRef[D]`, `DimKey`, `SameDimension`, a
matching `GridRef[D]`, private static interpretation, stable reference-data identity, or catalog provenance. It SHALL only
allow operations that preserve its already validated dimension index to construct further values at that same index
without requesting dimension authority again. Refined wrappers over an existing dimensional value SHALL inherit the
same dimension-index invariant.

Operation-local rejection of a hypothetical malformed carrier type SHALL NOT be required. A method body that accepts an
otherwise unobtainable `Quantity[Bad]` or `GridQuantity[Bad, G]` parameter MAY type-check for index-preserving
transformations, but supported public APIs SHALL provide no normally returning construction path for such an argument.
The ordinary supported-caller exclusions for casts, reflection, unsafe bytecode, and constructor-bypassing
deserialization remain unchanged; cast-free `null` SHALL NOT inhabit either opaque carrier. Literal `null` supplied as
reference-valued construction or alignment authority SHALL fail at the public boundary before a witness, dimensional
carrier, rate, or identity-bearing algebra capability is returned. A typed null `Rational` coefficient or `BigInt`
coordinate SHALL likewise be rejected at the shared coefficient or coordinate construction boundary; any downstream
checked decoder SHALL reconstruct through that same guarded coordinate boundary.

#### Scenario: Reduce existing generic quantities
- **WHEN** generic code receives a nonempty collection of `Quantity[D]` values and combines them with homogeneous
  addition
- **THEN** it requires no `DimRef[D]` or dimensional-equivalence evidence

#### Scenario: Transform one existing quantity
- **WHEN** generic code scales or exact-divides an existing `Quantity[D]` while preserving `D`
- **THEN** the result remains `Quantity[D]` without contextual dimension evidence

#### Scenario: Transform existing refined values
- **WHEN** generic code combines or otherwise transforms existing refined quantity or grid values without changing their
  dimension index
- **THEN** refinement closure and result construction require no dimension capability

#### Scenario: Keep value trust non-extractable
- **WHEN** generic code possesses `Quantity[D]` or `GridQuantity[D, G]`
- **THEN** it cannot summon or recover `DimRef[D]`, `SameDimension`, a runtime key, a grid witness, private static
  interpretation, stable reference-data identity, or catalog provenance from that value

#### Scenario: Reject malformed carrier construction
- **WHEN** supported code selects a zero-power or otherwise malformed `D` and attempts raw construction, witness-backed
  zero or coefficient construction, non-reflexive alignment from a valid carrier, or checked downstream reconstruction
- **THEN** no normally returning `Quantity[D]` or `GridQuantity[D, G]` is produced

#### Scenario: Permit an uncallable hypothetical transformation
- **WHEN** a method declares a `Quantity[Bad]` parameter and its body performs only index-preserving arithmetic
- **THEN** the body MAY type-check even though supported code cannot construct an argument that calls it normally

#### Scenario: Reject null carrier inhabitation
- **WHEN** supported Scala assigns literal `null` to `Quantity[D]` or `GridQuantity[D, G]` without a cast
- **THEN** compilation fails at the opaque carrier boundary

#### Scenario: Reject null numeric carrier payloads
- **WHEN** supported Scala supplies a typed null `Rational` coefficient or `BigInt` coordinate to witness-backed
  construction, including through an otherwise valid downstream record passed to checked decoding
- **THEN** the shared construction boundary terminates before returning a `Quantity`, `GridQuantity`, or dependent
  reconstructed carrier

#### Scenario: Reject null dimensional construction authority
- **WHEN** supported Scala explicitly supplies literal `null` as `DimRef[D]` to quantity, grid, refinement, rate, or
  identity-bearing algebra construction
- **THEN** the root terminates before returning any dimensional carrier, rate, or algebra capability

#### Scenario: Reject null alignment authority
- **WHEN** supported Scala explicitly supplies literal `null` as `SameDimension[D, E]` to quantity or grid `alignTo`
- **THEN** alignment terminates before returning a retagged dimensional carrier

#### Scenario: Reject null normalization authority
- **WHEN** supported Scala attempts the obsolete null normalization-authority construction path
- **THEN** no such public capability or entry point exists; retained `DimRef` construction and `SameDimension`
  alignment boundaries independently reject null before returning a carrier, witness, rate, or algebra capability

#### Scenario: Reject null grid-construction authority
- **WHEN** supported Scala supplies literal `null` as the `DimRef[D]` authority to uniform-grid construction
- **THEN** construction terminates before returning an anonymous `GridRef[D]` capable of attaching coordinates

#### Scenario: Reject null runtime-identity authority
- **WHEN** supported Scala supplies literal `null` as a `DimKey` atom or power component, a fresh key, or an atomic or
  nominal atom ID
- **THEN** the quantity construction root terminates before returning a key, dimension witness, equivalence, or
  dimensional carrier
