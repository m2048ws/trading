#!/usr/bin/env python3

"""Parent-owned cross-backend writer exclusion.

The reservation is an advisory lock on an open descriptor for the canonical
repository root, held by the parent process. A delegated worker never inherits
the descriptor and no workspace artifact can close or replace the locked root
inode. Native brokers and the standalone script launcher therefore participate
in the same exclusion boundary without a worker-readable lease authority.
"""

from __future__ import annotations

import datetime
import json
import os
import pathlib
import fcntl
from typing import Any


ROOT = pathlib.Path(__file__).resolve().parents[2]
REPORTS = ROOT / ".agent" / "reports"
TRACE = REPORTS / "workflow-trace.jsonl"


class WriterBusy(RuntimeError):
    pass


class WriterReservation:
    def __init__(self, root: pathlib.Path = ROOT) -> None:
        self.root = root.resolve()
        self.descriptor: int | None = None

    def acquire(self) -> None:
        if self.descriptor is not None:
            raise RuntimeError("writer reservation is already held")
        candidate = os.open(self.root, os.O_RDONLY)
        try:
            fcntl.flock(candidate, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as exc:
            os.close(candidate)
            raise WriterBusy(
                "A primary-worktree writer is already active for this repository"
            ) from exc
        self.descriptor = candidate

    def close(self) -> None:
        if self.descriptor is not None:
            fcntl.flock(self.descriptor, fcntl.LOCK_UN)
            os.close(self.descriptor)
            self.descriptor = None

    def __enter__(self) -> "WriterReservation":
        self.acquire()
        return self

    def __exit__(self, *_: object) -> None:
        self.close()


def append_trace(event: str, identity: dict[str, Any]) -> None:
    REPORTS.mkdir(parents=True, exist_ok=True)
    record = {
        "timestamp": datetime.datetime.now(datetime.UTC).isoformat(),
        "event": event,
        **identity,
    }
    with TRACE.open("a") as stream:
        stream.write(json.dumps(record, sort_keys=True) + "\n")
