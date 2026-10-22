#!/usr/bin/env python3
"""Check a vMAJOR.MINOR.PATCH tag against commits since the previous version tag.

fix bumps the patch version, feat the minor version, and a breaking change
the major version. A breaking change is a `!` on the subject or a
BREAKING CHANGE footer. Either marker bumps the major version. Commitlint
requires both on a new pull request. Other types do not bump the version.
The first version tag has no previous version tag, so its number is not
checked here. YYYY-MM_rN build stamps are not version tags.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

SEMVER = re.compile(r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)")
CONVENTIONAL = re.compile(
    r"^(?P<type>feat|fix|refactor|test|docs|chore|ci|build|perf)"
    r"(?:\([^)]+\))?(?P<bang>!)?: (?P<subject>.+)$"
)
BREAKING_FOOTER = re.compile(r"(?m)^BREAKING[ -]CHANGE:")
RANK = {"patch": 1, "minor": 2, "major": 3}


class VersionTagError(Exception):
    pass


def run_git(repo: Path, *args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", "-C", str(repo), *args],
        check=False,
        text=True,
        capture_output=True,
    )


def git_out(repo: Path, *args: str) -> str:
    result = run_git(repo, *args)
    if result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        raise VersionTagError(detail or f"git {' '.join(args)} failed")
    return result.stdout


def parse_version(tag: str) -> tuple[int, int, int] | None:
    match = SEMVER.fullmatch(tag)
    if match is None:
        return None
    return tuple(int(part) for part in match.groups())


def format_version(version: tuple[int, int, int]) -> str:
    return f"v{version[0]}.{version[1]}.{version[2]}"


def commit_sha(repo: Path, rev: str) -> str:
    try:
        return git_out(repo, "rev-parse", "--verify", f"{rev}^{{commit}}").strip()
    except VersionTagError as exc:
        raise VersionTagError(f"cannot resolve {rev}: {exc}") from exc


def previous_version_tag(repo: Path, head: str, current: str) -> str | None:
    names = [line for line in git_out(repo, "tag", "--merged", head).splitlines() if line]
    candidates: list[tuple[int, tuple[int, int, int], str]] = []
    for name in names:
        if name == current:
            continue
        version = parse_version(name)
        if version is None:
            continue
        count = int(git_out(repo, "rev-list", "--count", f"{name}..{head}").strip())
        # Nearest tag. On a tie, the greater version is the previous number.
        candidates.append((count, tuple(-part for part in version), name))
    if not candidates:
        return None
    candidates.sort()
    return candidates[0][2]


def classify(message: str) -> str | None:
    lines = message.strip().splitlines()
    if not lines:
        return None
    match = CONVENTIONAL.match(lines[0])
    bang = bool(match and match.group("bang"))
    if bang or BREAKING_FOOTER.search(message):
        return "major"
    if match is None:
        return None
    if match.group("type") == "feat":
        return "minor"
    if match.group("type") == "fix":
        return "patch"
    return None


def strongest_bump(messages: list[str]) -> str | None:
    best: str | None = None
    for message in messages:
        kind = classify(message)
        if kind is None:
            continue
        if best is None or RANK[kind] > RANK[best]:
            best = kind
    return best


def commit_messages(repo: Path, previous: str, head: str) -> list[str]:
    output = git_out(repo, "log", "--no-merges", f"--format=%B%x1e", f"{previous}..{head}")
    return [part.strip("\n") for part in output.split("\x1e") if part.strip()]


def next_version(previous: tuple[int, int, int], kind: str) -> tuple[int, int, int]:
    major, minor, patch = previous
    if kind == "major":
        return (major + 1, 0, 0)
    if kind == "minor":
        return (major, minor + 1, 0)
    if kind == "patch":
        return (major, minor, patch + 1)
    raise VersionTagError(f"unknown bump {kind}")


def require_on_master(repo: Path, head: str) -> None:
    probe = run_git(repo, "rev-parse", "--verify", "origin/master")
    if probe.returncode != 0:
        raise VersionTagError("origin/master is not available; fetch it before the version check")
    ancestor = run_git(repo, "merge-base", "--is-ancestor", head, "origin/master")
    if ancestor.returncode != 0:
        raise VersionTagError(f"{head} is not an ancestor of origin/master")


def check_version_tag(repo: Path | str, tag: str, head: str = "HEAD", require_master: bool = False) -> str:
    root = Path(repo)
    if parse_version(tag) is None:
        raise VersionTagError("version tags are vMAJOR.MINOR.PATCH with no leading zeros")
    head_sha = commit_sha(root, head)
    tagged = commit_sha(root, tag)
    if tagged != head_sha:
        raise VersionTagError(f"{tag} does not point at {head}")
    if require_master:
        require_on_master(root, head_sha)
    previous = previous_version_tag(root, head_sha, tag)
    if previous is None:
        return f"{tag} is the first version tag"
    kind = strongest_bump(commit_messages(root, previous, head_sha))
    if kind is None:
        raise VersionTagError(f"no fix, feat, or breaking change since {previous}")
    previous_version = parse_version(previous)
    if previous_version is None:
        raise VersionTagError(f"previous tag {previous} is not a version")
    expected = format_version(next_version(previous_version, kind))
    if tag != expected:
        raise VersionTagError(f"{tag} does not match the {kind} bump from {previous} to {expected}")
    return f"{tag} is the {kind} bump from {previous}"


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Check a version tag against conventional commits.")
    parser.add_argument("--repo", default=".")
    parser.add_argument("--tag", required=True)
    parser.add_argument("--head", default="HEAD")
    parser.add_argument("--require-master", action="store_true")
    args = parser.parse_args(argv)
    try:
        print(check_version_tag(args.repo, args.tag, args.head, args.require_master))
    except VersionTagError as exc:
        print(f"version tag: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
