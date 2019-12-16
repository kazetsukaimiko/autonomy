package io.freedriver.autonomy.entity.event.sensor;

import io.freedriver.autonomy.entity.event.Event;
import io.freedriver.autonomy.entity.event.EventCoordinate;
import io.freedriver.autonomy.entity.event.EventDescription;
import io.freedriver.autonomy.entity.event.EventType;
import org.dizitart.no2.objects.InheritIndices;

import java.math.BigDecimal;
import java.time.Instant;

@InheritIndices
public class GPSEvent extends Event {
    private BigDecimal latitude;
    private BigDecimal longitude;

    public GPSEvent() {
    }

    public GPSEvent(Instant timestamp, EventCoordinate coordinate, EventDescription description, BigDecimal latitude, BigDecimal longitude) {
        super(timestamp, coordinate, description);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GPSEvent(BigDecimal latitude, BigDecimal longitude) {
        this(
                Instant.now(),
                new EventCoordinate(null, "GPS_LOCATION"),
                new EventDescription(EventType.CHANGE_STATE,
                        "lat:" + String.valueOf(latitude) +
                                ";lon:" + String.valueOf(longitude)
                        ),
                    latitude,longitude
        );
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
}
