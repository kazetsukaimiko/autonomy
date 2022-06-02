package io.freedriver.autonomy.jpa.entity.event.sensor;

import io.freedriver.autonomy.jpa.entity.event.Event_;
import java.math.BigDecimal;
import javax.annotation.processing.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProcessor", date="2022-06-01T15:59:12", comments="EclipseLink-2.7.6.v20200131-rNA")
@StaticMetamodel(GPSEvent.class)
public class GPSEvent_ extends Event_ {

    public static volatile SingularAttribute<GPSEvent, BigDecimal> latitude;
    public static volatile SingularAttribute<GPSEvent, BigDecimal> longitude;

}