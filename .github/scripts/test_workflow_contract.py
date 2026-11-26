import json
import re
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
        self.assertIn("create-storage-record: false", text)
        self.assertIn("contents: write", text)
        self.assertIn("id-token: write", text)
        self.assertIn("attestations: write", text)
        self.assertNotIn("packages:", text)
        self.assertNotIn("security-events:", text)
        self.assertNotIn("artifact-metadata:", text)

    def test_release_job_finishes_the_release_release_please_created(self) -> None:
        text = workflow("release.yml")
        # One release per tag: release-please creates it, this job never does.
        self.assertNotIn("gh release create", text)
        self.assertNotIn("gh release", text)
        self.assertIn("release_publish.py state --tag", text)
        self.assertIn("release_publish.py publish --tag", text)
        self.assertIn("release_publish.py verify --tag", text)
        # The build runs for an unfinished release only, the check for a promoted one.
        self.assertEqual(text.count("if: needs.state.outputs.state == 'unfinished'"), 1)
        self.assertEqual(text.count("if: needs.state.outputs.state == 'promoted'"), 1)
        build = text.split("\n  release:\n", 1)[1].split("\n  verify:\n", 1)[0]
        self.assertIn("if: needs.state.outputs.state == 'unfinished'", build)
        self.assertIn("release_publish.py publish", build)
        self.assertLess(build.index("attest-build-provenance"), build.index("release_publish.py publish"))
        verify = text.split("\n  verify:\n", 1)[1]
        self.assertIn("if: needs.state.outputs.state == 'promoted'", verify)
        for word in ("mvn", "contents: write", "id-token", "release_publish.py publish", "upload"):
            self.assertNotIn(word, verify)

    def test_release_script_uploads_only_before_promotion(self) -> None:
        script = (ROOT / ".github" / "scripts" / "release_publish.py").read_text()
        self.assertNotIn('"create"', script)
        self.assertEqual(script.count('"upload"'), 1)
        self.assertIn('"upload", tag, str(archive), str(checksum), "--repo", repo, "--clobber"', script)
        publish = script.split("def publish(", 1)[1].split("\ndef ", 1)[0]
        guard = publish.index("if state(repo, tag) == PROMOTED:")
        self.assertLess(guard, publish.index('"--notes-file"'))
        self.assertLess(publish.index('"--notes-file"'), publish.index('"upload"'))
        self.assertLess(publish.index('"upload"'), publish.index('"--prerelease=false", "--latest"'))
        verify = script.split("def verify(", 1)[1].split("\ndef ", 1)[0]
        self.assertNotIn('"upload"', verify)
        self.assertNotIn('"edit"', verify)

    def test_release_notes_come_from_the_committed_changelog_and_the_bundled_jar(self) -> None:
        text = workflow("release.yml")
        self.assertIn("--changelog CHANGELOG.md", text)
        self.assertIn("--lib quarkus/target/quarkus-app/lib", text)
        self.assertNotIn("freedriver.sha", text)
        self.assertNotIn("Install freedriver SNAPSHOT", text)
        self.assertNotIn("drop-duplicate-freedriver-bom", text)
        self.assertIn("freedriver-web.sha", text)

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

    def fetch_step(self, name: str) -> str:
        text = workflow(name)
        # One secret reference, in the step that runs outside the checkout.
        self.assertEqual(text.count("FREEDRIVER_PACKAGES_TOKEN: ${{"), 1)
        self.assertEqual(text.count("secrets.FREEDRIVER_PACKAGES_TOKEN"), 1)
        step = text.split("- name: Fetch the freedriver release artifacts from GitHub Packages", 1)[1]
        return step.split("\n      - name:", 1)[0]

    def test_packages_token_stays_in_the_fetch_step(self) -> None:
        for name in ("ci.yml", "release.yml"):
            step = self.fetch_step(name)
            self.assertIn("secrets.FREEDRIVER_PACKAGES_TOKEN", step)
            self.assertIn("working-directory: ${{ runner.temp }}", step)
            self.assertIn("${env.FREEDRIVER_PACKAGES_TOKEN}", step)
            self.assertNotIn("echo \"${FREEDRIVER_PACKAGES_TOKEN}", step)
            self.assertNotIn("set -x", step)

    def test_release_fetches_the_same_freedriver_artifacts_as_ci(self) -> None:
        self.assertEqual(self.fetch_step("release.yml"), self.fetch_step("ci.yml"))


class ReleasePleaseWorkflowTest(unittest.TestCase):
    def test_runs_on_master_in_the_release_environment(self) -> None:
        text = workflow("release-please.yml")
        self.assertIn("    branches: [master]", text)
        self.assertNotIn("pull_request", text)
        self.assertNotIn("workflow_dispatch", text)
        self.assertEqual(text.count("\njobs:\n  release-please:\n"), 1)
        self.assertEqual(len(re.findall(r"(?m)^  [a-z][a-z-]*:$", text.split("\njobs:\n", 1)[1])), 1)
        self.assertIn("    environment: release\n", text)
        self.assertNotIn("contents: write\n", text.replace("permission-contents: write\n", ""))
        self.assertIn("group: release-please", text)
        self.assertIn("cancel-in-progress: false", text)

    def test_uses_the_app_token_and_never_the_built_in_one(self) -> None:
        text = workflow("release-please.yml")
        self.assertNotIn("secrets.GITHUB_TOKEN", text)
        self.assertNotIn("github.token", text)
        self.assertIn("app-id: ${{ secrets.RELEASE_APP_ID }}", text)
        self.assertIn("private-key: ${{ secrets.RELEASE_APP_PRIVATE_KEY }}", text)
        self.assertIn("repositories: autonomy", text)
        self.assertIn("permission-contents: write", text)
        self.assertIn("permission-pull-requests: write", text)
        self.assertEqual(len(re.findall(r"(?m)^ +permission-[a-z-]+:", text)), 2)
        self.assertIn("token: ${{ steps.app-token.outputs.token }}", text)
        self.assertNotIn("skip-github-release", text)
        self.assertNotIn("release-as", text)

    def test_every_action_is_pinned_to_a_full_commit_sha(self) -> None:
        uses = re.findall(r"(?m)^ +(?:- )?uses: (\S+)", workflow("release-please.yml"))
        self.assertEqual(
            [name.split("@")[0] for name in uses],
            ["actions/create-github-app-token", "googleapis/release-please-action"],
        )
        for name in uses:
            self.assertRegex(name, r"@[0-9a-f]{40}$")

    def test_new_release_is_marked_prerelease_with_the_app_token(self) -> None:
        text = workflow("release-please.yml")
        step = text.split("- name: Mark the new release a prerelease until its assets attach", 1)[1]
        self.assertIn("if: steps.release.outputs.release_created == 'true'", step)
        self.assertIn("GH_TOKEN: ${{ steps.app-token.outputs.token }}", step)
        self.assertIn('gh release edit "$TAG" --repo "$GITHUB_REPOSITORY" --prerelease\n', step)

    def test_app_secrets_stay_in_the_release_please_job(self) -> None:
        for path in sorted((ROOT / ".github" / "workflows").glob("*.y*ml")):
            text = path.read_text()
            expected = 1 if path.name == "release-please.yml" else 0
            self.assertEqual(text.count("RELEASE_APP_ID"), expected, path.name)
            self.assertEqual(text.count("RELEASE_APP_PRIVATE_KEY"), expected, path.name)
            if path.name != "release-please.yml":
                self.assertNotIn("environment: release", text, path.name)

    def test_no_workflow_merges_or_enables_auto_merge(self) -> None:
        for path in sorted((ROOT / ".github" / "workflows").glob("*.y*ml")):
            text = path.read_text().lower()
            for phrase in ("gh pr merge", "auto-merge", "automerge", "enablepullrequestautomerge", "/merge"):
                self.assertNotIn(phrase, text, path.name)


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
