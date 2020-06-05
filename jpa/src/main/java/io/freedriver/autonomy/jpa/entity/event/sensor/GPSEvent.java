package io.freedriver.autonomy.jpa.entity.event.sensor;

import io.freedriver.autonomy.jpa.entity.event.*;

import javax.persistence.Column;
import java.math.BigDecimal;
import java.time.Instant;

//@Table
//@Entity
public class GPSEvent extends Event {
    @Column
    private BigDecimal latitude;

    @Column
    private BigDecimal longitude;

    public GPSEvent() {
    }

    public GPSEvent(long timestamp, EventCoordinate coordinate, EventDescription description, BigDecimal latitude, BigDecimal longitude) {
        super(timestamp, coordinate, description, SourceType.NON_HUMAN);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GPSEvent(BigDecimal latitude, BigDecimal longitude) {
        this(
                Instant.now().toEpochMilli(),
                new EventCoordinate(null, "GPS_LOCATION"),
                new EventDescription(StateType.CHANGE_STATE,
                        "lat:" + latitude +
                                ";lon:" + longitude
                ),
                latitude, longitude
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
