#!/bin/sh

set -eu

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
forbidden='MethodHandle|MethodHandles|MethodType|privateLookupIn|java\.lang\.invoke|java\.lang\.reflect|Class\.forName|StackWalker|setAccessible|trySetAccessible|getDeclared(Constructor|Method|Field)'
scratch=$(mktemp -d "${TMPDIR:-/tmp}/in-process-reflection.XXXXXX")
sources="$scratch/sources"
matches="$scratch/matches"

cleanup() {
  rm -rf "$scratch"
}

trap cleanup 0 HUP INT TERM

cd "$repository_root"

rg --files -g '*/src/main/**/*.scala' -g '*/src/main/**/*.java' | LC_ALL=C sort > "$sources"
: > "$matches"

while IFS= read -r source; do
  if rg -n --with-filename "$forbidden" -- "$source" >> "$matches"; then
    :
  else
    status=$?
    if [ "$status" -ne 1 ]; then
      printf 'in-process reflection guard: scan failed for %s (exit %s)\n' "$source" "$status" >&2
      exit "$status"
    fi
  fi
done < "$sources"

if [ -s "$matches" ]; then
  printf 'in-process reflection guard: fail (prohibited production or benchmark token found)\n' >&2
  cat "$matches" >&2
  exit 1
fi

printf 'in-process reflection guard: pass (no production or benchmark sites)\n'
