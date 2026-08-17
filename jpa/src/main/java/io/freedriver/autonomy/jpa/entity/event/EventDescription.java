package io.freedriver.autonomy.jpa.entity.event;

import io.freedriver.autonomy.jpa.entity.EmbeddedEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class EventDescription extends EmbeddedEntityBase {
    @Enumerated(EnumType.STRING)
    private StateType type;
    @Column
    private String state;

    public EventDescription(StateType type, String state) {
        this.type = type;
        this.state = state;
    }

    public <E extends Enum<E>> EventDescription(StateType type, E enumState) {
        this.type = type;
        this.state = enumState.name();
    }
}
