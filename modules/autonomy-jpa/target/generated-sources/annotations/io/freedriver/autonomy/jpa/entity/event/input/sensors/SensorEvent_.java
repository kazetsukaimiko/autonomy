package io.freedriver.autonomy.jpa.entity.event.input.sensors;

import io.freedriver.autonomy.jpa.entity.event.Event_;
import java.util.UUID;
import javax.annotation.processing.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProcessor", date="2022-06-01T15:59:12", comments="EclipseLink-2.7.6.v20200131-rNA")
@StaticMetamodel(SensorEvent.class)
public abstract class SensorEvent_ extends Event_ {

    public static volatile SingularAttribute<SensorEvent, String> sensorName;
    public static volatile SingularAttribute<SensorEvent, UUID> boardId;

}