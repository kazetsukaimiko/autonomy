package io.freedriver.autonomy.async;

import kaze.victron.VEDirectDevice;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@ApplicationScoped
public class VEDirectDeviceService extends BaseService {
    private static final Logger LOGGER = Logger.getLogger(VEDirectDeviceService.class.getName());
    private static final Set<VEDirectDevice> ALL_DEVICES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public synchronized Stream<VEDirectDevice> allDevices() {
        try {
            VEDirectDevice.allVEDirectDevices()
                    .filter(this::veDeviceInactive)
                    .forEach(ALL_DEVICES::add);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Couldn't iterate over VEDirectDevices:", e);
        }
        return ALL_DEVICES.stream();
    }

    private boolean veDeviceInactive(VEDirectDevice veDirectDevice) {
        return Optional.of(veDirectDevice)
                .map(device -> !ALL_DEVICES.contains(device))
                .orElse(true);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
