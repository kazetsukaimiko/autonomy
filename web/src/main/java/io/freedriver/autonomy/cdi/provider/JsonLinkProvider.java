package io.freedriver.autonomy.cdi.provider;

import io.freedriver.autonomy.config.Configuration;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.pin.Pin;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.stream.Stream;

@ApplicationScoped
public class JsonLinkProvider {

    @Inject
    private Configuration configuration;

    private Connector connector;

    @Produces @Default @ApplicationScoped
    public Connector getDefaultConnector() throws ConnectorException {
        if (connector == null || connector.isClosed()) {
            connector = Connector.getDefault()
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
        }

        return connector;
    }
}
