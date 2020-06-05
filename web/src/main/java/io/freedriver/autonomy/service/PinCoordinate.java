package io.freedriver.autonomy.service;

import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

import java.util.Objects;
import java.util.UUID;

public class PinCoordinate {
    private final UUID boardId;
    private final Identifier identifier;

    public PinCoordinate(UUID boardId, Identifier identifier) {
        this.boardId = boardId;
        this.identifier = identifier;
    }

    public UUID getBoardId() {
        return boardId;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PinCoordinate that = (PinCoordinate) o;
        return Objects.equals(boardId, that.boardId) &&
                Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardId, identifier);
    }
}
