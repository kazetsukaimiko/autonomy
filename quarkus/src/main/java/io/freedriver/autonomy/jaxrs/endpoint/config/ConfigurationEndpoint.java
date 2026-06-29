package io.freedriver.autonomy.jaxrs.endpoint.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.freedriver.jsonlink.config.ConnectorConfig;

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
