package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.entity.jsonlink.BoardNameEntity;
import io.freedriver.ee.cdi.qualifier.NitriteDatabase;
import io.freedriver.jsonlink.Connector;
import org.dizitart.no2.Nitrite;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BoardNameService extends JsonLinkCRUDService<BoardNameEntity> {
    @Inject
    public BoardNameService(@NitriteDatabase(deployment = Autonomy.DEPLOYMENT, database = EntityBase.class) Nitrite nitrite) {
        super(nitrite);
    }

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
        nameEntity.setPosition(findAll().count());
        return save(nameEntity);
    }
}
