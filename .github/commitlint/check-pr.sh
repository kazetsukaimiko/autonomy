#!/usr/bin/env bash
# Lint the pull request title and body, which become the squash commit.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
event=${GITHUB_EVENT_PATH:?}
message=${RUNNER_TEMP:?}/pr-commit-msg.txt

python3 - "$event" "$message" << 'PY'
import json
import sys
from pathlib import Path

event = json.loads(Path(sys.argv[1]).read_text())
pull_request = event["pull_request"]
title = (pull_request.get("title") or "").strip()
body = (pull_request.get("body") or "").replace("\r\n", "\n").strip("\n")
text = title + "\n"
if body:
    text += "\n" + body + "\n"
Path(sys.argv[2]).write_text(text)
PY

npx commitlint --edit "$message" --verbose
