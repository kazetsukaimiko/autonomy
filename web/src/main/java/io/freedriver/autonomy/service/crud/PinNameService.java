package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.jsonlink.PinNameEntity;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PinNameService extends JsonLinkCRUDService<PinNameEntity> {
    @Override
    Class<PinNameEntity> getKlazz() {
        return PinNameEntity.class;
    }
}
