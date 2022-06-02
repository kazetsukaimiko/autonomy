package io.freedriver.autonomy.deployments.joystick.service;

import io.freedriver.autonomy.deployments.joystick.JoystickRequestTransform;
import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.jsonlink.jackson.schema.v1.Request;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class JoystickRequestTransformer implements JoystickRequestTransform {
    @Override
    public Request apply(JSTestEvent jsTestEvent, UUID connectorId) {
        return null;
    }
}
