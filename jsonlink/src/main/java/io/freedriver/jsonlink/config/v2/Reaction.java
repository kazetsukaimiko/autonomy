package io.freedriver.jsonlink.config.v2;

import java.util.Objects;

public class Reaction {
    private String coordinate;
    private String appliance;

    public Reaction() {
    }

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    public String getAppliance() {
        return appliance;
    }

    public void setAppliance(String appliance) {
        this.appliance = appliance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reaction reaction = (Reaction) o;
        return Objects.equals(coordinate, reaction.coordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinate);
    }

    @Override
    public String toString() {
        return "Reaction{" +
                "coordinate='" + coordinate + '\'' +
                ", appliance='" + appliance + '\'' +
                '}';
    }
}
