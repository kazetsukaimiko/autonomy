package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.autonomy.entity.EmbeddedEntityBase;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalWrite;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GroupEntity extends EmbeddedEntityBase {
    private String name;
    private List<PinEntity> pins = new ArrayList<>();
    private List<PermutationEntity> permutations = new ArrayList<>();

    public GroupEntity() {
    }

    public GroupEntity(GroupEntity entity) {
        super(entity);
        this.name = entity.name;
        this.pins = entity.pins;
        this.permutations = entity.permutations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PinEntity> getPins() {
        return pins;
    }

    public void setPins(List<PinEntity> pins) {
        this.pins = pins;
    }

    public List<PermutationEntity> getPermutations() {
        return permutations;
    }

    public void setPermutations(List<PermutationEntity> permutations) {
        this.permutations = permutations;
    }

    @Override
    public String toString() {
        return "PinGroupEntity{" +
                "name='" + name + '\'' +
                ", pinIds=" + pins +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GroupEntity that = (GroupEntity) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(pins, that.pins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, pins);
    }

    public boolean match(Response response, PermutationEntity permutation) {
        Map<Identifier, PinEntity> pins = this.pins.stream()
                .collect(Collectors.toMap(
                        PinEntity::getPin,
                        Function.identity(),
                        (a, b) -> b
                ));
        return response.getDigital()
                .entrySet().stream()
                .allMatch(entry -> entry.getValue() ?
                    permutation.getActivePins().contains(pins.get(entry.getKey()))
                    :
                    permutation.getInactivePins().contains(pins.get(entry.getKey())));
    }

    public PermutationEntity apply(Connector connector, PermutationEntity nextPermutation) {
        connector.send(new Request().digitalWrite(pins.stream().map(pinEntity -> new DigitalWrite(
                pinEntity.getPin(), nextPermutation.getActivePins().contains(pinEntity)
        ))));
        return nextPermutation;
    }

    public Response read(Connector connector) {
        return getPins().stream()
                .reduce(new Request(), (a, b) -> b.apply(a), (a, b) -> b)
                .invoke(connector);
    }
}
