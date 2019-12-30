package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.jsonlink.PinGroupEntity;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.util.Optional;

public class PinGroupService extends JsonLinkCRUDService<PinGroupEntity> {
    @Override
    Class<PinGroupEntity> getKlazz() {
        return PinGroupEntity.class;
    }

    public Optional<PinGroupEntity> getByName(String target) {
        return find(ObjectFilters.eq("name", target))
                .findFirst();
    }
}
