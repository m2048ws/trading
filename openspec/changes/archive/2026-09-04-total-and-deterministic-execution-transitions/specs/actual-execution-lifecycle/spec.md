## MODIFIED Requirements

### Requirement: Pure transition and replay are deterministic

Lifecycle evolution SHALL be a total checked pure transition over immutable state and normalized evidence. Equivalent
authoritative evidence sets SHALL derive the same commands, facts, effective fills, exact exposure, submission and
cancellation knowledge, completeness, conflicts, unresolved references, and anomalies regardless of delivery order
where that order has no authority. Expected invalidity, scope mismatch, duplicate, conflict, and uncertainty SHALL be
represented in domain result types rather than exceptions, flags, nulls, or mutable status.

Command recording and dispatch observation SHALL return a closed total transition sum. Its rejected alternative SHALL
carry non-empty command violations, while applied, idempotent, command-conflict, and dispatch-conflict alternatives
SHALL carry no optional violation payload; production transition handling MUST NOT use unchecked violation extraction.
Replay, effective-fill, anomaly, continuation, and diagnostic ordering SHALL compare typed components
lexicographically under owner-local orderings rather than encode composite keys as delimiter-concatenated strings.
One lifecycle observation SHALL derive one effective-fill ledger and supply that same derivation to anomaly
construction.

#### Scenario: Replay the same evidence repeatedly

- **WHEN** the same normalized command and fact history is replayed from an empty lifecycle more than once
- **THEN** every replay produces structurally equal state, derived observations, and deterministic diagnostics

#### Scenario: Permute delivery without changing authority

- **WHEN** the same source-qualified evidence is delivered in different orders while carrying identical authoritative sequence and reference relationships
- **THEN** the reconstructed authoritative lifecycle is equivalent and network order contributes no semantics

#### Scenario: Reject a foreign lifecycle input

- **WHEN** a command or fact targets another logical order, execution account, source scope, or instrument
- **THEN** a typed scope error is returned and no foreign economic contribution is applied

#### Scenario: Inspect an incomplete lifecycle

- **WHEN** gaps, source conflicts, rewinds, or unresolved references remain
- **THEN** callers can inspect retained evidence and exact known exposure while completeness remains an explicit non-empty diagnostic

#### Scenario: Classify every command transition

- **WHEN** command recording or dispatch observation applies evidence, receives an idempotent duplicate, retains a
  command or dispatch conflict, or rejects invalid input
- **THEN** exactly one exhaustive transition alternative is returned, only rejection carries non-empty violations,
  and the prior state and classification semantics are preserved

#### Scenario: Compare identifiers containing former delimiters

- **WHEN** distinct command, source, account, stream, event, order, fill, or lineage identifiers contain characters
  formerly used to concatenate sort keys
- **THEN** replay and observation compare their typed components without collisions and remain deterministic across
  delivery permutations

#### Scenario: Reuse the observation ledger for anomalies

- **WHEN** one lifecycle observation derives effective fills, exact exposure, corrections, busts, conflicts,
  unresolved references, cancellation races, and overfill anomalies
- **THEN** anomaly construction consumes that observation's single effective-fill ledger and preserves the existing
  structural results
