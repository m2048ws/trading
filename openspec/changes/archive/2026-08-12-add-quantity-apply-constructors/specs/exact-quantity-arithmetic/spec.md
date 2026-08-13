## MODIFIED Requirements

### Requirement: Exact quantity construction
`Quantity` SHALL provide concise, overloaded, dimension-witnessed `apply` construction from `Rational`, `BigInt`,
`Int`, `Long`, decimal text, and finite `java.math.BigDecimal`, plus dimension-polymorphic zero. `Int` and `Long`
construction SHALL widen exactly through `BigInt`. Overloaded `apply` SHALL be the sole public coefficient-bearing
construction surface: `fromRational`, `fromInteger`, `fromDecimal`, and `fromFiniteDecimal` SHALL NOT be available. The
scalar accessor SHALL return the canonical `Rational`. No authoritative constructor SHALL accept `Float` or `Double`,
and raw opaque reconstruction SHALL remain unavailable to supported callers, including downstream source that declares
`package trading.quantity`. Raw coefficient attachment and operation-result construction SHALL be lexically private within
the `Quantity` opaque owner; package-qualified visibility SHALL NOT be a construction boundary. Except for polymorphic
zero, caller-supplied coefficients SHALL require an authoritative `DimRef[D]`, and arithmetic results SHALL be derived
only from legitimate operands and checked evidence.

#### Scenario: Construct supported exact coefficients concisely
- **WHEN** `Quantity(dimension, coefficient)` receives a `Rational`, `BigInt`, `Int`, `Long`, decimal `String`, or
  finite `java.math.BigDecimal`
- **THEN** it returns the exact result or validation failure defined for that coefficient type

#### Scenario: Construct primitive integers concisely
- **WHEN** `Quantity(dimension, coefficient)` receives an `Int` or `Long` value or literal
- **THEN** it returns the same exact quantity as widening that coefficient to `BigInt`

#### Scenario: Construct exact decimal text
- **WHEN** `Quantity(usdDimension, "6000.001")` receives a USD dimension witness
- **THEN** it returns a `Quantity[USD]` with coefficient `6000001/1000`

#### Scenario: Reject removed named constructors
- **WHEN** supported Scala invokes `fromRational`, `fromInteger`, `fromDecimal`, or `fromFiniteDecimal`
- **THEN** the code does not compile and must use the matching `apply` overload

#### Scenario: Reject floating construction
- **WHEN** supported Scala attempts to construct an exact quantity from `0.1d` or `0.1f`
- **THEN** the code does not compile

#### Scenario: Reject same-package raw coefficient attachment
- **WHEN** downstream Scala declares `package trading.quantity` and supplies an arbitrary coefficient to a raw or
  operation-result construction helper
- **THEN** lexical privacy prevents construction of a chosen `Quantity[D]`
