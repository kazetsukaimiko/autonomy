package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.freedriver.base.util.file.DirectoryProviders;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class TTLEnforcementService {
    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Inject @Any
    Instance<EventCrudService<?>> eventCrudServices;

    public void init(@Observes StartupEvent ev) {
        executorService.submit(this::applyTTL);
    }

    public void applyTTL() {
        while (true) {
            try {
                Duration ttl = getTTL();
                eventCrudServices.forEach(eventCrudService -> {
                    applyTTLToEventService(ttl, eventCrudService);
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not apply TTL: ", e);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Could not apply TTL: ", e);
            }
        }
    }

    private void applyTTLToEventService(Duration ttl, EventCrudService<?> eventCrudService) {
        try {
            eventCrudService.applyTTL(ttl);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not apply TTL to "+eventCrudService.getClass().getSimpleName()+": ", e);
        }
    }

    public Mappings getMappings() throws IOException {
        return ObjectMapperContextResolver.getMapper().readValue(
                DirectoryProviders.CONFIG
                        .getProvider()
                        .subdir(Autonomy.DEPLOYMENT)
                        .file("mappings_v2.json")
                        .get()
                        .toFile(),
                Mappings.class);
    }

    public Duration getTTL() {
        try {
            Mappings mappings = getMappings();
            return Duration.of(mappings.getEventTTL(), mappings.getEventTTLUnit());
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Cannot read TTL:", ioe);
            return Duration.ofDays(7);
        }

    }



}
