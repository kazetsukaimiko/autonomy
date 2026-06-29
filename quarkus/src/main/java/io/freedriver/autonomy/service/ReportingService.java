package io.freedriver.autonomy.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ReportingService {
    private final Map<String, Instant> lastReportedMap = new ConcurrentHashMap<>();

    public synchronized <E> void update(String key, Runnable runnable, Duration every) {
        if (key != null &&
                (!lastReportedMap.containsKey(key))
                        || Instant.now().isAfter(lastReportedMap.get(key).plus(every))) {
            try {
                lastReportedMap.put(key, Instant.now());
                runnable.run();
            } catch (Exception e) {
                log.warn("Error reporting " + key, e);
            }
        }
    }
}
