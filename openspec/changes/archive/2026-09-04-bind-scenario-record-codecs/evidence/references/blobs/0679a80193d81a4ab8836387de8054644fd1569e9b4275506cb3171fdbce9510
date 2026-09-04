#!/bin/sh

set -eu

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
fixture_root=$(mktemp -d "${TMPDIR:-/tmp}/in-process-reflection-test.XXXXXX")

cleanup() {
  rm -rf "$fixture_root"
}

trap cleanup 0 HUP INT TERM

mkdir -p "$fixture_root/tools" "$fixture_root/fixture/src/main/scala"
cp "$repository_root/tools/check-in-process-reflection.sh" "$fixture_root/tools/check-in-process-reflection.sh"
chmod +x "$fixture_root/tools/check-in-process-reflection.sh"
printf '%s\n' 'object ReflectionLeak { val lookup = MethodHandles.lookup() }' > \
  "$fixture_root/fixture/src/main/scala/ReflectionLeak.scala"

actual="$fixture_root/actual"
expected="$fixture_root/expected"

if "$fixture_root/tools/check-in-process-reflection.sh" > "$actual" 2>&1; then
  printf 'in-process reflection guard regression: expected the prohibited token to fail\n' >&2
  exit 1
else
  status=$?
fi

if [ "$status" -ne 1 ]; then
  printf 'in-process reflection guard regression: expected exit 1, got %s\n' "$status" >&2
  cat "$actual" >&2
  exit 1
fi

printf '%s\n' \
  'in-process reflection guard: fail (prohibited production or benchmark token found)' \
  'fixture/src/main/scala/ReflectionLeak.scala:1:object ReflectionLeak { val lookup = MethodHandles.lookup() }' \
  > "$expected"

if ! cmp -s "$expected" "$actual"; then
  printf 'in-process reflection guard regression: unexpected diagnostic\n' >&2
  diff -u "$expected" "$actual" >&2 || true
  exit 1
fi

printf 'in-process reflection guard regression: pass\n'
