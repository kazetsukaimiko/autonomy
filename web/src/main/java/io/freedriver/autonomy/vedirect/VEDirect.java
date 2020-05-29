package io.freedriver.autonomy.vedirect;

import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import io.freedriver.autonomy.rest.VEDirectApi;
import kaze.victron.VEDirectColumn;
import kaze.victron.VictronDevice;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
    public Map<String, Integer> getColumnData(String serial, Integer number, ChronoUnit chronoUnit, VEDirectColumn column) {
        return getDevicesFrom(serial, number, chronoUnit)
                .stream()
                .map(VEDirectMessage::toNative)
                .collect(Collectors.toMap(
                        message -> keyOf(message, column),
                        message -> 1,
                        Integer::sum
                ));
    }

    public String keyOf(kaze.victron.VEDirectMessage message, VEDirectColumn column) {
        Object o = column.getDefinition().getter().apply(message);
        return (o != null) ? String.valueOf(o) : "NULL";
    }

    private Stream<VictronDevice> bySerial(String serial) {
        return getDevices().stream()
                .filter(device -> Objects.equals(serial, device.getSerialNumber()));
    }
}
