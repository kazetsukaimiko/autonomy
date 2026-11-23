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


if __name__ == "__main__":
    unittest.main()
