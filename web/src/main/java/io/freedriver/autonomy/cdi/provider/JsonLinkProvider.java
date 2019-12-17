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
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class JsonLinkProvider {

    private static final Logger LOGGER = Logger.getLogger(JsonLinkProvider.class.getName());
    @Inject
    private Configuration configuration;

    private Connector connector;

    @Produces @Default @Dependent
    public Connector getDefaultConnector() throws ConnectorException {
        if (connector == null || connector.isClosed()) {
            LOGGER.warning("Opening new Connector instance");
            connector = Connectors.allConnectors()
                    .findFirst()
                    .orElseThrow(() -> new ConnectorException("Couldn't spawn Connector."));
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

        return connector;
    }




    @Produces @Dependent @ByUUID("")
    public Connector getDefaultConnector(InjectionPoint injectionPoint) throws ConnectorException {
        UUID parameterValue = injectionPoint.getQualifiers().stream()
            .filter(ByUUID.class::isInstance)
            .map(ByUUID.class::cast)
            .map(ByUUID::value)
            .map(JsonLinkProvider::fromUUIDString)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Cannot get UUID for Connector."));

        if (connector == null || connector.isClosed()) {
            LOGGER.warning("Opening new Connector instance");
            connector = Connectors.allConnectors()
                    .findFirst()
                    .orElseThrow(() -> new ConnectorException("Couldn't spawn Connector."));
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

        return connector;
    }

    private static UUID fromUUIDString(String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Invalid UUID String: " + uuidString, iae);
        }
    }

}
