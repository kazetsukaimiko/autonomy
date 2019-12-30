package io.freedriver.autonomy.entity.jsonlink;

import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalWrite;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import org.dizitart.no2.NitriteId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PinGroupEntity extends JsonLinkEntity<PinGroupEntity> {
    private String name;
    private List<NitriteId> pinIds = new ArrayList<>();

    public PinGroupEntity() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NitriteId> getPinIds() {
        return pinIds;
    }

    public void setPinIds(List<NitriteId> pinIds) {
        this.pinIds = pinIds;
    }

    @Override
    public String toString() {
        return "PinGroupEntity{" +
                "name='" + name + '\'' +
                ", pinIds=" + pinIds +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PinGroupEntity that = (PinGroupEntity) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(pinIds, that.pinIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, pinIds);
    }

    public boolean match(Response response, PermutationEntity permutation, Supplier<Stream<PinNameEntity>> pinFetcher) {
        Map<Identifier, PinNameEntity> pins = pinFetcher.get()
                .collect(Collectors.toMap(
                        PinNameEntity::getPinNumber,
                        Function.identity(),
                        (a, b) -> b
                ));
        Map<NitriteId, Boolean> modeMap = response.getDigital().entrySet().stream()
                .filter(e -> pins.containsKey(e.getKey()))
                .collect(Collectors.toMap(
                        e -> pins.get(e.getKey()).getNitriteId(),
                        Map.Entry::getValue,
                        (a, b) -> b));
        return modeMap.entrySet().stream()
            .allMatch(e -> e.getValue() ? permutation.getActivePins().contains(e.getKey()) : pinIds.contains(e.getKey()));
    }

    public PermutationEntity apply(Connector connector, PermutationEntity nextPermutation, Supplier<Stream<PinNameEntity>> pinFetcher) {
        Map<NitriteId, PinNameEntity> pins = pinFetcher.get()
                .collect(Collectors.toMap(
                        PinNameEntity::getNitriteId,
                        Function.identity(),
                        (a, b) -> b
                ));

        connector.send(new Request().digitalWrite(pinIds.stream().map(pins::get).map(pinName -> new DigitalWrite(
                pinName.getPinNumber(), nextPermutation.getActivePins().contains(pinName.getNitriteId())
        ))));

        return nextPermutation;
    }
}
