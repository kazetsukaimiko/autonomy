package io.freedriver.jsonlink.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ConnectorConfig {
    private Set<PinName> pins = new HashSet<>();
    private Map<String, Map<String, Boolean>> groups = new HashMap<>();

    public ConnectorConfig() {
    }

    public Set<PinName> getPins() {
        return pins;
    }

    public void setPins(Set<PinName> pins) {
        this.pins = pins;
    }

    public Map<String, Map<String, Boolean>> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Map<String, Boolean>> groups) {
        this.groups = groups;
    }

    @JsonIgnore
    public Stream<Identifier> getIdentifiers() {
        return pins.stream()
                .map(PinName::getPinNumber);
    }
}
