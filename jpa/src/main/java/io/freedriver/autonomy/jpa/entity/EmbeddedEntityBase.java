package io.freedriver.autonomy.jpa.entity;

import java.io.Serializable;

import io.freedriver.autonomy.jpa.iface.Positional;

public abstract class EmbeddedEntityBase implements Serializable, Positional {
    private long position = 0;

    protected EmbeddedEntityBase() {
    }

    protected EmbeddedEntityBase(EmbeddedEntityBase base) {
        this.position = base.position;
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public void setPosition(long position) {
        this.position = position;
    }
}
