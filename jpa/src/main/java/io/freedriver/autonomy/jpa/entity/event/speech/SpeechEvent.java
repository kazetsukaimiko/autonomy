package io.freedriver.autonomy.jpa.entity.event.speech;

import io.freedriver.autonomy.jpa.entity.event.Event;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
public class SpeechEvent extends Event {

    @Column
    private SpeechEventType speechEventType = SpeechEventType.INFO;

    @Column
    private String subject;

    @Column
    private String text;

    public SpeechEvent() {
    }

    public SpeechEventType getSpeechEventType() {
        return speechEventType;
    }

    public void setSpeechEventType(SpeechEventType speechEventType) {
        this.speechEventType = speechEventType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        // if (!super.equals(o)) return false;
        SpeechEvent that = (SpeechEvent) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }
}
