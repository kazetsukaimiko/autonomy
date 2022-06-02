package io.freedriver.autonomy.jpa.entity.event;

import io.freedriver.autonomy.jpa.entity.event.StateType;
import javax.annotation.processing.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProcessor", date="2022-06-01T15:59:12", comments="EclipseLink-2.7.6.v20200131-rNA")
@StaticMetamodel(EventDescription.class)
public class EventDescription_ { 

    public static volatile SingularAttribute<EventDescription, String> state;
    public static volatile SingularAttribute<EventDescription, StateType> type;

}