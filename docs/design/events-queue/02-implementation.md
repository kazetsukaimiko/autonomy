# Implementation

The code that builds the first document is a later change. This document names that code. It adds no stage, type, or message.

## Existing code

`quarkus/src/main/java/io/freedriver/autonomy/events/JoystickEventSource.java` reads joystick devices through `AllJoysticks` and fires one `JoystickEvent` from `JoystickEventConverter` on its `Event<JoystickEvent>`. A reading with no subject title is logged and not fired. A failed fire is logged and not retried. Nothing in this file changes. It remains the joystick listener, and its fire stays unqualified.

`jpa/src/main/java/io/freedriver/autonomy/jpa/entity/event/input/joystick/JoystickEventConverter.java` builds the `JoystickEvent` fields, including a new `id`, `generationOrigin` of `NON_HUMAN`, and `BUTTON_UP` when a button value is `0`. Nothing in this file changes.

`model/src/main/java/io/freedriver/autonomy/event/input/joystick/JoystickEvent.java` is the `JoystickEvent` contract. Nothing in this file changes.

`model/src/main/java/io/freedriver/autonomy/events/store/EventTypes.java` holds `JOYSTICK_EVENT`, the string `joystick.event`. Nothing in this file changes. `QueuedEventMessage` does not exist yet. It will sit beside this file and beside `EventRecord.java`, at `model/src/main/java/io/freedriver/autonomy/events/store/QueuedEventMessage.java`. No body here.

`quarkus/src/main/java/io/freedriver/autonomy/service/JoystickEventCrudService.java` observes the listener's unqualified `JoystickEvent` and stores it. Nothing in this file changes. It is not a stage, and it does not move onto the queue.

`quarkus/src/main/java/io/freedriver/autonomy/service/SimpleAliasService.java` is the CDI recipient and the action. `handleJoystickEvent` observes the unqualified `JoystickEvent`, skips an initial event or a non-button, and for each mapping calls the two-argument `handleJoystickEvent`. That method builds the key `number`, a colon, and `eventValue`, reads `Mapping.controlMap`, and on a hit calls `toggleAppliances`. `toggleAppliances` reads the cached digital state and writes the opposite through `ConnectorService.send`. The two-argument method and `toggleAppliances` do not change. The one-argument observer changes only its qualifier, from unqualified to `EventsQueue`. Its body does not change.

`quarkus/src/main/java/io/freedriver/autonomy/service/ConnectorService.java` sends a board request. Nothing in this file changes. The action still calls it.

`quarkus/src/main/java/io/freedriver/autonomy/cdi/qualifier/ByUUID.java` is an existing qualifier. `EventsQueue` does not exist yet. It will sit beside this file, at `quarkus/src/main/java/io/freedriver/autonomy/cdi/qualifier/EventsQueue.java`. No fields and no body here.

`mqtt-api/src/main/java/io/freedriver/autonomy/mqtt/MqttConnector.java` is the house-broker client. `MqttConnectors` loads exactly one implementation of it. Nothing in those types changes. The events queue does not implement `MqttConnector` and is not registered as one, so that single implementation stays the house-broker client.

`EventsQueueClient` does not exist yet. It will sit beside `MqttConnector.java`, at `mqtt-api/src/main/java/io/freedriver/autonomy/mqtt/EventsQueueClient.java`. `EventsQueueClients` will sit beside `MqttConnectors.java` and load that client the same way, from its own service file. No body here.

`mqtt-impl/src/main/java/io/freedriver/autonomy/mqtt/impl/PahoMqttConnector.java` is the Paho client for appliance state and commands on the house broker. Nothing in this file changes. `PahoEventsQueueClient` does not exist yet. It will sit beside this file, at `mqtt-impl/src/main/java/io/freedriver/autonomy/mqtt/impl/PahoEventsQueueClient.java`, and it is the `EventsQueueClient` implementation. No body here.

`mqtt-impl/src/test/java/io/freedriver/autonomy/mqtt/impl/EmbeddedMqttBroker.java` starts Moquette for connector tests. Nothing in this file changes. `MoquetteEventsQueue` does not exist yet. It will sit in main sources beside that test broker, at `mqtt-impl/src/main/java/io/freedriver/autonomy/mqtt/impl/MoquetteEventsQueue.java`. No body here.

`mqtt-impl/pom.xml` depends on `io.moquette:moquette-broker` at version `0.17` from `autonomy-3p-bom`, test scope only, for `EmbeddedMqttBroker`. The dependency leaves test scope so the running process can host `MoquetteEventsQueue`. No broker properties are part of this document.

`quarkus/src/main/java/io/freedriver/autonomy/mqtt/MqttStatePublisher.java` starts `MqttConnector` when the house broker is enabled. Nothing in this file changes. The events queue does not start from this class and does not follow that enabled flag.

`EventsQueuePublisher`, `EventsQueueReceiver`, and `EventsQueueRouter` do not exist yet. They will sit beside `JoystickEventSource.java`, at:

- `quarkus/src/main/java/io/freedriver/autonomy/events/queue/EventsQueuePublisher.java`
- `quarkus/src/main/java/io/freedriver/autonomy/events/queue/EventsQueueReceiver.java`
- `quarkus/src/main/java/io/freedriver/autonomy/events/queue/EventsQueueRouter.java`

No bodies here. Quarkus already depends on `mqtt-api` to compile and on `mqtt-impl` at runtime. These three call `EventsQueueClient` only. They do not call Paho or Moquette types.

## How document 1 is built

### Joystick listener

`JoystickEventSource` and `JoystickEventConverter` realize this stage. When the step is done, a `JoystickEvent` has been fired, or the reading was dropped inside `JoystickEventSource`. `AllJoysticks` is unchanged and stays inside this stage.

### CDI event

The same `JoystickEventSource` fire realizes this stage. The type is `JoystickEvent`. The handoff from the listener is direct. When the step is done, the unqualified observers of that fire have been invoked. Those observers are `EventsQueuePublisher` and the unchanged `JoystickEventCrudService`. `SimpleAliasService` is not among them.

### MQTT publish

`EventsQueuePublisher` realizes this stage. It observes only the unqualified `JoystickEvent`. For each one it builds a `QueuedEventMessage` with `type` `joystick.event` and that event as `payload`, and it publishes through `EventsQueueClient`. `PahoEventsQueueClient` is that client. `MoquetteEventsQueue` is the broker that accepts the topic `events`.

The handoff from the CDI event is direct and carries `JoystickEvent`. The message to MQTT receive is `QueuedEventMessage`. When the step is done, Moquette has accepted that message and `EventsQueuePublisher` has returned to the listener's fire, or acceptance failed, the failure was logged, and the recipient was not called. There is no second publish from an `EventsQueue` fire.

### MQTT receive

`MoquetteEventsQueue` and `EventsQueueReceiver` realize this stage. `MoquetteEventsQueue` is up for the life of the process, including when `MqttStatePublisher` leaves the house broker off. `EventsQueueReceiver` subscribes only to `events`, at QoS 1, with retain false. `PahoEventsQueueClient` delivers that subscription off the `JoystickEventSource` fire, and only after publish has returned.

The handoff from publish is the `QueuedEventMessage` message. When the step is done, that message has been handed to `EventsQueueRouter`, or the message was not on `events` and was ignored. Receive does not read `type`.

### Router

`EventsQueueRouter` realizes this stage. The handoff from receive is direct and carries `QueuedEventMessage`. The router fires `JoystickEvent` with the `EventsQueue` qualifier only when `type` is `joystick.event`, the payload is a `JoystickEvent`, `id` is non-blank, and that `id` has not yet been handed to the recipient during this process. It remembers those ids. It drops every other message, including a second delivery of the same `id`.

When the step is done, `SimpleAliasService` has been given that one `JoystickEvent` through the qualifier, or the message was dropped. `EventsQueuePublisher` did not observe the fire. `JoystickEventCrudService` did not observe it either.

### CDI recipient

`SimpleAliasService.handleJoystickEvent` realizes this stage, observing `EventsQueue` and not the listener's fire. The handoff from the router is direct and carries `JoystickEvent`. The method body is the body that already skips an initial event and a non-button, then looks up each `Mapping`.

When the step is done, either the action was called once per mapping that has the key, or the event was not a mapped button and the action was not called.

### Action

`toggleAppliances` in `SimpleAliasService`, reached from the two-argument `handleJoystickEvent`, realizes this stage. The handoff is direct and carries the `Mapping` and the name list at the key `number`, a colon, and `eventValue`. The write still goes through `ConnectorService.send`.

When the step is done, each named appliance on that `connectorId` has been written to the opposite of the digital state the action read, or the write failed and the existing recipient log kept the failure. The listener's fire is not on this stack.

### Types and the qualifier

`JoystickEvent.java` realizes `JoystickEvent`. `QueuedEventMessage.java` realizes `QueuedEventMessage` with the two fields `type` and `payload`. `EventsQueue.java` realizes the qualifier with no fields. `io.freedriver.jsonlink.config.v2.Mapping` realizes `Mapping`. None of these four gains a method in this design. The later change adds the two new files and the qualifier on the existing observer, and it does not edit `Mapping`.
