package io.freedriver.autonomy.rest.view;

import io.freedriver.autonomy.entity.jsonlink.VersionEntity;
import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import org.dizitart.no2.NitriteId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class WorkspaceView {
    private UUID uuid;
    private NitriteId currentId;
    private String currentName;
    private VersionEntity currentVersion;
    private VersionEntity activeVersion;
    private List<VersionEntity> availableVersions = new ArrayList<>();
    private boolean active;

    public WorkspaceView() {
    }

    public WorkspaceView(UUID uuid, NitriteId currentId, String currentName, VersionEntity currentVersion, VersionEntity activeVersion, List<VersionEntity> availableVersions, boolean active) {
        this.uuid = uuid;
        this.currentId = currentId;
        this.currentName = currentName;
        this.currentVersion = currentVersion;
        this.activeVersion = activeVersion;
        this.availableVersions = availableVersions;
        this.active = active;
    }

    public static Stream<WorkspaceView> viewsOfEntities(Stream<WorkspaceEntity> stream) {
        Map<UUID, WorkspaceView> map = new HashMap<>();
        stream.forEach(entity -> {
            if (!map.containsKey(entity.getUuid())) {
                map.put(entity.getUuid(), new WorkspaceView(entity.getUuid(), entity.getId(), entity.getName(), entity.getVersion(), entity.isCurrent() ? entity.getVersion() : null, new ArrayList<>(), entity.isCurrent()));
            }
            map.get(entity.getUuid())
                    .apply(entity);
        });
        return map.values().stream()
                .peek(view -> Collections.sort(view.availableVersions));
    }

    private void apply(WorkspaceEntity entity) {
        if (entity.isCurrent()) {
            setActive(true);
            setCurrentId(entity.getId());
            setCurrentName(entity.getName());
            setActiveVersion(entity.getVersion());
        }
        if (getCurrentVersion() == null || getCurrentVersion().compareTo(entity.getVersion()) < 0) {
            setCurrentVersion(entity.getVersion());
        }
        if (!getAvailableVersions().contains(entity.getVersion())) {
            getAvailableVersions().add(entity.getVersion());
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public NitriteId getCurrentId() {
        return currentId;
    }

    public void setCurrentId(NitriteId currentId) {
        this.currentId = currentId;
    }

    public String getCurrentName() {
        return currentName;
    }

    public void setCurrentName(String currentName) {
        this.currentName = currentName;
    }

    public VersionEntity getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(VersionEntity currentVersion) {
        this.currentVersion = currentVersion;
    }

    public VersionEntity getActiveVersion() {
        return activeVersion;
    }

    public void setActiveVersion(VersionEntity activeVersion) {
        this.activeVersion = activeVersion;
    }

    public List<VersionEntity> getAvailableVersions() {
        return availableVersions;
    }

    public void setAvailableVersions(List<VersionEntity> availableVersions) {
        this.availableVersions = availableVersions;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
