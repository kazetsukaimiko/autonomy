package io.freedriver.jsonlink.config;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Mappings {
    private Set<Mapping> mappings = new HashSet<>();

    public Mappings() {
    }

    public Set<Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(Set<Mapping> mappings) {
        this.mappings = mappings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mappings mappings1 = (Mappings) o;
        return Objects.equals(mappings, mappings1.mappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappings);
    }
}
