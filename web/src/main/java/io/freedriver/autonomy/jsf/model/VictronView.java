package io.freedriver.autonomy.jsf.model;

import io.freedriver.autonomy.async.VEDirectDeviceService;
import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import io.freedriver.autonomy.jpa.entity.VEDirectMessage_;
import io.freedriver.autonomy.vedirect.VEDirectMessageService;
import kaze.victron.VictronDevice;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Named
@RequestScoped
public class VictronView {
    @Inject
    private VEDirectDeviceService deviceService;

    @Inject
    private VEDirectMessageService messageService;

    public List<VictronDevice> getProducts() {
        return messageService.devices()
                .stream()
                .sorted(Comparator.comparing(VictronDevice::getSerialNumber))
                .collect(Collectors.toList());
    }

    public VEDirectMessage getLastMessage(VictronDevice victronDevice) {
        return messageService.max(victronDevice, VEDirectMessage_.timestamp)
                .orElse(null);
    }
}
