package io.freedriver.autonomy.service;

import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ReportingService {
    private static final Logger LOGGER = Logger.getLogger(ReportingService.class.getName());
    private final Map<String, Instant> lastReportedMap = new ConcurrentHashMap<>();

    public synchronized <E> void update(String key, Runnable runnable, Duration every) {
        if (key != null &&
                (!lastReportedMap.containsKey(key))
                        || Instant.now().isAfter(lastReportedMap.get(key).plus(every))) {
            try {
                lastReportedMap.put(key, Instant.now());
                runnable.run();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error reporting " + key, e);
            }
        }
    }
}
