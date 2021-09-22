package io.freedriver.autonomy.async;

import io.freedriver.electrodacus.sbms.SBMS0Finder;
import io.freedriver.victron.VEDirectStreamer;

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
    private static final Set<VEDirectStreamer> ALL_DEVICES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public synchronized Stream<VEDirectStreamer> allDevices() {
        try {
            VEDirectStreamer.allMessageStreamers(SBMS0Finder::noMatch)
                    .filter(this::veDeviceInactive)
                    .forEach(ALL_DEVICES::add);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Couldn't iterate over VEDirectDevices:", e);
        }
        return ALL_DEVICES.stream();
    }

    private boolean veDeviceInactive(VEDirectStreamer veDirectDevice) {
        return Optional.of(veDirectDevice)
                .map(device -> !ALL_DEVICES.contains(device))
                .orElse(true);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
