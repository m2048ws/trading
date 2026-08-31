---
type: memory
created: 2026-08-29
---

# MEMORY — Hard Constraints

> Permanent, source-backed project knowledge. Read after `session-bridge.md` at every startup.

## Project Identity
- **Name**: trading
- **Purpose**: `trading` is a Scala 3 multi-module SBT foundation for exact, dimension-safe trading systems. The root project is a non-published aggregator: production code belongs to independently named modules rather than the repository root. The minimum build and runtime JDK is 25. (source: `README.md` or project metadata)
- **Stack**: Scala 3.8.x, multi-module SBT, Cats/Algebra where admitted by the owning module, and JDK 25 minimum. (source: `build.sbt`, `project/build.properties`, `docs/design-principles.md`)

## Permanent Constraints

- Exact quantity arithmetic remains rational and dimension-safe; grid membership and projection stay explicit and contextual. (source: `openspec/specs/exact-quantity-arithmetic/spec.md`, `openspec/specs/quantity-grid-projection/spec.md`)
- Production concepts have one primary owner, dependencies remain directed, and logical responsibility precedes physical module creation. (source: `openspec/specs/repository-architecture/spec.md`, `docs/design-principles.md`)
- Pure domain semantics remain separate from effect-polymorphic application ports and concrete runtime interpreters. (source: `openspec/specs/scala-functional-design/spec.md`, `docs/design-principles.md`)
- Corgi Run Contract state and the bound single Issue are delivery authority; legacy steward worker orchestration is retired. (source: `AGENTS.md`, `tools/corgi/README.md`)

Add an entry only after a human accepts it or a verified delivery proves it. Every entry must cite an accepted RFC, a current architecture page, or a concrete source file. `corgispec archive --local` alone writes archive-derived promotions; skills may only prepare or verify them read-only.

## Verified Deliveries
<!-- corgi:managed:start verified-deliveries -->
- [[wiki/deliveries/RFC-0002-architecture-portfolio-S-01-application-runtime-foundation|RFC-0002-architecture-portfolio/S-01-application-runtime-foundation]] — verified by sha256:4a4ee2d82d93b117f9e43689b2304cfa84221b2c8a3d23c66650e2ef6f0e4795
- [[wiki/deliveries/RFC-0002-architecture-portfolio-S-02-order-execution-scenarios|RFC-0002-architecture-portfolio/S-02-order-execution-scenarios]] — verified by sha256:cf55e7a63baf6f69f82624c3dda1f4cf8b2ca1ca742c6465fea71b43c28f7a03
<!-- corgi:managed:end verified-deliveries -->

## Working Preferences

(No source-backed preferences promoted yet.)
