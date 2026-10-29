# Implementation

This document names the code that builds document 1. It adds no stage, type, or message.

## Existing code

`api/src/main/java/io/freedriver/autonomy/events/EventSource.java` is the event-type SPI. Each source owns one domain and fires that payload with `Event.fireAsync`. CDI discovers `JoystickEventSource` and `VEDirectEventSource`. `EventSource.load()` stays for tests.

`quarkus/src/main/java/io/freedriver/autonomy/events/JoystickEventSource.java` reads joystick devices through `AllJoysticks` and fires one `JoystickEvent` from `JoystickEventConverter` on its `Event<JoystickEvent>` with `fireAsync`. A reading with no subject title is logged and not fired. A failed completion is logged and not retried. The fire stays unqualified.

`jpa/src/main/java/io/freedriver/autonomy/jpa/entity/event/input/joystick/JoystickEventConverter.java` builds the `JoystickEvent` fields, including a new `id`, `generationOrigin` of `NON_HUMAN`, and `BUTTON_UP` when a button value is `0`. Nothing in this file changes.

`model/src/main/java/io/freedriver/autonomy/event/input/joystick/JoystickEvent.java` is the `JoystickEvent` contract. Nothing in this file changes.

`model/src/main/java/io/freedriver/autonomy/events/store/EventTypes.java` holds `JOYSTICK_EVENT`, the string `joystick.event`. Nothing in this file changes. There is no `QueuedEventMessage`.

`quarkus/src/main/java/io/freedriver/autonomy/service/JoystickEventCrudService.java` observes `@ObservesAsync JoystickEvent` and stores it. It is the persist handler.

`quarkus/src/main/java/io/freedriver/autonomy/events/store/MongoEventStore.java` is the `EventStore`. A background ping sets presence. When Mongo is absent, `append`, `query`, and `purgeOlderThan` return without waiting on server selection.

`quarkus/src/main/java/io/freedriver/autonomy/service/SimpleAliasService.java` is the appliance handler and the action. `handleJoystickEvent` observes `@ObservesAsync JoystickEvent`, skips an initial event or a non-button, and for each mapping calls the two-argument `handleJoystickEvent`. That method builds the key `number`, a colon, and `eventValue`, reads `Mapping.controlMap`, and on a hit calls `toggleAppliances`. `toggleAppliances` reads the cached digital state and writes the opposite through `ConnectorService.send`. The two-argument method and `toggleAppliances` do not change.

`quarkus/src/main/java/io/freedriver/autonomy/service/ConnectorService.java` sends a board request. Nothing in this file changes. The action still calls it.

`quarkus/src/main/java/io/freedriver/autonomy/events/VEDirectEventSource.java` fires `VEDirectMessage` with `fireAsync`. `VEDirectMessageActor` observes `@ObservesAsync`. The same fan-out applies: persist and domain reaction stay off the reader thread.

`mqtt-api` and `mqtt-impl` stay the house-broker client for appliance state and commands. They do not carry autonomy events.

There is no `EventsQueue`, `EventsQueuePublisher`, `EventsQueueReceiver`, `EventsQueueRouter`, `EventsQueueClient`, `PahoEventsQueueClient`, or `MoquetteEventsQueue`.

## How document 1 is built

### Event source

`JoystickEventSource` and `JoystickEventConverter` realize this stage. When the step is done, a `JoystickEvent` has been submitted to `fireAsync`, or the reading was dropped inside `JoystickEventSource`. `AllJoysticks` is unchanged and stays inside this stage.

### CDI fireAsync

The same `JoystickEventSource` fire realizes this stage. The type is `JoystickEvent`. The handoff from the listener is direct. When the step is done, the source thread is free to read the next jstest line. `@ObservesAsync` handlers have been queued.

### Persist handler

`JoystickEventCrudService.actOnJoystickEvent` realizes this stage. `MongoEventStore.append` writes when Mongo is present. When Mongo is absent, the call returns the record and does not insert.

### Appliance handler

`SimpleAliasService.handleJoystickEvent` realizes this stage. The method body is the body that already skips an initial event and a non-button, then looks up each `Mapping`.

When the step is done, either the action was called once per mapping that has the key, or the event was not a mapped button and the action was not called.

### Action

`toggleAppliances` in `SimpleAliasService`, reached from the two-argument `handleJoystickEvent`, realizes this stage. The handoff is direct and carries the `Mapping` and the name list at the key `number`, a colon, and `eventValue`. The write still goes through `ConnectorService.send`.
