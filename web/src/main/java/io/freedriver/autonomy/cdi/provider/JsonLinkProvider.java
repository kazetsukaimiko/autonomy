package io.freedriver.autonomy.cdi.provider;

import io.freedriver.autonomy.config.Configuration;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.ConnectorException;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.pin.Pin;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@Dependent
public class JsonLinkProvider {

    @Inject
    private Configuration configuration;

    @Produces @Default
    public Connector getDefaultConnector() throws ConnectorException {
        Connector connector = Connector.getDefault()
                .orElseThrow(() -> new ConnectorException("Couldn't spawn Connector."));

        Request modeSets = configuration.getAliases()
                .keySet()
                .stream()
                .map(Identifier::new)
                .map(identifier -> identifier.setMode(Mode.OUTPUT))
                .reduce(new Request(), Request::modeSet, (a, b) -> a);

        connector.send(modeSets);

        return connector;
    }
}
