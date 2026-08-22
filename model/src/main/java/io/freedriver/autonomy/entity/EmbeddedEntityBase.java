package io.freedriver.autonomy.entity;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public abstract class EmbeddedEntityBase implements Serializable, Positional {
    private long position = 0;

    protected EmbeddedEntityBase(EmbeddedEntityBase base) {
        this.position = base.position;
    }
}
