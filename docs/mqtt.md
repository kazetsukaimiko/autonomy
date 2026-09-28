# MQTT appliance state

Autonomy publishes this home's appliance state to the freedriver.io Mosquitto broker. Publishing runs when `autonomy.mqtt.enabled=true`.

Every MQTT line uses the logger category `io.freedriver.autonomy.mqtt`. Techops reads them with:

```
journalctl --user-unit autonomy-next.service | grep 'io.freedriver.autonomy.mqtt'
```

`autonomy-next.service` is the unit that enables MQTT. `autonomy.service` stays disabled.

Per-publish lines are DEBUG. The default level is INFO. Set `quarkus.log.category."io.freedriver.autonomy.mqtt".level=DEBUG` to see them. "Sent" in the once-a-minute INFO summary is a QoS 1 publish that returned after PUBACK. `acknowledged` counts those; `failed` counts publishes that returned without a PUBACK.

## Modules

* `mqtt-api` is the client-agnostic API: `MqttConnector`, `MqttSettings`, `ApplianceStateSource`, and the logger category. Wire messages and the MQTT client live in `mqtt-impl`.
* `mqtt-impl` is the Eclipse Paho MQTT v3 1.2.5 client, TLS, reconnect, and logging. It is the module that references Paho and `io.freedriver:freedriver-mqtt-contract`.
* `quarkus` depends on `mqtt-api` at compile scope and on `mqtt-impl` at runtime scope. Service sources call the `mqtt-api` interfaces. A later client swap stays inside `mqtt-impl`.

The wire types are freedriver-web's `io.freedriver:freedriver-mqtt-contract` (`Appliance`, `ApplianceStateMessage`, `ApplianceJson`, `ApplianceSchemas`). Autonomy's `mqtt-contract` module is a different body shape. It stays in the reactor because CI still builds it. Retiring it is a follow-up.

## Contract pin

The pin is one line, `freedriver-web.sha` at the repo root: the full 40-character commit SHA of `kazetsukaimiko/freedriver-web` `main` that `freedriver-mqtt-contract` was built from. Techops reads that file for build provenance:

```
tr -d '[:space:]' < freedriver-web.sha
```

Confirm that commit is contained in `main`:

```
SHA=$(tr -d '[:space:]' < freedriver-web.sha)
git clone --filter=blob:none --no-checkout https://github.com/kazetsukaimiko/freedriver-web.git /tmp/freedriver-web-pin
git -C /tmp/freedriver-web-pin fetch origin main
git -C /tmp/freedriver-web-pin merge-base --is-ancestor "$SHA" FETCH_HEAD && echo "$SHA is on main"
```

`./scripts/install-freedriver-mqtt-contract.sh` accepts a 40-character hex commit, checks out that commit, and installs the contract. The installed Maven coordinate is `io.freedriver:freedriver-mqtt-contract:1.0.0-SNAPSHOT`. The build uses that checkout.

JDK 23 is required. freedriver-web sets `maven.compiler.release` to 23, and autonomy compiles with source and target 23.

From the autonomy repo root:

```
./scripts/install-freedriver-mqtt-contract.sh
```

That script checks out `https://github.com/kazetsukaimiko/freedriver-web` at the pinned commit and runs:

```
./mvnw --batch-mode -pl mqtt-contract -am install -DskipTests
```

Run it before any build that includes `mqtt-impl` or `quarkus` (`mvn -pl quarkus -am` builds `mqtt-impl`).

## Wire

Topic: `freedriver/v1/{instanceId}/appliances` (`ApplianceSchemas.appliancesTopic`). QoS 1, retain false. `instanceId` is the topic segment.

Body, periodic map (`appliedCommandId` is null until a later command card):

```json
{
  "instanceName": "example",
  "appliedCommandId": null,
  "appliances": [
    {"applianceName": "fridge", "state": true}
  ]
}
```

TLS to `host:port` (default `mqtt.freedriver.io:8883`). Hostname verification is always on. Leave `ca-file` unset: the broker certificate is Let's Encrypt, and the JVM public CA trust store is the trust anchor. `ca-file` replaces that trust store with the certificates in the file, and hostname verification stays on either way. A certificate that is trusted but names a different host is refused and logged as `hostname mismatch`.

Reconnect is autonomy's capped exponential backoff (1s initial, 60s cap, up to 20% jitter), with each attempt number and delay logged. It retries for the life of the process. Each new session is clean and publishes the current snapshot. A failed first connect leaves the process running.

Names that are blank or longer than `ApplianceSchemas.NAME_MAX` (64) stay out of the body, so one bad alias leaves the rest of the publish intact.

## State source

Names come from `SimpleAliasService.getMappings()` (`~/.config/autonomy/mappings_v2.json`). On/off comes from the `@ConnectorCache` `Map<PinCoordinate, Boolean>`. The publisher reads those two sources.

The connector checks about once a second. It publishes when the cached map changes, on every connect, and at least every `publish-interval` (default 10s).

Duplicate names: boards are ordered by connector UUID. Within a board, mapping list order is kept. The first appliance with a given name is the one that is published. Later copies are dropped, and one WARN is logged on `io.freedriver.autonomy.mqtt`. The wire is keyed by appliance name.

The published map includes pins that are already in the cache. When the mappings name appliances and the cache is still empty, the publisher skips that cycle. An empty appliance list goes out when the mappings list zero appliances.

`SimpleAliasHandler.setGroup` writes the board through `ConnectorService.writeDigital` and does not update the connector cache. Group flips therefore stay out of the published map until some other path writes the cache (`setState`, or a joystick toggle).

## Configuration

Prefix `autonomy.mqtt`. The mapping is plain configuration. `MqttStatePublisher` validates `MqttStartupConfig` once the application is up. With MQTT disabled, the required keys are optional. With MQTT enabled, each missing or invalid key is named in one ERROR on `io.freedriver.autonomy.mqtt`. The password is never logged. MQTT stays off and the service keeps running:

```
MQTT off; invalid configuration keys=autonomy.mqtt.instanceId,autonomy.mqtt.passwordFile
```

A bad password, a missing password file, or a broker that is down leaves the process running; the connector keeps retrying.

The password file is a path the operator sets. On a deployed host it lives in that user's `~/.config/autonomy/` directory. Before the file is read, group and other permission bits are off (`600` or `400`; owner execute is also owner-only). A file that group or others can read, write, or execute keeps MQTT off. The process keeps running. One ERROR line on `io.freedriver.autonomy.mqtt` names the path. The password is never logged:

```
MQTT off; password file is readable or writable by group or others path=<path>
```

The reader removes one trailing `\n` or `\r\n`. A trailing space, a leading space, or a second newline stays part of the password.

| Key | Default | Required when enabled |
| --- | --- | --- |
| `enabled` | `false` | |
| `host` | `mqtt.freedriver.io` | hostname or IPv4 address |
| `port` | `8883` | 1–65535 |
| `username` | | yes |
| `password-file` | | yes, path only; owner-only mode; the password is never logged |
| `ca-file` | JVM public CAs | leave unset in deployment; the broker certificate is Let's Encrypt |
| `client-id` | `autonomy-<instanceId>` | optional override |
| `instance-id` | | yes, UUID |
| `instance-name` | | yes, non-blank |
| `publish-interval` | `10s` | positive |
| `keepalive` | `60s` | positive; Paho receives whole seconds, minimum 1 |
| `connect-timeout` | `10s` | positive; Paho receives whole seconds, minimum 1 |

Startup INFO lists host, port, username, client id, instance id, instance name, trust source (`jvm-cacerts` or `ca-file`), and publish interval. The password is never logged.

Connect failures name the cause: `certificate not trusted`, `hostname mismatch`, `bad credentials` (MQTT code 4), `not authorized` (code 5), `unreachable or timeout`, or `other` together with the cause's class name. An unreadable password or CA file is `password file unreadable` or `ca file unreadable`. INFO logs every reconnect attempt with its number and delay. WARN logs every 5th consecutive failure with the attempt number, the next delay, and the cause.

Example (placeholders). `ca-file` stays unset. A properties file stores the password path as an absolute path. The deployed file is `~/.config/autonomy/mqtt-password`, mode `600` or `400`.

```
autonomy.mqtt.enabled=true
autonomy.mqtt.host=mqtt.freedriver.io
autonomy.mqtt.port=8883
autonomy.mqtt.username=<broker-username>
autonomy.mqtt.password-file=/home/<user>/.config/autonomy/mqtt-password
autonomy.mqtt.instance-id=00000000-0000-4000-8000-000000000000
autonomy.mqtt.instance-name=example
autonomy.mqtt.publish-interval=10s
```

Equivalent environment variables: `AUTONOMY_MQTT_ENABLED`, `AUTONOMY_MQTT_USERNAME`, `AUTONOMY_MQTT_PASSWORD_FILE`, `AUTONOMY_MQTT_INSTANCE_ID`, `AUTONOMY_MQTT_INSTANCE_NAME`, and the same pattern for the other keys.

`autonomy-next.service` enables MQTT. `autonomy.service` stays disabled. The two units would otherwise share the client id `autonomy-<instanceId>`. Leave the unit files as they are and enable MQTT from `~/.config/systemd/user/autonomy-next.service.d/mqtt.conf`. `%h` is that user's home:

```
[Service]
Environment=AUTONOMY_MQTT_ENABLED=true
Environment=AUTONOMY_MQTT_USERNAME=<broker-username>
Environment=AUTONOMY_MQTT_PASSWORD_FILE=%h/.config/autonomy/mqtt-password
Environment=AUTONOMY_MQTT_INSTANCE_ID=00000000-0000-4000-8000-000000000000
Environment=AUTONOMY_MQTT_INSTANCE_NAME=example
```

Leave `AUTONOMY_MQTT_CA_FILE` unset.

## CI

Workflow coverage for these modules is [autonomy#61](https://github.com/kazetsukaimiko/autonomy/issues/61). That job builds `mqtt-api,mqtt-impl,quarkus`.
