package io.freedriver.autonomy.mqtt;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;

/**
 * Records {@link MqttLog#CATEGORY} lines for tests. Installed before
 * {@link MqttStatePublisher} so the startup ERROR is kept.
 */
@ApplicationScoped
public class MqttStartupLogCapture {

    static final List<ExtLogRecord> records = new CopyOnWriteArrayList<>();

    private static final Handler HANDLER = new Handler() {
        @Override
        public void publish(LogRecord record) {
            records.add(ExtLogRecord.wrap(record));
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    void install(@Observes @Priority(1) StartupEvent event) {
        records.clear();
        Logger logger = Logger.getLogger(MqttLog.CATEGORY);
        logger.setLevel(Level.INFO);
        logger.removeHandler(HANDLER);
        logger.addHandler(HANDLER);
    }
}
