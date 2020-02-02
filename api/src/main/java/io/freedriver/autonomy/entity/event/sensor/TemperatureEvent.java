package io.freedriver.autonomy.entity.event.sensor;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.freedriver.autonomy.entity.event.Event;
import io.freedriver.autonomy.entity.event.EventCoordinate;
import io.freedriver.autonomy.entity.event.EventDescription;
import io.freedriver.autonomy.entity.event.SourceType;
import io.freedriver.autonomy.entity.event.StateType;
import org.dizitart.no2.objects.InheritIndices;

import java.time.Instant;

@InheritIndices
public class TemperatureEvent extends Event {
    @JsonUnwrapped
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
