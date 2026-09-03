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
- [[wiki/deliveries/RFC-0001-project-foundation-S-01-project-foundation|RFC-0001-project-foundation/S-01-project-foundation]] — verified by sha256:1f5cb11d10470ab8db46b0d0d8641fa30dacf366cde53d00f54981c40af8263e
- [[wiki/deliveries/RFC-0002-architecture-portfolio-S-01-application-runtime-foundation|RFC-0002-architecture-portfolio/S-01-application-runtime-foundation]] — verified by sha256:4a4ee2d82d93b117f9e43689b2304cfa84221b2c8a3d23c66650e2ef6f0e4795
- [[wiki/deliveries/RFC-0002-architecture-portfolio-S-02-order-execution-scenarios|RFC-0002-architecture-portfolio/S-02-order-execution-scenarios]] — verified by sha256:cf55e7a63baf6f69f82624c3dda1f4cf8b2ca1ca742c6465fea71b43c28f7a03
- [[wiki/deliveries/RFC-0002-architecture-portfolio-S-03-pure-risk|RFC-0002-architecture-portfolio/S-03-pure-risk]] — verified by sha256:12efcb23d09389513848f9ff1dda4f7e03a0e9eb1eba0b2648d70af0e4a5442f
- [[wiki/deliveries/RFC-0002-architecture-portfolio-S-04-fee-policy|RFC-0002-architecture-portfolio/S-04-fee-policy]] — verified by sha256:4556f072667579a209e7a80bfe55a0a29d43cdebc873a3682893dc9479016eb7
- [[wiki/deliveries/RFC-0002-architecture-portfolio-S-05-versioned-boundary-codecs|RFC-0002-architecture-portfolio/S-05-versioned-boundary-codecs]] — verified by sha256:21835d70b9da0a74b9b03f9a22ca11833eec287671357f63a7281dd3de69c897
- [[wiki/deliveries/RFC-0003-execution-lifecycle-foundation-S-01-actual-execution-lifecycle|RFC-0003-execution-lifecycle-foundation/S-01-actual-execution-lifecycle]] — verified by sha256:65afe49168a3c223c12556a2ffdfea42e0244580b2d29a30b3d5365b9d28c2e6
- [[wiki/deliveries/RFC-0004-simplify-in-process-trust-boundary-S-01-simplify-in-process-trust-boundary|RFC-0004-simplify-in-process-trust-boundary/S-01-simplify-in-process-trust-boundary]] — verified by sha256:77f7954a068af43ca196f8e88262efca7c1b782ca20ebfa860cf2ef3847ef022
<!-- corgi:managed:end verified-deliveries -->

## Working Preferences

(No source-backed preferences promoted yet.)
