import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def workflow(name: str) -> str:
    return (ROOT / ".github" / "workflows" / name).read_text()


class WorkflowContractTest(unittest.TestCase):
    def test_release_job_is_tag_only_and_can_attest(self) -> None:
        text = workflow("release.yml")
        self.assertNotIn("pull_request:", text)
        self.assertIn("tags:", text)
        self.assertIn("fetch-depth: 0", text)
        self.assertIn("actions/attest-build-provenance@v4", text)
        self.assertIn("contents: write", text)
        self.assertIn("id-token: write", text)
        self.assertIn("attestations: write", text)
        self.assertNotIn("packages:", text)
        self.assertNotIn("security-events:", text)
        self.assertNotIn("artifact-metadata:", text)

    def test_lint_job_is_read_only(self) -> None:
        text = workflow("commitlint.yml")
        self.assertIn("pull_request:", text)
        self.assertIn("contents: read", text)
        self.assertNotIn("contents: write", text)
        self.assertNotIn("id-token:", text)
        self.assertNotIn("attestations:", text)

    def test_verify_workflow_stays_read_only(self) -> None:
        text = workflow("ci.yml")
        self.assertIn("contents: read", text)
        self.assertIn("packages: read", text)
        self.assertIn("server-id: github", text)
        self.assertIn("jssc_classpath_check.py", text)
        self.assertNotIn("Install freedriver SNAPSHOT", text)
        self.assertNotIn("contents: write", text)
        self.assertNotIn("id-token:", text)
        self.assertNotIn("attestations:", text)

    def test_packages_token_stays_in_the_fetch_step(self) -> None:
        text = workflow("ci.yml")
        # One secret reference, in the step that runs outside the checkout.
        self.assertEqual(text.count("secrets.FREEDRIVER_PACKAGES_TOKEN"), 1)
        step = text.split("- name: Fetch the freedriver release artifacts from GitHub Packages", 1)[1]
        step = step.split("\n      - name:", 1)[0]
        self.assertIn("secrets.FREEDRIVER_PACKAGES_TOKEN", step)
        self.assertIn("working-directory: ${{ runner.temp }}", step)
        self.assertIn("${env.FREEDRIVER_PACKAGES_TOKEN}", step)
        self.assertNotIn("echo \"${FREEDRIVER_PACKAGES_TOKEN}", step)
        self.assertNotIn("set -x", step)


# The master head this branch starts from. release-please reads commits after it.
BOOTSTRAP_SHA = "7bbfab0cb628d26c6abbcccbfa162ea82a13c99b"
HIDDEN_TYPES = ["refactor", "docs", "perf", "test", "chore", "ci", "build", "style", "revert"]
RELEASE_FILES = ["/version.txt", "/release-please-config.json", "/.release-please-manifest.json"]


class ReleasePleaseConfigTest(unittest.TestCase):
    def package(self) -> dict:
        config = json.loads((ROOT / "release-please-config.json").read_text())
        self.assertEqual(list(config["packages"]), ["."])
        return config["packages"]["."]

    def test_only_feat_and_fix_render_a_changelog_section(self) -> None:
        # release-please cuts a release whenever the rendered changelog has text,
        # so a visible section for another type would cut a patch release.
        sections = self.package()["changelog-sections"]
        visible = [entry["type"] for entry in sections if not entry.get("hidden", False)]
        hidden = [entry["type"] for entry in sections if entry.get("hidden", False)]
        self.assertEqual(visible, ["feat", "fix"])
        self.assertEqual(sorted(hidden), sorted(HIDDEN_TYPES))

    def test_bootstrap_and_standing_pull_request(self) -> None:
        config = json.loads((ROOT / "release-please-config.json").read_text())
        self.assertEqual(config["bootstrap-sha"], BOOTSTRAP_SHA)
        self.assertIs(config["always-update"], True)
        self.assertNotIn("release-as", json.dumps(config))
        self.assertNotIn("skip-github-release", json.dumps(config))

    def test_version_file_tag_and_changelog(self) -> None:
        package = self.package()
        self.assertEqual(package["release-type"], "simple")
        self.assertEqual(package["version-file"], "version.txt")
        self.assertEqual(package["changelog-path"], "CHANGELOG.md")
        self.assertIs(package["include-v-in-tag"], True)
        manifest = json.loads((ROOT / ".release-please-manifest.json").read_text())
        self.assertEqual(list(manifest), ["."])
        self.assertEqual((ROOT / "version.txt").read_text().strip(), manifest["."])
        self.assertTrue((ROOT / "CHANGELOG.md").is_file())

    def test_pull_request_title_and_refs_footer_pass_commitlint(self) -> None:
        package = self.package()
        self.assertEqual(package["pull-request-title-pattern"], "chore(release): release ${version}")
        # The leading blank line makes Refs its own last paragraph after the closing ---.
        self.assertEqual(package["pull-request-footer"], "\nRefs: #128")
        fixtures = ROOT / ".github" / "commitlint" / "fixtures"
        accepted = (fixtures / "ok-release-please.txt").read_text()
        self.assertTrue(accepted.startswith("chore(release): release 1.1.0\n"))
        self.assertTrue(accepted.endswith("\n---\n" + package["pull-request-footer"] + "\n"))
        rejected = (fixtures / "bad-release-please-no-refs.txt").read_text()
        self.assertEqual(rejected, accepted[: -len(package["pull-request-footer"] + "\n")])
        config = (ROOT / ".github" / "commitlint" / "commitlint.config.js").read_text()
        self.assertIn('"interpret-trailers", "--parse", "--no-divider"', config)
        self.assertIn('"refs-trailer": [2, "always"]', config)
        self.assertIn('"refs-card": [2, "always"]', config)
        self.assertNotIn('"trailer-exists":', config)

    def test_release_files_need_the_owner_review(self) -> None:
        lines = (ROOT / ".github" / "CODEOWNERS").read_text().splitlines()
        for path in RELEASE_FILES:
            self.assertIn(f"{path} @kazetsukaimiko", lines)

    def test_jar_changelog_skips_release_commits(self) -> None:
        cliff = (ROOT / "cliff.toml").read_text()
        self.assertIn('{ message = "(?i)^chore\\\\(release\\\\): release", skip = true }', cliff)
        skip = cliff.index("): release\", skip = true")
        self.assertLess(skip, cliff.index('group = "<!-- 7 -->Miscellaneous"'))


if __name__ == "__main__":
    unittest.main()
