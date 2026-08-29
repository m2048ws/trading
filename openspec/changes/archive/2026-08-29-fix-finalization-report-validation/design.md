## Context

See `proposal.md` for motivation. Finalization reports share one schema for `commit_ready`, `committed`, and `blocked`, while logical validation supplies the status-dependent product constraints. The current validator applies the pre-commit `staged: true` constraint to both successful statuses, even though an authorized commit consumes the intended index and leaves a clean repository.

The fix is workflow-only. DEC-011 keeps every formal writer on the script backend, and this change must not alter backend selection, writer authority, archive order, or remediation lifecycle.

## Goals / Non-Goals

**Goals:**

- Make each successful finalization status describe one coherent repository/commit product.
- Bind an asserted created commit to the final reported repository HEAD.
- Exercise both the direct logical validator and the formal launcher validation boundary.

**Non-Goals:**

- Change the JSON schema shape or add report fields.
- Change native-writer eligibility or broker authority.
- Define recovery after post-archive remediation; that is a separate lifecycle question.
- Change Scala, SBT, production modules, dependencies, or public library APIs.

## Decisions

### 1. Split common success gates from status-specific Git/commit gates

Both successful statuses continue to require completed review/archive work, canonical spec update, passed validation, clean diff checks, and no unstaged or untracked files. `commit_ready` alone requires intended staged changes; `committed` instead requires no staged changes.

This models the actual sequence rather than weakening the staged guard globally. Treating `staged` as "work was staged at some earlier point" was rejected because the report field describes final Git state.

### 2. Validate the complete commit-state product

`commit_ready` requires no authorization, no created commit, and an empty hash. `committed` requires explicit authorization, a created commit, a non-empty hash, and equality between `git.head` and `commit.hash`.

Checking only that a hash is non-empty was rejected because it would not establish that the reported commit is the repository state being handed back to the steward.

### 3. Keep schema and lifecycle unchanged

The existing schema already represents every required boolean and string. The correction belongs in logical validation and focused tests. The separate inability of a remediation-ready report to describe an already archived change remains out of scope because changing it would alter INV-W3 and finalization recovery semantics rather than repair this status product.

### 4. Use claim-proportional workflow validation

Focused shell/Python/configuration checks, the agent workflow test executables, strict OpenSpec validation, and Git diff/state checks are proportional. Scala, SBT, Scalafmt, Scaladoc, and JMH are not applicable while the staged paths remain confined to workflow logic, tests, and OpenSpec artifacts.

## Risks / Trade-offs

- [Risk] A too-broad common success clause could reintroduce pre-commit assumptions for committed reports. → Mitigation: test valid and contradictory examples for each status through both validator layers.
- [Risk] Tightening commit-ready authorization/hash consistency could reject previously accepted contradictory fixtures. → Mitigation: specify the complete product explicitly and update only fixtures that claimed an impossible state.
- [Risk] The nearby post-archive remediation gap could tempt scope expansion. → Mitigation: retain it as a separately named lifecycle issue and make no remediation-report change here.
