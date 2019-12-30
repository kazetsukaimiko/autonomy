package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.jsonlink.BoardNameEntity;
import io.freedriver.jsonlink.Connector;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BoardNameService extends JsonLinkCRUDService<BoardNameEntity> {
    @Override
    Class<BoardNameEntity> getKlazz() {
        return BoardNameEntity.class;
    }

    public BoardNameEntity findOrCreate(Connector connector) {
        return findOneByBoardId(connector.getUUID())
                .orElseGet(() -> newBoard(connector));
    }

    public BoardNameEntity newBoard(Connector connector) {
        BoardNameEntity nameEntity = new BoardNameEntity();
        nameEntity.setName("Unnamed Board");
        nameEntity.setBoardId(connector.getUUID());
        nameEntity.setPosition(find().count());
        return save(nameEntity);
    }
}
