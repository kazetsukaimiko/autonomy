# Autonomy Deployment

This folder contains the deployment artifacts and documentation for running Autonomy, the Quarkus automation service, on a target machine (for example hakobune).

The Maven antrun plugin in the `quarkus` module copies the built application, the systemd user units, and the helper scripts into the user account.

## Versioned releases

release-please cuts the versions. Nobody chooses a number or pushes a tag.

1. Pull request titles are conventional commits, and each one becomes a squash commit on master. On every push to master, `.github/workflows/release-please.yml` opens one release pull request, or updates the open one. Its title is `chore(release): release X.Y.Z`. It changes `version.txt`, `.release-please-manifest.json` and `CHANGELOG.md`, and it runs the same checks as any pull request.
2. The release pull request stays open and collects merges. Kaze decides when to release. Then kazetsukaimiko approves and merges it. `version.txt` and the two release-please files have kazetsukaimiko as code owner, so only that approval can merge it, and auto-merge stays off for it.
3. The merge creates the tag `vX.Y.Z` and a GitHub release for it, marked prerelease.
4. The tag starts `.github/workflows/release.yml`. It builds from the tag, attaches the archive and its checksum to that release, and then promotes it: the release stops being a prerelease and becomes the latest.

The number comes from the commits since the previous version tag:

* A breaking change bumps the major version and resets minor and patch to 0. A breaking change is a `!` in the subject (`feat!:` or `feat(scope)!:`) or a `BREAKING CHANGE:` footer. A pull request has to use both, which commitlint checks.
* Otherwise a `feat` commit bumps the minor version and resets patch to 0.
* Otherwise a `fix` commit bumps the patch version.
* `refactor`, `perf`, `docs`, `test`, `chore`, `ci`, and `build` do not bump the version and stay out of `CHANGELOG.md`. A master with only those since the last release has no release pull request.

Tags are `vMAJOR.MINOR.PATCH` with no leading zeros. `YYYY-MM_rN` build stamps are not version tags. The manifest starts at 1.0.0, so the first release is 1.1.0, 1.0.1 or 2.0.0. There is no `v1.0.0` tag, so a compare link to `v1.0.0` in the first release notes leads nowhere.

### What the tag job does

The job first reads the release. A release is promoted when it is not a prerelease and lists both `autonomy-vX.Y.Z-quarkus-app.tar.gz` and `autonomy-vX.Y.Z-quarkus-app.tar.gz.sha256`. Anything else is unfinished, and the job marks it prerelease again before it builds.

For an unfinished release the job checks that the tagged commit is an ancestor of `origin/master`, that the number matches the bump rules above, and that the tag is the changelog cutoff. It packages the Quarkus fast-jar, not an uber-jar, against the published `io.freedriver.version` artifacts from GitHub Packages, the same ones CI verifies. The release asset is an archive of `quarkus-app/` (`quarkus-run.jar`, `app/`, `lib/`, and the rest of that directory), plus a SHA-256 checksum, plus a build attestation from `actions/attest-build-provenance`.

The release notes are the `CHANGELOG.md` section release-please wrote for that version, then one line naming the jsonlink jar inside the archive: `jsonlink: io.freedriver:jsonlink:<version> sha256:<sha256 of lib/main/io.freedriver.jsonlink-<version>.jar>`. `GET /api/changelog` still serves the changelog git-cliff generates during the build; nobody commits or edits that one.

### When the tag job fails

The release stays a prerelease. Fix the cause and re-run the failed jobs of that workflow run. A rerun sets the whole release body again, replaces any asset an earlier attempt uploaded, and promotes the release at the end.

If the version check rejects the tag itself, leave the prerelease in place and take it to kaze. Deleting a published tag is kaze's call.

A promoted release is never changed. Re-running all jobs for a promoted release builds nothing and uploads nothing. It downloads the published archive and checksum, runs `sha256sum -c`, and runs `gh attestation verify` for this repository, the `release.yml` workflow, the tag and its commit. It is green when both pass. The build is not reproducible, so a fresh build of the same commit has different bytes; the published archive is the release.

Deploy only a release that is no longer a prerelease and lists the archive and its checksum:

```bash
tag=vX.Y.Z
gh release view "$tag" --repo kazetsukaimiko/autonomy --json isPrerelease,assets \
  --jq '{isPrerelease, assets: [.assets[].name]}'
```

### Release credential

The release pull request, the tag and the release are made by a GitHub App, because a pull request or tag made with the built-in `GITHUB_TOKEN` starts no workflow. The App has `contents: write` and `pull-requests: write` and is installed on this repository only. `release-please.yml` mints a token for each run, limited to this repository and those two permissions.

kazetsukaimiko sets this up once, before the release-please workflow can run:

1. Create the App and install it on `kazetsukaimiko/autonomy` only.
2. Create the environment `release` with deployment branches limited to `master`.
3. Store the App's id and private key in that environment as the secrets `RELEASE_APP_ID` and `RELEASE_APP_PRIVATE_KEY`.
4. Add the App as a bypass actor on the tag ruleset `version tags` (`refs/tags/v*`). That ruleset restricts who creates version tags, so without the bypass release-please cannot create `vX.Y.Z`.

Deploy that archive. Verify it before replacing the running copy:

```bash
tag=vX.Y.Z
dir=$(mktemp -d)
gh release download "$tag" --repo kazetsukaimiko/autonomy --dir "$dir" \
  --pattern "autonomy-${tag}-quarkus-app.tar.gz*"
gh attestation verify "$dir/autonomy-${tag}-quarkus-app.tar.gz" --repo kazetsukaimiko/autonomy
( cd "$dir" && sha256sum -c "autonomy-${tag}-quarkus-app.tar.gz.sha256" )
rm -rf "${HOME}/.local/autonomy.rollback"
if [[ -d "${HOME}/.local/autonomy" ]]; then
  mv "${HOME}/.local/autonomy" "${HOME}/.local/autonomy.rollback"
fi
mkdir -p "${HOME}/.local/autonomy"
tar -xzf "$dir/autonomy-${tag}-quarkus-app.tar.gz" -C "${HOME}/.local/autonomy" --strip-components=1
systemctl --user restart autonomy-next.service
curl -fsS http://127.0.0.1:8080/api/changelog
```

`autonomy.service` stays disabled. Restart `autonomy-next.service`. Post the `GET /api/changelog` body in the deploy post. The previous build is `${HOME}/.local/autonomy.rollback`. To roll back, stop `autonomy-next.service`, swap the two directories, and start the unit again.

## Deploy Process

This is a local build. A version-tag deploy uses the release archive above.

1. Install the pinned MQTT contract, then build (from the autonomy repo root):
   ```
   ./scripts/install-freedriver-mqtt-contract.sh
   mvn install -pl quarkus -am
   ```
   The script checks out freedriver-web at the commit in `freedriver-web.sha` and installs `freedriver-mqtt-contract`. JDK 23 is required. MQTT itself stays off unless configured; see [docs/mqtt.md](../docs/mqtt.md).
   - This produces `quarkus/target/quarkus-app/...` and `quarkus-run.jar`.
   - The antrun plugin (bound to the `install` phase) copies:
     * App files → `~/.local/autonomy/` (full layout and the top-level runner jar).
     * `*.service` files → `~/.local/share/systemd/user/`.
     * `*.sh` scripts → `~/bin/` (and makes them executable).

2. On the target machine (as the user, for example `luna`):
   ```
   systemctl --user daemon-reload
   systemctl --user enable --now autonomy.service
   ```

3. Useful commands:
   ```
   # Restart (after deploys or config changes)
   ~/bin/restartAutonomy.sh
   # or
   systemctl --user restart autonomy

   # View logs (follow)
   ~/bin/autolog.sh
   # or
   journalctl -f --user-unit autonomy.service

   # Stop
   ~/bin/stopAutonomy.sh
   # or
   systemctl --user stop autonomy

   # Start + logs
   ~/bin/startAutonomy.sh
   ```

## Locations on Target Machine

* **Application / JARs**:
  * `~/.local/autonomy/quarkus-run.jar` (the executable jar).
  * `~/.local/autonomy/quarkus-app/` (the exploded fast-jar layout with `app/`, `lib/`, and the rest of the Quarkus layout).
  * These are overwritten on each `mvn install` of the `quarkus` module.

* **Configuration and data** (`~/.config/autonomy/`):
  * `mappings_v2.json`: appliance and connector mappings (relays, lights, water valves, pumps) and `eventTTL`. This build always reads that fixed path under `~/.config/autonomy/`. It does not read `autonomy.mappings.file`.
  * `sensor_history.json`: analog sensor history read by the alias service.
  * Other files that may already sit in the directory: `soc_calculations.json`, pin notes, backups.
  * The process keeps appliance state in those files. The service starts with no JDBC datasource. Event writes are accepted and discarded. This build does not read or write `appliance-state.json`.

* **Systemd user units** (`~/.local/share/systemd/user/`):
  * `autonomy.service`: the main daemon.
  * `autonomy-next.service`: the next unit, listening on `0.0.0.0:8080`.

* **Helper scripts** (`~/bin/`, executable):
  * `restartAutonomy.sh`, `startAutonomy.sh`, `stopAutonomy.sh`, `autolog.sh`.

* **App config** (baked into the jar, overridable at runtime):
  * Inside `quarkus-app/`: `application.properties` carries no JDBC datasource. The HTTP server uses Quarkus' default port 8080.
  * Runtime overrides use `-Dquarkus...` or an external config file. The usual path is to edit the source and rebuild.

## Appliance restore

This build does not restore appliances. It does not read `autonomy.state.dir`, `autonomy.state.restore-window`, or `autonomy.mappings.file`. It does not create a state directory mode `0700` or a state file mode `0600`, and it does not refuse symlinks for those paths. MQTT publish stays off with `autonomy.mqtt.enabled=false`. Command subscribe stays off with `autonomy.mqtt.commands-enabled=false` and `autonomy.mqtt.command-max-bytes=4096`. This build does not set `autonomy.mqtt.commands-enabled` to true and does not set `freedriver.appliances.live-commands`. Mappings stay the fixed file `~/.config/autonomy/mappings_v2.json`. This build does not write `appliance-state.json`. Quarkus tests use `MappingsTestResource`, which prepares that same mappings file. They do not set `%test.autonomy.state.dir`, and this build has no `ProxyBoardTestResource`.

A systemd drop-in or a `-D` flag for those three properties does nothing here. This build does not log `No saved appliance state exists; leaving every appliance off`, and a missing state file is not what keeps appliances off.

Those settings, the `0700` and `0600` modes, symlink refusal, and appliance restore belong to unmerged branch `uni/50-restore-appliance-state` (closed pull request #51). They are not operating instructions for the service this tree runs. On that branch only, the defaults are:

| Property | Default on `uni/50-restore-appliance-state` | Environment variable on that branch | What that branch would control |
|---|---|---|---|
| `autonomy.state.dir` | `${user.home}/.local/share/autonomy` | `AUTONOMY_STATE_DIR` | Directory that would hold `appliance-state.json`, outside the deployed build. |
| `autonomy.state.restore-window` | `30s` | `AUTONOMY_STATE_RESTORE_WINDOW` | How often that branch would restore one board. A duration such as `30s`. |
| `autonomy.mappings.file` | `${user.home}/.config/autonomy/mappings_v2.json` | `AUTONOMY_MAPPINGS_FILE` | Mappings file that branch would read instead of the fixed path. |

That branch would create the state directory mode `0700` and `appliance-state.json` mode `0600`, refuse symlinks, and on a first run with no saved file leave every appliance off and log `No saved appliance state exists; leaving every appliance off`. Its `%test` profile would set `autonomy.state.dir` to `${java.io.tmpdir}/autonomy-quarkus-test`, and `ProxyBoardTestResource` would point `autonomy.mappings.file` at a temporary mappings file. None of that runs in this build.

## Systemd Units

The units in `deploy/systemd/` are the source of truth. The build copies them.

### autonomy.service

```ini
[Unit]
Description=Autonomy Daemon
After=network.target

[Service]
# Note: %h is systemd user specifier for $HOME
ExecStart=/usr/bin/java -jar %h/.local/autonomy/quarkus-run.jar -DrestartOn=%h/.local/autonomy/quarkus-run.jar -DrestartCommand="%h/bin/restartAutonomy.sh"
# Quarkus handles SIGTERM (via kill $MAINPID) and runs shutdown hooks.
ExecStop=/bin/kill $MAINPID
KillMode=control-group
Restart=always

[Install]
WantedBy=multi-user.target
```

The unit uses portable `%h` specifiers. `-DrestartOn` and `-DrestartCommand` let `RestartOnHashChangeService` restart the process when the deployed jar changes.

Enable and start the unit as shown above.

### autonomy-next.service

```ini
[Unit]
Description=Autonomy Next Daemon
After=network.target

[Service]
WorkingDirectory=%h/.local/autonomy-next
ExecStart=/usr/bin/java -Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8080 -jar %h/.local/autonomy-next/quarkus-run.jar
ExecStop=/bin/kill $MAINPID
KillMode=control-group
TimeoutStopSec=20
Restart=always
RestartSec=3

[Install]
WantedBy=default.target
```

## MQTT

Publishing runs when `autonomy.mqtt.enabled=true` on `autonomy-next.service`. Command subscribe stays off unless `autonomy.mqtt.commands-enabled` is the value `true`. Any other value logs once, does not subscribe, and does not stop the process. This build does not turn that setting on and does not turn on `freedriver.appliances.live-commands`. `autonomy.service` stays disabled. The two units would otherwise share the client id `autonomy-<instanceId>`. Leave the unit files unchanged and put the keys in `~/.config/systemd/user/autonomy-next.service.d/mqtt.conf`. The password file is that user's `~/.config/autonomy/mqtt-password` (mode `600` or `400`); the unit stores the path. The password is never logged. Leave the CA file unset. The broker certificate is Let's Encrypt. The keys, a placeholder example, and `journalctl --user-unit autonomy-next.service | grep 'io.freedriver.autonomy.mqtt'` are in [docs/mqtt.md](../docs/mqtt.md).

## Other Notes

* **Shutdown**: Stop the daemon with the helper scripts or `systemctl --user stop autonomy`. Quarkus runs its shutdown hooks on SIGTERM.
* **Restart support**: `-DrestartOn` and `-DrestartCommand` are read by `RestartOnHashChangeService`.
* **Updating**: Re-run `mvn install -pl quarkus -am`, then restart the user unit. That is a local rebuild. A version-tag deploy uses the release archive; see [Versioned releases](#versioned-releases).
* **Quarkus layout**: The deployed layout is the fast-jar produced by `quarkus:build`. A `native` profile exists and is not the default deploy path.
* **Permissions**: The antrun plugin marks the scripts executable. The units are user units.

## Container image

`quarkus/src/main/docker/Dockerfile.jvm` builds an image of that fast-jar and installs the pinned `joystick` package (`1:1.8.1-2build1`) so `jstest` is on `PATH`. That package also installs `evdev-joystick`, `fftest`, `ffmvforce`, `ffcfstress`, `ffset`, `jscal`, `jscal-restore`, and `jscal-store`. It depends on `libsdl2`. `ffmvforce` links `libSDL2`. The other binaries in the package do not. `evtest` and `inputattach` stay out. Do not run the container `--privileged` and do not mount all of `/dev`. The process is the non-root user `autonomy` (uid 10001, home `/home/autonomy`). The fast-jar under `/deployments` is owned by root and is not writable by that user. The image has no house config. Mount the two directories the process already reads, `~/.config/autonomy` and `~/.config/jsonlink`, and keep the root filesystem read-only:

```
mvn -pl quarkus -am package -DskipTests
docker build -f quarkus/src/main/docker/Dockerfile.jvm -t autonomy quarkus
docker run --rm --read-only --tmpfs /tmp \
  -p 127.0.0.1:8080:8080 \
  -v "$HOME/.config/autonomy:/home/autonomy/.config/autonomy" \
  -v "$HOME/.config/jsonlink:/home/autonomy/.config/jsonlink" \
  autonomy
```

Publish `127.0.0.1:8080:8080` until the HTTP API has an auth boundary. The process listens on `0.0.0.0` inside the container network namespace. `package` leaves the fast-jar in `quarkus/target/quarkus-app/` and does not copy it into `~/.local`. JDK 23 and the pinned MQTT contract from the deploy steps above are required before that Maven command. The runtime image is a digest-pinned Temurin 25 JRE, which runs that Java 23 bytecode. The image directories `/home/autonomy/.config/autonomy` and `/home/autonomy/.config/jsonlink` are empty mount points. Those directories must be traversable by uid 10001, and writable if the process should update mappings or `connectors.json`. `GET /rest/simple/` is served on port 8080. MQTT stays off unless `autonomy.mqtt.enabled=true` is set at runtime. Leave the password file on the mount. `chown` `mqtt-password` to `10001:10001` and keep mode `600` or `400`. Do not add group or other bits: `PasswordFiles` rejects the file when any of those bits are set. Do not copy the password into the image.

## Events GUI

`deploy/compose.mongo-express.yaml` adds mongo-express for the `events` collection in database `autonomy`. The image is `mongo-express:1.0.2-20`, pinned by digest `sha256:30fc6333779340217c6a2bc0bc3cd53aa46028ab8fc6caef41bd00bf0faacf53`. The only published port is `127.0.0.1:8081`.

The browser password and the `events-reader` password are host environment values. Copy `deploy/mongo-express.env.example` outside the repo, fill it, and pass that path with `docker compose --env-file`. Do not commit the filled file. The container entrypoint refuses an empty password and the image defaults `admin` / `pass`, cookie secret `cookiesecret`, and session secret `secret`. It also refuses Mongo admin mode. The image default `ME_CONFIG_MONGODB_ENABLE_ADMIN=true` and the unauthenticated URL `mongodb://mongo:27017` are overridden at start. The connection user is `events-reader` on database `autonomy`.

`deploy/mongo-express-user.sh` creates that user, or pins it back to the `read` role on `autonomy`. It needs `MONGO_ADMIN_URL` with credentials and `MONGO_EXPRESS_DB_PASSWORD`. It does not start MongoDB and does not turn authentication off. The admin URL stays out of the GUI env file. `events-reader` is not the Mongo root user. If `MONGO_WRITER_USERNAME` is `events-reader`, the script stops.

Bring the GUI up only beside the compose service named `mongo`, on the same compose project, after that service has authentication on:

```
docker compose --env-file /path/outside/the/repo/mongo-express.env \
  -f deploy/compose.yaml \
  -f deploy/compose.mongo-express.yaml \
  up -d mongo-express
```

`compose.yaml` is the autonomy compose unit. This overlay does not add it. Kaze opens `127.0.0.1:8081` through an SSH tunnel. This file does not change `autonomy.mqtt.commands-enabled` or `freedriver.appliances.live-commands`.

## Source of Truth

* The files in `deploy/systemd/` and this README are canonical.
* The antrun plugin in `quarkus/pom.xml` copies them from `../../deploy/systemd/`.

For a local trial of the units, copy `deploy/systemd/*.service` to `~/.local/share/systemd/user/` and the scripts to `~/bin`, then run `systemctl --user daemon-reload`.
