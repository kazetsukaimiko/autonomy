package io.freedriver.autonomy;

import io.freedriver.autonomy.entity.jsonlink.BoardNameEntity;
import io.freedriver.autonomy.service.crud.BoardNameService;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import javax.inject.Inject;
import java.util.UUID;

@RunWith(Arquillian.class)
@DefaultDeployment
public class BoardNameServiceTest extends NitriteCRUDServiceITTest<BoardNameEntity, BoardNameService> {
    @Inject
    private BoardNameService victim;

    @Override
    BoardNameService getVictim() {
        return victim;
    }

    @Override
    BoardNameEntity generate(int i) {
        BoardNameEntity entity = new BoardNameEntity();
        entity.setName(randomName());
        entity.setPosition(i);
        entity.setBoardId(UUID.randomUUID());
        return entity;
    }
}
