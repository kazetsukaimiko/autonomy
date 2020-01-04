package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.autonomy.entity.EmbeddedEntityBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BoardEntity extends EmbeddedEntityBase {
    private UUID boardId;
    private String name;
    private List<GroupEntity> groups = new ArrayList<>();
    private List<PinEntity> pins = new ArrayList<>();

    public BoardEntity() {
    }

    public BoardEntity(BoardEntity entity) {
        super(entity);
        this.boardId = entity.boardId;
        this.name = entity.name;
        this.groups = entity.groups;
        this.pins = entity.pins;
    }

    public UUID getBoardId() {
        return boardId;
    }

    public void setBoardId(UUID boardId) {
        this.boardId = boardId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GroupEntity> getGroups() {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        return groups;
    }

    public void setGroups(List<GroupEntity> groups) {
        this.groups = groups;
    }

    public List<PinEntity> getPins() {
        return pins;
    }

    public void setPins(List<PinEntity> pins) {
        this.pins = pins;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BoardEntity that = (BoardEntity) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }

    @Override
    public String toString() {
        return "BoardNameEntity{" +
                "name='" + name + '\'' +
                '}';
    }
}
