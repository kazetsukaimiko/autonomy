package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.autonomy.entity.EntityBase;
import io.freedriver.autonomy.iface.Positional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WorkspaceEntity extends EntityBase {
    private VersionEntity version;
    private List<BoardEntity> boards = new ArrayList<>();
    private boolean current = false;

    public WorkspaceEntity() {
    }

    public WorkspaceEntity(WorkspaceEntity entity) {
        super(entity);
        this.boards = entity.boards;
        this.current = false;
    }

    public VersionEntity getVersion() {
        return version;
    }

    public void setVersion(VersionEntity version) {
        this.version = version;
    }

    public List<BoardEntity> getBoards() {
        if (boards == null) {
            boards = new ArrayList<>();
        }
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
