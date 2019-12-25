package io.freedriver.autonomy.rest;

import kaze.victron.VEDirectMessage;
import kaze.victron.VictronProduct;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path(VEDirectEndpointApi.ROOT)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface VEDirectEndpointApi {
    String ROOT = "/vedirect";
    String PRODUCT_PATH = "/product";

    @GET
    Map<VictronProduct, VEDirectMessage> getSummary();

    @GET
    @Path(PRODUCT_PATH)
    Set<VictronProduct> getProducts();
}
