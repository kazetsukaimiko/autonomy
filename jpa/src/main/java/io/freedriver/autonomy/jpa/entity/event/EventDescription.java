package io.freedriver.autonomy.jpa.entity.event;

import io.freedriver.autonomy.jpa.entity.EmbeddedEntityBase;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.Objects;

@Embeddable
public class EventDescription extends EmbeddedEntityBase {
    @Enumerated(EnumType.STRING)
    private StateType type;
    @Column
    private String state;

    public EventDescription() {
    }

    public EventDescription(StateType type, String state) {
        this.type = type;
        this.state = state;
    }

    public <E extends Enum<E>> EventDescription(StateType type, E enumState) {
        this.type = type;
        this.state = enumState.name();
    }

    public StateType getType() {
        return type;
    }

    public void setType(StateType type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventDescription that = (EventDescription) o;
        return type == that.type &&
                Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, state);
    }

    @Override
    public String toString() {
        return "EventDescription{" +
                "type=" + type +
                ", state='" + state + '\'' +
                '}';
    }
}
