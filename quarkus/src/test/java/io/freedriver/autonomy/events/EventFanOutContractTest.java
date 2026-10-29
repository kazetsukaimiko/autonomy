package io.freedriver.autonomy.events;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import io.freedriver.autonomy.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.service.JoystickEventCrudService;
import io.freedriver.autonomy.service.SimpleAliasService;
import io.freedriver.autonomy.vedirect.VEDirectMessageActor;
import io.freedriver.victron.VEDirectMessage;
import jakarta.enterprise.event.ObservesAsync;
import org.junit.jupiter.api.Test;

class EventFanOutContractTest {

    @Test
    void joystickAndVeDirectHandlersObserveAsync() throws Exception {
        assertObservesAsync(JoystickEventCrudService.class, "actOnJoystickEvent", JoystickEvent.class);
        assertObservesAsync(SimpleAliasService.class, "handleJoystickEvent", JoystickEvent.class);
        assertObservesAsync(VEDirectMessageActor.class, "actOnVEDirectMessage", VEDirectMessage.class);
    }

    private static void assertObservesAsync(Class<?> type, String methodName, Class<?> payload) throws Exception {
        Method method = type.getMethod(methodName, payload);
        Parameter parameter = method.getParameters()[0];
        assertNotNull(
                parameter.getAnnotation(ObservesAsync.class),
                type.getSimpleName() + "." + methodName + " must observe fireAsync");
    }
}
