package io.freedriver.autonomy.util;

import java.time.Duration;

public interface Delayable {
    /**
     *
     * @param waitTime
     */
    static void wait(Duration waitTime) {
        try {
            Thread.sleep(waitTime.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted: ", e);
        }
    }
}
