package io.freedriver.autonomy.events;

import io.freedriver.inotify.cdi.InotifyFilesystemEvent;
import io.freedriver.serial.api.connection.SerialConnectionManager;
import io.freedriver.serial.connection.DefaultSerialConnectionManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Reacts to serial device hotplug signals from freedriver {@code inotify-cdi}.
 *
 * <p>{@link io.freedriver.inotify.cdi.InotifyCdiBridge} publishes {@link InotifyFilesystemEvent}
 * when {@link io.freedriver.inotify.cdi.InotifyLifecycle} receives kernel inotify notifications
 * for {@code /dev/serial/by-id}.
 */
@ApplicationScoped
@Slf4j
public class SerialHotplugService {

    @Inject
    SerialConnectionManager connectionManager;

    void onFilesystemEvent(@Observes InotifyFilesystemEvent event) {
        log.debug("Serial hotplug signal: {}", event);
        if (connectionManager instanceof DefaultSerialConnectionManager manager) {
            manager.refreshConnections();
        }
    }
}