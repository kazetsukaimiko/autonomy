# Interfaces, contracts, and code flow

One design. A joystick mapping goes from the joystick listener, to a CDI event, to many asynchronous handlers. The listener and the appliance recipient stay. Events stay in-process on the CDI bus. They are not published to MQTT.

The house broker and its appliance and command topics are unchanged. An MQTT events queue (QueuedEventMessage, Moquette `events` topic, EventsQueue qualifier) is withdrawn: the house broker is remote, and a local MQTT queue is not justified over the CDI bus.

## Stages

### 1. Event source (SPI)

Owns the device read and the conversion of one reading into one typed payload. Each `EventSource` owns one domain. Joystick is one implementation. VEDirect is another. Does not own mappings, persist, or appliance writes.

### 2. CDI fireAsync

Owns in-process delivery of that payload. The source returns before handlers run. The type is the payload itself (`JoystickEvent`, `VEDirectMessage`). There is no envelope and no broker.

### 3. Handlers (async fan-out)

Each handler observes with `@ObservesAsync` and runs off the source thread. Handlers of one fire do not wait on each other.

- Persist: `EventStore.append`. `MongoEventStore` is the store. When Mongo is absent, append is a no-op and does not wait on server selection.
- Joystick appliance control: `SimpleAliasService` looks up `controlMap` and toggles appliances.
- Metrics emission is a later handler. It is not part of this change.

### 4. Action

Owns toggling the appliances named by a `controlMap` entry on that mapping's board. Unchanged.

## Types

### EventSource

Existing SPI `io.freedriver.autonomy.events.EventSource`. Modular event types. Each implementation fires one payload type with `Event.fireAsync`. CDI discovers running sources. `EventSource.load()` discovers test providers.

### JoystickEvent

Existing type `io.freedriver.autonomy.event.input.joystick.JoystickEvent`. It crosses from the listener to every joystick handler.

Fields:

- `id`, string. The listener assigns it.
- `timestamp`, instant.
- `generationOrigin`, `HUMAN` or `NON_HUMAN`. The listener sets `NON_HUMAN`.
- `sourceClass`, string.
- `sourceId`, string.
- `eventId`, string.
- `number`, long. Button or axis index.
- `eventValue`, long. On a button, `0` is release and any other value is press.
- `initial`, boolean.
- `joystickEventType`, one of `BUTTON_DOWN`, `BUTTON_UP`, or `AXIS`. `BUTTON_DOWN` and `BUTTON_UP` are buttons. `AXIS` is not. The listener sets `BUTTON_UP` when a button's `eventValue` is `0`, and `BUTTON_DOWN` otherwise.

No methods are part of this contract.

### EventStore

Existing SPI `io.freedriver.autonomy.events.store.EventStore`. Persist is one handler. `MongoEventStore` implements it. Absent Mongo does not fail the fire.

### Mapping

Existing type `io.freedriver.jsonlink.config.v2.Mapping`. It crosses from the joystick appliance handler to the action, together with the `controlMap` value.

Fields this handoff uses:

- `connectorId`, the board id.
- `appliances`, each appliance's `identifier` and `name`. `groups` is not part of this handoff.
- `controlMap`, a map from a key to a list of appliance names. The key is `number`, then a colon, then `eventValue`. The list is the names to toggle.

`analogSensors` and `analogAlerts` are not part of this handoff.

## Messages

Listener to CDI event. Nothing is sent on a broker. The handoff is `Event.fireAsync` of `JoystickEvent`.

CDI event to persist. Nothing is sent on a broker. The handoff is `@ObservesAsync JoystickEvent`. Persist calls `EventStore.append` with `EventTypes.JOYSTICK_EVENT`.

CDI event to appliance control. Nothing is sent on a broker. The handoff is `@ObservesAsync JoystickEvent` on `SimpleAliasService`.

CDI recipient to the action. Nothing is sent on a broker. The types are `Mapping` and the list of appliance names from `controlMap`.

## Handoffs

### Listener to CDI event

What moves is one `JoystickEvent`. The carrier is `fireAsync` of that type.

The handoff advances when the listener has a reading whose subject title is present, and conversion produces a `JoystickEvent`. It does not advance when the title is absent. A failed handler is logged on the completion stage and does not return to the listener's stack.

### CDI event to persist

What moves is that same `JoystickEvent`. The carrier is asynchronous observation of the listener's fire.

The handoff advances for every `JoystickEvent` on that fire, including an initial event and an `AXIS` event. When Mongo is absent, `append` returns the record and writes nothing. Persist does not call the appliance recipient.

### CDI event to appliance control

What moves is that same `JoystickEvent`. The carrier is a second asynchronous observation of the same fire.

The handoff advances when `initial` is false, `joystickEventType` is a button, and the key `number`, a colon, and `eventValue` is present in a mapping's `controlMap`. One event advances once for each mapping that has the key. It does not advance when `initial` is true, when `joystickEventType` is absent, when `joystickEventType` is `AXIS`, or when the key is absent.

The step is done when the action has returned for each matching mapping. A failed board write stays inside the recipient. It does not put the recipient back on the listener's fire.

### Recipient to the action

What moves is the `Mapping` that holds the key, and the list of appliance names at that key. The carrier is a direct call.

When the action is done, each named appliance on that board has been written to the opposite of the digital state the action read.
