# Corgi process-recovery and lint-correctness build

`corgispec-4.0.0-rc2-money.3.tgz` is a project-local build from upstream tag `v4.0.0-rc2`, commit
`461555c2a93dd708f3815863fce80534f0b09f52`, plus the self-contained
`corgispec-4.0.0-rc2-money.3.patch`. The patch includes every added source file and applies cleanly to that exact
upstream commit; it supersedes the earlier local builds while retaining their recovery behavior.

The build adds four bounded Run Contract v3 behaviors and one lint correction:

- Apply start automatically accepts an ordinary pre-Run HEAD advance only with unchanged completed handoff/source
  identity, strict readiness, old-to-current ancestry, and proof that current HEAD is on the integration branch;
- only the first current Task Group may acknowledge a two-parent integration merge, and only when its first parent is
  the planning baseline and its second parent is the exact configured integration revision;
- readiness compares every OpenSpec `MODIFIED` requirement with its canonical requirement and fails with
  `ARCHIVE_DELTA_COMPATIBLE` when a canonical scenario identity would be dropped;
- `archive --request-repair --reason <text>` rolls back only reproducible pre-closeout artifacts into formal repair,
  while the Archive skill keeps integration and tracker closeout ordered behind the exact archive commit.
- archived-delivery lint counts acceptance criteria by first-column rows in `## Acceptance Evidence`, so narrative QA
  references do not become false duplicates while missing or genuinely duplicated rows still fail.

The Apply skill is implicitly discoverable in Codex and documents the guarded start/integration paths. The package has
the unique local version `4.0.0-rc2-money.3`; it does not masquerade as the upstream registry artifact.

Artifact identities:

- patch SHA-256: `fe7a8190f12308f684b05dacb39d67ae93e5f5c0470cee54753b3739d79fc638`
- tarball SHA-256: `b9cea28eea36bb79a7d34ea9459598462278e4b2431584af2db439891c64f7b8`
- npm integrity:
  `sha512-fD0HZAhy310gFnYse/WO2R+Q5JS78blCykk3UsUbvBRvf7GtDmXj292tL62o9BZlMak+DfOCc1dhkx8yP+LLCQ==`

Before packing, the source passed type checking; all 1,214 runnable package tests (one skipped); native 27-skill and
58-file mirror validation; a smoke install with all 136 bundled asset checksums; and the production dependency audit
with zero vulnerabilities.
