package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import io.freedriver.ee.cdi.qualifier.NitriteDatabase;
import org.dizitart.no2.Nitrite;

import javax.inject.Inject;

public class WorkspaceService extends NitriteCRUDService<WorkspaceEntity> {
    @Inject
    protected WorkspaceService(@NitriteDatabase(deployment = Autonomy.DEPLOYMENT, database = WorkspaceEntity.class) Nitrite nitrite) {
        super(nitrite);
    }

    @Override
    protected Class<WorkspaceEntity> getKlazz() {
        return WorkspaceEntity.class;
    }
}
