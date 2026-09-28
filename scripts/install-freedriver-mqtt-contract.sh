#!/usr/bin/env bash
# Installs io.freedriver:freedriver-mqtt-contract:1.0.0-SNAPSHOT built from the
# freedriver-web commit named in freedriver-web.sha. Does not use GitHub Packages.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SHA="$(tr -d '[:space:]' < "${ROOT}/freedriver-web.sha")"
if [[ ! "${SHA}" =~ ^[0-9a-f]{40}$ ]]; then
  echo "freedriver-web.sha must be a 40-character commit id, found: ${SHA}" >&2
  exit 1
fi

if ! java -version 2>&1 | grep -q 'version "23\.'; then
  echo "JDK 23 is required. freedriver-mqtt-contract sets maven.compiler.release to 23." >&2
  exit 1
fi

WORKDIR="${FREEDRIVER_WEB_CHECKOUT:-${TMPDIR:-/tmp}/freedriver-web-${SHA}}"
mkdir -p "${WORKDIR}"
if [[ ! -d "${WORKDIR}/.git" ]]; then
  git init "${WORKDIR}"
  git -C "${WORKDIR}" remote add origin https://github.com/kazetsukaimiko/freedriver-web.git
fi
git -C "${WORKDIR}" fetch --depth 1 origin "${SHA}"
git -C "${WORKDIR}" checkout --detach FETCH_HEAD
"${WORKDIR}/mvnw" -f "${WORKDIR}/pom.xml" --batch-mode -pl mqtt-contract -am install -DskipTests
