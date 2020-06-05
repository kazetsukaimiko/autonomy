package io.freedriver.autonomy.jpa.entity.event.sensor;

import io.freedriver.autonomy.jpa.entity.event.*;

import java.time.Instant;

public class TemperatureEvent extends Event {

    private TemperatureValue temperature;

    public TemperatureEvent() {
    }

    public TemperatureEvent(long timestamp, EventCoordinate coordinate, EventDescription description, TemperatureValue temperature) {
        super(timestamp, coordinate, description, SourceType.NON_HUMAN);
        this.temperature = temperature;
    }

    public TemperatureEvent(TemperatureValue temperature) {
        this(
                Instant.now().toEpochMilli(),
                new EventCoordinate(null, "TEMPERATURE"),
                new EventDescription(StateType.CHANGE_STATE,
                        String.valueOf(temperature)
                ),
                temperature
        );
    }

    public TemperatureValue getTemperature() {
        return temperature;
    }

    public void setTemperature(TemperatureValue temperature) {
        this.temperature = temperature;
    }
}
