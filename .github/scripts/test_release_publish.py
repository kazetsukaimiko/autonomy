import hashlib
import json
import os
import stat
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import release_publish
from release_publish import ReleasePublishError

REPO = "kazetsukaimiko/autonomy"
TAG = "v1.1.0"
COMMIT = "a7038690bf0fd7395375e291e1a8ed55e5c1ae82"
ARCHIVE = f"autonomy-{TAG}-quarkus-app.tar.gz"
CHECKSUM = f"{ARCHIVE}.sha256"

# Stands in for gh. The release is a JSON file, published assets are a folder,
# and every call is appended to a log. sha256sum is the real one.
FAKE_GH = r"""#!/usr/bin/env python3
import json, os, shutil, sys
from pathlib import Path

home = Path(os.environ["FAKE_GH_HOME"])
args = sys.argv[1:]
with open(home / "calls.log", "a") as log:
    log.write(json.dumps(args) + "\n")
release_file = home / "release.json"
if not release_file.exists():
    print("release not found", file=sys.stderr)
    sys.exit(1)
release = json.loads(release_file.read_text())

def save():
    release_file.write_text(json.dumps(release))

if args[:2] == ["release", "view"]:
    print(json.dumps({"isPrerelease": release["isPrerelease"],
                      "assets": [{"name": name} for name in sorted(release["assets"])]}))
elif args[:2] == ["release", "edit"]:
    if "--prerelease" in args:
        release["isPrerelease"] = True
    if "--prerelease=false" in args:
        release["isPrerelease"] = False
    if "--notes-file" in args:
        release["body"] = Path(args[args.index("--notes-file") + 1]).read_text()
    save()
elif args[:2] == ["release", "upload"]:
    files = [arg for arg in args[3:] if not arg.startswith("--") and arg != release["repo"]]
    for name in files:
        if Path(name).name in release["assets"] and "--clobber" not in args:
            print("asset exists", file=sys.stderr)
            sys.exit(1)
        shutil.copy(name, home / "published" / Path(name).name)
        release["assets"].append(Path(name).name)
    release["assets"] = sorted(set(release["assets"]))
    save()
elif args[:2] == ["release", "download"]:
    target = Path(args[args.index("--dir") + 1])
    for name in release["assets"]:
        shutil.copy(home / "published" / name, target / name)
elif args[:2] == ["attestation", "verify"]:
    sys.exit(0 if release["attested"] else 1)
else:
    print("unexpected gh call", file=sys.stderr)
    sys.exit(2)
"""


class ReleasePublishTest(unittest.TestCase):
    def setUp(self) -> None:
        scratch = tempfile.TemporaryDirectory()
        self.addCleanup(scratch.cleanup)
        self.home = Path(scratch.name)
        (self.home / "published").mkdir()
        (self.home / "bin").mkdir()
        gh = self.home / "bin" / "gh"
        gh.write_text(FAKE_GH)
        gh.chmod(gh.stat().st_mode | stat.S_IXUSR)
        environment = mock.patch.dict(
            os.environ,
            {"PATH": f"{self.home / 'bin'}{os.pathsep}{os.environ['PATH']}", "FAKE_GH_HOME": str(self.home)},
        )
        environment.start()
        self.addCleanup(environment.stop)
        # What this run built.
        self.dist = self.home / "dist"
        self.dist.mkdir()
        self.archive = self.dist / ARCHIVE
        self.archive.write_bytes(b"this run's build")
        self.checksum = self.dist / CHECKSUM
        self.checksum.write_text(f"{hashlib.sha256(b"this run's build").hexdigest()}  {ARCHIVE}\n")
        self.notes = self.dist / "release-notes.md"
        self.notes.write_text("## 1.1.0\n\njsonlink: io.freedriver:jsonlink:1.0.0 sha256:00\n")

    def release(self, prerelease: bool, published: dict[str, bytes] | None = None, attested: bool = True) -> None:
        published = published or {}
        for name, content in published.items():
            (self.home / "published" / name).write_bytes(content)
        (self.home / "release.json").write_text(
            json.dumps(
                {"repo": REPO, "isPrerelease": prerelease, "assets": sorted(published), "attested": attested, "body": ""}
            )
        )

    def promoted(self, archive: bytes = b"first build", attested: bool = True, checksum_of: bytes | None = None) -> None:
        digest = hashlib.sha256(archive if checksum_of is None else checksum_of).hexdigest()
        self.release(False, {ARCHIVE: archive, CHECKSUM: f"{digest}  {ARCHIVE}\n".encode()}, attested)

    def stored(self) -> dict:
        return json.loads((self.home / "release.json").read_text())

    def calls(self) -> list[list[str]]:
        log = self.home / "calls.log"
        return [json.loads(line) for line in log.read_text().splitlines()] if log.exists() else []

    def writes(self) -> list[list[str]]:
        return [call for call in self.calls() if call[:2] in (["release", "edit"], ["release", "upload"])]

    def published_bytes(self) -> dict[str, bytes]:
        return {path.name: path.read_bytes() for path in (self.home / "published").iterdir()}

    def publish(self) -> None:
        release_publish.publish(REPO, TAG, self.notes, self.archive, self.checksum)

    # state

    def test_a_new_prerelease_is_unfinished(self) -> None:
        self.release(True)
        self.assertEqual(release_publish.state(REPO, TAG), "unfinished")
        self.assertTrue(self.stored()["isPrerelease"])

    def test_not_prerelease_without_assets_is_unfinished_and_marked_prerelease(self) -> None:
        # release-please has published the release and not yet marked it.
        self.release(False)
        self.assertEqual(release_publish.state(REPO, TAG), "unfinished")
        self.assertTrue(self.stored()["isPrerelease"])
        self.assertEqual(self.writes(), [["release", "edit", TAG, "--repo", REPO, "--prerelease"]])

    def test_not_prerelease_with_one_asset_is_unfinished(self) -> None:
        self.release(False, {ARCHIVE: b"first build"})
        self.assertEqual(release_publish.state(REPO, TAG), "unfinished")
        self.assertTrue(self.stored()["isPrerelease"])

    def test_not_prerelease_with_both_assets_is_promoted_and_left_alone(self) -> None:
        self.promoted()
        self.assertEqual(release_publish.state(REPO, TAG), "promoted")
        self.assertEqual(self.writes(), [])

    def test_a_tag_without_a_release_fails(self) -> None:
        # A hand-pushed tag has no release, so the job stops before it builds.
        with self.assertRaisesRegex(ReleasePublishError, "gh release view failed"):
            release_publish.state(REPO, TAG)
        self.assertEqual(release_publish.main(["--repo", REPO, "state", "--tag", TAG]), 1)

    def test_state_command_writes_the_step_output(self) -> None:
        self.release(True)
        output = self.home / "output"
        with mock.patch.dict(os.environ, {"GITHUB_OUTPUT": str(output), "GITHUB_REPOSITORY": REPO}):
            self.assertEqual(release_publish.main(["state", "--tag", TAG]), 0)
        self.assertEqual(output.read_text(), "state=unfinished\n")

    # publish

    def test_publish_sets_notes_uploads_then_promotes(self) -> None:
        self.release(True)
        self.publish()
        self.assertEqual(
            self.writes(),
            [
                ["release", "edit", TAG, "--repo", REPO, "--prerelease"],
                ["release", "edit", TAG, "--repo", REPO, "--notes-file", str(self.notes)],
                ["release", "upload", TAG, str(self.archive), str(self.checksum), "--repo", REPO, "--clobber"],
                ["release", "edit", TAG, "--repo", REPO, "--prerelease=false", "--latest"],
            ],
        )
        stored = self.stored()
        self.assertFalse(stored["isPrerelease"])
        self.assertEqual(stored["assets"], [ARCHIVE, CHECKSUM])
        self.assertEqual(release_publish.state(REPO, TAG), "promoted")

    def test_rerun_before_promotion_replaces_assets_and_keeps_one_jsonlink_line(self) -> None:
        # An earlier run uploaded and then failed before promoting.
        self.release(True, {ARCHIVE: b"earlier build", CHECKSUM: b"earlier checksum"})
        earlier = self.stored() | {"body": self.notes.read_text()}
        (self.home / "release.json").write_text(json.dumps(earlier))
        self.publish()
        body = self.stored()["body"]
        self.assertEqual(body, self.notes.read_text())
        self.assertEqual(sum(line.startswith("jsonlink: ") for line in body.splitlines()), 1)
        self.assertEqual(self.published_bytes()[ARCHIVE], b"this run's build")
        self.assertEqual(self.published_bytes()[CHECKSUM], self.checksum.read_bytes())

    def test_not_prerelease_without_assets_is_marked_uploaded_and_promoted(self) -> None:
        self.release(False)
        self.assertEqual(release_publish.state(REPO, TAG), "unfinished")
        self.publish()
        writes = self.writes()
        self.assertEqual(writes[0], ["release", "edit", TAG, "--repo", REPO, "--prerelease"])
        self.assertEqual(writes[-2][:2], ["release", "upload"])
        self.assertEqual(writes[-1][-2:], ["--prerelease=false", "--latest"])
        self.assertEqual(self.stored()["assets"], [ARCHIVE, CHECKSUM])
        self.assertNotIn(["attestation", "verify"], [call[:2] for call in self.calls()])

    def test_publish_after_promotion_uploads_nothing(self) -> None:
        self.promoted()
        before = self.published_bytes()
        with self.assertRaisesRegex(ReleasePublishError, "already promoted"):
            self.publish()
        self.assertEqual(self.writes(), [])
        self.assertEqual(self.published_bytes(), before)

    def test_publish_rejects_other_asset_names(self) -> None:
        self.release(True)
        other = self.dist / "autonomy-v9.9.9-quarkus-app.tar.gz"
        other.write_bytes(b"x")
        with self.assertRaisesRegex(ReleasePublishError, "assets must be named"):
            release_publish.publish(REPO, TAG, self.notes, other, self.checksum)
        self.assertEqual(self.calls(), [])

    # verify: the path a rerun takes after promotion

    def test_retry_after_promotion_passes_and_uploads_nothing(self) -> None:
        self.promoted()
        before = self.published_bytes()
        self.assertEqual(release_publish.state(REPO, TAG), "promoted")
        release_publish.verify(REPO, TAG, COMMIT)
        self.assertEqual(self.writes(), [])
        self.assertEqual(self.published_bytes(), before)
        attestation = [call for call in self.calls() if call[:2] == ["attestation", "verify"]]
        self.assertEqual(len(attestation), 1)
        self.assertTrue(attestation[0][2].endswith(f"/{ARCHIVE}"))
        self.assertEqual(
            attestation[0][3:],
            [
                "--repo", REPO,
                "--signer-workflow", f"{REPO}/.github/workflows/release.yml",
                "--source-ref", f"refs/tags/{TAG}",
                "--source-digest", COMMIT,
            ],
        )  # fmt: skip

    def test_retry_after_promotion_fails_on_a_checksum_mismatch_and_replaces_nothing(self) -> None:
        self.promoted(archive=b"swapped bytes", checksum_of=b"first build")
        before = self.published_bytes()
        with self.assertRaisesRegex(ReleasePublishError, "sha256sum -c"):
            release_publish.verify(REPO, TAG, COMMIT)
        self.assertEqual(self.writes(), [])
        self.assertEqual(self.published_bytes(), before)
        self.assertEqual(release_publish.main(["--repo", REPO, "verify", "--tag", TAG, "--commit", COMMIT]), 1)

    def test_retry_after_promotion_fails_on_the_attestation_and_replaces_nothing(self) -> None:
        self.promoted(attested=False)
        before = self.published_bytes()
        with self.assertRaisesRegex(ReleasePublishError, "gh attestation verify failed"):
            release_publish.verify(REPO, TAG, COMMIT)
        self.assertEqual(self.writes(), [])
        self.assertEqual(self.published_bytes(), before)


if __name__ == "__main__":
    unittest.main()
