import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts" / "drop-duplicate-freedriver-bom.py"

SAME = """<dependencies>
    <dependency>
        <artifactId>freedriver-jakarta-bom</artifactId>
    </dependency>
    <dependency>
        <artifactId>other</artifactId>
    </dependency>
    <dependency>
        <artifactId>freedriver-jakarta-bom</artifactId>
    </dependency>
</dependencies>
"""

DIFFERENT = SAME.replace(
    "<artifactId>freedriver-jakarta-bom</artifactId>\n    </dependency>\n</dependencies>",
    "<artifactId>freedriver-jakarta-bom</artifactId>\n        <version>9</version>\n    </dependency>\n</dependencies>",
    1,
)


class DropDuplicateTest(unittest.TestCase):
    def test_drops_the_second_identical_import(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            path.write_text(SAME)
            result = subprocess.run(
                ["python3", str(SCRIPT), str(path)],
                check=True,
                text=True,
                capture_output=True,
            )
            self.assertIn("dropped duplicate", result.stdout)
            self.assertEqual(path.read_text().count("freedriver-jakarta-bom"), 1)
            self.assertIn("other", path.read_text())

    def test_leaves_a_single_import(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            path.write_text(SAME.split("    <dependency>\n        <artifactId>other")[0] + "</dependencies>\n")
            result = subprocess.run(
                ["python3", str(SCRIPT), str(path)],
                check=True,
                text=True,
                capture_output=True,
            )
            self.assertIn("no duplicate", result.stdout)

    def test_refuses_differing_imports(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            path.write_text(DIFFERENT)
            result = subprocess.run(
                ["python3", str(SCRIPT), str(path)],
                check=False,
                text=True,
                capture_output=True,
            )
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("imports differ", result.stderr)


if __name__ == "__main__":
    unittest.main()
