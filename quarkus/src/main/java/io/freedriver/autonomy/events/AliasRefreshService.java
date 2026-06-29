package io.freedriver.autonomy.events;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.freedriver.autonomy.service.SimpleAliasService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class AliasRefreshService {

    @Inject
    SimpleAliasService simpleAliasService;

    private ExecutorService executor;

    void onStart(@Observes StartupEvent startupEvent) {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "alias-refresh");
            thread.setDaemon(true);
            return thread;
        });
        executor.submit(() -> {
            try {
                simpleAliasService.refreshAnalogPins();
            } catch (Exception e) {
                log.warn("Failed initial alias refresh", e);
            }
        });
    }

    void onStop(@Observes ShutdownEvent shutdownEvent) {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}