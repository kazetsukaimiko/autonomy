# Autonomy Deployment

This folder contains the deployment artifacts and documentation for running Autonomy, the Quarkus automation service, on a target machine (for example hakobune).

The Maven antrun plugin in the `quarkus` module copies the built application, the systemd user units, and the helper scripts into the user account.

## Deploy Process

1. Build and install (from the autonomy repo root or the `quarkus` module):
   ```
   mvn install -pl quarkus -am
   ```
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
  * `mappings_v2.json`: appliance and connector mappings (relays, lights, water valves, pumps) and `eventTTL`.
  * `sensor_history.json`: analog sensor history read by the alias service.
  * Other files that may already sit in the directory: `soc_calculations.json`, pin notes, backups.
  * The process keeps appliance state in those files. The service starts with no JDBC datasource. Event writes are accepted and discarded.

* **Systemd user units** (`~/.local/share/systemd/user/`):
  * `autonomy.service`: the main daemon.
  * `autonomy-next.service`: the next unit, listening on `0.0.0.0:8080`.

* **Helper scripts** (`~/bin/`, executable):
  * `restartAutonomy.sh`, `startAutonomy.sh`, `stopAutonomy.sh`, `autolog.sh`.

* **App config** (baked into the jar, overridable at runtime):
  * Inside `quarkus-app/`: `application.properties` carries no JDBC datasource. The HTTP server uses Quarkus' default port 8080.
  * Runtime overrides use `-Dquarkus...` or an external config file. The usual path is to edit the source and rebuild.

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
