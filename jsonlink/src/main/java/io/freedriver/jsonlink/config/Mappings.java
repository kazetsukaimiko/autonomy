package io.freedriver.jsonlink.config;

import io.freedriver.jsonlink.config.v2.Appliance;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Mappings extends ConfigFile implements Migration<io.freedriver.jsonlink.config.v2.Mappings> {
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

    @Override
    public String toString() {
        return "Mappings{" +
                "mappings=" + mappings +
                '}';
    }

    @Override
    public io.freedriver.jsonlink.config.v2.Mappings migrate() {
        io.freedriver.jsonlink.config.v2.Mappings mappings = new io.freedriver.jsonlink.config.v2.Mappings();
        mappings.setMappings(getMappings().stream()
                .map(this::migrateMapping).collect(Collectors.toSet()));
        return mappings;
    }

    public io.freedriver.jsonlink.config.v2.Mapping migrateMapping(Mapping config) {
        io.freedriver.jsonlink.config.v2.Mapping mapping = new io.freedriver.jsonlink.config.v2.Mapping();
        mapping.setConnectorId(config.getConnectorId());
        mapping.setConnectorName(config.getConnectorName());
        mapping.setAppliances(config.getPinNames().entrySet()
                .stream()
                .map(e -> new Appliance(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));
        return mapping;
    }

    @Override
    public String getFileName() {
        return "mappings.json";
    }
}
