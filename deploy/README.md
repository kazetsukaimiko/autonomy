# Autonomy Deployment

This folder contains the deployment artifacts and documentation for running Autonomy (the Quarkus-based automation service) in production on a target machine (e.g. hakobune or similar).

The build process (via Maven antrun in the `quarkus` module) automatically deploys:

* The built application (fast-jar layout).
* The systemd user units and helper scripts.

## Deploy Process

1. Build and deploy (from the autonomy repo root or the `quarkus` module):
   ```
   mvn install -pl quarkus -am
   ```
   - This produces `quarkus/target/quarkus-app/...` (and `quarkus-run.jar`).
   - The antrun plugin (bound to `install` phase) copies:
     * App files → `~/.local/autonomy/` (full layout + top-level runner jar).
     * `*.service` files → `~/.local/share/systemd/user/`.
     * `*.sh` scripts → `~/bin/` (and makes them executable).

2. On the target machine (as the user, e.g. `luna`):
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

4. (Optional) H2 TCP server (for external console/tools or direct DB access):
   ```
   systemctl --user enable --now h2.service
   # Stop gracefully
   systemctl --user stop h2
   ```

## Locations on Target Machine

* **Application / JARs**:
  * `~/.local/autonomy/quarkus-run.jar` (the executable jar).
  * `~/.local/autonomy/quarkus-app/` (the exploded fast-jar layout with `app/`, `lib/`, etc. for Quarkus).
  * These are overwritten on each `mvn install` of the `quarkus` module.

* **Configuration and Data** (`~/.config/autonomy/`):
  * `mappings_v2.json`: Appliance/connector mappings (for relays, lights, water valves/pumps etc.), `eventTTL` (e.g. 7 DAYS for pruning old events).
  * `autonomy.h2db.mv.db` (and .trace etc.): The H2 database file (used by JPA/Hibernate for events, etc.).
  * Other: `soc_calculations.json`, `sensor_history.json`, pin notes (e.g. `pin_27_is_old...`), backups, `datasource-config.yml` (legacy).
  * The DB uses file-based H2 (configured via `application.properties` inside the jar: `quarkus.datasource.jdbc.url = jdbc:h2:~/.config/autonomy/autonomy.h2db` + `hibernate-orm.database.generation=update`).
  * **Warning**: H2 file DB can corrupt on abrupt kills/power loss. Use graceful shutdown (see units below). TTL enforcement etc. is done by the app services.

* **Systemd User Units** (`~/.local/share/systemd/user/`):
  * `autonomy.service`: The main daemon.
  * `h2.service` (optional): Separate H2 TCP/web server (ports 9092 TCP, 8082 web by default).

* **Helper Scripts** (`~/bin/`, must be +x):
  * `restartAutonomy.sh`, `startAutonomy.sh`, `stopAutonomy.sh`, `autolog.sh`.

* **App Config** (baked into jar or override):
  * Inside `quarkus-app/`: `application.properties` (H2 datasource, Hibernate, Liquibase, HTTP port 8080, etc.).
  * Can be overridden at runtime with `-Dquarkus...` or external config, but typically edited/redeployed via source + rebuild.

## Systemd Units (Cleaned Versions)

The units here (in `deploy/systemd/`) are the source of truth. They are copied by the build.

### autonomy.service (cleaned)

```ini
[Unit]
Description=Autonomy Daemon
After=network.target

[Service]
# Note: %h is systemd user specifier for $HOME
ExecStart=/usr/bin/java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar %h/.local/autonomy/quarkus-run.jar -DrestartOn=%h/.local/autonomy/quarkus-run.jar -DrestartCommand="%h/bin/restartAutonomy.sh"
# Graceful shutdown: Quarkus handles SIGTERM (via kill $MAINPID) for proper shutdown hooks, datasource close etc.
# Do not depend on or post-stop h2 here (h2 is optional/separate; previous Requires+ExecStopPost could lead to abrupt kills and DB issues).
ExecStop=/bin/kill $MAINPID
KillMode=control-group
Restart=always

[Install]
WantedBy=multi-user.target
```

Key cleanups vs. original on hakobune:
* Removed `Requires=h2.service` and `ExecStopPost=systemctl --user stop h2`.
* H2 is now fully optional/independent (avoids ordering/kill races and DB corruption on forced stops).
* Uses portable `%h` specifiers (no hard-coded usernames/machines).
* Relies on Quarkus graceful shutdown for the main process (the `-Drestart*` props are app-specific for self-restart support).

Enable/start as shown above.

### h2.service (cleaned)

```ini
[Unit]
Description=H2 Database (optional TCP server for external access/console; autonomy can run without it using embedded H2)
After=network.target

[Service]
ExecStartPre=mkdir -p %h/.config/h2
# Avoid brittle old Maven pre-step (requires net + specific mvn version). Fetch jar via curl if missing.
ExecStartPre=/bin/bash -c 'if [ ! -f %h/.config/h2/h2-1.4.200.jar ]; then curl -sL -o %h/.config/h2/h2-1.4.200.jar https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar; fi'
ExecStart=/usr/bin/java -server -Xmx8g -Xms1g -cp %h/.config/h2/h2-1.4.200.jar org.h2.tools.Server -web -webAllowOthers -tcp -tcpPassword shutdownh2 -tcpAllowOthers -ifNotExists -baseDir %h/.config/h2
# Graceful shutdown via H2's tcpShutdown (instead of abrupt PID kill).
ExecStop=/usr/bin/java -cp %h/.config/h2/h2-1.4.200.jar org.h2.tools.Server -tcpShutdown tcp://localhost:9092 -tcpPassword shutdownh2

KillMode=process

[Install]
WantedBy=multi-user.target
```

Key cleanups:
* Replaced `mvn ...:get` pre-step (very brittle post-2020s, old plugin, no internet = fail) with simple curl fallback (idempotent; only downloads if missing).
* Explicit graceful `ExecStop` using H2's TCP shutdown protocol (the `KillMode=process` + tcpShutdown combo ensures clean close before any SIGKILL).
* Portable `%h`, no machine-specific paths.
* Documented as optional (the Quarkus app uses embedded H2 file mode by default; this unit is for the TCP server + web console).

## Other Notes

* **Graceful shutdown / H2 safety**: The main fixes address previous issues where stopping autonomy would force-kill h2 (or vice-versa via Requires/Post), leading to "Unable to read the page" corruption in the .mv.db file (see history of H2 inspection on hakobune). Quarkus app shutdown + explicit H2 tcpShutdown + no cross-requires = much safer. Always prefer the provided scripts or `systemctl --user stop`.
* **Debug**: The java command enables JDWP on *:5005 (for remote debug). Remove the `-agentlib:jdwp...` part in production if desired.
* **Restart support**: The `-DrestartOn=... -DrestartCommand=...` are used by the app (see Autonomy.java / RestartOnHashChangeService) to support in-place restarts without full systemctl.
* **Updating**: Re-run `mvn install -pl quarkus -am` on a dev machine, then the scripts on target (or use your CI to scp/rsync the artifacts + `systemctl --user restart`).
* **Legacy**: `datasource-config.yml` is old Thorntail-era (pre-Quarkus full migration); the current app uses Quarkus config (datasource + hibernate in `application.properties` inside the jar). `h2.service` uses very old H2 1.4.200 (for compat with existing DBs).
* **Quarkus specifics**: The deployed layout is "fast-jar" (from `quarkus:build`). For native, there's a profile but not the default deploy path.
* **Permissions**: Scripts are chmod'ed +x by the antrun. Units are user units (no root).

## Source of Truth

* The files in this `deploy/systemd/` dir (plus this README) are canonical.
* The build antrun (in `quarkus/pom.xml`) references them via relative path (`../../deploy/systemd/` from the module).
* The old copy under `quarkus/src/main/systemd/` is deprecated (kept only during transition; update references if you move things).

For development/testing the units locally, you can `cp deploy/systemd/* ~/.local/share/systemd/user/` (and sh to bin) then `systemctl --user daemon-reload`.

This setup supports the full revival plan (Quarkus 3, Jakarta, etc.) while keeping the systemd story simple and robust.