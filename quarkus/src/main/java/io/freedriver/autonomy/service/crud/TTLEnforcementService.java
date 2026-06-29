package io.freedriver.autonomy.service.crud;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.freedriver.base.util.file.DirectoryProviders;
import io.freedriver.base.util.tedious.Loops;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * This service enforces TTLs on all Event data.
 * TODO: Ask each Event service what their oldest data is like and plan TTL updates instead of polling
 */
@ApplicationScoped
@Slf4j
public class TTLEnforcementService {
    private static final Duration POLL_DURATION = Duration.ofSeconds(1);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Inject @Any
    Instance<EventCrudService<?>> eventCrudServices;

    private boolean continueTTLEnforcement = true;

    /**
     * Start the loop.
     */
    public void init(@Observes StartupEvent ev) {
        continueTTLEnforcement = true;
        executorService.submit(this::applyTTL);
    }

    /**
     * Stop the loop.
     */
    public void destroy(@Observes ShutdownEvent ev) {
        continueTTLEnforcement = false;
    }

    /**
     * Endless-Loop through all eventCrudServices and apply TTL to them.
     */
    public void applyTTL() {
        Loops.takeUntil(() -> {
            Duration ttl = getTTL();
            eventCrudServices.forEach(eventCrudService -> {
                applyTTLToEventService(ttl, eventCrudService);
            });
            return continueTTLEnforcement;
        }, POLL_DURATION, this::logTTLException);
        log.info("Shut down TTL Enforcement.");
    }

    private boolean logTTLException(Throwable throwable) {
        log.warn("Couldn't enforce TTL: ", throwable);
        return true;
    }

    /**
     * Apply the TTL enforcement to the given eventCrudService.
     * @param ttl
     * @param eventCrudService
     */
    private void applyTTLToEventService(Duration ttl, EventCrudService<?> eventCrudService) {
        try {
            int culled = eventCrudService.applyTTL(ttl);
            log.info("Culled " + culled + " from " + eventCrudService.getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("Could not apply TTL to "+eventCrudService.getClass().getSimpleName()+": ", e);
        }
    }

    /**
     * Get the mappings file config.
     * @return
     * @throws IOException
     */
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

    /**
     * Get the TTL from the Mappings file.
     * @return
     */
    public Duration getTTL() {
        try {
            Mappings mappings = getMappings();
            return Duration.of(mappings.getEventTTL(), mappings.getEventTTLUnit());
        } catch (IOException ioe) {
            log.warn("Cannot read TTL:", ioe);
            return Duration.ofDays(7);
        }

    }


}
