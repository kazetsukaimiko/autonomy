package io.freedriver.autonomy.async;

import kaze.serial.SBMS0Finder;
import kaze.victron.VEDirectReader;

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
    private static final Set<VEDirectReader> ALL_DEVICES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public synchronized Stream<VEDirectReader> allDevices() {
        try {
            VEDirectReader.allVEDirectDevices(SBMS0Finder::noMatch)
                    .filter(this::veDeviceInactive)
                    .forEach(ALL_DEVICES::add);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Couldn't iterate over VEDirectDevices:", e);
        }
        return ALL_DEVICES.stream();
    }

    private boolean veDeviceInactive(VEDirectReader veDirectDevice) {
        return Optional.of(veDirectDevice)
                .map(device -> !ALL_DEVICES.contains(device))
                .orElse(true);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
