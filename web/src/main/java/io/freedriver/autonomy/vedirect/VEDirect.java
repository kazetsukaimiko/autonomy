package io.freedriver.autonomy.vedirect;

import io.freedriver.autonomy.rest.VEDirectApi;
import kaze.victron.VEDirectMessage;
import kaze.victron.VictronDevice;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequestScoped
public class VEDirect implements VEDirectApi {
    @Inject
    private VEDirectMessageService messageService;

    @Override
    public Set<VictronDevice> getDevices() {
        return messageService.devices();
    }

    @Override
    public List<VEDirectMessage> getDevices(String serial) {
        return bySerial(serial)
                .flatMap(messageService::byDevice)
                .map(VEDirectMessage.class::cast)
                .collect(Collectors.toList());
    }

    @Override
    public List<VEDirectMessage> getDevicesFrom(String serial, Integer number, ChronoUnit chronoUnit) {
        return bySerial(serial)
                .flatMap(device -> messageService.last(device, Duration.of(number, chronoUnit)))
                .collect(Collectors.toList());
    }

    @Override
    public List<BigDecimal> getFieldData(String serial, Integer number, ChronoUnit chronoUnit, String field) {
        return null;
    }

    private Stream<VictronDevice> bySerial(String serial) {
        return getDevices().stream()
                .filter(device -> Objects.equals(serial, device.getSerialNumber()));
    }
}
