package io.freedriver.autonomy.jpa.entity.event;

import io.freedriver.autonomy.jpa.entity.EmbeddedEntityBase;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class EventCoordinate extends EmbeddedEntityBase {
    @Column(nullable = false)
    private String subject;
    @Column(nullable = false)
    private String property;

    public EventCoordinate() {
    }

    public EventCoordinate(String subject, String property) {
        this.subject = subject;
        this.property = property;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventCoordinate that = (EventCoordinate) o;
        return Objects.equals(subject, that.subject) &&
                Objects.equals(property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, property);
    }

    @Override
    public String toString() {
        return "EventCoordinate{" +
                "subject='" + subject + '\'' +
                ", property='" + property + '\'' +
                '}';
    }
}
