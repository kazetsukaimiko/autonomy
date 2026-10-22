import subprocess
import tempfile
import unittest
from pathlib import Path

import check_version_tag
from check_version_tag import VersionTagError, check_version_tag


def git(repo: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), *args],
        check=True,
        text=True,
        capture_output=True,
    )
    return result.stdout


def new_repo() -> tuple[tempfile.TemporaryDirectory[str], Path]:
    directory = tempfile.TemporaryDirectory()
    repo = Path(directory.name)
    git(repo, "init", "-b", "master")
    git(repo, "config", "user.email", "test@example.com")
    git(repo, "config", "user.name", "test")
    git(repo, "config", "commit.gpgsign", "false")
    git(repo, "config", "tag.gpgsign", "false")
    return directory, repo


def commit(repo: Path, message: str) -> None:
    git(repo, "commit", "--allow-empty", "-m", message)


def commit_body(repo: Path, subject: str, body: str) -> None:
    git(repo, "commit", "--allow-empty", "-m", subject, "-m", body)


class VersionTagTest(unittest.TestCase):
    def test_first_tag_accepts_any_strict_version(self) -> None:
        directory, repo = new_repo()
        self.addCleanup(directory.cleanup)
        commit(repo, "chore: start")
        git(repo, "tag", "v0.1.0")
        self.assertEqual(check_version_tag(repo, "v0.1.0"), "v0.1.0 is the first version tag")

    def test_rejects_loose_versions(self) -> None:
        directory, repo = new_repo()
        self.addCleanup(directory.cleanup)
        commit(repo, "chore: start")
        for tag in ("v1.2", "v01.2.3", "v1.2.03", "1.2.3"):
            with self.subTest(tag=tag):
                with self.assertRaises(VersionTagError):
                    check_version_tag(repo, tag)

    def test_patch_bump_from_fix(self) -> None:
        directory, repo = new_repo()
        self.addCleanup(directory.cleanup)
        commit(repo, "chore: start")
        git(repo, "tag", "v1.2.3")
        commit(repo, "fix(mqtt): keep booting")
        git(repo, "tag", "v1.2.4")
        self.assertEqual(check_version_tag(repo, "v1.2.4"), "v1.2.4 is the patch bump from v1.2.3")

    def test_feat_beats_fix(self) -> None:
        directory, repo = new_repo()
        self.addCleanup(directory.cleanup)
        commit(repo, "chore: start")
        git(repo, "tag", "v1.2.3")
        commit(repo, "fix: a bug")
        commit(repo, "feat(quarkus): run startup actions")
        git(repo, "tag", "v1.3.0")
        self.assertIn("minor bump from v1.2.3", check_version_tag(repo, "v1.3.0"))
        git(repo, "tag", "-d", "v1.3.0")
        git(repo, "tag", "v1.2.4")
        with self.assertRaisesRegex(VersionTagError, "minor bump from v1.2.3 to v1.3.0"):
            check_version_tag(repo, "v1.2.4")

    def test_breaking_bang_or_footer_bumps_major(self) -> None:
        directory, repo = new_repo()
        self.addCleanup(directory.cleanup)
        commit(repo, "chore: start")
        git(repo, "tag", "v0.2.3")
        commit(repo, "feat!: rename the pin path")
        git(repo, "tag", "v1.0.0")
        self.assertIn("major bump", check_version_tag(repo, "v1.0.0"))

        directory2, repo2 = new_repo()
        self.addCleanup(directory2.cleanup)
        commit(repo2, "chore: start")
        git(repo2, "tag", "v1.4.0")
        commit_body(repo2, "fix(api): rename the path", "BREAKING CHANGE: clients must update.")
        git(repo2, "tag", "v2.0.0")
        self.assertIn("major bump from v1.4.0", check_version_tag(repo2, "v2.0.0"))

    def test_other_types_do_not_bump(self) -> None:
        directory, repo = new_repo()
        self.addCleanup(directory.cleanup)
        commit(repo, "chore: start")
        git(repo, "tag", "v1.2.3")
        commit(repo, "docs(deploy): list the settings")
        commit(repo, "refactor(quarkus): rename a helper")
        git(repo, "tag", "v1.2.4")
        with self.assertRaisesRegex(VersionTagError, "no fix, feat, or breaking change since v1.2.3"):
            check_version_tag(repo, "v1.2.4")

    def test_merge_commit_title_does_not_count(self) -> None:
        directory, repo = new_repo()
        self.addCleanup(directory.cleanup)
        commit(repo, "chore: start")
        git(repo, "tag", "v1.0.0")
        git(repo, "checkout", "-b", "side")
        commit(repo, "fix(mqtt): keep the service up")
        git(repo, "checkout", "master")
        git(repo, "merge", "--no-ff", "-m", "feat: this merge title is not a feature", "side")
        git(repo, "tag", "v1.0.1")
        self.assertIn("patch bump", check_version_tag(repo, "v1.0.1"))

    def test_build_stamp_is_not_a_version_tag(self) -> None:
        directory, repo = new_repo()
        self.addCleanup(directory.cleanup)
        commit(repo, "chore: start")
        git(repo, "tag", "2026-10_r1")
        commit(repo, "fix: a bug")
        git(repo, "tag", "v1.0.0")
        self.assertEqual(check_version_tag(repo, "v1.0.0"), "v1.0.0 is the first version tag")

    def test_tag_must_point_at_head(self) -> None:
        directory, repo = new_repo()
        self.addCleanup(directory.cleanup)
        commit(repo, "chore: start")
        git(repo, "tag", "v1.0.0")
        commit(repo, "fix: later")
        with self.assertRaisesRegex(VersionTagError, "does not point at HEAD"):
            check_version_tag(repo, "v1.0.0")

    def test_require_master(self) -> None:
        directory, repo = new_repo()
        self.addCleanup(directory.cleanup)
        commit(repo, "chore: start")
        git(repo, "tag", "v1.0.0")
        with self.assertRaisesRegex(VersionTagError, "origin/master is not available"):
            check_version_tag(repo, "v1.0.0", require_master=True)
        git(repo, "update-ref", "refs/remotes/origin/master", "HEAD")
        self.assertIn("first version tag", check_version_tag(repo, "v1.0.0", require_master=True))
        commit(repo, "fix: not on master yet")
        git(repo, "tag", "v1.0.1")
        with self.assertRaisesRegex(VersionTagError, "not an ancestor of origin/master"):
            check_version_tag(repo, "v1.0.1", require_master=True)


if __name__ == "__main__":
    unittest.main()
