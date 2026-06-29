package io.freedriver.autonomy.events;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class EventSourceRegistry {

    @Inject
    Instance<EventSource> eventSources;

    void onStart(@Observes StartupEvent startupEvent) {
        eventSources.forEach(source -> {
            try {
                log.info("Starting event source: {}", source.name());
                source.start();
            } catch (Exception e) {
                log.error("Failed to start event source: {}", source.name(), e);
            }
        });
    }

    void onStop(@Observes ShutdownEvent shutdownEvent) {
        eventSources.forEach(source -> {
            try {
                log.info("Stopping event source: {}", source.name());
                source.stop();
            } catch (Exception e) {
                log.warn("Failed to stop event source: {}", source.name(), e);
            }
        });
    }
}