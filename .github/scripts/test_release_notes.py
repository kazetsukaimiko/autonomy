import hashlib
import tempfile
import unittest
from pathlib import Path

from release_notes import ReleaseNotesError, release_notes

CHANGELOG = """# Changelog

## [1.3.0](https://github.com/kazetsukaimiko/autonomy/compare/v1.2.4...v1.3.0) (2026-10-21)


### Features

* **quarkus:** run startup actions ([#45](https://github.com/kazetsukaimiko/autonomy/issues/45))

## [1.2.4](https://github.com/kazetsukaimiko/autonomy/compare/v1.2.3...v1.2.4) (2026-10-01)


### Bug Fixes

* **mqtt:** keep booting ([#12](https://github.com/kazetsukaimiko/autonomy/issues/12))

## 1.1.0 (2026-09-01)


### Features

* **api:** first version tag
"""

JAR = b"the bundled jsonlink jar"
JAR_NAME = "io.freedriver.jsonlink-1.0.0.jar"
LINE = f"jsonlink: io.freedriver:jsonlink:1.0.0 sha256:{hashlib.sha256(JAR).hexdigest()}"


class ReleaseNotesTest(unittest.TestCase):
    def setUp(self) -> None:
        scratch = tempfile.TemporaryDirectory()
        self.addCleanup(scratch.cleanup)
        # The fast-jar layout: quarkus-app/lib/{boot,main}.
        self.lib = Path(scratch.name) / "quarkus-app" / "lib"
        (self.lib / "main").mkdir(parents=True)
        (self.lib / "boot").mkdir()
        (self.lib / "main" / JAR_NAME).write_bytes(JAR)
        (self.lib / "main" / "io.freedriver.serial-impl-1.0.0.jar").write_bytes(b"another jar")

    def test_extracts_the_named_section_and_appends_jsonlink(self) -> None:
        notes = release_notes(CHANGELOG, "v1.3.0", self.lib)
        self.assertTrue(notes.startswith("## [1.3.0](https://github.com/"))
        self.assertIn("run startup actions", notes)
        self.assertNotIn("keep booting", notes)
        self.assertTrue(notes.endswith(f"\n\n{LINE}\n"))
        self.assertEqual(sum(line.startswith("jsonlink: ") for line in notes.splitlines()), 1)

    def test_jsonlink_line_is_the_digest_of_the_bundled_jar(self) -> None:
        (self.lib / "main" / JAR_NAME).write_bytes(b"a different build of jsonlink")
        digest = hashlib.sha256(b"a different build of jsonlink").hexdigest()
        notes = release_notes(CHANGELOG, "v1.3.0", self.lib)
        self.assertTrue(notes.endswith(f"\njsonlink: io.freedriver:jsonlink:1.0.0 sha256:{digest}\n"))

    def test_middle_section(self) -> None:
        notes = release_notes(CHANGELOG, "1.2.4", self.lib)
        self.assertIn("keep booting", notes)
        self.assertNotIn("run startup actions", notes)
        self.assertNotIn("first version tag", notes)

    def test_heading_without_a_compare_link(self) -> None:
        notes = release_notes(CHANGELOG, "v1.1.0", self.lib)
        self.assertTrue(notes.startswith("## 1.1.0 (2026-09-01)\n"))
        self.assertIn("first version tag", notes)
        self.assertTrue(notes.endswith(f"\n\n{LINE}\n"))

    def test_missing_section(self) -> None:
        with self.assertRaisesRegex(ReleaseNotesError, "no section for 9.9.9"):
            release_notes(CHANGELOG, "v9.9.9", self.lib)

    def test_a_longer_version_is_another_section(self) -> None:
        with self.assertRaisesRegex(ReleaseNotesError, "no section for 1.1.1"):
            release_notes(CHANGELOG.replace("## 1.1.0 ", "## 1.1.10 "), "v1.1.1", self.lib)

    def test_fails_without_a_jsonlink_jar(self) -> None:
        (self.lib / "main" / JAR_NAME).unlink()
        with self.assertRaisesRegex(ReleaseNotesError, "no jsonlink jar"):
            release_notes(CHANGELOG, "v1.3.0", self.lib)

    def test_fails_on_two_jsonlink_jars(self) -> None:
        (self.lib / "main" / "io.freedriver.jsonlink-1.0.1.jar").write_bytes(b"second")
        with self.assertRaisesRegex(ReleaseNotesError, "more than one jsonlink jar"):
            release_notes(CHANGELOG, "v1.3.0", self.lib)


if __name__ == "__main__":
    unittest.main()
