package io.freedriver.autonomy.jaxrs.endpoint;


import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.freedriver.autonomy.entity.view.LithiumBatteryView;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path(SBMSApi.ROOT)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public interface SBMSApi {
    String ROOT = "/sbms";

    @GET
    LithiumBatteryView getBatteryView();
}
