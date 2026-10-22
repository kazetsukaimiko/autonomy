#!/usr/bin/env python3
"""Drop a second identical freedriver-jakarta-bom import.

Maven 3.10 rejects that duplicate. Older Maven only warned, and kept the
first import. Differing imports are left in place and fail this script.
"""

from __future__ import annotations

import sys
from pathlib import Path


def drop_duplicate(path: Path) -> str:
    lines = path.read_text().splitlines(keepends=True)
    blocks: list[tuple[int, int, str]] = []
    index = 0
    while index < len(lines):
        if "<dependency>" in lines[index]:
            end = index
            while end < len(lines) and "</dependency>" not in lines[end]:
                end += 1
            if end >= len(lines):
                raise SystemExit(f"unclosed dependency in {path}")
            block = "".join(lines[index : end + 1])
            if "freedriver-jakarta-bom" in block:
                blocks.append((index, end, block))
            index = end + 1
        else:
            index += 1

    def norm(text: str) -> str:
        return " ".join(text.split())

    if len(blocks) < 2:
        return "no duplicate freedriver-jakarta-bom import"
    if norm(blocks[0][2]) != norm(blocks[1][2]):
        raise SystemExit("freedriver-jakarta-bom imports differ; not dropping")
    start, end, _ = blocks[1]
    del lines[start : end + 1]
    path.write_text("".join(lines))
    return "dropped duplicate freedriver-jakarta-bom import from inotify-cdi"


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print(
            "usage: drop-duplicate-freedriver-bom.py POM",
            file=sys.stderr,
        )
        return 2
    print(drop_duplicate(Path(argv[1])))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
