---
type: wiki
updated: 2026-08-29
---

# Project Foundations

This page preserves the verified project context formerly mixed into the retired steward control plane. Normative behavior remains in the cited specifications and source.

## Current structure

`trading` is a non-published SBT aggregate. Production code is owned by independently named modules; the adversarial boundary and benchmark projects are not production artifacts. The current module graph and source roots are defined by `build.sbt`, while target boundaries are accepted only through RFC delivery.

## Stable semantic boundaries

- `trading-quantities` owns exact rational quantities, static/runtime dimension semantics, anonymous grids, refinements, and mathematical algebra. Sources: `openspec/specs/exact-quantity-arithmetic/spec.md`, `openspec/specs/quantity-grid-projection/spec.md`, and `openspec/specs/runtime-quantity-identity/spec.md`; accepted evidence: RFC-0001-project-foundation.
- `trading-reference-data` owns stable identity, canonical handles, immutable catalog transitions, coherent snapshots, and reconciliation errors. Sources: `openspec/specs/reference-data-identity/spec.md` and `openspec/specs/reference-data-catalog/spec.md`; accepted evidence: RFC-0001-project-foundation.
- Domain and economic layers consume trusted values without live catalog, codec, or runtime dependencies. Application declares genuine environmental capabilities; runtime supplies concrete effects. Sources: `openspec/specs/repository-architecture/spec.md` and `openspec/specs/scala-functional-design/spec.md`; accepted evidence: RFC-0002-architecture-portfolio.
- Boundary data crosses one explicit parse, resolve, validate, and assemble transition before pure calculations. Durable representations contain stable identifiers and exact primitives rather than process-local authority. Source: `docs/design-principles.md`; accepted evidence: RFC-0002-architecture-portfolio.

## Delivery authority

Accepted RFCs define current change boundaries. Corgi planning artifacts provide requirements, design, tasks, source provenance, and AC traceability. Run Contract v3 and the single bound Issue own execution progress. The retired `.agent` steward, worker-role, and report workflow is historical only and must not be used as fallback orchestration.

## Sources

Primary sources are `build.sbt`, `README.md`, `docs/design-principles.md`, `openspec/specs/repository-architecture/spec.md`, `openspec/specs/scala-functional-design/spec.md`, `rfcs/RFC-0001-project-foundation/rfc.md`, and `rfcs/RFC-0002-architecture-portfolio/rfc.md`.
