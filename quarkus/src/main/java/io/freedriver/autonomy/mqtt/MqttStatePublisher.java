package io.freedriver.autonomy.mqtt;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the publish loop when MQTT is enabled. A failed connect stays on the
 * background thread. A password file that is not owner-only makes that thread
 * return. Neither case stops the process.
 */
@ApplicationScoped
public class MqttStatePublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MqttLog.CATEGORY);

    private final AutonomyMqttConfig config;
    private final Validator validator;
    private final ApplianceStateSource states;

    private MqttConnector connector;
    private Thread thread;

    @Inject
    public MqttStatePublisher(AutonomyMqttConfig config, Validator validator, ApplianceStateSource states) {
        this.config = config;
        this.validator = validator;
        this.states = states;
    }

    void onStart(@Observes StartupEvent event) {
        Set<ConstraintViolation<AutonomyMqttConfig>> violations = validator.validate(config);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .collect(Collectors.joining("; "));
            throw new ConstraintViolationException("autonomy.mqtt configuration is invalid: " + detail, violations);
        }
        if (!config.enabled()) {
            LOG.info("MQTT off");
            return;
        }
        MqttSettings settings = new AutonomyMqttSettings(config);
        connector = MqttConnectors.load();
        thread = new Thread(() -> {
            try {
                connector.start(settings, states);
            } catch (Exception e) {
                LOG.warn("MQTT connector stopped causeClass={}", e.getClass().getName());
            }
        }, "mqtt-state");
        thread.setDaemon(true);
        thread.start();
    }

    void onStop(@Observes ShutdownEvent event) {
        if (connector != null) {
            connector.close();
        }
        if (thread != null) {
            thread.interrupt();
        }
    }
}
