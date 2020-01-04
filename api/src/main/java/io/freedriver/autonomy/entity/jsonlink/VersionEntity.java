package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.autonomy.entity.EmbeddedEntityBase;

import java.util.UUID;

public class VersionEntity extends EmbeddedEntityBase implements Comparable<VersionEntity> {
    private long id;
    private String name;

    public VersionEntity() {
    }

    public VersionEntity(VersionEntity entity) {
        super(entity);
        this.id = entity.id;
        this.name = entity.name;
    }

    public static VersionEntity generate(String name) {
        VersionEntity entity = new VersionEntity();
        entity.setId(System.currentTimeMillis());
        entity.setName(name);
        return entity;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(VersionEntity entity) {
        return Long.compare(id, entity.id);
    }
}
