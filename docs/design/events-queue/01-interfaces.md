# Interfaces, contracts, and code flow

One design. A joystick mapping today goes from the joystick listener, to a CDI event, to a CDI recipient, to the action. The listener and the recipient stay. A queue sits between them. The broker for that queue is Moquette, which this repository already has. The house broker and its appliance and command topics are not this queue.

## Stages

### 1. Joystick listener

Owns the device read and the conversion of one reading into one `JoystickEvent`. Does not own mappings, the queue, or appliance writes.

### 2. CDI event

Owns in-process delivery of the listener's `JoystickEvent` to observers of that fire. Does not own the recipient that runs the action. Observers of this fire other than MQTT publish are outside this flow and are not routed.

### 3. MQTT publish

Owns one `QueuedEventMessage` on the generic events queue for each listener fire. Does not own the action, and does not observe a fire it did not publish from.

### 4. MQTT receive

Owns taking a `QueuedEventMessage` off that same queue. Does not choose a recipient and does not read mappings.

### 5. Router

Owns accepting or dropping that message, and firing the CDI event the recipient observes. Does not read mappings and does not write a board.

### 6. CDI recipient

Owns the decision that this `JoystickEvent` is a mapped button, and the `controlMap` lookup. This is the existing recipient.

### 7. Action

Owns toggling the appliances named by that `controlMap` entry on that mapping's board.

## Types

### JoystickEvent

Existing type `io.freedriver.autonomy.event.input.joystick.JoystickEvent`. It crosses from the listener to the CDI event. It is the payload of `QueuedEventMessage` when `type` is `joystick.event`. It crosses from the router to the recipient.

Fields:

- `id`, string. The router delivers only a non-blank `id`. The listener assigns it. The queue does not replace it.
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

### QueuedEventMessage

New type. It crosses from MQTT publish to MQTT receive, and from MQTT receive to the router.

Fields:

- `type`, string. This flow sends `joystick.event`, the existing `EventTypes.JOYSTICK_EVENT` value. This flow sends no other type.
- `payload`, a `JoystickEvent`, when `type` is `joystick.event`. The payload fields are the `JoystickEvent` fields above, unchanged.

No methods are part of this contract.

### EventsQueue

New qualifier. It has no fields. It is present only on the `JoystickEvent` the router fires. It is absent on the listener's fire.

### Mapping

Existing type `io.freedriver.jsonlink.config.v2.Mapping`. It crosses from the recipient to the action, together with the `controlMap` value.

Fields this handoff uses:

- `connectorId`, the board id.
- `appliances`, each appliance's `identifier` and `name`. `groups` is not part of this handoff.
- `controlMap`, a map from a key to a list of appliance names. The key is `number`, then a colon, then `eventValue`. The list is the names to toggle.

`analogSensors` and `analogAlerts` are not part of this handoff.

## Messages

Listener to CDI event. Nothing is sent on a broker. The handoff is direct. The type is `JoystickEvent`.

CDI event to MQTT publish. Nothing is sent on a broker. The handoff is direct. The type is `JoystickEvent`. Publish observes the listener's fire. That fire is not qualified with `EventsQueue`.

MQTT publish to MQTT receive. The message name is `QueuedEventMessage`. Direction is publish to receive, one way. The payload type is `QueuedEventMessage`. The sender is MQTT publish. The receiver is MQTT receive. The broker is Moquette. The topic is the single exact topic `events`. Delivery is QoS 1. Retain is false. This is not `freedriver/v1/{instanceId}/appliances` and not `freedriver/v1/{instanceId}/commands`. It is not a topic on the house broker, and the topic has no instance segment.

MQTT receive to the router. Nothing further is sent. The handoff is direct. The type is `QueuedEventMessage`.

Router to the CDI recipient. Nothing is sent on a broker. The handoff is direct. The type is `JoystickEvent`, qualified with `EventsQueue`.

CDI recipient to the action. Nothing is sent on a broker. The handoff is direct. The types are `Mapping` and the list of appliance names from `controlMap`.

## Handoffs

### Listener to CDI event

What moves is one `JoystickEvent`. The carrier is a direct fire of that type.

The handoff advances when the listener has a reading whose subject title is present, and conversion produces a `JoystickEvent`. It does not advance when the title is absent, or when the fire fails. Those cases stay in the listener.

### CDI event to MQTT publish

What moves is that same `JoystickEvent`. The carrier is direct observation of the listener's fire.

The handoff advances for every `JoystickEvent` on that fire, including an initial event and an `AXIS` event. It does not advance for an `EventsQueue` fire. Publish does not observe those.

### MQTT publish to MQTT receive

What moves is one `QueuedEventMessage` whose `type` is `joystick.event` and whose `payload` is that `JoystickEvent`. The carrier is the `events` topic on Moquette.

The handoff advances when Moquette has accepted the message and publish has returned to the listener's fire. Receive starts after that return, and not on the listener's stack. It does not advance when acceptance fails. A failed publish is not retried on that fire, and publish does not call the recipient.

### MQTT receive to the router

What moves is that `QueuedEventMessage`. The carrier is a direct handoff.

The handoff advances when the message arrived on the topic `events`. It does not advance for a message on any other topic.

### Router to the CDI recipient

What moves is the payload `JoystickEvent`. The carrier is a direct CDI fire qualified with `EventsQueue`.

The handoff advances when `type` is `joystick.event`, the payload is a `JoystickEvent`, `id` is non-blank, and this process has not already handed that `id` to the recipient. It does not advance for any other `type`, for a payload that is not a `JoystickEvent`, for a blank `id`, or for an `id` already handed off. A second delivery of the same `id` does not advance.

That `EventsQueue` fire is not published again. Storing the listener's event is not this handoff.

### Recipient to the action

What moves is the `Mapping` that holds the key, and the list of appliance names at that key. The carrier is a direct call. The key is `number`, a colon, and `eventValue`.

The handoff advances when `initial` is false, `joystickEventType` is a button, and that key is present in the mapping's `controlMap`. One event advances once for each mapping that has the key. It does not advance when `initial` is true, when `joystickEventType` is absent, when `joystickEventType` is `AXIS`, or when the key is absent.

The step is done when the action has returned for each matching mapping. A failed board write stays inside the recipient. It does not put the recipient back on the listener's fire.

When the action is done, each named appliance on that board has been written to the opposite of the digital state the action read.
