package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.entity.jsonlink.JsonLinkEntity;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public abstract class JsonLinkCRUDService<T extends JsonLinkEntity<T>> extends NitriteCRUDService<T> {
    public static ObjectFilter byBoardId(UUID boardId) {
        return ObjectFilters.eq("boardId", boardId);
    }

    public Stream<T> findByBoardId(UUID boardId) {
        return find(byBoardId(boardId));
    }

    public Optional<T> findOneByBoardId(UUID boardId) {
        return find(byBoardId(boardId)).findFirst();
    }
}
