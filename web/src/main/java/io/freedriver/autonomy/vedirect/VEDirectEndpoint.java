package io.freedriver.autonomy.vedirect;

import io.freedriver.autonomy.rest.VEDirectEndpointApi;
import kaze.victron.VEDirectMessage;
import kaze.victron.VictronProduct;

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
public class VEDirectEndpoint implements VEDirectEndpointApi {
    @Inject
    private VEDirectMessageService messageService;

    @Override
    public Set<VictronProduct> getProducts() {
        return messageService.products();
    }

    @Override
    public List<VEDirectMessage> getProducts(String serial) {
        return bySerial(serial)
                .flatMap(messageService::byProduct)
                .map(VEDirectMessage.class::cast)
                .collect(Collectors.toList());
    }

    @Override
    public List<VEDirectMessage> getProductsFrom(String serial, Integer number, ChronoUnit chronoUnit) {
        return bySerial(serial)
                .flatMap(product -> messageService.last(product, Duration.of(number, chronoUnit)))
                .collect(Collectors.toList());
    }

    @Override
    public List<BigDecimal> getFieldData(String serial, Integer number, ChronoUnit chronoUnit, String field) {
        return null;
    }

    private Stream<VictronProduct> bySerial(String serial) {
        return getProducts().stream()
                .filter(product -> Objects.equals(serial, product.getSerialNumber()));
    }
}
