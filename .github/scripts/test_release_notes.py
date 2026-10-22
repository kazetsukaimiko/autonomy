import unittest

from release_notes import ReleaseNotesError, release_notes

CHANGELOG = """# Changelog

## [1.3.0] - 2026-10-21

### Features

- *(quarkus)* Run startup actions

## [1.2.4] - 2026-10-01

### Bug Fixes

- *(mqtt)* Keep booting
"""

SHA = "a7038690bf0fd7395375e291e1a8ed55e5c1ae82"


class ReleaseNotesTest(unittest.TestCase):
    def test_extracts_the_named_section_and_appends_jsonlink(self) -> None:
        notes = release_notes(CHANGELOG, "v1.3.0", SHA)
        self.assertTrue(notes.startswith("## [1.3.0] - 2026-10-21\n"))
        self.assertIn("Run startup actions", notes)
        self.assertNotIn("Keep booting", notes)
        self.assertTrue(notes.endswith(f"\n\njsonlink: kazetsukaimiko/freedriver@{SHA}\n"))

    def test_last_section(self) -> None:
        notes = release_notes(CHANGELOG, "1.2.4", SHA)
        self.assertIn("Keep booting", notes)
        self.assertNotIn("Run startup actions", notes)

    def test_missing_section(self) -> None:
        with self.assertRaisesRegex(ReleaseNotesError, "no section for 9.9.9"):
            release_notes(CHANGELOG, "v9.9.9", SHA)

    def test_rejects_a_bad_sha(self) -> None:
        with self.assertRaisesRegex(ReleaseNotesError, "40 lowercase hex"):
            release_notes(CHANGELOG, "v1.3.0", "ABC")


if __name__ == "__main__":
    unittest.main()
