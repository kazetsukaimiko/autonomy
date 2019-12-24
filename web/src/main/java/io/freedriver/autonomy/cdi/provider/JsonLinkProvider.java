package io.freedriver.autonomy.cdi.provider;

import io.freedriver.autonomy.cdi.qualifier.ByUUID;
import io.freedriver.autonomy.config.Configuration;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.Request;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class JsonLinkProvider {

    private static final Logger LOGGER = Logger.getLogger(JsonLinkProvider.class.getName());
    private static final Map<UUID, Connector> connectors = new ConcurrentHashMap<>();

    @Inject
    private Configuration configuration;

    @Produces @Dependent @ByUUID("")
    public Connector getDefaultConnector(InjectionPoint injectionPoint) throws ConnectorException {
        UUID parameterValue = injectionPoint.getQualifiers().stream()
                .peek(q -> LOGGER.warning("Qualifier: " + q.getClass().getName()))
            .filter(ByUUID.class::isInstance)
            .map(ByUUID.class::cast)
            .map(ByUUID::value)
            .map(UUID::fromString)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(noUUIDExceptionMessage(injectionPoint)));

        if (!connectors.containsKey(parameterValue) || connectors.get(parameterValue).isClosed()) {
            Connector connector = Connectors.allConnectors()
                    .filter(candidate -> Objects.equals(candidate.getUUID(), parameterValue))
                    .findFirst()
                    .orElseThrow(() -> new ConnectorException(
                            "Couldn't find Connector for board "
                            + parameterValue.toString()
                            + ", available boards: "
                            + getAvailableUUIDs()
                    ));
            LOGGER.warning("Connector " + parameterValue.toString() + " spawned. Mode setting. ");

            Request modeSets = new Request();
            configuration.getAliases()
                    .keySet()
                    .stream()
                    .map(Identifier::new)
                    .forEach(identifier -> modeSets
                            .modeSet(identifier.setMode(Mode.OUTPUT))
                            .digitalWrite(identifier.setDigital(true)));

            connector.send(modeSets);
        } else {
            LOGGER.warning("Using existing connector instance");
        }
        return connectors.get(parameterValue);
    }

    private static final String getAvailableUUIDs() {
        return Connectors.allConnectors()
                .map(Connector::getUUID)
                .map(UUID::toString)
                .collect(Collectors.joining(", "));
    }

    private static String noUUIDExceptionMessage(InjectionPoint injectionPoint) {
        String uuids = getAvailableUUIDs();

        return "Cannot get UUID for Connector, injection point: "
                + injectionPoint.getMember().getDeclaringClass().getName()
                + "/"
                + injectionPoint.getMember().getName()
                + "\nAvailable UUIDs: ("+uuids+")"
                ;
    }

}
