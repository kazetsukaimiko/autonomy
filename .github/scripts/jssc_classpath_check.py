"""Fail unless autonomy-quarkus runtime resolves the recorded jssc 2.10.4 jars.

The check uses Maven's dependency plugin only. It does not compile quarkus
sources. The native load then runs with those two jars and no other
freedriver jar on the classpath.
"""

from __future__ import annotations

import hashlib
import os
import subprocess
import sys
import tempfile
from pathlib import Path

# Jar lines from kazetsukaimiko/freedriver serial-impl jssc-2.10.4-pin.txt.
EXPECTED = {
    "io.github.java-native:jssc:jar:2.10.4": (
        "f0cdf8ad9703c0be252c4206dae2d03823188fddab292d79356b56f02e252f40"
    ),
    "org.scijava:native-lib-loader:jar:2.5.0": (
        "7ec0dcf847cabdd6487c4724c3cede107a46bdb6585a9dcb63f54c0276b04b40"
    ),
}
FORBIDDEN = "org.scream3r:jssc:"

NATIVE_SOURCE = """\
public class JsscNativeLoad {
    public static void main(String[] args) throws Exception {
        String[] names = jssc.SerialPortList.getPortNames();
        System.out.println("jssc-native-load ports=" + names.length);
        java.util.LinkedHashSet<String> paths = new java.util.LinkedHashSet<>();
        String maps = java.nio.file.Files.readString(java.nio.file.Path.of("/proc/self/maps"));
        for (String line : maps.split("\\n")) {
            int slash = line.indexOf('/');
            if (slash < 0) {
                continue;
            }
            String path = line.substring(slash).trim();
            if (path.contains("libjssc")) {
                paths.add(path);
            }
        }
        System.out.println("jssc-native-count " + paths.size());
        for (String path : paths) {
            System.out.println("jssc-native " + path);
        }
        if (paths.size() != 1) {
            throw new IllegalStateException("expected one libjssc, saw " + paths.size());
        }
    }
}
"""


class ClasspathPinError(Exception):
    """The resolved runtime classpath is not the recorded jssc pin."""


def parse_list(text: str) -> list[tuple[str, str]]:
    """Return (group:artifact:type:version, absolute path) from dependency:list."""
    found: list[tuple[str, str]] = []
    for raw in text.splitlines():
        line = raw.strip()
        if line.startswith("[INFO]"):
            line = line[len("[INFO]") :].strip()
        parts = line.split(":")
        if len(parts) < 6:
            continue
        path = ":".join(parts[5:])
        # On JDK 9+ dependency:list appends " -- module <name>" (sometimes with
        # " [auto]") after the file name. Keep only the path.
        path = path.split(" -- module ", 1)[0].strip()
        if not path.startswith("/"):
            continue
        group, artifact, kind, version = parts[:4]
        found.append((f"{group}:{artifact}:{kind}:{version}", path))
    return found


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    digest.update(path.read_bytes())
    return digest.hexdigest()


def check_entries(entries: list[tuple[str, str]]) -> dict[str, str]:
    """Return the path of each expected jar, or raise ClasspathPinError."""
    forbidden = sorted({coord for coord, _path in entries if coord.startswith(FORBIDDEN)})
    if forbidden:
        raise ClasspathPinError("org.scream3r:jssc is on the runtime classpath: " + ", ".join(forbidden))
    chosen: dict[str, str] = {}
    for coord, path in entries:
        if coord in EXPECTED and coord not in chosen:
            chosen[coord] = path
    missing = [coord for coord in EXPECTED if coord not in chosen]
    if missing:
        raise ClasspathPinError("runtime classpath is missing " + ", ".join(missing))
    for coord, expected in EXPECTED.items():
        path = Path(chosen[coord])
        if not path.is_file():
            raise ClasspathPinError(f"{coord} path is not a file: {path}")
        actual = sha256(path)
        if actual != expected:
            raise ClasspathPinError(f"{coord} sha256 {actual} != {expected}")
    return chosen


def settings_arguments(settings: Path | None = None) -> list[str]:
    """Point Maven at the real settings file.

    CI sets user.home to a scratch directory so a build cannot install into
    the runner home. Maven then ignores the settings.xml that setup-java
    wrote under the real home, and GitHub Packages answers 401.
    """
    path = settings if settings is not None else Path.home() / ".m2" / "settings.xml"
    if path.is_file():
        return ["--settings", str(path)]
    return []


def maven_args(goal: str, output: Path) -> list[str]:
    args = [
        "mvn",
        "--batch-mode",
        "--no-transfer-progress",
        *settings_arguments(),
        "-pl",
        "quarkus",
        "-am",
        f"org.apache.maven.plugins:maven-dependency-plugin:3.8.1:{goal}",
        "-DskipAutonomyHomeInstall=true",
        f"-DoutputFile={output}",
    ]
    if goal == "tree":
        args.append("-Dscope=runtime")
    else:
        args.extend(
            [
                "-DincludeScope=runtime",
                "-DoutputAbsoluteArtifactFilename=true",
                "-DappendOutput=false",
            ]
        )
    home = os.environ.get("JAVA_TOOL_OPTIONS", "")
    for flag in ("-Duser.home=", "-Dautonomy.state.dir="):
        for piece in home.split():
            if piece.startswith(flag):
                args.append(piece)
    return args


def upstream_install_args() -> list[str]:
    """Install the modules quarkus depends on, without building quarkus.

    dependency:list needs each sibling's jar. Nothing in this run has packaged
    them, so without this Maven looks for the SNAPSHOT jars in remote
    repositories and fails. "quarkus,!quarkus" with -am selects only
    quarkus's upstream modules, so the quarkus sources are never compiled.
    """
    return [
        "mvn",
        "--batch-mode",
        "--no-transfer-progress",
        *settings_arguments(),
        "-pl",
        "quarkus,!quarkus",
        "-am",
        "install",
        "-DskipTests",
        *[
            piece
            for piece in os.environ.get("JAVA_TOOL_OPTIONS", "").split()
            if piece.startswith(("-Duser.home=", "-Dautonomy.state.dir="))
        ],
    ]


def run_maven(repo: Path) -> tuple[str, str]:
    target = repo / "quarkus" / "target"
    target.mkdir(parents=True, exist_ok=True)
    tree = target / "jssc-runtime-tree.txt"
    listing = target / "jssc-runtime-list.txt"
    subprocess.run(upstream_install_args(), cwd=repo, check=True)
    for goal, output in (("tree", tree), ("list", listing)):
        subprocess.run(maven_args(goal, output), cwd=repo, check=True)
    return tree.read_text(), listing.read_text()


def slf4j_api_jar(entries: list[tuple[str, str]]) -> str:
    """Return the slf4j-api jar the quarkus runtime resolves.

    native-lib-loader logs through slf4j, so loading jssc fails with
    NoClassDefFoundError: org/slf4j/LoggerFactory without it. It is not pinned
    by hash; only jssc and native-lib-loader are.
    """
    for coordinate, path in entries:
        if coordinate.startswith("org.slf4j:slf4j-api:jar:"):
            return path
    raise ClasspathPinError("org.slf4j:slf4j-api is not on the quarkus runtime classpath")


def load_native(jssc_jar: str, loader_jar: str, slf4j_jar: str) -> None:
    with tempfile.TemporaryDirectory() as directory:
        source = Path(directory) / "JsscNativeLoad.java"
        source.write_text(NATIVE_SOURCE)
        classpath = os.pathsep.join((jssc_jar, loader_jar, slf4j_jar))
        subprocess.run(
            ["javac", "--class-path", classpath, str(source)],
            cwd=directory,
            check=True,
        )
        subprocess.run(
            ["java", "--class-path", os.pathsep.join((directory, classpath)), "JsscNativeLoad"],
            cwd=directory,
            check=True,
        )


def main() -> int:
    repo = Path(__file__).resolve().parents[2]
    tree, listing = run_maven(repo)
    print(tree)
    try:
        entries = parse_list(listing)
        chosen = check_entries(entries)
        slf4j = slf4j_api_jar(entries)
    except ClasspathPinError as error:
        print(f"jssc classpath check failed: {error}", file=sys.stderr)
        return 1
    sys.stdout.flush()
    load_native(
        chosen["io.github.java-native:jssc:jar:2.10.4"],
        chosen["org.scijava:native-lib-loader:jar:2.5.0"],
        slf4j,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
