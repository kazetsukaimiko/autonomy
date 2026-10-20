# Autonomy Deployment

This folder contains the deployment artifacts and documentation for running Autonomy, the Quarkus automation service, on a target machine (for example hakobune).

The Maven antrun plugin in the `quarkus` module copies the built application, the systemd user units, and the helper scripts into the user account.

## Deploy Process

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

This build does not restore appliances. It does not read `autonomy.state.dir`, `autonomy.state.restore-window`, or `autonomy.mappings.file`. It does not create a state directory mode `0700` or a state file mode `0600`, and it does not refuse symlinks for those paths. The only autonomy setting in `application.properties` is `autonomy.mqtt.enabled=false`. Mappings stay the fixed file `~/.config/autonomy/mappings_v2.json`. This build does not write `appliance-state.json`. Quarkus tests use `MappingsTestResource`, which prepares that same mappings file. They do not set `%test.autonomy.state.dir`, and this build has no `ProxyBoardTestResource`.

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

Publishing runs when `autonomy.mqtt.enabled=true` on `autonomy-next.service`. `autonomy.service` stays disabled. The two units would otherwise share the client id `autonomy-<instanceId>`. Leave the unit files unchanged and put the keys in `~/.config/systemd/user/autonomy-next.service.d/mqtt.conf`. The password file is that user's `~/.config/autonomy/mqtt-password` (mode `600` or `400`); the unit stores the path. The password is never logged. Leave the CA file unset. The broker certificate is Let's Encrypt. The keys, a placeholder example, and `journalctl --user-unit autonomy-next.service | grep 'io.freedriver.autonomy.mqtt'` are in [docs/mqtt.md](../docs/mqtt.md).

## Other Notes

* **Shutdown**: Stop the daemon with the helper scripts or `systemctl --user stop autonomy`. Quarkus runs its shutdown hooks on SIGTERM.
* **Restart support**: `-DrestartOn` and `-DrestartCommand` are read by `RestartOnHashChangeService`.
* **Updating**: Re-run `mvn install -pl quarkus -am`, then restart the user unit.
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

## Source of Truth

* The files in `deploy/systemd/` and this README are canonical.
* The antrun plugin in `quarkus/pom.xml` copies them from `../../deploy/systemd/`.

For a local trial of the units, copy `deploy/systemd/*.service` to `~/.local/share/systemd/user/` and the scripts to `~/bin`, then run `systemctl --user daemon-reload`.
