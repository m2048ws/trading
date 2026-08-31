# Corgi Archive recovery build

`corgispec-4.0.0-rc2-archive-recovery.tgz` is a project-local build from upstream tag `v4.0.0-rc2`, commit
`461555c2a93dd708f3815863fce80534f0b09f52`, plus `corgispec-4.0.0-rc2-archive-recovery.patch`.

The patch adds three bounded Run Contract v3 behaviors:

- readiness compares every OpenSpec `MODIFIED` requirement with its current canonical requirement and fails with
  `ARCHIVE_DELTA_COMPATIBLE` when a canonical scenario identity would be dropped;
- `archive --request-repair --reason <text>` validates the exact run binding, removes only reproducible untracked
  Change evidence and a pending pre-closeout journal, and records `repair_required` without editing loop state;
- the Archive skill honors project-local PR/MR integration gates after local closeout, preserving the `archiving`
  Run and worktree until the exact archive commit passes the guarded merge/finalize boundary.

The npm package retains version `4.0.0-rc2` for the existing pilot adapter. Its identities are:

- SHA-256: `aecb212c6f35871408637fb3c0174eecb4789b7332a9ae83c530d82b34378e83`
- npm integrity: `sha512-0SbgGzIODXVQ79EI3kKLyazZT96BkidIMtda/dJM+Y/TltKOPQVvUnlmyEX/JkSDHWA2xCS1ucSXfTiwyqtPlg==`

Before packing, the source passed type checking, all 1,214 package tests after required mirror reconciliation, native
27-skill validation, the package smoke install, and the production dependency audit. The final integration-order update
also passed 63 focused lifecycle/archive/skill tests, mirror validation, and type checking. The live S-04 readiness
rehearsal identified all 13 omitted canonical scenarios that caused the original Archive failure.
