#!/usr/bin/env bash
# Runs a pinned git-cliff and, on package, copies the changelog into the app classpath.
# Output stays under the module target/ directory and is never committed.
# A checkout with no reachable release tag (a shallow clone) leaves the file out.
# A release tag is vMAJOR.MINOR.PATCH or the older YYYY-MM_rN build stamp.
set -euo pipefail

VERSION=2.14.2

usage() {
    echo "usage: generate-changelog.sh generate OUTPUT | package OUTPUT CLASSES | print-tag REPO" >&2
    exit 2
}

# Nearest ancestor release tag. A version tag wins when it sits on the same commit
# as a build stamp. Prints nothing and returns 1 when no release tag is reachable.
current_release_tag() {
    local root="$1"
    local head="$2"
    local tag count rank best_count="" best_rank="" best_tag=""
    if ! git -C "$root" rev-parse --verify "${head}^{commit}" >/dev/null 2>&1; then
        return 1
    fi
    while IFS= read -r tag; do
        [[ -n "$tag" ]] || continue
        if [[ "$tag" =~ ^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
            rank=0
        elif [[ "$tag" =~ ^[0-9]{4}-[0-9]{2}_r[0-9]+$ ]]; then
            rank=1
        else
            continue
        fi
        if ! git -C "$root" merge-base --is-ancestor "$tag" "$head" 2>/dev/null; then
            continue
        fi
        count=$(git -C "$root" rev-list --count "${tag}..${head}")
        if [[ -z "$best_tag" ]] || ((count < best_count)) || { ((count == best_count)) && ((rank < best_rank)); }; then
            best_count=$count
            best_rank=$rank
            best_tag=$tag
        fi
    done < <(git -C "$root" tag --merged "$head")
    [[ -n "$best_tag" ]] || return 1
    printf '%s\n' "$best_tag"
}

sha512_for() {
    case "$1" in
        x86_64-unknown-linux-gnu)
            printf '%s\n' "26d1f7c8ea2400f2ccb0b2e7f321635f600b583bf4cd1f7afaa5e8c24068ad1088a2504e0a4d9f4333d4bb20a80329327d9462634343ee3af91e1a7fbdc6f18a"
            ;;
        aarch64-unknown-linux-gnu)
            printf '%s\n' "fd27768ea76fea8c3ba3f3247fef2f6604988b629f15e48bf40be5eaa08b8fef2e4087cc9c8af16bc522bc19cc369611fa7692101b513f63469ad58522708600"
            ;;
        *)
            echo "git-cliff: unsupported target $1" >&2
            exit 1
            ;;
    esac
}

install_git_cliff() {
    local tools="$1"
    local bin="$tools/git-cliff"
    if [[ -x "$bin" ]]; then
        local version
        version=$("$bin" --version 2>/dev/null || true)
        if [[ "$version" == "git-cliff ${VERSION}" ]]; then
            printf '%s\n' "$bin"
            return
        fi
    fi

    local arch target
    arch=$(uname -m)
    case "$arch" in
        x86_64) target=x86_64-unknown-linux-gnu ;;
        aarch64 | arm64) target=aarch64-unknown-linux-gnu ;;
        *)
            echo "git-cliff: unsupported architecture ${arch}" >&2
            exit 1
            ;;
    esac

    local sha name url tmp actual
    sha=$(sha512_for "$target")
    name="git-cliff-${VERSION}-${target}.tar.gz"
    url="https://github.com/orhun/git-cliff/releases/download/v${VERSION}/${name}"
    tmp=$(mktemp -d)
    curl -fsSL --retry 3 --retry-delay 2 -o "$tmp/$name" "$url"
    actual=$(sha512sum "$tmp/$name" | awk '{print $1}')
    if [[ "$actual" != "$sha" ]]; then
        rm -rf "$tmp"
        echo "git-cliff: checksum mismatch for ${name}" >&2
        exit 1
    fi
    tar -xzf "$tmp/$name" -C "$tmp"
    mkdir -p "$tools"
    install -m 0755 "$tmp/git-cliff-${VERSION}/git-cliff" "$bin"
    rm -rf "$tmp"
    printf '%s\n' "$bin"
}

generate() {
    local output="$1"
    local module_dir tools root bin tag tag_sha config
    module_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
    tools="$module_dir/target/git-cliff-bin"
    mkdir -p "$(dirname "$output")"
    rm -f "$output"

    bin=$(install_git_cliff "$tools")
    "$bin" --version

    root=$(git -C "$module_dir" rev-parse --show-toplevel 2>/dev/null || true)
    if [[ -z "$root" ]]; then
        echo "git-cliff: not a git checkout; changelog omitted"
        return 0
    fi
    config="$root/cliff.toml"
    if [[ ! -f "$config" ]]; then
        echo "git-cliff: missing ${config}" >&2
        exit 1
    fi

    if ! tag=$(current_release_tag "$root" HEAD); then
        echo "git-cliff: no release tag reachable from HEAD; changelog omitted"
        return 0
    fi
    tag_sha=$(git -C "$root" rev-parse "${tag}^{commit}" | tr '[:upper:]' '[:lower:]')
    if [[ ! "$tag_sha" =~ ^[0-9a-f]{40}$ ]]; then
        echo "git-cliff: refusing to generate from '${tag_sha}'" >&2
        exit 1
    fi

    "$bin" --config "$config" --repository "$root" --workdir "$root" --offline \
        --output "$output" -- "$tag_sha"
    if [[ ! -s "$output" ]]; then
        echo "git-cliff: empty changelog up to ${tag}; omitting it"
        rm -f "$output"
        return 0
    fi
    echo "git-cliff: wrote changelog up to ${tag}"
}

package_resource() {
    local output="$1"
    local classes="$2"
    rm -f "$classes"
    if [[ ! -s "$output" ]]; then
        echo "git-cliff: no generated changelog to package"
        return 0
    fi
    mkdir -p "$(dirname "$classes")"
    cp "$output" "$classes"
    echo "git-cliff: packaged changelog/CHANGELOG.md"
}

if [[ $# -lt 2 ]]; then
    usage
fi

case "$1" in
    generate)
        [[ $# -eq 2 ]] || usage
        generate "$2"
        ;;
    package)
        [[ $# -eq 3 ]] || usage
        package_resource "$2" "$3"
        ;;
    print-tag)
        [[ $# -eq 2 ]] || usage
        current_release_tag "$2" HEAD
        ;;
    *)
        usage
        ;;
esac
