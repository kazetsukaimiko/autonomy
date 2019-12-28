package io.freedriver.autonomy.jsf.model;

import io.freedriver.autonomy.async.VEDirectDeviceService;
import io.freedriver.autonomy.vedirect.VEDirectMessageService;
import kaze.victron.VEDirectDevice;
import kaze.victron.VEDirectMessage;
import kaze.victron.VictronProduct;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.SECONDS;

@Named
@RequestScoped
public class VictronView {
    @Inject
    private VEDirectDeviceService deviceService;

    @Inject
    private VEDirectMessageService messageService;

    public List<VictronProduct> getProducts() {
        return deviceService.allDevices()
                .map(VEDirectDevice::attemptToGetProduct)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    public VEDirectMessage getLastMessage(VictronProduct victronProduct) {
        return messageService.last(victronProduct, Duration.of(1, SECONDS))
                .max(Comparator.comparing(VEDirectMessage::getTimestamp))
                .map(VEDirectMessage.class::cast)
                .orElseGet(VEDirectMessage::new);
    }
}
