package io.freedriver.autonomy.jpa.entity.event.speech;

import io.freedriver.autonomy.jpa.entity.event.Event_;
import io.freedriver.autonomy.jpa.entity.event.speech.SpeechEventType;
import javax.annotation.processing.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProcessor", date="2022-06-01T15:59:12", comments="EclipseLink-2.7.6.v20200131-rNA")
@StaticMetamodel(SpeechEvent.class)
public class SpeechEvent_ extends Event_ {

    public static volatile SingularAttribute<SpeechEvent, String> subject;
    public static volatile SingularAttribute<SpeechEvent, SpeechEventType> speechEventType;
    public static volatile SingularAttribute<SpeechEvent, String> text;

}