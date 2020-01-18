package io.freedriver.autonomy;

import io.freedriver.autonomy.entity.jsonlink.VersionEntity;
import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import io.freedriver.autonomy.service.crud.WorkspaceService;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import javax.inject.Inject;
import java.util.UUID;

@RunWith(Arquillian.class)
@DefaultDeployment
public class WorkspaceServiceITTest extends NitriteCRUDServiceITTest<WorkspaceEntity, WorkspaceService> {
    @Inject
    private WorkspaceService victim;

    @Override
    WorkspaceService getVictim() {
        return victim;
    }

    @Override
    protected WorkspaceEntity generate(UUID boardId, int i) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setVersion(new VersionEntity());
        entity.getBoards().add(generateBoard(boardId, 0));
        return entity;
    }

}
