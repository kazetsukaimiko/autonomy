package io.freedriver.autonomy.entity.jsonlink;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.freedriver.autonomy.entity.EmbeddedEntityBase;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VersionEntity extends EmbeddedEntityBase implements Comparable<VersionEntity> {
    private int major = 1;
    private int minor = 0;
    private int micro = 0;

    public VersionEntity() {
    }

    public VersionEntity(VersionEntity entity) {
        super(entity);
        this.major = entity.major;
        this.minor = entity.minor;
        this.micro = entity.micro;
    }

    public VersionEntity(int major, int minor, int micro) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
    }

    @JsonIgnore
    public VersionEntity bump() {
        return new VersionEntity(major, minor, micro+1);
    }

    @JsonIgnore
    public VersionEntity milestone() {
        return new VersionEntity(major, minor+1, 0);
    }

    @JsonIgnore
    public VersionEntity release() {
        return new VersionEntity(major+1, 0, 0);
    }

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

    @Override
    public int compareTo(VersionEntity entity) {
        return Stream.of(
                Integer.compare(major, entity.major),
                Integer.compare(minor, entity.minor),
                Integer.compare(micro, entity.micro))
                .filter(r -> r != 0)
                .findFirst()
                .orElse(0);
    }

    @Override
    public String toString() {
        return Stream.of(major, minor, micro)
                .map(String::valueOf)
                .collect(Collectors.joining("."));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionEntity entity = (VersionEntity) o;
        return major == entity.major &&
                minor == entity.minor &&
                micro == entity.micro;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, micro);
    }
}
