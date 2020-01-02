package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.jsonlink.JsonLinkEntity;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public abstract class JsonLinkCRUDService<T extends JsonLinkEntity> extends NitriteCRUDService<T> {
    protected JsonLinkCRUDService(Nitrite nitrite) {
        super(nitrite);
    }

    public static ObjectFilter byBoardId(UUID boardId) {
        return ObjectFilters.eq("boardId", boardId);
    }

    public Stream<T> findByBoardId(UUID boardId) {
        return findAll(byBoardId(boardId));
    }

    public Optional<T> findOneByBoardId(UUID boardId) {
        return findAll(byBoardId(boardId)).findFirst();
    }
}
