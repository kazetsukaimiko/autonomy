package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.autonomy.entity.EntityBase;

import java.util.Objects;
import java.util.UUID;

public abstract class JsonLinkEntity extends EntityBase {
    private UUID boardId;

    public JsonLinkEntity() {
    }

    public UUID getBoardId() {
        return boardId;
    }

    public void setBoardId(UUID boardId) {
        this.boardId = boardId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonLinkEntity that = (JsonLinkEntity) o;
        return Objects.equals(boardId, that.boardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardId);
    }

    @Override
    public String toString() {
        return "JsonLinkEntity{" +
                "boardId=" + boardId +
                '}';
    }
}
