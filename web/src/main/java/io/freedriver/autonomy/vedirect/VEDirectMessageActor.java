package io.freedriver.autonomy.vedirect;

import io.freedriver.autonomy.cdi.qualifier.VEProduct;
import io.freedriver.autonomy.service.ReportingService;
import kaze.victron.VEDirectMessage;
import kaze.victron.VictronDevice;
import kaze.victron.VictronProduct;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

@ApplicationScoped
public class VEDirectMessageActor {
    private static final Logger LOGGER = Logger.getLogger(VEDirectMessageActor.class.getName());
    private static final Map<VictronDevice, VEDirectMessage> lastMessage = new ConcurrentHashMap<>();

    @Inject
    private VEDirectMessageService messageService;

    @Inject
    private ReportingService reportingService;

    /*
     * SERVICE METHODS
     */
    public Map<VictronDevice, VEDirectMessage> getSummary() {
        return new HashMap<>(lastMessage);
    }

    public Set<VictronDevice> getProducts() {
        return lastMessage.keySet();
    }



    /*
     * EVENT HANDLERS
     */
    public synchronized void actOnVEDirectMessage(@Observes @Default VEDirectMessage veDirectMessage) throws IOException {
        report(veDirectMessage);
        VictronDevice.of(veDirectMessage)
                .ifPresent(product -> handleProductMessage(product, veDirectMessage));
    }

    /**
     * Log useful information.
     */
    private void report(VEDirectMessage veDirectMessage) {
        VEDirectMessageLogging.stream()
                .filter(field -> field.validate(veDirectMessage))
                .forEach(field ->
                        reportingService.update(
                                field.getFieldName(veDirectMessage),
                                () -> LOGGER.info(field.getFieldName(veDirectMessage) + ": " + field.getMessage(veDirectMessage)),
                                field.getInterval(veDirectMessage)));
    }

    public synchronized void handleProductMessage(VictronDevice product, VEDirectMessage veDirectMessage) {
        if (lastMessage.containsKey(product)) {
            Stream.of(VEDirectMessageChange.values())
                    .forEach(field ->
                            compareMessageField(
                                field,
                                lastMessage.get(product),
                                veDirectMessage));
        } else {
            LOGGER.info(product + " VE.Direct initial field values: \n" +
                    VEDirectMessageChange.allValues(veDirectMessage));
        }
        lastMessage.put(product, veDirectMessage);
        messageService.save(new io.freedriver.autonomy.jpa.entity.VEDirectMessage(veDirectMessage));
    }

    @Produces @VEProduct(value = VictronProduct.UNKNOWN, serial = "")
    public Optional<VEDirectMessage> getLastMessage(InjectionPoint injectionPoint) {
        return injectionPoint.getQualifiers().stream()
                .filter(VEProduct.class::isInstance)
                .map(VEProduct.class::cast)
                .findFirst()
                .map(productQualifier -> new VictronDevice(productQualifier.value(), productQualifier.serial()))
                .filter(lastMessage::containsKey)
                .map(lastMessage::get);
    }

    private void compareMessageField(VEDirectMessageChange field, VEDirectMessage oldMessage, VEDirectMessage newMessage) {
        field.test(oldMessage, newMessage, LOGGER::info);
    }

}
