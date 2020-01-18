package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.autonomy.entity.EntityBase;
import org.dizitart.no2.objects.InheritIndices;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@InheritIndices
public class WorkspaceEntity extends EntityBase {
    private VersionEntity version = new VersionEntity();
    private UUID uuid = UUID.randomUUID();
    private String name;
    private List<BoardEntity> boards = new ArrayList<>();
    private boolean current = false;

    public WorkspaceEntity() {
    }

    public WorkspaceEntity(WorkspaceEntity entity) {
        super(entity);
        this.version = entity.version;
        this.uuid = entity.uuid;
        this.name = entity.name;
        this.boards = entity.boards;
        this.current = false;
    }

    public VersionEntity getVersion() {
        return version;
    }

    public void setVersion(VersionEntity version) {
        this.version = version;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BoardEntity> getBoards() {
        return boards;
    }

    public void setBoards(List<BoardEntity> boards) {
        this.boards = boards;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }
}
