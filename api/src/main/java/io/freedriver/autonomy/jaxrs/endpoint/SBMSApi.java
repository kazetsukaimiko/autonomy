package io.freedriver.autonomy.jaxrs.endpoint;


import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.freedriver.autonomy.entity.view.LithiumBatteryView;

@Path(SBMSApi.ROOT)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface SBMSApi {
    String ROOT = "/sbms";

    @GET
    LithiumBatteryView getBatteryView();
}
