package io.freedriver.autonomy.jpa.entity.event.sensor;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.EventCoordinate;
import io.freedriver.autonomy.jpa.entity.event.EventDescription;
import io.freedriver.autonomy.jpa.entity.event.SourceType;
import io.freedriver.autonomy.jpa.entity.event.StateType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Table
@Entity
public class GPSEvent extends Event {
    @Column
    private BigDecimal latitude;

    @Column
    private BigDecimal longitude;

    public GPSEvent() {
    }

    public GPSEvent(Instant timestamp, EventCoordinate coordinate, EventDescription description, BigDecimal latitude, BigDecimal longitude) {
        super(timestamp, coordinate, description, SourceType.AUTOMATIC);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GPSEvent(BigDecimal latitude, BigDecimal longitude) {
        this(
                Instant.now(),
                new EventCoordinate(null, "GPS_LOCATION"),
                new EventDescription(StateType.CHANGE_STATE,
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
