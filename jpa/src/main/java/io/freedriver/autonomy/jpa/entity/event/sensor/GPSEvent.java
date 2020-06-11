package io.freedriver.autonomy.jpa.entity.event.sensor;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Table
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class GPSEvent extends Event {
    private static final String GPS_SOURCE = "GPS_LOCATION";

    @Column
    private BigDecimal latitude;

    @Column
    private BigDecimal longitude;

    public GPSEvent() {
    }


    public GPSEvent(long timestamp, GenerationOrigin generationOrigin, String sourceClass, String sourceId, String eventId, BigDecimal latitude, BigDecimal longitude) {
        super(timestamp, generationOrigin, sourceClass, sourceId, eventId);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GPSEvent(BigDecimal latitude, BigDecimal longitude) {
        this(
                Instant.now().toEpochMilli(),
                GenerationOrigin.NON_HUMAN,
                GPSEvent.class.getSimpleName(),
                GPS_SOURCE,
                null,
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
