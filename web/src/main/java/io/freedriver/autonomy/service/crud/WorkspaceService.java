package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WorkspaceService extends NitriteCRUDService<WorkspaceEntity> {

    public WorkspaceService() {
    }

    @Override
    protected Class<WorkspaceEntity> getKlazz() {
        return WorkspaceEntity.class;
    }
}