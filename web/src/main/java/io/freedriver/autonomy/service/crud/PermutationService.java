package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.jsonlink.PermutationEntity;
import io.freedriver.autonomy.entity.jsonlink.PinGroupEntity;
import org.dizitart.no2.objects.filters.ObjectFilters;

import javax.enterprise.context.ApplicationScoped;
import java.util.stream.Stream;

@ApplicationScoped
public class PermutationService extends JsonLinkCRUDService<PermutationEntity> {
    @Override
    Class<PermutationEntity> getKlazz() {
        return PermutationEntity.class;
    }

    public Stream<PermutationEntity> byPinGroup(PinGroupEntity pinGroupEntity) {
        return find(ObjectFilters.eq("groupId", pinGroupEntity.getNitriteId()));
    }
}
