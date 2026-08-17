package io.freedriver.autonomy.jpa.entity.event.sensor;

import java.math.BigDecimal;
import java.time.Instant;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Table
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class GPSEvent extends Event {
    private static final String GPS_SOURCE = "GPS_LOCATION";

    @Column
    private BigDecimal latitude;

    @Column
    private BigDecimal longitude;

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
}
