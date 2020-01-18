package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.jsonlink.VersionEntity;
import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import org.dizitart.no2.Nitrite;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

public class WorkspaceServiceUnitTest extends NitriteCRUDServiceUnitTest<WorkspaceEntity, WorkspaceService> {
    @Override
    Class<WorkspaceEntity> getKlazz() {
        return WorkspaceEntity.class;
    }

    @Override
    Function<Nitrite, WorkspaceService> getConstructor() {
        return (nitrite) -> {
            WorkspaceService service = new WorkspaceService();
            service.setNitrite(nitrite);
            return service;
        };
    }

    @Override
    WorkspaceEntity create(int idx) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setName("Generated");
        VersionEntity version = new VersionEntity();
        for (int i=0; i<idx;i++) {
            version = version.bump();
        }
        entity.setVersion(version);
        entity.getBoards().add(generateBoard(boardId, 0));
        return entity;
    }


    @Test
    public void testGenerateFromMappings() {
        //getVictim()
    }
}
