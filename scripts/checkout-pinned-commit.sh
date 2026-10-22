#!/usr/bin/env bash
# Check out the commit named in a pin file and refuse a pin that is not on
# the named remote branch. The pin file holds one 40-character SHA and nothing else.
# Usage: checkout-pinned-commit.sh SHA_FILE DEST_DIR REMOTE_URL REMOTE_BRANCH
set -euo pipefail

if [[ $# -ne 4 ]]; then
    echo "usage: checkout-pinned-commit.sh SHA_FILE DEST_DIR REMOTE_URL REMOTE_BRANCH" >&2
    exit 2
fi

sha_file=$1
dest=$2
url=$3
remote_branch=$4

sha=$(tr -d '[:space:]' < "$sha_file")
if [[ ! "$sha" =~ ^[0-9a-f]{40}$ ]]; then
    echo "::error file=${sha_file}::${sha_file} must hold one full 40-character commit SHA" >&2
    exit 1
fi

rm -rf "$dest"
git clone --filter=blob:none --no-checkout "$url" "$dest"
git -C "$dest" fetch origin "$remote_branch"
if ! git -C "$dest" merge-base --is-ancestor "$sha" "origin/${remote_branch}"; then
    echo "::error file=${sha_file}::${sha} is not on ${remote_branch}" >&2
    exit 1
fi
git -C "$dest" checkout --detach "$sha"
