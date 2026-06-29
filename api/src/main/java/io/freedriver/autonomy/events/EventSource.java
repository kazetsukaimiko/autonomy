package io.freedriver.autonomy.events;

import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Modular producer of CDI events. Each implementation owns one domain (VEDirect, joystick, etc.).
 *
 * <p>Register providers via {@code META-INF/services/} and discover them with {@link #load()}.
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