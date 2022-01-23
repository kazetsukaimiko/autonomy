package io.freedriver.autonomy.service;

import io.freedriver.autonomy.cdi.qualifier.SpeechCache;
import io.freedriver.autonomy.jpa.entity.event.speech.SpeechEvent;
import io.freedriver.autonomy.jpa.entity.event.speech.SpeechEventType;
import io.freedriver.autonomy.service.crud.JPACrudService;
import io.freedriver.base.util.Festival;
import org.infinispan.Cache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class SpeechService extends JPACrudService<SpeechEvent> {
    private static final Logger LOGGER = Logger.getLogger(SpeechService.class.getName());

    private static final Duration LIMIT = Duration.ofDays(1);

    /*
    @Inject
    @SpeechCache
    Cache<String, SpeechEvent> speechCache;
     */

    List<SpeechEvent> recentEvents = new ArrayList<>();

    @Override
    public Class<SpeechEvent> getEntityClass() {
        return SpeechEvent.class;
    }

    @Transactional
    public void observeEvent(@Observes SpeechEvent speechEvent) {
        speak(speechEvent);
    }

    private boolean shouldActOnEvent(SpeechEvent event) {
        return
                event.getSpeechEventType() == SpeechEventType.IMPERATIVE
                ||
                        (!haveRecentActivityOnSubject(event) && !lastActivityOnSubjectIdentical(event));
    }

    private Optional<SpeechEvent> getLatestSameSubject(SpeechEvent event) {
        return recentEvents.stream()
                .filter(previousEvent -> Objects.equals(previousEvent.getSubject(), event.getSubject()))
                .max(Comparator.comparingLong(SpeechEvent::getTimestamp));
    }


    // Don't spam stuff.
    private boolean haveRecentActivityOnSubject(SpeechEvent event) {
        Optional<SpeechEvent> recentBySubject = getLatestSameSubject(event);
        return recentBySubject
                .map(speechEvent -> Instant.ofEpochMilli(speechEvent.getTimestamp())
                    .plus(Duration.ofSeconds(30))
                    .isAfter(Instant.now()))
                .orElse(false);
    }

    // Ensure we don't repeat messages.
    private boolean lastActivityOnSubjectIdentical(SpeechEvent event) {
        Optional<SpeechEvent> recentBySubject = getLatestSameSubject(event);
        return recentBySubject
                .map(previousEvent -> Objects.equals(event.getText(), previousEvent.getText()))
                .orElse(false);
    }

    private synchronized void speak(SpeechEvent event) {
        if (shouldActOnEvent(event)) {
            // Festival.speak(event.getText());
            LOGGER.info("SPEAK: " + event.getText());
            recentEvents.add(event);
            recentEvents = recentEvents.stream()
                    .filter(previousEvent -> Instant.ofEpochMilli(previousEvent.getTimestamp())
                            .plus(LIMIT)
                            .isBefore(Instant.now()))
                    .collect(Collectors.toList());
            persist(event);
        }
    }
}
