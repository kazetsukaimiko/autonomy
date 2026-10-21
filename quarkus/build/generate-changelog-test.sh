#!/usr/bin/env bash
# Selects the nearest release tag without downloading git-cliff.
set -euo pipefail

script=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/generate-changelog.sh
root=$(mktemp -d)
trap 'rm -rf "$root"' EXIT

new_repo() {
    rm -rf "$root/repo"
    mkdir -p "$root/repo"
    git -C "$root/repo" init -b master >/dev/null
    git -C "$root/repo" config user.email test@example.com
    git -C "$root/repo" config user.name test
    git -C "$root/repo" config commit.gpgsign false
    git -C "$root/repo" config tag.gpgsign false
}

commit() {
    git -C "$root/repo" commit --allow-empty -m "$1" >/dev/null
}

expect() {
    local want=$1
    local got
    got=$(bash "$script" print-tag "$root/repo")
    if [[ "$got" != "$want" ]]; then
        echo "expected ${want}, got ${got}" >&2
        exit 1
    fi
}

expect_none() {
    if bash "$script" print-tag "$root/repo" >/dev/null; then
        echo "expected no release tag" >&2
        exit 1
    fi
}

new_repo
commit "no tag yet"
expect_none

new_repo
commit "stamp"
git -C "$root/repo" tag 2026-10_r1
expect 2026-10_r1

new_repo
commit "stamp"
git -C "$root/repo" tag 2026-10_r2
commit "version"
git -C "$root/repo" tag v0.1.0
expect v0.1.0

new_repo
commit "version"
git -C "$root/repo" tag v0.1.0
commit "later stamp"
git -C "$root/repo" tag 2026-10_r3
expect 2026-10_r3

new_repo
commit "both"
git -C "$root/repo" tag 2026-10_r4
git -C "$root/repo" tag v0.2.0
expect v0.2.0

new_repo
commit "stamp"
git -C "$root/repo" tag 2026-10_r5
commit "leading zero is not a version tag"
git -C "$root/repo" tag v01.2.3
expect 2026-10_r5

new_repo
commit "base"
git -C "$root/repo" tag 2026-10_r6
git -C "$root/repo" checkout -b side >/dev/null
commit "side"
git -C "$root/repo" tag v9.9.9
git -C "$root/repo" checkout master >/dev/null
expect 2026-10_r6

echo "generate-changelog tag selection passed"
