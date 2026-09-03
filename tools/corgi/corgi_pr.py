#!/usr/bin/env python3

"""Guarded projection between a Corgi v4 delivery and one GitHub draft PR.

This adapter deliberately owns no lifecycle state.  Corgi JSON, Git, and
GitHub remain authoritative.  The adapter validates their identities before
each bounded transition and fails closed on disagreement.
"""

from __future__ import annotations

import argparse
import fcntl
import json
import os
import pathlib
import re
import subprocess
import sys
from dataclasses import dataclass
from typing import Any, Iterable, Sequence


sys.dont_write_bytecode = True

MARKER_VERSION = 1
BODY_MARKER_RE = re.compile(
    r"<!-- corgi-pr:v(?P<version>\d+) "
    r"change=(?P<change>[a-z0-9][a-z0-9-]*) "
    r"issue=(?P<issue>\d+) "
    r"base=(?P<base>[A-Za-z0-9._/-]+) "
    r"head=(?P<head>[A-Za-z0-9._/-]+) -->"
)
REVIEW_MARKER_RE = re.compile(
    r"<!-- corgi-review:v(?P<version>\d+) "
    r"change=(?P<change>[a-z0-9][a-z0-9-]*) "
    r"sha=(?P<sha>[0-9a-f]{40,64}) -->"
)
REVIEW_REPORT_KEYS = {
    "schemaVersion",
    "verdict",
    "summary",
    "findings",
    "repository_unchanged",
}
REVIEW_FINDING_KEYS = {
    "id",
    "severity",
    "title",
    "location",
    "evidence",
    "smallest_remediation",
}
REVIEW_SEVERITIES = {"critical", "high", "medium", "low"}
CHANGE_RE = re.compile(r"[a-z0-9][a-z0-9-]{0,79}\Z")
SHA_RE = re.compile(r"[0-9a-f]{40,64}\Z")
SAFE_REF_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9._/-]{0,199}\Z")
SENSITIVE_RE = re.compile(
    r"(?i)(?:github_pat_[A-Za-z0-9_]+|gh[pousr]_[A-Za-z0-9]+|"
    r"bearer\s+[A-Za-z0-9._~+/=-]+|"
    r"(?:nonce|cas(?:[-_ ]?token)?|access[-_ ]?token|password|secret)"
    r"\s*[:=]\s*\S+)"
)
OPEN_PHASES = {
    "applying",
    "awaiting_verify",
    "awaiting_human_review",
    "awaiting_human_qa",
    "ready_for_archive",
    "archiving",
}
MANUAL_CI_WORKFLOW = "ci.yml"


class PilotError(RuntimeError):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code


@dataclass(frozen=True)
class Completed:
    args: tuple[str, ...]
    returncode: int
    stdout: str
    stderr: str


class Runner:
    def run(
        self,
        args: Sequence[str],
        *,
        cwd: pathlib.Path,
        accepted: Iterable[int] = (0,),
        input_text: str | None = None,
    ) -> Completed:
        result = subprocess.run(
            tuple(args),
            cwd=cwd,
            text=True,
            input=input_text,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            close_fds=True,
        )
        completed = Completed(
            tuple(args), result.returncode, result.stdout, result.stderr
        )
        if completed.returncode not in set(accepted):
            detail = completed.stderr.strip() or completed.stdout.strip()
            command = pathlib.Path(args[0]).name
            raise PilotError(
                "command_failed",
                f"{command} failed with status {completed.returncode}"
                + (f": {bounded(detail, 500)}" if detail else ""),
            )
        return completed


@dataclass(frozen=True)
class Authority:
    local_corgi_commits: bool
    fast_forward_wip_push: bool
    draft_pr_create_or_update: bool
    ready_for_review: bool
    request_reviewers: bool
    merge: bool
    remote_branch_delete: bool
    tag: bool
    publish: bool
    release: bool


@dataclass(frozen=True)
class PilotConfig:
    path: pathlib.Path
    enabled: bool
    blocked_reason: str
    corgi_version: str
    corgi_binary: pathlib.Path
    corgi_integrity: str
    repository: str
    remote: str
    base_branch: str
    branch_prefix: str
    worktree_root: pathlib.Path
    admitted_changes: frozenset[str]
    authority: Authority

    @classmethod
    def load(cls, root: pathlib.Path, path: pathlib.Path) -> "PilotConfig":
        try:
            raw = json.loads(path.read_text())
        except (OSError, json.JSONDecodeError) as exc:
            raise PilotError("config_invalid", f"Cannot read pilot config: {exc}")
        if not isinstance(raw, dict) or raw.get("schemaVersion") != 1:
            raise PilotError("config_invalid", "Pilot config schemaVersion must be 1")
        corgi = mapping(raw.get("corgi"), "corgi")
        github = mapping(raw.get("github"), "github")
        worktrees = mapping(raw.get("worktrees"), "worktrees")
        auth = mapping(raw.get("authority"), "authority")
        binary = inside(root, root / required_string(corgi, "binary"), "corgi.binary")
        worktree_root = inside(
            root, root / required_string(worktrees, "root"), "worktrees.root"
        )
        admitted_raw = raw.get("admittedChanges")
        if not isinstance(admitted_raw, list) or not all(
            isinstance(item, str) and CHANGE_RE.fullmatch(item)
            for item in admitted_raw
        ):
            raise PilotError(
                "config_invalid", "admittedChanges must contain kebab-case names"
            )
        repository = required_string(github, "repository")
        if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
            raise PilotError("config_invalid", "github.repository must be owner/name")
        remote = required_string(github, "remote")
        base = required_string(github, "baseBranch")
        prefix = required_string(github, "branchPrefix")
        if not SAFE_REF_RE.fullmatch(remote):
            raise PilotError("config_invalid", "github.remote is not a safe Git remote")
        if not SAFE_REF_RE.fullmatch(base) or not SAFE_REF_RE.fullmatch(prefix):
            raise PilotError("config_invalid", "GitHub base or branch prefix is unsafe")
        if not prefix.endswith("/"):
            raise PilotError("config_invalid", "github.branchPrefix must end with '/'")
        return cls(
            path=path,
            enabled=raw.get("enabled") is True,
            blocked_reason=str(raw.get("blockedReason") or "pilot is disabled"),
            corgi_version=required_string(corgi, "version"),
            corgi_binary=binary,
            corgi_integrity=required_string(corgi, "npmIntegrity"),
            repository=repository,
            remote=remote,
            base_branch=base,
            branch_prefix=prefix,
            worktree_root=worktree_root,
            admitted_changes=frozenset(admitted_raw),
            authority=Authority(
                local_corgi_commits=auth.get("localCorgiCommits") is True,
                fast_forward_wip_push=auth.get("fastForwardWipPush") is True,
                draft_pr_create_or_update=auth.get("draftPrCreateOrUpdate") is True,
                ready_for_review=auth.get("readyForReview") is True,
                request_reviewers=auth.get("requestReviewers") is True,
                merge=auth.get("merge") is True,
                remote_branch_delete=auth.get("remoteBranchDelete") is True,
                tag=auth.get("tag") is True,
                publish=auth.get("publish") is True,
                release=auth.get("release") is True,
            ),
        )

    def require_admitted(self, change: str) -> None:
        validate_change(change)
        if not self.enabled:
            raise PilotError("pilot_disabled", self.blocked_reason)
        if change not in self.admitted_changes:
            raise PilotError(
                "change_not_admitted",
                f"Change '{change}' is not explicitly admitted to the Corgi pilot",
            )

    def branch(self, change: str) -> str:
        value = f"{self.branch_prefix}{change}"
        if not SAFE_REF_RE.fullmatch(value):
            raise PilotError("branch_invalid", "Derived WIP branch is not a safe Git ref")
        return value


@dataclass(frozen=True)
class DeliveryStatus:
    raw: dict[str, Any]
    change: str
    phase: str
    final_revision: str | None
    integration_revision: str | None
    review_approved: bool
    issue_id: int
    issue_url: str
    groups_complete: int
    groups_total: int


@dataclass(frozen=True)
class PullRequest:
    number: int
    url: str
    state: str
    is_draft: bool
    head_ref: str
    head_owner: str
    base_ref: str
    head_oid: str
    body: str


@dataclass(frozen=True)
class Worktree:
    path: pathlib.Path
    head: str
    branch: str | None


class DescriptorLock:
    def __init__(self, path: pathlib.Path, label: str) -> None:
        self.path = path
        self.label = label
        self.descriptor: int | None = None

    def __enter__(self) -> "DescriptorLock":
        try:
            candidate = os.open(self.path, os.O_RDONLY)
            fcntl.flock(candidate, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as exc:
            os.close(candidate)
            raise PilotError("lock_busy", f"{self.label} is already active") from exc
        except OSError as exc:
            raise PilotError("lock_failed", f"Cannot lock {self.label}: {exc}") from exc
        self.descriptor = candidate
        return self

    def __exit__(self, *_: object) -> None:
        if self.descriptor is not None:
            fcntl.flock(self.descriptor, fcntl.LOCK_UN)
            os.close(self.descriptor)
            self.descriptor = None


class Adapter:
    def __init__(
        self,
        root: pathlib.Path,
        config: PilotConfig,
        runner: Runner | None = None,
    ) -> None:
        self.root = root.resolve()
        self.config = config
        self.runner = runner or Runner()
        self._resolved_corgi_binary: pathlib.Path | None = None

    def inspect(self, change: str | None = None) -> dict[str, Any]:
        result: dict[str, Any] = {
            "schemaVersion": 1,
            "enabled": self.config.enabled,
            "blockedReason": None
            if self.config.enabled
            else self.config.blocked_reason,
            "corgiVersion": self.config.corgi_version,
            "repository": self.config.repository,
            "baseBranch": self.config.base_branch,
            "branchPrefix": self.config.branch_prefix,
            "admittedChanges": sorted(self.config.admitted_changes),
            "authority": {
                "localCorgiCommits": self.config.authority.local_corgi_commits,
                "fastForwardWipPush": self.config.authority.fast_forward_wip_push,
                "draftPrCreateOrUpdate": self.config.authority.draft_pr_create_or_update,
                "readyForReview": self.config.authority.ready_for_review,
                "merge": self.config.authority.merge,
            },
        }
        if change is not None:
            validate_change(change)
            result["change"] = change
            result["admitted"] = change in self.config.admitted_changes
            result["expectedBranch"] = self.config.branch(change)
            result["expectedWorktree"] = str(
                self.config.worktree_root / change
            ).replace(str(self.root) + os.sep, "")
        return result

    def claim(self, change: str, *, owner: str, session: str) -> dict[str, Any]:
        self.config.require_admitted(change)
        if not self.config.authority.local_corgi_commits:
            raise PilotError(
                "local_commits_not_authorized", "Corgi local commit authority is disabled"
            )
        for label, value in (("owner", owner), ("session", session)):
            if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,99}", value):
                raise PilotError("claim_identity_invalid", f"{label} identity is invalid")
        with DescriptorLock(self.root, f"writer for {change}"):
            status = self.corgi_status(
                change, require_checkpoint=False, allow_planning=True
            )
            if status.phase != "planning_ready":
                raise PilotError(
                    "claim_exists",
                    f"Change already has lifecycle phase '{status.phase}'",
                )
            self.git_branch_identity(change)
            self.require_dependencies_closed(status.issue_id)
            raw = parse_json(
                self.runner.run(
                    (
                        str(self.corgi_binary()),
                        "apply",
                        change,
                        "--path",
                        str(self.root),
                        "--session",
                        session,
                        "--owner",
                        owner,
                        "--owner-kind",
                        "agent",
                        "--json",
                    ),
                    cwd=self.root,
                    accepted=(0, 1),
                ).stdout,
                "Corgi Apply",
            )
            if raw.get("status") != "ok":
                error = raw.get("error") if isinstance(raw.get("error"), dict) else {}
                raise PilotError(
                    "claim_failed",
                    bounded(str(error.get("message") or "Corgi Apply failed"), 500),
                )
            state = mapping(raw.get("state"), "Corgi Apply state")
            if state.get("changeName") != change or state.get("phase") != "applying":
                raise PilotError("claim_failed", "Corgi did not confirm the applying claim")
            token_path = self.root / ".corgi" / "adapter" / f"{change}-token.json"
            write_token_file(token_path, token_from_corgi(raw), self.root)
            return {
                "operation": "claimed",
                "change": change,
                "phase": "applying",
                "tokenFile": token_path.relative_to(self.root).as_posix(),
            }

    def admit(self, change: str) -> dict[str, Any]:
        """Create exactly one configured delivery branch and worktree.

        Git is the durable identity store.  No claim database or lease file is
        introduced; Corgi's Run owner/CAS becomes the exclusive change claim
        when Apply starts inside this worktree.
        """

        self.config.require_admitted(change)
        branch = self.config.branch(change)
        expected = (self.config.worktree_root / change).resolve()
        with self.integration_lock():
            worktrees = parse_worktrees(
                self.git("worktree", "list", "--porcelain").stdout
            )
            if not worktrees:
                raise PilotError("worktree_discovery_failed", "Git returned no worktrees")
            if worktrees[0].path.resolve() != self.root:
                raise PilotError(
                    "admission_not_primary",
                    "Admission must run from the repository's primary worktree",
                )
            at_path = [item for item in worktrees if item.path.resolve() == expected]
            on_branch = [item for item in worktrees if item.branch == branch]
            if len(at_path) == 1 and len(on_branch) == 1 and at_path[0] == on_branch[0]:
                return {
                    "operation": "already-admitted",
                    "change": change,
                    "branch": branch,
                    "worktree": expected.relative_to(self.root).as_posix(),
                    "head": at_path[0].head,
                }
            if at_path or on_branch:
                raise PilotError(
                    "worktree_identity_conflict",
                    "Configured branch or worktree is already bound differently",
                )
            if expected.exists():
                raise PilotError(
                    "worktree_path_occupied",
                    "Configured worktree path exists but is not registered with Git",
                )
            local_branch = self.git(
                "show-ref",
                "--verify",
                "--quiet",
                f"refs/heads/{branch}",
                accepted=(0, 1),
            )
            if local_branch.returncode == 0:
                raise PilotError(
                    "branch_identity_conflict",
                    "Configured branch already exists outside its worktree",
                )
            if self.remote_sha(f"refs/heads/{branch}") is not None:
                raise PilotError(
                    "remote_branch_conflict",
                    "Configured remote branch already exists before admission",
                )
            base_ref = f"refs/heads/{self.config.base_branch}"
            self.git("fetch", "--no-tags", self.config.remote, base_ref)
            base = self.git("rev-parse", "--verify", "FETCH_HEAD").stdout.strip()
            if not SHA_RE.fullmatch(base):
                raise PilotError("base_sha_invalid", "Fetched base SHA is invalid")
            self.git("worktree", "add", "-b", branch, str(expected), base)
            refreshed = parse_worktrees(
                self.git("worktree", "list", "--porcelain").stdout
            )
            matches = [
                item
                for item in refreshed
                if item.path.resolve() == expected and item.branch == branch
            ]
            if len(matches) != 1 or matches[0].head != base:
                raise PilotError(
                    "worktree_creation_unconfirmed",
                    "Git did not confirm the exact delivery worktree identity",
                )
            return {
                "operation": "admitted",
                "change": change,
                "branch": branch,
                "worktree": expected.relative_to(self.root).as_posix(),
                "head": base,
            }

    def open_or_sync(self, change: str, *, create: bool) -> dict[str, Any]:
        self.config.require_admitted(change)
        if not self.config.authority.fast_forward_wip_push:
            raise PilotError("push_not_authorized", "WIP push authority is disabled")
        if not self.config.authority.draft_pr_create_or_update:
            raise PilotError("draft_pr_not_authorized", "Draft PR authority is disabled")
        with DescriptorLock(self.root, f"writer for {change}"):
            archived_local = False
            try:
                status = self.corgi_status(change, require_checkpoint=True)
                branch, head = self.git_identity(change)
            except PilotError as exc:
                if create or exc.code != "corgi_contract_error":
                    raise
                branch = self.config.branch(change)
                status, head, _, _ = self.archived_status(
                    change, branch, worktree_state="present"
                )
                archived_local = True
            self.require_open_issue(status.issue_id)
            if archived_local:
                self.fast_forward_push_ref(branch, head)
            else:
                self.fast_forward_push(branch, head)
            prs = self.find_prs(branch)
            body = render_body(status, self.config, branch, head)
            assert_safe_projection(body, self.root)
            if not prs:
                if not create:
                    raise PilotError("pr_missing", "No matching draft PR exists")
                self.create_draft_pr(change, branch, body)
                prs = self.find_prs(branch)
            pr = require_one_pr(prs, self.config, status, branch)
            if pr.state != "OPEN":
                raise PilotError("pr_not_open", f"PR #{pr.number} is {pr.state.lower()}")
            if not pr.is_draft:
                raise PilotError("pr_not_draft", "WIP synchronization requires a draft PR")
            if pr.head_oid and pr.head_oid != head:
                raise PilotError(
                    "pr_head_stale",
                    f"PR head is {pr.head_oid}, expected freshly pushed {head}",
                )
            if pr.body != body:
                self.gh(
                    "pr",
                    "edit",
                    str(pr.number),
                    "--repo",
                    self.config.repository,
                    "--body",
                    body,
                )
            refreshed = require_one_pr(
                self.find_prs(branch), self.config, status, branch
            )
            return result_for("opened" if create else "synchronized", refreshed, head)

    def sync_archived(self, change: str) -> dict[str, Any]:
        """Recover publication after an archived Run removed its worktree.

        This path owns no lifecycle repair. It accepts only the exact local
        archive closeout commit proven by immutable archived evidence, then
        performs the same fast-forward push and bounded draft-PR projection as
        ordinary sync.
        """

        self.config.require_admitted(change)
        if not self.config.authority.fast_forward_wip_push:
            raise PilotError("push_not_authorized", "WIP push authority is disabled")
        if not self.config.authority.draft_pr_create_or_update:
            raise PilotError(
                "draft_pr_not_authorized", "Draft PR authority is disabled"
            )
        with self.integration_lock():
            branch = self.config.branch(change)
            status, head, archive_root, manifest_hash = self.archived_status(
                change, branch
            )
            prs = self.find_prs(branch)
            pr = require_one_pr(prs, self.config, status, branch)
            if pr.state != "OPEN":
                raise PilotError("pr_not_open", f"PR #{pr.number} is {pr.state.lower()}")
            if not pr.is_draft:
                raise PilotError(
                    "pr_not_draft", "Archived recovery may update only a draft PR"
                )
            self.require_archived_issue(status.issue_id)
            self.fast_forward_push_ref(branch, head)
            body = render_body(status, self.config, branch, head)
            assert_safe_projection(body, self.root)
            refreshed = require_one_pr(
                self.find_prs(branch), self.config, status, branch
            )
            if refreshed.head_oid != head:
                raise PilotError(
                    "pr_head_stale",
                    f"PR head is {refreshed.head_oid}, expected freshly pushed {head}",
                )
            if refreshed.body != body:
                self.gh(
                    "pr",
                    "edit",
                    str(refreshed.number),
                    "--repo",
                    self.config.repository,
                    "--body",
                    body,
                )
            confirmed = require_one_pr(
                self.find_prs(branch), self.config, status, branch
            )
            if confirmed.head_oid != head or confirmed.body != body:
                raise PilotError(
                    "pr_sync_unconfirmed",
                    "GitHub did not retain the exact archived head and bounded status",
                )
            return {
                "operation": "archived-synchronized",
                "pr": confirmed.number,
                "url": confirmed.url,
                "draft": confirmed.is_draft,
                "head": head,
                "archiveRoot": archive_root,
                "evidenceManifest": manifest_hash,
            }

    def merge_archived(self, change: str, *, authorized: bool) -> dict[str, Any]:
        """Merge an already-synchronized archive commit through immutable evidence."""

        self.config.require_admitted(change)
        if not authorized or not self.config.authority.merge:
            raise PilotError(
                "merge_not_authorized",
                "Merge requires both explicit invocation and enabled authority",
            )
        with self.integration_lock():
            branch = self.config.branch(change)
            status, head, _, _ = self.archived_status(change, branch)
            pr = require_one_pr(self.find_prs(branch), self.config, status, branch)
            if pr.state != "OPEN":
                raise PilotError("pr_not_open", f"PR #{pr.number} is {pr.state.lower()}")
            if pr.is_draft:
                raise PilotError("pr_is_draft", "PR must be explicitly marked ready first")
            if pr.head_oid != head:
                raise PilotError("pr_head_stale", "PR head does not match archive evidence")
            self.require_archived_issue(status.issue_id)
            self.require_dependencies_closed(status.issue_id)
            detail = self.pr_detail(pr.number)
            require_mergeable(
                detail,
                head,
                canonical_review_approved=status.review_approved,
            )
            self.gh(
                "pr",
                "merge",
                str(pr.number),
                "--repo",
                self.config.repository,
                "--merge",
                "--match-head-commit",
                head,
            )
            refreshed = self.pr_detail(pr.number)
            if (
                refreshed.get("state") != "MERGED"
                or refreshed.get("headRefOid") != head
            ):
                raise PilotError(
                    "merge_unconfirmed", "GitHub did not confirm the exact archived merge"
                )
            return {
                "operation": "archived-merged",
                "pr": pr.number,
                "url": pr.url,
                "head": head,
                "mergedAt": refreshed.get("mergedAt"),
            }

    def publish_review(
        self,
        change: str,
        report_path: pathlib.Path,
        reviewed_sha: str,
    ) -> dict[str, Any]:
        self.config.require_admitted(change)
        if not SHA_RE.fullmatch(reviewed_sha):
            raise PilotError("review_sha_invalid", "Reviewed SHA must be a full Git id")
        status = self.corgi_status(change, require_checkpoint=True)
        if status.phase != "awaiting_human_review":
            raise PilotError(
                "review_phase_invalid",
                "Automated review publication requires awaiting_human_review",
            )
        branch, head = self.git_identity(change)
        if head != reviewed_sha:
            raise PilotError("review_stale", "Local HEAD differs from reviewed SHA")
        pr = require_one_pr(self.find_prs(branch), self.config, status, branch)
        if pr.head_oid != reviewed_sha:
            raise PilotError("review_stale", "GitHub PR head differs from reviewed SHA")
        resolved_report = inside(
            self.root,
            report_path if report_path.is_absolute() else self.root / report_path,
            "review report",
        )
        report = load_review_report(resolved_report, self.root)
        body = render_review_comment(change, reviewed_sha, report, self.root)
        assert_safe_projection(body, self.root)
        comment_id = self.upsert_review_comment(pr.number, change, reviewed_sha, body)
        return {
            "operation": "review-published",
            "pr": pr.number,
            "url": pr.url,
            "head": reviewed_sha,
            "verdict": report["verdict"],
            "commentId": comment_id,
        }

    def ready(self, change: str, *, authorized: bool) -> dict[str, Any]:
        self.config.require_admitted(change)
        if not authorized or not self.config.authority.ready_for_review:
            raise PilotError(
                "ready_not_authorized",
                "Ready-for-review requires both explicit invocation and enabled authority",
            )
        with self.integration_lock():
            status, pr, head = self.integration_identity(change)
            if status.phase != "archiving" or status.integration_revision != head:
                raise PilotError(
                    "ready_phase_invalid",
                    "Ready requires the locally materialized Archive head",
                )
            self.require_open_issue(status.issue_id)
            self.require_dependencies_closed(status.issue_id)
            self.final_check_detail(
                self.pr_detail(pr.number),
                head,
                allow_draft=True,
                canonical_review_approved=status.review_approved,
            )
            if not pr.is_draft:
                return result_for("already-ready", pr, head)
            self.gh(
                "pr", "ready", str(pr.number), "--repo", self.config.repository
            )
            refreshed = require_one_pr(
                self.find_prs(pr.head_ref), self.config, status, pr.head_ref
            )
            return result_for("ready", refreshed, head)

    def merge(self, change: str, *, authorized: bool) -> dict[str, Any]:
        self.config.require_admitted(change)
        if not authorized or not self.config.authority.merge:
            raise PilotError(
                "merge_not_authorized",
                "Merge requires both explicit invocation and enabled authority",
            )
        with self.integration_lock():
            status, pr, head = self.integration_identity(change)
            if status.phase != "archiving" or status.integration_revision != head:
                raise PilotError("merge_phase_invalid", "Merge requires the Archive head")
            if pr.is_draft:
                raise PilotError("pr_is_draft", "PR must be explicitly marked ready first")
            self.require_open_issue(status.issue_id)
            self.require_dependencies_closed(status.issue_id)
            detail = self.pr_detail(pr.number)
            validated_detail = self.final_check_detail(
                detail,
                head,
                allow_draft=False,
                canonical_review_approved=status.review_approved,
            )
            require_mergeable(
                validated_detail,
                head,
                canonical_review_approved=status.review_approved,
            )
            self.gh(
                "pr",
                "merge",
                str(pr.number),
                "--repo",
                self.config.repository,
                "--merge",
                "--match-head-commit",
                head,
            )
            refreshed = self.pr_detail(pr.number)
            if refreshed.get("state") != "MERGED":
                raise PilotError("merge_unconfirmed", "GitHub did not confirm the merge")
            return {
                "operation": "merged",
                "pr": pr.number,
                "url": pr.url,
                "head": head,
            }

    def finalize(
        self,
        change: str,
        *,
        authorized: bool,
        token_file: pathlib.Path,
    ) -> dict[str, Any]:
        self.config.require_admitted(change)
        if not authorized:
            raise PilotError(
                "finalize_not_authorized", "Finalize requires explicit invocation"
            )
        with self.integration_lock():
            status, pr, head = self.integration_identity(change, allow_merged=True)
            branch = self.config.branch(change)
            delivery_root = self.archive_worktree(change, branch, head) or self.root
            detail = self.pr_detail(pr.number)
            if detail.get("state") != "MERGED" or detail.get("headRefOid") != head:
                raise PilotError(
                    "merge_not_confirmed", "Exact Corgi head is not confirmed merged"
                )
            self.require_dependencies_closed(status.issue_id)
            self.final_check_detail(
                detail,
                head,
                allow_draft=False,
                canonical_review_approved=status.review_approved,
            )
            resolved_token_file = inside(
                delivery_root,
                token_file
                if token_file.is_absolute()
                else delivery_root / token_file,
                "token file",
            )
            token = load_token_file(resolved_token_file, delivery_root)
            confirmed = self.corgi_archive(
                change, "--confirm-tracker", token, path=delivery_root
            )
            next_token = token_from_corgi(confirmed)
            write_token_file(resolved_token_file, next_token, delivery_root)
            finished = self.corgi_archive(
                change, "--finish", next_token, path=delivery_root
            )
            state = mapping(finished.get("state"), "Corgi archive state")
            if state.get("phase") != "archived":
                raise PilotError("archive_not_finished", "Corgi did not reach archived")
            resolved_token_file.unlink(missing_ok=True)
            return {
                "operation": "finalized",
                "pr": pr.number,
                "url": pr.url,
                "head": head,
                "phase": "archived",
            }

    def corgi_status(
        self,
        change: str,
        *,
        require_checkpoint: bool,
        allow_planning: bool = False,
    ) -> DeliveryStatus:
        binary = self.corgi_binary()
        completed = self.runner.run(
            (
                str(binary),
                "status",
                change,
                "--path",
                str(self.root),
                "--json",
            ),
            cwd=self.root,
            accepted=(0, 1),
        )
        raw = parse_json(completed.stdout, "Corgi status")
        if raw.get("status") == "contract_error" or completed.returncode != 0:
            error = raw.get("error") if isinstance(raw.get("error"), dict) else {}
            raise PilotError(
                "corgi_contract_error",
                bounded(str(error.get("message") or "Corgi status failed"), 500),
            )
        if raw.get("changeName") != change:
            raise PilotError("corgi_identity_mismatch", "Corgi returned another change")
        run_raw = raw.get("runContract")
        if run_raw is None:
            if not allow_planning or raw.get("planningComplete") is not True:
                raise PilotError("corgi_run_missing", "Corgi Run Contract is absent")
            run: dict[str, Any] = {}
            phase = "planning_ready"
        else:
            run = mapping(run_raw, "runContract")
            phase = required_string(run, "phase")
            # A repair successor already owns a real planning_ready Run. Claim
            # explicitly opts into that phase; publication/finalization paths
            # must continue to reject it until Apply has started.
            if phase == "planning_ready":
                if not allow_planning:
                    raise PilotError("corgi_phase_invalid", f"Unsupported Corgi phase '{phase}'")
            elif phase not in OPEN_PHASES and phase != "archived":
                raise PilotError("corgi_phase_invalid", f"Unsupported Corgi phase '{phase}'")
        contract = mapping(raw.get("contract"), "contract")
        tracker = mapping(contract.get("tracker"), "contract.tracker")
        if tracker.get("provider") != "github":
            raise PilotError("tracker_not_github", "Pilot change must use GitHub tracking")
        issue = mapping(tracker.get("issue"), "contract.tracker.issue")
        issue_raw = required_string(issue, "id")
        if not issue_raw.isdigit() or int(issue_raw) <= 0:
            raise PilotError("issue_invalid", "Corgi issue id must be a positive number")
        issue_url = required_string(issue, "url")
        expected_suffix = f"/{self.config.repository}/issues/{issue_raw}"
        if not issue_url.startswith("https://github.com/") or not issue_url.endswith(
            expected_suffix
        ):
            raise PilotError("issue_identity_mismatch", "Corgi issue URL is unexpected")
        groups = raw.get("taskGroups")
        if not isinstance(groups, list) or not groups:
            raise PilotError("task_groups_invalid", "Corgi returned no Task Groups")
        complete = sum(
            1
            for group in groups
            if isinstance(group, dict) and group.get("status") == "completed"
        )
        if require_checkpoint and complete == 0:
            raise PilotError(
                "apply_checkpoint_missing",
                "Draft PR publication starts after the first completed Task Group",
            )
        final_revision = run.get("finalRevision")
        if final_revision is not None and not (
            isinstance(final_revision, str) and SHA_RE.fullmatch(final_revision)
        ):
            raise PilotError("corgi_status_invalid", "Corgi finalRevision is invalid")
        integration_revision = final_revision
        archive_raw = run.get("archive")
        if isinstance(archive_raw, dict) and archive_raw.get("localCompleted") is True:
            closeout_commit = archive_raw.get("closeoutCommit")
            if not isinstance(closeout_commit, str) or not SHA_RE.fullmatch(
                closeout_commit
            ):
                raise PilotError(
                    "corgi_status_invalid",
                    "Locally archived Corgi status lacks a valid closeout commit",
                )
            integration_revision = closeout_commit
        review_raw = run.get("review")
        review_approved = (
            isinstance(review_raw, dict)
            and review_raw.get("decision") == "approve"
            and review_raw.get("finalRevision") == final_revision
            and isinstance(review_raw.get("reviewer"), str)
            and bool(str(review_raw.get("reviewer")).strip())
        )
        return DeliveryStatus(
            raw=raw,
            change=change,
            phase=phase,
            final_revision=final_revision,
            integration_revision=integration_revision,
            review_approved=review_approved,
            issue_id=int(issue_raw),
            issue_url=issue_url,
            groups_complete=complete,
            groups_total=len(groups),
        )

    def archived_status(
        self, change: str, branch: str, *, worktree_state: str = "absent"
    ) -> tuple[DeliveryStatus, str, str, str]:
        if worktree_state not in {"absent", "present"}:
            raise PilotError(
                "archive_worktree_state_invalid",
                "Archive worktree state must be absent or present",
            )
        local_ref = f"refs/heads/{branch}"
        head = self.git("rev-parse", "--verify", local_ref).stdout.strip()
        if not SHA_RE.fullmatch(head):
            raise PilotError("head_invalid", "Local archive branch head is invalid")
        archive_worktree = self.archive_worktree(change, branch, head)
        if worktree_state == "absent" and archive_worktree is not None:
            raise PilotError(
                "archived_worktree_present",
                "Use ordinary sync while the delivery worktree is registered",
            )
        if worktree_state == "present" and archive_worktree is None:
            raise PilotError(
                "archive_worktree_missing",
                "Local Archive integration requires the registered delivery worktree",
            )
        tree = self.git(
            "ls-tree",
            "-r",
            "--name-only",
            head,
            "--",
            "openspec/changes/archive",
        ).stdout.splitlines()
        binding_pattern = re.compile(
            rf"^openspec/changes/archive/[^/]*{re.escape(change)}/evidence/run-binding\.json$"
        )
        bindings = [path for path in tree if binding_pattern.fullmatch(path)]
        if len(bindings) != 1:
            raise PilotError(
                "archive_evidence_ambiguous",
                f"Expected one archived run binding for '{change}', found {len(bindings)}",
            )
        binding_path = bindings[0]
        archive_root = binding_path.removesuffix("/evidence/run-binding.json")
        binding = mapping(
            parse_json(
                self.git("show", f"{head}:{binding_path}").stdout,
                "archived run binding",
            ),
            "archived run binding",
        )
        if binding.get("schemaVersion") != 3 or binding.get("changeName") != change:
            raise PilotError(
                "archive_evidence_invalid", "Archived run binding identity is invalid"
            )
        final_revision = required_string(binding, "finalRevision")
        if not SHA_RE.fullmatch(final_revision):
            raise PilotError(
                "archive_evidence_invalid", "Archived final revision is invalid"
            )
        contract = mapping(binding.get("contract"), "archived contract")
        delivery_ref = required_string(contract, "deliveryRef")
        tracker = mapping(contract.get("tracker"), "archived tracker")
        if tracker.get("provider") != "github":
            raise PilotError(
                "tracker_not_github", "Archived recovery requires a GitHub binding"
            )
        issue = mapping(tracker.get("issue"), "archived tracker issue")
        issue_raw = required_string(issue, "id")
        if not issue_raw.isdigit() or int(issue_raw) <= 0:
            raise PilotError("issue_invalid", "Archived Issue id is invalid")
        issue_url = required_string(issue, "url")
        expected_suffix = f"/{self.config.repository}/issues/{issue_raw}"
        if not issue_url.startswith("https://github.com/") or not issue_url.endswith(
            expected_suffix
        ):
            raise PilotError(
                "issue_identity_mismatch", "Archived Issue URL is unexpected"
            )
        parents = self.git("rev-list", "--parents", "-n", "1", head).stdout.split()
        if parents != [head, final_revision]:
            raise PilotError(
                "archive_commit_invalid",
                "Archive closeout must be a direct child of the verified final revision",
            )
        subject = self.git("show", "-s", "--format=%s", head).stdout.strip()
        if subject != f"chore(corgi): archive {delivery_ref}":
            raise PilotError(
                "archive_commit_invalid", "Archive closeout commit subject is invalid"
            )
        active_probe = self.git(
            "ls-tree",
            "--name-only",
            head,
            "--",
            f"openspec/changes/{change}/.openspec.yaml",
        ).stdout.strip()
        if active_probe:
            raise PilotError(
                "archive_commit_invalid", "Archive branch still contains the active Change"
            )
        manifest_path = f"{archive_root}/evidence/manifest.json"
        manifest = mapping(
            parse_json(
                self.git("show", f"{head}:{manifest_path}").stdout,
                "archive evidence manifest",
            ),
            "archive evidence manifest",
        )
        manifest_hash = required_string(manifest, "manifestHash")
        if not re.fullmatch(r"sha256:[0-9a-f]{64}", manifest_hash):
            raise PilotError(
                "archive_evidence_invalid", "Evidence manifest hash is invalid"
            )
        expected_manifest = {
            "schemaVersion": 3,
            "changeName": change,
            "runId": required_string(binding, "runId"),
            "finalRevision": final_revision,
            "planningRevision": required_string(binding, "planningRevision"),
            "sourceDigest": required_string(contract, "sourceDigest"),
            "traceabilityDigest": required_string(contract, "traceabilityDigest"),
        }
        if any(manifest.get(key) != value for key, value in expected_manifest.items()):
            raise PilotError(
                "archive_evidence_invalid",
                "Evidence manifest does not match the archived run binding",
            )
        review_path = f"{archive_root}/evidence/human-review.json"
        review = mapping(
            parse_json(
                self.git("show", f"{head}:{review_path}").stdout,
                "archived Human Review",
            ),
            "archived Human Review",
        )
        review_approved = (
            review.get("decision") == "approve"
            and review.get("finalRevision") == final_revision
            and review.get("planningRevision") == binding.get("planningRevision")
            and isinstance(review.get("reviewer"), str)
            and bool(str(review.get("reviewer")).strip())
        )
        if not review_approved:
            raise PilotError(
                "archive_review_invalid",
                "Archived Human Review does not approve the verified revision",
            )
        group_pattern = re.compile(
            rf"^{re.escape(archive_root)}/evidence/groups/([1-9][0-9]*)/evidence\.json$"
        )
        groups = {match.group(1) for path in tree if (match := group_pattern.fullmatch(path))}
        if not groups:
            raise PilotError(
                "archive_evidence_invalid", "Archived evidence contains no Task Groups"
            )
        status = DeliveryStatus(
            raw={"archiveRoot": archive_root, "manifestHash": manifest_hash},
            change=change,
            phase="archiving" if archive_worktree is not None else "archived",
            final_revision=final_revision,
            integration_revision=head,
            review_approved=True,
            issue_id=int(issue_raw),
            issue_url=issue_url,
            groups_complete=len(groups),
            groups_total=len(groups),
        )
        return status, head, archive_root, manifest_hash

    def archive_worktree(
        self, change: str, branch: str, head: str
    ) -> pathlib.Path | None:
        worktrees = parse_worktrees(
            self.git("worktree", "list", "--porcelain").stdout
        )
        matches = [item for item in worktrees if item.branch == branch]
        if len(matches) > 1:
            raise PilotError(
                "archive_worktree_ambiguous",
                "Archive branch is registered in multiple worktrees",
            )
        if not matches:
            return None
        worktree = matches[0]
        expected = (self.config.worktree_root / change).resolve()
        if worktree.path != expected:
            raise PilotError(
                "archive_worktree_identity_mismatch",
                "Archive branch is not registered at the configured delivery worktree",
            )
        if worktree.head != head:
            raise PilotError(
                "archive_worktree_head_mismatch",
                "Registered delivery worktree does not match the archive branch head",
            )
        status = self.runner.run(
            ("git", "status", "--porcelain=v1", "--untracked-files=all"),
            cwd=worktree.path,
        ).stdout
        if status:
            raise PilotError(
                "worktree_dirty",
                "Local Archive integration requires a clean delivery worktree",
            )
        return worktree.path

    def git_identity(self, change: str) -> tuple[str, str]:
        branch, head = self.git_branch_identity(change)
        status = self.git(
            "status", "--porcelain=v1", "--untracked-files=all"
        ).stdout
        if status:
            raise PilotError("worktree_dirty", "WIP publication requires a clean worktree")
        return branch, head

    def git_branch_identity(self, change: str) -> tuple[str, str]:
        top = self.git("rev-parse", "--show-toplevel").stdout.strip()
        if pathlib.Path(top).resolve() != self.root:
            raise PilotError("git_root_mismatch", "Command root is not the Git worktree root")
        branch = self.git("branch", "--show-current").stdout.strip()
        expected = self.config.branch(change)
        if branch != expected:
            raise PilotError(
                "branch_identity_mismatch",
                f"Current branch '{branch}' is not expected branch '{expected}'",
            )
        if branch == self.config.base_branch:
            raise PilotError("protected_branch", "Refusing WIP operation on base branch")
        head = self.git("rev-parse", "--verify", "HEAD").stdout.strip()
        if not SHA_RE.fullmatch(head):
            raise PilotError("head_invalid", "Git returned an invalid HEAD")
        return branch, head

    def fast_forward_push(self, branch: str, head: str) -> None:
        ref = f"refs/heads/{branch}"
        before = self.remote_sha(ref)
        if before:
            self.git("fetch", "--no-tags", self.config.remote, ref)
            fetched = self.git("rev-parse", "--verify", "FETCH_HEAD").stdout.strip()
            if fetched != before:
                raise PilotError("remote_changed", "Remote branch changed during preflight")
            ancestry = self.git(
                "merge-base", "--is-ancestor", before, head, accepted=(0, 1)
            )
            if ancestry.returncode != 0:
                raise PilotError(
                    "remote_diverged", "Remote WIP branch is not an ancestor of local HEAD"
                )
        self.git(
            "push",
            "--set-upstream",
            self.config.remote,
            f"HEAD:{ref}",
        )
        after = self.remote_sha(ref)
        if after != head:
            raise PilotError("push_unconfirmed", "Remote WIP branch does not match HEAD")

    def fast_forward_push_ref(self, branch: str, head: str) -> None:
        ref = f"refs/heads/{branch}"
        local_ref = f"refs/heads/{branch}"
        resolved = self.git("rev-parse", "--verify", local_ref).stdout.strip()
        if resolved != head:
            raise PilotError("head_changed", "Local archive branch changed during preflight")
        before = self.remote_sha(ref)
        if before:
            self.git("fetch", "--no-tags", self.config.remote, ref)
            fetched = self.git("rev-parse", "--verify", "FETCH_HEAD").stdout.strip()
            if fetched != before:
                raise PilotError("remote_changed", "Remote branch changed during preflight")
            ancestry = self.git(
                "merge-base", "--is-ancestor", before, head, accepted=(0, 1)
            )
            if ancestry.returncode != 0:
                raise PilotError(
                    "remote_diverged", "Remote WIP branch is not an ancestor of local HEAD"
                )
        self.git("push", self.config.remote, f"{local_ref}:{ref}")
        after = self.remote_sha(ref)
        if after != head:
            raise PilotError("push_unconfirmed", "Remote WIP branch does not match HEAD")

    def require_archived_issue(self, issue_id: int) -> None:
        raw = mapping(
            parse_json(
                self.gh(
                    "issue",
                    "view",
                    str(issue_id),
                    "--repo",
                    self.config.repository,
                    "--json",
                    "number,state,labels",
                ).stdout,
                "archived Issue",
            ),
            "archived Issue",
        )
        if raw.get("number") != issue_id or raw.get("state") != "CLOSED":
            raise PilotError(
                "archive_issue_invalid", "Archived recovery requires the closed bound Issue"
            )
        labels = raw.get("labels")
        if not isinstance(labels, list) or "done" not in {
            str(label.get("name") or "")
            for label in labels
            if isinstance(label, dict)
        }:
            raise PilotError(
                "archive_issue_invalid", "Archived Issue lacks the done label"
            )

    def require_open_issue(self, issue_id: int) -> None:
        raw = mapping(
            parse_json(
                self.gh(
                    "issue",
                    "view",
                    str(issue_id),
                    "--repo",
                    self.config.repository,
                    "--json",
                    "number,state,labels",
                ).stdout,
                "open Issue",
            ),
            "open Issue",
        )
        if raw.get("number") != issue_id or raw.get("state") != "OPEN":
            raise PilotError(
                "archive_issue_invalid",
                "Local Archive integration requires the open bound Issue",
            )
        labels = raw.get("labels")
        if not isinstance(labels, list):
            raise PilotError("archive_issue_invalid", "Issue labels are invalid")
        if "done" in {
            str(label.get("name") or "")
            for label in labels
            if isinstance(label, dict)
        }:
            raise PilotError(
                "archive_issue_invalid",
                "Local Archive integration rejects an Issue already marked done",
            )

    def remote_sha(self, ref: str) -> str | None:
        output = self.git("ls-remote", "--heads", self.config.remote, ref).stdout
        rows = [line.split() for line in output.splitlines() if line.strip()]
        if not rows:
            return None
        if len(rows) != 1 or len(rows[0]) != 2 or rows[0][1] != ref:
            raise PilotError("remote_identity_ambiguous", "Remote branch lookup is ambiguous")
        sha = rows[0][0]
        if not SHA_RE.fullmatch(sha):
            raise PilotError("remote_sha_invalid", "Remote returned an invalid SHA")
        return sha

    def find_prs(self, branch: str) -> list[PullRequest]:
        fields = (
            "number,url,isDraft,state,headRefName,headRepositoryOwner,"
            "baseRefName,headRefOid,body"
        )
        result = self.gh(
            "pr",
            "list",
            "--repo",
            self.config.repository,
            "--state",
            "all",
            "--head",
            branch,
            "--json",
            fields,
        )
        raw = parse_json(result.stdout, "GitHub PR list")
        if not isinstance(raw, list):
            raise PilotError("github_response_invalid", "PR list must be an array")
        prs: list[PullRequest] = []
        for item in raw:
            data = mapping(item, "pull request")
            try:
                number = int(data["number"])
            except (KeyError, TypeError, ValueError) as exc:
                raise PilotError("github_response_invalid", "PR number is invalid") from exc
            owner_raw = data.get("headRepositoryOwner")
            head_owner = (
                str(owner_raw.get("login") or "")
                if isinstance(owner_raw, dict)
                else str(owner_raw or "")
            )
            prs.append(
                PullRequest(
                    number=number,
                    url=str(data.get("url") or ""),
                    state=str(data.get("state") or ""),
                    is_draft=data.get("isDraft") is True,
                    head_ref=str(data.get("headRefName") or ""),
                    head_owner=head_owner,
                    base_ref=str(data.get("baseRefName") or ""),
                    head_oid=str(data.get("headRefOid") or ""),
                    body=str(data.get("body") or ""),
                )
            )
        return prs

    def create_draft_pr(self, change: str, branch: str, body: str) -> None:
        self.gh(
            "pr",
            "create",
            "--repo",
            self.config.repository,
            "--draft",
            "--base",
            self.config.base_branch,
            "--head",
            branch,
            "--title",
            f"draft(corgi): {change}",
            "--body",
            body,
        )

    def upsert_review_comment(
        self, pr_number: int, change: str, reviewed_sha: str, body: str
    ) -> int:
        endpoint = f"repos/{self.config.repository}/issues/{pr_number}/comments"
        pages = parse_json(
            self.gh("api", "--paginate", "--slurp", endpoint).stdout,
            "GitHub comments",
        )
        if not isinstance(pages, list):
            raise PilotError("github_response_invalid", "Comment pages must be an array")
        comments = [
            comment
            for page in pages
            for comment in (page if isinstance(page, list) else [])
            if isinstance(comment, dict)
        ]
        viewer = mapping(
            parse_json(self.gh("api", "user").stdout, "GitHub viewer"),
            "GitHub viewer",
        )
        viewer_login = required_string(viewer, "login")
        matches = []
        for comment in comments:
            marker = REVIEW_MARKER_RE.search(str(comment.get("body") or ""))
            if marker and marker.group("change") == change:
                author = comment.get("user")
                author_login = (
                    str(author.get("login") or "")
                    if isinstance(author, dict)
                    else ""
                )
                if author_login != viewer_login:
                    raise PilotError(
                        "review_comment_owner_mismatch",
                        "Review marker belongs to another GitHub user",
                    )
                matches.append(comment)
        if len(matches) > 1:
            raise PilotError("review_comment_ambiguous", "Multiple review markers exist")
        if matches:
            comment_id = int(matches[0].get("id"))
            response = self.gh(
                "api",
                "--method",
                "PATCH",
                f"repos/{self.config.repository}/issues/comments/{comment_id}",
                "-f",
                f"body={body}",
            )
        else:
            response = self.gh(
                "api",
                "--method",
                "POST",
                endpoint,
                "-f",
                f"body={body}",
            )
        created = parse_json(response.stdout, "GitHub review comment")
        try:
            return int(mapping(created, "comment")["id"])
        except (KeyError, TypeError, ValueError) as exc:
            raise PilotError("github_response_invalid", "Comment id is invalid") from exc

    def integration_identity(
        self, change: str, *, allow_merged: bool = False
    ) -> tuple[DeliveryStatus, PullRequest, str]:
        try:
            status = self.corgi_status(change, require_checkpoint=True)
            branch, head = self.git_identity(change)
        except PilotError as exc:
            if exc.code != "corgi_contract_error":
                raise
            branch = self.config.branch(change)
            status, head, _, _ = self.archived_status(
                change, branch, worktree_state="present"
            )
        pr = require_one_pr(self.find_prs(branch), self.config, status, branch)
        allowed_states = {"OPEN", "MERGED"} if allow_merged else {"OPEN"}
        if pr.state not in allowed_states:
            raise PilotError("pr_state_invalid", f"Unexpected PR state {pr.state}")
        if pr.head_oid != head:
            raise PilotError("pr_head_stale", "PR head does not match local HEAD")
        if status.integration_revision != head:
            raise PilotError(
                "integration_head_stale",
                "Local HEAD does not match the Corgi integration revision",
            )
        return status, pr, head

    def require_dependencies_closed(self, issue_id: int) -> None:
        raw = parse_json(
            self.gh(
                "issue",
                "view",
                str(issue_id),
                "--repo",
                self.config.repository,
                "--json",
                "blockedBy",
            ).stdout,
            "GitHub issue dependencies",
        )
        blocked_by_value = mapping(raw, "issue dependencies").get("blockedBy")
        if isinstance(blocked_by_value, list):
            blocked_by = blocked_by_value
        elif isinstance(blocked_by_value, dict):
            blocked_by = blocked_by_value.get("nodes")
            if not isinstance(blocked_by, list):
                raise PilotError(
                    "dependency_response_invalid",
                    "blockedBy.nodes must be an array",
                )
        else:
            raise PilotError(
                "dependency_response_invalid",
                "blockedBy must be an array or connection object",
            )
        open_dependencies = [
            item
            for item in blocked_by
            if isinstance(item, dict) and str(item.get("state") or "").upper() != "CLOSED"
        ]
        if open_dependencies:
            numbers = ", ".join(
                f"#{item.get('number')}" for item in open_dependencies[:10]
            )
            raise PilotError("dependencies_blocking", f"Open dependencies: {numbers}")

    def pr_detail(self, number: int) -> dict[str, Any]:
        fields = (
            "number,url,isDraft,state,headRefName,baseRefName,headRefOid,"
            "mergeStateStatus,reviewDecision,statusCheckRollup,mergedAt"
        )
        return mapping(
            parse_json(
                self.gh(
                    "pr",
                    "view",
                    str(number),
                    "--repo",
                    self.config.repository,
                    "--json",
                    fields,
                ).stdout,
                "GitHub PR detail",
            ),
            "pull request",
        )

    def final_check_detail(
        self,
        detail: dict[str, Any],
        head: str,
        *,
        allow_draft: bool,
        canonical_review_approved: bool = False,
    ) -> dict[str, Any]:
        try:
            require_review_and_checks(
                detail,
                head,
                allow_draft=allow_draft,
                canonical_review_approved=canonical_review_approved,
            )
            return detail
        except PilotError as exc:
            if exc.code != "checks_missing":
                raise

        endpoint = (
            f"repos/{self.config.repository}/actions/workflows/"
            f"{MANUAL_CI_WORKFLOW}/runs"
        )
        raw = parse_json(
            self.gh(
                "api",
                "--method",
                "GET",
                endpoint,
                "-f",
                "event=workflow_dispatch",
                "-f",
                f"head_sha={head}",
                "-f",
                "per_page=100",
            ).stdout,
            "manual CI workflow runs",
        )
        require_exact_manual_ci(raw, head)
        validated = dict(detail)
        validated["statusCheckRollup"] = [
            {
                "name": f"{MANUAL_CI_WORKFLOW} workflow_dispatch",
                "conclusion": "SUCCESS",
            }
        ]
        return validated

    def corgi_archive(
        self,
        change: str,
        phase_flag: str,
        token: dict[str, Any],
        *,
        path: pathlib.Path | None = None,
    ) -> dict[str, Any]:
        operation_root = (path or self.root).resolve()
        args = [
            str(self.corgi_binary()),
            "archive",
            change,
            "--path",
            str(operation_root),
            "--json",
            phase_flag,
            "--run-id",
            str(token["runId"]),
            "--session",
            str(token["sessionId"]),
            "--state-revision",
            str(token["stateRevision"]),
            "--nonce",
            str(token["nonce"]),
        ]
        raw = parse_json(
            self.runner.run(args, cwd=operation_root, accepted=(0, 1)).stdout,
            "Corgi archive",
        )
        if raw.get("status") != "ok":
            error = raw.get("error") if isinstance(raw.get("error"), dict) else {}
            raise PilotError(
                "corgi_archive_failed",
                bounded(str(error.get("message") or "Archive transition failed"), 500),
            )
        return raw

    def corgi_binary(self) -> pathlib.Path:
        if self._resolved_corgi_binary is not None:
            return self._resolved_corgi_binary
        configured = self.config.corgi_binary.resolve()
        candidates = [configured]
        try:
            relative = configured.relative_to(self.root)
        except ValueError as exc:
            raise PilotError("corgi_path_invalid", "Corgi binary escapes the worktree") from exc
        if not configured.is_file():
            worktrees = parse_worktrees(
                self.git("worktree", "list", "--porcelain").stdout
            )
            if worktrees:
                candidates.append(worktrees[0].path / relative)
        binary = next((candidate for candidate in candidates if candidate.is_file()), None)
        if binary is None:
            raise PilotError(
                "corgi_not_installed",
                "Pinned Corgi binary is missing; run npm ci in tools/corgi in the primary worktree",
            )
        version = self.runner.run((str(binary), "--version"), cwd=self.root)
        if version.stdout.strip() != self.config.corgi_version:
            raise PilotError(
                "corgi_version_mismatch",
                "Installed Corgi binary does not match the checked-in exact version",
            )
        self._resolved_corgi_binary = binary
        return binary

    def integration_lock(self) -> DescriptorLock:
        common = self.git("rev-parse", "--git-common-dir").stdout.strip()
        path = pathlib.Path(common)
        if not path.is_absolute():
            path = (self.root / path).resolve()
        if not path.is_dir():
            raise PilotError("git_common_dir_invalid", "Git common directory is missing")
        return DescriptorLock(path, "repository integration")

    def git(
        self, *args: str, accepted: Iterable[int] = (0,)
    ) -> Completed:
        return self.runner.run(("git", *args), cwd=self.root, accepted=accepted)

    def gh(self, *args: str) -> Completed:
        return self.runner.run(("gh", *args), cwd=self.root)


def mapping(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise PilotError("contract_invalid", f"{label} must be an object")
    return value


def required_string(value: dict[str, Any], key: str) -> str:
    item = value.get(key)
    if not isinstance(item, str) or not item.strip():
        raise PilotError("contract_invalid", f"{key} must be a non-empty string")
    return item.strip()


def inside(root: pathlib.Path, candidate: pathlib.Path, label: str) -> pathlib.Path:
    resolved_root = root.resolve()
    resolved = candidate.resolve()
    if resolved != resolved_root and resolved_root not in resolved.parents:
        raise PilotError("config_invalid", f"{label} escapes the repository")
    return resolved


def validate_change(change: str) -> None:
    if not CHANGE_RE.fullmatch(change):
        raise PilotError("change_invalid", "Change must be lower kebab-case")


def parse_json(text: str, label: str) -> Any:
    try:
        return json.loads(text)
    except json.JSONDecodeError as exc:
        raise PilotError("response_not_json", f"{label} did not return JSON") from exc


def parse_worktrees(text: str) -> list[Worktree]:
    result: list[Worktree] = []
    for block in re.split(r"\n\s*\n", text.strip()):
        if not block.strip():
            continue
        fields: dict[str, str] = {}
        for line in block.splitlines():
            key, separator, value = line.partition(" ")
            fields[key] = value if separator else ""
        path = fields.get("worktree")
        head = fields.get("HEAD")
        if not path or not head or not SHA_RE.fullmatch(head):
            raise PilotError("worktree_response_invalid", "Malformed Git worktree record")
        branch_value = fields.get("branch")
        branch = (
            branch_value.removeprefix("refs/heads/")
            if branch_value and branch_value.startswith("refs/heads/")
            else None
        )
        result.append(Worktree(pathlib.Path(path).resolve(), head, branch))
    return result


def bounded(value: str, limit: int) -> str:
    normalized = " ".join(value.split())
    return normalized if len(normalized) <= limit else normalized[: limit - 1] + "…"


def marker_for(change: str, issue: int, base: str, head: str) -> str:
    return (
        f"<!-- corgi-pr:v{MARKER_VERSION} change={change} issue={issue} "
        f"base={base} head={head} -->"
    )


def render_body(
    status: DeliveryStatus, config: PilotConfig, branch: str, head: str
) -> str:
    return "\n".join(
        [
            marker_for(
                status.change,
                status.issue_id,
                config.base_branch,
                branch,
            ),
            "## Corgi delivery",
            "",
            f"- Change: `{status.change}`",
            f"- Issue: #{status.issue_id}",
            f"- Phase: `{status.phase}`",
            f"- Head: `{head}`",
            f"- Task Groups: {status.groups_complete}/{status.groups_total}",
            "",
            "This bounded status block is generated from Corgi, Git, and GitHub. "
            "OpenSpec artifacts and local structured evidence remain authoritative.",
        ]
    ) + "\n"


def require_one_pr(
    prs: list[PullRequest],
    config: PilotConfig,
    status: DeliveryStatus,
    branch: str,
) -> PullRequest:
    if len(prs) != 1:
        raise PilotError(
            "pr_identity_ambiguous",
            f"Expected exactly one PR for '{branch}', found {len(prs)}",
        )
    pr = prs[0]
    expected_owner = config.repository.split("/", 1)[0]
    if (
        pr.head_ref != branch
        or pr.head_owner != expected_owner
        or pr.base_ref != config.base_branch
    ):
        raise PilotError("pr_identity_mismatch", "PR head or base branch is unexpected")
    markers = list(BODY_MARKER_RE.finditer(pr.body))
    if not markers:
        raise PilotError("pr_marker_missing", "Existing PR lacks the Corgi marker")
    if len(markers) != 1 or markers[0].start() != 0:
        raise PilotError(
            "pr_marker_ambiguous", "Existing PR marker is duplicated or misplaced"
        )
    marker = markers[0]
    expected = {
        "version": str(MARKER_VERSION),
        "change": status.change,
        "issue": str(status.issue_id),
        "base": config.base_branch,
        "head": branch,
    }
    if any(marker.group(key) != value for key, value in expected.items()):
        raise PilotError("pr_marker_mismatch", "Existing PR marker identity differs")
    return pr


def result_for(operation: str, pr: PullRequest, head: str) -> dict[str, Any]:
    return {
        "operation": operation,
        "pr": pr.number,
        "url": pr.url,
        "draft": pr.is_draft,
        "head": head,
    }


def assert_safe_projection(body: str, root: pathlib.Path) -> None:
    if str(root) in body or SENSITIVE_RE.search(body):
        raise PilotError(
            "projection_sensitive", "Projection contains private path or sensitive data"
        )
    if any(ord(character) < 32 and character not in "\n\t" for character in body):
        raise PilotError("projection_invalid", "Projection contains control characters")
    if len(body.encode()) > 32_000:
        raise PilotError("projection_too_large", "Projection exceeds the bounded size")


def load_review_report(path: pathlib.Path, root: pathlib.Path) -> dict[str, Any]:
    resolved = inside(root, path if path.is_absolute() else root / path, "review report")
    try:
        if resolved.stat().st_size > 256_000:
            raise PilotError("review_report_invalid", "Review report is too large")
        raw = json.loads(resolved.read_text())
    except PilotError:
        raise
    except (OSError, json.JSONDecodeError) as exc:
        raise PilotError("review_report_invalid", f"Cannot read review report: {exc}")
    if not isinstance(raw, dict):
        raise PilotError("review_report_invalid", "Review report must be an object")
    report = raw
    if set(report) != REVIEW_REPORT_KEYS:
        raise PilotError(
            "review_report_invalid",
            "Review report fields do not match the Corgi PR review contract",
        )
    if report.get("schemaVersion") != 1:
        raise PilotError("review_report_invalid", "Review schemaVersion must be 1")
    if report.get("verdict") not in {"ready", "blocked"}:
        raise PilotError("review_report_invalid", "Review verdict is invalid")
    summary = report.get("summary")
    if not isinstance(summary, str) or not summary.strip() or len(summary) > 5_000:
        raise PilotError("review_report_invalid", "Review summary is invalid")
    if report.get("repository_unchanged") is not True:
        raise PilotError("review_report_invalid", "Reviewer changed its worktree")
    findings = report.get("findings")
    if not isinstance(findings, list) or len(findings) > 100:
        raise PilotError("review_report_invalid", "Review findings are invalid")
    if report["verdict"] == "ready" and findings:
        raise PilotError("review_report_invalid", "Ready review must have no findings")
    if report["verdict"] == "blocked" and not findings:
        raise PilotError("review_report_invalid", "Blocked review requires findings")
    seen_ids: set[str] = set()
    for raw_finding in findings:
        if not isinstance(raw_finding, dict) or set(raw_finding) != REVIEW_FINDING_KEYS:
            raise PilotError(
                "review_report_invalid",
                "Review finding fields do not match the Corgi PR review contract",
            )
        finding_id = raw_finding.get("id")
        if (
            not isinstance(finding_id, str)
            or not finding_id.strip()
            or len(finding_id) > 80
            or finding_id in seen_ids
        ):
            raise PilotError("review_report_invalid", "Review finding id is invalid")
        seen_ids.add(finding_id)
        if raw_finding.get("severity") not in REVIEW_SEVERITIES:
            raise PilotError("review_report_invalid", "Review severity is invalid")
        for field in (
            "title",
            "location",
            "evidence",
            "smallest_remediation",
        ):
            value = raw_finding.get(field)
            if not isinstance(value, str) or not value.strip() or len(value) > 5_000:
                raise PilotError(
                    "review_report_invalid", f"Review finding {field} is invalid"
                )
    return report


def safe_review_field(value: Any, root: pathlib.Path, limit: int) -> str:
    text = bounded(str(value or ""), limit)
    if not text:
        return "unspecified"
    if str(root) in text or SENSITIVE_RE.search(text):
        return "[redacted from PR projection]"
    return text.replace("`", "'")


def render_review_comment(
    change: str, reviewed_sha: str, report: dict[str, Any], root: pathlib.Path
) -> str:
    lines = [
        f"<!-- corgi-review:v{MARKER_VERSION} change={change} sha={reviewed_sha} -->",
        "## Automated whole-change review",
        "",
        f"- Reviewed SHA: `{reviewed_sha}`",
        f"- Verdict: **{report['verdict']}**",
        f"- Summary: {safe_review_field(report.get('summary'), root, 500)}",
    ]
    findings = report.get("findings", [])
    if findings:
        lines.extend(["", "### Actionable findings", ""])
        for finding in findings:
            item = mapping(finding, "review finding")
            finding_id = safe_review_field(item.get("id"), root, 40)
            severity = safe_review_field(item.get("severity"), root, 20)
            title = safe_review_field(item.get("title"), root, 180)
            remediation = safe_review_field(
                item.get("smallest_remediation"), root, 500
            )
            lines.append(f"- **{finding_id} [{severity}] {title}** — {remediation}")
    else:
        lines.extend(["", "No actionable findings."])
    lines.extend(
        [
            "",
            "The complete structured report remains local. This verdict becomes stale "
            "if the PR head changes.",
        ]
    )
    return "\n".join(lines) + "\n"


def require_mergeable(
    detail: dict[str, Any],
    head: str,
    *,
    canonical_review_approved: bool = False,
) -> None:
    require_review_and_checks(
        detail,
        head,
        allow_draft=False,
        canonical_review_approved=canonical_review_approved,
    )
    if detail.get("mergeStateStatus") not in {"CLEAN", "HAS_HOOKS"}:
        raise PilotError(
            "pr_not_mergeable", "GitHub merge state is not clean at the exact head"
        )


def require_review_and_checks(
    detail: dict[str, Any],
    head: str,
    *,
    allow_draft: bool,
    canonical_review_approved: bool = False,
) -> None:
    if detail.get("headRefOid") != head:
        raise PilotError("pr_head_stale", "PR head changed before the transition")
    if not allow_draft and detail.get("isDraft") is True:
        raise PilotError("pr_is_draft", "PR is still draft")
    if detail.get("reviewDecision") != "APPROVED" and not canonical_review_approved:
        raise PilotError(
            "review_missing",
            "Neither GitHub nor canonical Corgi review approves the exact head",
        )
    checks = detail.get("statusCheckRollup")
    if not isinstance(checks, list) or not checks:
        raise PilotError("checks_missing", "No final GitHub checks were reported")
    failing = []
    for check in checks:
        item = mapping(check, "status check")
        conclusion = str(item.get("conclusion") or item.get("state") or "").upper()
        if conclusion not in {"SUCCESS", "NEUTRAL", "SKIPPED"}:
            failing.append(str(item.get("name") or item.get("context") or "unknown"))
    if failing:
        raise PilotError("checks_not_green", f"Checks not green: {', '.join(failing)}")


def require_exact_manual_ci(value: Any, head: str) -> None:
    response = mapping(value, "manual CI workflow runs")
    runs = response.get("workflow_runs")
    if not isinstance(runs, list) or not runs:
        raise PilotError("checks_missing", "No exact-head manual CI run was reported")
    total_count = response.get("total_count")
    if not isinstance(total_count, int) or total_count != len(runs):
        raise PilotError(
            "checks_ambiguous",
            "Exact-head manual CI runs exceed or disagree with the bounded response",
        )
    failing = []
    for run in runs:
        item = mapping(run, "manual CI workflow run")
        if item.get("head_sha") != head or item.get("event") != "workflow_dispatch":
            raise PilotError(
                "checks_identity_mismatch",
                "Manual CI response does not match the sealed head and event",
            )
        status = str(item.get("status") or "").lower()
        conclusion = str(item.get("conclusion") or "").lower()
        if status != "completed" or conclusion != "success":
            failing.append(str(item.get("name") or item.get("id") or "manual CI"))
    if failing:
        raise PilotError(
            "checks_not_green", f"Manual CI not green: {', '.join(failing)}"
        )


def load_token_file(path: pathlib.Path, root: pathlib.Path) -> dict[str, Any]:
    resolved = inside(root, path if path.is_absolute() else root / path, "token file")
    relative = resolved.relative_to(root).as_posix()
    if relative != ".corgi" and not relative.startswith(".corgi/"):
        raise PilotError("token_path_invalid", "Archive token file must be under .corgi/")
    try:
        raw = json.loads(resolved.read_text())
    except (OSError, json.JSONDecodeError) as exc:
        raise PilotError("token_invalid", f"Cannot read archive token: {exc}")
    token = mapping(raw, "archive token")
    return validate_token(token)


def validate_token(token: dict[str, Any]) -> dict[str, Any]:
    run_id = required_string(token, "runId")
    session_id = required_string(token, "sessionId")
    nonce = required_string(token, "nonce")
    revision = token.get("stateRevision")
    if not isinstance(revision, int) or revision < 0:
        raise PilotError("token_invalid", "stateRevision must be a nonnegative integer")
    if any(len(value) > 200 for value in (run_id, session_id, nonce)):
        raise PilotError("token_invalid", "Archive token field is too long")
    return {
        "runId": run_id,
        "sessionId": session_id,
        "stateRevision": revision,
        "nonce": nonce,
    }


def token_from_corgi(raw: dict[str, Any]) -> dict[str, Any]:
    return validate_token(mapping(raw.get("token"), "Corgi token"))


def write_token_file(
    path: pathlib.Path, token: dict[str, Any], root: pathlib.Path
) -> None:
    resolved = inside(root, path, "token file")
    relative = resolved.relative_to(root).as_posix()
    if not relative.startswith(".corgi/"):
        raise PilotError("token_path_invalid", "Token handoff must be under .corgi/")
    resolved.parent.mkdir(parents=True, exist_ok=True)
    temporary = resolved.with_name(f".{resolved.name}.{os.getpid()}.tmp")
    descriptor = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    try:
        with os.fdopen(descriptor, "w") as stream:
            json.dump(validate_token(token), stream, sort_keys=True)
            stream.write("\n")
        os.replace(temporary, resolved)
        os.chmod(resolved, 0o600)
    finally:
        if temporary.exists():
            temporary.unlink()


def discover_root(explicit: str | None, runner: Runner) -> pathlib.Path:
    if explicit:
        return pathlib.Path(explicit).resolve()
    result = runner.run(("git", "rev-parse", "--show-toplevel"), cwd=pathlib.Path.cwd())
    return pathlib.Path(result.stdout.strip()).resolve()


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(
        description="Guarded Corgi v4 to GitHub draft-PR adapter"
    )
    result.add_argument("--root", help="Git worktree root")
    result.add_argument("--json", action="store_true", help="Machine-readable output")
    commands = result.add_subparsers(dest="command", required=True)
    inspect = commands.add_parser("inspect", help="Show dormant/admission state")
    inspect.add_argument("change", nargs="?")
    admit = commands.add_parser("admit", help="Create one delivery branch/worktree")
    admit.add_argument("change")
    claim = commands.add_parser("claim", help="Check dependencies and start one Corgi Run claim")
    claim.add_argument("change")
    claim.add_argument("--owner", required=True)
    claim.add_argument("--session", required=True)
    for name in ("open", "sync"):
        command = commands.add_parser(name)
        command.add_argument("change")
    sync_archived = commands.add_parser(
        "sync-archived",
        help="Recover exact archive-commit publication after worktree cleanup",
    )
    sync_archived.add_argument("change")
    merge_archived = commands.add_parser(
        "merge-archived",
        help="Merge an exact archive commit using immutable Corgi evidence",
    )
    merge_archived.add_argument("change")
    merge_archived.add_argument("--authorize-merge", action="store_true")
    review = commands.add_parser("review")
    review.add_argument("change")
    review.add_argument("--report", required=True)
    review.add_argument("--reviewed-sha", required=True)
    ready = commands.add_parser("ready")
    ready.add_argument("change")
    ready.add_argument("--authorize-ready", action="store_true")
    merge = commands.add_parser("merge")
    merge.add_argument("change")
    merge.add_argument("--authorize-merge", action="store_true")
    finalize = commands.add_parser("finalize")
    finalize.add_argument("change")
    finalize.add_argument("--authorize-finalize", action="store_true")
    finalize.add_argument("--token-file", required=True)
    return result


def main(argv: Sequence[str] | None = None) -> int:
    args = parser().parse_args(argv)
    runner = Runner()
    try:
        root = discover_root(args.root, runner)
        config_path = root / "tools/corgi/pilot.json"
        config = PilotConfig.load(root, config_path)
        adapter = Adapter(root, config, runner)
        if args.command == "inspect":
            output = adapter.inspect(args.change)
        elif args.command == "admit":
            output = adapter.admit(args.change)
        elif args.command == "claim":
            output = adapter.claim(
                args.change, owner=args.owner, session=args.session
            )
        elif args.command in {"open", "sync"}:
            output = adapter.open_or_sync(args.change, create=args.command == "open")
        elif args.command == "sync-archived":
            output = adapter.sync_archived(args.change)
        elif args.command == "merge-archived":
            output = adapter.merge_archived(
                args.change, authorized=args.authorize_merge
            )
        elif args.command == "review":
            output = adapter.publish_review(
                args.change, pathlib.Path(args.report), args.reviewed_sha
            )
        elif args.command == "ready":
            output = adapter.ready(args.change, authorized=args.authorize_ready)
        elif args.command == "merge":
            output = adapter.merge(args.change, authorized=args.authorize_merge)
        elif args.command == "finalize":
            output = adapter.finalize(
                args.change,
                authorized=args.authorize_finalize,
                token_file=pathlib.Path(args.token_file),
            )
        else:
            raise PilotError("command_invalid", "Unknown command")
        if args.json:
            print(json.dumps({"status": "ok", **output}, sort_keys=True))
        else:
            print(json.dumps(output, indent=2, sort_keys=True))
        return 0
    except PilotError as exc:
        payload = {"status": "error", "error": {"code": exc.code, "message": str(exc)}}
        if args.json:
            print(json.dumps(payload, sort_keys=True))
        else:
            print(f"corgi-pr: {exc.code}: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
