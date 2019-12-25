package io.freedriver.autonomy.async;

import io.freedriver.autonomy.cdi.qualifier.VEProduct;
import io.freedriver.autonomy.rest.VEDirectEndpointApi;
import kaze.victron.VEDirectMessage;
import kaze.victron.VictronProduct;
import kaze.victron.VictronProductType;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

@ApplicationScoped
public class VEDirectMessageActor implements VEDirectEndpointApi {
    private static final Logger LOGGER = Logger.getLogger(VEDirectMessageActor.class.getName());

    private static final Map<VictronProduct, VEDirectMessage> lastMessage = new ConcurrentHashMap<>();

    /*
     * SERVICE METHODS
     */
    public Map<VictronProduct, VEDirectMessage> getSummary() {
        return new HashMap<>(lastMessage);
    }

    public Set<VictronProduct> getProducts() {
        return lastMessage.keySet();
    }



    /*
     * EVENT HANDLERS
     */

    public synchronized void actOnVEDirectMessage(@Observes @Default VEDirectMessage veDirectMessage) throws IOException {
        handleProductMessage(VictronProduct.of(veDirectMessage), veDirectMessage);
    }

    public synchronized void handleProductMessage(VictronProduct product, VEDirectMessage veDirectMessage) {
        if (lastMessage.containsKey(product)) {
            Stream.of(VEDirectMessageChange.values())
                    .forEach(field ->
                            compareMessageField(
                                field,
                                lastMessage.get(product),
                                veDirectMessage));
        } else {
            LOGGER.info(product + " VE.Direct initial field values: " +
                    VEDirectMessageChange.allValues(veDirectMessage));
        }
        lastMessage.put(product, veDirectMessage);
    }

    @Produces @VEProduct(value = VictronProductType.UNKNOWN, serial = "")
    public Optional<VEDirectMessage> getLastMessage(InjectionPoint injectionPoint) {
        return injectionPoint.getQualifiers().stream()
                .filter(VEProduct.class::isInstance)
                .map(VEProduct.class::cast)
                .findFirst()
                .map(productQualifier -> new VictronProduct(productQualifier.value(), productQualifier.serial()))
                .filter(lastMessage::containsKey)
                .map(lastMessage::get);
    }

    private void compareMessageField(VEDirectMessageChange field, VEDirectMessage oldMessage, VEDirectMessage newMessage) {
        field.test(oldMessage, newMessage, LOGGER::info);
    }

}
