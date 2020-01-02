package io.freedriver.autonomy.entity.jsonlink;

import java.util.Objects;

public class BoardNameEntity extends JsonLinkEntity {
    private String name;

    public BoardNameEntity() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BoardNameEntity that = (BoardNameEntity) o;
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
