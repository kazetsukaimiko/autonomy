import hashlib
import tempfile
import unittest
from pathlib import Path

import jssc_classpath_check
from jssc_classpath_check import ClasspathPinError, check_entries, parse_list


def jar(directory: Path, name: str, payload: bytes) -> Path:
    path = directory / name
    path.write_bytes(payload)
    return path


class ParseListTest(unittest.TestCase):
    def test_reads_info_lines_and_plain_lines(self) -> None:
        text = "\n".join(
            [
                "[INFO] The following files have been resolved:",
                "[INFO]    io.github.java-native:jssc:jar:2.10.4:compile:/repo/jssc.jar",
                "org.scijava:native-lib-loader:jar:2.5.0:compile:/repo/loader.jar",
                "not a coordinate",
            ]
        )
        self.assertEqual(
            parse_list(text),
            [
                ("io.github.java-native:jssc:jar:2.10.4", "/repo/jssc.jar"),
                ("org.scijava:native-lib-loader:jar:2.5.0", "/repo/loader.jar"),
            ],
        )


class CheckEntriesTest(unittest.TestCase):
    def test_accepts_the_recorded_jars(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payloads = {
                "io.github.java-native:jssc:jar:2.10.4": b"jssc-bytes",
                "org.scijava:native-lib-loader:jar:2.5.0": b"loader-bytes",
            }
            original = dict(jssc_classpath_check.EXPECTED)
            try:
                jssc_classpath_check.EXPECTED = {
                    coord: hashlib.sha256(payload).hexdigest() for coord, payload in payloads.items()
                }
                entries = [
                    (coord, str(jar(root, f"{index}.jar", payload)))
                    for index, (coord, payload) in enumerate(payloads.items())
                ]
                chosen = check_entries(entries)
            finally:
                jssc_classpath_check.EXPECTED = original
            self.assertEqual(set(chosen), set(payloads))

    def test_rejects_scream3r_and_a_bad_hash_and_a_missing_jar(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            jssc = jar(root, "jssc.jar", b"jssc-bytes")
            loader = jar(root, "loader.jar", b"loader-bytes")
            original = dict(jssc_classpath_check.EXPECTED)
            try:
                jssc_classpath_check.EXPECTED = {
                    "io.github.java-native:jssc:jar:2.10.4": hashlib.sha256(b"jssc-bytes").hexdigest(),
                    "org.scijava:native-lib-loader:jar:2.5.0": hashlib.sha256(b"loader-bytes").hexdigest(),
                }
                scream = [
                    ("org.scream3r:jssc:jar:2.8.0", str(jssc)),
                    ("io.github.java-native:jssc:jar:2.10.4", str(jssc)),
                    ("org.scijava:native-lib-loader:jar:2.5.0", str(loader)),
                ]
                with self.assertRaises(ClasspathPinError):
                    check_entries(scream)
                bad = [
                    ("io.github.java-native:jssc:jar:2.10.4", str(loader)),
                    ("org.scijava:native-lib-loader:jar:2.5.0", str(loader)),
                ]
                with self.assertRaises(ClasspathPinError):
                    check_entries(bad)
                with self.assertRaises(ClasspathPinError):
                    check_entries([("org.scijava:native-lib-loader:jar:2.5.0", str(loader))])
            finally:
                jssc_classpath_check.EXPECTED = original


if __name__ == "__main__":
    unittest.main()
