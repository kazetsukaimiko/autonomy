package io.freedriver.autonomy.jpa.entity.event;

import io.freedriver.autonomy.jpa.entity.event.GenerationOrigin;
import javax.annotation.processing.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProcessor", date="2022-06-01T15:59:12", comments="EclipseLink-2.7.6.v20200131-rNA")
@StaticMetamodel(EventCoordinate.class)
public class EventCoordinate_ { 

    public static volatile SingularAttribute<EventCoordinate, String> sourceId;
    public static volatile SingularAttribute<EventCoordinate, String> sourceClass;
    public static volatile SingularAttribute<EventCoordinate, GenerationOrigin> generationOrigin;

}