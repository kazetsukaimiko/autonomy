package io.freedriver.autonomy.mqtt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Checks {@link AutonomyMqttConfig} when MQTT is enabled.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MqttConfigValidator.class)
public @interface MqttConfigValid {

    String message() default "autonomy.mqtt configuration is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
