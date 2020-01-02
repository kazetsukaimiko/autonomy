package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.entity.jsonlink.PinGroupEntity;
import io.freedriver.ee.cdi.qualifier.NitriteDatabase;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.filters.ObjectFilters;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class PinGroupService extends JsonLinkCRUDService<PinGroupEntity> {
    @Inject
    public PinGroupService(@NitriteDatabase(deployment = Autonomy.DEPLOYMENT, database = EntityBase.class) Nitrite nitrite) {
        super(nitrite);
    }

    @Override
    Class<PinGroupEntity> getKlazz() {
        return PinGroupEntity.class;
    }

    public Optional<PinGroupEntity> getByName(String target) {
        return findAll(ObjectFilters.eq("name", target))
                .findFirst();
    }
}
