package io.freedriver.autonomy.jpa.entity.event.speech;

import io.freedriver.autonomy.jpa.entity.event.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class SpeechEvent extends Event {

    @Column
    private SpeechEventType speechEventType = SpeechEventType.INFO;

    @Column
    private String subject;

    @Column
    private String text;
}
