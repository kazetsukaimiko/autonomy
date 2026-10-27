#!/bin/bash
# Create or pin events-reader to the read role on database autonomy.
# Admin credentials stay in MONGO_ADMIN_URL. The reader password stays in
# MONGO_EXPRESS_DB_PASSWORD. Neither value is written here.
# This script does not start MongoDB and does not turn authentication off.
set -euo pipefail

reject() {
    echo "mongo-express-user: $*" >&2
    exit 1
}

: "${MONGO_ADMIN_URL:?set MONGO_ADMIN_URL}"
: "${MONGO_EXPRESS_DB_PASSWORD:?set MONGO_EXPRESS_DB_PASSWORD}"

case "${MONGO_ADMIN_URL}" in
    mongodb://*:*@*|mongodb+srv://*:*@*) ;;
    *) reject "admin URL must include credentials" ;;
esac

case "${MONGO_EXPRESS_DB_PASSWORD}" in
    ""|pass) reject "database password is empty or the published default" ;;
    *[[:space:]]*) reject "database password contains whitespace" ;;
esac

userinfo="${MONGO_ADMIN_URL#*://}"
userinfo="${userinfo%%@*}"
admin_user="${userinfo%%:*}"
case "${admin_user}" in
    ""|events-reader) reject "admin URL user must not be events-reader" ;;
esac

if [ "${MONGO_WRITER_USERNAME:-}" = "events-reader" ]; then
    reject "events-reader must not be autonomy's writer"
fi

js="$(mktemp)"
trap 'rm -f "${js}"' EXIT
cat > "${js}" <<'JS'
if (typeof process === "undefined" || !process.env) {
    print("mongosh did not expose process.env");
    quit(1);
}
const pwd = process.env.MONGO_EXPRESS_DB_PASSWORD;
if (typeof pwd !== "string" || pwd.length === 0 || pwd === "pass") {
    print("reader password was not available to mongosh");
    quit(1);
}
const roles = [{ role: "read", db: "autonomy" }];
try {
    const target = db.getSiblingDB("autonomy");
    const existing = target.getUser("events-reader");
    if (existing) {
        target.updateUser("events-reader", { pwd: pwd, roles: roles });
    } else {
        target.createUser({ user: "events-reader", pwd: pwd, roles: roles });
    }
    const user = target.getUser("events-reader");
    const direct = user && user.roles ? user.roles : [];
    const pinned = direct.length === 1 && direct[0].role === "read" && direct[0].db === "autonomy";
    if (!pinned) {
        print("events-reader is not exactly read on autonomy");
        quit(1);
    }
} catch (e) {
    const msg = String(e && e.message ? e.message : e);
    print(msg.indexOf(pwd) === -1 ? msg : "events-reader update failed");
    quit(1);
}
JS

# Prefer host mongosh so the password stays in the environment, not in argv.
# The docker fallback is only for a mongo container that already has mongosh.
# The script is on stdin because a host temp path does not exist in the container.
run_mongosh() {
    if command -v mongosh >/dev/null 2>&1; then
        mongosh "${MONGO_ADMIN_URL}" --quiet --norc < "${js}"
        return
    fi
    if command -v docker >/dev/null 2>&1; then
        docker compose exec -T \
            -e "MONGO_EXPRESS_DB_PASSWORD=${MONGO_EXPRESS_DB_PASSWORD}" \
            mongo \
            mongosh "${MONGO_ADMIN_URL}" --quiet --norc < "${js}"
        return
    fi
    reject "mongosh is not on PATH and docker is not installed"
}

run_mongosh
