## Context

See `proposal.md` for motivation and the exact-construction delta spec for the behavioral contract. `Quantity` is an
opaque `Rational` owned by its companion, so every public constructor must continue to pass through that lexical owner
and require a `DimRef[D]`. The new coefficient types have distinct JVM representations and can therefore be represented
by JVM-distinct Scala overloads. Scala overload resolution does not adapt `Int` or `Long` arguments to `BigInt` when
selecting among those overloads, so explicit primitive adapters are required.

## Goals / Non-Goals

**Goals:**

- Make `Quantity(dimension, coefficient)` the concise canonical spelling for every currently supported coefficient
  type.
- Preserve concise construction from primitive integer values and literals without approximation.
- Preserve the precise result and error types of text and finite-decimal construction.
- Expose one coefficient-construction spelling instead of parallel named and overloaded APIs.

**Non-Goals:**

- Add generic numeric construction, implicit conversions, or floating-point support.
- Change `Rate`, grid construction, opaque representations, or runtime persistence.

## Decisions

### Make the overloads the sole public construction path

Each `apply` overload will contain or compose the existing construction logic. `fromRational`, `fromInteger`,
`fromDecimal`, and `fromFiniteDecimal` will be removed instead of retained as aliases. This makes the concise spelling
the only public coefficient-bearing path and prevents the API from carrying two names for identical behavior.

Keeping one-line compatibility aliases was considered, but rejected because it preserves redundant surface area and
allows new code to continue choosing the verbose spelling. The source incompatibility is explicit and mechanical: each
removed call maps directly to one `Quantity(dimension, coefficient)` call with unchanged result and error types.

### Overload the four semantic types and forward primitive integers explicitly

The semantic overload set will accept `Rational`, `BigInt`, `String`, and `java.math.BigDecimal`. Additional `Int` and
`Long` overloads will widen to `BigInt` and delegate to the authoritative integer overload. This is required because
Scala does not apply the usual primitive-to-`BigInt` adaptation while resolving the broader overload set. The adapters
add no new numerical semantics and preserve exact integer values.

Requiring callers to write `BigInt(42)` was rejected because it undermines the requested brevity and prevents a direct
replacement for existing literal-based `fromInteger` calls. A generic numeric typeclass or `Any`-based dispatcher was
also rejected because either could broaden construction authority, weaken static error reporting, or accidentally
admit approximate inputs. Unsupported `Float` and `Double` arguments continue to have no applicable overload.

### Exercise the concise spelling and enforce removal

Production code, tests, and README examples in the `trading-quantities` module will use `Quantity(...)` so the new API
is continuously compiled in realistic contexts. Focused tests will cover each semantic overload, verify the primitive
adapters against `BigInt`, assert that every removed name fails compilation, and retain negative compile checks for
floating inputs.

## Risks / Trade-offs

- [Primitive integer adapters accidentally alter numerical semantics] → Delegate both concrete overloads directly to
  `BigInt` construction and compare their results in tests.
- [A future overload admits an approximate caller type] → Keep overloads concrete and retain compile-time checks for
  rejected floating inputs.
- [Downstream source still calls a removed constructor] → Document the one-to-one migration and use compile-time checks
  to keep the removed names from reappearing.
- [Large mechanical call-site migration obscures semantic changes] → Restrict replacements to `Quantity.fromXxx`
  invocations and rely on formatting plus the full test suite to verify the result.

## Migration Plan

Add overloads first, migrate every repository call site, remove the four named methods, then run compile-time boundary
tests and the full suite. Downstream source migrates `Quantity.fromXxx(dimension, coefficient)` to
`Quantity(dimension, coefficient)` without changing how successful or failed results are handled. Rollback consists of
restoring the named methods and their compile-time availability; no stored data or runtime schema is involved.
