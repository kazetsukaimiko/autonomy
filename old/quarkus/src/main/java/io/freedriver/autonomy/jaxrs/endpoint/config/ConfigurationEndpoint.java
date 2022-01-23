package io.freedriver.autonomy.jaxrs.endpoint.config;

import io.freedriver.jsonlink.config.ConnectorConfig;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigurationEndpoint {

    @Inject
    ConnectorConfig configuration;

    @GET
    public ConnectorConfig getConfiguration() {
        return configuration;
    }


}
