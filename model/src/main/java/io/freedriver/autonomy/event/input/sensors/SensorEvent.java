package io.freedriver.autonomy.event.input.sensors;

import java.util.UUID;

import io.freedriver.autonomy.event.Event;

public interface SensorEvent extends Event {
    UUID boardId();

    String sensorName();
}
