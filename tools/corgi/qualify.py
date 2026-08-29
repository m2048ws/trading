#!/usr/bin/env python3

"""Read-only activation preflight for the pinned Corgi pilot."""

from __future__ import annotations

import json
import pathlib
import subprocess
import sys
from typing import Any, Sequence


sys.dont_write_bytecode = True

ROOT = pathlib.Path(__file__).resolve().parents[2]
TOOL_ROOT = ROOT / "tools" / "corgi"
EXPECTED_VERSION = "4.0.0-rc2"
EXPECTED_INTEGRITY = "sha512-J+i7s4BthgIDvnMFTq/o/moRmnELa9EZ7So/a+3x+0CcUvzgASt/mESHX+/aTd3KuETk5onEkXZqZUhV/RX+ZQ=="


def run(args: Sequence[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        tuple(args),
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        close_fds=True,
    )


def load_json(path: pathlib.Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text())
    except (OSError, json.JSONDecodeError):
        return {}
    return value if isinstance(value, dict) else {}


def parse_json(text: str) -> Any:
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return None


def main() -> int:
    lock = load_json(TOOL_ROOT / "package-lock.json")
    packages = lock.get("packages") if isinstance(lock.get("packages"), dict) else {}
    pinned = packages.get("node_modules/corgispec")
    pinned = pinned if isinstance(pinned, dict) else {}
    pin_ok = (
        pinned.get("version") == EXPECTED_VERSION
        and pinned.get("integrity") == EXPECTED_INTEGRITY
        and pinned.get("license") == "MIT"
    )

    binary = TOOL_ROOT / "node_modules" / ".bin" / "corgispec"
    version_result = run((str(binary), "--version")) if binary.is_file() else None
    binary_ok = (
        version_result is not None
        and version_result.returncode == 0
        and version_result.stdout.strip() == EXPECTED_VERSION
    )

    bootstrap: dict[str, Any] = {}
    doctor: list[dict[str, Any]] = []
    if binary_ok:
        bootstrap_result = run(
            (
                str(binary),
                "bootstrap",
                "--target",
                str(ROOT),
                "--schema",
                "spec-driven",
                "--platform",
                "codex",
                "--scope",
                "local",
                "--migrate-v4",
                "--dry-run",
                "--json",
            )
        )
        parsed_bootstrap = parse_json(bootstrap_result.stdout)
        if isinstance(parsed_bootstrap, dict):
            bootstrap = parsed_bootstrap
        doctor_result = run((str(binary), "doctor", "--path", str(ROOT), "--json"))
        parsed_doctor = parse_json(doctor_result.stdout)
        if isinstance(parsed_doctor, list):
            doctor = [item for item in parsed_doctor if isinstance(item, dict)]

    active_changes = sorted(
        path.name
        for path in (ROOT / "openspec" / "changes").iterdir()
        if path.is_dir() and path.name != "archive"
    )
    bootstrap_ready = bootstrap.get("status") == "success"
    user_codex = next((item for item in doctor if item.get("name") == "codex skills"), {})
    github_auth = run(("gh", "auth", "status", "--hostname", "github.com"))
    github_ready = github_auth.returncode == 0
    config = load_json(TOOL_ROOT / "pilot.json")

    blockers = []
    if not pin_ok:
        blockers.append("The package lock does not match the qualified MIT release identity.")
    if not binary_ok:
        blockers.append("The project-local runtime is absent or has the wrong version; run npm ci in tools/corgi.")
    if active_changes:
        blockers.append("Corgi v4 migration requires zero active OpenSpec changes in every worktree.")
    if not bootstrap_ready:
        blockers.append("Corgi v4 bootstrap dry-run is not successful.")
    if user_codex.get("passed") is not True:
        blockers.append("Corgi's Codex skill installer is user-level and has not passed its writable-directory check.")
    if not github_ready:
        blockers.append("GitHub CLI authentication is not valid.")
    if config.get("enabled") is not True:
        blockers.append("The project-owned pilot admission switch is disabled.")

    output = {
        "schemaVersion": 1,
        "status": "ready" if not blockers else "blocked",
        "package": {
            "name": "corgispec",
            "version": EXPECTED_VERSION,
            "license": pinned.get("license"),
            "integrity": pinned.get("integrity"),
            "pinValid": pin_ok,
            "binaryValid": binary_ok,
        },
        "activeChanges": active_changes,
        "bootstrap": {
            "status": bootstrap.get("status", "unavailable"),
            "message": bootstrap.get("message", "No bootstrap result"),
        },
        "codexSkills": {
            "passed": user_codex.get("passed") is True,
            "message": user_codex.get("message", "No doctor result"),
        },
        "githubAuthenticated": github_ready,
        "pilotEnabled": config.get("enabled") is True,
        "blockers": blockers,
    }
    print(json.dumps(output, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
