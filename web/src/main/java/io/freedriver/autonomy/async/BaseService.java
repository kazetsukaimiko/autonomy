package io.freedriver.autonomy.async;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseService {
    protected abstract Logger getLogger();
    protected void wait(Duration duration) {
        getLogger().info("Waiting " + duration.toMillis() +"ms");
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            getLogger().log(Level.SEVERE, "Failed wait: ", e);
        }
    }
}
