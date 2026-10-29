package io.freedriver.autonomy.events;

import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Modular producer of one event payload type on the CDI event bus.
 *
 * <p>Each implementation owns one domain (joystick, VEDirect, and later types). The source
 * fires that payload with {@code Event.fireAsync} so handlers run off the reader thread.
 * Persist, appliance control, and metrics are separate {@code @ObservesAsync} handlers of
 * that fire. Events stay in-process. They are not published to MQTT.
 *
 * <p>The running process discovers sources as CDI beans. Register test providers via
 * {@code META-INF/services/} and discover them with {@link #load()}.
 */
public interface EventSource extends AutoCloseable {
    String name();

    void start();

    void stop();

    /**
     * Discovers registered {@link EventSource} providers.
     *
     * <p>Uses this interface's defining class loader, not the current thread context class loader.
     */
    static Stream<EventSource> load() {
        return ServiceLoader.load(EventSource.class, EventSource.class.getClassLoader())
                .stream()
                .map(ServiceLoader.Provider::get);
    }

    @Override
    default void close() {
        stop();
    }
}