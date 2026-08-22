package io.freedriver.autonomy.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.freedriver.autonomy.event.speech.SpeechEvent;
import io.freedriver.autonomy.event.speech.SpeechEventType;
import io.freedriver.autonomy.events.store.EventTypes;
import io.freedriver.autonomy.service.crud.EventCrudService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class SpeechService extends EventCrudService<SpeechEvent> {

    private static final Duration LIMIT = Duration.ofDays(1);

    List<SpeechEvent> recentEvents = new ArrayList<>();

    @Override
    public String eventType() {
        return EventTypes.SPEECH_EVENT;
    }

    @Override
    public Class<SpeechEvent> payloadType() {
        return SpeechEvent.class;
    }

    public void observeEvent(@Observes SpeechEvent speechEvent) {
        speak(speechEvent);
    }

    private boolean shouldActOnEvent(SpeechEvent event) {
        return event.speechEventType() == SpeechEventType.IMPERATIVE
                || (!haveRecentActivityOnSubject(event) && !lastActivityOnSubjectIdentical(event));
    }

    private Optional<SpeechEvent> getLatestSameSubject(SpeechEvent event) {
        return recentEvents.stream()
                .filter(previousEvent -> Objects.equals(previousEvent.subject(), event.subject()))
                .max(Comparator.comparing(SpeechEvent::timestamp));
    }

    private boolean haveRecentActivityOnSubject(SpeechEvent event) {
        return getLatestSameSubject(event)
                .map(speechEvent -> speechEvent.timestamp()
                        .plus(Duration.ofSeconds(30))
                        .isAfter(Instant.now()))
                .orElse(false);
    }

    private boolean lastActivityOnSubjectIdentical(SpeechEvent event) {
        return getLatestSameSubject(event)
                .map(previousEvent -> Objects.equals(event.text(), previousEvent.text()))
                .orElse(false);
    }

    private synchronized void speak(SpeechEvent event) {
        if (shouldActOnEvent(event)) {
            log.info("SPEAK: " + event.text());
            recentEvents.add(event);
            recentEvents = recentEvents.stream()
                    .filter(previousEvent -> previousEvent.timestamp()
                            .plus(LIMIT)
                            .isBefore(Instant.now()))
                    .collect(Collectors.toList());
            persist(event);
        }
    }
}
