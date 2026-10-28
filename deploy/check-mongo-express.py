#!/usr/bin/env python3
"""Fail if the mongo-express runtime files drop a required guard."""

from pathlib import Path

ROOT = Path(__file__).resolve().parent
COMPOSE = (ROOT / "compose.mongo-express.yaml").read_text()
ENTRY = (ROOT / "mongo-express-entrypoint.sh").read_text()
USER = (ROOT / "mongo-express-user.sh").read_text()
EXAMPLE = (ROOT / "mongo-express.env.example").read_text()
GITIGNORE = (ROOT.parent / ".gitignore").read_text()

IMAGE = (
    "mongo-express:1.0.2-20"
    "@sha256:30fc6333779340217c6a2bc0bc3cd53aa46028ab8fc6caef41bd00bf0faacf53"
)


def fail(message: str) -> None:
    raise SystemExit(message)


def require(text: str, needle: str, where: str) -> None:
    if needle not in text:
        fail(f"{where} missing {needle}")


def forbid(text: str, needle: str, where: str) -> None:
    if needle in text:
        fail(f"{where} contains {needle}")


require(COMPOSE, IMAGE, "compose")
require(COMPOSE, '"127.0.0.1:8081:8081"', "compose")
require(COMPOSE, 'ME_CONFIG_BASICAUTH: "true"', "compose")
require(COMPOSE, 'ME_CONFIG_MONGODB_ENABLE_ADMIN: "false"', "compose")
require(COMPOSE, 'ME_CONFIG_OPTIONS_READONLY: "true"', "compose")
require(COMPOSE, "mongo-express-entrypoint.sh:ro", "compose")
forbid(COMPOSE, "0.0.0.0", "compose")
forbid(COMPOSE, "privileged", "compose")
forbid(COMPOSE, "network_mode", "compose")
forbid(COMPOSE, "docker.sock", "compose")
forbid(COMPOSE, "build:", "compose")
forbid(COMPOSE, "latest", "compose")
forbid(COMPOSE, ": pass", "compose")
forbid(COMPOSE, "cookiesecret", "compose")
forbid(COMPOSE, "sessionsecret", "compose")

for needle in (
    "basic auth is off",
    "published default",
    "Mongo admin mode is on",
    "events-reader:",
    "authSource=autonomy",
    "unset MONGO_EXPRESS_DB_PASSWORD",
):
    require(ENTRY, needle, "entrypoint")

require(USER, 'role: "read", db: "autonomy"', "user script")
require(USER, "events-reader", "user script")
require(USER, "admin URL must include credentials", "user script")
forbid(USER, "--noauth", "user script")
forbid(USER, "authorization", "user script")

for line in EXAMPLE.splitlines():
    if not line or line.startswith("#"):
        continue
    key, sep, value = line.partition("=")
    if sep != "=" or not key or value != "":
        fail(f"example env line is not an empty assignment: {line}")

require(GITIGNORE, "deploy/mongo-express.env", "gitignore")
forbid(COMPOSE, "commands-enabled", "compose")
forbid(ENTRY, "commands-enabled", "entrypoint")
print("mongo-express checks ok")
