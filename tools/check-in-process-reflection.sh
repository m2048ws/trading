#!/bin/sh

set -eu

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
baseline="$repository_root/tools/in-process-reflection-baseline.tsv"
forbidden='MethodHandle|MethodHandles|MethodType|privateLookupIn|java\.lang\.invoke|java\.lang\.reflect|setAccessible|trySetAccessible|getDeclared(Constructor|Method|Field)'
failed=0
remaining=0

cd "$repository_root"

for source in $(rg --files -g '*/src/main/**/*.scala' | LC_ALL=C sort); do
  count=$(rg -o "$forbidden" "$source" 2>/dev/null | wc -l | tr -d ' ')
  allowed=$(awk -F '\t' -v source="$source" '$1 == source { print $2 }' "$baseline")
  if [ -z "$allowed" ]; then
    allowed=0
  fi

  if [ "$count" -gt "$allowed" ]; then
    printf '%s: found %s forbidden reflective tokens; migration allowance is %s\n' "$source" "$count" "$allowed" >&2
    rg -n "$forbidden" "$source" >&2 || true
    failed=1
  fi
  remaining=$((remaining + count))
done

if [ "$failed" -ne 0 ]; then
  exit 1
fi

if [ "$remaining" -eq 0 ]; then
  printf 'in-process reflection guard: pass (no production or benchmark sites)\n'
else
  printf 'in-process reflection guard: pass (%s reviewed migration tokens remain; no regression)\n' "$remaining"
fi
