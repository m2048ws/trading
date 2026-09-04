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
- [[wiki/deliveries/RFC-0005-simplify-post-trust-boundary-S-01-retire-trust-boundary-migration-scaffolding|RFC-0005-simplify-post-trust-boundary/S-01-retire-trust-boundary-migration-scaffolding]] — verified by sha256:048651d8e79a23efe53616bb45e0c46e0f4a986f1278d5816470b1fc68702ad8
- [[wiki/deliveries/RFC-0005-simplify-post-trust-boundary-S-02-total-and-deterministic-execution-transitions|RFC-0005-simplify-post-trust-boundary/S-02-total-and-deterministic-execution-transitions]] — verified by sha256:fc98726449f5ed9c353d75b88b17d255c3c509f1c8d84be94eda66e5ee00e8c9
- [[wiki/deliveries/RFC-0005-simplify-post-trust-boundary-S-03-use-direct-scala-derived-models|RFC-0005-simplify-post-trust-boundary/S-03-use-direct-scala-derived-models]] — verified by sha256:aa9b0dc3e278d955dd75f2edab6fd5d572f17f2754252b7e4cb6b345e9e80658
- [[wiki/deliveries/RFC-0005-simplify-post-trust-boundary-S-04-use-scala-first-quantity-and-reference-data|RFC-0005-simplify-post-trust-boundary/S-04-use-scala-first-quantity-and-reference-data]] — verified by sha256:95f7a7ae69b6481f23ea39097a3533e1ad73ee57563f054d6581379a78567432
- [[wiki/deliveries/RFC-0005-simplify-post-trust-boundary-S-05-remove-java-api-compatibility|RFC-0005-simplify-post-trust-boundary/S-05-remove-java-api-compatibility]] — verified by sha256:60fca10695ecec821f321730ede792e35d7310ca94e497504462a719c001b84b
- [[wiki/deliveries/RFC-0008-simplify-instrument-dependent-apis-S-01-name-instrument-dimensions|RFC-0008-simplify-instrument-dependent-apis/S-01-name-instrument-dimensions]] — verified by sha256:f51be78a9c5b67c22ac29adfeebbb80bdbddd712d4c29f51933da3fbad945e3f
- [[wiki/deliveries/RFC-0008-simplify-instrument-dependent-apis-S-02-bind-standard-order-construction|RFC-0008-simplify-instrument-dependent-apis/S-02-bind-standard-order-construction]] — verified by sha256:e0cb40a5a2bbfb4b3c5ae954d21e7be212433eab7114ed6adbfd81038d228a35
- [[wiki/deliveries/RFC-0008-simplify-instrument-dependent-apis-S-03-bind-source-fact-construction|RFC-0008-simplify-instrument-dependent-apis/S-03-bind-source-fact-construction]] — verified by sha256:336f9805040785ac5aea98745d63ded1121da6e23cab778dd0de44d489250a9e
- [[wiki/deliveries/RFC-0008-simplify-instrument-dependent-apis-S-04-bind-scenario-record-codecs|RFC-0008-simplify-instrument-dependent-apis/S-04-bind-scenario-record-codecs]] — verified by sha256:db11f417b9b2b52e876c9e618045c0ea2bac19033a63d234ad7cb44c75c8f1f9
- [[wiki/deliveries/RFC-0008-simplify-instrument-dependent-apis-S-05-bind-instrument-risk|RFC-0008-simplify-instrument-dependent-apis/S-05-bind-instrument-risk]] — verified by sha256:a512c2e68318d9b39f26cb899009f9d524de936ec6d6bbb2151c910f78fb3f5b
<!-- corgi:managed:end verified-deliveries -->

## Working Preferences

(No source-backed preferences promoted yet.)
