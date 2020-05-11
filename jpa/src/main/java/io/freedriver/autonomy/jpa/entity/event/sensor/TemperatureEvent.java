package io.freedriver.autonomy.jpa.entity.event.sensor;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.EventCoordinate;
import io.freedriver.autonomy.jpa.entity.event.EventDescription;
import io.freedriver.autonomy.jpa.entity.event.SourceType;
import io.freedriver.autonomy.jpa.entity.event.StateType;

import java.time.Instant;

public class TemperatureEvent extends Event {

    private TemperatureValue temperature;

    public TemperatureEvent() {
    }

    public TemperatureEvent(Instant timestamp, EventCoordinate coordinate, EventDescription description, TemperatureValue temperature) {
        super(timestamp, coordinate, description, SourceType.AUTOMATIC);
        this.temperature = temperature;
    }

    public TemperatureEvent(TemperatureValue temperature) {
        this(
                Instant.now(),
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
