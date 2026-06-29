package io.freedriver.autonomy.async;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import io.freedriver.electrodacus.sbms.SBMS0Finder;
import io.freedriver.victron.VEDirectReader;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class VEDirectDeviceService extends BaseService {
    private static final Set<VEDirectReader> ALL_DEVICES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public synchronized Stream<VEDirectReader> allDevices() {
        try {
            VEDirectReader.allVEDirectDevices(SBMS0Finder::noMatch)
                    .filter(this::veDeviceInactive)
                    .forEach(ALL_DEVICES::add);
        } catch (IOException e) {
            log.error("Couldn't iterate over VEDirectDevices:", e);
        }
        return ALL_DEVICES.stream();
    }

    private boolean veDeviceInactive(VEDirectReader veDirectDevice) {
        return Optional.of(veDirectDevice)
                .map(device -> !ALL_DEVICES.contains(device))
                .orElse(true);
    }

}
