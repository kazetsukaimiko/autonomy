package io.freedriver.autonomy.jaxrs.endpoint.sbms;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.freedriver.autonomy.entity.view.LithiumBatteryView;
import io.freedriver.autonomy.jaxrs.endpoint.SBMSApi;
import io.freedriver.autonomy.service.SBMSEventService;

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
