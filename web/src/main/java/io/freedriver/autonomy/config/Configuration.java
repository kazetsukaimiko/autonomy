package io.freedriver.autonomy.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Configuration {
    private Map<Integer, String> aliases = new HashMap<>();
    private List<PinGroup> groups = new ArrayList<>();

    public Map<Integer, String> getAliases() {
        return aliases;
    }

    public void setAliases(Map<Integer, String> aliases) {
        this.aliases = aliases;
    }

    public List<PinGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<PinGroup> groups) {
        this.groups = groups;
    }


    @JsonIgnore
    public Stream<Identifier> getIdentifiers() {
        return aliases.keySet().stream()
                .map(Identifier::new);
    }
}
