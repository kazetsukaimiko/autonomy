package io.freedriver.autonomy.entity.jsonlink;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.freedriver.autonomy.entity.EmbeddedEntityBase;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.Request;

import java.util.Objects;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@entity")
public abstract class PinEntity extends EmbeddedEntityBase {
    private Identifier pin;
    private String name;
    private Mode mode;

    abstract Request apply(Request request);

    public PinEntity() {
    }

    public PinEntity(int pin, String name, Mode mode) {
        this(Identifier.of(pin), name, mode);
    }

    public PinEntity(Identifier pin, String name, Mode mode) {
        this.pin = pin;
        this.name = name;
        this.mode = mode;
    }

    public PinEntity(PinEntity entity) {
        super(entity);
        this.pin = entity.pin;
        this.name = entity.name;
        this.mode = entity.mode;
    }

    public Identifier getPin() {
        return pin;
    }

    public void setPin(Identifier pin) {
        this.pin = pin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PinEntity that = (PinEntity) o;
        return Objects.equals(pin, that.pin) &&
                Objects.equals(name, that.name) &&
                mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pin, name, mode);
    }

    @Override
    public String toString() {
        return "PinNameEntity{" +
                "pinNumber=" + pin +
                ", pinName='" + name + '\'' +
                ", pinMode=" + mode +
                '}';
    }
}
