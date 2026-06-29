package io.freedriver.autonomy.async;

import java.time.Duration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseService {
    protected void wait(Duration duration) {
        log.info("Waiting {}ms", duration.toMillis());
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            log.error("Failed wait: ", e);
        }
    }
}