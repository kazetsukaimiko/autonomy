#!/bin/bash
# Runs inside mongo-express:1.0.2-20. Refuses the image's published defaults,
# then connects as events-reader. Does not print passwords.
set -euo pipefail

reject() {
    echo "mongo-express: $*" >&2
    exit 1
}

case "${ME_CONFIG_BASICAUTH:-}" in
    true) ;;
    *) reject "basic auth is off" ;;
esac

case "${ME_CONFIG_BASICAUTH_USERNAME:-}" in
    ""|admin|root) reject "browser username is empty or a published default" ;;
esac

case "${ME_CONFIG_BASICAUTH_PASSWORD:-}" in
    ""|pass) reject "browser password is empty or the published default" ;;
    *[[:space:]]*) reject "browser password contains whitespace" ;;
esac

case "${MONGO_EXPRESS_DB_PASSWORD:-}" in
    ""|pass) reject "database password is empty or the published default" ;;
    *[[:space:]]*) reject "database password contains whitespace" ;;
esac

case "${ME_CONFIG_SITE_COOKIESECRET:-}" in
    ""|cookiesecret|secret) reject "cookie secret is empty or a published default" ;;
esac

case "${ME_CONFIG_SITE_SESSIONSECRET:-}" in
    ""|sessionsecret|secret) reject "session secret is empty or a published default" ;;
esac

case "${ME_CONFIG_MONGODB_ENABLE_ADMIN:-}" in
    false) ;;
    *) reject "Mongo admin mode is on" ;;
esac

case "${ME_CONFIG_OPTIONS_READONLY:-}" in
    true) ;;
    *) reject "the UI is not read-only" ;;
esac

export ME_CONFIG_BASICAUTH=true
export ME_CONFIG_MONGODB_ENABLE_ADMIN=false
export ME_CONFIG_OPTIONS_READONLY=true
export ME_CONFIG_OPTIONS_NO_DELETE=true

encoded="$(node -e 'process.stdout.write(encodeURIComponent(process.env.MONGO_EXPRESS_DB_PASSWORD))')"
export ME_CONFIG_MONGODB_URL="mongodb://events-reader:${encoded}@mongo:27017/autonomy?authSource=autonomy"
unset MONGO_EXPRESS_DB_PASSWORD

exec /docker-entrypoint.sh mongo-express
