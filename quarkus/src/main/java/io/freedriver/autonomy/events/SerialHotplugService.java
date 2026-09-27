package io.freedriver.autonomy.events;

import io.freedriver.autonomy.service.ConnectorService;
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
 * for {@code /dev/serial/by-id}. A board that comes back on a new tty node is opened here.
 * That open waits for the board to finish reset and complete its UUID handshake, then
 * restores saved appliance state.
 */
@ApplicationScoped
@Slf4j
public class SerialHotplugService {

    @Inject
    SerialConnectionManager connectionManager;

    @Inject
    ConnectorService connectorService;

    void onFilesystemEvent(@Observes InotifyFilesystemEvent event) {
        log.debug("Serial hotplug signal: {}", event);
        if (connectionManager instanceof DefaultSerialConnectionManager manager) {
            manager.refreshConnections();
        }
        connectorService.refreshConnectedBoards();
    }
}