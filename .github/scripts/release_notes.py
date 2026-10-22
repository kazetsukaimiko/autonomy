#!/usr/bin/env python3
"""Take the changelog section for one version and name the jsonlink commit.

The section text is the generated changelog. This script does not edit that
file. The jsonlink line is release-note provenance, after the section.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

SECTION = re.compile(
    r"^(## \[(?:v)?(?P<version>[0-9]+\.[0-9]+\.[0-9]+)\][^\n]*\n.*?)(?=^## |\Z)",
    re.M | re.S,
)
SHA = re.compile(r"[0-9a-f]{40}")


class ReleaseNotesError(Exception):
    pass


def version_number(version: str) -> str:
    number = version[1:] if version.startswith("v") else version
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", number):
        raise ReleaseNotesError(f"not a version: {version}")
    return number


def changelog_section(changelog: str, version: str) -> str:
    number = version_number(version)
    for match in SECTION.finditer(changelog):
        if match.group("version") == number:
            return match.group(1).rstrip() + "\n"
    raise ReleaseNotesError(f"changelog has no section for {number}")


def release_notes(changelog: str, version: str, freedriver_sha: str) -> str:
    if SHA.fullmatch(freedriver_sha) is None:
        raise ReleaseNotesError("freedriver sha must be 40 lowercase hex characters")
    section = changelog_section(changelog, version)
    return f"{section.rstrip()}\n\njsonlink: kazetsukaimiko/freedriver@{freedriver_sha}\n"


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Write release notes for one version tag.")
    parser.add_argument("--changelog", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--freedriver-sha", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args(argv)
    try:
        text = release_notes(
            Path(args.changelog).read_text(),
            args.version,
            args.freedriver_sha,
        )
    except ReleaseNotesError as exc:
        print(f"release notes: {exc}", file=sys.stderr)
        return 1
    except OSError as exc:
        print(f"release notes: {exc}", file=sys.stderr)
        return 1
    Path(args.output).write_text(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
