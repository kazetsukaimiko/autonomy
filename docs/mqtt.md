# MQTT appliance state

Autonomy publishes this home's appliance state to the freedriver.io Mosquitto broker. It does not subscribe to commands. Publishing stays off unless `autonomy.mqtt.enabled=true`.

Every MQTT line uses the logger category `io.freedriver.autonomy.mqtt`. On hakobune, Techops greps that string:

```
journalctl --user-unit autonomy.service | grep 'io.freedriver.autonomy.mqtt'
```

Per-publish lines are DEBUG. The default level is INFO. Set `quarkus.log.category."io.freedriver.autonomy.mqtt".level=DEBUG` to see them. "Sent" in the once-a-minute INFO summary is a QoS 1 publish that returned after PUBACK. `acknowledged` counts those; `failed` counts publishes that did not.

## Modules

* `mqtt-api` is the client-agnostic API: `MqttConnector`, `MqttSettings`, `ApplianceStateSource`, and the logger category. It has no wire DTOs and no MQTT client.
* `mqtt-impl` is the Eclipse Paho MQTT v3 1.2.5 client, TLS, reconnect, and logging. It is the only module that references Paho or `io.freedriver:freedriver-mqtt-contract`.
* `quarkus` depends on `mqtt-api` at compile scope and on `mqtt-impl` at runtime scope. Service sources do not mention Paho. A later client swap stays inside `mqtt-impl`.

The wire types are freedriver-web's `io.freedriver:freedriver-mqtt-contract` (`Appliance`, `ApplianceStateMessage`, `ApplianceJson`, `ApplianceSchemas`). Autonomy's own `mqtt-contract` module is a different body shape and is not used here. It stays in the reactor because CI still builds it. Retiring it is a follow-up.

## Contract pin

The commit is the single line in `freedriver-web.sha` at the repo root. The installed Maven coordinate is `io.freedriver:freedriver-mqtt-contract:1.0.0-SNAPSHOT`, built from that commit. No GitHub Packages token.

JDK 23 is required. freedriver-web sets `maven.compiler.release` to 23, and autonomy compiles with source and target 23.

From the autonomy repo root:

```
./scripts/install-freedriver-mqtt-contract.sh
```

That script checks out `https://github.com/kazetsukaimiko/freedriver-web` at the pinned commit and runs:

```
./mvnw --batch-mode -pl mqtt-contract -am install -DskipTests
```

Run it before any build that includes `mqtt-impl` or `quarkus` (`mvn -pl quarkus -am` now builds `mqtt-impl`).

## Wire

Topic: `freedriver/v1/{instanceId}/appliances` (`ApplianceSchemas.appliancesTopic`). QoS 1, retain false. `instanceId` is only the topic segment.

Body, periodic map (`appliedCommandId` is null until a later command card):

```json
{
  "instanceName": "Cabin",
  "appliedCommandId": null,
  "appliances": [
    {"applianceName": "fridge", "state": true}
  ]
}
```

TLS to `host:port` (default `mqtt.freedriver.io:8883`). Hostname verification stays on. With no `ca-file`, the JVM public CA trust store is the trust anchor (the broker uses Let's Encrypt). `ca-file` replaces that trust store with the certificates in the file. There is no certificate pin and no skip-verify.

Reconnect is autonomy's capped exponential backoff (1s initial, 60s cap, up to 20% jitter), not Paho automatic reconnect. It retries forever. Each new session is clean and publishes the current snapshot; a previous payload is not replayed. A failed first connect does not stop the process.

Names that are blank or longer than `ApplianceSchemas.NAME_MAX` (64) are left out of the body so one bad alias cannot fail the publish.

## State source

Names come from `SimpleAliasService.getMappings()` (`~/.config/autonomy/mappings_v2.json`). On/off comes from the `@ConnectorCache` `Map<PinCoordinate, Boolean>`. The publisher does not call `currentState()`, `makeView()`, or anything that reads or writes the serial boards.

The connector checks about once a second. It publishes when the cached map changes, on every connect, and at least every `publish-interval` (default 10s).

Duplicate names: boards are ordered by connector UUID. Within a board, mapping list order is kept. The first appliance with a given name is the one that is published. Later copies are dropped, and one WARN is logged on `io.freedriver.autonomy.mqtt`. The wire has no board id.

A pin that is not in the cache yet is omitted, not reported as off. If the mappings name appliances and none of them are cached, that cycle is not published, so an empty list is not sent in place of a house that simply has not been cached yet. An empty publish is sent only when the mappings themselves name no appliances.

`SimpleAliasHandler.setGroup` writes the board through `ConnectorService.writeDigital` and does not update the connector cache. Group flips therefore stay out of the published map until some other path writes the cache (`setState`, or a joystick toggle). This branch does not change that.

## Configuration

Prefix `autonomy.mqtt`. Jakarta Validation runs at startup. When MQTT is off, the required keys may be absent. When it is on, a missing or invalid required key stops startup. A bad password, an unreadable password file, or a broker that is down does not stop startup; the connector keeps retrying.

| Key | Default | Required when enabled |
| --- | --- | --- |
| `enabled` | `false` | |
| `host` | `mqtt.freedriver.io` | hostname or IPv4, no scheme |
| `port` | `8883` | 1–65535 |
| `username` | | yes |
| `password-file` | | yes, path only; the file is read by the connector and is never logged |
| `ca-file` | JVM public CAs | optional PEM or DER certificates |
| `client-id` | `autonomy-<instanceId>` | optional override |
| `instance-id` | | yes, UUID |
| `instance-name` | | yes, non-blank |
| `publish-interval` | `10s` | positive |
| `keepalive` | `60s` | positive; Paho receives whole seconds, minimum 1 |
| `connect-timeout` | `10s` | positive; Paho receives whole seconds, minimum 1 |

Startup INFO lists host, port, username, client id, instance id, instance name, trust source (`jvm-cacerts` or `ca-file`), and publish interval. It does not list the password, the password file's contents, or the CA bytes.

Connect failures name the cause: `certificate not trusted`, `hostname mismatch`, `bad credentials` (MQTT code 4), `not authorized` (code 5), `unreachable or timeout`. An unreadable password or CA file is `password file unreadable` or `ca file unreadable`. INFO logs every reconnect attempt with its number and delay. WARN logs every 5th consecutive failure with the attempt number, the next delay, and the cause.

Example (environment file or Quarkus system properties; do not commit the password):

```
autonomy.mqtt.enabled=true
autonomy.mqtt.host=mqtt.freedriver.io
autonomy.mqtt.port=8883
autonomy.mqtt.username=autonomy
autonomy.mqtt.password-file=/path/to/autonomy.pass
autonomy.mqtt.instance-id=550e8400-e29b-41d4-a716-446655440000
autonomy.mqtt.instance-name=Cabin
autonomy.mqtt.publish-interval=10s
```

Equivalent environment variables: `AUTONOMY_MQTT_ENABLED`, `AUTONOMY_MQTT_USERNAME`, `AUTONOMY_MQTT_PASSWORD_FILE`, `AUTONOMY_MQTT_INSTANCE_ID`, `AUTONOMY_MQTT_INSTANCE_NAME`, and the same pattern for the other keys.

Only one of `autonomy.service` and `autonomy-next.service` may set `enabled=true`. Both would use the client id `autonomy-<instanceId>`. Leave the unit files as they are and enable MQTT from one drop-in, for example `~/.config/systemd/user/autonomy.service.d/mqtt.conf`:

```
[Service]
Environment=AUTONOMY_MQTT_ENABLED=true
Environment=AUTONOMY_MQTT_USERNAME=autonomy
Environment=AUTONOMY_MQTT_PASSWORD_FILE=/path/to/autonomy.pass
Environment=AUTONOMY_MQTT_INSTANCE_ID=550e8400-e29b-41d4-a716-446655440000
Environment=AUTONOMY_MQTT_INSTANCE_NAME=Cabin
```

Do not add the same drop-in to `autonomy-next.service`.

## CI

`.github/workflows/ci.yml` still verifies only `bom,jpa,api,mqtt-contract`, so this branch does not turn that job red. The verify job needs the contract install and the new modules before `mqtt-api` / `mqtt-impl` tests run there. After the existing "Install freedriver SNAPSHOT" step:

```yaml
      - name: Read freedriver-web pin
        id: freedriver-web
        run: echo "sha=$(tr -d '[:space:]' < freedriver-web.sha)" >> "$GITHUB_OUTPUT"

      - name: Checkout freedriver-web
        uses: actions/checkout@v4
        with:
          repository: kazetsukaimiko/freedriver-web
          ref: ${{ steps.freedriver-web.outputs.sha }}
          path: freedriver-web

      - name: Install freedriver-mqtt-contract
        working-directory: freedriver-web
        run: ./mvnw --batch-mode --no-transfer-progress -pl mqtt-contract -am install -DskipTests
```

Replace the verify command with:

```yaml
      - name: Build, test, and Spotless check
        run: mvn --batch-mode --no-transfer-progress clean verify -pl bom,jpa,api,mqtt-contract,mqtt-api,mqtt-impl -am
```

JDK 23 is already set up earlier in the job, which `./mvnw` needs in order to compile the contract. The state-source test lives in `quarkus`, which this workflow still excludes (the existing "VEDirect refactor" note). Adding `quarkus` to `-pl` is that separate follow-up, not this workflow edit.
