package io.freedriver.autonomy.hrorm;


import io.freedriver.autonomy.entity.JoystickEvent;
import org.hrorm.Converter;

import java.util.Optional;

public class JoystickTypeConverter implements Converter<JoystickEvent.Type, String> {
    @Override
    public String from(JoystickEvent.Type type) {
        return Optional.ofNullable(type)
                .map(JoystickEvent.Type::name)
                .orElse(null);
    }

    @Override
    public JoystickEvent.Type to(String s) {
        return Optional.ofNullable(s)
                .map(JoystickEvent.Type::byName)
                .orElse(null);
    }
}
