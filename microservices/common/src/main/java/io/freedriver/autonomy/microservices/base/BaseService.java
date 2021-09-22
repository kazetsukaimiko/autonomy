package io.freedriver.autonomy.microservices.base;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO : autonomy-base
public abstract class BaseService {
    protected abstract Logger getLogger();

    // Goal: Support at least the number of VEDirectReaders.
    protected final ExecutorService pool = Executors.newCachedThreadPool();

    protected void wait(Duration duration) {
        getLogger().info("Waiting " + duration.toMillis() +"ms");
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            getLogger().log(Level.SEVERE, "Failed wait: ", e);
        }
    }
}
