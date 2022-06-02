package io.freedriver.autonomy.jpa.entity.event.input.joystick;

import io.freedriver.autonomy.jpa.entity.event.Event_;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEventType;
import javax.annotation.processing.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProcessor", date="2022-06-01T15:59:12", comments="EclipseLink-2.7.6.v20200131-rNA")
@StaticMetamodel(JoystickEvent.class)
public class JoystickEvent_ extends Event_ {

    public static volatile SingularAttribute<JoystickEvent, Long> number;
    public static volatile SingularAttribute<JoystickEvent, JoystickEventType> joystickEventType;
    public static volatile SingularAttribute<JoystickEvent, Boolean> initial;
    public static volatile SingularAttribute<JoystickEvent, Long> value;

}