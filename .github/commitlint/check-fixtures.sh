#!/usr/bin/env bash
# Accept fixtures/ok-*.txt and reject fixtures/bad-*.txt.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
fail=0

for file in fixtures/ok-*.txt; do
    if ! npx commitlint --edit "${PWD}/${file}" --verbose; then
        echo "expected pass: ${file}" >&2
        fail=1
    fi
done

for file in fixtures/bad-*.txt; do
    if npx commitlint --edit "${PWD}/${file}" --verbose; then
        echo "expected fail: ${file}" >&2
        fail=1
    else
        echo "rejected: ${file}"
    fi
done

exit "$fail"
