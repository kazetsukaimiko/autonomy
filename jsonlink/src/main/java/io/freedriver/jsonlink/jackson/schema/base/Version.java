package io.freedriver.jsonlink.jackson.schema.base;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Version {
    public static final String DELIMITER = "\\.";
    public static final int LENGTH_PARTS = 3;

    private int major;
    private int minor;
    private int micro;

    public Version(int major, int minor, int micro) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
    }

    @JsonCreator
    public Version(int[] parts) {
        this(parts[0], parts[1], parts[2]);
    }

    /*
    private Version(String[] parts) {
        this(Stream.of(parts).mapToInt(Integer::parseInt).toArray());
    }

    public Version(String representation) {
        this(Optional.ofNullable(representation)
                .map(r -> r.split(DELIMITER))
                .filter(p -> p.length == LENGTH_PARTS)
                .orElseThrow(() -> new IllegalArgumentException("Missing or malformed representation.")));
    }*/

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getMicro() {
        return micro;
    }

    public void setMicro(int micro) {
        this.micro = micro;
    }

    @JsonValue
    public int[] jsonValue() {
        return new int[] {major, minor, micro};
    }

    @Override
    public String toString() {
        return Stream.of(major, minor, micro)
                .map(String::valueOf)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return major == version.major &&
                minor == version.minor &&
                micro == version.micro;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, micro);
    }
}
