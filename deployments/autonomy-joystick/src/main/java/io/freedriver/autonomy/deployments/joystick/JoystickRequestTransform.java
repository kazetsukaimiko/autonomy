package io.freedriver.autonomy.deployments.joystick;

import io.freedriver.autonomy.jpa.entity.event.input.joystick.jstest.JSTestEvent;
import io.freedriver.jsonlink.jackson.schema.v1.Request;

import java.util.UUID;
import java.util.function.BiFunction;

public interface JoystickRequestTransform extends BiFunction<JSTestEvent, UUID, Request> {
}
