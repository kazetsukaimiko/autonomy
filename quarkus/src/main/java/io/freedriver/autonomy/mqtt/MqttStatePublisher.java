package io.freedriver.autonomy.mqtt;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the publish loop when MQTT is enabled. Invalid settings and a password
 * file that is not owner-only log one error and leave MQTT off. A failed connect
 * stays on the background thread. The process keeps running in each case.
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
            LOG.error("MQTT off; invalid configuration keys={}", invalidKeys(violations));
            return;
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

    static String invalidKeys(Set<ConstraintViolation<AutonomyMqttConfig>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .filter(key -> !key.isBlank())
                .distinct()
                .sorted()
                .map(key -> "autonomy.mqtt." + key)
                .collect(Collectors.joining(","));
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
