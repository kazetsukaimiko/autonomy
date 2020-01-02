package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.entity.jsonlink.PermutationEntity;
import io.freedriver.autonomy.entity.jsonlink.PinGroupEntity;
import io.freedriver.ee.cdi.qualifier.NitriteDatabase;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.filters.ObjectFilters;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.stream.Stream;

@ApplicationScoped
public class PermutationService extends JsonLinkCRUDService<PermutationEntity> {
    @Inject
    public PermutationService(@NitriteDatabase(deployment = Autonomy.DEPLOYMENT, database = EntityBase.class) Nitrite nitrite) {
        super(nitrite);
    }

    @Override
    Class<PermutationEntity> getKlazz() {
        return PermutationEntity.class;
    }

    public Stream<PermutationEntity> byPinGroup(PinGroupEntity pinGroupEntity) {
        return findAll(ObjectFilters.eq("groupId", pinGroupEntity.getId()));
    }
}
