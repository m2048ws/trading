## 1. Quantity API

- [x] 1.1 Add authoritative `Quantity.apply` overloads for `Rational`, `BigInt`, `String`, and `java.math.BigDecimal`.
- [x] 1.2 Add exact `Int` and `Long` forwarding overloads that delegate through `BigInt`.
- [x] 1.3 Remove `fromRational`, `fromInteger`, `fromDecimal`, and `fromFiniteDecimal` from `Quantity`.

## 2. Coverage and Adoption

- [x] 2.1 Replace named-constructor compatibility coverage with compile-time checks that all four removed methods are
  unavailable.
- [x] 2.2 Add focused runtime coverage proving the `Int` and `Long` adapters match exact `BigInt` construction.
- [x] 2.3 Add compile-time coverage for `Int`/`Long` construction and rejected `Float`/`Double` construction.
- [x] 2.4 Migrate production code, tests, and README examples from `Quantity.fromXxx` to `Quantity(...)`.

## 3. Verification

- [x] 3.1 Format all changed Scala sources and verify no `Quantity.fromXxx` definitions or compiling call sites remain;
  retain only the four negative compile fixtures.
- [x] 3.2 Run the full `quantities`, adversarial-boundary, and aggregate `trading` test suites.
- [x] 3.3 Validate the OpenSpec change strictly.
