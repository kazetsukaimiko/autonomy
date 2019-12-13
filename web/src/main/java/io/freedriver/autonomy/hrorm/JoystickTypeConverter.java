package io.freedriver.autonomy.hrorm;

import io.freedriver.autonomy.entity.event.input.joystick.JoystickEventType;
import org.hrorm.Converter;

import java.util.Optional;

public class JoystickTypeConverter implements Converter<JoystickEventType, String> {
    @Override
    public String from(JoystickEventType type) {
        return Optional.ofNullable(type)
                .map(JoystickEventType::name)
                .orElse(null);
    }

    @Override
    public JoystickEventType to(String s) {
        return Optional.ofNullable(s)
                .map(JoystickEventType::byName)
                .orElse(null);
    }
}
