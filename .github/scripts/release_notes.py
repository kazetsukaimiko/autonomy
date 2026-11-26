#!/usr/bin/env python3
"""Take the changelog section for one version and name the bundled jsonlink jar.

The section comes from the committed CHANGELOG.md that release-please writes.
This script does not edit that file. The jsonlink line is release-note
provenance, after the section: the Maven coordinate and the sha256 of the jar
in the quarkus-app lib directory, which is the jar the archive ships.
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path

# release-please writes `## [1.2.0](compare link) (date)`, and `## 1.1.0 (date)`
# for a release with no earlier version tag to compare against.
SECTION = re.compile(
    r"^(## (?:\[v?(?P<linked>[0-9]+\.[0-9]+\.[0-9]+)\]|(?P<plain>[0-9]+\.[0-9]+\.[0-9]+)(?=[ \n]))"
    r"[^\n]*\n.*?)(?=^## |\Z)",
    re.M | re.S,
)
# Quarkus fast-jar names a dependency <groupId>.<artifactId>-<version>.jar under lib/main.
JSONLINK_JAR = re.compile(r"io\.freedriver\.jsonlink-(?P<version>[0-9][A-Za-z0-9.+-]*)\.jar")


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
        if (match.group("linked") or match.group("plain")) == number:
            return match.group(1).rstrip() + "\n"
    raise ReleaseNotesError(f"changelog has no section for {number}")


def jsonlink_line(lib: Path) -> str:
    jars = sorted(path for path in lib.rglob("*.jar") if JSONLINK_JAR.fullmatch(path.name))
    if not jars:
        raise ReleaseNotesError(f"no jsonlink jar under {lib}")
    if len(jars) > 1:
        names = ", ".join(str(path.relative_to(lib)) for path in jars)
        raise ReleaseNotesError(f"more than one jsonlink jar under {lib}: {names}")
    version = JSONLINK_JAR.fullmatch(jars[0].name).group("version")
    digest = hashlib.sha256(jars[0].read_bytes()).hexdigest()
    return f"jsonlink: io.freedriver:jsonlink:{version} sha256:{digest}"


def release_notes(changelog: str, version: str, lib: Path) -> str:
    section = changelog_section(changelog, version)
    return f"{section.rstrip()}\n\n{jsonlink_line(lib)}\n"


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Write release notes for one version tag.")
    parser.add_argument("--changelog", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--lib", required=True, help="quarkus-app/lib of the build being released")
    parser.add_argument("--output", required=True)
    args = parser.parse_args(argv)
    try:
        text = release_notes(
            Path(args.changelog).read_text(),
            args.version,
            Path(args.lib),
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
