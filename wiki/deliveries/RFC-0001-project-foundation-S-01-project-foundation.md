---
type: delivery
updated: 2026-09-03
rfc: RFC-0001-project-foundation
slice: S-01-project-foundation
change: establish-architecture-and-functional-design-charter
status: archived
archived: 2026-08-28
evidence_manifest: sha256:1f5cb11d10470ab8db46b0d0d8641fa30dacf366cde53d00f54981c40af8263e
source_digest: sha256:8515cbb2322cbdb58a9f914bb4308cc19d1f34c624973594646be806200767b0
---

# RFC-0001-project-foundation/S-01-project-foundation

## Outcome

Established the repository-wide architecture and functional-design foundation: primary responsibility ownership,
acyclic dependency direction, explicit trust transitions, semantic evidence in types, pure/effect separation,
claim-proportional verification, and domain-readable public APIs.

This Slice was implemented and archived through the repository's original OpenSpec workflow before Corgi v4 was
bootstrapped. The 2026-09-03 reconciliation records that historical delivery without claiming that a Run Contract v3
run existed.

## Boundary Delivered

The delivery created the normative `repository-architecture` and `scala-functional-design` specifications, the
human-facing design principles, and the architecture charter audit. It changed governance, specifications, and
documentation; it did not introduce a production API, module, dependency, or runtime behavior.

## Acceptance Evidence

| AC | Requirement | Evidence | Result |
|---|---|---|---|
| AC-001 | both | Archived architecture specification, dependency audit, accepted RFC, and legacy reconciliation record | PASS |
| AC-002 | both | Archived trust-boundary and functional-design specifications plus accepted RFC | PASS |
| AC-003 | automated | Completed historical strict-validation, formatting, and clean-build task record | PASS |
| AC-004 | both | Claim-proportional verification matrix and completed independent-review task | PASS |
| AC-005 | human | Accepted foundation RFC and explicit confirmation by m2048ws during reconciliation | PASS |

## Implementation

- Legacy OpenSpec delivery: `3901ec2bca5f2b3ea36496ccb0a239d6d794f8a4`
- Final HEAD: `3901ec2bca5f2b3ea36496ccb0a239d6d794f8a4`

## Review and QA

- Human Review: approved by m2048ws through RFC acceptance and confirmed during migration reconciliation.
- Human QA: skipped — the delivery changed governance, specifications, and documentation only, with no runtime impact.

## Knowledge Promoted

- Registered the pre-Corgi foundation delivery as historical provenance for the existing architecture and memory pages.
- No Run Contract, tracker Issue, or post-hoc implementation work is claimed.

## Sources

- `rfcs/RFC-0001-project-foundation`
- `openspec/changes/archive/2026-08-28-establish-architecture-and-functional-design-charter`
- `openspec/changes/archive/2026-08-28-establish-architecture-and-functional-design-charter/evidence/manifest.json`
- `tools/corgi/migration-manifest.json`
