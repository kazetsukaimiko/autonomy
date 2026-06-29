package io.freedriver.autonomy.events;

import io.freedriver.inotify.cdi.InotifyFilesystemEvent;
import io.freedriver.serial.api.connection.SerialConnectionManager;
import io.freedriver.serial.connection.DefaultSerialConnectionManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

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