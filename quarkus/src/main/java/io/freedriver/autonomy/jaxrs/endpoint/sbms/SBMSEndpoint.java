package io.freedriver.autonomy.jaxrs.endpoint.sbms;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.freedriver.autonomy.entity.view.LithiumBatteryView;
import io.freedriver.autonomy.jaxrs.endpoint.SBMSApi;
import io.freedriver.autonomy.service.SBMSEventService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path(SBMSApi.ROOT)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SBMSEndpoint implements SBMSApi {

    @Inject
    SBMSEventService eventService;

    @Override
    public LithiumBatteryView getBatteryView() {
        return eventService.getLithiumBatteryView();
    }
}
