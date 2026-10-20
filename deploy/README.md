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
  * `mappings_v2.json`: appliance and connector mappings (relays, lights, water valves, pumps) and `eventTTL`. The path is the setting `autonomy.mappings.file` (see [Appliance restore](#appliance-restore)).
  * `sensor_history.json`: analog sensor history read by the alias service.
  * Other files that may already sit in the directory: `soc_calculations.json`, pin notes, backups.
  * The service starts with no JDBC datasource. Event writes are accepted and discarded.

* **Appliance state** (default `~/.local/share/autonomy/`, outside the deployed build):
  * `appliance-state.json`: the last on or off state of every appliance. Deploys and rollbacks do not touch this directory. See [Appliance restore](#appliance-restore).

* **Systemd user units** (`~/.local/share/systemd/user/`):
  * `autonomy.service`: the main daemon.
  * `autonomy-next.service`: the next unit, listening on `0.0.0.0:8080`.

* **Helper scripts** (`~/bin/`, executable):
  * `restartAutonomy.sh`, `startAutonomy.sh`, `stopAutonomy.sh`, `autolog.sh`.

* **App config** (baked into the jar, overridable at runtime):
  * Inside `quarkus-app/`: `application.properties` carries no JDBC datasource. The HTTP server uses Quarkus' default port 8080.
  * Runtime overrides use `-Dquarkus...` or an external config file. The usual path is to edit the source and rebuild.

## Appliance restore

After a board reset or a USB reconnect, autonomy puts each appliance back to the on or off state it last saved. Set these on the user unit (a drop-in under `~/.config/systemd/user/autonomy.service.d/`, or the equivalent for `autonomy-next.service`). Quarkus also accepts the property names as `-D` system properties.

| Property | Default | Environment variable | What it controls |
|---|---|---|---|
| `autonomy.state.dir` | `${user.home}/.local/share/autonomy` | `AUTONOMY_STATE_DIR` | Directory that holds `appliance-state.json`. It sits outside `~/.local/autonomy` and `~/.local/autonomy-next`, so a deploy or rollback leaves it alone. Mount this directory as a volume when the service moves to Docker Compose. |
| `autonomy.state.restore-window` | `30s` | `AUTONOMY_STATE_RESTORE_WINDOW` | Each board is restored at most once inside this window. A reconnect inside the window is logged, and one restore for that board runs when the window ends. A duration such as `30s`. |
| `autonomy.mappings.file` | `${user.home}/.config/autonomy/mappings_v2.json` | `AUTONOMY_MAPPINGS_FILE` | Appliance and connector mappings (relays, lights, water valves, pumps) and `eventTTL`. |

The service creates the state directory mode `0700` and `appliance-state.json` mode `0600`, both owned by the service user. Parent directories it has to create keep the default permissions. A state directory that already allows group or other access is tightened to `0700`, and a warning is logged. The directory must be a real directory owned by the service user, and the state file, when it exists, a regular file owned by that user with the group and other write bits clear. The service does not follow symlinks for either check. Otherwise it logs an error and skips the restore and the save. Each write goes to a temp file in that directory, is forced to disk, and is renamed over the previous file.

The first run starts with no saved file. Every appliance stays off. The log line is `No saved appliance state exists; leaving every appliance off`. Set the appliances by hand before the fridge on/off restart check.

Quarkus tests do not use the real state directory. The `%test` profile sets `autonomy.state.dir` to `${java.io.tmpdir}/autonomy-quarkus-test`, and those tests read and write that directory instead. `ProxyBoardTestResource` points `autonomy.mappings.file` at a temporary mappings file before the application starts, so that test resource does not change the default mappings file.

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

## Source of Truth

* The files in `deploy/systemd/` and this README are canonical.
* The antrun plugin in `quarkus/pom.xml` copies them from `../../deploy/systemd/`.

For a local trial of the units, copy `deploy/systemd/*.service` to `~/.local/share/systemd/user/` and the scripts to `~/bin`, then run `systemctl --user daemon-reload`.
