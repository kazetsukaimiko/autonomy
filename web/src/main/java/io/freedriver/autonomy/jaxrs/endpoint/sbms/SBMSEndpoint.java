package io.freedriver.autonomy.jaxrs.endpoint.sbms;

import io.freedriver.autonomy.entity.view.LithiumBatteryView;
import io.freedriver.autonomy.jaxrs.endpoint.SBMSApi;
import io.freedriver.autonomy.service.SBMSEventService;

import javax.inject.Inject;

public class SBMSEndpoint implements SBMSApi {

    @Inject
    private SBMSEventService eventService;

    @Override
    public LithiumBatteryView getBatteryView() {
        return eventService.getLithiumBatteryView();
    }
}
