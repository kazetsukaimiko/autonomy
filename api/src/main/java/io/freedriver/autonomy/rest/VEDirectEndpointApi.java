package io.freedriver.autonomy.rest;

import kaze.victron.VEDirectMessage;
import kaze.victron.VictronProduct;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path(VEDirectEndpointApi.ROOT)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface VEDirectEndpointApi {
    String ROOT = "/vedirect";
    String PRODUCT_PATH = "/product";
    String PRODUCT_SERIAL = "productSerial";
    String PRODUCT_SERIAL_PATH = PRODUCT_PATH + "/{"+PRODUCT_SERIAL+"}";
    String NUMBER = "number";
    String UNITS = "units";
    String MESSAGE_TIME_PATH = PRODUCT_SERIAL_PATH + "/last/{"+NUMBER+"}/{"+UNITS+"}";

    @GET
    @Path(PRODUCT_PATH)
    Set<VictronProduct> getProducts();

    @GET
    @Path(PRODUCT_SERIAL_PATH)
    List<VEDirectMessage> getProducts(@PathParam(PRODUCT_SERIAL) String serial);

    @GET
    @Path(MESSAGE_TIME_PATH)
    List<VEDirectMessage> getProductsFrom(@PathParam(PRODUCT_SERIAL) String serial, @PathParam(NUMBER) Integer number, @PathParam(UNITS) ChronoUnit chronoUnit);
}
