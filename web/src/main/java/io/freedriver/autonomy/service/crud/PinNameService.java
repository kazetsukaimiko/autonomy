package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.entity.jsonlink.PinNameEntity;
import io.freedriver.ee.cdi.qualifier.NitriteDatabase;
import org.dizitart.no2.Nitrite;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PinNameService extends JsonLinkCRUDService<PinNameEntity> {
    @Inject
    public PinNameService(@NitriteDatabase(deployment = Autonomy.DEPLOYMENT, database = EntityBase.class) Nitrite nitrite) {
        super(nitrite);
    }

    @Override
    Class<PinNameEntity> getKlazz() {
        return PinNameEntity.class;
    }
}
