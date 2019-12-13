package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.config.Configuration;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

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
    private Configuration configuration;


    @GET
    public Configuration getConfiguration() {
        return configuration;
    }


}
