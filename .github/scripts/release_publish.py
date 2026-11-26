#!/usr/bin/env python3
"""Attach the build to the GitHub release release-please created, then promote it.

release-please publishes the release when the release pull request merges and
marks it a prerelease. The tag job uploads the archive and its checksum while
the release is a prerelease, and promotes it last. A release is promoted when
it is not a prerelease and lists both assets. Anything else is unfinished.

A promoted release is never changed. A rerun verifies what is published: the
checksum file against the archive, and the archive's build attestation.

    state    print promoted or unfinished; mark an unfinished release prerelease
    publish  set the notes, upload both assets, promote
    verify   check the published archive, checksum and attestation
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path

PROMOTED = "promoted"
UNFINISHED = "unfinished"
SIGNER_WORKFLOW = ".github/workflows/release.yml"


class ReleasePublishError(Exception):
    pass


def asset_names(tag: str) -> tuple[str, str]:
    archive = f"autonomy-{tag}-quarkus-app.tar.gz"
    return archive, f"{archive}.sha256"


def run(*command: str, cwd: Path | None = None) -> str:
    result = subprocess.run(command, check=False, text=True, capture_output=True, cwd=cwd)
    if result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        raise ReleasePublishError(f"{' '.join(command[:3])} failed: {detail}")
    return result.stdout


def classify(release: dict, tag: str) -> str:
    present = {asset["name"] for asset in release.get("assets", [])}
    if not release["isPrerelease"] and present.issuperset(asset_names(tag)):
        return PROMOTED
    return UNFINISHED


def read_state(repo: str, tag: str) -> str:
    """A tag with no release fails here, so a hand-pushed tag builds nothing."""
    view = run("gh", "release", "view", tag, "--repo", repo, "--json", "isPrerelease,assets")
    return classify(json.loads(view), tag)


def state(repo: str, tag: str) -> str:
    current = read_state(repo, tag)
    if current == UNFINISHED:
        # release-please publishes first and marks the prerelease in a later
        # step, so this job can read the release in between.
        run("gh", "release", "edit", tag, "--repo", repo, "--prerelease")
    return current


def publish(repo: str, tag: str, notes: Path, archive: Path, checksum: Path) -> None:
    expected = asset_names(tag)
    if (archive.name, checksum.name) != expected:
        raise ReleasePublishError(f"assets must be named {expected[0]} and {expected[1]}")
    for path in (notes, archive, checksum):
        if not path.is_file():
            raise ReleasePublishError(f"missing file: {path}")
    if state(repo, tag) == PROMOTED:
        raise ReleasePublishError(
            f"{tag} is already promoted; its assets stay as published. Re-run all jobs to verify them."
        )
    # The whole body is set each time, so a rerun leaves one jsonlink line.
    run("gh", "release", "edit", tag, "--repo", repo, "--notes-file", str(notes))
    run("gh", "release", "upload", tag, str(archive), str(checksum), "--repo", repo, "--clobber")
    run("gh", "release", "edit", tag, "--repo", repo, "--prerelease=false", "--latest")


def verify(repo: str, tag: str, commit: str) -> None:
    archive, checksum = asset_names(tag)
    with tempfile.TemporaryDirectory() as scratch:
        run(
            "gh", "release", "download", tag, "--repo", repo, "--dir", scratch,
            "--pattern", archive, "--pattern", checksum,
        )  # fmt: skip
        run("sha256sum", "-c", checksum, cwd=Path(scratch))
        run(
            "gh", "attestation", "verify", str(Path(scratch) / archive),
            "--repo", repo,
            "--signer-workflow", f"{repo}/{SIGNER_WORKFLOW}",
            "--source-ref", f"refs/tags/{tag}",
            "--source-digest", commit,
        )  # fmt: skip


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Finish or verify the GitHub release for a version tag.")
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"))
    commands = parser.add_subparsers(dest="command", required=True)
    for name in ("state", "publish", "verify"):
        command = commands.add_parser(name)
        command.add_argument("--tag", required=True)
        if name == "publish":
            command.add_argument("--notes", required=True, type=Path)
            command.add_argument("--archive", required=True, type=Path)
            command.add_argument("--checksum", required=True, type=Path)
        if name == "verify":
            command.add_argument("--commit", required=True)
    args = parser.parse_args(argv)
    if not args.repo:
        parser.error("--repo or GITHUB_REPOSITORY is required")
    try:
        if args.command == "state":
            current = state(args.repo, args.tag)
            print(current)
            output = os.environ.get("GITHUB_OUTPUT")
            if output:
                with open(output, "a") as handle:
                    handle.write(f"state={current}\n")
        elif args.command == "publish":
            publish(args.repo, args.tag, args.notes, args.archive, args.checksum)
            print(f"{args.tag} is promoted")
        else:
            verify(args.repo, args.tag, args.commit)
            print(f"{args.tag}: the published archive matches its checksum and attestation")
    except ReleasePublishError as exc:
        print(f"release publish: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
