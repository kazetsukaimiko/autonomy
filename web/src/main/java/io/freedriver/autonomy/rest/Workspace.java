package io.freedriver.autonomy.rest;

import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import io.freedriver.autonomy.service.crud.WorkspaceService;
import org.dizitart.no2.NitriteId;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@RequestScoped
public class Workspace implements WorkspaceApi {
    @Inject
    private WorkspaceService service;

    @Override
    public List<WorkspaceEntity> getWorkspaces() {
        return service.findAll()
                .collect(Collectors.toList());
    }

    @Override
    public WorkspaceEntity getWorkspaceById(NitriteId id) {
        return null;
    }

    @Override
    public WorkspaceEntity createWorkspace() {
        return null;
    }

    @Override
    public WorkspaceEntity update(NitriteId id, WorkspaceEntity workspaceEntity) {
        return null;
    }

    @Override
    public boolean delete(NitriteId id) {
        return false;
    }
}
