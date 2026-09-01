# Corgi process-recovery build

`corgispec-4.0.0-rc2-money.2.tgz` is a project-local build from upstream tag `v4.0.0-rc2`, commit
`461555c2a93dd708f3815863fce80534f0b09f52`, plus the self-contained
`corgispec-4.0.0-rc2-money.2.patch`. The patch includes every added source file and applies cleanly to that exact
upstream commit; it supersedes the earlier Archive-only patch, which omitted its new compatibility-check source file.

The build adds four bounded Run Contract v3 behaviors:

- Apply start automatically accepts an ordinary pre-Run HEAD advance only with unchanged completed handoff/source
  identity, strict readiness, old-to-current ancestry, and proof that current HEAD is on the integration branch;
- only the first current Task Group may acknowledge a two-parent integration merge, and only when its first parent is
  the planning baseline and its second parent is the exact configured integration revision;
- readiness compares every OpenSpec `MODIFIED` requirement with its canonical requirement and fails with
  `ARCHIVE_DELTA_COMPATIBLE` when a canonical scenario identity would be dropped;
- `archive --request-repair --reason <text>` rolls back only reproducible pre-closeout artifacts into formal repair,
  while the Archive skill keeps integration and tracker closeout ordered behind the exact archive commit.

The Apply skill is implicitly discoverable in Codex and documents the guarded start/integration paths. The package has
the unique local version `4.0.0-rc2-money.2`; it does not masquerade as the upstream registry artifact.

Artifact identities:

- patch SHA-256: `bfdf72de1edd49678863955e51e6081c5f8bba75b8448bf3f21168ceb806348b`
- tarball SHA-256: `c814c573416e3cc60f345b0432cbb8b6e0ada54a6929580dee1750c771729156`
- npm integrity:
  `sha512-Gs9kVgOg+rezg8sVojrgqZ85htTQTL/3gEqvAThwVpAYIFKkqJRyIbj0hfnplE0hsYiX22cEOqVAo8uVl1iOZg==`

Before packing, the source passed type checking; all 1,214 runnable package tests (one skipped); native 27-skill and
58-file mirror validation; a smoke install with all 136 bundled asset checksums; and the production dependency audit
with zero vulnerabilities.
